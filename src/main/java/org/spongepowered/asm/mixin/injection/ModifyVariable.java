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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.injection.throwables.InjectionError;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.ConstraintParser.Constraint;

/**
 * Specifies that this mixin method should inject a variable modifier callback
 * to itself in the target method(s) identified by {@link #method}.
 * 
 * <p><tt>ModifyVariable</tt> callbacks should always take one argument of the
 * type to capture and return the same type. For example a <tt>ModifyVariable
 * </tt> for a local of type {@link String} should have the signature:</p>
 * 
 * <code>private String methodName(String variable) { ...</code>
 * 
 * <p>The callback receives the current value of the local variable, and should
 * return the new value.</p>
 * 
 * <p>The injector has two operating modes, <em>explicit</em> and <em>implicit
 * </em>, and can operate either on the entire LVT or on the method arguments
 * only</p>.
 * 
 * <p>In <em>explicit</em> mode, the variable to capture can be specified by
 * specifying values for the discriminator arguments {@link #ordinal},
 * {@link #index} and {@link #name}. The injector uses the discriminators in 
 * order to attempt to locate the variable to capture. If no local variable
 * matches any discriminators, the capture fails.</p>
 * 
 * <p>If no values for the capture discrimiators are specified, the injector
 * operates in <em>implicit</em> mode. If exactly one variable of the capture
 * type exists in the target LVT, then capture will succeed. However, if more
 * than one variable of the required type is encountered in the LVT then an
 * {@link InvalidInjectionException} is thrown.</p> 
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ModifyVariable {
    
    /**
     * String representation of one or more
     * {@link org.spongepowered.asm.mixin.injection.struct.MemberInfo 
     * MemberInfo} which identify the target methods.
     * 
     * @return target method(s) for this injector
     */
    public String[] method();
    
    /**
     * A {@link Slice} annotation which describes the method bisection used in
     * the {@link #at} query for this injector.
     * 
     * @return slice
     */
    public Slice slice() default @Slice;

    /**
     * An {@link At} annotation which describes the {@link InjectionPoint} in
     * the target method. The specified {@link InjectionPoint} <i>must only</i>
     * return {@link org.spongepowered.asm.lib.tree.MethodInsnNode} instances
     * and an exception will be thrown if this is not the case.
     * 
     * @return {@link At} which identifies the target method invocation
     */
    public At at();
    
    /**
     * When creating a {@link ModifyVariable} callback, you may wish to first
     * inspect the local variable table in the target method at the injection
     * point. Set this value to <tt>true</tt> to print the local variable table
     * and perform no injection.
     * 
     * @return true to print the LVT to the console
     */
    public boolean print() default false;
    
    /**
     * Gets the local variable ordinal by type. For example, if there are 3
     * {@link String} arguments in the local variable table, ordinal 0 specifies
     * the first, 1 specifies the second, etc. Use <tt>ordinal</tt> when the
     * index within the LVT is known. Takes precedence over {@link #index}.
     * 
     * @return variable ordinal
     */
    public int ordinal() default -1;
    
    /**
     * Gets the absolute index of the local variable within the local variable
     * table to capture. The local variable at the specified index must be of
     * the same type as the capture. Takes precedence over {@link #name}.
     * 
     * @return argument index to modify or -1 for automatic
     */
    public int index() default -1;
    
    /**
     * Gets the name of the variable to capture. Only used if the variable
     * cannot be located via {@link #ordinal} or {@link #index}.
     * 
     * @return possible names to capture, only useful when the LVT in the target
     *      method is known to be complete.
     */
    public String[] name() default {};

    /**
     * Under normal circumstances the injector will consider all local variables
     * including method arguments and all other local variables. This involves
     * reading (and possibly generating) the local variable table for the method
     * which can have mixed results. Set this value to <tt>true</tt> to <i>only
     * </i> consider method arguments.
     * 
     * @return true if this injector should only consider method arguments and
     *      not all locals.
     */
    public boolean argsOnly() default false;
    
    /**
     * By default, the annotation processor will attempt to locate an
     * obfuscation mapping for all {@link ModifyVariable} methods since it is
     * anticipated that in general the target of a {@link ModifyVariable}
     * annotation will be an obfuscated method in the target class. However 
     * since it is possible to also apply mixins to non-obfuscated targets (or
     * non- obfuscated methods in obfuscated targets, such as methods added by
     * Forge) it may be necessary to suppress the compiler error which would
     * otherwise be generated. Setting this value to <em>false</em> will cause
     * the annotation processor to skip this annotation when attempting to build
     * the obfuscation table for the mixin.
     * 
     * @return True to instruct the annotation processor to search for
     *      obfuscation mappings for this annotation 
     */
    public boolean remap() default true;
    
    /**
     * In general, injectors are intended to "fail soft" in that a failure to
     * locate the injection point in the target method is not considered an
     * error condition. Another transformer may have changed the method
     * structure or any number of reasons may cause an injection to fail. This
     * also makes it possible to define several injections to achieve the same
     * task given <em>expected</em> mutation of the target class and the
     * injectors which fail are simply ignored.
     * 
     * <p>However, this behaviour is not always desirable. For example, if your
     * application depends on a particular injection succeeding you may wish to
     * detect the injection failure as an error condition. This argument is thus
     * provided to allow you to stipulate a <b>minimum</b> number of successful
     * injections for this callback handler. If the number of injections
     * specified is not achieved then an {@link InjectionError} is thrown at
     * application time. Use this option with care.</p>
     * 
     * @return Minimum required number of injected callbacks, default specified
     *      by the containing config
     */
    public int require() default -1;
    
    /**
     * Like {@link #require()} but only enabled if the
     * {@link Option#DEBUG_INJECTORS mixin.debug.countInjections} option is set
     * to <tt>true</tt> and defaults to 1. Use this option during debugging to
     * perform simple checking of your injectors. Causes the injector to throw
     * a {@link InvalidInjectionException} if the expected number of injections
     * is not realised.
     * 
     * @return Minimum number of <em>expected</em> callbacks, default 1
     */
    public int expect() default 1;
    
    /**
     * Injection points are in general expected to match every candidate
     * instruction in the target method or slice, except in cases where options
     * such as {@link At#ordinal} are specified which naturally limit the number
     * of results.
     * 
     * <p>This option allows for sanity-checking to be performed on the results
     * of an injection point by specifying a maximum allowed number of matches,
     * similar to that afforded by {@link Group#max}. For example if your
     * injection is expected to match 4 invocations of a target method, but
     * instead matches 5, this can become a detectable tamper condition by
     * setting this value to <tt>4</tt>.
     * 
     * <p>Setting any value 1 or greater is allowed. Values less than 1 or less
     * than {@link #require} are ignored. {@link #require} supercedes this
     * argument such that if <tt>allow</tt> is less than <tt>require</tt> the
     * value of <tt>require</tt> is always used.</p>
     * 
     * <p>Note that this option is not a <i>limit</i> on the query behaviour of
     * this injection point. It is only a sanity check used to ensure that the
     * number of matches is not too high 
     * 
     * @return Maximum allowed number of injections for this 
     */
    public int allow() default -1;

    /**
     * Returns constraints which must be validated for this injector to
     * succeed. See {@link Constraint} for details of constraint formats.
     * 
     * @return Constraints for this annotation
     */
    public String constraints() default "";

}
