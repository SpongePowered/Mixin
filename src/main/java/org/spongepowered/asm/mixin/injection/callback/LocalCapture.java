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
package org.spongepowered.asm.mixin.injection.callback;

/**
 * Specifies the behaviour for capturing local variables at an injection point.
 * 
 * <p>Since local capture relies on calculating the local variable table for the
 * target method it is disabled by default for performance reasons. When
 * capturing is enabled, local variables are passed to the handler method after
 * the {@link CallbackInfo} argument. Since it is entirely possible for another
 * transformer to make an incompatible change to the the local variable table at
 * run time, the purpose of this enum is to specify the behaviour for local
 * capture and the type of recovery to be performed when an incompatible change
 * is detected.</p>
 */
public enum LocalCapture {
    
    /**
     * Do not capture locals, this is the default behaviour
     */
    NO_CAPTURE(false, false),
    
    /**
     * Do not capture locals. Print the expected method signature to stderr
     * instead.
     */
    PRINT(false, true),

    /**
     * Capture locals. If the calculated locals are different from the expected
     * values, log a warning and skip this injection.
     */
    CAPTURE_FAILSOFT,
    
    /**
     * Capture locals. If the calculated locals are different from the expected
     * values, throw an {@link Error}.
     */
    CAPTURE_FAILHARD,
    
    /**
     * Capture locals. If the calculated locals are different from the expected
     * values, generate a method stub containing an exception. This will allow
     * normal execution to continue unless the callback is encountered.
     */
    CAPTURE_FAILEXCEPTION;
    
    private final boolean captureLocals;
    
    private final boolean printLocals;
    
    private LocalCapture() {
        this(true, false);
    }

    private LocalCapture(boolean captureLocals, boolean printLocals) {
        this.captureLocals = captureLocals;
        this.printLocals = printLocals;
    }

    boolean isCaptureLocals() {
        return this.captureLocals;
    }
    
    boolean isPrintLocals() {
        return this.printLocals;
    }
}
