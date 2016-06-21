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

import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;

/**
 * This annotation has two uses:
 * 
 * <ul>
 *   <li>
 *     On an {@link Shadow} field, it can be used to raise an error-level log
 *     message if any write occurrences appear in the mixin bytecode. This can
 *     be used in place of declaring the field as actually <tt>final</tt>. This
 *     is required since it is normally desirable to remove the <tt>final</tt>
 *     modifier from shadow fields to avoid unwanted field initialisers. If
 *     {@link Option#DEBUG_VERIFY} is <tt>true</tt>, then an
 *     {@link InvalidMixinException} is thrown.
 *   </li>
 *   <li>
 *     On an {@link Inject injector} or {@link Overwrite overwritten} method,
 *     it is equivalent to setting the priority of the containing mixin to
 *     {@link Integer#MAX_VALUE} but applies only to the annotated method. This
 *     allows methods to mark themselves as effectively final, preventing their
 *     replacement by later mixins with higher priority.
 *   </li>
 * </ul>
 */
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Final {

}
