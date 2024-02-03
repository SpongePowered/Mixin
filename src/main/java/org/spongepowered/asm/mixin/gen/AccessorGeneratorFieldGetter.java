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
package org.spongepowered.asm.mixin.gen;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Generator for instance field getters
 */
public class AccessorGeneratorFieldGetter extends AccessorGeneratorField {
    
    public AccessorGeneratorFieldGetter(AccessorInfo info) {
        super(info);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.gen.AccessorGenerator#generate()
     */
    @Override
    public MethodNode generate() {
        MethodNode method = this.createMethod(this.targetType.getSize(), this.targetType.getSize());
        if (!this.targetIsStatic) {
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        int opcode = this.targetIsStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD;
        method.instructions.add(new FieldInsnNode(opcode, this.info.getTargetClassNode().name, this.targetField.name, this.targetField.desc));
        method.instructions.add(new InsnNode(this.targetType.getOpcode(Opcodes.IRETURN)));
        return method;
    }
    
}
