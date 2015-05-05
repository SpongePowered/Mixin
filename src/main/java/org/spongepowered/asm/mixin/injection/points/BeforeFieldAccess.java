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
package org.spongepowered.asm.mixin.injection.points;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.FieldInsnNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;

/**
 * <p>This injection point searches for GETFIELD and PUTFIELD (and static
 * equivalent) opcodes matching its arguments and returns a list of insns
 * immediately prior to matching instructions. It accepts the following
 * parameters from {@link org.spongepowered.asm.mixin.injection.At At}:
 * </p>
 * 
 * <dl>
 *   <dt>target</dt>
 *   <dd>A
 *   {@link org.spongepowered.asm.mixin.injection.struct.MemberInfo MemberInfo}
 *   which identifies the target field.</dd>
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
 * <p>Note that like all standard injection points, this class matches the insn
 * itself, putting the injection point immediately <em>before</em> the access in
 * question. Use {@link org.spongepowered.asm.mixin.injection.At#shift shift}
 * specifier to adjust the matched opcode as necessary.</p>
 */
public class BeforeFieldAccess extends BeforeInvoke {

    @SuppressWarnings("hiding")
    public static final String CODE = "FIELD";

    private final int opcode;

    public BeforeFieldAccess(InjectionPointData data) {
        super(data);
        this.opcode = data.getOpcode(-1, Opcodes.GETFIELD, Opcodes.PUTFIELD, Opcodes.GETSTATIC, Opcodes.PUTSTATIC, -1);
    }

    @Override
    protected boolean matchesInsn(AbstractInsnNode insn) {
        return insn instanceof FieldInsnNode && (((FieldInsnNode) insn).getOpcode() == this.opcode || this.opcode == -1);
    }
}
