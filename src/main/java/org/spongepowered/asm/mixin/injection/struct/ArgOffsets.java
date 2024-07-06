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
 * they must be), this results in locating the original method args as as a
 * contiguous "window" of arguments somewhere in the middle of the args as they
 * exist at application time.
 * 
 * <p>Injectors which mutate the arguments of an invocation should apply this
 * decoration to indicate the starting offset and size of the window which
 * contains the original args.</p>
 */
public class ArgOffsets implements IChainedDecoration<ArgOffsets> {
    
    /**
     * No-op arg offsets to be used when we just want unaltered arg offsets
     */
    private static class Default extends ArgOffsets {

        public Default() {
            super(0, 255);
        }
        
        @Override
        public int getArgIndex(int index) {
            return index;
        }
        
        @Override   
        public Type[] apply(Type[] args) {
            return args;
        }

    }
    
    /**
     * Null offsets
     */
    public static ArgOffsets DEFAULT = new ArgOffsets.Default();
    
    /**
     * Decoration key for this decoration type 
     */
    public static final String KEY = "argOffsets";
    
    /**
     * The offset for the start of the (original) args within the new args
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
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("ArgOffsets[start=%d(%d),length=%d]", this.offset, this.getStartIndex(), this.length);
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
     * Get the size of the offset window
     */
    public int getLength() {
        return this.length;
    }
    
    /**
     * Get whether this argument offset window is empty
     */
    public boolean isEmpty() {
        return this.length == 0;
    }
    
    /**
     * Compute the argument index for the start of the window (offet 0)
     * 
     * @return the offset index for the start of the window (inclusive)
     */
    public int getStartIndex() {
        return this.getArgIndex(0);
    }
    
    /**
     * Compute the argument index for the end of the window (offset length)
     * 
     * @return the offset index for the end of the window (inclusive)
     */
    public int getEndIndex() {
        return this.isEmpty() ? this.getStartIndex() : this.getArgIndex(this.length - 1);
    }
    
    /**
     * Compute the argument index for the specified new index
     * 
     * @param index The new index to compute
     * @return The original index based on this mapping
     */
    public int getArgIndex(int index) {
        return this.getArgIndex(index, false);
    }
        
    /**
     * Compute the argument index for the specified new index
     * 
     * @param index The new index to compute
     * @param mustBeInWindow Throw an exception if the requested index exceeds
     *      the length of the defined window
     * @return The original index based on this mapping
     */
    public int getArgIndex(int index, boolean mustBeInWindow) {
        if (mustBeInWindow && index > this.length) {
            throw new IndexOutOfBoundsException("The specified arg index " + index + " is greater than the window size " + this.length);
        }
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
