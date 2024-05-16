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
package org.spongepowered.asm.mixin.injection.struct;

import org.objectweb.asm.Type;

/**
 * Decoration which stores a mapping of new argument offsets to original
 * argument offsets.
 */
public class ArgOffsets implements IChainedDecoration<ArgOffsets> {
    
    /**
     * Decoration key for this decoration type 
     */
    public static final String KEY = "argOffsets";
    
    /**
     *  Default offsets
     */
    public static final ArgOffsets UNITY = new ArgOffsets(0, 1024);
    
    /**
     * Mapping of original offsets to new offsets
     */
    private final int[] mapping;
    
    /**
     * If this offset collection replaces a previous mapping, chain to the next
     * mapping in order to apply these offsets atop the old ones
     */
    private ArgOffsets next;
    
    /**
     * Create contiguous offsets starting from start and continuing for length
     * 
     * @param start start index
     * @param length length
     */
    public ArgOffsets(int start, int length) {
        this.mapping = new int[length];
        for (int i = 0; i < length; i++) {
            this.mapping[i] = start++;
        }
    }
    
    /**
     * Create an offset collection from an explicit array
     * 
     * @param offsets offsets to store
     */
    public ArgOffsets(int... offsets) {
        this.mapping = offsets;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.struct.IChainedDecoration
     *      #replace(
     *      org.spongepowered.asm.mixin.injection.struct.IChainedDecoration)
     */
    @Override
    public void replace(ArgOffsets old) {
        this.next = old;
    }
    
    /**
     * Get the size of this mapping collection
     */
    public int getLength() {
        return this.mapping.length;
    }
    
    /**
     * Compute the argument index for the specified new index
     * 
     * @param index The new index to compute
     * @return The original index based on this mapping
     */
    public int getArgIndex(int index) {
        int offsetIndex = this.mapping[index];
        return this.next != null ? this.next.getArgIndex(offsetIndex) : offsetIndex;
    }

    /**
     * Apply this offset collection to the supplied argument array
     * 
     * @param args New arguments
     * @return Unmapped arguments
     */
    public Type[] apply(Type[] args) {
        Type[] transformed = new Type[this.mapping.length];
        for (int i = 0; i < this.mapping.length; i++) {
            int offset = this.getArgIndex(i);
            if (offset < args.length) {
                transformed[i] = args[offset];
            }
        }
        return transformed;
    }

}
