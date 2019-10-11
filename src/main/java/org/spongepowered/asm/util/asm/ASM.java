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
package org.spongepowered.asm.util.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

/**
 * Utility methods for determining ASM version and other version-specific
 * shenanigans
 */
public final class ASM {
    
    @SuppressWarnings("deprecation")
    private static final int[] EXPERIMENTAL_VERSIONS = { Opcodes.ASM7_EXPERIMENTAL }; 
    private static final int[] SUPPORTED_VERSIONS = { Opcodes.ASM6, Opcodes.ASM5 }; 
    
    public static final int API_VERSION = ASM.detectVersion();
    
    private static boolean experimental;
    
    private ASM() {
    }
    
    /**
     * Get the ASM API version as a string (mostly for debugging and the banner)
     * 
     * @return
     */
    public static String getApiVersionString() {
        String suffix = "";
        if (ASM.experimental) {
            int version = ASM.detectVersion(ASM.SUPPORTED_VERSIONS);
            suffix = String.format("-EXPERIMENTAL (%d.%d)", ((0xFF0000 & version) >> 16), ((0xFF00 & version) >> 8));
        }
        return String.format("ASM %d.%d%s", ((0xFF0000 & ASM.API_VERSION) >> 16), ((0xFF00 & ASM.API_VERSION) >> 8), suffix);
    }

    private static int detectVersion() {
        int expVersion = ASM.detectVersion(ASM.EXPERIMENTAL_VERSIONS);
        if (expVersion > 0) {
            ASM.experimental = true;
            return expVersion;
        }
        
        ASM.experimental = false;
        int version = ASM.detectVersion(ASM.SUPPORTED_VERSIONS);
        if (version > 0) {
            return version;
        }

        return Opcodes.ASM5;
    }

    private static int detectVersion(int[] versions) {
        for (int version : versions) {
            try {
                new ClassNode(version).hashCode();
                return version;
            } catch (IllegalArgumentException ex) {
                // expected, this version is not supported
            }
        }
        return 0;
    }

}
