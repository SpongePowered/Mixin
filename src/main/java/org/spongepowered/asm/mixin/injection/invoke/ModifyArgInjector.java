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

import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InjectionPoint.RestrictTargetLevel;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.struct.ArgOffsets;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.struct.Target.Extension;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.Bytecode;

/**
 * A bytecode injector which allows a single argument of a chosen method call to
 * be altered. For details see javadoc for {@link ModifyArg &#64;ModifyArg}.
 */
public class ModifyArgInjector extends InvokeInjector {

    /**
     * Index of the target arg or -1 to find the arg automatically (only works
     * where there is only one arg of specified type on the element)
     */
    private final int index;
    
    /**
     * True if processing in single arg mode, the callback only accepts a single
     * arg of the same type as the return. Otherwise the handler method is
     * expected to have matching args to the target invocation
     */
    private final boolean singleArgMode;

    /**
     * @param info Injection info
     * @param index target arg index
     */
    public ModifyArgInjector(InjectionInfo info, int index) {
        super(info, "@ModifyArg");
        this.index = index;
        this.singleArgMode = this.methodArgs.length == 1;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.callback.BytecodeInjector
     *      #sanityCheck(org.spongepowered.asm.mixin.injection.callback.Target,
     *      java.util.List)
     */
    @Override
    protected void sanityCheck(Target target, List<InjectionPoint> injectionPoints) {
        super.sanityCheck(target, injectionPoints);
        
        if (this.singleArgMode) {
            if (!this.methodArgs[0].equals(this.returnType)) {
                throw new InvalidInjectionException(this.info, "@ModifyArg return type on " + this + " must match the parameter type."
                        + " ARG=" + this.methodArgs[0] + " RETURN=" + this.returnType);
            }
        }
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
    
    @Override
    protected void inject(Target target, InjectionNode node) {
        this.checkTargetForNode(target, node, RestrictTargetLevel.ALLOW_ALL);
        super.inject(target, node);
    }
    
    /**
     * Do the injection
     */
    @Override
    protected void injectAtInvoke(Target target, InjectionNode node) {
        MethodInsnNode methodNode = (MethodInsnNode)node.getCurrentTarget();
        Type[] args = Type.getArgumentTypes(methodNode.desc);
        ArgOffsets offsets = node.<ArgOffsets>getDecoration(ArgOffsets.KEY, ArgOffsets.DEFAULT);
        boolean nested = node.hasDecoration(ArgOffsets.KEY);
        Type[] originalArgs = offsets.apply(args);
        
        if (originalArgs.length == 0) {
            throw new InvalidInjectionException(this.info, "@ModifyArg injector " + this + " targets a method invocation "
                    + ((MethodInsnNode)node.getOriginalTarget()).name + "()" + Type.getReturnType(methodNode.desc) + " with no arguments!");
        }

        int argIndex = offsets.getArgIndex(this.findArgIndex(target, originalArgs));
        int baseIndex = offsets.getStartIndex();
        
        InsnList insns = new InsnList();
        Extension extraLocals = target.extendLocals();
        
        if (this.singleArgMode) {
            this.injectSingleArgHandler(target, extraLocals, args, argIndex, insns, nested);
        } else {
            if (!Arrays.equals(originalArgs, this.methodArgs)) {
                throw new InvalidInjectionException(this.info, "@ModifyArg injector " + this + " targets a method with an invalid signature "
                        + Bytecode.getDescriptor(originalArgs) + ", expected " + Bytecode.getDescriptor(this.methodArgs));
            }

            this.injectMultiArgHandler(target, extraLocals, args, baseIndex, argIndex, insns, nested);
        }
        
        target.insns.insertBefore(methodNode, insns);
        target.extendStack().set(2 - (extraLocals.get() - 1)).apply();
        extraLocals.apply();
    }

    /**
     * Inject handler opcodes for a single arg handler
     */
    private void injectSingleArgHandler(Target target, Extension extraLocals, Type[] args, int argIndex, InsnList insns, boolean nested) {
        int[] argMap = target.generateArgMap(args, argIndex, nested);
        this.storeArgs(target, args, insns, argMap, argIndex, args.length, null, null);
        this.invokeHandlerWithArgs(args, insns, argMap, argIndex, argIndex + 1);
        this.pushArgs(args, insns, argMap, argIndex + 1, args.length);
        extraLocals.add((argMap[argMap.length - 1] - target.getMaxLocals()) + args[args.length - 1].getSize());
    }

    /**
     * Inject handler opcodes for a multi arg handler
     */
    private void injectMultiArgHandler(Target target, Extension extraLocals, Type[] args, int baseIndex, int argIndex, InsnList insns,
            boolean nested) {
        int[] argMap = target.generateArgMap(args, baseIndex, nested);
        int[] handlerArgMap = baseIndex == 0 ? argMap : Arrays.copyOfRange(argMap, baseIndex, baseIndex + this.methodArgs.length);
        this.storeArgs(target, args, insns, argMap, baseIndex, args.length, null, null);
        this.pushArgs(args, insns, argMap, baseIndex, argIndex);
        this.invokeHandlerWithArgs(this.methodArgs, insns, handlerArgMap, 0, this.methodArgs.length);
        this.pushArgs(args, insns, argMap, argIndex + 1, args.length);
        extraLocals.add((argMap[argMap.length - 1] - target.getMaxLocals()) + args[args.length - 1].getSize());
    }

    protected int findArgIndex(Target target, Type[] args) {
        if (this.index > -1) {
            if (this.index >= args.length || !args[this.index].equals(this.returnType)) {
                throw new InvalidInjectionException(this.info, "Specified index " + this.index + " for @ModifyArg is invalid for args "
                        + Bytecode.getDescriptor(args) + ", expected " + this.returnType + " on " + this);
            }
            return this.index;
        }
        
        int argIndex = -1;
        
        for (int arg = 0; arg < args.length; arg++) {
            if (!args[arg].equals(this.returnType)) {
                continue;
            }
            
            if (argIndex != -1) {
                throw new InvalidInjectionException(this.info, "Found duplicate args with index [" + argIndex + ", " + arg + "] matching type "
                        + this.returnType + " for @ModifyArg target " + target + " in " + this + ". Please specify index of desired arg.");
            }
            
            argIndex = arg;
        }
        
        if (argIndex == -1) {
            throw new InvalidInjectionException(this.info, "Could not find arg matching type " + this.returnType + " for @ModifyArg target "
                    + target + " in " + this);
        }

        return argIndex;
    }
}
