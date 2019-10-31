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
package org.spongepowered.tools.obfuscation.ext;

import java.util.HashSet;
import java.util.Set;

/**
 * Centralised registry of packages with special handling by the AP. This is
 * public and centralised
 */
public final class SpecialPackages {

    private static final Set<String> suppressWarningsForPackages = new HashSet<String>();
    
    static {
        SpecialPackages.addExcludedPackage("java.");
        SpecialPackages.addExcludedPackage("javax.");
        SpecialPackages.addExcludedPackage("sun.");
        SpecialPackages.addExcludedPackage("com.sun.");
    }
    
    private SpecialPackages() {
    }
    
    /**
     * Add a package to exclude from remapping warnings. This only suppresses
     * warnings if a mapping is not found and does not attempt to short-circuit
     * the mapping process. Thus if a mapping is present for the element in
     * question then the behaviour is preserved. Adding a package here simply
     * suppresses warnings if no mapping is found.
     * 
     * @param packageName Package name, including trailing period or slash
     */
    public static final void addExcludedPackage(String packageName) {
        String internalName = packageName.replace('.', '/');
        if (!internalName.endsWith("/")) {
            internalName += "/";
        }
        SpecialPackages.suppressWarningsForPackages.add(internalName);
    }
    
    /**
     * Check whether a package is in the exclusion set.
     * 
     * @param internalName Internal class or package name
     * @return true if warnings for the supplied package should be suppressed 
     */
    public static boolean isExcludedPackage(String internalName) {
        for (String prefix : SpecialPackages.suppressWarningsForPackages) {
            if (internalName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

}
