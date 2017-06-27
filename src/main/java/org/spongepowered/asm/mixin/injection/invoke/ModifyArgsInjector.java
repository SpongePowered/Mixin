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
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.InsnNode;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.invoke.arg.ArgsClassGenerator;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.Bytecode;

/**
 * A bytecode injector which allows a single argument of a chosen method call to
 * be altered.
 */
public class ModifyArgsInjector extends InvokeInjector {

    /**
     * @param info Injection info
     * @param index target arg index
     */
    public ModifyArgsInjector(InjectionInfo info) {
        super(info, "@ModifyArgs");
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.invoke.InvokeInjector
     *      #checkTarget(org.spongepowered.asm.mixin.injection.struct.Target)
     */
    @Override
    protected void checkTarget(Target target) {
        if (!this.isStatic && target.isStatic) {
            throw new InvalidInjectionException(this.info, "non-static callback method " + this + " targets a static method which is not supported");
        }
    }
    
    /**
     * Do the injection
     */
    @Override
    protected void injectAtInvoke(Target target, InjectionNode node) {
        MethodInsnNode targetMethod = (MethodInsnNode)node.getCurrentTarget();
        
        Type[] args = Type.getArgumentTypes(targetMethod.desc);
        if (args.length == 0) {
            throw new InvalidInjectionException(this.info, "@ModifyArgs injector " + this + " targets a method invocation "
                    + targetMethod.name + targetMethod.desc + " with no arguments!");
        }
        
        String argClass = ArgsClassGenerator.getInstance().getClassRef(targetMethod.desc);
        
        boolean withArgs = false;
        String shortDesc = String.format("(L%s;)V", ArgsClassGenerator.CREF_ARGS);
        if (!this.methodNode.desc.equals(shortDesc)) {
            String targetDesc = Bytecode.changeDescriptorReturnType(target.method.desc, "V");
            String longDesc = String.format("(L%s;%s", ArgsClassGenerator.CREF_ARGS, targetDesc.substring(1));
            if (this.methodNode.desc.equals(longDesc)) {
                withArgs = true;
            } else {
                throw new InvalidInjectionException(this.info, "@ModifyArgs injector " + this + " has an invalid signature "
                        + this.methodNode.desc + ", expected " + shortDesc + " or " + longDesc);
            }
        }

        InsnList insns = new InsnList();
        target.addToStack(1);
        
        String factoryDesc = Bytecode.changeDescriptorReturnType(targetMethod.desc, "L" + argClass + ";");
        insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, argClass, "of", factoryDesc, false));
        insns.add(new InsnNode(Opcodes.DUP));
        
        if (!this.isStatic) {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            insns.add(new InsnNode(Opcodes.SWAP));
        }
        
        if (withArgs) {
            target.addToStack(Bytecode.getArgsSize(target.arguments));
            Bytecode.loadArgs(target.arguments, insns, target.isStatic ? 0 : 1);
        }
        
        this.invokeHandler(insns);
        
        for (int i = 0; i < args.length; i++) {
            if (i < args.length - 1) {
                insns.add(new InsnNode(Opcodes.DUP));
            }
            insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, argClass, ArgsClassGenerator.GETTER + i, "()" + args[i].getDescriptor(), false));
            if (i < args.length - 1) {
                if (args[i].getSize() == 1) {
                    insns.add(new InsnNode(Opcodes.SWAP));
                } else {
                    insns.add(new InsnNode(Opcodes.DUP2_X1));
                    insns.add(new InsnNode(Opcodes.POP2));
                }
            }
        }
        
        target.insns.insertBefore(targetMethod, insns);
    }
}
