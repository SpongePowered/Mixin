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
package org.spongepowered.asm.mixin.transformer.struct;

/**
 * Struct for representing a range
 */
public class Range {

    /**
     * Start of the range
     */
    public final int start;
    
    /**
     * End of the range 
     */
    public final int end;
    
    /**
     * Range marker
     */
    public final int marker;

    /**
     * Create a range with the specified values.
     * 
     * @param start Start of the range
     * @param end End of the range
     * @param marker Arbitrary marker value
     */
    public Range(int start, int end, int marker) {
        this.start = start;
        this.end = end;
        this.marker = marker;
    }
    
    /**
     * Range is valid if both start and end are nonzero and end is after or
     * at start
     * 
     * @return true if valid
     */
    public boolean isValid() {
        return (this.start != 0 && this.end != 0 && this.end >= this.start);
    }
    
    /**
     * Returns true if the supplied value is between or equal to start and
     * end
     * 
     * @param value true if the range contains value
     */
    public boolean contains(int value) {
        return value >= this.start && value <= this.end;
    }
    
    /**
     * Returns true if the supplied value is outside the range
     * 
     * @param value true if the range does not contain value
     */
    public boolean excludes(int value) {
        return value < this.start || value > this.end;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("Range[%d-%d,%d,valid=%s)", this.start, this.end, this.marker, this.isValid());
    }

}
