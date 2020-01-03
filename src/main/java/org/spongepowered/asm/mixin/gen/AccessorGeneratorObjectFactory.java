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
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Constants;

/**
 * Generator for factory methods (constructor invokers)
 */
public class AccessorGeneratorObjectFactory extends AccessorGeneratorMethodProxy {

    public AccessorGeneratorObjectFactory(AccessorInfo info) {
        super(info, true);
        if (!info.isStatic()) {
            throw new InvalidInjectionException(info.getContext(), String.format("%s is invalid. Factory method must be static.", this.info));
        }
    }

    @Override
    public MethodNode generate() {
        int returnSize = this.returnType.getSize();
        // size includes return size x2 since we immediately DUP the value to invoke the ctor on it
        int size = Bytecode.getArgsSize(this.argTypes) + (returnSize * 2);
        MethodNode method = this.createMethod(size, size);
        
        String className = this.info.getClassNode().name;
        method.instructions.add(new TypeInsnNode(Opcodes.NEW, className));
        method.instructions.add(new InsnNode(returnSize == 1 ? Opcodes.DUP : Opcodes.DUP2));
        Bytecode.loadArgs(this.argTypes, method.instructions, 0);
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, className, Constants.CTOR, this.targetMethod.desc, false));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));

        return method;
    }

}
