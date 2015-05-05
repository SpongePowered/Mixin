/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered.org <http://www.spongepowered.org>
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

import net.minecraftforge.srg2source.rangeapplier.MethodData;

import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.FieldInsnNode;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.transformer.IReferenceMapperContext;
import org.spongepowered.asm.util.SignaturePrinter;


/**
 * <p>Information bundle about a member (method or field) parsed from a String
 * token in another annotation, this is used where target members need to be
 * specified as Strings in order to parse the String representation to something
 * useful.</p>
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
public class MemberInfo {
    
    /**
     * Member owner in internal form but without L;, can be null
     */
    public final String owner;
    
    /**
     * Member name, can be null to match any member
     */
    public final String name;
    
    /**
     * Member descriptor, can be null
     */
    public final String desc;
    
    /**
     * True to match all matching members, not just the first
     */
    public final boolean matchAll;

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
     * @param matchAll True to match all matching members, not just the first
     */
    public MemberInfo(String name, String owner, String desc, boolean matchAll) {
        if (owner != null && owner.contains(".")) {
            throw new IllegalArgumentException("Attempt to instance a MemberInfo with an invalid owner format");
        }
        
        this.owner    = owner;
        this.name     = name;
        this.desc     = desc;
        this.matchAll = matchAll;
    }
    
    /**
     * Initialise a MemberInfo using the supplied insn which must be an instance
     * of MethodInsnNode or FieldInsnNode.
     * 
     * @param insn instruction node to copy values from
     */
    public MemberInfo(AbstractInsnNode insn) {
        this.matchAll = false;
        
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
     * Initialise a MemberInfo using the supplied MethodData object
     * 
     * @param methodData MethodData object to copy values from
     */
    private MemberInfo(MethodData methodData) {
        int slashPos = methodData.name.lastIndexOf('/');
        this.owner = methodData.name.substring(0, slashPos);
        this.name = methodData.name.substring(slashPos + 1);
        this.desc = methodData.sig;
        this.matchAll = false;
    }
    
    /**
     * Initialise a remapped MemberInfo using the supplied MethodData object
     * 
     * @param methodData MethodData object to copy values from
     */
    private MemberInfo(MemberInfo remapped, MethodData methodData, boolean setOwner) {
        int slashPos = methodData.name.lastIndexOf('/');
        this.owner = setOwner ? methodData.name.substring(0, slashPos) : remapped.owner;
        this.name = methodData.name.substring(slashPos + 1);
        this.desc = remapped.desc;
        this.matchAll = false;
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
     */
    public String toSrg() {
        if (!this.isFullyQualified()) {
            throw new RuntimeException("Cannot convert unqalified reference to SRG mapping");
        }
        
        if (this.desc.startsWith("(")) {
            return this.owner + "/" + this.name + " " + this.desc;
        }
        
        return this.owner + "/" + this.name;
    }
    
    public String toDescriptor() {
        if (this.desc == null) {
            return "";
        }
        
        return new SignaturePrinter(this).setFullyQualified(true).toDescriptor();
    }
    
    public MethodData asMethodData() {
        if (!this.isFullyQualified()) {
            throw new RuntimeException("Cannot convert unqalified reference to MethodData");
        }
        
        if (this.isField()) {
            throw new RuntimeException("Cannot convert a non-method reference to MethodData");
        }
        
        return new MethodData(this.owner + "/" + this.name, this.desc);
    }
    
    /**
     * Get whether this reference is fully qualified
     * 
     * @return true if all components of this reference are non-null 
     */
    public boolean isFullyQualified() {
        return this.owner != null && this.name != null && this.desc != null;
    }
    
    /**
     * Get whether this MemberInfo is definitely a field, the output of this
     * method is undefined if {@link #isFullyQualified} returns false.
     * 
     * @return true if this is definitely a field
     */
    public boolean isField() {
        return this.desc != null && !this.desc.startsWith("(");
    }

    /**
     * Test whether this MemberInfo matches the supplied values. Null values are
     * ignored.

     * @param owner Owner to compare with, null to skip
     * @param name Name to compare with, null to skip
     * @param desc Signature to compare with, null to skip
     * @return true if all non-null values in this reference match non-null
     *      arguments supplied to this method
     */
    public boolean matches(String owner, String name, String desc) {
        return this.matches(owner, name, desc, 0);
    }
    
    /**
     * Test whether this MemberInfo matches the supplied values at the specified
     * ordinal. Null values are ignored.
     * 
     * @param owner Owner to compare with, null to skip
     * @param name Name to compare with, null to skip
     * @param desc Signature to compare with, null to skip
     * @param ordinal ordinal position within the class, used to honour the
     *      matchAll semantics
     * @return true if all non-null values in this reference match non-null
     *      arguments supplied to this method
     */
    public boolean matches(String owner, String name, String desc, int ordinal) {
        if (this.desc != null && desc != null && !this.desc.equals(desc)) {
            return false;
        }
        if (this.name != null && name != null && !this.name.equals(name)) {
            return false;
        }
        if (this.owner != null && owner != null && !this.owner.equals(owner)) {
            return false;
        }
        return ordinal == 0 || this.matchAll;
    }

    /**
     * Test whether this MemberInfo matches the supplied values. Null values are
     * ignored.

     * @param name Name to compare with, null to skip
     * @param desc Signature to compare with, null to skip
     * @return true if all non-null values in this reference match non-null
     *      arguments supplied to this method
     */
    public boolean matches(String name, String desc) {
        return this.matches(name, desc, 0);
    }
    
    /**
     * Test whether this MemberInfo matches the supplied values at the specified
     * ordinal. Null values are ignored.
     * 
     * @param name Name to compare with, null to skip
     * @param desc Signature to compare with, null to skip
     * @param ordinal ordinal position within the class, used to honour the
     *      matchAll semantics
     * @return true if all non-null values in this reference match non-null
     *      arguments supplied to this method
     */
    public boolean matches(String name, String desc, int ordinal) {
        return (this.name == null || this.name.equals(name)) 
            && (this.desc == null || (desc != null && desc.equals(this.desc)))
            && (ordinal == 0 || this.matchAll);
    }
    
    /**
     * Create a remapped version of this member using the supplied method data
     * 
     * @param methodData Method data to use
     * @param setOwner True to set the owner as well as the name
     * @return New MethodInfo with remapped values
     */
    public MemberInfo remapUsing(MethodData methodData, boolean setOwner) {
        return new MemberInfo(this, methodData, setOwner);
    }
    
    /**
     * Parse a MemberInfo from a string
     * 
     * @param string String to parse MemberInfo from
     * @return parsed MemberInfo
     */
    public static MemberInfo parse(String string) {
        return MemberInfo.parse(string, null, null);
    }
    
    /**
     * Parse a MemberInfo from a string
     * 
     * @param string String to parse MemberInfo from
     * @param context Context to use for reference mapping
     * @return parsed MemberInfo
     */
    public static MemberInfo parse(String string, IReferenceMapperContext context) {
        return MemberInfo.parse(string, context.getReferenceMapper(), context.getClassRef());
    }
    
    /**
     * Parse a MemberInfo from a string
     * 
     * @param name String to parse MemberInfo from
     * @param refMapper Reference mapper to use
     * @param mixinClass Mixin class to use for remapping
     * @return parsed MemberInfo
     */
    public static MemberInfo parse(String name, ReferenceMapper refMapper, String mixinClass) {
        String desc = null;
        String owner = null;
        
        if (refMapper != null) {
            name = refMapper.remap(mixinClass, name);
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
        
        boolean matchAll = name.endsWith("*");
        if (matchAll) {
            name = name.substring(0, name.length() - 1);
        }
        
        if (name.isEmpty()) {
            name = null;
        }
        
        return new MemberInfo(name, owner, desc, matchAll);
    }

    public static MemberInfo fromSrgField(String srgName, String desc) {
        int slashPos = srgName.lastIndexOf('/');
        String owner = srgName.substring(0, slashPos);
        String name = srgName.substring(slashPos + 1);
        return new MemberInfo(name, owner, desc, false);
    }
    
    public static MemberInfo fromSrgMethod(MethodData methodData) {
        return new MemberInfo(methodData);
    }
}
