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
package org.spongepowered.asm.mixin.gen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.spongepowered.asm.mixin.Shadow;

/**
 * Defines an <em>invoker method</em> (also known as a <em>proxy method</em> in
 * a mixin, the method. The annotated method must be <tt>abstract</tt> and the
 * signature of the method must match the target method precisely.
 *
 * <p>Invokers provide a simple way of gaining access to internal class members
 * in a target class without needing to resort to access transformers, and
 * without the usual need to {@link Shadow} a target method. This can both
 * greatly simplify mixins which <b>only</b> contain accessors, and provide for
 * faster development than with access transformers since no re-decompile is
 * needed to put the changes into effect.</p>
 *
 * <p>Invokers can be used in regular mixins as a way to provide access to a
 * private method without needing to shadow and then manually proxy the method
 * call. They can also be used to create <i>"Accessor Mixins"</i> which are
 * special mixins defined as <tt>interface</tt>s which must <b>only</b> contain
 * {@link Accessor} and {@link Invoker} methods. Unlike normal mixins however,
 * <i>Accessor Mixins</i> are accessible via user code and thus no surrogate
 * <i>"Duck"</i> interface is required to expose the generated methods, the
 * mixin itself acts as its own Duck.</p>
 *
 * <ul><li>See also {@link Accessor}</li></ul>
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Invoker {

    /**
     * Normally the target method name is inflected by examining the annotated
     * method name. If the annotated method starts with "call" or "invoke"
     * followed by a capital letter, then the prefix is stripped and the
     * remainder of the method name is used as the target method name.
     *
     * <p>However sometimes it maye be desirable to name an accessor method
     * differently to the target method. In this case you may specify the method
     * using its name.</p>
     *
     * @return name for the target method, or empty string to inflect using the
     *      annotated method name
     */
    public String value() default "";

    /**
     * By default, the annotation processor will attempt to locate an
     * obfuscation mapping for all {@link Invoker} methods since it is
     * anticipated that in general the target of a {@link Invoker} annotation
     * will be an obfuscated method in the target class. However since it is
     * possible that the target is not obfuscated, it may be desirable to
     * suppress the compiler warning which would be generated. Setting this
     * value to <em>false</em> will cause the annotation processor to skip
     * remapping for this annotation.
     *
     * @return True to instruct the annotation processor to search for
     *      obfuscation mappings for this annotation
     */
    public boolean remap() default true;

}
