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

import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.LdcInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint.AtCode;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;

/**
 * <p>Like {@link BeforeInvoke}, this injection point searches for
 * INVOKEVIRTUAL, INVOKESTATIC and INVOKESPECIAL opcodes matching its arguments
 * and returns a list of insns immediately prior to matching instructions. This
 * specialised version however only matches methods which accept a single string
 * and return void, but allows the string itself to be matched as part of the
 * search process. This is primarily used for matching particular invocations of
 * <em>Profiler::startSection</em> with a specific argument. Note that because a
 * string literal is required, this injection point can not be used to match
 * invocations where the value being passed in is a variable.</p>
 * 
 * <p>To be precise, this injection point matches invocations of the specified
 * method which are preceded by an LDC instruction. The LDC instruction's
 * payload can be specified with the <b>ldc</b> named argument (see below)</p>
 * 
 * <p>The following parameters from
 * {@link org.spongepowered.asm.mixin.injection.At At} are accepted</p>
 * 
 * <dl>
 *   <dt>target</dt>
 *   <dd>A
 *   {@link org.spongepowered.asm.mixin.injection.struct.MemberInfo MemberInfo}
 *   which identifies the target method, the method <b>must</b> be specified
 *   with a signature which accepts a single string and returns void,
 *   eg. <code>(Ljava/lang/String;)V</code></dd>
 *   <dt>ordinal</dt>
 *   <dd>The ordinal position of the method invocation to match. For example if
 *   the method is invoked 3 times and you want to match the 3rd then you can
 *   specify an <em>ordinal</em> of <b>2</b> (ordinals are zero-indexed). The
 *   default value is <b>-1</b> which supresses ordinal matching</dd>
 *   <dt><em>named argument</em> ldc</dt>
 *   <dd>The value of the LDC node to look for prior to the method invocation
 *   </dd>
 * </dl>
 * 
 * <p>Example:</p>
 * <blockquote><pre>
 *   &#064;At(value = "INVOKE_STRING",
 *      target="startSection(Ljava/lang/String;)V", args = { "ldc=root" })</pre>
 * </blockquote>
 * <p>Notice the use of the <em>named argument</em> "ldc" which specifies the
 * value of the target LDC node.</p> 
 * 
 * <p>Note that like all standard injection points, this class matches the insn
 * itself, putting the injection point immediately <em>before</em> the access in
 * question. Use {@link org.spongepowered.asm.mixin.injection.At#shift shift}
 * specifier to adjust the matched opcode as necessary.</p>
 */
@AtCode("INVOKE_STRING")
public class BeforeStringInvoke extends BeforeInvoke {

    private static final String STRING_VOID_SIG = "(Ljava/lang/String;)V";

    /**
     * LDC value we're searching for
     */
    private final String ldcValue;

    /**
     * True once a matching LDC node is found
     */
    private boolean foundLdc;

    public BeforeStringInvoke(InjectionPointData data) {
        super(data);
        this.ldcValue = data.get("ldc", null);
        
        if (this.ldcValue == null) {
            throw new IllegalArgumentException(this.getClass().getSimpleName() + " requires named argument \"ldc\" to specify the desired target");
        }
        
        if (!STRING_VOID_SIG.equals(this.target.desc)) {
            throw new IllegalArgumentException(this.getClass().getSimpleName() + " requires target method with with signature " + STRING_VOID_SIG);
        }
    }

    @Override
    public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
        this.foundLdc = false;

        return super.find(desc, insns, nodes);
    }

    @Override
    protected void inspectInsn(String desc, InsnList insns, AbstractInsnNode insn) {
        if (insn instanceof LdcInsnNode) {
            LdcInsnNode node = (LdcInsnNode) insn;
            if (node.cst instanceof String && this.ldcValue.equals(node.cst)) {
                this.log("{} > found a matching LDC with value {}", this.className, node.cst);
                this.foundLdc = true;
                return;
            }
        }

        this.foundLdc = false;
    }

    @Override
    protected boolean matchesInsn(MemberInfo nodeInfo, int ordinal) {
        this.log("{} > > found LDC \"{}\" = {}", this.className, this.ldcValue, this.foundLdc);
        return this.foundLdc && super.matchesInsn(nodeInfo, ordinal);
    }
}
