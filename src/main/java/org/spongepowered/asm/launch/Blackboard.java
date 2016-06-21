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

import net.minecraft.launchwrapper.Launch;

/**
 * Abstractions for working with {@link Launch#blackboard}
 */
public final class Blackboard {

    /**
     * Blackboard keys
     */
    public static final class Keys {

        public static final String TWEAKCLASSES                 = "TweakClasses";
        public static final String TWEAKS                       = "Tweaks";
        
        public static final String INIT                         = "mixin.initialised";
        public static final String AGENTS                       = "mixin.agents";
        public static final String CONFIGS                      = "mixin.configs";
        public static final String TRANSFORMER                  = "mixin.transformer";
        
        public static final String FML_LOAD_CORE_MOD            = "mixin.launch.fml.loadcoremodmethod";
        public static final String FML_GET_REPARSEABLE_COREMODS = "mixin.launch.fml.reparseablecoremodsmethod";
        public static final String FML_CORE_MOD_MANAGER         = "mixin.launch.fml.coremodmanagerclass";
        public static final String FML_GET_IGNORED_MODS         = "mixin.launch.fml.ignoredmodsmethod";

        private Keys() {}

    }
    
    private Blackboard() {}
    
    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        return (T)Launch.blackboard.get(key);
    }

    public static void put(String key, Object value) {
        Launch.blackboard.put(key, value);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T get(String key, T defaultValue) {
        Object value = Launch.blackboard.get(key);
        return value != null ? (T)value : defaultValue;
    }
    
    public static String getString(String key, String defaultValue) {
        Object value = Launch.blackboard.get(key);
        return value != null ? value.toString() : defaultValue;
    }

}
