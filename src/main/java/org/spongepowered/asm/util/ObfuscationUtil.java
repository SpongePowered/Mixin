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
 * Utility methods for obfuscation tasks
 */
public abstract class ObfuscationUtil {
    
    /**
     * Interface for remapper proxies
     */
    public interface IClassRemapper {

        /**
         * Map type name to the new name. Subclasses can override.
         * 
         * @param typeName Class name to convert 
         * @return new name for the class
         */
        public abstract String map(String typeName);

        /**
         * Convert a mapped type name back to the original obfuscated name
         * 
         * @param typeName Class name to convert
         * @return old name for the class
         */
        public abstract String unmap(String typeName);
        
    }
    
    private ObfuscationUtil() {}
    
    /**
     * Map a descriptor using the supplied rempper
     * 
     * @param desc descriptor to remap
     * @param remapper remapper to use
     * @return mapped descriptor
     */
    public static String mapDescriptor(String desc, IClassRemapper remapper) {
        return ObfuscationUtil.remapDescriptor(desc, remapper, false);
    }
    
    /**
     * Unmap (inverse of map) a descriptor using the supplied rempper
     * 
     * @param desc descriptor to unmap
     * @param remapper remapper to use
     * @return unmapped descriptor
     */
    public static String unmapDescriptor(String desc, IClassRemapper remapper) {
        return ObfuscationUtil.remapDescriptor(desc, remapper, true);
    }
    
    private static String remapDescriptor(String desc, IClassRemapper remapper, boolean unmap) {
        StringBuilder sb = new StringBuilder();
        StringBuilder token = null;

        for (int pos = 0; pos < desc.length(); pos++) {
            char c = desc.charAt(pos);
            if (token != null) {
                if (c == ';') {
                    sb.append('L').append(ObfuscationUtil.remap(token.toString(), remapper, unmap)).append(';');
                    token = null;
                } else {
                    token.append(c);
                }
                continue;
            }
            if (c == 'L') {
                token = new StringBuilder();
            } else {
                sb.append(c);
            }
        }
        
        if (token != null) {
            throw new IllegalArgumentException("Invalid descriptor '" + desc + "', missing ';'");
        }
        
        return sb.toString();
    }

    private static Object remap(String typeName, IClassRemapper remapper, boolean unmap) {
        String result = unmap ? remapper.unmap(typeName) : remapper.map(typeName);
        return result != null ? result : typeName;
    }

}
