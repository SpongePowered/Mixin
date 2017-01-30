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

import org.spongepowered.asm.mixin.injection.code.ISliceContext;
import org.spongepowered.asm.mixin.refmap.IMixinContext;

/**
 * Thrown when an injector slice fails a state check, for example if a slice
 * selector fails or a slice is negative in size
 */
public class InvalidSliceException extends InvalidInjectionException {

    private static final long serialVersionUID = 1L;
    
    public InvalidSliceException(IMixinContext context, String message) {
        super(context, message);
    }

    public InvalidSliceException(ISliceContext owner, String message) {
        super(owner.getContext(), message);
    }

    public InvalidSliceException(IMixinContext context, Throwable cause) {
        super(context, cause);
    }

    public InvalidSliceException(ISliceContext owner, Throwable cause) {
        super(owner.getContext(), cause);
    }

    public InvalidSliceException(IMixinContext context, String message, Throwable cause) {
        super(context, message, cause);
    }

    public InvalidSliceException(ISliceContext owner, String message, Throwable cause) {
        super(owner.getContext(), message, cause);
    }

}
