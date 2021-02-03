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
package org.spongepowered.asm.mixin.injection.selectors;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.util.asm.ElementNode;

/**
 * Target Selectors are used to identify target members in a class, with the
 * criteria for selection being specified in a single string, effectively
 * defining the query parameters. They are used by injectors and other
 * components which need to identify a target element using a string.
 * 
 * <h3>Explicit Target Selectors</h3>
 * 
 * <p><b>Explicit</b> Target Selectors are handled internally using
 * {@link MemberInfo} structs, see the javadoc for {@link MemberInfo} for the
 * supported variants and examples.</p>
 * 
 * <h3>Pattern Target Selectors (Regex Selectors)</h3>
 * 
 * <p><b>Pattern</b> Target Selectors are handled internally using
 * {@link MemberMatcher}, see the javadoc for {@link MemberMatcher} for the
 * supported variants and examples. Pattern Selectors always end with a forward
 * slash character.</p>
 * 
 * <h3>Dynamic Target Selectors</h3>
 * 
 * <p><b>Dynamic</b> Target Selectors can be built-in or user-supplied types
 * with their own specialised syntax or behaviour. Dynamic selectors are
 * specified in the following format, and can be recognised by the fact that
 * the selector string starts with "<tt>&#064;</tt>":</p>
 * 
 * <blockquote><pre>
 *   <del>// Built-in dynamic selector without argument string</del>
 *   &#064;SelectorId
 *   
 *   <del>// Built-in dynamic selector with empty argument string</del>
 *   &#064;SelectorId()
 *   
 *   <del>// Built-in dynamic selector with argument</del>
 *   &#064;SelectorId(custom,argument,string,in,any,format)
 *   
 *   <del>// User-provided dynamic selector with argument. Note that
 *   // user-provided dynamic selectors are namespaced to avoid conflicts.</del>
 *   &#064;Namespace:SelectorId(some arguments)</pre>
 * </blockquote>
 * 
 * <p>The exact format of the argument string is specified by the dynamic
 * selector itself, consult the documentation for the dynamic selector you are
 * using for details on the required format.</p>
 * 
 */
public interface ITargetSelector {
    
    /**
     * Get the next target selector in this path (or <tt>null</tt> if this
     * selector is the last selector in the chain. Called at recurse points in
     * the subject in order to match against the child subject.
     * 
     * <p>Can return null</p>
     */
    public abstract ITargetSelector next();
    
    /**
     * Configure and return a modified version of this selector by consuming the
     * supplied arguments. Results from this method should be idempotent in
     * terms of the configuration of the returned object, but do not have to
     * necessarily return the same object if the callee already matches the
     * supplied configuration, though this is generally the case.
     * 
     * <p>In other words, calling <tt>configure("foo")</tt> when this object is
     * <i>already</i> configured according to "foo" may simply return this
     * object, or might return an identically-configured copy.</p>
     * 
     * <p>Must not return null</p>
     * 
     * @param args Configuration arguments
     * @return Configured selector, may return this selector if the specified
     *      condition is already satisfied
     */
    public abstract ITargetSelector configure(String... args);
    
    /**
     * Perform basic sanity-check validation of the selector, checks that the
     * parsed out parameters are basically sane
     * 
     * @return fluent (this selector)
     * 
     * @throws InvalidSelectorException if any sanity check fails
     */
    public abstract ITargetSelector validate() throws InvalidSelectorException;

    /**
     * Attach this selector to the specified context. Should return this
     * selector unmodified if all is well, or a new selector to be used for
     * further processing of the supplied context. If the supplied context is
     * invalid, an {@link InvalidSelectorException} is thrown.
     * 
     * @param context Context to attach to
     * @return Attached selector
     */
    public abstract ITargetSelector attach(ISelectorContext context) throws InvalidSelectorException;
    
    /**
     * Number of candidates which this selector should match
     */
    public abstract int getMatchCount();
    
    /**
     * Test whether this selector matches the supplied element node
     * 
     * @param node node node to test
     * @param <TNode> node type
     * @return true if this selector can match the supplied field
     */
    public abstract <TNode> MatchResult match(ElementNode<TNode> node);
    
    /**
     * Test whether this selector matches the supplied instruction node
     * 
     * @param insn instruction node to test
     * @return true if this selector can match the supplied instruction
     */
    public abstract MatchResult match(AbstractInsnNode insn);

}
