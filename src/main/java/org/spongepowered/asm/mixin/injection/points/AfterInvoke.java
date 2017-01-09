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

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InjectionPoint.AtCode;
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
 *   {@link org.spongepowered.asm.mixin.injection.struct.MemberInfo MemberInfo}
 *   which identifies the target method</dd>
 *   <dt>ordinal</dt>
 *   <dd>The ordinal position of the method invocation to match. For example if
 *   the method is invoked 3 times and you want to match the 3rd then you can
 *   specify an <em>ordinal</em> of <b>2</b> (ordinals are zero-indexed). The
 *   default value is <b>-1</b> which supresses ordinal matching</dd>
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

    public AfterInvoke(InjectionPointData data) {
        super(data);
    }

    @Override
    protected boolean addInsn(InsnList insns, Collection<AbstractInsnNode> nodes, AbstractInsnNode insn) {
        MethodInsnNode methodNode = (MethodInsnNode)insn;
        if (Type.getReturnType(methodNode.desc) == Type.VOID_TYPE) {
            return false;
        }
        
        insn = InjectionPoint.nextNode(insns, insn);
        if (insn instanceof VarInsnNode && insn.getOpcode() >= Opcodes.ISTORE) {
            insn = InjectionPoint.nextNode(insns, insn);
        }
        
        nodes.add(insn);
        return true;
    }
}
