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
package org.spongepowered.tools.obfuscation;

import org.spongepowered.asm.mixin.Overwrite;

/**
 * A centralised list of tokens supported by the AP for use in
 * {@link SuppressWarnings} annotations. Collected here mainly for
 * self-documentation purposes.
 */
public enum SuppressedBy {
    
    /**
     * Suppress warnings for constraint violations
     * <div class="example">Usage:
     * <code><b>&#64;SuppressWarnings(<i>'constraints'</i>)</b></code>
     * </div>
     */
    CONSTRAINTS("constraints"),
    
    /**
     * Suppress warnings about overwrite visibility upgrading/downgrading target
     * visibility 
     * <div class="example">Usage:
     * <code><b>&#64;SuppressWarnings(<i>'visibility'</i>)</b></code>
     * </div>
     */
    VISIBILITY("visibility"),
    
    /**
     * Suppress warnings when an injector target cannot be found
     * <div class="example">Usage:
     * <code><b>&#64;SuppressWarnings(<i>'target'</i>)</b></code>
     * </div>
     */
    TARGET("target"),
    
    /**
     * Suppress warnings when a class, method or field mapping cannot be located
     * <div class="example">Usage:
     * <code><b>&#64;SuppressWarnings(<i>'mapping'</i>)</b></code>
     * </div>
     */
    MAPPING("mapping"),
    
    /**
     * Suppress warnings for when an {@link Overwrite &#064;Overwrite} method is
     * missing javadoc, or author or reason tags
     * <div class="example">Usage:
     * <code><b>&#64;SuppressWarnings(<i>'overwrite'</i>)</b></code>
     * </div>
     */
    OVERWRITE("overwrite"),
    
    /**
     * Suppress warnings when a mixin target specified by name is located in the
     * default package
     * <div class="example">Usage:
     * <code><b>&#64;SuppressWarnings(<i>'default-package'</i>)</b></code>
     * </div>
     */
    DEFAULT_PACKAGE("default-package"),
    
    /**
     * Suppress warnings when a mixin target is resolved by the AP as visible
     * but cannot be referenced with a class literal for some reason 
     * <div class="example">Usage:
     * <code><b>&#64;SuppressWarnings(<i>'public-target'</i>)</b></code>
     * </div>
     */
    PUBLIC_TARGET("public-target"),
    
    /**
     * Suppress warning when a mixin target is resolved by the AP as imaginary
     * (unresolvable via Mirror at compile time), for example for anonymous
     * inner classes or other synthetic member classes
     * <div class="example">Usage:
     * <code><b>&#64;SuppressWarnings(<i>'unresolvable-target'</i>)</b></code>
     * </div>
     */
    UNRESOLVABLE_TARGET("unresolvable-target"),
    
    /**
     * The default java "raw types" suppressions
     * <div class="example">Usage:
     * <code><b>&#64;SuppressWarnings(<i>'rawtypes'</i>)</b></code>
     * </div>
     */
    RAW_TYPES("rawtypes");
    
    private final String token;

    private SuppressedBy(String token) {
        this.token = token;
    }
    
    /**
     * Get the string token which is used in the {@link SuppressWarnings}
     * annotation
     */
    public String getToken() {
        return this.token;
    }

}
