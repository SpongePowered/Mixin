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

import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

public abstract class MixinBootstrap {

    public static final String VERSION = "0.3";
    public static final String INIT_KEY = "mixin.initialised";
    
    private static final String MIXIN_PACKAGE = "org.spongepowered.asm.mixin.";
    private static final String ASM_PACKAGE = "org.objectweb.asm.";
    private static final String TRANSFORMER_CLASS = MixinBootstrap.MIXIN_PACKAGE + "transformer.MixinTransformer";
    
    static {
        Launch.classLoader.addClassLoaderExclusion(MixinBootstrap.ASM_PACKAGE);
        
        @SuppressWarnings("unused")
        IMixinConfigPlugin forceClassLoad = null;
        
        Launch.classLoader.addClassLoaderExclusion(MixinBootstrap.MIXIN_PACKAGE);
    }

    private MixinBootstrap() {}

    public static void init() {
        Object registeredVersion = Launch.blackboard.get(MixinBootstrap.INIT_KEY);
        if (registeredVersion != null && !registeredVersion.equals(MixinBootstrap.VERSION)) {
            throw new MixinInitialisationError("Mixin subsystem version " + registeredVersion
                    + " was already initialised. Cannot bootstrap version " + MixinBootstrap.VERSION);
        }

        Launch.blackboard.put(MixinBootstrap.INIT_KEY, MixinBootstrap.VERSION);
        MixinEnvironment.getCurrentEnvironment();

        Launch.classLoader.registerTransformer(MixinBootstrap.TRANSFORMER_CLASS);
        
        @SuppressWarnings("unchecked")
        List<String> tweakClasses = (List<String>)Launch.blackboard.get("TweakClasses");
        if (tweakClasses != null) {
            tweakClasses.add(MixinEnvironment.class.getName() + "$EnvironmentStateTweaker");
        }
    }
}
