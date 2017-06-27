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
package org.spongepowered.asm.mixin.injection.invoke.arg;

import org.spongepowered.asm.mixin.injection.ModifyArgs;

/**
 * Argument bundle class used in {@link ModifyArgs} callbacks. See the
 * documentation for {@link ModifyArgs} for details. Synthetic subclasses are
 * generated at runtime for specific injectors. 
 */
public abstract class Args {
    
    /**
     * Argument values
     */
    protected final Object[] values;

    /**
     * Ctor.
     * 
     * @param values argument values
     */
    protected Args(Object[] values) {
        this.values = values;
    }
    
    /**
     * Return the argument list size.
     * 
     * @return number of arguments available
     */
    public int size() {
        return this.values.length;
    }

    /**
     * Retrieve the argument value at the specified index
     * 
     * @param index argument index to retrieve
     * @param <T> the argument type
     * @return the argument value
     * @throws ArrayIndexOutOfBoundsException if a value outside the range of
     *      available arguments is accessed
     */
    @SuppressWarnings("unchecked")
    public <T> T get(int index) {
        return (T)this.values[index];
    }

    /**
     * Set (modify) the specified argument value. Internal verification is
     * performed upon supplied values and the following requirements are
     * enforced:
     * 
     * <ul>
     *   <li>Reference types must be assignable to the object type, or can be
     *      <tt>null</tt>.
     *   <li>Primitive types must match the target types exactly and <b>cannot
     *      </b> be <tt>null</tt>.
     * </ul>
     * 
     * @param index Argument index to set
     * @param value Argument value
     * @param <T> Argument type
     * @throws ArgumentIndexOutOfBoundsException if the specified argument index
     *      is outside the range of available arguments
     */
    public abstract <T> void set(int index, T value);

    /**
     * Set (modify) all argument values. The number and type of arguments
     * supplied to this method must precisely match the argument types in the
     * bundle. See {@link #set(int, Object)} for details.
     * 
     * @param values Argument values to set
     */
    public abstract void setAll(Object... values);
    
}