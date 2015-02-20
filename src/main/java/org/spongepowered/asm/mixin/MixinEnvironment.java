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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.launch.MixinBootstrap;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;


/**
 * Env interaction for mixins
 */
public class MixinEnvironment {
    
    public static enum Side {
        /**
         * The environment was unable to determine current side
         */
        UNKNOWN {
            @Override
            protected boolean detect() {
                return false;
            }
        },
        
        /**
         * Client-side environment 
         */
        CLIENT {
            @Override
            protected boolean detect() {
                String sideName = this.getSideName();
                return "CLIENT".equals(sideName);
            }
        },
        
        /**
         * (Dedicated) Server-side environment 
         */
        SERVER {
            @Override
            protected boolean detect() {
                String sideName = this.getSideName();
                return "SERVER".equals(sideName) || "DEDICATEDSERVER".equals(sideName);
            }
        };
        
        protected abstract boolean detect();

        protected final String getSideName() {
            String name = this.getSideName("net.minecraftforge.fml.relauncher.FMLLaunchHandler", "side");
            if (name != null) {
                return name;
            }
            
            name = this.getSideName("cpw.mods.fml.relauncher.FMLLaunchHandler", "side");
            if (name != null) {
                return name;
            }
            
            name = this.getSideName("com.mumfrey.liteloader.core.LiteLoader", "getEnvironmentType");
            if (name != null) {
                return name;
            }
            
            return "UNKNOWN";
        }

        private String getSideName(String className, String methodName) {
            try {
                Class<?> clazz = Class.forName(className, false, Launch.classLoader);
                Method method = clazz.getDeclaredMethod(methodName);
                return ((Enum<?>)method.invoke(null)).name();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private static final String CONFIGS_KEY = "mixin.configs";
    private static final String TRANSFORMER_KEY = "mixin.transformer";
    
    private static MixinEnvironment env;
    
    private Side side;
    
    private MixinEnvironment() {
        // Sanity check
        Object version = Launch.blackboard.get(MixinBootstrap.INIT_KEY);
        if (version == null || !MixinBootstrap.VERSION.equals(version)) {
            throw new RuntimeException("Environment conflict, mismatched versions or you didn't call MixinBootstrap.init()");
        }
        
        // Also sanity check
        if (this.getClass().getClassLoader() != Launch.class.getClassLoader()) {
            throw new RuntimeException("Attempted to init the mixin environment in the wrong classloader");
        }
    }
    
    /**
     * Get mixin configurations from the blackboard
     * 
     * @return list of registered mixin configs
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
     * 
     * @param config Name of configuration resource to add
     * @return fluent interface
     */
    public MixinEnvironment addConfiguration(String config) {
        List<String> configs = this.getMixinConfigs();
        if (!configs.contains(config)) {
            configs.add(config);
        }
        return this;
    }

    public Object getActiveTransformer() {
        return Launch.blackboard.get(MixinEnvironment.TRANSFORMER_KEY);
    }

    public void setActiveTransformer(IClassTransformer transformer) {
        Launch.blackboard.put(MixinEnvironment.TRANSFORMER_KEY, transformer);        
    }
    
    public MixinEnvironment setSide(Side side) {
        if (side != null && this.getSide() == Side.UNKNOWN && side != Side.UNKNOWN) {
            this.side = side;
        }
        return this;
    }
    
    public Side getSide() {
        if (this.side == null) {
            for (Side side : Side.values()) {
                if (side.detect()) {
                    this.side = side;
                    break;
                }
            }
        }
        
        return this.side != null ? this.side : Side.UNKNOWN;
    }

    public static MixinEnvironment getCurrentEnvironment() {
        if (MixinEnvironment.env == null) {
            MixinEnvironment.env = new MixinEnvironment();
        }
        
        return MixinEnvironment.env;
    }
}
