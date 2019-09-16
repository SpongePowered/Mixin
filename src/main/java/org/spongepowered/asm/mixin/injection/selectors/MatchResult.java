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
 * Result of a {@link ITargetSelector target selector} <em>match</em> operation
 * which describes the type of match. 
 */
public enum MatchResult {

    /**
     * No result, the selector does not match the candidate
     */
    NONE,
    
    /**
     * Matches only in the weakest sense, use this result only if all else fails
     * and nothing else matches at all 
     */
    WEAK,
    
    /**
     * A confident match, but not an exact match. It may be that other optional
     * parts of the selector did not match the candidate. For example this could
     * mean that the selector matched case-insensitively. This is the best
     * result if an {@link #EXACT_MATCH} match is not found.
     */
    MATCH,
    
    /**
     * All parts, including optional parts of the selector matched the candidate
     * and this result is the best. If more than one <tt>EXACT_MATCH</tt> is
     * returned from a query, the first one should be used.
     */
    EXACT_MATCH;
    
    
    /**
     * Get whether this match level represents a level which is the
     * same or greater than the supplied level
     * 
     * @param other level to compare to
     * @return true if greater or equal
     */
    public boolean isAtLeast(MatchResult other) {
        return other == null || other.ordinal() <= this.ordinal();
    }
    
    /**
     * Get whether this match succeeded
     */
    public boolean isMatch() {
        return this.ordinal() >= MatchResult.MATCH.ordinal();
    }
    
    /**
     * Get whether this is an exact match
     */
    public boolean isExactMatch() {
        return this == MatchResult.EXACT_MATCH;
    }
    
}
