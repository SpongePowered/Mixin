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
package org.spongepowered.asm.mixin.injection.invoke;

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InvalidInjectionException;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.ASMHelper;


public abstract class InvokeInjector extends Injector {
    
    /**
     * Arguments of the handler method 
     */
    protected final Type[] methodArgs;
    
    /**
     * Return type of the handler method 
     */
    protected final Type returnType;
    
    protected final String annotationType;

    public InvokeInjector(InjectionInfo info, String annotationType) {
        super(info);
        this.methodArgs = Type.getArgumentTypes(this.methodNode.desc);
        this.returnType = Type.getReturnType(this.methodNode.desc);
        this.annotationType = annotationType;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.callback.BytecodeInjector#sanityCheck(org.spongepowered.asm.mixin.injection.callback.Target,
     *      java.util.List)
     */
    @Override
    protected void sanityCheck(Target target, List<InjectionPoint> injectionPoints) {
        if (ASMHelper.methodIsStatic(target.method) != this.isStatic) {
            throw new InvalidInjectionException("'static' modifier of callback method does not match target in " + this.methodNode.name);
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.callback.BytecodeInjector#inject(org.spongepowered.asm.mixin.injection.callback.Target,
     *      org.objectweb.asm.tree.AbstractInsnNode)
     */
    @Override
    protected void inject(Target target, AbstractInsnNode node) {
        if (!(node instanceof MethodInsnNode)) {
            throw new InvalidInjectionException(this.annotationType + " annotation is targetting a non-method insn in " + target
                    + " in " + this.classNode.name);
        }
        
        this.inject(target, (MethodInsnNode)node);
    }
    
    /**
     * Do the injection
     */
    protected abstract void inject(Target target, MethodInsnNode node);

    /**
     * @param args
     * @param insns
     * @param argMap
     * @param startArg
     * @param endArg
     */
    protected void invokeHandlerWithArgs(Type[] args, InsnList insns, int[] argMap, int startArg, int endArg) {
        if (!this.isStatic) {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        this.pushArgs(insns, args, argMap, startArg, endArg);
        insns.add(new MethodInsnNode(this.isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKESPECIAL,
                this.classNode.name, this.methodNode.name, this.methodNode.desc, false));
    }

    /**
     * Generate an array containing local indexes for the specified args, returns an array of identical size to the supplied array with an
     * allocated local index in each corresponding position
     */
    protected int[] generateArgMap(Type[] args, int start, int local) {
        int[] argMap = new int[args.length];
        for (int arg = start; arg < args.length; arg++) {
            argMap[arg] = local;
            local += args[arg].getSize();
        }
        return argMap;
    }

    /**
     * Store args on the stack starting at the end and working back to position specified by start, return the generated argMap
     */
    protected int[] storeArgs(Target target, Type[] args, InsnList insns, int start) {
        int[] argMap = this.generateArgMap(args, start, target.method.maxLocals);
        this.storeArgs(args, insns, argMap, start, args.length);
        return argMap;
    }

    /**
     * Store args on the stack to their positions allocated based on argMap 
     */
    protected void storeArgs(Type[] args, InsnList insns, int[] argMap, int start, int end) {
        for (int arg = end - 1; arg >= start; arg--) {
            insns.add(new VarInsnNode(args[arg].getOpcode(Opcodes.ISTORE), argMap[arg]));
        }
    }

    /**
     * Load args onto the stack from their positions allocated in argMap 
     */
    protected void pushArgs(InsnList insns, Type[] args, int[] argMap, int start, int end) {
        for (int arg = start; arg < end; arg++) {
            insns.add(new VarInsnNode(args[arg].getOpcode(Opcodes.ILOAD), argMap[arg]));
        }
    }
}
