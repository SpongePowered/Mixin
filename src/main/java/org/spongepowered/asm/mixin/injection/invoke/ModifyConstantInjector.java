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
package org.spongepowered.asm.mixin.injection.invoke;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.ASMHelper;

/**
 * A bytecode injector which allows a specific
 */
public class ModifyConstantInjector extends RedirectInjector {

    /**
     * @param info Injection info
     */
    public ModifyConstantInjector(InjectionInfo info) {
        super(info, "@ModifyConstant");
    }
    
    @Override
    protected void inject(Target target, InjectionNode node) {
        if (!this.preInject(node)) {
            return;
        }
            
        if (node.isReplaced()) {
            throw new UnsupportedOperationException("Target failure for " + this.info);
        }
        
        if (ASMHelper.isConstant(node.getCurrentTarget())) {
            this.injectConstantModifier(target, node);
            return;
        }
        
        throw new InvalidInjectionException(this.info, this.annotationType + " annotation on is targetting an invalid insn in "
                + target + " in " + this);
    }
    
    private void injectConstantModifier(Target target, InjectionNode node) {
        final AbstractInsnNode constNode = node.getCurrentTarget();
        final Type constantType = ASMHelper.getConstantType(constNode);
        final InsnList before = new InsnList();
        final InsnList after = new InsnList();
        AbstractInsnNode invoke = this.invokeConstantHandler(constNode, constantType, target, before, after);
        target.wrapNode(constNode, invoke, before, after);
    }

    private AbstractInsnNode invokeConstantHandler(AbstractInsnNode targetNode, Type constantType, Target target, InsnList before, InsnList after) {
        final String handlerDesc = ASMHelper.generateDescriptor(constantType, constantType);
        final boolean withArgs = this.checkDescriptor(handlerDesc, target, "getter");

        if (!this.isStatic) {
            before.add(new VarInsnNode(Opcodes.ALOAD, 0));
            target.addToStack(1);
        }
        
        if (withArgs) {
            this.pushArgs(target.arguments, after, target.argIndices, 0, target.arguments.length);
            target.addToStack(ASMHelper.getArgsSize(target.arguments));
        }
        
        return this.invokeHandler(after);
    }
}
