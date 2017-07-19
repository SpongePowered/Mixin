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

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.points.*;

/**
 * Annotation for specifying the type of {@link InjectionPoint} to use to
 * perform an {@link Inject} process. This annotation allows the
 * {@link InjectionPoint} class to be specified, as well as arguments to be
 * passed to the {@link InjectionPoint} instance to configure it. The data
 * contained in the annotation are wrapped into a
 * {@link org.spongepowered.asm.mixin.injection.struct.InjectionInfo
 * InjectionInfo} object before being passed to the {@link InjectionPoint} for
 * parsing. All values are optional apart from {@link #value}, which specifies
 * the type of {@link InjectionPoint} to use. All other parameters depend on the
 * InjectionPoint chosen, and the javadoc for each {@link InjectionPoint} class
 * should be consulted for the meaning of the argument to that particular class.
 * A general description of each parameter is provided below. 
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface At {
    
    /**
     * <b>Shift</b> is used to shift resulting opcodes
     */
    public enum Shift {
        
        /**
         * Do not shift the returned opcodes
         */
        NONE,
        
        /**
         * Shift the returned opcodes back one instruction 
         */
        BEFORE,
        
        /**
         * Shift the returned opcodes forward one instruction 
         */
        AFTER,
        
        /**
         * Shift the returned opcodes by the amount specified in {@link At#by} 
         */
        BY
        
    }
    
    /**
     * The identifier for this injection point, can be retrieved via the
     * {@link CallbackInfo#getId} accessor. If specified, the ID is appended to
     * the value specified in the outer annotion. Eg. specifying "foo" for this
     * attribute and "bar" for the <tt>Inject.{@link Inject#id}</tt> attribute
     * will result in a combined id of <tt>"bar:foo"</tt>. Note that if no id
     * is specified for the outer injector, the name of the calling method is
     * used.
     * 
     * @return the injection point id to use
     */
    public String id() default "";

    /**
     * <p>Type of {@link InjectionPoint} to use. Can be a built-in class or the
     * fully-qualified name of a custom class which extends
     * {@link InjectionPoint}.</p>
     * 
     * <p>Built-in types are
     * {@link MethodHead HEAD},
     * {@link BeforeReturn RETURN},
     * {@link BeforeFinalReturn TAIL},
     * {@link BeforeInvoke INVOKE},
     * {@link AfterInvoke INVOKE_ASSIGN},
     * {@link BeforeFieldAccess FIELD},
     * {@link BeforeNew NEW},
     * {@link BeforeStringInvoke INVOKE_STRING},
     * {@link JumpInsnPoint JUMP} and
     * {@link BeforeConstant CONSTANT}.
     * See the javadoc for each type for more details on the scheme used by each
     * injection point.</p>
     * 
     * @return Injection point specifier or fully-qualified class name
     */
    public String value();
    
    /**
     * For {@link Inject} queries, this specifies the ID of the slice to use for
     * this query. For other injector types it is ignored because only one slice
     * is supported.
     * 
     * <p>For more details see the {@link Slice#id}</p>
     * 
     * @return the slice identifier, or empty string to use the default slice
     */
    public String slice() default "";
    
    /**
     * Shift type for returned opcodes. For example use {@link At.Shift#AFTER
     * AFTER} with an INVOKE InjectionPoint to move the returned opcodes to
     * <i>after</i> the invoation. Use {@link At.Shift#BY BY} in conjunction
     * with the {@link #by} parameter to shift by an arbitrary number of
     * opcodes. 
     * 
     * @return Type of shift to apply
     */
    public Shift shift() default Shift.NONE;
    
    /**
     * If {@link #shift} is specified as {@link At.Shift#BY BY}, specifies the
     * number of opcodes to shift by (negative numbers are allowed). Note that
     * values above <tt>3</tt> should be avoided and in general either replaced
     * with a custom injection point or with sliced injection points. The
     * warning/error threshold is defined by the config (with a hard limit on
     * value of {@link InjectionPoint#MAX_ALLOWED_SHIFT_BY})
     * 
     * @return Amount of shift to apply for the {@link At.Shift#BY BY} shift
     */
    public int by() default 0;
    
    /**
     * <p>The <b>named arguments</b> list is used to expand the scope of the
     * annotation beyond the fixed values below in order to accommodate the
     * needs of custom injection point classes.</p>
     * 
     * @return Named arguments for the injection point
     */
    public String[] args() default { };
    
    /**
     * Target identifier used by INVOKE, INVOKE_STRING, INVOKE_ASSIGN, FIELD and
     * NEW. This <b>must be specified as a fully-qualified member path</b>
     * including the class name and signature. Failing to fully-qualify the
     * target member will result in an error at obfuscation time.
     * 
     * @return target reference for supported InjectionPoint types
     */
    public String target() default "";
    
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
     * Target opcode for FIELD and JUMP InjectionPoints. See the javadoc for the
     * relevant injection point for more details.
     * 
     * @return Bytecode opcode for supported InjectionPoints
     */
    public int opcode() default -1;
    
    /**
     * By default, the annotation processor will attempt to locate an
     * obfuscation mapping for the {@link #target} member and any other
     * {@link #args} known to contain potentially obfuscated references, since
     * it is anticipated that in general the target of an {@link At} annotation
     * will be an obfuscated method or field. However since it is also possible
     * that the target is a non-obfuscated reference it may be necessary to
     * suppress the compiler error which would otherwise be generated. Setting
     * this value to <em>false</em> will cause the annotation processor to skip
     * this annotation when attempting to build the obfuscation table for the
     * mixin.
     * 
     * @return True to instruct the annotation processor to search for
     *      obfuscation mappings for this annotation 
     */
    public boolean remap() default true;
}
