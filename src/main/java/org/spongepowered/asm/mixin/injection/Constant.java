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
package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation for specifying the injection point for an {@link ModifyConstant}
 * injector. Leaving all values unset causes the injection point to match all
 * constants with the same type as the {@link ModifyConstant} handler's return
 * type.
 * 
 * <p>To match a specific constant, specify the appropriate value for the
 * appropriate argument. Specifying values of different types will cause an
 * error to be raised by the injector.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Constant {
    
    /**
     * Causes this injector to match ACONST_NULL (null object) literals
     * 
     * @return true to match <tt>null</tt>
     */
    public boolean nullValue() default false;

    /**
     * Specify an integer constant to match, includes byte and short values
     * 
     * @return integer value to match
     */
    public int intValue() default 0;
    
    /**
     * Specify a float constant to match
     * 
     * @return float value to match
     */
    public float floatValue() default 0.0F;
    
    /**
     * Specify a long constant to match
     * 
     * @return long value to match
     */
    public long longValue() default 0L;
    
    /**
     * Specify a double constant to match
     * 
     * @return double value to match
     */
    public double doubleValue() default 0.0;
    
    /**
     * Specify a String constant to match
     * 
     * @return string value to match
     */
    public String stringValue() default "";
    
    /**
     * Specify a type literal to match
     * 
     * @return type literal to match
     */
    public Class<?> classValue() default Object.class;
    
    /**
     * Ordinal offset. Many InjectionPoints will return every opcode matching
     * their criteria, specifying <em>ordinal</em> allows a particular opcode to
     * be identified from the returned list. The default value of -1 does not
     * alter the behaviour and returns all matching opcodes. Specifying a value
     * of 0 or higher returns <em>only</em> the requested opcode (if one exists:
     * for example specifying an ordinal of 4 when only 2 opcodes are matched by
     * the InjectionPoint is not going to work particularly well!)
     * 
     * @return ordinal value for supported InjectionPoint types
     */
    public int ordinal() default -1;
    
    /**
     * @return true to enable verbose debug logging for this injection point
     */
    public boolean log() default false;
}


