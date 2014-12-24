/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered.org <http://www.spongepowered.org>
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
 * Annotation for specifying the type of {@link InjectionPoint} to use to perform an {@link Inject} process. This annotation allows the
 * {@link InjectionPoint} class to be specified, as well as arguments to be passed to the {@link InjectionPoint} instance to configure it. The data
 * contained in the annotation are wrapped into a {@link org.spongepowered.asm.mixin.injection.struct.InjectionInfo InjectionInfo} object before
 * being passed to the {@link InjectionPoint} for parsing. All values are optional apart from {@link #value}, which specifies the type of
 * {@link InjectionPoint} to use. All other parameters depend on the InjectionPoint chosen, and the javadoc for each {@link InjectionPoint} class
 * should be consulted for the meaning of the argument to that particular class. A general description of each parameter is provided below. 
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface At {
    /**
     * <b>Shift</b> is used to shift resulting opcodes
     */
    public enum Shift {
        NONE,
        BEFORE,
        AFTER,
        BY
    }
    
    /**
     * <p>Type of {@link InjectionPoint} to use. Can be a built-in class or the fully-qualified name of a custom class which extends
     * {@link InjectionPoint}.</p>
     * 
     * <p>Built-in types are {@link org.spongepowered.asm.mixin.injection.points.MethodHead HEAD},
     * {@link org.spongepowered.asm.mixin.injection.points.BeforeReturn RETURN},
     * {@link org.spongepowered.asm.mixin.injection.points.BeforeInvoke INVOKE},
     * {@link org.spongepowered.asm.mixin.injection.points.BeforeFieldAccess FIELD},
     * {@link org.spongepowered.asm.mixin.injection.points.BeforeNew NEW},
     * {@link org.spongepowered.asm.mixin.injection.points.BeforeStringInvoke INVOKE_STRING} and
     * {@link org.spongepowered.asm.mixin.injection.points.JumpInsnPoint JUMP}. See the javadoc for each type for more details on the scheme used by
     * each injection point.</p>
     */
    public String value();
    
    /**
     * Shift type for returned opcodes. For example use {@link At.Shift#AFTER AFTER} with an INVOKE InjectionPoint to move the returned opcodes to
     * <i>after</i> the invoation. Use {@link At.Shift#BY BY} in conjunction with the {@link #by} parameter to shift by an arbitrary number of
     * opcodes. 
     */
    public Shift shift() default Shift.NONE;
    
    /**
     * If {@link #shift} is specified as {@link At.Shift#BY BY}, specifies the number of opcodes to shift by (negative numbers are allowed).
     */
    public int by() default 0;
    
    /**
     * <p>The <b>named arguments</b> list is used to expand the scope of the annotation beyond the fixed values below in order to accommodate the
     * needs of custom injection point classes.
     */
    public String[] args() default { };
    
    /**
     * Target member used by INVOKE, INVOKE_STRING and FIELD
     */
    public String target() default "";
    
    /**
     * Ordinal offset. Many InjectionPoints will return every opcode matching their criteria, specifying <em>ordinal</em> allows a particular opcode
     * to be identified from the returned list. The default value of -1 does not alter the behaviour and returns all matching opcodes. Specifying a
     * value of 0 or higher returns <em>only</em> the requested opcode (if one exists: for example specifying an ordinal of 4 when only 2 opcodes are
     * matched by the InjectionPoint is not going to work particularly well!) 
     */
    public int ordinal() default -1;
    
    /**
     * Target opcode for FIELD and JUMP InjectionPoints. See the javadoc for the relevant injection point for more details.
     */
    public int opcode() default -1;
}


