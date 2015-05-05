/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered.org <http://www.spongepowered.org>
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

import java.util.List;

import net.minecraft.launchwrapper.Launch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;

/**
 * Bootstaps the mixin subsystem. This class acts as a bridge between the mixin
 * subsystem and the tweaker or coremod which is boostrapping it. Without this
 * class, a coremod may cause classload of MixinEnvironment in the
 * LaunchClassLoader before we have a chance to exclude it. By placing the main
 * bootstap logic here we avoid the need for consumers to add the classloader
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
 * witout triggering a ConcurrentModificationException in the tweaker list. To
 * work around this we register the secondary tweaker from within the mixin 
 * tweaker's acceptOptions method instead.</p>
 */
public abstract class MixinBootstrap {

    /**
     * Subsystem version
     */
    public static final String VERSION = "0.4";
    
    /**
     * Blackboard key where the subsystem version will be stored to indicate
     * successful bootstrap
     */
    public static final String INIT_KEY = "mixin.initialised";
    
    // Consts
    private static final String LAUNCH_PACKAGE = "org.spongepowered.asm.launch.";
    private static final String MIXIN_PACKAGE = "org.spongepowered.asm.mixin.";
    private static final String ASM_PACKAGE = "org.spongepowered.asm.lib.";
    private static final String TRANSFORMER_PROXY_CLASS = MixinBootstrap.MIXIN_PACKAGE + "transformer.MixinTransformer$Proxy";
    
    /**
     * Log all the things
     */
    private static final Logger logger = LogManager.getLogger("mixin");
    
    // These are Klass local, with luck this shouldn't be a problem
    private static boolean initialised = false;
    private static boolean injectStateTweaker = true;
    
    // Static initialiser, add classloader exclusions as early as possible
    static {
        // The important ones
        Launch.classLoader.addClassLoaderExclusion(MixinBootstrap.ASM_PACKAGE);
        Launch.classLoader.addClassLoaderExclusion(MixinBootstrap.MIXIN_PACKAGE);
        
        // Only needed in dev, in production this would be handled by the tweaker
        Launch.classLoader.addClassLoaderExclusion(MixinBootstrap.LAUNCH_PACKAGE);
    }

    private MixinBootstrap() {}
    
    /**
     * Register a new proxy transformer
     */
    public static void addProxy() {
        Launch.classLoader.registerTransformer(MixinBootstrap.TRANSFORMER_PROXY_CLASS);
    }

    /**
     * Initialise the mixin subsystem
     */
    public static void init() {
        if (!MixinBootstrap.preInit()) {
            return;
        }

        MixinBootstrap.register();
    }

    /**
     * Phase 1 of mixin initialisation
     */
    static boolean preInit() {
        Object registeredVersion = Launch.blackboard.get(MixinBootstrap.INIT_KEY);
        if (registeredVersion != null) {
            if (!registeredVersion.equals(MixinBootstrap.VERSION)) {
                throw new MixinInitialisationError("Mixin subsystem version " + registeredVersion
                        + " was already initialised. Cannot bootstrap version " + MixinBootstrap.VERSION);
            }
            
            return false;
        }

        Launch.blackboard.put(MixinBootstrap.INIT_KEY, MixinBootstrap.VERSION);
        
        if (!MixinBootstrap.initialised) {
            MixinBootstrap.initialised = true;

            if (MixinBootstrap.findInStackTrace(Launch.class.getName(), "launch") > 132) {
                MixinBootstrap.logger.error("Initialising mixin subsystem after game pre-init phase! Some mixins may be skipped.");
                MixinEnvironment.init(Phase.DEFAULT);
                MixinBootstrap.injectStateTweaker = false;
            } else {
                MixinEnvironment.init(Phase.PREINIT);
            }
            
            MixinBootstrap.addProxy();
        }
        
        return true;
    }

    /**
     * Phase 2 of mixin initialisation, register the state tweaker
     */
    static void register() {
        if (!MixinBootstrap.initialised) {
            throw new IllegalStateException("MixinBootstrap.register() called before MixinBootstrap.preInit()");
        }

        if (MixinBootstrap.injectStateTweaker) {
            if (MixinBootstrap.findInStackTrace(Launch.class.getName(), "launch") < 4) {
                MixinBootstrap.logger.warn("MixinBootstrap.register() called during a tweak constructor. Expect CoModificationException in 5.. 4..");
            }
            
            @SuppressWarnings("unchecked")
            List<String> tweakClasses = (List<String>)Launch.blackboard.get("TweakClasses");
            if (tweakClasses != null) {
                tweakClasses.add(MixinEnvironment.class.getName() + "$EnvironmentStateTweaker");
            }
        }
    }

    private static int findInStackTrace(String className, String methodName) {
        Thread currentThread = Thread.currentThread();
        
        if (!"main".equals(currentThread.getName())) {
            return 0;
        }
        
        StackTraceElement[] stackTrace = currentThread.getStackTrace();
        for (StackTraceElement s : stackTrace) {
            if (className.equals(s.getClassName()) && methodName.equals(s.getMethodName())) {
                return s.getLineNumber();
            }
        }
        
        return 0;
    }
}
