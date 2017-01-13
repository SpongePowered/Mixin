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

import org.spongepowered.asm.mixin.injection.InjectionPoint.Selector;

/**
 * A <tt>Slice</tt> identifies a section of a method to search for injection
 * points.
 * 
 * <p>Using slices provides for a much more expressive way of identifying target
 * injection points in a method, which has two advantages:</p>
 * 
 * <ol>
 *   <li>Encoding assumptions about the structure of a method makes it easier to
 *   detect (and fail-fast) when a method has changed in an unexpected way.</li>
 *   <li>Injection points using slices are less brittle than points specified
 *   with large ordinals because they allow injection points to be specified
 *   with reference to characteristics of the method rather than arbitrary
 *   ordinal values.</li>
 * </ol>
 * 
 * <p>Consider the following example:</p>
 * 
 * <blockquote><pre>
 * private void foo(Bar bar) {
 *     bar.update();
 *     List&lt;Thing&gt; list = bar.getThings();
 *     for (Thing thing : list) {
 *         thing.<b>processStuff</b>();
 *     }
 *     
 *     Thing specialThing = bar.getSpecialThing();
 *     if (specialThing.isReady()) {
 *         specialThing.<b>processStuff</b>();
 *         bar.update();
 *     }
 *     
 *     bar.notifyFoo();
 * }</pre></blockquote>
 * 
 * <p>Let's assume we are interested in applying a {@link Redirect} to the
 * <tt>processStuff()</tt> method call within the <tt>if</tt> block. Using
 * <tt>ordinal</tt> we can achieve this as follows:</p>
 * 
 * <blockquote><pre>
 * &#064;At(value = "INVOKE", target = "processStuff", ordinal = 1)</pre>
 * </blockquote>
 * 
 * <p>This will work fine initially. However consider the case that in a future
 * version of the target library the method is altered and an additional call to
 * <tt>processStuff()</tt> is added:</p>  
 * 
 * <blockquote><pre>
 * private void foo(Bar bar) {
 *     bar.update();
 *     List&lt;Thing&gt; list = bar.getThings();
 *     for (Thing thing : list) {
 *         thing.<b>processStuff</b>();
 *     }
 *     
 *     // A new call to processStuff, which now has the ordinal 1
 *     bar.getActiveThing().<b>processStuff</b>();
 *     
 *     Thing specialThing = bar.getSpecialThing();
 *     if (specialThing.isReady()) {
 *         specialThing.<b>processStuff</b>();
 *         bar.update();
 *     }
 *     
 *     bar.notifyFoo();
 * }</pre></blockquote>
 * 
 * <p>It's still pretty easy for a human to identify the correct point since the
 * original <tt>if</tt> hasn't changed. However the use of ordinals means that
 * the injection is now wrong and must be fixed by hand.</p>
 * 
 * <p>We can make the injection point more expressive and reliable by using a
 * <tt>Slice</tt>. We know in this example that the call to <tt>processStuff()
 * </tt> we want is the one immediately after a call to <tt>isReady()</tt>. We
 * can <em>slice</em> the method at the call to <tt>isReady()</tt> and modify
 * the injector accordingly:</p>
 * 
 * <blockquote><pre>
 * &#064;Inject(
 *     slice = &#064;Slice(
 *         from = &#064;At(value = "INVOKE", target = "isReady")
 *     )
 *     at = &#064;At(value = "INVOKE", target = "processStuff", ordinal = 0)
 * )</pre></blockquote>
 * 
 * <p>The <tt>ordinal</tt> is specified as <tt>0</tt> because the scope of the
 * injection point is now the region of the method defined by the <em>slice</em>
 * .</p>
 * 
 * <p>Slices can be specified using a {@link #from} point, a {@link #to} point,
 * or both. {@link Inject Callback Injectors} using multiple injection points
 * can distinguish different slices using {@link #id} and then specify the slice
 * to use in the {@link At#slice} argument.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Slice {
    
    /**
     * The identifier for this slice, specified using the {@link At#slice} value
     * (if omitted, this slice becomes the <em>default slice</em> and applies to
     * all undecorated {@link At} queries).
     * 
     * <p>This value can be safely ignored for injector types which only accept
     * a single query (eg. {@link Redirect} and others). However since
     * {@link Inject} injectors can have multiple {@link At} queries, it may be
     * desirable to have multiple slices as well. When specifying multiple
     * slices, each should be designed a unique <tt>id</tt> which can then be
     * referenced in the corresponding <tt>At</tt>.</p>
     * 
     * <p>There are no specifications or restrictions for valid <tt>id</tt>s,
     * however it is recommended that the <tt>id</tt> in some way describe the
     * slice, thus allowing the <tt>At</tt> query to be read without necessarily
     * reading the slice itself. For example, if the slice is selecting <em>all
     * instructions before the first call to some method <tt>init</tt></em>,
     * then using an id <tt>&quot;beforeInit</tt> makes sense. Using <em>before,
     * after</em> and <em>between</em> prefixes as a loose standard is
     * considered good practice.</p>
     * 
     * <p>This value defaults to an empty string, the empty string is used as a
     * default identifier throughout the injection subsystem, and any {@link At}
     * which doesn't specify a slice explicitly will use this identifer.
     * Specifying <tt>id</tt> or <tt>slice</tt> for injectors which only support
     * a single slice is ignored internally, so you may use this field to give a
     * descriptive name to the slice if you wish.</p>
     * 
     * @return The identifier for this slice
     */
    public String id() default "";
    
    /**
     * Injection point which specifies the <em>start</em> of the slice region.
     * {@link At}s supplied here should generally specify a {@link Selector}
     * in order to identify which instruction should be used for queries which
     * return multiple results. The selector is specified by appending the
     * selector type to the injection point type as follows:
     * 
     * <blockquote><pre>&#064;At(value = "INVOKE:LAST", ... )</pre></blockquote>
     * 
     * <p>If <tt>from</tt> is not supplied then {@link #to} must be supplied. It
     * is allowed to specify <b>both</b> <tt>from</tt> and <tt>to</tt> as long
     * as the points select a region with positive size (eg the insn returned by
     * <tt>to</tt> must appear after that selected by <tt>from</tt> in the
     * method body).</p>
     * 
     * @return the start point of the slice
     */
    public At from() default @At("HEAD");
    
    /**
     * Injection point which specifies the <em>end</em> of the slice region.
     * Like {@link #from}, {@link At}s supplied here should generally specify a
     * {@link Selector} in order to identify which instruction should be used
     * for queries which return multiple results. The selector is specified by
     * appending the selector type to the injection point type as follows:
     * 
     * <blockquote><pre>&#064;At(value = "INVOKE:LAST", ... )</pre></blockquote>
     * 
     * <p>If <tt>to</tt> is not supplied then {@link #from} must be supplied. It
     * is allowed to specify <b>both</b> <tt>from</tt> and <tt>to</tt> as long
     * as the points select a region with positive size (eg the insn returned by
     * <tt>to</tt> must appear <em>after</em> that selected by <tt>from</tt> in
     * the method body).</p>
     * 
     * @return the start point of the slice
     */
    public At to() default @At("TAIL");
    
}
