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
package org.spongepowered.asm.mixin.injection.throwables;

import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.refmap.IMixinContext;

/**
 * Thrown when an injection point cannot be parsed due to invalid data
 */
public class InvalidInjectionPointException extends InvalidInjectionException {

    private static final long serialVersionUID = 2L;
    
    public InvalidInjectionPointException(IMixinContext context, String format, Object... args) {
        super(context, String.format(format, args));
    }
    
    public InvalidInjectionPointException(InjectionInfo info, String format, Object... args) {
        super(info, String.format(format, args));
    }
    
    public InvalidInjectionPointException(IMixinContext context, Throwable cause, String format, Object... args) {
        super(context, String.format(format, args), cause);
    }

    public InvalidInjectionPointException(InjectionInfo info, Throwable cause, String format, Object... args) {
        super(info, String.format(format, args), cause);
    }
    
}
