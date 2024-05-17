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
 * Decoration which stores a linear offset of arguments when a node replacement
 * results in a call to a method with the same arguments as the original
 * (replaced) call but offset by some fixed amount. Since ModifyArg and
 * ModifyArgs always assume the method args are on the top of the stack (which
 * they must be), this essentially results in just chopping off a fixed number
 * of arguments from the start of the method.
 */
public class ArgOffsets implements IChainedDecoration<ArgOffsets> {
    
    /**
     * Decoration key for this decoration type 
     */
    public static final String KEY = "argOffsets";
    
    /**
     * The offset for the start of the args
     */
    private final int offset;
    
    /**
     * The total number of (original) args
     */
    private final int length;
    
    /**
     * If this offset collection replaces a previous mapping, chain to the next
     * mapping in order to apply these offsets atop the old ones
     */
    private ArgOffsets next;
    
    /**
     * Create contiguous offsets starting from start and continuing for length
     * 
     * @param offset start index
     * @param length length
     */
    public ArgOffsets(int offset, int length) {
        this.offset = offset;
        this.length = length;
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
        return this.length;
    }
    
    /**
     * Compute the argument index for the specified new index
     * 
     * @param index The new index to compute
     * @return The original index based on this mapping
     */
    public int getArgIndex(int index) {
        int offsetIndex = index + this.offset;
        return this.next != null ? this.next.getArgIndex(offsetIndex) : offsetIndex;
    }

    /**
     * Apply this offset collection to the supplied argument array
     * 
     * @param args New arguments
     * @return Unmapped arguments
     */
    public Type[] apply(Type[] args) {
        Type[] transformed = new Type[this.length];
        for (int i = 0; i < this.length; i++) {
            int offset = this.getArgIndex(i);
            if (offset < args.length) {
                transformed[i] = args[offset];
            }
        }
        return transformed;
    }

}
