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

import java.io.File;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Miscellaneous shared constants
 */
public abstract class Constants {

    public static final String CTOR = "<init>";
    public static final String CLINIT = "<clinit>";
    public static final String IMAGINARY_SUPER = "super$";
    public static final String DEBUG_OUTPUT_PATH = ".mixin.out";
    public static final String MIXIN_PACKAGE = Mixin.class.getPackage().getName();
    public static final String MIXIN_PACKAGE_REF = Constants.MIXIN_PACKAGE.replace('.', '/');

    public static final String STRING = "java/lang/String";
    public static final String OBJECT = "java/lang/Object";
    public static final String CLASS = "java/lang/Class";
    
    public static final String STRING_DESC = "L" + Constants.STRING + ";";
    public static final String OBJECT_DESC = "L" + Constants.OBJECT + ";";
    public static final String CLASS_DESC = "L" + Constants.CLASS + ";";
    
    public static final String SYNTHETIC_PACKAGE = "org.spongepowered.asm.synthetic";
    public static final char UNICODE_SNOWMAN = '\u2603';
    
    public static final File DEBUG_OUTPUT_DIR = new File(Constants.DEBUG_OUTPUT_PATH);
    
    public static final String SIDE_DEDICATEDSERVER = "DEDICATEDSERVER";
    public static final String SIDE_SERVER = "SERVER";
    public static final String SIDE_CLIENT = "CLIENT";
    public static final String SIDE_UNKNOWN = "UNKNOWN";
    
    private Constants() {}
    
    /**
     * Shared Jar Manifest Attributes
     */
    public abstract static class ManifestAttributes {
        
        public static final String TWEAKER = "TweakClass";
        public static final String MAINCLASS = "Main-Class";
        public static final String MIXINCONFIGS = "MixinConfigs";
        public static final String TOKENPROVIDERS = "MixinTokenProviders";
        public static final String MIXINCONNECTOR = "MixinConnector";
        
        @Deprecated
        public static final String COMPATIBILITY = "MixinCompatibilityLevel";

        private ManifestAttributes() {}
    }
}
