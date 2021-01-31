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

import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Method;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Bytecode;

/**
 * Generator for field setters
 */
public class AccessorGeneratorFieldSetter extends AccessorGeneratorField {
    
    /**
     * True if the accessor method is decorated with {@link Mutable} 
     */
    private boolean mutable;
    
    public AccessorGeneratorFieldSetter(AccessorInfo info) {
        super(info);
    }
    
    @Override
    public void validate() {
        super.validate();
        
        Method method = this.info.getClassInfo().findMethod(this.info.getMethod());
        this.mutable = method.isDecoratedMutable();
        if (this.mutable || !Bytecode.hasFlag(this.targetField, Opcodes.ACC_FINAL)) {
            return;
        }
        
        if (this.info.getMixin().getOption(Option.DEBUG_VERBOSE)) {
            LogManager.getLogger("mixin").warn("{} for final field {}::{} is not @Mutable", this.info,
                    ((MixinTargetContext)this.info.getMixin()).getTarget(), this.targetField.name);
        }                    
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.gen.AccessorGenerator#generate()
     */
    @Override
    public MethodNode generate() {
        if (this.mutable) {
            this.targetField.access &= ~Opcodes.ACC_FINAL;
        }
        
        int stackSpace = this.targetIsStatic ? 0 : 1; // Stack space for "this"
        int maxLocals = stackSpace + this.targetType.getSize();
        int maxStack = stackSpace + this.targetType.getSize();
        MethodNode method = this.createMethod(maxLocals, maxStack);
        if (!this.targetIsStatic) {
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        method.instructions.add(new VarInsnNode(this.targetType.getOpcode(Opcodes.ILOAD), stackSpace));
        int opcode = this.targetIsStatic ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD;
        method.instructions.add(new FieldInsnNode(opcode, this.info.getClassNode().name, this.targetField.name, this.targetField.desc));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        return method;
    }

}
