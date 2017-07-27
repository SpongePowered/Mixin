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
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.InsnNode;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.lib.tree.VarInsnNode;
import org.spongepowered.asm.util.Bytecode;

/**
 * Generator for proxy methods
 */
public class AccessorGeneratorMethodProxy extends AccessorGenerator {
    
    /**
     * The target field, identified by the accessor info
     */
    private final MethodNode targetMethod;
    
    /**
     * Accessor method argument types (raw, from method)
     */
    private final Type[] argTypes;
    
    /**
     * Accessor method return type (raw, from method)
     */
    private final Type returnType;
    
    /**
     * True for instance method, false for static method 
     */
    private final boolean isInstanceMethod;

    public AccessorGeneratorMethodProxy(AccessorInfo info) {
        super(info);
        this.targetMethod = info.getTargetMethod();
        this.argTypes = info.getArgTypes();
        this.returnType = info.getReturnType();
        this.isInstanceMethod = !Bytecode.hasFlag(this.targetMethod, Opcodes.ACC_STATIC);
    }

    @Override
    public MethodNode generate() {
        int size = Bytecode.getArgsSize(this.argTypes) + this.returnType.getSize() + (this.isInstanceMethod ? 1 : 0);
        MethodNode method = this.createMethod(size, size);
        if (this.isInstanceMethod) {
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        Bytecode.loadArgs(this.argTypes, method.instructions, this.isInstanceMethod ? 1 : 0);
        boolean isPrivate = Bytecode.hasFlag(this.targetMethod, Opcodes.ACC_PRIVATE);
        int opcode = this.isInstanceMethod ? (isPrivate ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL) : Opcodes.INVOKESTATIC;
        method.instructions.add(new MethodInsnNode(opcode, this.info.getClassNode().name, this.targetMethod.name, this.targetMethod.desc, false));
        method.instructions.add(new InsnNode(this.returnType.getOpcode(Opcodes.IRETURN)));
        return method;
    }

}
