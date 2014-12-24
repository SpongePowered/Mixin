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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InvalidInjectionException;
import org.spongepowered.asm.mixin.injection.points.BeforeReturn;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.util.ASMHelper;
import org.spongepowered.asm.util.Locals;

/**
 * This class is responsible for generating the bytecode for injected callbacks
 */
public class CallbackInjector {

    private static final String CTOR = "<init>";

    /**
     * Information about the current injection target, mainly just convenience rather than passing a bunch of values around
     */
    class Target {

        /**
         * Target method
         */
        final MethodNode method;
        
        /**
         * Method arguments
         */
        final Type[] arguments;
        
        /**
         * Return type computed from the method descriptor 
         */
        final Type returnType;
        
        /**
         * Method's (original) MAXS 
         */
        // CHECKSTYLE:OFF
        final int MAXS;
        // CHECKSTYLE:ON

        /**
         * Callback method descriptor based on this target 
         */
        final String callbackDescriptor;
        
        /**
         * Callback info class
         */
        final String callbackInfoClass;

        /**
         * Make a new Target for the supplied method
         */
        Target(MethodNode method) {
            this.method = method;
            this.arguments = Type.getArgumentTypes(method.desc);

            this.returnType = Type.getReturnType(method.desc);
            this.MAXS = method.maxStack;
            this.callbackInfoClass = CallbackInfo.getCallInfoClassName(this.returnType);
            this.callbackDescriptor = String.format("(%sL%s;)V", method.desc.substring(1, method.desc.indexOf(')')), this.callbackInfoClass);
        }

        /**
         * Get the callback descriptor
         */
        String getCallbackDescriptor(final boolean captureLocals, final Type[] locals, Type[] argumentTypes, int startIndex) {
            if (!captureLocals) {
                return this.callbackDescriptor;
            }

            String descriptor = this.callbackDescriptor.substring(0, this.callbackDescriptor.indexOf(')'));
            for (int l = startIndex; l < locals.length; l++) {
                if (locals[l] != null) {
                    descriptor += locals[l].getDescriptor();
                }
            }

            return descriptor + ")V";
        }
    }

    /**
     * True if the callback method is static
     */
    private final boolean isStatic;
    
    /**
     * Class node
     */
    private final ClassNode classNode;
    
    /**
     * Callback method 
     */
    private final MethodNode methodNode;
    
    /**
     * True if cancellable 
     */
    private final boolean cancellable;
    
    /**
     * True if capturing locals 
     */
    private final boolean captureLocals;

    /**
     * Make a new CallbackInjector for the supplied InjectionInfo
     */
    public CallbackInjector(InjectionInfo info) {
        this(info.getClassNode(), info.getMethod(), info.getCancellable(), info.getCaptureLocals());
    }

    /**
     * Make a new CallbackInjector with the supplied args
     */
    public CallbackInjector(ClassNode classNode, MethodNode methodNode, boolean cancellable, boolean captureLocals) {
        this.classNode = classNode;
        this.methodNode = methodNode;
        this.cancellable = cancellable;
        this.captureLocals = captureLocals;
        this.isStatic = ASMHelper.methodIsStatic(methodNode);
    }

    /**
     * Inject into the specified method at the specified injection points
     */
    public void injectInto(MethodNode into, List<InjectionPoint> injectionPoints) {
        this.sanityCheck(into, injectionPoints);

        for (AbstractInsnNode targetNode : this.findTargetNodes(into, injectionPoints)) {
            Type[] localTypes = null;

            if (this.captureLocals) {
                LocalVariableNode[] locals = Locals.getLocalsAt(this.classNode, into, targetNode);

                if (locals != null) {
                    localTypes = new Type[locals.length];
                    for (int l = 0; l < locals.length; l++) {
                        if (locals[l] != null) {
                            localTypes[l] = Type.getType(locals[l].desc);
                        }
                    }
                }
            }

            Target target = new Target(into);
            this.inject(target, targetNode, localTypes);
        }
    }

    private Set<AbstractInsnNode> findTargetNodes(MethodNode into, List<InjectionPoint> injectionPoints) {
        Set<AbstractInsnNode> targetNodes = new HashSet<AbstractInsnNode>();

        // Defensive objects, so that injectionPoint instances can't modify our working copies
        ReadOnlyInsnList insns = new ReadOnlyInsnList(into.instructions);
        Collection<AbstractInsnNode> nodes = new ArrayList<AbstractInsnNode>(32);

        for (InjectionPoint injectionPoint : injectionPoints) {
            nodes.clear();
            if (injectionPoint.find(into.desc, insns, nodes)) {
                targetNodes.addAll(nodes);
            }
        }
        
        insns.dispose();
        return targetNodes;
    }

    private void sanityCheck(MethodNode target, List<InjectionPoint> injectionPoints) {
        if (ASMHelper.methodIsStatic(target) != this.isStatic) {
            throw new InvalidInjectionException("'static' modifier of callback method does not match target in " + this.methodNode.name);
        }

        if (CallbackInjector.CTOR.equals(target.name)) {
            for (InjectionPoint injectionPoint : injectionPoints) {
                if (!injectionPoint.getClass().equals(BeforeReturn.class)) {
                    throw new InvalidInjectionException("Found injection point type " + injectionPoint.getClass().getSimpleName()
                            + " targetting a ctor in " + this.classNode.name + ". Only RETURN allowed for a ctor target");
                }
            }
        }
    }

    /**
     * Generate the actual bytecode
     */
    private void inject(Target target, final AbstractInsnNode targetNode, final Type[] locals) {
        // Calculate the initial frame size based on the target method's arguments
        int initialFrameSize = ASMHelper.getFirstNonArgLocalIndex(target.arguments, !this.isStatic);

        // Work out whether we have enough info to capture locals in this scenario
        boolean doCaptureLocals = this.captureLocals && locals != null && locals.length > initialFrameSize;
        
        // Determine the descriptor for the callback based on the target method and current context
        String callbackDescriptor = target.getCallbackDescriptor(doCaptureLocals, locals, target.arguments, initialFrameSize);
        
        // If it doesn't match, then cry
        if (!callbackDescriptor.equals(this.methodNode.desc)) {
            throw new InvalidInjectionException("Invalid descriptor on callback method, expected " + callbackDescriptor);
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
        if (targetNode instanceof InsnNode && targetNode.getOpcode() >= Opcodes.IRETURN && targetNode.getOpcode() < Opcodes.RETURN) {
            pushReturnValue = true;
            insns.add(new InsnNode(Opcodes.DUP));
            insns.add(new VarInsnNode(target.returnType.getOpcode(Opcodes.ISTORE), marshallVar));
        }

        // CHECKSTYLE:OFF
        // Instance the EventInfo for this event
        insns.add(new TypeInsnNode(Opcodes.NEW, target.callbackInfoClass)); ctorMAXS++;
        insns.add(new InsnNode(Opcodes.DUP)); ctorMAXS++; invokeMAXS++;
        // CHECKSTYLE:ON
        ctorMAXS += this.invokeCallbackInfoCtor(target, insns, this.cancellable, pushReturnValue, marshallVar);
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
        insns.add(new MethodInsnNode(this.isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKESPECIAL,
                this.classNode.name, this.methodNode.name, this.methodNode.desc, false));

        if (this.cancellable) {
            // Inject the if (e.isCancelled()) return e.getReturnValue();
            this.injectCancellationCode(target, insns, targetNode, marshallVar);
        }

        // Inject our generated code into the method
        target.method.instructions.insertBefore(targetNode, insns);
        target.method.maxStack = Math.max(target.method.maxStack, Math.max(target.MAXS + ctorMAXS, target.MAXS + invokeMAXS));
    }

    protected int invokeCallbackInfoCtor(Target target, InsnList insns, boolean cancellable, boolean pushReturnValue, int marshallVar) {
        int ctorMAXS = 0;

        // CHECKSTYLE:OFF
        insns.add(new LdcInsnNode(target.method.name)); ctorMAXS++;
        insns.add(new InsnNode(cancellable ? Opcodes.ICONST_1 : Opcodes.ICONST_0)); ctorMAXS++;
        // CHECKSTYLE:ON

        if (pushReturnValue) {
            insns.add(new VarInsnNode(target.returnType.getOpcode(Opcodes.ILOAD), marshallVar));
            insns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                    target.callbackInfoClass, CallbackInjector.CTOR, CallbackInfo.getConstructorDescriptor(target.returnType), false));
        } else {
            insns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                    target.callbackInfoClass, CallbackInjector.CTOR, CallbackInfo.getConstructorDescriptor(), false));
        }

        return ctorMAXS;
    }

    /**
     * if (e.isCancelled()) return e.getReturnValue();
     * 
     * @param insns
     * @param targetNode
     * @param marshallVar
     */
    protected void injectCancellationCode(Target target, final InsnList insns, final AbstractInsnNode targetNode, int marshallVar) {
        insns.add(new VarInsnNode(Opcodes.ALOAD, marshallVar));
        insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, target.callbackInfoClass, CallbackInfo.getIsCancelledMethodName(),
                CallbackInfo.getIsCancelledMethodSig(), false));

        LabelNode notCancelled = new LabelNode();
        insns.add(new JumpInsnNode(Opcodes.IFEQ, notCancelled));

        // If this is a void method, just injects a RETURN opcode, otherwise we need to get the return value from the EventInfo
        this.injectReturnCode(target, insns, targetNode, marshallVar);

        insns.add(notCancelled);
    }

    /**
     * Inject the appropriate return code for the method type
     * 
     * @param insns
     * @param targetNode
     * @param marshallVar
     */
    protected void injectReturnCode(Target target, final InsnList insns, final AbstractInsnNode targetNode, int marshallVar) {
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
