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
package org.spongepowered.asm.mixin.injection.struct;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.injection.selectors.ElementNode;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorByName;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorConstructor;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorRemappable;
import org.spongepowered.asm.mixin.injection.selectors.InvalidSelectorException;
import org.spongepowered.asm.mixin.injection.selectors.MatchResult;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.obfuscation.mapping.IMapping;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.Quantifier;
import org.spongepowered.asm.util.SignaturePrinter;
import org.spongepowered.asm.util.asm.ASM;

import com.google.common.base.Objects;
import com.google.common.base.Strings;

/**
 * <p>Struct which defines an <b>Explcit Target selector</b>,  See
 * {@link ITargetSelector} for other supported selector types. This struct is
 * also used as a general-purpose information bundle about a member (method or
 * field) parsed from a String token in another annotation, this is used where
 * target members need to be specified as Strings in order to parse the String
 * representation to something useful.</p>
 * 
 * <p>In general a <tt>MemberInfo</tt> consists of 4 parts, <span class="ownr">
 * owner</span>, <span class="name">name</span>, <span class="quan">quantifier
 * </span> and <span class="desc">descriptor</span>, all of which are optional:
 * </p>
 * 
 * <table class="selectorTable">
 *   <thead>
 *     <tr>
 *       <th>&nbsp;</th>
 *       <th class="ownr">Owner</th>
 *       <th class="name">Name</th>
 *       <th class="quan">Quantifier</th>
 *       <th>&nbsp;</th>
 *       <th class="desc">Descriptor</th>
 *       <th>Resulting selector string</th>
 *     </tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <td class="th">Method</td>
 *       <td class="ownr">Lfully/qualified/OwnerClass;</td>
 *       <td class="name">methodName</td>
 *       <td class="quan">{1,3}</td>
 *       <td>&nbsp;</td>
 *       <td class="desc">(III)V</td>
 *       <td><span class="ownr">Lfully/qualified/OwnerClass;</span><span
 *          class="name">methodName</span><span class="quan">{1,3}</span><span
 *          class="desc">(III)V</span></td>
 *     </tr>
 *     <tr>
 *       <td class="th">Field</td>
 *       <td class="ownr">Lfully/qualified/OwnerClass;</td>
 *       <td class="name">fieldName</td>
 *       <td class="quan">*</td>
 *       <td>:</td>
 *       <td class="desc">Ljava/lang/String;</td>
 *       <td><span class="ownr">Lfully/qualified/OwnerClass;</span><span
 *          class="name">fieldName</span><span class="quan">*</span>:<span
 *          class="desc">Ljava/lang/String;</span></td>
 *     </tr>
 *   </tbody>
 * </table>
 * 
 * <p>Any part of the selector can be omitted, though they must appear in the
 * correct order. Some examples:</p>
 * 
 * <blockquote><code>
 * <del>// selects a method or field called func_1234_a, if there are multiple
 * <br />
 * // members with the same signature, matches the first occurrence<br /></del>
 * <span class="name">func_1234_a</span><br />
 * <br />
 * <del>// selects a method or field called func_1234_a, if there are multiple
 * <br />
 * // members with matching name, matches all occurrences</del><br />
 * <span class="name">func_1234_a</span><span class="quan">*</span><br />
 * <br />
 * <del>// selects a method or field called func_1234_a, if there are multiple
 * <br />
 * // members with matching name, matches all occurrences, matching less than 1
 * <br />
 * // occurrence is an error condition</del><br />
 * <span class="name">func_1234_a</span><span class="quan">+</span><br />
 * <br />
 * <del>// selects a method or field called func_1234_a, if there are multiple
 * <br />
 * // members with matching name, matches up to 3 occurrences</del><br />
 * <span class="name">func_1234_a</span><span class="quan">{,3}</span><br />
 * <br />
 * <del>// selects a method or field called func_1234_a, if there are multiple
 * <br />
 * // members with matching name, matches exactly 3 occurrences, matching fewer
 * <br />
 * // than 3 occurrences is an error condition</del><br />
 * <span class="name">func_1234_a</span><span class="quan">{3}</span><br />
 * <br />
 * <del>// selects a method or field called func_1234_a, if there are multiple
 * <br />
 * // members with matching name, matches at least 3 occurrences, matching fewer
 * <br />
 * // than 3 occurrences is an error condition</del><br />
 * <span class="name">func_1234_a</span><span class="quan">{3,}</span><br />
 * <br />
 * <del>// selects all members of any type and descriptor</del><br />
 * <span class="quan">*</span><br />
 * <br />
 * <del>// selects the first member of any type and descriptor</del><br />
 * <span class="quan">{1}</span><br />
 * <br />
 * <del>// selects all methods which take 3 ints and return a bool</del><br />
 * <span class="quan">*</span><span class="desc">(III)Z</span><br />
 * <br />
 * <del>// selects the first 2 methods which take a bool and return void</del>
 * <br />
 * <span class="quan">{2}</span><span class="desc">(Z)V</span><br />
 * <br />
 * <del>// selects a method called func_1234_a which takes 3 ints and returns a
 * bool</del><br />
 * <span class="name">func_1234_a</span><span class="desc">(III)Z</span><br />
 * <br />
 * <del>// selects a field called field_5678_z which is a String</del><br />
 * <span class="name">field_5678_z</span>:<span class="desc">Ljava/lang/String;
 * </span><br />
 * <br />
 * <del>// selects a ctor which takes a single String argument</del><br /> 
 * <span class="name">&lt;init&gt;</span><span
 *      class="desc">(Ljava/lang/String;)V</span><br />
 * <br />
 * <del>// selects a method called func_1234_a in class foo.bar.Baz</del><br />
 * <span class="ownr">Lfoo/bar/Baz;</span><span class="name">func_1234_a</span>
 * <br />
 * <br />
 * <del>// selects a field called field_5678_z in class com.example.Dave</del>
 * <br />
 * <span class="ownr">Lcom/example/Dave;</span><span class="name">field_5678_z
 * </span><br />
 * <br />
 * <del>// selects a field called field_5678_z in class com.example.Dave<br />
 * // which is of type String</del><br />
 * <span class="ownr">Lcom/example/Dave;</span><span
 *      class="name">field_5678_z</span>:<span class="desc">Ljava/lang/String;
 *      </span><br />
 * <br />
 * <del>// selects a method called func_1234_a in class foo.bar.Baz which<br />
 * // takes three doubles and returns void</del><br />
 * <span class="ownr">Lfoo/bar/Baz;</span><span
 *      class="name">func_1234_a</span><span class="desc">(DDD)V</span><br />
 * <br />
 * <del>// alternate syntax for the same</del><br />
 * <span class="ownr">foo.bar.Baz</span>.<span
 *      class="name">func_1234_a</span><span class="desc">(DDD)V</span>
 * </code></blockquote>
 * 
 * <h4>Notes</h4>
 * 
 * <ul>
 *   <li>All whitespace in the selector string is stripped before the selector
 *     is parsed.</li>
 *   <li><span class="quan">Quantifiers</span> for selectors can be used as a
 *     form of early validation (prior to injector <tt>require</tt> directives)
 *     that an injection point has matched a required number of targets or to
 *     limit the number of matches it can return.</li>
 *   <li>The syntax for <span class="quan">quantifiers</span> is based on the 
 *     syntax for <a href="https://www.regular-expressions.info/refrepeat.html"
 *     target="_top">quantifiers in regular expressions</a>, though more
 *     limited. In particular:
 *     <ul>
 *       <li><b>No quantifer</b> is equivalent to the regex <tt>?</tt>
 *         quantifier, in that undecorated selectors can match <tt>{0,1}</tt>,
 *         this is for backward compatibility reasons. Likewise <tt>?</tt> is
 *         not supported since it's implied. To require exactly one match use
 *         <tt>{1}</tt>.</li>
 *       <li>The quantifiers <tt>*</tt> and <tt>+</tt> work the same as their
 *         regex counterparts.</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public final class MemberInfo implements ITargetSelectorRemappable, ITargetSelectorConstructor {
    
    /**
     * Separator for elements in the path
     */
    private static final String ARROW = "->";
    
    /**
     * Member owner in internal form but without L;, can be null
     */
    private final String owner;
    
    /**
     * Member name, can be null to match any member
     */
    private final String name;
    
    /**
     * Member descriptor, can be null
     */
    private final String desc;
    
    /**
     * Required matches
     */
    private final Quantifier matches;
    
    /**
     * Force this member to report as a field
     */
    private final boolean forceField;
    
    /**
     * The actual String value passed into the {@link #parse} method 
     */
    private final String input;
    
    /**
     * The actual String value passed into the {@link #parse} method 
     */
    private final String tail;
    
    /**
     * ctor
     * 
     * @param name Member name, must not be null
     * @param matches Quantifier specifying the number of matches required
     */
    public MemberInfo(String name, Quantifier matches) {
        this(name, null, null, matches, null, null);
    }
    
    /**
     * ctor
     * 
     * @param name Member name, must not be null
     * @param owner Member owner, can be null otherwise must be in internal form
     *      without L;
     * @param matches Quantifier specifying the number of matches required
     */
    public MemberInfo(String name, String owner, Quantifier matches) {
        this(name, owner, null, matches, null, null);
    }
    
    /**
     * ctor
     * 
     * @param name Member name, must not be null
     * @param owner Member owner, can be null otherwise must be in internal form
     *      without L;
     * @param desc Member descriptor, can be null
     */
    public MemberInfo(String name, String owner, String desc) {
        this(name, owner, desc, Quantifier.DEFAULT, null, null);
    }
    
    /**
     * ctor
     * 
     * @param name Member name, must not be null
     * @param owner Member owner, can be null otherwise must be in internal form
     *      without L;
     * @param desc Member descriptor, can be null
     * @param matches Quantifier specifying the number of matches required
     */
    public MemberInfo(String name, String owner, String desc, Quantifier matches) {
        this(name, owner, desc, matches, null, null);
    }
    
    /**
     * ctor
     * 
     * @param name Member name, must not be null
     * @param owner Member owner, can be null otherwise must be in internal form
     *      without L;
     * @param desc Member descriptor, can be null
     * @param matches Quantifier specifying the number of matches required
     */
    public MemberInfo(String name, String owner, String desc, Quantifier matches, String tail) {
        this(name, owner, desc, matches, tail, null);
    }
    
    /**
     * ctor
     * 
     * @param name Member name, must not be null
     * @param owner Member owner, can be null otherwise must be in internal form
     *      without L;
     * @param desc Member descriptor, can be null
     * @param matches Quantifier specifying the number of matches required
     */
    public MemberInfo(String name, String owner, String desc, Quantifier matches, String tail, String input) {
        if (owner != null && owner.contains(".")) {
            throw new IllegalArgumentException("Attempt to instance a MemberInfo with an invalid owner format");
        }
        
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.matches = matches;
        this.forceField = false;
        this.tail = tail;
        this.input = input;
    }
    
    /**
     * Initialise a MemberInfo using the supplied insn which must be an instance
     * of MethodInsnNode or FieldInsnNode.
     * 
     * @param insn instruction node to copy values from
     */
    public MemberInfo(AbstractInsnNode insn) {
        this.matches = Quantifier.DEFAULT;
        this.forceField = false;
        this.input = null;
        this.tail = null;
        
        if (insn instanceof MethodInsnNode) {
            MethodInsnNode methodNode = (MethodInsnNode) insn;
            this.owner = methodNode.owner;
            this.name = methodNode.name;
            this.desc = methodNode.desc;
        } else if (insn instanceof FieldInsnNode) {
            FieldInsnNode fieldNode = (FieldInsnNode) insn;
            this.owner = fieldNode.owner;
            this.name = fieldNode.name;
            this.desc = fieldNode.desc;
        } else {
            throw new IllegalArgumentException("insn must be an instance of MethodInsnNode or FieldInsnNode");
        }
    }
    
    /**
     * Initialise a MemberInfo using the supplied mapping object
     * 
     * @param mapping Mapping object to copy values from
     */
    public MemberInfo(IMapping<?> mapping) {
        this.owner = mapping.getOwner();
        this.name = mapping.getSimpleName();
        this.desc = mapping.getDesc();
        this.matches = Quantifier.SINGLE;
        this.forceField = mapping.getType() == IMapping.Type.FIELD;
        this.tail = null;
        this.input = null;
    }
    
    /**
     * Initialise a remapped MemberInfo using the supplied mapping object
     * 
     * @param method mapping method object to copy values from
     */
    private MemberInfo(MemberInfo remapped, MappingMethod method, boolean setOwner) {
        this.owner = setOwner ? method.getOwner() : remapped.owner;
        this.name = method.getSimpleName();
        this.desc = method.getDesc();
        this.matches = remapped.matches;
        this.forceField = false;
        this.tail = null;
        this.input = null;
    }

    /**
     * Initialise a remapped MemberInfo with a new name
     * 
     * @param original Original MemberInfo
     * @param owner new owner
     */
    private MemberInfo(MemberInfo original, String owner) {
        this.owner = owner;
        this.name = original.name;
        this.desc = original.desc;
        this.matches = original.matches;
        this.forceField = original.forceField;
        this.tail = original.tail;
        this.input = null;
    }
    
    @Override
    public ITargetSelector next() {
        return Strings.isNullOrEmpty(this.tail) ? null : MemberInfo.parse(this.tail, null);
    }
    
    @Override
    public String getOwner() {
        return this.owner;
    }
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public String getDesc() {
        return this.desc;
    }
    
    @Override
    public int getMinMatchCount() {
        return this.matches.getClampedMin();
    }
    
    @Override
    public int getMaxMatchCount() {
        return this.matches.getClampedMax();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String owner = this.owner != null ? "L" + this.owner + ";" : "";
        String name = this.name != null ? this.name : "";
        String quantifier = this.matches.toString();
        String desc = this.desc != null ? this.desc : "";
        String separator = desc.startsWith("(") ? "" : (this.desc != null ? ":" : "");
        String tail = this.tail != null ? " " + MemberInfo.ARROW + " " + this.tail : ""; 
        return owner + name + quantifier + separator + desc + tail;
    }

    /**
     * Return this MemberInfo as an SRG mapping
     * 
     * @return SRG representation of this MemberInfo
     * @deprecated use m.asMethodMapping().serialise() instead
     */
    @Deprecated
    public String toSrg() {
        if (!this.isFullyQualified()) {
            throw new MixinException("Cannot convert unqualified reference to SRG mapping");
        }
        
        if (this.desc.startsWith("(")) {
            return this.owner + "/" + this.name + " " + this.desc;
        }
        
        return this.owner + "/" + this.name;
    }
    
    /**
     * Returns this MemberInfo as a java-style descriptor 
     */
    @Override
    public String toDescriptor() {
        if (this.desc == null) {
            return "";
        }
        
        return new SignaturePrinter(this).setFullyQualified(true).toDescriptor();
    }
    
    /**
     * Returns the <em>constructor type</em> represented by this MemberInfo
     */
    @Override
    public String toCtorType() {
        if (this.input == null) {
            return null;
        }
        
        String returnType = this.getReturnType();
        if (returnType != null) {
            return returnType;
        }

        if (this.owner != null) {
            return this.owner;
        }

        if (this.name != null && this.desc == null) {
            return this.name;
        }

        return this.desc != null ? this.desc : this.input;
    }
    
    /**
     * Returns the <em>constructor descriptor</em> represented by this
     * MemberInfo, returns null if no descriptor is present.
     */
    @Override
    public String toCtorDesc() {
        return Bytecode.changeDescriptorReturnType(this.desc, "V");
    }
    
    /**
     * Get the return type for this MemberInfo, if the decriptor is present,
     * returns null if the descriptor is absent or if this MemberInfo represents
     * a field
     */
    private String getReturnType() {
        if (this.desc == null || this.desc.indexOf(')') == -1 || this.desc.indexOf('(') != 0 ) {
            return null;
        }
        
        String returnType = this.desc.substring(this.desc.indexOf(')') + 1);
        if (returnType.startsWith("L") && returnType.endsWith(";")) {
            return returnType.substring(1, returnType.length() - 1);
        }
        return returnType;
    }

    /**
     * Returns this MemberInfo as a {@link MappingField} or
     * {@link MappingMethod}
     */
    @Override
    public IMapping<?> asMapping() {
        return this.isField() ? this.asFieldMapping() : this.asMethodMapping();
    }
    
    /**
     * Returns this MemberInfo as a mapping method
     */
    @Override
    public MappingMethod asMethodMapping() {
        if (!this.isFullyQualified()) {
            throw new MixinException("Cannot convert unqualified reference " + this + " to MethodMapping");
        }
        
        if (this.isField()) {
            throw new MixinException("Cannot convert a non-method reference " + this + " to MethodMapping");
        }
        
        return new MappingMethod(this.owner, this.name, this.desc);
    }
    
    /**
     * Returns this MemberInfo as a mapping field
     */
    @Override
    public MappingField asFieldMapping() {
        if (!this.isField()) {
            throw new MixinException("Cannot convert non-field reference " + this + " to FieldMapping");
        }
        
        return new MappingField(this.owner, this.name, this.desc);
    }
    
    @Override
    public boolean isFullyQualified() {
        return this.owner != null && this.name != null && this.desc != null;
    }
    
    /**
     * Get whether this MemberInfo is definitely a field, the output of this
     * method is undefined if {@link #isFullyQualified} returns false.
     * 
     * @return true if this is definitely a field
     */
    @Override
    public boolean isField() {
        return this.forceField || (this.desc != null && !this.desc.startsWith("("));
    }
    
    /**
     * Get whether this member represents a constructor
     * 
     * @return true if member name is <tt>&lt;init&gt;</tt>
     */
    @Override
    public boolean isConstructor() {
        return Constants.CTOR.equals(this.name);
    }
    
    /**
     * Get whether this member represents a class initialiser
     * 
     * @return true if member name is <tt>&lt;clinit&gt;</tt>
     */
    @Override
    public boolean isClassInitialiser() {
        return Constants.CLINIT.equals(this.name);
    }
    
    /**
     * Get whether this member represents a constructor or class initialiser
     * 
     * @return true if member name is <tt>&lt;init&gt;</tt> or
     *      <tt>&lt;clinit&gt;</tt>
     */
    @Override
    public boolean isInitialiser() {
        return this.isConstructor() || this.isClassInitialiser();
    }
    
    /**
     * Perform ultra-simple validation of the descriptor, checks that the parts
     * of the descriptor are basically sane.
     * 
     * @return fluent
     * 
     * @throws InvalidSelectorException if any validation check fails
     */
    @Override
    public MemberInfo validate() throws InvalidSelectorException {
        // Parse emits a match count of 0 if the quantifier is incorrectly specified
        if (this.getMaxMatchCount() == 0) {
            throw new InvalidMemberDescriptorException(this.input, "Malformed quantifier in selector: " + this.input);
        }
        
        // Extremely naive class name validation, just to spot really egregious errors
        if (this.owner != null) {
            if (!this.owner.matches("(?i)^[\\w\\p{Sc}/]+$")) {
                throw new InvalidMemberDescriptorException(this.input, "Invalid owner: " + this.owner);
            }
            // We can't detect this situation 100% reliably, but we can take a
            // decent stab at it in order to detect really obvious cases where
            // the user types a dot instead of a semicolon
            if (this.input != null && this.input.lastIndexOf('.') > 0 && this.owner.startsWith("L")) {
                throw new InvalidMemberDescriptorException(this.input, "Malformed owner: " + this.owner + " If you are seeing this message"
                        + "unexpectedly and the owner appears to be correct, replace the owner descriptor with formal type L" + this.owner
                        + "; to suppress this error");
            }
        }
        
        // Also naive validation, we're looking for stupid errors here
        if (this.name != null && !this.name.matches("(?i)^<?[\\w\\p{Sc}]+>?$")) {
            throw new InvalidMemberDescriptorException(this.input, "Invalid name: " + this.name);
        }
        
        if (this.desc != null) {
            if (!this.desc.matches("^(\\([\\w\\p{Sc}\\[/;]*\\))?\\[*[\\w\\p{Sc}/;]+$")) {
                throw new InvalidMemberDescriptorException(this.input, "Invalid descriptor: " + this.desc);
            }
            if (this.isField()) {
                if (!this.desc.equals(Type.getType(this.desc).getDescriptor())) {
                    throw new InvalidMemberDescriptorException(this.input, "Invalid field type in descriptor: " + this.desc);
                }
            } else {
                try {
                    // getArgumentTypes can choke on some invalid descriptors
                    Type[] argTypes = Type.getArgumentTypes(this.desc);
                    // getInternalName is a useful litmus test for improperly-formatted types which parse out of
                    // the descriptor correctly but are actually invalid, for example unterminated class names.
                    // However it doesn't support primitive types properly in ASM versions before 6 so don't run
                    // the test unless running ASM 6 or later
                    if (ASM.isAtLeastVersion(6)) {
                        for (Type argType : argTypes) {
                            argType.getInternalName();
                        }
                    }
                } catch (Exception ex) {
                    throw new InvalidMemberDescriptorException(this.input, "Invalid descriptor: " + this.desc);
                }
    
                String retString = this.desc.substring(this.desc.indexOf(')') + 1);
                try {
                    Type retType = Type.getType(retString);
                    int sort = retType.getSort();
                    if (sort >= Type.ARRAY) {
                        retType.getInternalName(); // sanity check
                    }
                    if (!retString.equals(retType.getDescriptor())) {
                        throw new InvalidMemberDescriptorException(this.input, "Invalid return type \"" + retString + "\" in descriptor: "
                                + this.desc);
                    }
                } catch (Exception ex) {
                    throw new InvalidMemberDescriptorException(this.input, "Invalid return type \"" + retString + "\" in descriptor: "
                            + this.desc);
                }
            }
        }
        
        return this;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ITargetSelector
     *      #match(org.spongepowered.asm.util.asm.ElementNode)
     */
    @Override
    public <TNode> MatchResult match(ElementNode<TNode> node) {
        return node == null ? MatchResult.NONE : this.matches(node.getOwner(), node.getName(), node.getDesc());
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ITargetSelector
     *      #matches(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public MatchResult matches(String owner, String name, String desc) {
        if (this.desc != null && desc != null && !this.desc.equals(desc)) {
            return MatchResult.NONE;
        }
        if (this.owner != null && owner != null && !this.owner.equals(owner)) {
            return MatchResult.NONE;
        }
        if (this.name != null && name != null) {
            if (this.name.equals(name)) {
                return MatchResult.EXACT_MATCH;
            }
            if (this.name.equalsIgnoreCase(name)) {
                return MatchResult.MATCH;
            }
            return MatchResult.NONE;
        }
        return MatchResult.EXACT_MATCH;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ITargetSelectorByName)) {
            return false;
        }
        
        ITargetSelectorByName other = (ITargetSelectorByName)obj;
        boolean otherForceField = other instanceof MemberInfo ? ((MemberInfo)other).forceField
                : other instanceof ITargetSelectorRemappable ? ((ITargetSelectorRemappable)other).isField() : false;
        
        return this.compareMatches(other) && this.forceField == otherForceField
                && Objects.equal(this.owner, other.getOwner())
                && Objects.equal(this.name, other.getName())
                && Objects.equal(this.desc, other.getDesc());
    }
    
    /**
     * Compare local match count with match count of other selector
     */
    private boolean compareMatches(ITargetSelectorByName other) {
        if (other instanceof MemberInfo) {
            return ((MemberInfo)other).matches.equals(this.matches);
        }
        return this.getMinMatchCount() == other.getMinMatchCount() && this.getMaxMatchCount() == other.getMaxMatchCount();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(this.matches, this.owner, this.name, this.desc);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ITargetSelector
     *      #mutate(java.lang.String[])
     */
    @Override
    public ITargetSelector configure(Configure request, String... args) {
        request.checkArgs(args);
        switch (request) {
            case SELECT_MEMBER:
                if (this.matches.isDefault()) {
                    return new MemberInfo(this.name, this.owner, this.desc, Quantifier.SINGLE, this.tail);
                }
                break;
            case SELECT_INSTRUCTION:
                if (this.matches.isDefault()) {
                    return new MemberInfo(this.name, this.owner, this.desc, Quantifier.ANY, this.tail);
                }
                break;
            case MOVE:
                return this.move(Strings.emptyToNull(args[0]));
            case ORPHAN:
                return this.move(null);
            case TRANSFORM:
                return this.transform(Strings.emptyToNull(args[0]));
            case PERMISSIVE:
                return this.transform(null);
            case CLEAR_LIMITS:
                if (this.matches.getMin() != 0 || this.matches.getMax() < Integer.MAX_VALUE) {
                    return new MemberInfo(this.name, this.owner, this.desc, Quantifier.ANY, this.tail);
                }
                break;
            default:
                break;
        }
        return this;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ITargetSelector
     *      #attach(org.spongepowered.asm.mixin.refmap.IMixinContext)
     */
    @Override
    public ITargetSelector attach(ISelectorContext context) throws InvalidSelectorException {
        if (this.owner != null && !this.owner.equals(context.getMixin().getTargetClassRef())) {
            throw new TargetNotSupportedException(this.owner);
        }
        return this;
    }
    
    /**
     * Create a new version of this member with a different owner
     * 
     * @param newOwner New owner for this member
     */
    @Override
    public ITargetSelectorRemappable move(String newOwner) {
        if ((newOwner == null && this.owner == null) || (newOwner != null && newOwner.equals(this.owner))) {
            return this;
        }
        return new MemberInfo(this, newOwner); 
    }
    
    /**
     * Create a new version of this member with a different descriptor
     * 
     * @param newDesc New descriptor for this member
     */
    @Override
    public ITargetSelectorRemappable transform(String newDesc) {
        if ((newDesc == null && this.desc == null) || (newDesc != null && newDesc.equals(this.desc))) {
            return this;
        }
        return new MemberInfo(this.name, this.owner, newDesc, this.matches); 
    }
    
    /**
     * Create a remapped version of this member using the supplied method data
     * 
     * @param srgMethod SRG method data to use
     * @param setOwner True to set the owner as well as the name
     * @return New MethodInfo with remapped values
     */
    @Override
    public ITargetSelectorRemappable remapUsing(MappingMethod srgMethod, boolean setOwner) {
        return new MemberInfo(this, srgMethod, setOwner);
    }
    
    /**
     * Parse a MemberInfo from a string
     * 
     * @param input String to parse MemberInfo from
     * @param context Selector context for this parse request
     * @return parsed MemberInfo
     */
    public static MemberInfo parse(final String input, final ISelectorContext context) {
        String desc = null;
        String owner = null;
        String name = Strings.nullToEmpty(input).replaceAll("\\s", "");
        String tail = null;
        
        int arrowPos = name.indexOf(MemberInfo.ARROW);
        if (arrowPos > -1) {
            tail = name.substring(arrowPos + 2);
            name = name.substring(0, arrowPos);
        }

        if (context != null) {
            name = context.remap(name);
        }
        
        int lastDotPos = name.lastIndexOf('.');
        int semiColonPos = name.indexOf(';');
        if (lastDotPos > -1) {
            owner = name.substring(0, lastDotPos).replace('.', '/');
            name = name.substring(lastDotPos + 1);
        } else if (semiColonPos > -1 && name.startsWith("L")) {
            owner = name.substring(1, semiColonPos).replace('.', '/');
            name = name.substring(semiColonPos + 1);
        }

        int parenPos = name.indexOf('(');
        int colonPos = name.indexOf(':');
        if (parenPos > -1) {
            desc = name.substring(parenPos);
            name = name.substring(0, parenPos);
        } else if (colonPos > -1) {
            desc = name.substring(colonPos + 1);
            name = name.substring(0, colonPos);
        }
        
        if ((name.indexOf('/') > -1 || name.indexOf('.') > -1) && owner == null) {
            owner = name;
            name = "";
        }
        
        // Use default quantifier with negative max value. Used to indicate that
        // an explicit quantifier was not parsed from the selector string, this
        // allows us to provide backward-compatible behaviour for injection
        // points vs. selecting target members which have different default
        // semantics when omitting the quantifier. This is handled by consumers
        // calling configure() with SELECT_MEMBER or SELECT_INSTRUCTION to
        // promote the default case to a concrete case.
        Quantifier quantifier = Quantifier.DEFAULT;
        if (name.endsWith("*")) {
            quantifier = Quantifier.ANY;
            name = name.substring(0, name.length() - 1);
        } else if (name.endsWith("+")) {
            quantifier = Quantifier.PLUS;
            name = name.substring(0, name.length() - 1);
        } else if (name.endsWith("}")) {
            quantifier = Quantifier.NONE; // Assume invalid until quantifier is parsed
            int bracePos = name.indexOf("{");
            if (bracePos >= 0) {
                try {
                    quantifier = Quantifier.parse(name.substring(bracePos, name.length()));
                    name = name.substring(0, bracePos);
                } catch (Exception ex) {
                    // Handled later in validate since matchCount will be 0
                }
            }
        } else if (name.indexOf("{") >= 0) {
            quantifier = Quantifier.NONE; // Probably incomplete quantifier
        }
        
        if (name.isEmpty()) {
            name = null;
        }
        
        return new MemberInfo(name, owner, desc, quantifier, tail, input);
    }

    /**
     * Return the supplied mapping parsed as a MemberInfo
     * 
     * @param mapping mapping to parse
     * @return new MemberInfo
     */
    public static MemberInfo fromMapping(IMapping<?> mapping) {
        return new MemberInfo(mapping);
    }

}
