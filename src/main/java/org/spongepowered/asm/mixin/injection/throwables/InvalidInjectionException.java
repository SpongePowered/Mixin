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

import org.spongepowered.asm.mixin.extensibility.IActivityContext;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;

/**
 * Thrown when an injector fails a state check, for example if an injector
 * handler signature is invalid, an invalid opcode is targetted, etc.
 */
public class InvalidInjectionException extends InvalidMixinException {

    private static final long serialVersionUID = 2L;
    
    private final ISelectorContext selectorContext;

    public InvalidInjectionException(IMixinContext context, String message) {
        super(context, message);
        this.selectorContext = null;
    }

    public InvalidInjectionException(IMixinContext context, String message, IActivityContext activityContext) {
        super(context, message, activityContext);
        this.selectorContext = null;
    }

    public InvalidInjectionException(ISelectorContext selectorContext, String message) {
        super(selectorContext.getMixin(), message);
        this.selectorContext = selectorContext;
    }

    public InvalidInjectionException(ISelectorContext selectorContext, String message, IActivityContext activityContext) {
        super(selectorContext.getMixin(), message, activityContext);
        this.selectorContext = selectorContext;
    }

    public InvalidInjectionException(IMixinContext context, Throwable cause) {
        super(context, cause);
        this.selectorContext = null;
    }

    public InvalidInjectionException(IMixinContext context, Throwable cause, IActivityContext activityContext) {
        super(context, cause, activityContext);
        this.selectorContext = null;
    }

    public InvalidInjectionException(ISelectorContext selectorContext, Throwable cause) {
        super(selectorContext.getMixin(), cause);
        this.selectorContext = selectorContext;
    }

    public InvalidInjectionException(ISelectorContext selectorContext, Throwable cause, IActivityContext activityContext) {
        super(selectorContext.getMixin(), cause, activityContext);
        this.selectorContext = selectorContext;
    }

    public InvalidInjectionException(IMixinContext context, String message, Throwable cause) {
        super(context, message, cause);
        this.selectorContext = null;
    }

    public InvalidInjectionException(IMixinContext context, String message, Throwable cause, IActivityContext activityContext) {
        super(context, message, cause, activityContext);
        this.selectorContext = null;
    }

    public InvalidInjectionException(ISelectorContext selectorContext, String message, Throwable cause) {
        super(selectorContext.getMixin(), message, cause);
        this.selectorContext = selectorContext;
    }
    
    public InvalidInjectionException(ISelectorContext selectorContext, String message, Throwable cause, IActivityContext activityContext) {
        super(selectorContext.getMixin(), message, cause, activityContext);
        this.selectorContext = selectorContext;
    }
    
    public ISelectorContext getContext() {
        return this.selectorContext;
    }
    
}
