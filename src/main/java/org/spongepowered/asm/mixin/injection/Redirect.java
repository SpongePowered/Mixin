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
 * Specifies that this mixin method should redirect the specified {@link #method} call to the method decorated with this annotation.
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Redirect {
    
    /**
     * String representation of a {@link org.spongepowered.asm.mixin.injection.struct.MemberInfo MemberInfo} which identifies the target method.
     */
    public String method();
    
    /**
     * An {@link At} annotation which describes the {@link InjectionPoint} in the target method. The specified {@link InjectionPoint} <em>must only
     * </em> return nodes of type {@link org.objectweb.asm.tree.MethodInsnNode} and an exception will be thrown if this is not the case.
     */
    public At at();

    /**
     * By default, the annotation processor will attempt to locate an obfuscation mapping for all {@link ModifyArg} methods since it is anticipated
     * that in general the target of a {@link ModifyArg} annotation will be an obfuscated method in the target class. However since it is possible to
     * also apply mixins to non-obfuscated targets (or non-obfuscated methods in obfuscated targets, such as methods added by Forge) it may be
     * necessary to suppress the compiler error which would otherwise be generated. Setting this value to <em>false</em> will cause the annotation
     * processor to skip this annotation when attempting to build the obfuscation table for the mixin.
     */
    public boolean remap() default true;
}
