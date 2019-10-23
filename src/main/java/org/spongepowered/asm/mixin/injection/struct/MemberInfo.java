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
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorByName;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorConstructor;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorRemappable;
import org.spongepowered.asm.mixin.injection.selectors.InvalidSelectorException;
import org.spongepowered.asm.mixin.injection.selectors.MatchResult;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.obfuscation.mapping.IMapping;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.SignaturePrinter;
import org.spongepowered.asm.util.asm.ElementNode;

import com.google.common.base.Objects;
import com.google.common.base.Strings;

/**
 * <p>Target selector, also used as a general-purpose information bundle about a
 * member (method or field) parsed from a String token in another annotation,
 * this is used where target members need to be specified as Strings in order to
 * parse the String representation to something useful. See
 * {@link ITargetSelector} for other supported selector types.</p>
 * 
 * <p>Some examples:</p>
 * <blockquote><pre>
 *   // references a method or field called func_1234_a, if there are multiple
 *   // members with the same signature, matches the first occurrence
 *   func_1234_a
 *   
 *   // references a method or field called func_1234_a, if there are multiple
 *   // members with the same signature, matches all occurrences
 *   func_1234_a*
 *   
 *   // references a method called func_1234_a which takes 3 ints and returns
 *   // a bool
 *   func_1234_a(III)Z
 *   
 *   // references a field called field_5678_z which is a String
 *   field_5678_z:Ljava/lang/String;
 *   
 *   // references a ctor which takes a single String argument 
 *   &lt;init&gt;(Ljava/lang/String;)V
 *   
 *   // references a method called func_1234_a in class foo.bar.Baz
 *   Lfoo/bar/Baz;func_1234_a
 *  
 *   // references a field called field_5678_z in class com.example.Dave
 *   Lcom/example/Dave;field_5678_z
 *  
 *   // references a method called func_1234_a in class foo.bar.Baz which takes
 *   // three doubles and returns void
 *   Lfoo/bar/Baz;func_1234_a(DDD)V
 *   
 *   // alternate syntax for the same
 *   foo.bar.Baz.func_1234_a(DDD)V</pre>
 * </blockquote>
 */
public final class MemberInfo implements ITargetSelectorRemappable, ITargetSelectorConstructor {
    
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
     * True to match all matching members, not just the first
     */
    private final boolean matchAll;
    
    /**
     * Force this member to report as a field
     */
    private final boolean forceField;
    
    /**
     * The actual String value passed into the {@link #parse} method 
     */
    private final String unparsed;

    /**
     * ctor
     * 
     * @param name Member name, must not be null
     * @param matchAll true if this info should match all matching references,
     *      or only the first
     */
    public MemberInfo(String name, boolean matchAll) {
        this(name, null, null, matchAll);
    }
    
    /**
     * ctor
     * 
     * @param name Member name, must not be null
     * @param owner Member owner, can be null otherwise must be in internal form
     *      without L;
     * @param matchAll true if this info should match all matching references,
     *      or only the first
     */
    public MemberInfo(String name, String owner, boolean matchAll) {
        this(name, owner, null, matchAll);
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
        this(name, owner, desc, false);
    }
    
    /**
     * ctor
     * 
     * @param name Member name, must not be null
     * @param owner Member owner, can be null otherwise must be in internal form
     *      without L;
     * @param desc Member descriptor, can be null
     * @param matchAll True to match all matching members, not just the first
     */
    public MemberInfo(String name, String owner, String desc, boolean matchAll) {
        this(name, owner, desc, matchAll, null);
    }
    
    /**
     * ctor
     * 
     * @param name Member name, must not be null
     * @param owner Member owner, can be null otherwise must be in internal form
     *      without L;
     * @param desc Member descriptor, can be null
     * @param matchAll True to match all matching members, not just the first
     */
    public MemberInfo(String name, String owner, String desc, boolean matchAll, String unparsed) {
        if (owner != null && owner.contains(".")) {
            throw new IllegalArgumentException("Attempt to instance a MemberInfo with an invalid owner format");
        }
        
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.matchAll = matchAll;
        this.forceField = false;
        this.unparsed = unparsed;
    }
    
    /**
     * Initialise a MemberInfo using the supplied insn which must be an instance
     * of MethodInsnNode or FieldInsnNode.
     * 
     * @param insn instruction node to copy values from
     */
    public MemberInfo(AbstractInsnNode insn) {
        this.matchAll = false;
        this.forceField = false;
        this.unparsed = null;
        
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
        this.matchAll = false;
        this.forceField = mapping.getType() == IMapping.Type.FIELD;
        this.unparsed = null;
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
        this.matchAll = remapped.matchAll;
        this.forceField = false;
        this.unparsed = null;
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
        this.matchAll = original.matchAll;
        this.forceField = original.forceField;
        this.unparsed = null;
    }
    
    @Override
    public ITargetSelector next() {
        return null; // Not supported yet
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
    public int getMatchCount() {
        return this.matchAll ? Integer.MAX_VALUE : 1;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String owner = this.owner != null ? "L" + this.owner + ";" : "";
        String name = this.name != null ? this.name : "";
        String qualifier = this.matchAll ? "*" : "";
        String desc = this.desc != null ? this.desc : "";
        String separator = desc.startsWith("(") ? "" : (this.desc != null ? ":" : "");
        return owner + name + qualifier + separator + desc;
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
        if (this.unparsed == null) {
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

        return this.desc != null ? this.desc : this.unparsed;
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
     * @throws InvalidMemberDescriptorException if any validation check fails
     */
    @Override
    public MemberInfo validate() throws InvalidMemberDescriptorException {
        // Extremely naive class name validation, just to spot really egregious errors
        if (this.owner != null) {
            if (!this.owner.matches("(?i)^[\\w\\p{Sc}/]+$")) {
                throw new InvalidMemberDescriptorException("Invalid owner: " + this.owner);
            }
            // We can't detect this situation 100% reliably, but we can take a
            // decent stab at it in order to detect really obvious cases where
            // the user types a dot instead of a semicolon
            if (this.unparsed != null && this.unparsed.lastIndexOf('.') > 0 && this.owner.startsWith("L")) {
                throw new InvalidMemberDescriptorException("Malformed owner: " + this.owner + " If you are seeing this message unexpectedly and the"
                        + " owner appears to be correct, replace the owner descriptor with formal type L" + this.owner + "; to suppress this error");
            }
        }
        
        // Also naive validation, we're looking for stupid errors here
        if (this.name != null && !this.name.matches("(?i)^<?[\\w\\p{Sc}]+>?$")) {
            throw new InvalidMemberDescriptorException("Invalid name: " + this.name);
        }
        
        if (this.desc != null) {
            if (!this.desc.matches("^(\\([\\w\\p{Sc}\\[/;]*\\))?\\[*[\\w\\p{Sc}/;]+$")) {
                throw new InvalidMemberDescriptorException("Invalid descriptor: " + this.desc);
            }
            if (this.isField()) {
                if (!this.desc.equals(Type.getType(this.desc).getDescriptor())) {
                    throw new InvalidMemberDescriptorException("Invalid field type in descriptor: " + this.desc);
                }
            } else {
                try {
                    Type.getArgumentTypes(this.desc);
                } catch (Exception ex) {
                    throw new InvalidMemberDescriptorException("Invalid descriptor: " + this.desc);
                }
    
                String retString = this.desc.substring(this.desc.indexOf(')') + 1);
                try {
                    Type retType = Type.getType(retString);
                    if (!retString.equals(retType.getDescriptor())) {
                        throw new InvalidMemberDescriptorException("Invalid return type \"" + retString + "\" in descriptor: " + this.desc);
                    }
                } catch (Exception ex) {
                    throw new InvalidMemberDescriptorException("Invalid return type \"" + retString + "\" in descriptor: " + this.desc);
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
        return node == null ? MatchResult.NONE : this.matches(node.getOwnerName(), node.getName(), node.getDesc());
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ITargetSelector
     *      #matches(org.objectweb.asm.tree.AbstractInsnNode)
     */
    @Override
    public MatchResult match(AbstractInsnNode insn) {
        if (insn instanceof MethodInsnNode) {
            MethodInsnNode method = (MethodInsnNode)insn;
            return this.matches(method.owner, method.name, method.desc);
        } else if (insn instanceof FieldInsnNode) {
            FieldInsnNode field = (FieldInsnNode)insn;
            return this.matches(field.owner, field.name, field.desc);
        }
        return MatchResult.NONE;
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
        boolean otherForceField = other instanceof MemberInfo ? ((MemberInfo)other).forceField : other.isField();
        boolean otherMatchAll = other.getMatchCount() == Integer.MAX_VALUE;
        
        return this.matchAll == otherMatchAll && this.forceField == otherForceField
                && Objects.equal(this.owner, other.getOwner())
                && Objects.equal(this.name, other.getName())
                && Objects.equal(this.desc, other.getDesc());
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(this.matchAll, this.owner, this.name, this.desc);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ITargetSelector
     *      #configure(java.lang.String[])
     */
    @Override
    public ITargetSelector configure(String... args) {
        ITargetSelectorRemappable configured = this;
        for (String arg : args) {
            if (arg == null) {
                continue;
            }
            
            if (arg.startsWith("move:")) {
                configured = configured.move(Strings.emptyToNull(arg.substring(5)));
            } else if (arg.startsWith("transform:")) {
                configured = configured.transform(Strings.emptyToNull(arg.substring(10)));
            } else if ("permissive".equals(arg)) {
                configured = configured.transform(null);
            } else if ("orphan".equals(arg)) {
                configured = configured.move(null);
            }
        }
        return configured;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ITargetSelector
     *      #attach(org.spongepowered.asm.mixin.refmap.IMixinContext)
     */
    @Override
    public ITargetSelector attach(IMixinContext context) throws InvalidSelectorException {
        if (this.owner != null && !this.owner.equals(context.getTargetClassRef())) {
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
        return new MemberInfo(this.name, this.owner, newDesc, this.matchAll); 
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
     * @param refMapper Reference mapper to use
     * @param className Class context to use for remapping
     * @return parsed MemberInfo
     */
    public static MemberInfo parse(String input, IReferenceMapper refMapper, String className) {
        String desc = null;
        String owner = null;
        String name = Strings.nullToEmpty(input).replaceAll("\\s", "");

        if (refMapper != null) {
            name = refMapper.remap(className, name);
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
        
        boolean matchAll = name.endsWith("*");
        if (matchAll) {
            name = name.substring(0, name.length() - 1);
        }
        
        if (name.isEmpty()) {
            name = null;
        }
        
        return new MemberInfo(name, owner, desc, matchAll, input);
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
