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
package org.spongepowered.asm.service.mojang;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

import net.minecraft.launchwrapper.Launch;

/**
 * Global property service backed by LaunchWrapper blackboard
 */
public class Blackboard implements IGlobalPropertyService {
    
    /**
     * Property key
     */
    class Key implements IPropertyKey {
        
        private final String key;

        Key(String key) {
            this.key = key;
        }
        
        @Override
        public String toString() {
            return this.key;
        }
    }

    public Blackboard() {
        Launch.classLoader.hashCode();
    }
    
    @Override
    public IPropertyKey resolveKey(String name) {
        return new Key(name);
    }

    /**
     * Get a value from the blackboard and duck-type it to the specified type
     * 
     * @param key blackboard key
     * @param <T> duck type
     * @return value
     */
    @Override
    @SuppressWarnings("unchecked")
    public final <T> T getProperty(IPropertyKey key) {
        return (T)Launch.blackboard.get(key.toString());
    }

    /**
     * Put the specified value onto the blackboard
     * 
     * @param key blackboard key
     * @param value new value
     */
    @Override
    public final void setProperty(IPropertyKey key, Object value) {
        Launch.blackboard.put(key.toString(), value);
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
    @Override
    @SuppressWarnings("unchecked")
    public final <T> T getProperty(IPropertyKey key, T defaultValue) {
        Object value = Launch.blackboard.get(key.toString());
        return value != null ? (T)value : defaultValue;
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
    @Override
    public final String getPropertyString(IPropertyKey key, String defaultValue) {
        Object value = Launch.blackboard.get(key.toString());
        return value != null ? value.toString() : defaultValue;
    }

}
