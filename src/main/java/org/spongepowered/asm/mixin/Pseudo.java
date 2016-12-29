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
 * A Mixin marked as <b>Pseudo</b> is allowed to target classes which are not
 * available at compile time and may not be available at runtime. This means
 * that certain restrictions apply:
 * 
 * <p>In particular, the superclass requirement for pseudo mixins is extremely
 * important if the target has an obfuscated class in its hierarchy. For example
 * let's assume that we're mixing into a class <tt>SomeCustomScreen</tt> from
 * another party which extends <tt>GuiScreen</tt> which is obfuscated.
 * Attempting to inject into <tt>initGui</tt> will succeed at dev time and fail
 * at production time, because the reference is obfuscated. We can overcome this
 * by ensuring the mixin inherits from the same superclass, thus allowing
 * <tt>initGui</tt> to be resolved in the superclass hierarchy (this is not the
 * case for normal mixins).</p>
 * 
 * <p>{@link Overwrite} methods which are <b>not</b> inherited from a superclass
 * (if the target is obfuscated) <b>must</b> be decorated manually with aliases.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Pseudo {

}
