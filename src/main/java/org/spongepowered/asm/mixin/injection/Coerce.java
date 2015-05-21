/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
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
package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;


/**
 * Indicates that a local capture injector should coerce top-level primitive
 * types to covariant types.
 * 
 * <p>During LVT generation it is not always possible to inflect the exact local
 * type for types represented internally as integers, for example booleans and
 * shorts. However adding a surrogate for these cases is overkill when the type
 * is known for certain by the injector. Since the bytecode for all types stored
 * as integer interally will be valid, we can force the local type to any
 * covariant type as long as we know this in advance.</p>
 * 
 * <p>This annotation allows a covariant type parameter to be marked, and thus
 * coerced to the correct type when the LVT generation would otherwise mark the
 * type as invalid.</p>
 */
@Target({ ElementType.PARAMETER })
public @interface Coerce {
    
}
