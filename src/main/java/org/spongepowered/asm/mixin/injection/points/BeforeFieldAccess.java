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
package org.spongepowered.asm.mixin.injection.points;

import java.util.Collection;
import java.util.Iterator;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.FieldInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.InjectionPoint.AtCode;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.util.Bytecode;

/**
 * <p>This injection point searches for GETFIELD and PUTFIELD (and static
 * equivalent) opcodes matching its arguments and returns a list of insns
 * immediately prior to matching instructions. It accepts the following
 * parameters from {@link At}:
 * </p>
 * 
 * <dl>
 *   <dt>target</dt>
 *   <dd>A {@link MemberInfo MemberInfo} which identifies the target field.</dd>
 *   <dt>opcode</dt>
 *   <dd>The {@link Opcodes opcode} of the field access, must be one of
 *   GETSTATIC, PUTSTATIC, GETFIELD or PUTFIELD.</dd>
 *   <dt>ordinal</dt>
 *   <dd>The ordinal position of the field access to match. For example if the
 *   field is referenced 3 times and you want to match the 3rd then you can
 *   specify an <em>ordinal</em> of <b>2</b> (ordinals are zero-indexed). The
 *   default value is <b>-1</b> which supresses ordinal matching</dd>
 * </dl>
 * 
 * <p>Example:</p>
 * <blockquote><pre>
 *   &#064;At(value = "FIELD", target="field_59_z:I", opcode = Opcodes.GETFIELD)
 * </pre>
 * </blockquote>
 * 
 * <p>Matching array access:</p>
 * <p>For array fields, it is possible to match field accesses followed by a
 * corresponding array element <em>get</em>, <em>set</em> or <em>length</em>
 * operation. To enable this behaviour specify the <tt>array</tt> named-argument
 * with the desired operation:</p> 
 * 
 * <blockquote><pre>
 *   &#064;At(value = "FIELD", target="myIntArray:[I", args = "array=get")
 * </pre>
 * </blockquote>
 * 
 * <p>See {@link Redirect} for information on array element redirection.</p>
 * 
 * <p>Note that like all standard injection points, this class matches the insn
 * itself, putting the injection point immediately <em>before</em> the access in
 * question. Use {@link At#shift} specifier to adjust the matched opcode as
 * necessary.</p>
 */
@AtCode("FIELD")
public class BeforeFieldAccess extends BeforeInvoke {
    
    private static final String ARRAY_GET = "get";
    private static final String ARRAY_SET = "set";
    private static final String ARRAY_LENGTH = "length";

    /**
     * Default fuzz factor for searching for array access opcodes
     */
    public static final int ARRAY_SEARCH_FUZZ_DEFAULT = 8;

    /**
     * Explicit opcode to search for, this should be omitted if searching for an
     * array access
     */
    private final int opcode;
    
    /**
     * Array opcode (base, eg. IALOAD, IASTORE) - will be translated to target
     * type by individual searches
     */
    private final int arrOpcode;
    
    /**
     * Array opcode search range ('fuzz factor'), 1 to 32 opcodes, default 8
     */
    private final int fuzzFactor;

    public BeforeFieldAccess(InjectionPointData data) {
        super(data);
        this.opcode = data.getOpcode(-1, Opcodes.GETFIELD, Opcodes.PUTFIELD, Opcodes.GETSTATIC, Opcodes.PUTSTATIC, -1);
        
        String array = data.get("array", "");
        this.arrOpcode = BeforeFieldAccess.ARRAY_GET.equalsIgnoreCase(array) ? Opcodes.IALOAD
                : BeforeFieldAccess.ARRAY_SET.equalsIgnoreCase(array) ? Opcodes.IASTORE
                : BeforeFieldAccess.ARRAY_LENGTH.equalsIgnoreCase(array) ? Opcodes.ARRAYLENGTH : 0;
        this.fuzzFactor = Math.min(Math.max(data.get("fuzz", BeforeFieldAccess.ARRAY_SEARCH_FUZZ_DEFAULT), 1), 32);
    }
    
    public int getFuzzFactor() {
        return this.fuzzFactor;
    }
    
    public int getArrayOpcode() {
        return this.arrOpcode;
    }

    private int getArrayOpcode(String desc) {
        if (this.arrOpcode != Opcodes.ARRAYLENGTH) {
            return Type.getType(desc).getElementType().getOpcode(this.arrOpcode); 
        }
        return this.arrOpcode;
    }

    @Override
    protected boolean matchesInsn(AbstractInsnNode insn) {
        if (insn instanceof FieldInsnNode && (((FieldInsnNode) insn).getOpcode() == this.opcode || this.opcode == -1)) {
            if (this.arrOpcode == 0) {
                return true;
            }
            
            if (insn.getOpcode() != Opcodes.GETSTATIC && insn.getOpcode() != Opcodes.GETFIELD) {
                return false;
            }
            
            return Type.getType(((FieldInsnNode)insn).desc).getSort() == Type.ARRAY;
        }
        
        return false;
    }
    
    @Override
    protected boolean addInsn(InsnList insns, Collection<AbstractInsnNode> nodes, AbstractInsnNode insn) {
        if (this.arrOpcode > 0) {
            FieldInsnNode fieldInsn = (FieldInsnNode)insn;
            int accOpcode = this.getArrayOpcode(fieldInsn.desc);
            this.log("{} > > > > searching for array access opcode {} fuzz={}", this.className, Bytecode.getOpcodeName(accOpcode), this.fuzzFactor);
            
            if (BeforeFieldAccess.findArrayNode(insns, fieldInsn, accOpcode, this.fuzzFactor) == null) {
                this.log("{} > > > > > failed to locate matching insn", this.className);
                return false;
            }
        }
        
        this.log("{} > > > > > adding matching insn", this.className);

        return super.addInsn(insns, nodes, insn);
    }
    
    /**
     * Searches for an array access instruction in the supplied instruction list
     * which is within <tt>searchRange</tt> instructions of the supplied field
     * instruction. Searching halts if the search range is exhausted, if an
     * {@link Opcodes#ARRAYLENGTH} opcode is encountered immediately after the
     * specified access, if a matching field access is found, or if the end of 
     * the method is reached.
     * 
     * @param insns Instruction list to search
     * @param fieldNode Field instruction to search from
     * @param opcode array access opcode to search for
     * @param searchRange search range
     * @return matching opcode or <tt>null</tt> if not matched
     */
    public static AbstractInsnNode findArrayNode(InsnList insns, FieldInsnNode fieldNode, int opcode, int searchRange) {
        int pos = 0;
        for (Iterator<AbstractInsnNode> iter = insns.iterator(insns.indexOf(fieldNode) + 1); iter.hasNext();) {
            AbstractInsnNode insn = iter.next();
            if (insn.getOpcode() == opcode) {
                return insn;
            } else if (insn.getOpcode() == Opcodes.ARRAYLENGTH && pos == 0) {
                return null;
            } else if (insn instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode) insn;
                if (field.desc.equals(fieldNode.desc) && field.name.equals(fieldNode.name) && field.owner.equals(fieldNode.owner)) {
                    return null;
                }
            }
            if (pos++ > searchRange) {
                return null;
            }
        }
        return null;
    }
    
}
