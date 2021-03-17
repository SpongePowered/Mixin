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

import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.selectors.dynamic.DynamicSelectorDesc;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;

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
 * <blockquote><code>
 * <del>// An example explicit target selector</del><br />
 * Lfully/qualified/OwnerClass;methodName*(III)V
 * </code></blockquote>
 * 
 * <h3>Pattern Target Selectors (Regex Selectors)</h3>
 * 
 * <p><b>Pattern</b> Target Selectors are handled internally using
 * {@link MemberMatcher}, see the javadoc for {@link MemberMatcher} for the
 * supported variants and examples. Pattern Selectors always end with a forward
 * slash character.</p>
 * 
 * <blockquote><code>
 * <del>// Matches candidates ending with "Bar" and which take a single int
 * <br /></del>
 * /bar$/ desc=/^\\(I\\)/<br />
 * <br />
 * <del>// The same example but with "name" explicitly specified (optional)
 * <br /></del>
 * name=/bar$/ desc=/^\\(I\\)/<br />
 * <br />
 * <del>// Matches candidates whose name contains "Entity"</del><br />
 * /Entity/
 * </code></blockquote>
 * 
 * <h3>Dynamic Target Selectors</h3>
 * 
 * <p><b>Dynamic</b> Target Selectors can be built-in or user-supplied types
 * with their own specialised syntax or behaviour.</p>
 * 
 * <h4>Built-in dynamic selectors:</h4>
 * 
 * <ul>
 *   <li>{@link DynamicSelectorDesc &#64;Desc Selector}: Selector which uses
 *     descriptors defined in {@link Desc &#64;Desc} annotations</li>
 * </ul>
 *  
 * <p>Dynamic selectors are specified in the following format, and can be
 * recognised by the fact that the selector string starts with "<tt>&#64;</tt>":
 * </p>
 * 
 * <blockquote><pre>
 * <del>// Built-in dynamic selector without argument string</del>
 * &#064;SelectorId
 *   
 * <del>// Built-in dynamic selector with empty argument string</del>
 * &#064;SelectorId()
 *   
 * <del>// Built-in dynamic selector with argument</del>
 * &#064;SelectorId(custom,argument,string,in,any,format)
 *   
 * <del>// User-provided dynamic selector with argument. Note that
 * // user-provided dynamic selectors are namespaced to avoid conflicts.</del>
 * &#064;Namespace:SelectorId(some arguments)</pre>
 * </blockquote>
 * 
 * <p>The exact format of the argument string is specified by the dynamic
 * selector itself, consult the documentation for the dynamic selector you are
 * using for details on the required format.</p>
 * 
 */
public interface ITargetSelector {
    
    /**
     * Available selector reconfigurations
     */
    public enum Configure {
        
        /**
         * Configure this selector for matching members in a class. Usually used
         * to set defaults for match limits based on role. 
         */
        SELECT_MEMBER(0),
        
        /**
         * Configure this selector for matching field and method instructions in
         * a method body. Usually used to set defaults for match limits. 
         */
        SELECT_INSTRUCTION(0),
        
        /**
         * Where supported, changes the owner selection to the specified value.
         */
        MOVE(1),
        
        /**
         * Where supported, changes the owner selection to match all owners,
         * retaining other properties.
         */
        ORPHAN(0),
        
        /**
         * Where supported, changes the descriptor to the specified value.
         */
        TRANSFORM(1),
        
        /**
         * Where supported, changes the descriptor to match all target
         * descriptors, retaining other properties
         */
        PERMISSIVE(0),
        
        /**
         * Where supported, removes the min and max limits for the selector,
         * allowing it to return as many or as few matches as required.
         */
        CLEAR_LIMITS(0);
        
        private int requiredArgs;

        private Configure(int requiredArgs) {
            this.requiredArgs = requiredArgs;
        }
        
        public void checkArgs(String... args) throws IllegalArgumentException {
            int argc = args == null ? 0 : args.length;
            if (argc < this.requiredArgs) {
                throw new IllegalArgumentException("Insufficient arguments for " + this.name() + " mutation. Required " + this.requiredArgs
                        + " but received " + argc);
            }
        }
        
    }
    
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
     * supplied configuration, or if the requested mutation is not supported by
     * the selector, though this is generally the case.
     * 
     * <p>In other words, calling <tt>configure(Configure.ORPHAN)</tt> when this
     * object is <i>already</i> an orphan or does not support orphaning, may
     * simply return this object, or might return an identically-configured
     * copy.</p>
     * 
     * <p>Must not return null, defaults to returning unmodified selector.</p>
     * 
     * @param request Requested operation
     * @param args Configuration arguments
     * @return Configured selector, may return this selector if the specified
     *      condition is already satisfied
     */
    public abstract ITargetSelector configure(Configure request, String... args);
    
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
     * Minimum number of candidates this selector must match 
     */
    public abstract int getMinMatchCount();
    
    /**
     * Maximum number of candidates this selector can match
     */
    public abstract int getMaxMatchCount();
    
    /**
     * Test whether this selector matches the supplied element node
     * 
     * @param node node node to test
     * @param <TNode> node type
     * @return true if this selector can match the supplied field
     */
    public abstract <TNode> MatchResult match(ElementNode<TNode> node);

}
