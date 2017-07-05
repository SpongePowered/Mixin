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
 * This annotation, when applied to a member method or field in a mixin,
 * indicates that the member <b>should never</b> overwrite a matching member in
 * the target class. This indicates that the member differs from the normal
 * "overlay-like" behaviour of mixins in general, and should only ever be
 * <em>added</em> to the target. For public fields, the annotation has no
 * effect.
 * 
 * <p>Typical usage of this annotation would be to decorate a utility method in
 * a mixin, or mark an interface-implementing method which must not overwrite a
 * target if it exists (consider appropriate use of {@link Intrinsic} in these
 * situations).</p>
 *
 * <p>Because of the mixed usage, this annotation has different implications for
 * methods with differing visibility:</p>
 * 
 * <dl>
 *   <dt>public methods</dt>
 *   <dd>public methods marked with this annotation are <b>discarded</b> if a
 *   matching target exists. Unless {@link #silent} is set to <tt>true</tt>, a
 *   <tt>warning</tt>-level message is generated.</dd>
 *   <dt>private and protected methods</dt>
 *   <dd>non-public methods are <b>renamed</b> if a matching target method is
 *   found, this allows utility methods to be safely assigned meaningful names
 *   in code, but renamed if a conflict occurs when a mixin is applied.</dd>
 * </dl>
 * 
 * <p><strong>Notes</strong></p>
 * 
 * <ul>
 *   <li>To mark all methods in a mixin as unique, apply the annotation to the
 *     mixin itself</li>
 *   <li>Uniqueness can be defined on a per-interface basis by using an
 *     {@link Implements} annotation with <tt>unique</tt> set to <tt>true</tt>
 *     </li>
 * </ul>
 */
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Unique {
    
    /**
     * If this annotation is applied to a public method in a mixin and a
     * conflicting method is found in a target class, then a
     * <tt>warning</tt>-level message is generated in the log. To suppress this
     * message, set this value to <tt>true</tt>.
     * 
     * @return true to suppress warning message when a public method is
     *      discarded
     */
    public boolean silent() default false;

}
