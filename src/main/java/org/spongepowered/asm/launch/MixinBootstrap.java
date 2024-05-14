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
package org.spongepowered.asm.launch;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.launch.platform.CommandLineOptions;
import org.spongepowered.asm.launch.platform.MixinPlatformManager;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.throwables.MixinError;
import org.spongepowered.asm.service.IMixinInternal;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;

/**
 * Bootstraps the mixin subsystem. This class acts as a bridge between the mixin
 * subsystem and the tweaker or coremod which is boostrapping it. Without this
 * class, a coremod may cause classload of MixinEnvironment in the
 * LaunchClassLoader before we have a chance to exclude it. By placing the main
 * bootstrap logic here we avoid the need for consumers to add the classloader
 * exclusion themselves.
 * 
 * <p>In development, where (because of the classloader environment at dev time)
 * it is safe to let a coremod initialise the mixin subsystem, we can perform
 * initialisation all in one go using the {@link #init} method and everything is
 * fine. However in production the tweaker must be used and the situation is a
 * little more delicate.</p>
 * 
 * <p>In an ideal world, the mixin tweaker would initialise the environment in
 * its constructor and that would be the end of the story. However we also need
 * to register the additional tweaker for environment to detect the transition
 * from pre-init to default and we cannot do this within the tweaker constructor
 * without triggering a ConcurrentModificationException in the tweaker list. To
 * work around this we register the secondary tweaker from within the mixin 
 * tweaker's acceptOptions method instead.</p>
 */
public abstract class MixinBootstrap {

    /**
     * Subsystem version
     */
    public static final String VERSION = "0.8.7";
    
    /**
     * Transformer factory 
     */
    private static final String MIXIN_TRANSFORMER_FACTORY_CLASS = "org.spongepowered.asm.mixin.transformer.MixinTransformer$Factory";
    
    
    // These are Klass local, with luck this shouldn't be a problem
    private static boolean initialised = false;
    private static boolean initState = true;
    
    /**
     * Log all the things
     */
    private static ILogger logger;
    
    // Static initialiser, run boot services as early as possible
    static {
        MixinService.boot();
        MixinService.getService().prepare();
        MixinBootstrap.logger = MixinService.getService().getLogger("mixin");
    }

    /**
     * Platform manager instance
     */
    private static MixinPlatformManager platform;

    private MixinBootstrap() {}
    
    /**
     * @deprecated use <tt>MixinService.getService().beginPhase()</tt> instead
     */
    @Deprecated
    public static void addProxy() {
        MixinService.getService().beginPhase();
    }

    /**
     * Get the platform manager
     */
    public static MixinPlatformManager getPlatform() {
        if (MixinBootstrap.platform == null) {
            Object globalPlatformManager = GlobalProperties.<Object>get(GlobalProperties.Keys.PLATFORM_MANAGER);
            if (globalPlatformManager instanceof MixinPlatformManager) {
                MixinBootstrap.platform = (MixinPlatformManager)globalPlatformManager;
            } else {
                MixinBootstrap.platform = new MixinPlatformManager();
                GlobalProperties.put(GlobalProperties.Keys.PLATFORM_MANAGER, MixinBootstrap.platform);
                MixinBootstrap.platform.init();
            }
        }
        return MixinBootstrap.platform;
    }

    /**
     * Initialise the mixin subsystem
     */
    public static void init() {
        if (!MixinBootstrap.start()) {
            return;
        }

        MixinBootstrap.doInit(CommandLineOptions.defaultArgs());
    }

    /**
     * Phase 1 of mixin initialisation
     */
    static boolean start() {
        if (MixinBootstrap.isSubsystemRegistered()) {
            if (!MixinBootstrap.checkSubsystemVersion()) {
                throw new MixinInitialisationError("Mixin subsystem version " + MixinBootstrap.getActiveSubsystemVersion()
                        + " was already initialised. Cannot bootstrap version " + MixinBootstrap.VERSION);
            }
            return false;
        }
            
        MixinBootstrap.registerSubsystem(MixinBootstrap.VERSION);
        MixinBootstrap.offerInternals();
        
        if (!MixinBootstrap.initialised) {
            MixinBootstrap.initialised = true;
            
            Phase initialPhase = MixinService.getService().getInitialPhase();
            if (initialPhase == Phase.DEFAULT) {
                MixinBootstrap.logger.error("Initialising mixin subsystem after game pre-init phase! Some mixins may be skipped.");
                MixinEnvironment.init(initialPhase);
                MixinBootstrap.getPlatform().prepare(CommandLineOptions.defaultArgs());
                MixinBootstrap.initState = false;
            } else {
                MixinEnvironment.init(initialPhase);
            }
            
            MixinService.getService().beginPhase();
        }
        
        MixinBootstrap.getPlatform();
        
        return true;
    }

    @Deprecated
    static void doInit(List<String> args) {
        MixinBootstrap.doInit(CommandLineOptions.ofArgs(args));
    }
    
    /**
     * Phase 2 of mixin initialisation, initialise the phases
     */
    static void doInit(CommandLineOptions args) {
        if (!MixinBootstrap.initialised) {
            if (MixinBootstrap.isSubsystemRegistered()) {
                MixinBootstrap.logger.warn("Multiple Mixin containers present, init suppressed for {}", MixinBootstrap.VERSION);
                return;
            }
            
            throw new IllegalStateException("MixinBootstrap.doInit() called before MixinBootstrap.start()");
        }

        MixinBootstrap.getPlatform().getPhaseProviderClasses();
//        for (String platformProviderClass : MixinBootstrap.getPlatform().getPhaseProviderClasses()) {
//            System.err.printf("Registering %s\n", platformProviderClass);
//            MixinEnvironment.registerPhaseProvider(platformProviderClass);
//        }

        if (MixinBootstrap.initState) {
            MixinBootstrap.getPlatform().prepare(args);
            MixinService.getService().init();
        }
    }

    static void inject() {
        MixinBootstrap.getPlatform().inject();
    }

    private static boolean isSubsystemRegistered() {
        return GlobalProperties.<Object>get(GlobalProperties.Keys.INIT) != null;
    }

    private static boolean checkSubsystemVersion() {
        return MixinBootstrap.VERSION.equals(MixinBootstrap.getActiveSubsystemVersion());
    }

    private static Object getActiveSubsystemVersion() {
        Object version = GlobalProperties.get(GlobalProperties.Keys.INIT);
        return version != null ? version : "";
    }

    private static void registerSubsystem(String version) {
        GlobalProperties.put(GlobalProperties.Keys.INIT, version);
    }

    private static void offerInternals() {
        IMixinService service = MixinService.getService();

        try {
            for (IMixinInternal internal : MixinBootstrap.getInternals()) {
                service.offer(internal);
            }
        } catch (AbstractMethodError ex) {
            // outdated service
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<IMixinInternal> getInternals() throws MixinError {
        List<IMixinInternal> internals = new ArrayList<IMixinInternal>();
        try {
            Class<IMixinInternal> clTransformerFactory = (Class<IMixinInternal>)Class.forName(MixinBootstrap.MIXIN_TRANSFORMER_FACTORY_CLASS);
            Constructor<IMixinInternal> ctor = clTransformerFactory.getDeclaredConstructor();
            ctor.setAccessible(true);
            internals.add(ctor.newInstance());
        } catch (ReflectiveOperationException ex) {
            throw new MixinError(ex);
        }
        return internals;
    }

}
