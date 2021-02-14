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

/**
 * A target selector which selects elements directly by name.
 */
public interface ITargetSelectorByName extends ITargetSelector {

    /**
     * Get the member owner, can be null
     */
    public abstract String getOwner();
    
    /**
     * Get the member name, can be null
     */
    public abstract String getName();
    
    /**
     * Get the member descriptor, can be null
     */
    public abstract String getDesc();

    /**
     * Get a representation of this selector as a complete descriptor
     */
    public abstract String toDescriptor();

    /**
     * Test whether this selector matches the supplied values. Null values are
     * ignored.

     * @param owner Owner to compare with, null to skip
     * @param name Name to compare with, null to skip
     * @param desc Signature to compare with, null to skip
     * @return true if all non-null values in this reference match non-null
     *      arguments supplied to this method
     */
    public abstract MatchResult matches(String owner, String name, String desc);

}
