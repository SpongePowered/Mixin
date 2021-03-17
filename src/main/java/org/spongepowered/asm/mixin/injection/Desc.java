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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.spongepowered.asm.mixin.injection.selectors.dynamic.DynamicSelectorDesc;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;

/**
 * <p>A descriptor for matching a target element, for details of usage see
 * {@link DynamicSelectorDesc}.</p>
 * 
 * <p>Unlike {@link MemberInfo Explicit Target Selectors} which can match any
 * element of a candidate member, <tt>&#064;Desc</tt> descriptors always match
 * all elements of a target explicitly. Element defaults are as follows:</p>
 * 
 * <blockquote><dl>
 *   <dt>{@link #owner}</dt>
 *   <dd>Defaults to the current target of the mixin if not specified</dd>
 *   <dt>{@link #ret}</dt>
 *   <dd>Defaults to <tt>void</tt></dd>
 *   <dt>{@link #args}</dt>
 *   <dd>Defaults to an empty array matching a target with <b>no arguments</b>,
 *      omitting this value does <em>not</em> mean "match any" signature and
 *      will explicitly match targets with no arguments. This value is ignored
 *      for fields.</dd>
 * </dl></blockquote>
 * 
 * <h4>Notes</h4>
 * 
 * <ul>
 *   <li>It should be noted that specifying the only required value
 *   ({@link #value}) will therefore match elements with owner type equal to the
 *   current mixin target, taking no arguments and returning void.</li>
 * 
 *   <li>Note that when matching fields the value of {@link #args} is ignored.
 *   </li>
 * </ul>
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Descriptors.class)
public @interface Desc {
    
    /**
     * The identifier for this descriptor. Can be an implicit coordinate or a
     * user-defined string. IDs are not case-sensitive but must be unique within
     * a mixin or resolution scope. For more details on resolution for
     * descriptors see {@link DynamicSelectorDesc}.
     */
    public String id() default "";

    /**
     * The owner of the member to match. The "owner" is the class which declares
     * the member <b>or, ocasionally</b> the type of class upon which a virtual
     * call is being made. For example if a class <tt>SuperType</tt> defines a
     * method <tt>doSomething</tt>, usually <tt>owner</tt> will be
     * <tt>SuperType</tt>. However note that where a method retrieves a
     * reference to a derived type <tt>DerivedType</tt> which overrides
     * <tt>doSomething</tt>, it's possible the invocation will be made using
     * that type. To ensure usage of the correct type, inspect the disassembled
     * bytecode for your target.
     * 
     * <p>When <tt>owner</tt> is not specified, it defaults to the <em>current
     * </em> target of the mixin.</p>
     */
    public Class<?> owner() default void.class;
    
    /**
     * The name of the member to match. Must be specified. Matched
     * case-sensitively against targets.
     */
    public String value();
    
    /**
     * The return type of a method to match, or the declared type of a field.
     * Defaults to <tt>void</tt>.
     */
    public Class<?> ret() default void.class;
    
    /**
     * The argument types of a method to match, ignored for fields. Note that
     * failing to specify this value <b>matches a target with no arguments</b>.
     */
    public Class<?>[] args() default { };
    
    /**
     * The next elements of this descriptor path, evaluated in order for each
     * recurse point.
     */
    // public Next[] next() default { };
    
    /**
     * The minimum number of times this selector should match. By default the
     * selector is allowed to match no targets. When selecting fields or methods
     * setting this value to anything other than 0 or 1 is pointless since the
     * member can either be present or absent. However when matching method <em>
     * calls</em> or field <em>accesses</em> (eg. when using this value inside
     * {@link At#desc &#64;At.desc}) this allows a minimum number of matches to
     * be specified in order to provide early validation that the selector
     * matched the correct number of candidates. To specify an exact number of
     * matches, set {@link #max max} to the same value as <tt>min</tt>. Values
     * less than zero are ignored since selectors cannot match a negative number
     * of times.
     */
    public int min() default 0;
    
    /**
     * The maximum number of times this selector can match. By default the
     * selector is allowed to match an unlimited number of targets. When
     * selecting fields or methods, setting this value to anything other than 1
     * is pointless since the member can either be present or absent. However
     * when matching method <em>calls</em> or field <em>accesses</em> (eg. when
     * using this value inside {@link At#desc &#64;At.desc}) this allows a
     * maximum number of matches to be specified in order to limit the number of
     * times that the selector can match candidates. To specify an exact number
     * of matches, set <tt>max</tt> to the same value as {@link #min min}.
     * Values less than 1 are treated as <tt>Integer.MAX_VALUE</tt> (the
     * default) since setting a value of 0 or less has no meaning.
     */
    public int max() default Integer.MAX_VALUE;
    
}
