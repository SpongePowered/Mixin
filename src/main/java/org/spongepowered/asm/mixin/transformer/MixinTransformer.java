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
import java.util.*;
import java.util.regex.Pattern;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.FieldNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinApplyError;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler.ErrorAction;
import org.spongepowered.asm.mixin.transformer.debug.IDecompiler;
import org.spongepowered.asm.transformers.TreeTransformer;
import org.spongepowered.asm.util.Constants;

/**
 * Transformer which manages the mixin configuration and application process
 */
public class MixinTransformer extends TreeTransformer {
    
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
     * Mapping of all interface mixins, with the key being their there targeted
     * interface
     */
    private HashMap<String, List<MixinInfo>> configInterfaceMixins = new HashMap<String, List<MixinInfo>>();

    /**
     * Cache of all interface mixins. This is used to keep track of interface
     * mixins while doing depth first search for them
     */
    private HashMap<String, List<MixinInfo>> interfaceMixins = new HashMap<String, List<MixinInfo>>();

    /**
     * ctor 
     */
    MixinTransformer() {
        MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();
        
        Object globalMixinTransformer = environment.getActiveTransformer();
        if (globalMixinTransformer instanceof IClassTransformer) {
            throw new RuntimeException("Terminating MixinTransformer instance " + this);
        }
        
        // I am a leaf on the wind
        environment.setActiveTransformer(this);
        
        TreeInfo.setLock(this.lock);
        
        this.decompiler = this.initDecompiler(new File(MixinTransformer.DEBUG_OUTPUT, "java"));
        
        try {
            FileUtils.deleteDirectory(this.classExportDir);
        } catch (IOException ex) {
            this.logger.warn("Error cleaning class output directory: {}", ex.getMessage());
        }
    }
    
    private IDecompiler initDecompiler(File outputPath) {
        if (!MixinEnvironment.getCurrentEnvironment().getOption(Option.DEBUG_EXPORT)) {
            return null;
        }

        try {
            this.logger.info("Attempting to load Fernflower decompiler");
            @SuppressWarnings("unchecked")
            Class<? extends IDecompiler> clazz =
                (Class<? extends IDecompiler>)Class.forName("org.spongepowered.asm.mixin.transformer.debug.RuntimeDecompiler");
            Constructor<? extends IDecompiler> ctor = clazz.getDeclaredConstructor(File.class);
            return ctor.newInstance(outputPath);
        } catch (Throwable th) {
            this.logger.info("Fernflower could not be loaded, exported classes will not be decompiled. {}: {}",
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
        
        for (String nextClass : unhandled) {
            try {
                this.logger.info("Force-loading class {}", nextClass);
                Class.forName(nextClass, true, Launch.classLoader);
            } catch (ClassNotFoundException ex) {
                throw new Error("Could not force-load " + nextClass);
            }
        }
    }

    /* (non-Javadoc)
     * @see net.minecraft.launchwrapper.IClassTransformer
     *      #transform(java.lang.String, java.lang.String, byte[])
     */
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || transformedName == null || this.errorState) {
            return basicClass;
        }

        List<MixinInfo> interfaceMixins = this.findInterfaceMixins(transformedName, basicClass);

        boolean locked = this.lock.push().isSet();

        MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();

        if (this.currentEnvironment != environment && !locked) {
            try {
                this.init(environment);
            } catch (Exception ex) {
                this.lock.pop();
                throw new RuntimeException(ex);
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
                mixins.addAll(interfaceMixins);
            } else if (!interfaceMixins.isEmpty()) {
                mixins = new TreeSet<MixinInfo>(interfaceMixins);
            }

            if (mixins != null) {
                // Re-entrance is "safe" as long as we don't need to apply any mixins, if there are mixins then we need to panic now
                if (locked) {
                    this.logger.warn("Re-entrance detected, this will cause serious problems.", new RuntimeException());
                    throw new MixinApplyError("Re-entrance error.");
                }
                
                try {
                    basicClass = this.applyMixins(transformedName, basicClass, mixins);
                } catch (InvalidMixinException th) {
                    if (environment.getOption(Option.DUMP_TARGET_ON_FAILURE)) {
                        this.dumpClass(transformedName.replace('.', '/') + ".target", basicClass);
                    }
                    
                    this.handleMixinErrorState(transformedName, th);
                }
            }

            return basicClass;
        } catch (Exception ex) {
            if (environment.getOption(Option.DUMP_TARGET_ON_FAILURE)) {
                this.dumpClass(transformedName.replace('.', '/') + ".target", basicClass);
            }

            throw new MixinTransformerError("An unexpected critical error was encountered", ex);
        } finally {
            this.lock.pop();
        }
    }

    private ClassNode getClassNode(int flags, byte[] bytes) {
        ClassNode classNode = new ClassNode(Opcodes.ASM5);
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(classNode, flags);
        return classNode;
    }

    private List<MixinInfo> interfaceMixinsForClass(String className) {
        className = className.replace('/', '.');
        List<MixinInfo> mixinsForInterface = this.interfaceMixins.get(className);
        if (mixinsForInterface == null) {
            try {
                Launch.classLoader.loadClass(className);
                mixinsForInterface = this.interfaceMixins.get(className);
            } catch (ClassNotFoundException e) {
                this.logger.catching(e);
                mixinsForInterface = Collections.<MixinInfo>emptyList();
            }
        }
        return mixinsForInterface;
    }

    /**
     * Finds all of the interface mixins associated with the class.
     *
     * Its all of the mixins that target at least of of its super interfaces
     * and not target any of its superclasses either directly or indirectly.
     *
     * @param className Name of the class
     * @param bytecodes Bytecode of the class
     * @return List containing all interface mixins for the class
     */
    private List<MixinInfo> findInterfaceMixins(String className, byte[] bytecodes) {
        List<MixinInfo> classInterfaceMixins = new ArrayList<MixinInfo>();
        ClassNode node = this.getClassNode(0, bytecodes);
        for (String intf : node.interfaces) {
            List<MixinInfo> mixinsForInterface = this.interfaceMixinsForClass(intf);
            if (mixinsForInterface != null) {
                classInterfaceMixins.addAll(mixinsForInterface);
            }
        }
        List<MixinInfo> mixinsFoSuperClass = this.interfaceMixinsForClass(node.superName);
        if (mixinsFoSuperClass != null) {
            classInterfaceMixins.removeAll(mixinsFoSuperClass);
        }
        List<MixinInfo> configMixins = this.configInterfaceMixins.get(className);
        if (configMixins != null) {
            classInterfaceMixins.addAll(configMixins);
        }
        this.interfaceMixins.put(className, classInterfaceMixins);
        if ((node.access & Opcodes.ACC_INTERFACE) != 0) {
            return Collections.<MixinInfo>emptyList();
        }
        return classInterfaceMixins;
    }

    private void init(MixinEnvironment environment) {
        this.verboseLoggingLevel = (environment.getOption(Option.DEBUG_VERBOSE)) ? Level.INFO : Level.DEBUG;
        this.logger.log(this.verboseLoggingLevel, "Preparing mixins for {}", environment);

        this.interfaceMixins.put("java.lang.Object", Collections.<MixinInfo>emptyList());

        this.addConfigs(environment);
        this.addModules(environment);
        this.initConfigs();
        this.currentEnvironment = environment;
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
                    MixinConfig config = MixinConfig.create(configFile);
                    if (config != null) {
                        this.logger.log(this.verboseLoggingLevel, "Adding mixin config {}", config);
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
     */
    private void initConfigs() {
        for (MixinConfig config : this.pendingConfigs) {
            try {
                config.initialise();
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
                config.postInitialise();
                Map<String, List<MixinInfo>> interfaceMixins = config.getInterfaceMixins();
                for (String interfaceTarget : interfaceMixins.keySet()) {
                    if (!this.configInterfaceMixins.containsKey(interfaceTarget)) {
                        this.configInterfaceMixins.put(interfaceTarget, new ArrayList<MixinInfo>());
                    }
                    this.configInterfaceMixins.get(interfaceTarget).addAll(interfaceMixins.get(interfaceTarget));
                }
            } catch (Exception ex) {
                this.logger.error("Error encountered during mixin config postInit setp'" + config.getName() + "': " + ex.getMessage(), ex);
            }
        }
        
        this.configs.addAll(this.pendingConfigs);
        Collections.sort(this.configs);
        this.pendingConfigs.clear();
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
     * @param transformedName 
     * @param basicClass
     * @param mixins
     * @return class bytecode after application of mixins
     */
    private byte[] applyMixins(String transformedName, byte[] basicClass, SortedSet<MixinInfo> mixins) {
        // Tree for target class
        ClassNode targetClass = this.readClass(basicClass, true);
        
        this.preApply(transformedName, targetClass, mixins);
        this.apply(transformedName, targetClass, mixins);
        this.postApply(transformedName, targetClass, mixins);
        
        return this.writeClass(transformedName, targetClass);
    }

    /**
     * Process tasks before mixin application
     * 
     * @param transformedName Target class transformed name
     * @param targetClass Target class
     * @param mixins Mixin which were just applied
     */
    private void preApply(String transformedName, ClassNode targetClass, SortedSet<MixinInfo> mixins) {
        for (IMixinTransformerModule module : this.modules) {
            module.preApply(transformedName, targetClass, mixins);
        }
    }

    /**
     * Apply the mixins to the target class
     * 
     * @param transformedName Target class transformed name
     * @param targetClass Target class
     * @param mixins Mixin which were just applied
     */
    private void apply(String transformedName, ClassNode targetClass, SortedSet<MixinInfo> mixins) {
        MixinApplicator applicator = new MixinApplicator(this.sessionId, transformedName, targetClass);
        applicator.apply(mixins);
    }

    /**
     * Process tasks after mixin application
     * 
     * @param transformedName Target class transformed name
     * @param targetClass Target class
     * @param mixins Mixin which were just applied
     */
    private void postApply(String transformedName, ClassNode targetClass, SortedSet<MixinInfo> mixins) {
        for (IMixinTransformerModule module : this.modules) {
            module.postApply(transformedName, targetClass, mixins);
        }
    }

    private void handleMixinErrorState(String targetClassName, InvalidMixinException th) throws MixinApplyError {
        this.errorState = true;
        
        MixinInfo mixin = th.getMixin();
        MixinConfig config = mixin.getParent();
        ErrorAction action = config.isRequired() ? ErrorAction.ERROR : ErrorAction.WARN;
        
        if (MixinEnvironment.getCurrentEnvironment().getOption(Option.DEBUG_VERBOSE)) {
            th.printStackTrace();
        }
        
        for (IMixinErrorHandler handler : this.getErrorHandlers(mixin.getPhase())) {
            ErrorAction newAction = handler.onError(targetClassName, th, mixin, action);
            if (newAction != null) {
                action = newAction;
            }
        }
        
        this.logger.log(action.logLevel, String.format("Mixin failed applying %s -> %s: %s %s", mixin, targetClassName,
                th.getClass().getName(), th.getMessage()), th);
        
        this.errorState = false;

        if (action == ErrorAction.ERROR) {
            throw new MixinApplyError(String.format("Mixin [%s] from phase [%s] in config [%s] FAILED", mixin, mixin.getPhase(),
                    config), th);
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
