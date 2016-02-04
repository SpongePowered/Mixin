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

import java.util.List;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InvalidInjectionException;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;

/**
 * Base class for injectors which inject at method invokes
 */
public abstract class InvokeInjector extends Injector {
    
    protected final String annotationType;

    /**
     * @param info Information about this injection
     * @param annotationType Annotation type, used for error messages
     */
    public InvokeInjector(InjectionInfo info, String annotationType) {
        super(info);
        this.annotationType = annotationType;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.callback.BytecodeInjector
     *      #sanityCheck(org.spongepowered.asm.mixin.injection.callback.Target,
     *      java.util.List)
     */
    @Override
    protected void sanityCheck(Target target, List<InjectionPoint> injectionPoints) {
        super.sanityCheck(target, injectionPoints);
        
        if (target.isStatic != this.isStatic) {
            throw new InvalidInjectionException(this.info, "'static' modifier of callback method does not match target in " + this);
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.callback.BytecodeInjector
     *      #inject(org.spongepowered.asm.mixin.injection.callback.Target,
     *      org.objectweb.asm.tree.AbstractInsnNode)
     */
    @Override
    protected void inject(Target target, InjectionNode node) {
        if (!(node.getCurrentTarget() instanceof MethodInsnNode)) {
            throw new InvalidInjectionException(this.info, this.annotationType + " annotation on is targetting a non-method insn in " + target
                    + " in " + this);
        }
        
        this.injectAtInvoke(target, node);
    }
    
    /**
     * Perform a single injection
     * 
     * @param target Target to inject into
     * @param node Discovered instruction node 
     */
    protected abstract void injectAtInvoke(Target target, InjectionNode node);

    /**
     * @param args handler arguments
     * @param insns InsnList to inject insns into
     * @param argMap Mapping of args to local variables
     */
    protected AbstractInsnNode invokeHandlerWithArgs(Type[] args, InsnList insns, int[] argMap) {
        return this.invokeHandlerWithArgs(args, insns, argMap, 0, args.length);
    }
    
    /**
     * @param args handler arguments
     * @param insns InsnList to inject insns into
     * @param argMap Mapping of args to local variables
     * @param startArg Starting arg to consume
     * @param endArg Ending arg to consume
     */
    protected AbstractInsnNode invokeHandlerWithArgs(Type[] args, InsnList insns, int[] argMap, int startArg, int endArg) {
        if (!this.isStatic) {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        this.pushArgs(args, insns, argMap, startArg, endArg);
        return this.invokeHandler(insns);
    }

    /**
     * Store args on the stack starting at the end and working back to position
     * specified by start, return the generated argMap
     * 
     * @param target target method
     * @param args argument types
     * @param insns instruction list to generate insns into
     * @param start Starting index
     * @return the generated argmap
     */
    protected int[] storeArgs(Target target, Type[] args, InsnList insns, int start) {
        int[] argMap = target.generateArgMap(args, start);
        this.storeArgs(args, insns, argMap, start, args.length);
        return argMap;
    }

    /**
     * Store args on the stack to their positions allocated based on argMap
     * 
     * @param args argument types
     * @param insns instruction list to generate insns into
     * @param argMap generated argmap containing local indices for all args
     * @param start Starting index
     * @param end Ending index
     */
    protected void storeArgs(Type[] args, InsnList insns, int[] argMap, int start, int end) {
        for (int arg = end - 1; arg >= start; arg--) {
            insns.add(new VarInsnNode(args[arg].getOpcode(Opcodes.ISTORE), argMap[arg]));
        }
    }

    /**
     * Load args onto the stack from their positions allocated in argMap
     * @param args argument types
     * @param insns instruction list to generate insns into
     * @param argMap generated argmap containing local indices for all args
     * @param start Starting index
     * @param end Ending index
     */
    protected void pushArgs(Type[] args, InsnList insns, int[] argMap, int start, int end) {
        for (int arg = start; arg < end; arg++) {
            insns.add(new VarInsnNode(args[arg].getOpcode(Opcodes.ILOAD), argMap[arg]));
        }
    }
}
