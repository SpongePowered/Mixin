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

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;
import org.spongepowered.asm.service.MixinService;

/**
 * Access to underlying global property service provided by the current
 * environment
 */
public final class GlobalProperties {

    /**
     * Global property keys
     */
    public static final class Keys {

        public static final Keys INIT                         = Keys.of("mixin.initialised");
        public static final Keys AGENTS                       = Keys.of("mixin.agents");
        public static final Keys CONFIGS                      = Keys.of("mixin.configs");
        public static final Keys PLATFORM_MANAGER             = Keys.of("mixin.platform");
        
        public static final Keys FML_LOAD_CORE_MOD            = Keys.of("mixin.launch.fml.loadcoremodmethod");
        public static final Keys FML_GET_REPARSEABLE_COREMODS = Keys.of("mixin.launch.fml.reparseablecoremodsmethod");
        public static final Keys FML_CORE_MOD_MANAGER         = Keys.of("mixin.launch.fml.coremodmanagerclass");
        public static final Keys FML_GET_IGNORED_MODS         = Keys.of("mixin.launch.fml.ignoredmodsmethod");
        
        private static Map<String, Keys> keys;
        
        private final String name;
        
        private IPropertyKey key;

        private Keys(String name) {
            this.name = name;
        }
        
        IPropertyKey resolve(IGlobalPropertyService service) {
            if (this.key != null) {
                return this.key;
            }
            if (service == null) {
                return null;
            }
            
            return this.key = service.resolveKey(this.name);
        }
        
        @Override
        public String toString() {
            return this.name;
        }

        /**
         * Get or create a new global property key
         * 
         * @param name name of key to get or create
         * @return new or existing key
         */
        public static Keys of(String name) {
            if (Keys.keys == null) {
                Keys.keys = new HashMap<String, Keys>();                
            }
            
            Keys key = Keys.keys.get(name);
            if (key == null) {
                key = new Keys(name);
                Keys.keys.put(name, key);
            }
            return key;
        }
        
    }
    
    private static IGlobalPropertyService service;
    
    private GlobalProperties() {}
    
    private static IGlobalPropertyService getService() {
        if (GlobalProperties.service == null) {
            GlobalProperties.service = MixinService.getGlobalPropertyService();
        }
        return GlobalProperties.service;
    }
    
    /**
     * Get a value from the blackboard and duck-type it to the specified type
     * 
     * @param key blackboard key
     * @param <T> duck type
     * @return value
     */
    public static <T> T get(Keys key) {
        IGlobalPropertyService service = GlobalProperties.getService();
        return service.<T>getProperty(key.resolve(service));
    }

    /**
     * Put the specified value onto the blackboard
     * 
     * @param key blackboard key
     * @param value new value
     */
    public static void put(Keys key, Object value) {
        IGlobalPropertyService service = GlobalProperties.getService();
        service.setProperty(key.resolve(service), value);
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
    public static <T> T get(Keys key, T defaultValue) {
        IGlobalPropertyService service = GlobalProperties.getService();
        return service.getProperty(key.resolve(service), defaultValue);
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
    public static String getString(Keys key, String defaultValue) {
        IGlobalPropertyService service = GlobalProperties.getService();
        return service.getPropertyString(key.resolve(service), defaultValue);
    }

}
