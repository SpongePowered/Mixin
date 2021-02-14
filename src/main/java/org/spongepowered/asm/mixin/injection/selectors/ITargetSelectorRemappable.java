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
package org.spongepowered.asm.mixin.injection.selectors;

import org.spongepowered.asm.obfuscation.mapping.IMapping;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;

/**
 * A target selector which can be remapped at compile time via an obfuscation
 * service
 */
public interface ITargetSelectorRemappable extends ITargetSelectorByName {

    /**
     * Get whether this reference is fully qualified
     * 
     * @return true if all components of this reference are non-null 
     */
    public abstract boolean isFullyQualified();

    /**
     * Get whether this target selector is definitely a field, the output of
     * this method is undefined if {@link #isFullyQualified} returns false.
     * 
     * @return true if this is definitely a field
     */
    public abstract boolean isField();

    /**
     * Get whether this member represents a constructor
     * 
     * @return true if member name is <tt>&lt;init&gt;</tt>
     */
    public abstract boolean isConstructor();
    
    /**
     * Get whether this selector represents a class initialiser
     * 
     * @return true if member name is <tt>&lt;clinit&gt;</tt>
     */
    public abstract boolean isClassInitialiser();
    
    /**
     * Get whether this selector represents a constructor or class initialiser
     * 
     * @return true if member name is <tt>&lt;init&gt;</tt> or
     *      <tt>&lt;clinit&gt;</tt>
     */
    public abstract boolean isInitialiser();

    /**
     * Returns this selector as a {@link MappingField} or
     * {@link MappingMethod}
     */
    public abstract IMapping<?> asMapping();

    /**
     * Returns this selector as a mapping method
     */
    public abstract MappingMethod asMethodMapping();
    
    /**
     * Returns this selector as a mapping field
     */
    public abstract MappingField asFieldMapping();
    
    /**
     * Create a new version of this member with a different owner
     * 
     * @param newOwner New owner for this member
     */
    public abstract ITargetSelectorRemappable move(String newOwner);
    
    /**
     * Create a new version of this member with a different descriptor
     * 
     * @param newDesc New descriptor for this member
     */
    public abstract ITargetSelectorRemappable transform(String newDesc);
    
    /**
     * Create a remapped version of this member using the supplied method data
     * 
     * @param srgMethod SRG method data to use
     * @param setOwner True to set the owner as well as the name
     * @return New MethodInfo with remapped values
     */
    public abstract ITargetSelectorRemappable remapUsing(MappingMethod srgMethod, boolean setOwner);
    
}
