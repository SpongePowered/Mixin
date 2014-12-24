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
package org.spongepowered.asm.mixin;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;


/**
 * Env interaction for mixins
 */
public class MixinEnvironment {

    public static final String CONFIGS_KEY = "mixin.configs";
    public static final String TRANSFORMER_KEY = "mixin.transformer";
    public static final String MIXIN_TRANSFORMER_PACKAGE = "org.spongepowered.asm.mixin.transformer.";
    public static final String MIXIN_TRANSFORMER_CLASS = MixinEnvironment.MIXIN_TRANSFORMER_PACKAGE + "MixinTransformer";

    private static MixinEnvironment env;
    
    private MixinEnvironment() {
        // Sanity check
        if (this.getClass().getClassLoader() != Launch.class.getClassLoader()) {
            throw new RuntimeException("Attempted to init the mixin environment in the wrong classloader");
        }
        
        Launch.classLoader.addClassLoaderExclusion(MixinEnvironment.MIXIN_TRANSFORMER_PACKAGE);
    }
    
    /**
     * Get mixin configurations from the blackboard
     */
    public List<String> getMixinConfigs() {
        @SuppressWarnings("unchecked")
        List<String> mixinConfigs = (List<String>) Launch.blackboard.get(MixinEnvironment.CONFIGS_KEY);
        if (mixinConfigs == null) {
            mixinConfigs = new ArrayList<String>();
            Launch.blackboard.put(MixinEnvironment.CONFIGS_KEY, mixinConfigs);
        }
        return mixinConfigs;
    }
    
    /**
     * Add a mixin configuration to the blackboard
     * @return 
     */
    public MixinEnvironment addConfiguration(String config) {
        this.getMixinConfigs().add(config);
        return this;
    }

    public Object getActiveTransformer() {
        return Launch.blackboard.get(MixinEnvironment.TRANSFORMER_KEY);
    }

    public void setActiveTransformer(IClassTransformer transformer) {
        Launch.blackboard.put(MixinEnvironment.TRANSFORMER_KEY, this);        
    }

    public static MixinEnvironment getCurrentEnvironment() {
        if (MixinEnvironment.env == null) {
            MixinEnvironment.env = new MixinEnvironment();
        }
        
        return MixinEnvironment.env;
    }
}
