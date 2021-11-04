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

import javax.lang.model.type.TypeMirror;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

/**
 * Interface for annotation handle since some classes may need to read info from
 * both ASM {@link AnnotationNode}s at runtime and mirror annotations in the AP
 * at compile time.
 */
public interface IAnnotationHandle {
    
    /**
     * Get whether the annotation inside the handle actually exists, if the
     * contained element is null, returns false.
     * 
     * @return true if the annotation exists
     */
    public abstract boolean exists();

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
     * Get an annotation value as an ASM {@link Type}. This is special-cased
     * because the different APIs return class literals in different ways. Under
     * ASM we will receieve {@link Type} instances, but at compile time we will
     * get {@link TypeMirror}s instead. This overload is provided so that
     * subclasses have to marshal everything into {@link Type} for consistency.
     * 
     * @param key key to fetch
     * @return value
     */
    public abstract Type getTypeValue(String key);

    /**
     * Retrieve an annotation key as a list of Types. This is special-cased
     * because the different APIs return class literals in different ways. Under
     * ASM we will receieve {@link Type} instances, but at compile time we will
     * get {@link TypeMirror}s instead. This overload is provided so that
     * subclasses have to marshal everything into {@link Type} for consistency.
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
     * Get the primitive boolean value with the specified key or return null if
     * not present or not set
     * 
     * @param key key to fetch
     * @param defaultValue default value to return if value is not present
     * @return value or default if not present or not set
     */
    public abstract boolean getBoolean(String key, boolean defaultValue);

    /**
     * Retrieve the annotation value as a list with values of the specified
     * type. Returns an empty list if the value is not present or not set.
     * 
     * @param <T> list element duck type
     * @return list of values
     */
    public abstract <T> List<T> getList();

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
