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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small helper to resolve the current java version
 */
public abstract class JavaVersion {
    
    private static double current = 0.0;
    
    private JavaVersion() {}
    
    /**
     * Get the current java version, calculates if necessary
     */
    public static double current() {
        if (JavaVersion.current == 0.0) {
            JavaVersion.current = JavaVersion.resolveCurrentVersion();
        }
        return JavaVersion.current;
    }

    private static double resolveCurrentVersion() {
        String version = System.getProperty("java.version");
        Matcher matcher = Pattern.compile("[0-9]+\\.[0-9]+").matcher(version);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group());
        }
        return 1.6;
    }

}
