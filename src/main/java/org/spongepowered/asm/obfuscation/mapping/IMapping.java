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
package org.spongepowered.asm.obfuscation.mapping;

/**
 * Base class for member mapping entries
 * 
 * @param <TMapping> type of this member, for transformation method return types
 */
public interface IMapping<TMapping> {

    /**
     * Type of mapping
     */
    public enum Type {
        FIELD,
        METHOD,
        CLASS,
        PACKAGE
    }
    
    /**
     * Get the mapping type (field, method, class, package)
     */
    public abstract Type getType();
    
    /**
     * Create a clone of this mapping with a new owner
     * 
     * @param newOwner new owner
     * @return cloned mapping
     */
    public abstract TMapping move(String newOwner);

    /**
     * Create a clone of this mapping with a new name
     * 
     * @param newName new name
     * @return cloned mapping
     */
    public abstract TMapping remap(String newName);
    
    /**
     * Create a clone of this mapping with a new descriptor
     * 
     * @param newDesc new descriptor
     * @return cloned mapping
     */
    public abstract TMapping transform(String newDesc);
    
    /**
     * Create a clone of this mapping
     * 
     * @return cloned mapping
     */
    public abstract TMapping copy();

    /**
     * Get the mapping name, for method mappings this includes the owner
     * 
     * @return the mapping name, includes the owner for method mappings
     */
    public abstract String getName();

    /**
     * Get the base name of this member, for example the bare field, method or
     * class name
     * 
     * @return the base name of this mapping
     */
    public abstract String getSimpleName();

    /**
     * Get the owner of this member, for fields and methods this is the class
     * name, for classes it is the package name, for packages it is undefined.
     * Can return null.
     * 
     * @return the parent of this mapping
     */
    public abstract String getOwner();

    /**
     * Get the descriptor of this member, for example the method descriptor or
     * field type. For classes and packages this is undefined. Can return null
     * since not all mapping types support descriptors.
     * 
     * @return the mapping descriptor
     */
    public abstract String getDesc();
    
    /**
     * Get the next most immediate super-implementation of this mapping. For
     * example if the mapping is a method and the method overrides a method in
     * the immediate superclass, return that method. Can return null if no
     * superclass is available or if no superclass definition exists.
     * 
     * @return the method immediately overridden by this method, or null if not
     *      present or not resolvable 
     */
    public abstract TMapping getSuper();

    /**
     * Get a representation of this mapping for serialisation. Individual
     * writers are free to use their own mappings, this method is for
     * convenience only.
     * 
     * @return string representation of this mapping
     */
    public abstract String serialise();
    
}
