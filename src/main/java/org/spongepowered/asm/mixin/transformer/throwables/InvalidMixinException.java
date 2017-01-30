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
package org.spongepowered.asm.mixin.transformer.throwables;

import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.throwables.MixinException;

/**
 * Thrown by the mixin validator when a mixin fails a pre-flight check
 */
public class InvalidMixinException extends MixinException {

    private static final long serialVersionUID = 2L;
    
    private final IMixinInfo mixin;

    public InvalidMixinException(IMixinInfo mixin, String message) {
        super(message);
        this.mixin = mixin;
    }
    
    public InvalidMixinException(IMixinContext context, String message) {
        this(context.getMixin(), message);
    }

    public InvalidMixinException(IMixinInfo mixin, Throwable cause) {
        super(cause);
        this.mixin = mixin;
    }

    public InvalidMixinException(IMixinContext context, Throwable cause) {
        this(context.getMixin(), cause);
    }
    
    public InvalidMixinException(IMixinInfo mixin, String message, Throwable cause) {
        super(message, cause);
        this.mixin = mixin;
    }
    
    public InvalidMixinException(IMixinContext context, String message, Throwable cause) {
        super(message, cause);
        this.mixin = context.getMixin();
    }
    
    public IMixinInfo getMixin() {
        return this.mixin;
    }
}
