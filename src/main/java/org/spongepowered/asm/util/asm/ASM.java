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
import java.util.jar.Attributes;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.launch.platform.MainAttributes;
import org.spongepowered.asm.util.VersionNumber;

/**
 * Utility methods for determining ASM version and other version-specific
 * shenanigans
 */
public final class ASM {
    
    private static int majorVersion = 5;
    private static int minorVersion = 0;
    
    // Implementation versions, only available from ASM
    private static int implMinorVersion = 0;
    private static int patchVersion = 0;
    
    private static String maxVersion = "FALLBACK";
    
    private static int maxClassVersion = Opcodes.V1_6;
    private static int maxClassMajorVersion = Opcodes.V1_6 & 0xFFFF;
    private static int maxClassMinorVersion = (Opcodes.V1_6 >> 16) & 0xFFFF;
    private static String maxJavaVersion = "V1.6";
    
    /**
     * The detected ASM API Version
     */
    public static final int API_VERSION = ASM.detectVersion();

    private ASM() {
    }
    
    /**
     * Get whether the current ASM API is at least the specified version
     * 
     * @param majorVersion version to check for (eg. 6)
     */
    public static boolean isAtLeastVersion(int majorVersion) {
        return ASM.majorVersion >= majorVersion;
    }
    
    /**
     * Get whether the current ASM API is at least the specified version
     * (including minor version when it's relevant)
     * 
     * @param majorVersion major version to check for (eg. 6)
     * @param minorVersion minor version to check for
     */
    public static boolean isAtLeastVersion(int majorVersion, int minorVersion) {
        return ASM.majorVersion >= majorVersion && (ASM.majorVersion > majorVersion || ASM.implMinorVersion >= minorVersion);
    }
    
    /**
     * Get whether the current ASM API is at least the specified version
     * (including minor version and patch version when it's relevant)
     * 
     * @param majorVersion major version to check for (eg. 6)
     * @param minorVersion minor version to check for
     * @param patchVersion patch version to check for
     */
    public static boolean isAtLeastVersion(int majorVersion, int minorVersion, int patchVersion) {
        if (ASM.majorVersion == majorVersion) {
            return ASM.implMinorVersion >= minorVersion && (ASM.implMinorVersion > minorVersion || ASM.patchVersion >= patchVersion);
        }
        return ASM.majorVersion > majorVersion;
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
     * Get the ASM API version as a string (mostly for debugging)
     * 
     * @return ASM API version as string
     */
    public static String getApiVersionString() {
        return String.format("%d.%d", ASM.majorVersion, ASM.minorVersion);
    }
    
    /**
     * Get the ASM version as a string (mostly for debugging and the banner)
     * 
     * @return ASM library version as string
     */
    public static String getVersionString() {
        return String.format("ASM %d.%d%s (%s)",
                ASM.majorVersion, ASM.implMinorVersion, ASM.patchVersion > 0 ? "." + ASM.patchVersion : "", ASM.maxVersion);
    }

    /**
     * Get the maximum supported class version (raw)
     */
    public static int getMaxSupportedClassVersion() {
        return ASM.maxClassVersion;
    }
    
    /**
     * Get the maximum supported major class versior
     */
    public static int getMaxSupportedClassVersionMajor() {
        return ASM.maxClassMajorVersion;
    }
    
    /**
     * Get the maximum supported minor class versior
     */
    public static int getMaxSupportedClassVersionMinor() {
        return ASM.maxClassMinorVersion;
    }
    
    /**
     * Get the supported java version as a string (mostly for the banner)
     * 
     * @return Java class supported version as string
     */
    public static String getClassVersionString() {
        return String.format("Up to Java %s (class file version %d.%d)", ASM.maxJavaVersion, ASM.maxClassMajorVersion, ASM.maxClassMinorVersion);
    }

    private static int detectVersion() {
        int apiVersion = Opcodes.ASM4;

        VersionNumber packageVersion = ASM.getPackageVersion(Opcodes.class);

        for (Field field : Opcodes.class.getDeclaredFields()) {
            if (field.getType() != Integer.TYPE) {
                continue;
            }
            
            try {
                String name = field.getName();
                int version = field.getInt(null);
                if (name.startsWith("ASM")) {
                    // int patch = version & 0xFF;
                    int minor = (version >> 8) & 0xFF;
                    int major = (version >> 16) & 0xFF;
                    boolean experimental = ((version >> 24) & 0xFF) != 0;
                    
                    if (major >= ASM.majorVersion) {
                        ASM.maxVersion = name;
                        if (!experimental) {
                            apiVersion = version;
                            ASM.majorVersion = major;
                            ASM.minorVersion = ASM.implMinorVersion = minor;
                            
                            if (packageVersion.getMajor() == major && minor == 0) {
                                ASM.implMinorVersion = packageVersion.getMinor();
                                ASM.patchVersion = packageVersion.getPatch();
                            }
                        }
                    }
                } else if (name.matches("V([0-9_]+)")) {
                    int minor = (version >> 16) & 0xFFFF;
                    int major = (version) & 0xFFFF;
                    if (major > ASM.maxClassMajorVersion || (major == ASM.maxClassMajorVersion && minor > ASM.maxClassMinorVersion)) {
                        ASM.maxClassMajorVersion = major;
                        ASM.maxClassMinorVersion = minor;
                        ASM.maxClassVersion = version;
                        ASM.maxJavaVersion = name.replace('_', '.').substring(1);
                    }
                } else if ("ACC_PUBLIC".equals(name)) {
                    break;
                }
            } catch (ReflectiveOperationException ex) {
                throw new Error(ex);
            }
        }
        
        return apiVersion;
    }

    private static VersionNumber getPackageVersion(Class<?> clazz) {
        String implVersion = clazz.getPackage().getImplementationVersion();
        if (implVersion != null) {
            return VersionNumber.parse(implVersion);
        }
        
        try {
            MainAttributes manifest = MainAttributes.of(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
            return VersionNumber.parse(manifest.get(Attributes.Name.IMPLEMENTATION_VERSION));
        } catch (Exception ex) {
            return VersionNumber.NONE;
        }
    }

}
