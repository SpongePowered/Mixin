/*
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
package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Specifies that this mixin method should redirect the specified
 * {@link #method} call to the method decorated with this annotation.</p>
 * 
 * <p>The handler method signature must match the hooked method precisely
 * <b>but</b> prepended with an arg of the owning object's type to accept the
 * object instance the method was going to be invoked on. For example when
 * hooking the following call:</p>
 * 
 * <blockquote><pre>
 *   public void baz(int someInt, String someString) {
 *     int abc = 0;
 *     int def = 1;
 *     Foo someObject = new Foo();
 *     
 *     // Hooking this method
 *     boolean xyz = someObject.bar(abc, def);
 *   }</pre>
 * </blockquote>
 * 
 * <p>The signature of the redirected method should be:</p>
 * 
 * <blockquote>
 *      <pre>public boolean barProxy(Foo someObject, int abc, int def)</pre>
 * </blockquote>
 * 
 * <p>For obvious reasons this does not apply for static methods, for static
 * methods it is sufficient that the signature simply match the hooked method.
 * </p>
 * 
 * <p>It is also possible to capture the arguments of the target method in
 * addition to the arguments being passed to the method call (for example in
 * the code above this would be the <em>someInt</em> and <em>someString</em>
 * arguments) by appending the arguments to the method signature:</p>
 * 
 * <blockquote>
 *      <pre>public boolean barProxy(Foo someObject, int abc, int def,
 *   int someInt, String someString)</pre>
 * </blockquote>
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Redirect {
    
    /**
     * String representation of a
     * {@link org.spongepowered.asm.mixin.injection.struct.MemberInfo
     * MemberInfo} which identifies the target method.
     * 
     * @return method to inject into
     */
    public String method();
    
    /**
     * An {@link At} annotation which describes the {@link InjectionPoint} in
     * the target method. The specified {@link InjectionPoint} <i>must only</i>
     * return {@link org.spongepowered.asm.lib.tree.MethodInsnNode} and an
     * exception will be thrown if this is not the case.
     * 
     * @return {@link At} which identifies the target method invocation
     */
    public At at();

    /**
     * By default, the annotation processor will attempt to locate an
     * obfuscation mapping for all {@link Redirect} methods since it is
     * anticipated that in general the target of a {@link Redirect} annotation
     * will be an obfuscated method in the target class. However since it is
     * possible to also apply mixins to non-obfuscated targets (or non-
     * obfuscated methods in obfuscated targets, such as methods added by Forge)
     * it may be necessary to suppress the compiler error which would otherwise
     * be generated. Setting this value to <em>false</em> will cause the
     * annotation processor to skip this annotation when attempting to build the
     * obfuscation table for the mixin.
     * 
     * @return True to instruct the annotation processor to search for
     *      obfuscation mappings for this annotation 
     */
    public boolean remap() default true;
}
