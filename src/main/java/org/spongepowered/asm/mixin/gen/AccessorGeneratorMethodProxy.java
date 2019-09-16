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
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.util.Bytecode;

/**
 * Generator for proxy methods
 */
public class AccessorGeneratorMethodProxy extends AccessorGenerator {
    
    /**
     * The target field, identified by the accessor info
     */
    protected final MethodNode targetMethod;
    
    /**
     * Accessor method argument types (raw, from method)
     */
    protected final Type[] argTypes;
    
    /**
     * Accessor method return type (raw, from method)
     */
    protected final Type returnType;

    public AccessorGeneratorMethodProxy(AccessorInfo info) {
        super(info, Bytecode.isStatic(info.getTargetMethod()));
        this.targetMethod = info.getTargetMethod();
        this.argTypes = info.getArgTypes();
        this.returnType = info.getReturnType();
        this.checkModifiers();
    }
    
    protected AccessorGeneratorMethodProxy(AccessorInfo info, boolean isStatic) {
        super(info, isStatic);
        this.targetMethod = info.getTargetMethod();
        this.argTypes = info.getArgTypes();
        this.returnType = info.getReturnType();
    }

    @Override
    public MethodNode generate() {
        int size = Bytecode.getArgsSize(this.argTypes) + this.returnType.getSize() + (this.targetIsStatic ? 0 : 1);
        MethodNode method = this.createMethod(size, size);
        if (!this.targetIsStatic) {
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        Bytecode.loadArgs(this.argTypes, method.instructions, this.targetIsStatic ? 0 : 1);
        boolean isPrivate = Bytecode.hasFlag(this.targetMethod, Opcodes.ACC_PRIVATE);
        int opcode = this.targetIsStatic ? Opcodes.INVOKESTATIC : (isPrivate ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL);
        method.instructions.add(new MethodInsnNode(opcode, this.info.getClassNode().name, this.targetMethod.name, this.targetMethod.desc, false));
        method.instructions.add(new InsnNode(this.returnType.getOpcode(Opcodes.IRETURN)));
        return method;
    }

}
