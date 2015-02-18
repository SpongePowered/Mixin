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
package org.spongepowered.asm.mixin.injection.callback;

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InvalidInjectionException;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.points.BeforeReturn;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.ASMHelper;
import org.spongepowered.asm.util.Locals;

/**
 * This class is responsible for generating the bytecode for injected callbacks
 */
public class CallbackInjector extends Injector {
    
    /**
     * True if cancellable 
     */
    private final boolean cancellable;
    
    /**
     * True if capturing locals 
     */
    private final boolean captureLocals;

    /**
     * Make a new CallbackInjector with the supplied args
     * 
     * @param info information about this injector
     * @param cancellable True if injections performed by this injector should
     *      be cancellable
     * @param captureLocals True to capture local variables from the frame at
     *      the target location
     */
    public CallbackInjector(InjectionInfo info, boolean cancellable, boolean captureLocals) {
        super(info);
        this.cancellable = cancellable;
        this.captureLocals = captureLocals;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.callback.BytecodeInjector
     *      #sanityCheck(org.spongepowered.asm.mixin.injection.callback.Target,
     *      java.util.List)
     */
    @Override
    protected void sanityCheck(Target target, List<InjectionPoint> injectionPoints) {
        if (ASMHelper.methodIsStatic(target.method) != this.isStatic) {
            throw new InvalidInjectionException("'static' modifier of callback method does not match target in " + this.methodNode.name);
        }

        if (Injector.CTOR.equals(target.method.name)) {
            for (InjectionPoint injectionPoint : injectionPoints) {
                if (!injectionPoint.getClass().equals(BeforeReturn.class)) {
                    throw new InvalidInjectionException("Found injection point type " + injectionPoint.getClass().getSimpleName()
                            + " targetting a ctor in " + this.classNode.name + ". Only RETURN allowed for a ctor target");
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.callback.BytecodeInjector
     *      #inject(org.spongepowered.asm.mixin.injection.callback.Target,
     *      org.objectweb.asm.tree.AbstractInsnNode)
     */
    @Override
    protected void inject(Target target, AbstractInsnNode node) {
        Type[] localTypes = null;

        if (this.captureLocals) {
            LocalVariableNode[] locals = Locals.getLocalsAt(this.classNode, target.method, node);

            if (locals != null) {
                localTypes = new Type[locals.length];
                for (int l = 0; l < locals.length; l++) {
                    if (locals[l] != null) {
                        localTypes[l] = Type.getType(locals[l].desc);
                    }
                }
            }
        }

        this.inject(target, node, localTypes);
    }

    /**
     * Generate the actual bytecode for the callback
     * 
     * @param target Target method to inject callback into
     * @param node Target node within
     * @param locals Inferred local types at the target location
     */
    private void inject(Target target, final AbstractInsnNode node, final Type[] locals) {
        // Calculate the initial frame size based on the target method's arguments
        int initialFrameSize = ASMHelper.getFirstNonArgLocalIndex(target.arguments, !this.isStatic);

        // Work out whether we have enough info to capture locals in this scenario
        boolean doCaptureLocals = this.captureLocals && locals != null && locals.length > initialFrameSize;
        
        // Determine the descriptor for the callback based on the target method and current context
        String callbackDescriptor = target.getCallbackDescriptor(doCaptureLocals, locals, target.arguments, initialFrameSize);
        
        // If it doesn't match, then cry
        if (!callbackDescriptor.equals(this.methodNode.desc)) {
            throw new InvalidInjectionException("Invalid descriptor on callback: expected " + callbackDescriptor + " found " + this.methodNode.desc);
        }

        // These two variables keep track of the (additional) stack size required for the two actions we're going to be injecting insns to perform,
        // namely calling the CallbackInfo ctor, and then invoking the callback itself. When we get to the end of this injection we will then set
        // the MAXS value on the target method to its original value plus the larger of the two values.
        int ctorMAXS = 0;
        int invokeMAXS = target.arguments.length + (doCaptureLocals ? locals.length - initialFrameSize : 0);

        // Marshall var is the local where we marshall the utility references we need during invoke of the callback, those being the current return
        // value for value-return scenarios (we store the topmost stack entry and then push it into the ctor of the CallbackInfo) and also the
        // CallbackInfo reference itself (we use the same local var for these two purposes because they don't exist at the same time).
        int marshallVar = target.method.maxLocals++;

        // Insns we will inject
        InsnList insns = new InsnList();

        // Assume (for now) that we won't push the return value into the CallbackInfo ctor
        boolean pushReturnValue = false;

        // If this is a ReturnEventInfo AND we are right before a RETURN opcode (so we can expect the *original* return value to be on the stack,
        // then we dup the return value into a local var so we can push it later when we invoke the ReturnEventInfo ctor
        if (node instanceof InsnNode && node.getOpcode() >= Opcodes.IRETURN && node.getOpcode() < Opcodes.RETURN) {
            pushReturnValue = true;
            insns.add(new InsnNode(Opcodes.DUP));
            insns.add(new VarInsnNode(target.returnType.getOpcode(Opcodes.ISTORE), marshallVar));
        }

        // CHECKSTYLE:OFF
        // Instance the EventInfo for this event
        insns.add(new TypeInsnNode(Opcodes.NEW, target.callbackInfoClass)); ctorMAXS++;
        insns.add(new InsnNode(Opcodes.DUP)); ctorMAXS++; invokeMAXS++;
        // CHECKSTYLE:ON
        ctorMAXS += this.invokeCallbackInfoCtor(target, insns, pushReturnValue, marshallVar);
        insns.add(new VarInsnNode(Opcodes.ASTORE, marshallVar));

        // Push "this" onto the stack if the callback is not static
        if (!this.isStatic) {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }

        // Push the target method's parameters onto the stack
        ASMHelper.loadArgs(target.arguments, insns, this.isStatic ? 0 : 1);
        
        // Push the callback info onto the stack
        insns.add(new VarInsnNode(Opcodes.ALOAD, marshallVar));
        
        // (Maybe) push the locals onto the stack
        if (doCaptureLocals) {
            Locals.loadLocals(locals, insns, initialFrameSize);
        }
        
        // Call the callback!
        boolean isPrivate = (this.methodNode.access & Opcodes.ACC_PRIVATE) != 0;
        int invokeOpcode = this.isStatic ? Opcodes.INVOKESTATIC : isPrivate ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL;
        insns.add(new MethodInsnNode(invokeOpcode,
                this.classNode.name, this.methodNode.name, this.methodNode.desc, false));

        if (this.cancellable) {
            // Inject the if (e.isCancelled()) return e.getReturnValue();
            this.injectCancellationCode(target, insns, node, marshallVar);
        }

        // Inject our generated code into the method
        target.method.instructions.insertBefore(node, insns);
        target.method.maxStack = Math.max(target.method.maxStack, Math.max(target.maxStack + ctorMAXS, target.maxStack + invokeMAXS));
    }

    /**
     * @param target target method
     * @param insns instruction list to append to
     * @param pushReturnValue True to push the current value on the stack into
     *      the CallbackInfo ctor, used when injecting at a RETURN opcode in
     *      order to capture the current return value
     * @param marshallVar "working" variable used to marshallt temporary values
     * @return additional MAXS slots required
     */
    protected int invokeCallbackInfoCtor(Target target, InsnList insns, boolean pushReturnValue, int marshallVar) {
        int ctorMAXS = 0;

        // CHECKSTYLE:OFF
        insns.add(new LdcInsnNode(target.method.name)); ctorMAXS++;
        insns.add(new InsnNode(this.cancellable ? Opcodes.ICONST_1 : Opcodes.ICONST_0)); ctorMAXS++;
        // CHECKSTYLE:ON

        if (pushReturnValue) {
            insns.add(new VarInsnNode(target.returnType.getOpcode(Opcodes.ILOAD), marshallVar));
            insns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                    target.callbackInfoClass, Injector.CTOR, CallbackInfo.getConstructorDescriptor(target.returnType), false));
        } else {
            insns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                    target.callbackInfoClass, Injector.CTOR, CallbackInfo.getConstructorDescriptor(), false));
        }

        return ctorMAXS;
    }

    /**
     * if (e.isCancelled()) return e.getReturnValue();
     * 
     * @param target target method
     * @param insns instruction list to append to
     * @param node target node
     * @param marshallVar "working" variable used to marshallt temporary values
     */
    protected void injectCancellationCode(Target target, final InsnList insns, final AbstractInsnNode node, int marshallVar) {
        insns.add(new VarInsnNode(Opcodes.ALOAD, marshallVar));
        insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, target.callbackInfoClass, CallbackInfo.getIsCancelledMethodName(),
                CallbackInfo.getIsCancelledMethodSig(), false));

        LabelNode notCancelled = new LabelNode();
        insns.add(new JumpInsnNode(Opcodes.IFEQ, notCancelled));

        // If this is a void method, just injects a RETURN opcode, otherwise we need to get the return value from the EventInfo
        this.injectReturnCode(target, insns, node, marshallVar);

        insns.add(notCancelled);
    }

    /**
     * Inject the appropriate return code for the method type
     * 
     * @param target target method
     * @param insns instruction list to append to
     * @param node target node
     * @param marshallVar "working" variable used to marshallt temporary values
     */
    protected void injectReturnCode(Target target, final InsnList insns, final AbstractInsnNode node, int marshallVar) {
        if (target.returnType.equals(Type.VOID_TYPE)) {
            // Void method, so just return void
            insns.add(new InsnNode(Opcodes.RETURN));
        } else {
            // Non-void method, so work out which accessor to call to get the return value, and return it
            insns.add(new VarInsnNode(Opcodes.ALOAD, marshallVar));
            String accessor = CallbackInfoReturnable.getReturnAccessor(target.returnType);
            String descriptor = CallbackInfoReturnable.getReturnDescriptor(target.returnType);
            insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, target.callbackInfoClass, accessor, descriptor, false));
            if (target.returnType.getSort() == Type.OBJECT) {
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, target.returnType.getInternalName()));
            }
            insns.add(new InsnNode(target.returnType.getOpcode(Opcodes.IRETURN)));
        }
    }
}
