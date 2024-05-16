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
package org.spongepowered.asm.service.modlauncher;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.TypesafeMap;

/**
 * Global property service backed by ModLauncher blackboard
 */
public class Blackboard implements IGlobalPropertyService {
    
    /**
     * Type safe property key
     * 
     * @param <V> value type
     */
    class Key<V> implements IPropertyKey {
        
        final TypesafeMap.Key<V> key;
        
        public Key(TypesafeMap owner, String name, Class<V> clazz) {
            this.key = TypesafeMap.Key.<V>getOrCreate(owner, name, clazz);
        }
        
    }
    
    private final Map<String, IPropertyKey> keys = new HashMap<String, IPropertyKey>(); 
    
    private final TypesafeMap blackboard;

    public Blackboard() {
        this.blackboard = Launcher.INSTANCE.blackboard();
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IGlobalPropertyService#resolveKey(
     *      java.lang.String)
     */
    @Override
    public IPropertyKey resolveKey(String name) {
        return this.keys.computeIfAbsent(name, key -> new Key<Object>(this.blackboard, key, Object.class));
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IGlobalPropertyService#getProperty(
     *      org.spongepowered.asm.service.IPropertyKey)
     */
    @Override
    public <T> T getProperty(IPropertyKey key) {
        return this.getProperty(key, null);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IGlobalPropertyService#setProperty(
     *      org.spongepowered.asm.service.IPropertyKey, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void setProperty(IPropertyKey key, final Object value) {
        this.blackboard.computeIfAbsent(((Key<Object>)key).key, k -> value);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IGlobalPropertyService
     *      #getPropertyString(org.spongepowered.asm.service.IPropertyKey,
     *      java.lang.String)
     */
    @Override
    public String getPropertyString(IPropertyKey key, String defaultValue) {
        return this.getProperty(key, defaultValue);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IGlobalPropertyService#getProperty(
     *      org.spongepowered.asm.service.IPropertyKey, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getProperty(IPropertyKey key, T defaultValue) {
        return this.blackboard.<T>get(((Key<T>)key).key).orElse(defaultValue);
    }

}
