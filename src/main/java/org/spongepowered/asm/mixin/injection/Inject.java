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
 * Specifies that this mixin method should inject a callback (or callback<b>s</b>) to itself in the target method(s) identified by {@link #method}.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Inject {
    
    /**
     * String representation of a {@link org.spongepowered.asm.mixin.injection.struct.MemberInfo MemberInfo} which identifies the target method.
     */
    public String method();
    
    /**
     * Array of {@link At} annotations which describe the {@link InjectionPoint}s in the target method. Allows one or more callbacks to be injected in
     * the target method
     */
    public At[] at();
    
    /**
     * Setting an injected callback to <em>cancellable</em> allows the injected callback to inject optional RETURN opcodes into the target method, the
     * return behaviour can then be controlled from within the callback by interacting with the supplied
     * {@link org.spongepowered.asm.mixin.injection.callback.CallbackInfo} object.
     */
    public boolean cancellable() default false;
    
    /**
     * Set to true to allow local variables to be captured from the target method as well as method parameters.
     */
//    public boolean captureLocals() default false;
}
