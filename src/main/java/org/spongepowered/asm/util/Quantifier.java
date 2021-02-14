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
package org.spongepowered.asm.util;

/**
 * A regex-style quantifier, specified as a number or pair of numbers in braces.
 */
public final class Quantifier {
    
    /**
     * Default quantifier, used when a quantifier was not specified
     * (empty string)
     */
    public static Quantifier DEFAULT = new Quantifier(0, -1);
    
    /**
     * Invalid (matches none) 
     */
    public static Quantifier NONE = new Quantifier(0, 0);
    
    /**
     * Single (matches zero or 1)
     */
    public static Quantifier SINGLE = new Quantifier(0, 1);
    
    /**
     * Default (matches any)
     */
    public static Quantifier ANY = new Quantifier(0, Integer.MAX_VALUE);
    
    /**
     * Plus (matches 1 or more)
     */
    public static Quantifier PLUS = new Quantifier(1, Integer.MAX_VALUE);
    
    /**
     * Minimum value, parsed from expression, 0 if unbounded
     */
    private final int min;
    
    /**
     * Maximum value, parsed from expression, Integer.MAX_VALUE if unbounded
     */
    private final int max;

    public Quantifier(int min, int max) {
        this.min = min;
        this.max = max;
    }
    
    /**
     * Check whether this is a defaulted qualifier
     */
    public boolean isDefault() {
        return this.min == 0 && this.max < 0;
    }
    
    /**
     * Get the literal min value
     */
    public int getMin() {
        return this.min;
    }
    
    /**
     * Get the literal max value
     */
    public int getMax() {
        return this.max;
    }
    
    /**
     * Get the clamped min value
     */
    public int getClampedMin() {
        return Math.max(0, this.min);
    }
    
    /**
     * Get the clamped max value
     */
    public int getClampedMax() {
        return this.max < 0 ? 1 : Math.max(this.min, this.max);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        if (this.min == this.max) {
            sb.append(this.min);
        } else if (this.max < this.min) {
            return "";
        } else {
            if (this.min == 0) {
                if (this.max == 1) {
                    return "";
                } else if (this.max == Integer.MAX_VALUE) {
                    return "*";
                }
            }
            if (this.min == 1 && this.max == Integer.MAX_VALUE) {
                return "+";
            }
            if (this.min > 0) {
                sb.append(this.min);
            }
            if (this.min >= 0) {
                sb.append(',');                
            }
            if (this.max < Integer.MAX_VALUE) {
                sb.append(this.max);
            }
        }
        return sb.append('}').toString();
    }

    /**
     * Parse a quantifier from the supplied string
     * 
     * @param string string to parse
     * @return parsed quantifier, malformed quantifiers return NONE
     */
    public static Quantifier parse(String string) {
        if (string == null || ((string = string.trim()).length() == 0)) {
            return Quantifier.DEFAULT;
        }
        
        if ("*".equals(string)) {
            return Quantifier.ANY;
        }
        
        if ("+".equals(string)) {
            return Quantifier.PLUS;
        }
        
        if (!string.startsWith("{") || !string.endsWith("}") || string.length() < 3) {
            return Quantifier.NONE; // malformed
        }

        String inner = string.substring(1, string.length() - 1).trim();
        if (inner.isEmpty()) {
            return Quantifier.NONE;
        }
        
        String strMin = inner;
        String strMax = inner;
        
        int comma = inner.indexOf(',');
        if (comma > -1) {
            strMin = inner.substring(0, comma).trim();
            strMax = inner.substring(comma + 1).trim();
        }

        try {
            int min = strMin.length() > 0 ? Integer.parseInt(strMin) : 0;
            int max = strMax.length() > 0 ? Integer.parseInt(strMax) : Integer.MAX_VALUE;
            return new Quantifier(min, max);
        } catch (NumberFormatException ex) {
            return Quantifier.NONE;
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Quantifier) {
            Quantifier other = (Quantifier)obj;
            return other.min == this.min && other.max == this.max;
        }
        
        if (obj instanceof Number) {
            int intValue = ((Number)obj).intValue();
            return (intValue == this.min) && (intValue == this.max);
        }
        
        return false;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return 31 * this.min * this.max;
    }

}
