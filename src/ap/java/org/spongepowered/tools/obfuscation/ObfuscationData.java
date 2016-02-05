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
package org.spongepowered.tools.obfuscation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.spongepowered.asm.obfuscation.SrgMethod;

/**
 * Return value struct for various obfuscation queries performed by the mixin
 * annotation processor.
 * 
 * <p>When obfuscation queries are performed by the AP, the returned data are
 * encapsulated in an <tt>ObfuscationData</tt> object which contains a mapping
 * of the different obfuscation types to the respective remapped value of the
 * original entry for that environment.</p>
 * 
 * <p>The returned data are iterable over the keys, consumers should call
 * {@link #get} to retrieve the remapped entries for each key.</p>
 * 
 * @param <T> Contained type of the data, usually {@link String} for field
 *      and type obfuscations, and {@link SrgMethod} for methods.
 */
public class ObfuscationData<T> implements Iterable<ObfuscationType> {
    
    private final Map<ObfuscationType, T> data = new HashMap<ObfuscationType, T>();
    
    private final T defaultValue;
    
    public ObfuscationData() {
        this(null);
    }
    
    public ObfuscationData(T defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    public void add(ObfuscationType type, T value) {
        this.data.put(type, value);
    }

    public boolean isEmpty() {
        return this.data.isEmpty();
    }
    
    public T get(ObfuscationType type) {
        T value = this.data.get(type);
        return value != null ? value : this.defaultValue;
    }

    @Override
    public Iterator<ObfuscationType> iterator() {
        return this.data.keySet().iterator();
    }
    
}
