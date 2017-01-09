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
import java.util.ListIterator;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.JumpInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InjectionPoint.AtCode;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;

/**
 * <p>This injection point searches for JUMP opcodes (if, try/catch, continue,
 * break, conditional assignment, etc.) with either a particular opcode or at a
 * particular ordinal in the method body (eg. "the Nth JUMP insn" where N is the
 * ordinal of the instruction). By default it returns all JUMP instructions in a
 * method body. It accepts the following parameters from
 * {@link org.spongepowered.asm.mixin.injection.At At}:</p>
 * 
 * <dl>
 *   <dt>opcode</dt>
 *   <dd>The {@link Opcodes opcode} of the jump instruction, must be one of
 *   IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT,
 *   IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, GOTO, JSR, IFNULL or
 *   IFNONNULL. Defaults to <b>-1</b> which matches any JUMP opcode.
 *   </dd>
 *   <dt>ordinal</dt>
 *   <dd>The ordinal position of the jump insn to match. For example if there
 *   are 3 jumps of the specified type and you want to match the 2nd then you
 *   can specify an <em>ordinal</em> of <b>1</b> (ordinals are zero-indexed).
 *   The default value is <b>-1</b> which supresses ordinal matching</dd>
 * </dl>
 * 
 * <p>Example:</p>
 * <blockquote><pre>
 *   &#064;At(value = "JUMP", opcode = Opcodes.IFLE, ordinal = 2)</pre>
 * </blockquote>
 * 
 * <p>Note that like all standard injection points, this class matches the insn
 * itself, putting the injection point immediately <em>before</em> the access in
 * question. Use {@link org.spongepowered.asm.mixin.injection.At#shift shift}
 * specifier to adjust the matched opcode as necessary.</p>
 */
@AtCode("JUMP")
public class JumpInsnPoint extends InjectionPoint {

    private final int opCode;

    private final int ordinal;

    public JumpInsnPoint(InjectionPointData data) {
        this.opCode = data.getOpcode(-1, Opcodes.IFEQ, Opcodes.IFNE, Opcodes.IFLT, Opcodes.IFGE, Opcodes.IFGT, Opcodes.IFLE, Opcodes.IF_ICMPEQ,
                Opcodes.IF_ICMPNE, Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE, Opcodes.IF_ICMPGT, Opcodes.IF_ICMPLE, Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE,
                Opcodes.GOTO, Opcodes.JSR, Opcodes.IFNULL, Opcodes.IFNONNULL, -1);
        this.ordinal = data.getOrdinal();
    }

    @Override
    public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
        boolean found = false;
        int ordinal = 0;

        ListIterator<AbstractInsnNode> iter = insns.iterator();
        while (iter.hasNext()) {
            AbstractInsnNode insn = iter.next();

            if (insn instanceof JumpInsnNode && (this.opCode == -1 || insn.getOpcode() == this.opCode)) {
                if (this.ordinal == -1 || this.ordinal == ordinal) {
                    nodes.add(insn);
                    found = true;
                }

                ordinal++;
            }
        }

        return found;
    }
}
