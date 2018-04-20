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
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.InsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InjectionPoint.AtCode;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;

/**
 * <p>This injection point searches for RETURN opcodes in the target method and
 * returns a list of insns immediately prior to matching instructions. Note that
 * <em>every</em> RETURN opcode will be returned and thus every natural exit
 * from the method except for exception throws will be implicitly specified. To
 * specify a particular RETURN use the <em>ordinal</em> parameter. The injection
 * point accepts the following parameters from
 * {@link org.spongepowered.asm.mixin.injection.At At}:</p>
 * 
 * <dl>
 *   <dt>ordinal</dt>
 *   <dd>The ordinal position of the RETURN opcode to match, if not specified
 *   then the injection point returns <em>all</em> RETURN opcodes. For example
 *   if the RETURN opcode appears 3 times in the method and you want to match
 *   the 3rd then you can specify an <em>ordinal</em> of <b>2</b> (ordinals are
 *   zero-indexed). The default value is <b>-1</b> which supresses ordinal
 *   matching</dd>
 * </dl>
 * 
 * <p>Example:</p>
 * <blockquote><pre>
 *   &#064;At(value = "RETURN")</pre>
 * </blockquote>
 * <p>Note that if <em>value</em> is the only parameter specified, it can be
 * omitted:</p> 
 * <blockquote><pre>
 *   &#064;At("RETURN")</pre>
 * </blockquote>
 */
@AtCode("RETURN")
public class BeforeReturn extends InjectionPoint {

    /**
     * Ordinal of the target insn
     */
    private final int ordinal;

    public BeforeReturn(InjectionPointData data) {
        super(data);

        this.ordinal = data.getOrdinal();
    }
    
    @Override
    public boolean checkPriority(int targetPriority, int ownerPriority) {
        return true;
    }

    @Override
    public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
        boolean found = false;
        
        // RETURN opcode varies based on return type, thus we calculate what opcode we're actually looking for by inspecting the target method
        int returnOpcode = Type.getReturnType(desc).getOpcode(Opcodes.IRETURN);
        int ordinal = 0;

        ListIterator<AbstractInsnNode> iter = insns.iterator();
        while (iter.hasNext()) {
            AbstractInsnNode insn = iter.next();

            if (insn instanceof InsnNode && insn.getOpcode() == returnOpcode) {
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
