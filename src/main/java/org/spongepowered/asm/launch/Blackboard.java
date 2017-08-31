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

import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;

/**
 * Abstractions for working with the blackboard
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
        public static final String PLATFORM_MANAGER             = "mixin.platform";
        
        public static final String FML_LOAD_CORE_MOD            = "mixin.launch.fml.loadcoremodmethod";
        public static final String FML_GET_REPARSEABLE_COREMODS = "mixin.launch.fml.reparseablecoremodsmethod";
        public static final String FML_CORE_MOD_MANAGER         = "mixin.launch.fml.coremodmanagerclass";
        public static final String FML_GET_IGNORED_MODS         = "mixin.launch.fml.ignoredmodsmethod";

        private Keys() {}

    }
    
    private static IMixinService service;
    
    private Blackboard() {}
    
    private static IMixinService getService() {
        if (Blackboard.service == null) {
            Blackboard.service = MixinService.getService();
        }
        return Blackboard.service;
    }
    
    /**
     * Get a value from the blackboard and duck-type it to the specified type
     * 
     * @param key blackboard key
     * @param <T> duck type
     * @return value
     */
    public static <T> T get(String key) {
        return Blackboard.getService().<T>getGlobalProperty(key);
    }

    /**
     * Put the specified value onto the blackboard
     * 
     * @param key blackboard key
     * @param value new value
     */
    public static void put(String key, Object value) {
        Blackboard.getService().setGlobalProperty(key, value);
    }
    
    /**
     * Get the value from the blackboard but return <tt>defaultValue</tt> if the
     * specified key is not set.
     * 
     * @param key blackboard key
     * @param defaultValue value to return if the key is not set or is null
     * @param <T> duck type
     * @return value from blackboard or default value
     */
    public static <T> T get(String key, T defaultValue) {
        return Blackboard.getService().getGlobalProperty(key, defaultValue);
    }
    
    /**
     * Get a string from the blackboard, returns default value if not set or
     * null.
     * 
     * @param key blackboard key
     * @param defaultValue default value to return if the specified key is not
     *      set or is null
     * @return value from blackboard or default
     */
    public static String getString(String key, String defaultValue) {
        return Blackboard.getService().getGlobalPropertyString(key, defaultValue);
    }

}
