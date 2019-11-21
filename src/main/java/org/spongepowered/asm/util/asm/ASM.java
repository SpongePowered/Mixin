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

import java.lang.reflect.Field;

import org.objectweb.asm.Opcodes;

/**
 * Utility methods for determining ASM version and other version-specific
 * shenanigans
 */
public final class ASM {
    
    private static int majorVersion = 5;
    private static int minorVersion = 0;
    private static String maxVersion = "FALLBACK";
    
    /**
     * The detected ASM API Version
     */
    public static final int API_VERSION = ASM.detectVersion();

    private ASM() {
    }
    
    /**
     * Get the major API version
     */
    public static int getApiVersionMajor() {
        return ASM.majorVersion;
    }
    
    /**
     * Get the minor API version
     */
    public static int getApiVersionMinor() {
        return ASM.minorVersion;
    }
    
    /**
     * Get the ASM API version as a string (mostly for debugging and the banner)
     * 
     * @return ASM API version as string
     */
    public static String getApiVersionString() {
        return String.format("ASM %d.%d (%s)", ASM.majorVersion, ASM.minorVersion, ASM.maxVersion);
    }

    private static int detectVersion() {
        int apiVersion = Opcodes.ASM4;
        
        for (Field field : Opcodes.class.getDeclaredFields()) {
            if (field.getType() != Integer.TYPE || !field.getName().startsWith("ASM")) {
                continue;
            }
            
            try {
                int version = field.getInt(null);
                
                // int patch = version & 0xFF;
                int minor = (version >> 8) & 0xFF;
                int major = (version >> 16) & 0xFF;
                boolean experimental = ((version >> 24) & 0xFF) != 0;
                
                if (major >= ASM.majorVersion) {
                    ASM.maxVersion = field.getName();
                    if (!experimental) {
                        apiVersion = version;
                        ASM.majorVersion = major;
                        ASM.minorVersion = minor;
                    }
                }
            } catch (ReflectiveOperationException ex) {
                throw new Error(ex);
            }
        }
        
        return apiVersion;
    }

}
