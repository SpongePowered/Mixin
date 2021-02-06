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
 * A Mixin marked as <tt>&#64;Pseudo</tt> is allowed to target classes which are
 * not available at <em>compile time</em> and may not be available at runtime.
 * This can be used in situations where - for one reason or another - a target
 * class is not available to a project being compiled and would therefore fail
 * AP verification. The target class is simulated by the AP using knowledge of
 * only the superclass of the target (or at least, any <em>available</em> known
 * superclass of the target. This means that certain restrictions apply:
 * 
 * <p>The superclass requirement for pseudo mixins is extremely important <b>if
 * the target has an obfuscated class in its hierarchy</b>. For example
 * let's assume that we're mixing into a class <tt>CustomGuiScreen</tt> from
 * another party which extends <tt>GuiScreen</tt>, where <tt>GuiScreen</tt> is
 * an obfuscated class. <tt>GuiScreen</tt> contains an obfuscated method
 * <tt>initGui</tt> which the (&#64;Pseudo) target class overrides. Attempting
 * to inject into <tt>initGui</tt> would succeed at dev time but would fail
 * at production time, because the reference is obfuscated. Normally when a
 * target overrides an obfuscated method in this way, the AP can resolve the
 * obfuscation by walking the superclass hierarchy of the target in order to
 * discover a mapping. However when the target is not available at compile time
 * the AP cannot do this and must rely on only information from the mixin
 * itself. We can overcome this problem by ensuring the mixin inherits from the
 * same superclass (or at least a superclass which contains the obfuscated 
 * methods or fields used in the mixin), thus allowing our example <tt>initGui
 * </tt> method to be resolved in the superclass hierarchy. This behaviour is
 * not available to normal mixins as the AP always resolves the hierarchy via
 * the target class metadata when it is available.</p>
 * 
 * <p>If the target class contains obfuscated methods which the mixin needs to
 * {@link Overwrite} or {@link Shadow} which are <b>not</b> inherited from a
 * superclass (eg. the target is obfuscated), the {@link Overwrite} or
 * {@link Shadow} <b>must</b> be decorated manually with <tt>aliases</tt> since
 * there is no mechanism for the AP to resolve the mappings automatically.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Pseudo {

}
