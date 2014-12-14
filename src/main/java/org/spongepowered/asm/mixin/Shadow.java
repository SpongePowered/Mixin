/**
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered.org <http://www.spongepowered.org>
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
 * Used to indicate a Mixin class member which is acting as a placeholder for a method or field in the target class 
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Shadow {

    /**
     * <p>In general, shadow methods can be declared using their name in the target class as you would expect, however we run into a problem when we
     * want to mix in a method with the same name and arguments, but a different return type to the shadow method. While the JVM itself will happily
     * support methods with signatures that differ only on return type, the compiler itself does not. This poses a problem, since we have no way to
     * leverage this behaviour since our mixin class will not compile.</p>
     *
     * <p>To circumvent this compiler limitation, the prefix option can be used. By specifying a prefix for the shadow method, it is subsequently
     * possible to compile the mixin class, the specified prefix will then be stripped from the method name prior to applying the mixin, and
     * everything will work as expected. You may either use the default prefix: "shadow$", or you may specify your own. It is good practice to
     * specify the prefix if you are using it, regardless of whether you use the default or not. For example consider the intrinsic readability of
     * the following snippets</p>:
     *
     * <blockquote><pre>
     *     &#64;Shadow abstract void someMethod(int arg1, int arg2);
     *     &#64;Shadow abstract void shadow$someMethod(int arg1, int arg2);
     *     &#64;Shadow(prefix = "shadow$") abstract void shadow$someMethod(int arg1, int arg2);
     *     &#64;Shadow(prefix = "foo$") abstract void foo$someMethod(int arg1, int arg2);
     * </pre></blockquote>
     *
     * <p>All of these declarations are semantically equivalent, however the third and fourth are the most expressive in terms of making their
     * intentions clear, and thus specifying prefix is recommended, since it aids readability and maintainability.<p>
     *
     * <p>Note that specifying a <em>prefix</em> does not <b>enforce</b> use of the prefix, the behaviour of <em>prefix</em> is such that the prefix
     * will be stripped from the start of the method name <em>as long as the method name actually starts with the prefix</em>! This has important
     * repercussions since if the annotation value does not match the method prefix then <em>no renaming will take place</em> likey resulting in
     * a failure state indicated by an {@link InvalidMixinException} at run time.</p>
     */
    public String prefix() default "shadow$";

}
