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
package org.spongepowered.asm.util.asm;

import java.util.List;

import org.objectweb.asm.Type;

/**
 * Interface for annotation handle since main classes may need to read info from
 * mirror annotations in the AP
 */
public interface IAnnotationHandle {

    /**
     * Get the annotation descriptor
     */
    public abstract String getDesc();
    
    /**
     * Retrieve an annotation key as a list of annotation handles
     * 
     * @param key key to fetch
     * @return list of annotations
     */
    public abstract List<IAnnotationHandle> getAnnotationList(String key);
    
    /**
     * Retrieve an annotation key as a list of annotation Types
     * 
     * @param key key to fetch
     * @return list of types
     */
    public abstract List<Type> getTypeList(String key);
    
    /**
     * Get an annotation value as an annotation handle
     * 
     * @param key key to search for in the value map
     * @return value or <tt>null</tt> if not set
     */
    public abstract IAnnotationHandle getAnnotation(String key);
    
    /**
     * Get a value with the specified key from this annotation, return the
     * specified default value if the key is not set or is not present
     * 
     * @param key key
     * @param defaultValue value to return if the key is not set or not present
     * @param <T> duck type
     * @return value or default if not set
     */
    public abstract <T> T getValue(String key, T defaultValue);

    /**
     * Get the annotation value or return null if not present or not set
     * 
     * @param <T> duck type
     * @return value or null if not present or not set
     */
    public abstract <T> T getValue();
    
    /**
     * Get the annotation value with the specified key or return null if not
     * present or not set
     * 
     * @param key key to fetch
     * @param <T> duck type
     * @return value or null if not present or not set
     */
    public abstract <T> T getValue(String key);

    /**
     * Retrieve the annotation value with the specified key as a list with
     * values of the specified type. Returns an empty list if the value is not
     * present or not set.
     * 
     * @param key key to fetch
     * @param <T> list element duck type
     * @return list of values
     */
    public abstract <T> List<T> getList(String key);

}
