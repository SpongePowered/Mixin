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

import java.util.Arrays;
import java.util.List;

import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InvalidInjectionException;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;

/**
 * A bytecode injector which allows a single argument of a chosen method call to
 * be altered.
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
                throw new InvalidInjectionException(this.info, "@ModifyArg return type must match the parameter type."
                        + " ARG=" + this.methodArgs[0] + " RETURN=" + this.returnType);
            }
        }
    }
    
    /**
     * Do the injection
     */
    @Override
    protected void inject(Target target, MethodInsnNode node) {
        Type[] args = Type.getArgumentTypes(node.desc);
        int argIndex = this.findArgIndex(target, args);
        InsnList insns = new InsnList();
        int extraLocals = 0;
        
        if (this.singleArgMode) {
            extraLocals = this.injectSingleArgHandler(target, args, argIndex, insns);
        } else {
            extraLocals = this.injectMultiArgHandler(target, args, argIndex, insns);
        }
        
        target.insns.insertBefore(node, insns);
        target.addToLocals(extraLocals);
        target.addToStack(2 - (extraLocals - 1));
    }

    /**
     * Inject handler opcodes for a single arg handler
     */
    private int injectSingleArgHandler(Target target, Type[] args, int argIndex, InsnList insns) {
        int[] argMap = this.storeArgs(target, args, insns, argIndex);
        this.invokeHandlerWithArgs(args, insns, argMap, argIndex, argIndex + 1);
        this.pushArgs(args, insns, argMap, argIndex + 1, args.length);
        return (target.getCurrentMaxLocals() - argMap[argMap.length - 1]) + 1 + args[args.length - 1].getSize();
    }

    /**
     * Inject handler opcodes for a multi arg handler
     */
    private int injectMultiArgHandler(Target target, Type[] args, int argIndex, InsnList insns) {
        if (!Arrays.equals(args, this.methodArgs)) {
            throw new InvalidInjectionException(this.info, "@ModifyArg method targets a method with an invalid signature "
                    + Injector.printArgs(args) + ", expected " + Injector.printArgs(this.methodArgs));
        }

        int[] argMap = this.storeArgs(target, args, insns, 0);
        this.pushArgs(args, insns, argMap, 0, argIndex);
        this.invokeHandlerWithArgs(args, insns, argMap, 0, args.length);
        this.pushArgs(args, insns, argMap, argIndex + 1, args.length);
        return (target.getCurrentMaxLocals() - argMap[argMap.length - 1]) + 1 + args[args.length - 1].getSize();
    }

    protected int findArgIndex(Target target, Type[] args) {
        if (this.index > -1) {
            if (this.index >= args.length || !args[this.index].equals(this.returnType)) {
                throw new InvalidInjectionException(this.info, "Specified index " + this.index + " for @ModifyArg is invalid for args "
                        + Injector.printArgs(args) + ", expected " + this.returnType);
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
                        + this.returnType + " for @ModifyArg target " + target + " in " + this.classNode.name
                        + ". Please specify index of desired arg.");
            }
            
            argIndex = arg;
        }
        
        if (argIndex == -1) {
            throw new InvalidInjectionException(this.info, "Could not find arg matching type " + this.returnType + " for @ModifyArg target "
                    + target + " in " + this.classNode.name);
        }

        return argIndex;
    }
}
