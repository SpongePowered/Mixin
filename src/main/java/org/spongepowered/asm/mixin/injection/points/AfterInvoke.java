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

import java.util.Arrays;
import java.util.Collection;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InjectionPoint.AtCode;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;

/**
 * <p>This injection point searches for INVOKEVIRTUAL, INVOKESTATIC and
 * INVOKESPECIAL opcodes matching its arguments and returns a list of insns
 * after the matching instructions, with special handling for methods
 * invocations which return a value and immediately assign it to a local
 * variable. It accepts the following parameters from
 * {@link org.spongepowered.asm.mixin.injection.At At}:</p>
 * 
 * <dl>
 *   <dt>target</dt>
 *   <dd>A
 *   {@link ITargetSelector Target Selector}
 *   which identifies the target method</dd>
 *   <dt>ordinal</dt>
 *   <dd>The ordinal position of the method invocation to match. For example if
 *   the method is invoked 3 times and you want to match the 3rd then you can
 *   specify an <em>ordinal</em> of <b>2</b> (ordinals are zero-indexed). The
 *   default value is <b>-1</b> which supresses ordinal matching</dd>
 *   <dt><i>named argument:</i> fuzz</dt>
 *   <dd>By default, the injection point inspects only the instruction
 *   immediately following the matched invocation and will match store
 *   instructions. Specifying a higher <tt>fuzz</tt> increases the search range,
 *   skipping instructions as necessary to find a matching store opcode.</dd>
 *   <dt><i>named argument:</i> skip</dt>
 *   <dd>When <tt>fuzz</tt> is specified, a default list of skippable opcodes is
 *   used. The list of skippable opcodes can be overridden by specifying a list
 *   of opcodes (numeric values or constant names from {@link Opcodes} can be
 *   used). This can be used to restrict the <tt>fuzz</tt> behaviour to consume
 *   only expected opcodes (eg. <tt>CHECKCAST</tt>). Note that store opcodes
 *   cannot be skipped and specifying them has no effect.</dd>
 * </dl>
 * 
 * <p>Example:</p>
 * <blockquote><pre>
 *   &#064;At(value = "INVOKE_ASSIGN", target="func_1234_a(III)J")</pre>
 * </blockquote> 
 * 
 * <p>Note that unlike other standard injection points, this class matches the
 * insn after the invocation, and after any local variable assignment. Use the
 * {@link org.spongepowered.asm.mixin.injection.At#shift shift} specifier to
 * adjust the matched opcode as necessary.</p>
 */
@AtCode("INVOKE_ASSIGN")
public class AfterInvoke extends BeforeInvoke {
    
    /**
     * Default opcodes which are eligible to be skipped (see <i>named argument
     * </i> <tt>skip</tt> above) if <i>named argument </i> <tt>fuzz</tt> is
     * increased beyond the default value of <tt>1</tt>.
     * 
     * <p>Skipped opcodes: <tt>DUP</tt>, <tt>IADD</tt>, <tt>LADD</tt>,
     * <tt>FADD</tt>, <tt>DADD</tt>, <tt>ISUB</tt>, <tt>LSUB</tt>,
     * <tt>FSUB</tt>, <tt>DSUB</tt>, <tt>IMUL</tt>, <tt>LMUL</tt>,
     * <tt>FMUL</tt>, <tt>DMUL</tt>, <tt>IDIV</tt>, <tt>LDIV</tt>,
     * <tt>FDIV</tt>, <tt>DDIV</tt>, <tt>IREM</tt>, <tt>LREM</tt>,
     * <tt>FREM</tt>, <tt>DREM</tt>, <tt>INEG</tt>, <tt>LNEG</tt>,
     * <tt>FNEG</tt>, <tt>DNEG</tt>, <tt>ISHL</tt>, <tt>LSHL</tt>,
     * <tt>ISHR</tt>, <tt>LSHR</tt>, <tt>IUSHR</tt>, <tt>LUSHR</tt>,
     * <tt>IAND</tt>, <tt>LAND</tt>, <tt>IOR</tt>, <tt>LOR</tt>, <tt>IXOR</tt>,
     * <tt>LXOR</tt>, <tt>IINC</tt>, <tt>I2L</tt>, <tt>I2F</tt>, <tt>I2D</tt>,
     * <tt>L2I</tt>, <tt>L2F</tt>, <tt>L2D</tt>, <tt>F2I</tt>, <tt>F2L</tt>,
     * <tt>F2D</tt>, <tt>D2I</tt>, <tt>D2L</tt>, <tt>D2F</tt>, <tt>I2B</tt>,
     * <tt>I2C</tt>, <tt>I2S</tt>, <tt>CHECKCAST</tt>, <tt>INSTANCEOF</tt></p>
     */
    public static final int[] DEFAULT_SKIP = new int[] {
        // Opcodes which may appear if the targetted method is part of an
        // expression eg. int foo = 2 + this.bar();
        Opcodes.DUP, Opcodes.IADD, Opcodes.LADD, Opcodes.FADD, Opcodes.DADD,
        Opcodes.ISUB, Opcodes.LSUB, Opcodes.FSUB, Opcodes.DSUB, Opcodes.IMUL, 
        Opcodes.LMUL, Opcodes.FMUL, Opcodes.DMUL, Opcodes.IDIV, Opcodes.LDIV, 
        Opcodes.FDIV, Opcodes.DDIV, Opcodes.IREM, Opcodes.LREM, Opcodes.FREM, 
        Opcodes.DREM, Opcodes.INEG, Opcodes.LNEG, Opcodes.FNEG, Opcodes.DNEG, 
        Opcodes.ISHL, Opcodes.LSHL, Opcodes.ISHR, Opcodes.LSHR, Opcodes.IUSHR, 
        Opcodes.LUSHR, Opcodes.IAND, Opcodes.LAND, Opcodes.IOR, Opcodes.LOR, 
        Opcodes.IXOR, Opcodes.LXOR, Opcodes.IINC,
        
        // Opcodes which may appear if the targetted method is cast before
        // assignment eg. int foo = (int)this.getFloat();
        Opcodes.I2L, Opcodes.I2F, Opcodes.I2D, Opcodes.L2I, Opcodes.L2F,
        Opcodes.L2D, Opcodes.F2I, Opcodes.F2L, Opcodes.F2D, Opcodes.D2I,
        Opcodes.D2L, Opcodes.D2F, Opcodes.I2B, Opcodes.I2C, Opcodes.I2S,
        Opcodes.CHECKCAST, Opcodes.INSTANCEOF
    };
    
    /**
     * Lookahead fuzz factor for finding the corresponding assignment, by
     * default only the opcode immediately following the invocation is inspected
     */
    private int fuzz = 1;
    
    /**
     * If fuzz is increased beyond the default, the author can specify an allow-
     * list of opcodes which can be skipped, by default the lookahead is
     * unconstrained.  
     */
    private int[] skip = null;
    
    public AfterInvoke(InjectionPointData data) {
        super(data);
        this.fuzz = Math.max(data.get("fuzz", this.fuzz), 1);
        this.skip = data.getOpcodeList("skip", AfterInvoke.DEFAULT_SKIP);
    }

    @Override
    protected boolean addInsn(InsnList insns, Collection<AbstractInsnNode> nodes, AbstractInsnNode insn) {
        MethodInsnNode methodNode = (MethodInsnNode)insn;
        if (Type.getReturnType(methodNode.desc) == Type.VOID_TYPE) {
            return false;
        }

        if (this.fuzz > 0) {
            int offset = insns.indexOf(insn);
            int maxOffset = Math.min(insns.size(), offset + this.fuzz + 1);
            for (int index = offset + 1; index < maxOffset; index++) {
                AbstractInsnNode candidate = insns.get(index);
                if (candidate instanceof VarInsnNode && insn.getOpcode() >= Opcodes.ISTORE) {
                    insn = candidate;
                    break;
                } else if (this.skip != null && this.skip.length > 0 && Arrays.binarySearch(this.skip, candidate.getOpcode()) < 0) {
                    break;
                }
            }
        }
        
        insn = InjectionPoint.nextNode(insns, insn);
        nodes.add(insn);
        return true;
    }

}
