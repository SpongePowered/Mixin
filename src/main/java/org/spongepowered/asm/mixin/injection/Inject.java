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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.throwables.InjectionError;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.ConstraintParser.Constraint;

/**
 * Specifies that this mixin method should inject a callback (or
 * callback<b>s</b>) to itself in the target method(s) identified by
 * {@link #method}.
 * 
 * <p>Callbacks are simple injectors which simply inject a call to the decorated
 * method (the <em>handler</em>) in the <em>target</em> method (or methods)
 * selected by the selectors specified in {@link #method}. Callback Injectors
 * can also capture arguments and local variables from the <em>target</em> for
 * use in the handler.</p>
 * 
 * <p>Callback handler methods should always return <tt>void</tt> and should
 * have the same <tt>static</tt>-ness as their target (though it is allowable to
 * have a <tt>static</tt> callback injected into an instance method, and for
 * obvious reasons the inverse is not permitted).</p> 
 * 
 * <h3>Basic usage</h3>
 * 
 * <p>The simplest usage of <tt>&#064;Inject</tt> captures no context from the
 * target scope. This is particularly useful if the injector is targetting
 * multiple methods with different signatures. In this case only the 
 * {@link CallbackInfo} (or {@link CallbackInfoReturnable} as appropriate) is
 * required.</p>
 * 
 * <blockquote><tt>private void onSomeEvent(CallbackInfo ci)</tt></blockquote>
 * 
 * <h3>Capture target arguments</h3>
 * 
 * <p>Callbacks can also capture the arguments passed to the target method. To
 * do so specify the target arguments before the {@link CallbackInfo}:</p>
 * 
 * <blockquote><tt>private void onSomeEvent(int arg1, String arg2,
 *      CallbackInfo ci)</tt></blockquote>
 * 
 * <h3>Surrogate methods</h3>
 * 
 * <p>If injecting into multiple methods with different target arguments it is 
 * obviously possible to ignore the target arguments (see "Basic Usage" above)
 * but this may be unsuitable if arguments from the target are required. If you
 * need to inject into multiple methods but also wish to capture method
 * arguments you may provide a <em>surrogate</em> method with the alternative
 * signature. In fact you may provide as many surrogates as required by the
 * injection. Surrogate methods much have the same name as the handler method
 * and must be decorated with {@link Surrogate}. A surrogate may also be
 * required where the LVT of a method with <em>local capture</em> (see below) is
 * known to change between different environments or injection points.</p>
 * 
 * <h3>Capture local variables</h3>
 * 
 * <p>In addition to capturing the target method arguments, it may be desirable
 * to capture locally-scoped variables from the target method at the point of
 * injection. This is usually executed in two stages:</p>
 * 
 * <ol>
 *   <li>Set the {@link #locals()} value of your injection to
 *     {@link LocalCapture#PRINT} and run the application.</li>
 *   <li>When the injector is processed, a listing of the LVT is produced
 *     accompanied by a generated signature for your handler method including
 *     the discovered args. Modify your handler signature accordingly.</li>
 * </ol>
 * 
 * <p>For more details see {@link #locals()}.</p> 
 * 
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Inject {
    
    /**
     * The identifier for this injector, can be retrieved via the
     * {@link CallbackInfo#getId} accessor. If not specified, the ID defaults to
     * the target method name.
     * 
     * @return the injector id to use
     */
    public String id() default "";
    
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
     * Array of {@link Slice} annotations which describe the method bisections
     * used in the {@link #at} queries for this injector.
     * 
     * @return slices
     */
    public Slice[] slice() default {};
    
    /**
     * Array of {@link At} annotations which describe the
     * {@link InjectionPoint}s in the target method. Allows one or more
     * callbacks to be injected in the target method.
     * 
     * @return injection point specifiers for this injector
     */
    public At[] at();
    
    /**
     * Setting an injected callback to <em>cancellable</em> allows the injected
     * callback to inject optional RETURN opcodes into the target method, the
     * return behaviour can then be controlled from within the callback by
     * interacting with the supplied {@link CallbackInfo} object.
     * 
     * @return true if this injector should inject appropriate RETURN opcodes
     *      which allow it to be cancelled
     */
    public boolean cancellable() default false;
    
    /**
     * Specifies the local variable capture behaviour for this injector.
     * 
     * <p>When capturing local variables in scope, the variables are appended to
     * the callback invocation after the {@link CallbackInfo} argument.</p>
     * 
     * <p>Capturing local variables from the target scope requires careful
     * planning because unlike other aspects of an injection (such as the target
     * method name and signature), the local variable table is <b>not</b> safe
     * from modification by other transformers which may be in use in the
     * production environment. Even other injectors which target the same target
     * method have the ability to modify the local variable table and thus it is
     * in no way safe to assume that local variables in scope at development
     * time will be so in production.</p>
     * 
     * <p>To provide some level of flexibility, especially where changes can be
     * anticipated (for example a well-known mod makes changes which result in a
     * particular structure for the local variable table) it is possible to
     * provide <em>overloads</em> for the handler method which will become
     * surrogate targets for the orphaned injector by annotating them with an
     * {@link Surrogate} annotation.</p>
     * 
     * <p>You can improve the robustness of your local capture injection by only
     * specifying locals up to the last variable you wish to use. For example if
     * the target LVT contains <tt>&lt;int, int, int, float, String&gt;</tt> and
     * you only need the <tt>float</tt> value, you can choose to omit the unused
     * <tt>String</tt> and changes to the LVT beyond that point will not affect
     * your injection.</p>
     * 
     * <p>It is also important to nominate the failure behaviour to follow when
     * local capture fails and so all {@link LocalCapture} behaviours which
     * specify a capture action imply a particular behaviour for handling
     * failure. See the javadoc on the {@link LocalCapture} members for more
     * details.</p>
     * 
     * <p>Determining what local variables are available to you and in what
     * order can be somewhat tricky, and so a simple mechanism for enumerating
     * available locals is provided. By setting <code>locals</code> to
     * {@link LocalCapture#PRINT}, the injector writes the local capture state
     * to STDERR instead of injecting the callback. Using the output thus
     * obtained it is then a straightforward matter of altering the callback
     * method signature to match the signature proposed by the Callback
     * Injector.</p> 
     * 
     * @return the desired local capture behaviour for this injector
     */
    public LocalCapture locals() default LocalCapture.NO_CAPTURE;

    /**
     * By default, the annotation processor will attempt to locate an
     * obfuscation mapping for all {@link Inject} methods since it is
     * anticipated that in general the target of a {@link Inject} annotation
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
    
}
