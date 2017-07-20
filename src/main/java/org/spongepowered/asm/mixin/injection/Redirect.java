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
import org.spongepowered.asm.mixin.injection.points.BeforeFieldAccess;
import org.spongepowered.asm.mixin.injection.throwables.InjectionError;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.ConstraintParser.Constraint;

/**
 * <p>Specifies that this mixin method should redirect the specified
 * method call, field access or object construction (via the <tt>new</tt>
 * keyword) to the method decorated with this annotation.</p>
 * 
 * <h4>Method Redirect Mode</h4>
 * 
 * <p>The handler method signature must match the hooked method precisely
 * <b>but</b> prepended with an arg of the owning object's type to accept the
 * object instance the method was going to be invoked on. For example when
 * hooking the following call:</p>
 * 
 * <blockquote><pre>public void baz(int someInt, String someString) {
 *    int abc = 0;
 *    int def = 1;
 *    Foo someObject = new Foo();
 *  
 *    // Hooking this method
 *    boolean xyz = someObject.bar(abc, def);
 *}</pre>
 * </blockquote>
 * 
 * <p>The signature of the redirected method should be:</p>
 * 
 * <blockquote>
 *      <pre>public boolean barProxy(Foo someObject, int abc, int def)</pre>
 * </blockquote>
 * 
 * <p>For obvious reasons this does not apply for static methods, for static
 * methods it is sufficient that the signature simply match the hooked method.
 * </p>
 * 
 * <p>It is also possible to capture the arguments of the target method in
 * addition to the arguments being passed to the method call (for example in
 * the code above this would be the <em>someInt</em> and <em>someString</em>
 * arguments) by appending the arguments to the method signature:</p>
 * 
 * <blockquote>
 *      <pre>public boolean barProxy(Foo someObject, int abc, int def,
 *    int someInt, String someString)</pre>
 * </blockquote>
 * 
 * <h4>Field Access Redirect Mode</h4>
 * 
 * <p>The handler method signature varies depending on whether the redirector is
 * handling a field <b>write</b> (<tt>PUTFIELD</tt>, <tt>PUTSTATIC</tt>) or a
 * field <b>read</b> (<tt>GETFIELD</tt>, <tt>GETSTATIC</tt>).</p>
 * 
 * <table width="100%">
 *   <tr>
 *     <th width="25%">Operation (OPCODE)</th>
 *     <th>Handler signature</th>
 *   </tr>
 *   <tr>
 *     <td>Read static field (<tt>GETSTATIC</tt>)</td>
 *     <td><code>private <b>FieldType</b> getFieldValue()</code></td>
 *   </tr>
 *   <tr>
 *     <td>Read instance field (<tt>GETFIELD</tt>)</td>
 *     <td><code>private <b>FieldType</b> getFieldValue(<b>OwnerType</b>
 *     owner)</code></td>
 *   </tr>
 *   <tr>
 *     <td>Write static field (<tt>PUTSTATIC</tt>)</td>
 *     <td><code>private void setFieldValue(<b>FieldType</b> value)</code></td>
 *   </tr>
 *   <tr>
 *     <td>Write instance field (<tt>PUTFIELD</tt>)</td>
 *     <td><code>private void setFieldValue(<b>OwnerType</b>
 *     owner, <b>FieldType</b> value)</code></td>
 *   </tr>
 * </table>
 * 
 * <p>It is also possible to capture the arguments of the target method in
 * addition to the arguments being passed to the method call (for example in
 * the code above this would be the <em>someInt</em> and <em>someString</em>
 * arguments) by appending the arguments to the method signature.</p>
 * 
 * <h4>Array Element Access Redirect Mode</h4>
 * 
 * <p>For fields of an array type, it is possible to redirect the access to the
 * actual array field itself using the behaviour above. However it is also
 * possible to redirect access to individual array elements. Consider the
 * following example:</p>
 * 
 * <blockquote><pre>private String[] strings = { "foo", "bar", "baz" };
 *
 *public void print(int index) {
 *    System.err.println(this.strings[index]);
 *}</pre>
 * </blockquote>
 * 
 * <p>It may be desirable to redirect the access to this array element. To do
 * so, declare a redirect handler which takes the array and the index (or
 * <em>indices</em>, for multi-dimensional arrays) and returns <em>the element
 * type</em> (in this case <tt>String</tt>) as follows:</p>
 * 
 * <table width="100%">
 *   <tr>
 *     <th width="25%">Operation</th>
 *     <th>Handler signature</th>
 *   </tr>
 *   <tr>
 *     <td>Read element</td>
 *     <td><code>private <b>ElementType</b> getElement(<b>ElementType</b>[]
 *          array, int index)</code></td>
 *   </tr>
 *   <tr>
 *     <td>Write element</td>
 *     <td><code>private void setElement(<b>ElementType</b>[] array, int index,
 *          <b>ElementType</b> value)</code></td>
 *   </tr>
 * </table>
 * 
 * <p>The handler receives a reference to the array itself (read from the field)
 * and the indices being accessed. See the
 * {@link BeforeFieldAccess BeforeFieldAccess args} for details
 * on matching array accesses using the <tt>FIELD</tt> injection point.</p>
 * 
 * <h4>Array Length Redirect Mode</h4>
 * 
 * <p>For fields of an array type, it is possible to redirect the call to the
 * builtin pseudo-property <tt>length</tt>. To do so, specify the argument
 * <tt>array=length</tt> and configure your handler signature to return <tt>int
 * </tt> and consume the array as an argument:</p>
 * 
 * <blockquote><code>private int getLength(<b>ElementType</b>[] array)
 * </code></blockquote>
 * 
 * <p>For multi-dimensional arrays, provide one <tt>int</tt> argument for each
 * additional dimension of the array:</p>
 * 
 * <blockquote><code>// Multidimensional array<br />
 * private <b>ElementType</b>[][] array;<br /><br />
 * // Handler signature:<br /> 
 * private <b>int</b> getLength(<b>ElementType</b>[][] array, int baseDim)
 * </code></blockquote>
 * 
 * <h4>Constructor Redirect Mode</h4>
 * 
 * <p>The handler method signature must match the constructor being redirected
 * and the return type must match the type of object being constructed. For
 * example to redirect the following constructor call:</p>
 * 
 * <blockquote><pre>public void baz(int someInt, String someString) {
 *    // Hooking this constructor
 *    Foo someObject = new Foo("bar");
 *}</pre>
 * </blockquote>
 * 
 * <p>The signature of the handler method should be:</p>
 * 
 * <blockquote>
 *      <pre>public Foo constructFoo(String arg1)</pre>
 * </blockquote>
 * 
 * <p>Note that like other redirectors, it is possible to capture the target
 * method's arguments by appending them to the handler method's signature.</p>
 * 
 * <h4>A note on <tt>static</tt> modifiers for handler methods</h4>
 *
 * <p>In general, when declaring a redirect handler the <tt>static</tt> modifier
 * of the handler method must always match the target method. The exception to
 * this rule is application of <tt>&#064;Redirect</tt> to instructions which are
 * effectively <b>before</b> the call to <tt>super()</tt> in a constructor.</p>
 * 
 * <p>Consider the following code:</p>
 * 
 * <blockquote><pre>class Foo : Bar {
 *    Foo(int arg) {
 *        super(<span style="background: #FF0">Foo.isEven(arg)</span>);
 *    }
 *    
 *    static int isEven(int arg) {
 *        return arg % 2 == 0;
 *    }
 *}</pre>
 * </blockquote>
 * 
 * <p>In this example, the inline call to <tt>Foo.isEven</tt> takes place
 * <em>before</em> the call to <tt>super()</tt> and (per Java rules) this method
 * must be static (because the instance (<tt>this</tt>) is unavailable until the
 * initialiser is complete. Thus if you wish to <tt>&#064;Redirect</tt> the call
 * to <tt>isEven</tt> then the handler method must also be <tt>static</tt>. The
 * injection subsystem will raise an exception for non-static handlers in this
 * situation to indicate that the staticness of the handler is incorrect.</p>
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Redirect {
    
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
     * return {@link org.spongepowered.asm.lib.tree.MethodInsnNode} and an
     * exception will be thrown if this is not the case.
     * 
     * @return {@link At} which identifies the target method invocation
     */
    public At at();

    /**
     * By default, the annotation processor will attempt to locate an
     * obfuscation mapping for all {@link Redirect} methods since it is
     * anticipated that in general the target of a {@link Redirect} annotation
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
