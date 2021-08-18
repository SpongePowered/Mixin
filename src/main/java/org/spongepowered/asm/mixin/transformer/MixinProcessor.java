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

import java.text.DecimalFormat;
import java.util.*;

import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.logging.ILogger;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler.ErrorAction;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorDynamic;
import org.spongepowered.asm.mixin.throwables.ClassAlreadyLoadedException;
import org.spongepowered.asm.mixin.throwables.MixinApplyError;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.mixin.throwables.MixinPrepareError;
import org.spongepowered.asm.mixin.transformer.MixinConfig.IListener;
import org.spongepowered.asm.mixin.transformer.MixinCoprocessor.ProcessResult;
import org.spongepowered.asm.mixin.transformer.MixinInfo.Variant;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IHotSwap;
import org.spongepowered.asm.mixin.transformer.ext.extensions.ExtensionClassExporter;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.mixin.transformer.throwables.IllegalClassLoadError;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.mixin.transformer.throwables.MixinTransformerError;
import org.spongepowered.asm.mixin.transformer.throwables.ReEntrantTransformerError;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.asm.util.ReEntranceLock;
import org.spongepowered.asm.util.perf.Profiler;
import org.spongepowered.asm.util.perf.Profiler.Section;

/**
 * Heart of the Mixin pipeline 
 */
class MixinProcessor {

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
            ErrorAction onError(IMixinErrorHandler handler, String context, InvalidMixinException ex, IMixinInfo mixin, ErrorAction action) {
                try {
                    return handler.onPrepareError(mixin.getConfig(), ex, mixin, action);
                } catch (AbstractMethodError ame) {
                    // Catch if error handler is pre-0.5.4
                    return action;
                }
            }
            
            @Override
            protected String getContext(IMixinInfo mixin, String context) {
                return String.format("preparing %s in %s", mixin.getName(), context);
            }
        },
        /**
         * Error during application of a mixin to a target class
         */
        APPLY {
            @Override
            ErrorAction onError(IMixinErrorHandler handler, String context, InvalidMixinException ex, IMixinInfo mixin, ErrorAction action) {
                try {
                    return handler.onApplyError(context, ex, mixin, action);
                } catch (AbstractMethodError ame) {
                    // Catch if error handler is pre-0.5.4
                    return action;
                }
            }
            
            @Override
            protected String getContext(IMixinInfo mixin, String context) {
                return String.format("%s -> %s", mixin, context);
            }
        };
        
        /**
         * Human-readable name
         */
        private final String text;
        
        private ErrorPhase() {
            this.text = this.name().toLowerCase(Locale.ROOT);
        }
        
        abstract ErrorAction onError(IMixinErrorHandler handler, String context, InvalidMixinException ex, IMixinInfo mixin, ErrorAction action);

        protected abstract String getContext(IMixinInfo mixin, String context);

        public String getLogMessage(String context, InvalidMixinException ex, IMixinInfo mixin) {
            return String.format("Mixin %s failed %s: %s %s", this.text, this.getContext(mixin, context), ex.getClass().getName(), ex.getMessage());
        }

        public String getErrorMessage(IMixinInfo mixin, IMixinConfig config, Phase phase) {
            return String.format("Mixin [%s] from phase [%s] in config [%s] FAILED during %s", mixin, phase, config, this.name());
        }
        
    }

    /**
     * Log all the things
     */
    static final ILogger logger = MixinService.getService().getLogger("mixin");
    
    /**
     * Service 
     */
    private final IMixinService service = MixinService.getService();
    
    /**
     * All mixin configuration bundles
     */
    private final List<MixinConfig> configs = new ArrayList<MixinConfig>();
    
    /**
     * Uninitialised mixin configuration bundles 
     */
    private final List<MixinConfig> pendingConfigs = new ArrayList<MixinConfig>();
    
    /**
     * Re-entrance detector
     */
    private final ReEntranceLock lock;
    
    /**
     * Session ID, used as a check when parsing {@link MixinMerged} annotations
     * to prevent them being applied at compile time by people trying to
     * circumvent mixin application
     */
    private final String sessionId = UUID.randomUUID().toString();
    
    /**
     * Processor extensions
     */
    private final Extensions extensions;
    
    /**
     * Hot-Swap agent
     */
    private final IHotSwap hotSwapper;
    
    /**
     * Postprocessor for passthrough 
     */
    private final MixinCoprocessors coprocessors = new MixinCoprocessors();
    
    /**
     * Profiler 
     */
    private final Profiler profiler;
    
    /**
     * Audit trail (if available); 
     */
    private final IMixinAuditTrail auditTrail;

    /**
     * Current environment 
     */
    private MixinEnvironment currentEnvironment;

    /**
     * Logging level for verbose messages 
     */
    private Level verboseLoggingLevel = Level.DEBUG;

    /**
     * Handling an error state, do not process further mixins
     */
    private boolean errorState = false;
    
    /**
     * Number of classes transformed in the current phase
     */
    private int transformedCount = 0;

    /**
     * ctor 
     */
    MixinProcessor(MixinEnvironment environment, Extensions extensions, IHotSwap hotSwapper, MixinCoprocessorNestHost nestHostCoprocessor) {
        this.lock = this.service.getReEntranceLock();
        
        this.extensions = extensions;
        this.hotSwapper = hotSwapper;
        
        this.coprocessors.add(new MixinCoprocessorPassthrough());
        this.coprocessors.add(new MixinCoprocessorSyntheticInner());
        this.coprocessors.add(new MixinCoprocessorAccessor(this.sessionId));
        this.coprocessors.add(nestHostCoprocessor);
        
        this.profiler = MixinEnvironment.getProfiler();
        this.auditTrail = this.service.getAuditTrail();
    }

    /**
     * Force-load all classes targetted by mixins but not yet applied
     * 
     * @param environment current environment
     */
    public void audit(MixinEnvironment environment) {
        Set<String> unhandled = new HashSet<String>();
        
        for (MixinConfig config : this.configs) {
            unhandled.addAll(config.getUnhandledTargets());
        }

        ILogger auditLogger = MixinService.getService().getLogger("mixin.audit");

        for (String target : unhandled) {
            try {
                auditLogger.info("Force-loading class {}", target);
                this.service.getClassProvider().findClass(target, true);
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
        
        if (environment.getOption(Option.DEBUG_PROFILER)) {
            this.profiler.printSummary();
        }
    }

    synchronized boolean applyMixins(MixinEnvironment environment, String name, ClassNode targetClassNode) {
        if (name == null || this.errorState) {
            return false;
        }
        
        boolean locked = this.lock.push().check();
        Section mixinTimer = this.profiler.begin("mixin");

        if (locked) {
            for (MixinConfig config : this.pendingConfigs) {
                if (config.hasPendingMixinsFor(name)) {
                    ReEntrantTransformerError error = new ReEntrantTransformerError("Re-entrance error.");
                    MixinProcessor.logger.warn("Re-entrance detected during prepare phase, this will cause serious problems.", error);
                    throw error;
                }
            }
        } else {
            try {
                this.checkSelect(environment);
            } catch (Exception ex) {
                this.lock.pop();
                mixinTimer.end();
                throw new MixinException(ex);
            }
        }
        
        boolean transformed = false;
        
        try {
            ProcessResult result = this.coprocessors.process(name, targetClassNode);
            transformed |= result.isTransformed();
            
            if (result.isPassthrough()) {
                for (MixinCoprocessor coprocessor : this.coprocessors) {
                    transformed |= coprocessor.postProcess(name, targetClassNode);
                }
                if (this.auditTrail != null) {
                    this.auditTrail.onPostProcess(name);
                }
                this.extensions.export(environment, name, false, targetClassNode);
                return transformed;
            }

            MixinConfig packageOwnedByConfig = null;
            
            for (MixinConfig config : this.configs) {
                if (config.packageMatch(name)) {
                    int packageLen = packageOwnedByConfig != null ? packageOwnedByConfig.getMixinPackage().length() : 0;
                    if (config.getMixinPackage().length() > packageLen) {
                        packageOwnedByConfig = config;
                    }
                    continue;
                }
            }                

            if (packageOwnedByConfig != null) {
                // AMS - Temp passthrough for injection points and dynamic selectors. Moving to service in 0.9
                ClassInfo targetInfo = ClassInfo.fromClassNode(targetClassNode);
                if (targetInfo.hasSuperClass(InjectionPoint.class) || targetInfo.hasSuperClass(ITargetSelectorDynamic.class)) {
                    return transformed;
                }
                
                throw new IllegalClassLoadError(this.getInvalidClassError(name, targetClassNode, packageOwnedByConfig));
            }

            SortedSet<MixinInfo> mixins = null;
            for (MixinConfig config : this.configs) {
                if (config.hasMixinsFor(name)) {
                    if (mixins == null) {
                        mixins = new TreeSet<MixinInfo>();
                    }
                    
                    // Get and sort mixins for the class
                    mixins.addAll(config.getMixinsFor(name));
                }
            }
            
            if (mixins != null) {
                // Re-entrance is "safe" as long as we don't need to apply any mixins, if there are mixins then we need to panic now
                if (locked) {
                    ReEntrantTransformerError error = new ReEntrantTransformerError("Re-entrance error.");
                    MixinProcessor.logger.warn("Re-entrance detected, this will cause serious problems.", error);
                    throw error;
                }

                if (this.hotSwapper != null) {
                    this.hotSwapper.registerTargetClass(name, targetClassNode);
                }

                try {
                    TargetClassContext context = new TargetClassContext(environment, this.extensions, this.sessionId, name, targetClassNode, mixins);
                    context.applyMixins();
                    
                    transformed |= this.coprocessors.postProcess(name, targetClassNode);

                    if (context.isExported()) {
                        this.extensions.export(environment, context.getClassName(), context.isExportForced(), context.getClassNode());
                    }
                    
                    for (InvalidMixinException suppressed : context.getSuppressedExceptions()) {
                        this.handleMixinApplyError(context.getClassName(), suppressed, environment);
                    }

                    this.transformedCount++;
                    transformed = true;
                } catch (InvalidMixinException th) {
                    this.dumpClassOnFailure(name, targetClassNode, environment);
                    this.handleMixinApplyError(name, th, environment);
                }
            } else {
                // No mixins, but still need to run postProcess stage of coprocessors
                if (this.coprocessors.postProcess(name, targetClassNode)) {
                    transformed = true;
                    this.extensions.export(environment, name, false, targetClassNode);
                }
            }
        } catch (MixinTransformerError er) {
            throw er;
        } catch (Throwable th) {
            this.dumpClassOnFailure(name, targetClassNode, environment);
            throw new MixinTransformerError("An unexpected critical error was encountered", th);
        } finally {
            this.lock.pop();
            mixinTimer.end();
        }
        return transformed;
    }

    private String getInvalidClassError(String name, ClassNode targetClassNode, MixinConfig ownedByConfig) {
        if (ownedByConfig.getClasses().contains(name)) {
            return String.format("Illegal classload request for %s. Mixin is defined in %s and cannot be referenced directly", name, ownedByConfig);
        }

        AnnotationNode mixin = Annotations.getInvisible(targetClassNode, Mixin.class);
        if (mixin != null) {
            Variant variant = MixinInfo.getVariant(targetClassNode);
            if (variant == Variant.ACCESSOR) {
                return String.format("Illegal classload request for accessor mixin %s. The mixin is missing from %s which owns "
                        + "package %s* and the mixin has not been applied.", name, ownedByConfig, ownedByConfig.getMixinPackage());
            }
        }

        return String.format("%s is in a defined mixin package %s* owned by %s and cannot be referenced directly",
                name, ownedByConfig.getMixinPackage(), ownedByConfig);
    }
    
    /**
     * Update a mixin class with new bytecode.
     *
     * @param mixinClass Name of the mixin
     * @param classNode New class
     * @return List of classes that need to be updated
     */
    public List<String> reload(String mixinClass, ClassNode classNode) {
        if (this.lock.getDepth() > 0) {
            throw new MixinApplyError("Cannot reload mixin if re-entrant lock entered");
        }
        List<String> targets = new ArrayList<String>();
        for (MixinConfig config : this.configs) {
            targets.addAll(config.reloadMixin(mixinClass, classNode));
        }
        return targets;
    }

    private void checkSelect(MixinEnvironment environment) {
        if (this.currentEnvironment != environment) {
            this.select(environment);
            return;
        }
        
        int unvisitedCount = Mixins.getUnvisitedCount();
        if (unvisitedCount > 0 && this.transformedCount == 0) {
            this.select(environment);
        }
    }

    private void select(MixinEnvironment environment) {
        this.verboseLoggingLevel = (environment.getOption(Option.DEBUG_VERBOSE)) ? Level.INFO : Level.DEBUG;
        if (this.transformedCount > 0) {
            MixinProcessor.logger.log(this.verboseLoggingLevel, "Ending {}, applied {} mixins", this.currentEnvironment, this.transformedCount);
        }
        String action = this.currentEnvironment == environment ? "Checking for additional" : "Preparing";
        MixinProcessor.logger.log(this.verboseLoggingLevel, "{} mixins for {}", action, environment);
        
        this.profiler.setActive(true);
        this.profiler.mark(environment.getPhase().toString() + ":prepare");
        Section prepareTimer = this.profiler.begin("prepare");
        
        this.selectConfigs(environment);
        this.extensions.select(environment);
        int totalMixins = this.prepareConfigs(environment, this.extensions);
        this.currentEnvironment = environment;
        this.transformedCount = 0;

        prepareTimer.end();
        
        long elapsedMs = prepareTimer.getTime();
        double elapsedTime = prepareTimer.getSeconds();
        if (elapsedTime > 0.25D) {
            long loadTime = this.profiler.get("class.load").getTime();
            long transformTime = this.profiler.get("class.transform").getTime();
            long pluginTime = this.profiler.get("mixin.plugin").getTime();
            String elapsed = new DecimalFormat("###0.000").format(elapsedTime);
            String perMixinTime = new DecimalFormat("###0.0").format(((double)elapsedMs) / totalMixins);
            
            MixinProcessor.logger.log(this.verboseLoggingLevel, "Prepared {} mixins in {} sec ({}ms avg) ({}ms load, {}ms transform, {}ms plugin)",
                    totalMixins, elapsed, perMixinTime, loadTime, transformTime, pluginTime);
        }

        this.profiler.mark(environment.getPhase().toString() + ":apply");
        this.profiler.setActive(environment.getOption(Option.DEBUG_PROFILER));
    }

    /**
     * Add configurations from the supplied mixin environment to the configs set
     * 
     * @param environment Environment to query
     */
    private void selectConfigs(MixinEnvironment environment) {
        for (Iterator<Config> iter = Mixins.getConfigs().iterator(); iter.hasNext();) {
            Config handle = iter.next();
            try {
                MixinConfig config = handle.get();
                if (config.select(environment)) {
                    iter.remove();
                    MixinProcessor.logger.log(this.verboseLoggingLevel, "Selecting config {}", config);
                    config.onSelect();
                    this.pendingConfigs.add(config);
                }
            } catch (Exception ex) {
                MixinProcessor.logger.warn(String.format("Failed to select mixin config: %s", handle), ex);
            }
        }
        
        Collections.sort(this.pendingConfigs);
    }

    /**
     * Prepare mixin configs
     * 
     * @param environment Environment
     * @return total number of mixins initialised
     */
    private int prepareConfigs(MixinEnvironment environment, Extensions extensions) {
        int totalMixins = 0;
        
        final IHotSwap hotSwapper = this.hotSwapper;
        for (MixinConfig config : this.pendingConfigs) {
            for (MixinCoprocessor coprocessor : this.coprocessors) {
                config.addListener(coprocessor);
            }
            if (hotSwapper != null) {
                config.addListener(new IListener() {
                    @Override
                    public void onPrepare(MixinInfo mixin) {
                        hotSwapper.registerMixinClass(mixin.getClassName());
                    }
                    @Override
                    public void onInit(MixinInfo mixin) {
                    }
                });
            }
        }
        
        for (MixinConfig config : this.pendingConfigs) {
            try {
                MixinProcessor.logger.log(this.verboseLoggingLevel, "Preparing {} ({})", config, config.getDeclaredMixinCount());
                config.prepare(extensions);
                totalMixins += config.getMixinCount();
            } catch (InvalidMixinException ex) {
                this.handleMixinPrepareError(config, ex, environment);
            } catch (Exception ex) {
                String message = ex.getMessage();
                MixinProcessor.logger.error("Error encountered whilst initialising mixin config '" + config.getName() + "': " + message, ex);
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
            
            plugin.acceptTargets(config.getTargetsSet(), Collections.<String>unmodifiableSet(otherTargets));
        }

        for (MixinConfig config : this.pendingConfigs) {
            try {
                config.postInitialise(this.extensions);
            } catch (InvalidMixinException ex) {
                this.handleMixinPrepareError(config, ex, environment);
            } catch (Exception ex) {
                String message = ex.getMessage();
                MixinProcessor.logger.error("Error encountered during mixin config postInit step'" + config.getName() + "': " + message, ex);
            }
        }
        
        this.configs.addAll(this.pendingConfigs);
        Collections.sort(this.configs);
        this.pendingConfigs.clear();
        
        return totalMixins;
    }

    private void handleMixinPrepareError(MixinConfig config, InvalidMixinException ex, MixinEnvironment environment) throws MixinPrepareError {
        this.handleMixinError(config.getName(), ex, environment, ErrorPhase.PREPARE);
    }
    
    private void handleMixinApplyError(String targetClass, InvalidMixinException ex, MixinEnvironment environment) throws MixinApplyError {
        this.handleMixinError(targetClass, ex, environment, ErrorPhase.APPLY);
    }

    private void handleMixinError(String context, InvalidMixinException ex, MixinEnvironment environment, ErrorPhase errorPhase) throws Error {
        this.errorState = true;
        
        IMixinInfo mixin = ex.getMixin();
        
        if (mixin == null) {
            MixinProcessor.logger.error("InvalidMixinException has no mixin!", ex);
            throw ex;
        }
        
        IMixinConfig config = mixin.getConfig();
        Phase phase = mixin.getPhase();
        ErrorAction action = config.isRequired() ? ErrorAction.ERROR : ErrorAction.WARN;
        
        if (environment.getOption(Option.DEBUG_VERBOSE)) {
            new PrettyPrinter()
                .wrapTo(160)
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
                .add(ex, 8)
                .log(action.logLevel);
        }
    
        for (IMixinErrorHandler handler : this.getErrorHandlers(mixin.getPhase())) {
            ErrorAction newAction = errorPhase.onError(handler, context, ex, mixin, action);
            if (newAction != null) {
                action = newAction;
            }
        }
        
        MixinProcessor.logger.log(action.logLevel, errorPhase.getLogMessage(context, ex, mixin), ex);
        
        this.errorState = false;

        if (action == ErrorAction.ERROR) {
            throw new MixinApplyError(errorPhase.getErrorMessage(mixin, config, phase), ex);
        }
    }

    private List<IMixinErrorHandler> getErrorHandlers(Phase phase) {
        List<IMixinErrorHandler> handlers = new ArrayList<IMixinErrorHandler>();
        
        for (String handlerClassName : Mixins.getErrorHandlerClasses()) {
            try {
                MixinProcessor.logger.info("Instancing error handler class {}", handlerClassName);
                Class<?> handlerClass = this.service.getClassProvider().findClass(handlerClassName, true);
                IMixinErrorHandler handler = (IMixinErrorHandler)handlerClass.getDeclaredConstructor().newInstance();
                if (handler != null) {
                    handlers.add(handler);
                }
            } catch (Throwable th) {
                // skip bad handlers
            }
        }
        
        return handlers;
    }

    private void dumpClassOnFailure(String className, ClassNode classNode, MixinEnvironment env) {
        if (env.getOption(Option.DUMP_TARGET_ON_FAILURE)) {
            ExtensionClassExporter exporter = this.extensions.<ExtensionClassExporter>getExtension(ExtensionClassExporter.class);
            exporter.dumpClass(className.replace('.', '/') + ".target", classNode);
        }
    }

}
