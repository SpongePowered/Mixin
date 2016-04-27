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

import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.util.Constants;

/**
 * For callbacks with a non-void return type, a CallbackInfoReturnable is passed
 * to the callback instead to allow the callback to interact with the method
 * return value.
 * 
 * @param <R> Return type
 */
public class CallbackInfoReturnable<R> extends CallbackInfo {

    private R returnValue;

    public CallbackInfoReturnable(String name, boolean cancellable) {
        super(name, cancellable);
        this.returnValue = null;
    }

    public CallbackInfoReturnable(String name, boolean cancellable, R returnValue) {
        super(name, cancellable);
        this.returnValue = returnValue;
    }

    @SuppressWarnings("unchecked")
    public CallbackInfoReturnable(String name, boolean cancellable, byte returnValue) {
        super(name, cancellable);
        this.returnValue = (R) Byte.valueOf(returnValue);
    }

    @SuppressWarnings("unchecked")
    public CallbackInfoReturnable(String name, boolean cancellable, char returnValue) {
        super(name, cancellable);
        this.returnValue = (R) Character.valueOf(returnValue);
    }

    @SuppressWarnings("unchecked")
    public CallbackInfoReturnable(String name, boolean cancellable, double returnValue) {
        super(name, cancellable);
        this.returnValue = (R) Double.valueOf(returnValue);
    }

    @SuppressWarnings("unchecked")
    public CallbackInfoReturnable(String name, boolean cancellable, float returnValue) {
        super(name, cancellable);
        this.returnValue = (R) Float.valueOf(returnValue);
    }

    @SuppressWarnings("unchecked")
    public CallbackInfoReturnable(String name, boolean cancellable, int returnValue) {
        super(name, cancellable);
        this.returnValue = (R) Integer.valueOf(returnValue);
    }

    @SuppressWarnings("unchecked")
    public CallbackInfoReturnable(String name, boolean cancellable, long returnValue) {
        super(name, cancellable);
        this.returnValue = (R) Long.valueOf(returnValue);
    }

    @SuppressWarnings("unchecked")
    public CallbackInfoReturnable(String name, boolean cancellable, short returnValue) {
        super(name, cancellable);
        this.returnValue = (R) Short.valueOf(returnValue);
    }

    @SuppressWarnings("unchecked")
    public CallbackInfoReturnable(String name, boolean cancellable, boolean returnValue) {
        super(name, cancellable);
        this.returnValue = (R) Boolean.valueOf(returnValue);
    }

    /**
     * Sets a return value for this callback and cancels the callback (required
     * in order to return the new value)
     * 
     * @param returnValue value to return
     */
    public void setReturnValue(R returnValue) throws CancellationException {
        super.cancel();

        this.returnValue = returnValue;
    }

    public R getReturnValue() {
        return this.returnValue;
    }

    // All of the accessors below are to avoid having to generate unboxing conversions in bytecode
    // CHECKSTYLE:OFF
    public byte    getReturnValueB() { if (this.returnValue == null) { return 0;     } return (Byte)     this.returnValue; }
    public char    getReturnValueC() { if (this.returnValue == null) { return 0;     } return (Character)this.returnValue; }
    public double  getReturnValueD() { if (this.returnValue == null) { return 0.0;   } return (Double)   this.returnValue; }
    public float   getReturnValueF() { if (this.returnValue == null) { return 0.0F;  } return (Float)    this.returnValue; }
    public int     getReturnValueI() { if (this.returnValue == null) { return 0;     } return (Integer)  this.returnValue; }
    public long    getReturnValueJ() { if (this.returnValue == null) { return 0;     } return (Long)     this.returnValue; }
    public short   getReturnValueS() { if (this.returnValue == null) { return 0;     } return (Short)    this.returnValue; }
    public boolean getReturnValueZ() { if (this.returnValue == null) { return false; } return (Boolean)  this.returnValue; }
    // CHECKSTYLE:ON

    static String getReturnAccessor(Type returnType) {
        if (returnType.getSort() == Type.OBJECT || returnType.getSort() == Type.ARRAY) {
            return "getReturnValue";
        }

        return String.format("getReturnValue%s", returnType.getDescriptor());
    }

    static String getReturnDescriptor(Type returnType) {
        if (returnType.getSort() == Type.OBJECT || returnType.getSort() == Type.ARRAY) {
            return String.format("()%s", Constants.OBJECT);
        }

        return String.format("()%s", returnType.getDescriptor());
    }
}
