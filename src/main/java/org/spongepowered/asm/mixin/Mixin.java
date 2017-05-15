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
package org.spongepowered.asm.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Decorator for mixin classes
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Mixin {

    /**
     * Target class(es) for this mixin
     * 
     * @return classes this mixin targets
     */
    public Class<?>[] value() default { };
    
    /**
     * Since specifying targets in {@link #value} requires that the classes be
     * publicly visible, this property is provided to allow package-private,
     * anonymous innner, and private inner classes to be referenced. Referencing
     * an otherwise public class using this property is an error condition and
     * will throw an exception at runtime. It is completely fine to specify both
     * public and private targets for the same mixin however.
     *
     * @return protected or package-private classes this mixin targets
     */
    public String[] targets() default { };

    /**
     * Priority for the mixin, relative to other mixins targetting the same
     * classes
     * 
     * @return the mixin priority (relative to other mixins targetting the same
     *      class)
     */
    public int priority() default 1000;

    /**
     * By default, the annotation processor will attempt to locate an
     * obfuscation mapping for all {@link Shadow} and
     * {@link org.spongepowered.asm.mixin.injection.Inject} annotated members
     * since it is anticipated that in general the target of a {@link Mixin}
     * will be an obfuscated class and all annotated members will need to be
     * added to the obfuscation table. However since it is possible to also
     * apply mixins to non-obfuscated targets it may be desirable to suppress
     * the compiler warnings which would otherwise be generated. This can be
     * done on an individual member basis by setting <code>remap</code> to
     * <em>false</em> on the individual annotations, or disabled for the entire
     * mixin by setting the value here to <em>false</em>. Doing so will cause
     * the annotation processor to skip all annotations in this mixin when
     * building the obfuscation table unless the individual annotation is
     * explicitly decorated with <tt>remap = true</tt>.
     * 
     * @return True to instruct the annotation processor to search for
     *      obfuscation mappings for this annotation (default true). 
     */
    public boolean remap() default true;
}
