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

import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;

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
 *      and type obfuscations, and {@link MappingMethod} for methods.
 */
public class ObfuscationData<T> implements Iterable<ObfuscationType> {
    
    /**
     * Data points stored in this struct, entries are stored by type
     */
    private final Map<ObfuscationType, T> data = new HashMap<ObfuscationType, T>();
    
    /**
     * Default obfuscation value to return when an obfuscation entry is
     * requested but not present in the map
     */
    private final T defaultValue;
    
    public ObfuscationData() {
        this(null);
    }
    
    public ObfuscationData(T defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    /**
     * Add an entry to the map, overwrites any previous entries. Since this
     * method name poorly communicates its purpose, it is deprecated in favour
     * of {@link #put}.  
     * 
     * @param type obfuscation type
     * @param value new entry
     * 
     * @deprecated Use {@link #put} instead
     */
    @Deprecated
    public void add(ObfuscationType type, T value) {
        this.put(type, value);
    }

    /**
     * Put an entry into this map, replaces any existing entries.
     * 
     * @param type obfuscation type
     * @param value entry
     */
    public void put(ObfuscationType type, T value) {
        this.data.put(type, value);
    }

    /**
     * Returns true if this store contains no entries
     */
    public boolean isEmpty() {
        return this.data.isEmpty();
    }
    
    /**
     * Get the obfuscation entry for the specified obfuscation type, returns the
     * default (if present) if no entry is found for the specified type
     * 
     * @param type obfuscation type
     * @return obfuscation entry or default value if absent
     */
    public T get(ObfuscationType type) {
        T value = this.data.get(type);
        return value != null ? value : this.defaultValue;
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<ObfuscationType> iterator() {
        return this.data.keySet().iterator();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("ObfuscationData[%s,DEFAULT=%s]", this.listValues(), this.defaultValue);
    }

    /**
     * Get a string representation of the values in this data set
     * 
     * @return string
     */
    public String values() {
        return "[" + this.listValues() + "]";
    }

    private String listValues() {
        StringBuilder sb = new StringBuilder();
        boolean delim = false;
        for (ObfuscationType type : this.data.keySet()) {
            if (delim) {
                sb.append(',');
            }
            sb.append(type.getKey()).append('=').append(this.data.get(type));
            delim = true;
        }
        return sb.toString();
    }
    
}
