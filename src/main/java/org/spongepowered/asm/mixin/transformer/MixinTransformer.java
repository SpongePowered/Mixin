/*
 * This file is part of Mixin, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.asm.mixin.transformer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.FieldNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinApplyError;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.MixinException;
import org.spongepowered.asm.mixin.MixinPrepareError;
import org.spongepowered.asm.mixin.audit.ClassAlreadyLoadedException;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler.ErrorAction;
import org.spongepowered.asm.mixin.transformer.MixinTransformerModuleCheckClass.ValidationFailedException;
import org.spongepowered.asm.mixin.transformer.debug.IDecompiler;
import org.spongepowered.asm.mixin.transformer.debug.IHotSwap;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.transformers.TreeTransformer;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.PrettyPrinter;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

/**
 * Transformer which manages the mixin configuration and application process
 */
public class MixinTransformer extends TreeTransformer {
    
    /**
     * Phase during which an error occurred, delegates to functionality in
     * available handler
     */
    static enum ErrorPhase {
        /**
         * Error during initialisation of a MixinConfig
         */
        PREPARE {
            @Override
            ErrorAction onError(IMixinErrorHandler handler, String context, InvalidMixinException ex, MixinInfo mixin, ErrorAction action) {
                try {
                    return handler.onPrepareError(mixin.getParent(), ex, mixin, action);
                } catch (AbstractMethodError ame) {
                    // Catch if error handler is pre-0.5.4
                    return action;
                }
            }
            
            @Override
            protected String getContext(MixinInfo mixin, String context) {
                return String.format("preparing %s in %s", mixin.getName(), context);
            }
        },
        /**
         * Error during application of a mixin to a target class
         */
        APPLY {
            @Override
            ErrorAction onError(IMixinErrorHandler handler, String context, InvalidMixinException ex, MixinInfo mixin, ErrorAction action) {
                try {
                    return handler.onApplyError(context, ex, mixin, action);
                } catch (AbstractMethodError ame) {
                    // Catch if error handler is pre-0.5.4
                    return action;
                }
            }
            
            @Override
            protected String getContext(MixinInfo mixin, String context) {
                return String.format("%s -> %s", mixin, context);
            }
        };
        
        /**
         * Human-readable name
         */
        private final String text;
        
        private ErrorPhase() {
            this.text = this.name().toLowerCase();
        }
        
        abstract ErrorAction onError(IMixinErrorHandler handler, String context, InvalidMixinException ex, MixinInfo mixin, ErrorAction action);

        protected abstract String getContext(MixinInfo mixin, String context);

        public String getLogMessage(String context, InvalidMixinException ex, MixinInfo mixin) {
            return String.format("Mixin %s failed %s: %s %s", this.text, this.getContext(mixin, context), ex.getClass().getName(), ex.getMessage());
        }

        public String getErrorMessage(MixinInfo mixin, MixinConfig config, Phase phase) {
            return String.format("Mixin [%s] from phase [%s] in config [%s] FAILED during %s", mixin, phase, config, this.name());
        }
        
    }
    
    /**
     * Proxy transformer for the mixin transformer. These transformers are used
     * to allow the mixin transformer to be re-registered in the transformer
     * chain at a later stage in startup without having to fully re-initialise
     * the mixin transformer itself. Only the latest proxy to be instantiated
     * will actually provide callbacks to the underlying mixin transformer.
     */
    public static class Proxy implements IClassTransformer {
        
        /**
         * All existing proxies
         */
        private static List<Proxy> proxies = new ArrayList<Proxy>();
        
        /**
         * Actual mixin transformer instance
         */
        private static MixinTransformer transformer = new MixinTransformer();
        
        /**
         * True if this is the active proxy, newer proxies disable their older
         * siblings
         */
        private boolean isActive = true;
        
        public Proxy() {
            for (Proxy hook : Proxy.proxies) {
                hook.isActive = false;
            }
            
            Proxy.proxies.add(this);
            LogManager.getLogger("mixin").debug("Adding new mixin transformer proxy #{}", Proxy.proxies.size());
        }
        
        @Override
        public byte[] transform(String name, String transformedName, byte[] basicClass) {
            if (this.isActive) {
                return Proxy.transformer.transform(name, transformedName, basicClass);
            }
            
            return basicClass;
        }
    }

    /**
     * Re-entrance semaphore used to share re-entrance data with the TreeInfo
     */
    class ReEntranceState {
        
        /**
         * Max valid depth
         */
        private final int maxDepth;
        
        /**
         * Re-entrance depth
         */
        private int depth = 0;
        
        /**
         * Semaphore set when check exceeds a depth of 1
         */
        private boolean semaphore = false;
        
        public ReEntranceState(int maxDepth) {
            this.maxDepth = maxDepth;
        }
        
        /**
         * Get max depth
         */
        public int getMaxDepth() {
            return this.maxDepth;
        }
        
        /**
         * Get current depth
         */
        public int getDepth() {
            return this.depth;
        }
        
        /**
         * Increase the re-entrance depth counter and set the semaphore if depth
         * exceeds max depth
         * 
         * @return fluent interface
         */
        ReEntranceState push() {
            this.depth++;
            this.checkAndSet();
            return this;
        }
        
        /**
         * Decrease the re-entrance depth
         * 
         * @return fluent interface
         */
        ReEntranceState pop() {
            if (this.depth == 0) {
                throw new IllegalStateException("ReEntranceState pop() with zero depth");
            }
            
            this.depth--;
            return this;
        }
        
        /**
         * Run the depth check but do not set the semaphore
         * 
         * @return true if depth has exceeded max
         */
        boolean check() {
            return this.depth > this.maxDepth;
        }
        
        /**
         * Run the depth check and set the semaphore if depth is exceeded
         * 
         * @return true if semaphore is set
         */
        boolean checkAndSet() {
            return this.semaphore |= this.check();
        }
        
        /**
         * Set the semaphore
         * 
         * @return fluent interface
         */
        ReEntranceState set() {
            this.semaphore = true;
            return this;
        }
        
        /**
         * Get whether the semaphore is set
         */
        boolean isSet() {
            return this.semaphore;
        }
        
        /**
         * Clear the semaphore
         * 
         * @return fluent interface
         */
        ReEntranceState clear() {
            this.semaphore = false;
            return this;
        }
    }
    
    static final File DEBUG_OUTPUT = new File(Constants.DEBUG_OUTPUT_PATH);
    
    /**
     * Log all the things
     */
    private final Logger logger = LogManager.getLogger("mixin");
    
    /**
     * All mixin configuration bundles
     */
    private final List<MixinConfig> configs = new ArrayList<MixinConfig>();
    
    /**
     * Uninitialised mixin configuration bundles 
     */
    private final List<MixinConfig> pendingConfigs = new ArrayList<MixinConfig>();
    
    /**
     * Transformer modules
     */
    private final List<IMixinTransformerModule> modules = new ArrayList<IMixinTransformerModule>();

    /**
     * Current environment 
     */
    private MixinEnvironment currentEnvironment;
    
    /**
     * Re-entrance detector
     */
    private final ReEntranceState lock = new ReEntranceState(1);
    
    /**
     * Session ID, used as a check when parsing {@link MixinMerged} annotations
     * to prevent them being applied at compile time by people trying to
     * circumvent mixin application
     */
    private final String sessionId = UUID.randomUUID().toString();
    
    /**
     * Logging level for verbose messages 
     */
    private Level verboseLoggingLevel = Level.DEBUG;

    /**
     * Handling an error state, do not process further mixins
     */
    private boolean errorState = false; 
    
    /**
     * Directory to export classes to when debug.export is enabled
     */
    private final File classExportDir = new File(MixinTransformer.DEBUG_OUTPUT, "class");
    
    /**
     * Runtime decompiler for exported classes 
     */
    private final IDecompiler decompiler;

    /**
     * Hot-Swap agent
     */
    private final IHotSwap hotSwapper;

    /**
     * ctor 
     */
    MixinTransformer() {
        MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();
        
        Object globalMixinTransformer = environment.getActiveTransformer();
        if (globalMixinTransformer instanceof IClassTransformer) {
            throw new MixinException("Terminating MixinTransformer instance " + this);
        }
        
        // I am a leaf on the wind
        environment.setActiveTransformer(this);
        
        TreeInfo.setLock(this.lock);
        
        this.decompiler = this.initDecompiler(new File(MixinTransformer.DEBUG_OUTPUT, "java"));
        this.hotSwapper = this.initHotSwapper();

        try {
            FileUtils.deleteDirectory(this.classExportDir);
        } catch (IOException ex) {
            this.logger.warn("Error cleaning class output directory: {}", ex.getMessage());
        }
    }
    
    private IDecompiler initDecompiler(File outputPath) {
        if (!MixinEnvironment.getCurrentEnvironment().getOption(Option.DEBUG_EXPORT_DECOMPILE)) {
            return null;
        }

        try {
            this.logger.info("Attempting to load Fernflower decompiler");
            @SuppressWarnings("unchecked")
            Class<? extends IDecompiler> clazz =
                (Class<? extends IDecompiler>)Class.forName("org.spongepowered.asm.mixin.transformer.debug.RuntimeDecompiler");
            Constructor<? extends IDecompiler> ctor = clazz.getDeclaredConstructor(File.class);
            IDecompiler decompiler = ctor.newInstance(outputPath);
            this.logger.info("Fernflower decompiler was successfully initialised, exported classes will be decompiled");
            return decompiler;
        } catch (Throwable th) {
            this.logger.info("Fernflower could not be loaded, exported classes will not be decompiled. {}: {}",
                    th.getClass().getSimpleName(), th.getMessage());
        }
        return null;
    }

    private IHotSwap initHotSwapper() {
        if (!MixinEnvironment.getCurrentEnvironment().getOption(Option.HOT_SWAP)) {
            return null;
        }

        try {
            this.logger.info("Attempting to load Hot-Swap agent");
            @SuppressWarnings("unchecked")
            Class<? extends IHotSwap> clazz =
                    (Class<? extends IHotSwap>)Class.forName("org.spongepowered.tools.agent.MixinAgent");
            Constructor<? extends IHotSwap> ctor = clazz.getDeclaredConstructor(MixinTransformer.class);
            return ctor.newInstance(this);
        } catch (Throwable th) {
            this.logger.info("Hot-swap agent could not be loaded, hot swapping of mixins won't work. {}: {}",
                    th.getClass().getSimpleName(), th.getMessage());
        }

        return null;
    }

    /**
     * Force-load all classes targetted by mixins but not yet applied
     */
    public void audit() {
        Set<String> unhandled = new HashSet<String>();
        
        for (MixinConfig config : this.configs) {
            unhandled.addAll(config.getUnhandledTargets());
        }

        Logger auditLogger = LogManager.getLogger("mixin/audit");

        for (String target : unhandled) {
            try {
                auditLogger.info("Force-loading class {}", target);
                Class.forName(target, true, Launch.classLoader);
            } catch (ClassNotFoundException ex) {
                auditLogger.error("Could not force-load " + target, ex);
            }
        }
        
        for (MixinConfig config : this.configs) {
            for (String target : config.getUnhandledTargets()) {
                ClassAlreadyLoadedException ex = new ClassAlreadyLoadedException(target + " was already classloaded");
                auditLogger.error("Could not force-load " + target, ex);
            }
        }
    }

    /* (non-Javadoc)
     * @see net.minecraft.launchwrapper.IClassTransformer
     *      #transform(java.lang.String, java.lang.String, byte[])
     */
    @Override
    public synchronized byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || transformedName == null || this.errorState) {
            return basicClass;
        }
        
        boolean locked = this.lock.push().isSet();
        
        MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();
        
        if (this.currentEnvironment != environment && !locked) {
            try {
                this.init(environment);
            } catch (Exception ex) {
                this.lock.pop();
                throw new MixinException(ex);
            }
        }
        
        try {
            SortedSet<MixinInfo> mixins = null;
            boolean invalidRef = false;
            
            for (MixinConfig config : this.configs) {
                if (config.packageMatch(transformedName)) {
                    if (config.canPassThrough(transformedName)) {
                        return this.passThrough(name, transformedName, basicClass);
                    }
                    invalidRef = true;
                    continue;
                }
                
                if (config.hasMixinsFor(transformedName)) {
                    if (mixins == null) {
                        mixins = new TreeSet<MixinInfo>();
                    }
                    
                    // Get and sort mixins for the class
                    mixins.addAll(config.getMixinsFor(transformedName));
                }
            }
            
            if (invalidRef) {
                throw new NoClassDefFoundError(String.format("%s is a mixin class and cannot be referenced directly", transformedName));
            }
            
            if (mixins != null) {
                // Re-entrance is "safe" as long as we don't need to apply any mixins, if there are mixins then we need to panic now
                if (locked) {
                    this.logger.warn("Re-entrance detected, this will cause serious problems.", new MixinException());
                    throw new MixinApplyError("Re-entrance error.");
                }

                if (this.hotSwapper != null) {
                    this.hotSwapper.registerTargetClass(transformedName, basicClass);
                }

                try {
                    // Tree for target class
                    ClassNode targetClassNode = this.readClass(basicClass, true);
                    TargetClassContext context = new TargetClassContext(this.sessionId, transformedName, targetClassNode, mixins);
                    basicClass = this.applyMixins(context);
                } catch (InvalidMixinException th) {
                    this.dumpClassOnFailure(transformedName, basicClass, environment);
                    this.handleMixinApplyError(transformedName, th, environment);
                }
            }

            return basicClass;
        } catch (Throwable th) {
            th.printStackTrace();
            this.dumpClassOnFailure(transformedName, basicClass, environment);
            throw new MixinTransformerError("An unexpected critical error was encountered", th);
        } finally {
            this.lock.pop();
        }
    }

    /**
     * Update a mixin class with new bytecode.
     *
     * @param mixinClass Name of the mixin
     * @param bytes New bytecode
     * @return List of classes that need to be updated
     */
    public List<String> reload(String mixinClass, byte[] bytes) {
        if (this.lock.getDepth() > 0) {
            throw new MixinApplyError("Cannot reload mixin if re-entrant lock entered");
        }
        List<String> targets = new ArrayList<String>();
        for (MixinConfig config : this.configs) {
            targets.addAll(config.reloadMixin(mixinClass, bytes));
        }
        return targets;
    }

    private void init(MixinEnvironment environment) {
        this.verboseLoggingLevel = (environment.getOption(Option.DEBUG_VERBOSE)) ? Level.INFO : Level.DEBUG;
        this.logger.log(this.verboseLoggingLevel, "Preparing mixins for {}", environment);
        long startTime = System.currentTimeMillis();
        
        this.addConfigs(environment);
        this.addModules(environment);
        int totalMixins = this.initConfigs(environment);
        this.currentEnvironment = environment;
        
        double elapsedTime = (System.currentTimeMillis() - startTime) * 0.001D;
        if (elapsedTime > 0.25D) {
            String elapsed = new DecimalFormat("###0.000").format(elapsedTime);
            String perMixinTime = new DecimalFormat("###0.0").format((elapsedTime / totalMixins) * 1000.0);
            this.logger.log(this.verboseLoggingLevel, "Prepared {} mixins in {} sec ({} msec avg.)", totalMixins, elapsed, perMixinTime);
        }
    }

    /**
     * Add configurations from the supplied mixin environment to the configs set
     * 
     * @param environment Environment to query
     */
    private void addConfigs(MixinEnvironment environment) {
        List<String> configs = environment.getMixinConfigs();
        
        if (configs != null) {
            for (String configFile : configs) {
                try {
                    MixinConfig config = MixinConfig.create(configFile, environment);
                    if (config != null) {
                        this.logger.log(this.verboseLoggingLevel, "Adding config {}", config);
                        this.pendingConfigs.add(config);
                    }
                } catch (Exception ex) {
                    this.logger.warn(String.format("Failed to load mixin config: %s", configFile), ex);
                }
            }
        }
        
        Collections.sort(this.pendingConfigs);
    }

    /**
     * Set up this transformer using options from the supplied environment
     * 
     * @param environment Environment to query
     */
    private void addModules(MixinEnvironment environment) {
        this.modules.clear();
        
        // Run CheckClassAdapter on the mixin bytecode if debug option is enabled 
        if (environment.getOption(Option.DEBUG_VERIFY)) {
            this.modules.add(new MixinTransformerModuleCheckClass());
        }
        
        // Run implementation checker if option is enabled
        if (environment.getOption(Option.CHECK_IMPLEMENTS)) {
            this.modules.add(new MixinTransformerModuleInterfaceChecker());
        }
    }

    /**
     * Initialise mixin configs
     * 
     * @param environment Environment
     * @return total number of mixins initialised
     */
    private int initConfigs(MixinEnvironment environment) {
        int totalMixins = 0;
        
        for (MixinConfig config : this.pendingConfigs) {
            try {
                this.logger.log(this.verboseLoggingLevel, "Preparing {} ({})", config, config.getDeclaredMixinCount());
                config.initialise(this.hotSwapper);
                totalMixins += config.getMixinCount();
            } catch (InvalidMixinException ex) {
                this.handleMixinPrepareError(config, ex, environment);
            } catch (Exception ex) {
                this.logger.error("Error encountered whilst initialising mixin config '" + config.getName() + "': " + ex.getMessage(), ex);
            }
        }
        
        for (MixinConfig config : this.pendingConfigs) {
            IMixinConfigPlugin plugin = config.getPlugin();
            if (plugin == null) {
                continue;
            }
            
            Set<String> otherTargets = new HashSet<String>();
            for (MixinConfig otherConfig : this.pendingConfigs) {
                if (!otherConfig.equals(config)) {
                    otherTargets.addAll(otherConfig.getTargets());
                }
            }
            
            plugin.acceptTargets(config.getTargets(), Collections.unmodifiableSet(otherTargets));
        }

        for (MixinConfig config : this.pendingConfigs) {
            try {
                config.postInitialise(this.hotSwapper);
            } catch (InvalidMixinException ex) {
                this.handleMixinPrepareError(config, ex, environment);
            } catch (Exception ex) {
                this.logger.error("Error encountered during mixin config postInit step'" + config.getName() + "': " + ex.getMessage(), ex);
            }
        }
        
        this.configs.addAll(this.pendingConfigs);
        Collections.sort(this.configs);
        this.pendingConfigs.clear();
        
        return totalMixins;
    }

    /**
     * "Pass through" a synthetic inner class. Transforms package-private
     * members in the class into public so that they are accessible from their
     * new home in the target class
     * 
     * @param name original class name
     * @param transformedName deobfuscated class name
     * @param basicClass class bytecode
     * @return public-ified class bytecode
     */
    private byte[] passThrough(String name, String transformedName, byte[] basicClass) {
        ClassNode passThroughClass = this.readClass(basicClass, true);
        
        // Make the class public
        passThroughClass.access |= Opcodes.ACC_PUBLIC;
        
        // Make package-private fields public
        for (FieldNode field : passThroughClass.fields) {
            if ((field.access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) == 0) {
                field.access |= Opcodes.ACC_PUBLIC;
            }
        }
        
        // Make package-private methods public
        for (MethodNode method : passThroughClass.methods) {
            if ((method.access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) == 0) {
                method.access |= Opcodes.ACC_PUBLIC;
            }
        }
        
        return this.writeClass(transformedName, passThroughClass);
    }

    /**
     * Apply mixins for specified target class to the class described by the
     * supplied byte array.
     * 
     * @param context 
     * @return class bytecode after application of mixins
     */
    private byte[] applyMixins(TargetClassContext context) {
        this.preApply(context);
        this.apply(context);
        try {
            this.postApply(context);
        } catch (ValidationFailedException ex) {
            this.logger.info(ex.getMessage());
            // If verify is enabled and failed, write out the bytecode to allow us to inspect it
            if (MixinEnvironment.getCurrentEnvironment().getOption(Option.DEBUG_EXPORT)) {
                this.writeClass(context);
            }
        }
        return this.writeClass(context);
    }

    /**
     * Process tasks before mixin application
     * 
     * @param context Target class context
     */
    private void preApply(TargetClassContext context) {
        for (IMixinTransformerModule module : this.modules) {
            module.preApply(context);
        }
    }

    /**
     * Apply the mixins to the target class
     * 
     * @param context Target class context
     */
    private void apply(TargetClassContext context) {
        context.applyMixins();
    }

    /**
     * Process tasks after mixin application
     * 
     * @param context Target class context
     */
    private void postApply(TargetClassContext context) {
        for (IMixinTransformerModule module : this.modules) {
            module.postApply(context);
        }
    }

    private void handleMixinPrepareError(MixinConfig config, InvalidMixinException ex, MixinEnvironment environment) throws MixinPrepareError {
        this.handleMixinError(config.getName(), ex, environment, ErrorPhase.PREPARE);
    }
    
    private void handleMixinApplyError(String targetClass, InvalidMixinException ex, MixinEnvironment environment) throws MixinApplyError {
        this.handleMixinError(targetClass, ex, environment, ErrorPhase.APPLY);
    }

    private void handleMixinError(String context, InvalidMixinException ex, MixinEnvironment environment, ErrorPhase errorPhase) throws Error {
        this.errorState = true;
        
        MixinInfo mixin = ex.getMixin();
        MixinConfig config = mixin.getParent();
        Phase phase = mixin.getPhase();
        ErrorAction action = config.isRequired() ? ErrorAction.ERROR : ErrorAction.WARN;
        
        if (environment.getOption(Option.DEBUG_VERBOSE)) {
            new PrettyPrinter()
                .add("Invalid Mixin").centre()
                .hr('-')
                .kvWidth(10)
                .kv("Action", errorPhase.name())
                .kv("Mixin", mixin.getClassName())
                .kv("Config", config.getName())
                .kv("Phase", phase)
                .hr('-')
                .add("    %s", ex.getClass().getName())
                .hr('-')
                .addWrapped("    %s", ex.getMessage())
                .hr('-')
                .add(ex.getStackTrace(), 8)
                .trace(action.logLevel);
        }
    
        for (IMixinErrorHandler handler : this.getErrorHandlers(mixin.getPhase())) {
            ErrorAction newAction = errorPhase.onError(handler, context, ex, mixin, action);
            if (newAction != null) {
                action = newAction;
            }
        }
        
        this.logger.log(action.logLevel, errorPhase.getLogMessage(context, ex, mixin), ex);
        
        this.errorState = false;

        if (action == ErrorAction.ERROR) {
            throw new MixinApplyError(errorPhase.getErrorMessage(mixin, config, phase), ex);
        }
    }

    private List<IMixinErrorHandler> getErrorHandlers(Phase phase) {
        List<IMixinErrorHandler> handlers = new ArrayList<IMixinErrorHandler>();
        
        for (String handlerClassName : MixinEnvironment.getEnvironment(phase).getErrorHandlerClasses()) {
            try {
                this.logger.info("Instancing error handler class {}", handlerClassName);
                Class<?> handlerClass = Class.forName(handlerClassName, true, Launch.classLoader);
                IMixinErrorHandler handler = (IMixinErrorHandler)handlerClass.newInstance();
                if (handler != null) {
                    handlers.add(handler);
                }
            } catch (Throwable th) {
                // skip bad handlers
            }
        }
        
        return handlers;
    }

    private String prepareFilter(String filter) {
        filter = "^\\Q" + filter.replace("**", "\201").replace("*", "\202").replace("?", "\203") + "\\E$";
        return filter.replace("\201", "\\E.*\\Q").replace("\202", "\\E[^\\.]+\\Q").replace("\203", "\\E.\\Q").replace("\\Q\\E", "");
    }

    private boolean applyFilter(String filter, String subject) {
        return Pattern.compile(this.prepareFilter(filter), Pattern.CASE_INSENSITIVE).matcher(subject).matches();
    }

    private byte[] writeClass(TargetClassContext context) {
        return this.writeClass(context.getClassName(), context.getClassNode());
    }
    
    private byte[] writeClass(String transformedName, ClassNode targetClass) {
        // Collapse tree to bytes
        byte[] bytes = this.writeClass(targetClass);
        
        // Export transformed class for debugging purposes
        MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();
        if (environment.getOption(Option.DEBUG_EXPORT)) {
            String filter = environment.getOptionValue(Option.DEBUG_EXPORT_FILTER);
            if (filter == null || this.applyFilter(filter, transformedName)) {
                File outputFile = this.dumpClass(transformedName.replace('.', '/'), bytes);
                if (this.decompiler != null) {
                    this.decompiler.decompile(outputFile);
                }
            }
        }
        
        return bytes;
    }

    private void dumpClassOnFailure(String className, byte[] bytes, MixinEnvironment env) {
        if (env.getOption(Option.DUMP_TARGET_ON_FAILURE)) {
            this.dumpClass(className.replace('.', '/') + ".target", bytes);
        }
    }

    private File dumpClass(String fileName, byte[] bytes) {
        File outputFile = new File(this.classExportDir, fileName + ".class");
        try {
            FileUtils.writeByteArrayToFile(outputFile, bytes);
        } catch (IOException ex) {
            // don't care
        }
        return outputFile;
    }
}
