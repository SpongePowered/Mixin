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
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.throwables.InjectionError;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.ConstraintParser.Constraint;

/**
 * Specifies that this mixin method should inject an argument modifier to itself
 * in the target method(s) identified by {@link #method}. This type of injection
 * provides a lightweight mechanism for changing a single argument of a selected
 * method invocation within the target method(s). To affect multiple arguments
 * of an invocation all at once, use {@link ModifyArgs} instead.
 * 
 * <p>Use this injector when a (target) method contains an method invocation
 * (the subject) and you wish to change a single value being <em>passed to</em>
 * that subject method. If you need to alter an argument <em>received by</em> a
 * target method, use {@link ModifyVariable} instead.</p>
 * 
 * <p>Consider the following method:</p>
 * 
 * <blockquote><pre><code>private void targetMethod() {
 *    Entity someEntity = this.obtainEntity();
 *    float x = 1.0F, y = 3.0F, z = 0.1F;
 *    someEntity.<ins>setLocation</ins>(x, <ins>y</ins>, z, true); // subject
 *}</code></pre>
 *</blockquote>
 * 
 * <p>Let us assume that we wish to modify the <ins><tt>y</tt></ins> value when
 * calling the <ins><tt>setLocation</tt></ins> method. We know that the
 * arguments are <tt>float</tt>s and that the <tt>y</tt> value is the <em>second
 * </em> (index = 1) <tt>float</tt> argument. Thus our injector requires the
 * following signature:
 *  
 * <blockquote><code>&#064;ModifyArg(method = "targetMethod", at = &#64;At(value
 * = "INVOKE", target = "<ins>setLocation(FFFZ)V</ins>"), index = 1)<br />
 * private float adjustYCoord(float y) {<br />
 * &nbsp; &nbsp; return y + 64.0F;<br />
 * }</code></blockquote>
 * 
 * <p>The callback consumes the original value of <tt>y</tt> and returns the
 * adjusted value.</p>
 * 
 * <p><tt>&#064;ModifyArg</tt> can also consume all of the subject method's
 * arguments if required, to provide additional context for the callback. In
 * this case the arguments of the callback should match the target method:</p> 
 *  
 * <blockquote><code>&#064;ModifyArg(method = "targetMethod", at = &#64;At(value
 * = "INVOKE", target = "<ins>setLocation(FFFZ)V</ins>"), index = 1)<br />
 * private float adjustYCoord(float x, float y, float z, boolean interpolate) {
 * <br />&nbsp; &nbsp; return (x == 0 &amp;&amp; y == 0) ? 0 : y;<br />
 * }</code></blockquote>
 * 
 * <p>Note that <tt>&#064;ModifyArg</tt> <em>cannot</em> capture the arguments
 * of the <em>target</em> method like some other injectors can
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ModifyArg {
    
    /**
     * String representation of one or more
     * {@link ITargetSelector target selectors} which identify the target
     * methods.
     * 
     * @return target method(s) for this injector
     */
    public String[] method() default {};
    
    /**
     * Literal representation of one or more {@link Desc &#064;Desc} annotations
     * which identify the target methods.
     * 
     * @return target method(s) for this injector as descriptors
     */
    public Desc[] target() default {};
    
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
     * return {@link org.objectweb.asm.tree.MethodInsnNode} instances
     * and an exception will be thrown if this is not the case.
     * 
     * @return {@link At} which identifies the target method invocation
     */
    public At at();
    
    /**
     * <p>Gets the argument index on the target to set. It is not necessary to
     * set this value if there is only one argument of the modifier type in the
     * hooked method's signature. For example if the target method accepts a
     * boolean, an integer and a String, and the modifier method accepts and
     * returns an integer, then the integer parameter will be automatically
     * selected.</p>
     * 
     * <p>The index is zero-based.</p>
     * 
     * @return argument index to modify or -1 for automatic
     */
    public int index() default -1;

    /**
     * By default, the annotation processor will attempt to locate an
     * obfuscation mapping for all {@link ModifyArg} methods since it is
     * anticipated that in general the target of a {@link ModifyArg} annotation
     * will be an obfuscated method in the target class. However since it is
     * possible to also apply mixins to non-obfuscated targets (or non-
     * obfuscated methods in obfuscated targets, such as methods added by Forge)
     * it may be necessary to suppress the compiler error which would otherwise
     * be generated. Setting this value to <em>false</em> will cause the
     * annotation processor to skip this annotation when attempting to build the
     * obfuscation table for the mixin.
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
    
    /**
     * By default almost all injectors for a target class apply their injections
     * at the same time. In other words, if multiple mixins target the same
     * class then injectors are applied in priority order (since the mixins
     * themselves are merged in priority order, and injectors run in the order
     * they were merged). The exception being redirect injectors, which apply in
     * a later pass.
     * 
     * <p>The default order for injectors is <tt>1000</tt>, and redirect
     * injectors use <tt>10000</tt>.</p>
     * 
     * <p>Specifying a value for <tt>order</tt> alters this default behaviour
     * and causes the injector to inject either earlier or later than it
     * normally would. For example specifying <tt>900</tt> will cause the
     * injector to apply before others, while <tt>1100</tt> will apply later.
     * Injectors with the same <tt>order</tt> will still apply in order of their
     * mixin's <tt>priority</tt>.
     * 
     * @return the application order for this injector, uses DEFAULT (1000) if
     *      not specified
     */
    public int order() default 1000;    

}
