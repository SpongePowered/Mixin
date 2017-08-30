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
package org.spongepowered.asm.mixin.refmap;

/**
 * Interface for reference mapper objects
 */
public interface IReferenceMapper {

    /**
     * Get whether this mapper is defaulted. Use this flag rather than reference
     * comparison to {@link ReferenceMapper#DEFAULT_MAPPER} because of
     * classloader shenanigans
     * 
     * @return true if this mapper is a defaulted mapper
     */
    public abstract boolean isDefault();
    
    /**
     * Get the resource name this refmap was loaded from (if available).
     * 
     * @return name of the resource
     */
    public abstract String getResourceName();
    
    /**
     * Get a user-readable "status" string for this refmap for use in error 
     * messages
     * 
     * @return status message
     */
    public abstract String getStatus();
    
    /**
     * Get the current context
     * 
     * @return current context key, can be null
     */
    public abstract String getContext();
    
    /**
     * Set the current remap context, can be null
     * 
     * @param context remap context
     */
    public abstract void setContext(String context);
    
    /**
     * Remap a reference for the specified owning class in the current context
     * 
     * @param className Owner class
     * @param reference Reference to remap
     * @return remapped reference, returns original reference if not remapped
     */
    public abstract String remap(String className, String reference);
    
    /**
     * Remap a reference for the specified owning class in the specified context
     * 
     * @param context Remap context to use
     * @param className Owner class
     * @param reference Reference to remap
     * @return remapped reference, returns original reference if not remapped
     */
    public abstract String remapWithContext(String context, String className, String reference);

}
