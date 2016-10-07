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

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.FieldInsnNode;
import org.spongepowered.asm.lib.tree.InsnNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.lib.tree.VarInsnNode;

/**
 * Generator for field setters
 */
public class AccessorGeneratorFieldSetter extends AccessorGeneratorField {
    
    public AccessorGeneratorFieldSetter(AccessorInfo info) {
        super(info);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.gen.AccessorGenerator#generate()
     */
    @Override
    public MethodNode generate() {
        int stackSpace = this.isInstanceField ? 1 : 0;
        int maxLocals = stackSpace + this.targetType.getSize();
        int maxStack = stackSpace + this.targetType.getSize();
        MethodNode method = this.createMethod(maxLocals, maxStack);
        if (this.isInstanceField) {
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        method.instructions.add(new VarInsnNode(this.targetType.getOpcode(Opcodes.ILOAD), stackSpace));
        int opcode = this.isInstanceField ? Opcodes.PUTFIELD : Opcodes.PUTSTATIC;
        method.instructions.add(new FieldInsnNode(opcode, this.info.getClassNode().name, this.targetField.name, this.targetField.desc));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        return method;
    }

}
