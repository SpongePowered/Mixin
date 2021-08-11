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
    
    /**
     * Version number for Java 6
     */
    public static final double JAVA_6 = 1.6;
    
    /**
     * Version number for Java 7
     */
    public static final double JAVA_7 = 1.7;
    
    /**
     * Version number for Java 8
     */
    public static final double JAVA_8 = 1.8;
    
    /**
     * Version number for Java 9
     */
    public static final double JAVA_9 = 9.0;
    
    /**
     * Version number for Java 10
     */
    public static final double JAVA_10 = 10.0;
    
    /**
     * Version number for Java 11
     */
    public static final double JAVA_11 = 11.0;
    
    /**
     * Version number for Java 12
     */
    public static final double JAVA_12 = 12.0;
    
    /**
     * Version number for Java 13
     */
    public static final double JAVA_13 = 13.0;
    
    /**
     * Version number for Java 14
     */
    public static final double JAVA_14 = 14.0;
    
    /**
     * Version number for Java 15
     */
    public static final double JAVA_15 = 15.0;
    
    /**
     * Version number for Java 16
     */
    public static final double JAVA_16 = 16.0;
    
    /**
     * Version number for Java 17
     */
    public static final double JAVA_17 = 17.0;
    
    /**
     * Version number for Java 18
     */
    public static final double JAVA_18 = 18.0;
    
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
        Matcher decimalMatcher = Pattern.compile("[0-9]+\\.[0-9]+").matcher(version);
        if (decimalMatcher.find()) {
            return Double.parseDouble(decimalMatcher.group());
        }
        Matcher numberMatcher = Pattern.compile("[0-9]+").matcher(version);
        if (numberMatcher.find()) {
            return Double.parseDouble(numberMatcher.group());
        }
        return 1.6;
    }

}
