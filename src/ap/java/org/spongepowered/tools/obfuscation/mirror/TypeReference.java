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
package org.spongepowered.tools.obfuscation.mirror;

import java.io.Serializable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * Soft wrapper for a {@link TypeHandle} so that we can serialise it
 */
public class TypeReference implements Serializable, Comparable<TypeReference> {
    
    private static final long serialVersionUID = 1L;

    /**
     * Class name, used to recreate the handle on the other side
     */
    private final String name;
    
    /**
     * Transient handle, not serialised, regenerated when required
     */
    private transient TypeHandle handle;

    /**
     * Create a new soft wrapper for the specified type handle
     * 
     * @param handle handle to wrap
     */
    public TypeReference(TypeHandle handle) {
        this.name = handle.getName();
        this.handle = handle;
    }
    
    /**
     * Create a type reference with no handle
     * 
     * @param name Name of the type
     */
    public TypeReference(String name) {
        this.name = name;
    }
    
    /**
     * Get the class name (internal format)
     */
    public String getName() {
        return this.name;
    }
    
    /**
     * Get the FQ class name (dotted format)
     */
    public String getClassName() {
        return this.name.replace('/', '.');
    }
    
    /**
     * Fetch or attempt to generate the type handle
     * 
     * @param processingEnv environment to create handle if it needs to be
     *      regenerated
     * @return type handle
     */
    public TypeHandle getHandle(ProcessingEnvironment processingEnv) {
        if (this.handle == null) {
            TypeElement element = processingEnv.getElementUtils().getTypeElement(this.getClassName());
            try {
                this.handle = new TypeHandle(element);
            } catch (Exception ex) {
                // Class probably doesn't exist in scope :/
                ex.printStackTrace();
            }
        }
        
        return this.handle;
    }

    @Override
    public String toString() {
        return String.format("TypeReference[%s]", this.name);
    }

    @Override
    public int compareTo(TypeReference other) {
        return other == null ? -1 : this.name.compareTo(other.name);
    }
    
    @Override
    public boolean equals(Object other) {
        return other instanceof TypeReference && this.compareTo((TypeReference)other) == 0;
    }
    
    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
    
}
