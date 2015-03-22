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
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionError;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InvalidInjectionException;
import org.spongepowered.asm.mixin.injection.Surrogate;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.points.BeforeReturn;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.ASMHelper;
import org.spongepowered.asm.util.Locals;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.asm.util.SignaturePrinter;

/**
 * This class is responsible for generating the bytecode for injected callbacks
 */
public class CallbackInjector extends Injector {
    
    /**
     * Struct to replace all the horrible state variables from before 
     */
    private class Callback extends InsnList {
        
        /**
         * Target method handle
         */
        final Target target;
        
        /**
         * Target node, callback injected <b>before</b> this node 
         */
        final AbstractInsnNode node;
        
        /**
         * Calculated local variables 
         */
        final LocalVariableNode[] locals;
        
        /**
         * Local variable types
         */
        final Type[] localTypes;
        
        /**
         * The initial frame size based on the target method's arguments
         */
        final int frameSize;

        /**
         * True if the injector is set to capture locals and we acutally <b>can
         * </b> capture the locals (have sufficient info etc.)
         */
        final boolean canCaptureLocals;

        /**
         * True if the target insn is a RETURN opcode
         */
        final boolean isAtReturn;

        /**
         * Callback descriptor without locals
         */
        final String desc;
        
        /**
         * Callback descriptor with locals
         */
        final String descl;
        
        /**
         * Callback argument names, unlike the locals arrays this array does not
         * contain null entries for TOP slots, so each index in this array
         * matches the corresponding argument index in the callback  
         */
        final String[] argNames;

        /**
         * These two variables keep track of the (additional) stack size
         * required for the two actions we're going to be injecting insns to
         * perform, namely calling the CallbackInfo ctor, and then invoking the
         * callback itself. When we get to the end of this injection we will
         * then set the MAXS value on the target method to its original value
         * plus the larger of the two values.
         */
        int ctor, invoke;

        /**
         * Marshall var is the local where we marshall the utility references we
         * need during invoke of the callback, those being the current return
         * value for value-return scenarios (we store the topmost stack entry
         * and then push it into the ctor of the CallbackInfo) and also the
         * CallbackInfo reference itself (we use the same local var for these
         * two purposes because they don't exist at the same time).
         */
        final int marshallVar;

        Callback(Target target, final AbstractInsnNode node, final LocalVariableNode[] locals, boolean captureLocals) {
            this.target = target;
            this.node = node;
            this.locals = locals;
            this.localTypes = locals != null ? new Type[locals.length] : null;
            this.frameSize = ASMHelper.getFirstNonArgLocalIndex(target.arguments, !CallbackInjector.this.isStatic());
            List<String> argNames = null; 
            
            if (locals != null) {
                argNames = new ArrayList<String>();
                for (int l = 0; l <= locals.length; l++) {
                    if (l == this.frameSize) {
                        argNames.add(target.returnType == Type.VOID_TYPE ? "ci" : "cir");
                    }
                    if (l < locals.length && locals[l] != null) {
                        this.localTypes[l] = Type.getType(locals[l].desc);
                        if (l > 0) {
                            argNames.add(locals[l].name);
                        }
                    }
                }
            }

            this.argNames = argNames != null ? argNames.toArray(new String[argNames.size()]) : null;
            this.canCaptureLocals = captureLocals && locals != null && locals.length > this.frameSize;
            this.isAtReturn = this.node instanceof InsnNode && this.isValueReturnOpcode(this.node.getOpcode());
            this.desc = target.getCallbackDescriptor(false, this.localTypes, target.arguments, this.frameSize);
            this.descl = target.getCallbackDescriptor(true, this.localTypes, target.arguments, this.frameSize);

            this.invoke = target.arguments.length + (this.canCaptureLocals ? this.localTypes.length - this.frameSize : 0);
            this.marshallVar = target.method.maxLocals++;
        }
        
        /**
         * Returns true if the supplied opcode represents a <em>non-void</em>
         * RETURN opcode
         * 
         * @param opcode opcode to check
         * @return true if value return
         */
        private boolean isValueReturnOpcode(int opcode) {
            return opcode >= Opcodes.IRETURN && opcode < Opcodes.RETURN;
        }
        
        String getDescriptor() {
            return this.canCaptureLocals ? this.descl : this.desc;
        }

        /**
         * Add an instruction to this callback and increment the appropriate
         * stack sizes
         * 
         * @param insn Instruction to append
         * @param ctorStack true if this insn contributes to the ctor stack
         * @param invokeStack true if this insn contributes to the invoke stack
         */
        void add(AbstractInsnNode insn, boolean ctorStack, boolean invokeStack) {
            this.add(insn);
            this.ctor += (ctorStack ? 1 : 0);
            this.invoke += (invokeStack ? 1 : 0);
        }
        
        /**
         * Inject our generated code into the method and set the max stack size
         * for the method based on our calculated values
         */
        void inject() {
            this.target.method.instructions.insertBefore(this.node, this);
            this.target.method.maxStack = Math.max(this.target.method.maxStack, this.target.maxStack + Math.max(this.invoke, this.ctor));
        }
    }
    
    private final Logger logger = LogManager.getLogger("mixin");
    
    /**
     * True if cancellable 
     */
    private final boolean cancellable;
    
    /**
     * Local variable capture behaviour
     */
    private final LocalCapture localCapture;
    
    /**
     * Make a new CallbackInjector with the supplied args
     * 
     * @param info information about this injector
     * @param cancellable True if injections performed by this injector should
     *      be cancellable
     * @param localCapture Local variable capture behaviour
     */
    public CallbackInjector(InjectionInfo info, boolean cancellable, LocalCapture localCapture) {
        super(info);
        this.cancellable = cancellable;
        this.localCapture = localCapture;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.callback.BytecodeInjector
     *      #sanityCheck(org.spongepowered.asm.mixin.injection.callback.Target,
     *      java.util.List)
     */
    @Override
    protected void sanityCheck(Target target, List<InjectionPoint> injectionPoints) {
        if (ASMHelper.methodIsStatic(target.method) != this.isStatic) {
            throw new InvalidInjectionException(this.info, "'static' modifier of callback method does not match target in " + this.methodNode.name);
        }

        if (Injector.CTOR.equals(target.method.name)) {
            for (InjectionPoint injectionPoint : injectionPoints) {
                if (!injectionPoint.getClass().equals(BeforeReturn.class)) {
                    throw new InvalidInjectionException(this.info, "Found injection point type " + injectionPoint.getClass().getSimpleName()
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
        LocalVariableNode[] locals = null;

        if (this.localCapture.isCaptureLocals() || this.localCapture.isPrintLocals()) {
            locals = Locals.getLocalsAt(this.classNode, target.method, node);
        }

        this.inject(new Callback(target, node, locals, this.localCapture.isCaptureLocals()));
    }

    /**
     * Generate the actual bytecode for the callback
     * 
     * @param callback callback handle
     */
    private void inject(final Callback callback) {
        if (this.localCapture.isPrintLocals()) {
            this.printLocals(callback);
            return;
        }
        
        // The actual callback method, to start with this is set to the handler
        // method but we will redirect to our generated handler if the signature
        // is invalid and we have to generate an error handler stub method
        MethodNode callbackMethod = this.methodNode;

        if (!callback.getDescriptor().equals(this.methodNode.desc)) {
            
            if (callback.canCaptureLocals) {
                // First check whether there is a compatible method in the class
                // the method must have an identical name and an appropriate
                // signature for the current locals. This allows silent failover
                // if changes to the local variable table are EXPECTED for some
                // reason.
                MethodNode handler = ASMHelper.findMethod(this.classNode, this.methodNode.name, callback.getDescriptor());
                if (handler != null && ASMHelper.getVisibleAnnotation(handler, Surrogate.class) != null) {
                    // Found a matching method, use it
                    callbackMethod = handler;
                } else {
                    // No matching method, generate a message to bitch about it
                    String message = this.generateBadLVTMessage(callback);
                    
                    switch (this.localCapture) {
                        case CAPTURE_FAILEXCEPTION:
                            this.logger.error("Injection error: {}", message);
                            callbackMethod = this.generateErrorMethod(callback, "org/spongepowered/asm/mixin/injection/InjectionError", message);
                            break;
                        case CAPTURE_FAILSOFT:
                            this.logger.warn("Injection warning: {}", message);
                            return;
                        default:
                            this.logger.error("Critical injection failure: {}", message);
                            throw new InjectionError(message);
                    }
                }
            } else {
                throw new InvalidInjectionException(this.info, "Invalid descriptor on callback: expected " + callback.getDescriptor()
                        + " but found " + this.methodNode.desc);
            }
        }
        
        this.createCallbackInfo(callback);
        this.invokeCallback(callback, callbackMethod);
        this.injectCancellationCode(callback);
        
        callback.inject();
    }

    /**
     * Generate the "bad local variable table" message
     * 
     * @param callback callback handle
     * @return generated message
     */
    private String generateBadLVTMessage(final Callback callback) {
        List<String> expected = CallbackInjector.summariseLocals(this.methodNode.desc, callback.target.arguments.length + 1);
        List<String> found = CallbackInjector.summariseLocals(callback.localTypes, callback.frameSize);
        String message = String.format("Local variable table in %s::%s has incompatible changes. Expected %s but found %s. Callback %s%s",
                this.classNode.name.replace('/', '.'), callback.target.method.name, expected, found, this.methodNode.name, this.methodNode.desc);
        return message;
    }

    /**
     * Generates a method which throws an error
     * 
     * @param callback callback handle
     * @param errorClass error class to throw
     * @param message message for the error
     * @return generated method
     */
    private MethodNode generateErrorMethod(Callback callback, String errorClass, String message) {
        MethodNode method = this.info.addMethod(this.methodNode.access, this.methodNode.name + "$missing", callback.getDescriptor());
        method.maxLocals = ASMHelper.getFirstNonArgLocalIndex(Type.getArgumentTypes(callback.getDescriptor()), !this.isStatic);
        method.maxStack = 3;
        InsnList insns = method.instructions;
        insns.add(new TypeInsnNode(Opcodes.NEW, errorClass));
        insns.add(new InsnNode(Opcodes.DUP));
        insns.add(new LdcInsnNode(message));
        insns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, errorClass, "<init>", "(Ljava/lang/String;)V", false));
        insns.add(new InsnNode(Opcodes.ATHROW));
        return method;
    }
    
    /**
     * Pretty-print local variable information to stderr
     * 
     * @param callback callback handle
     */
    private void printLocals(final Callback callback) {
        Type[] args = Type.getArgumentTypes(callback.descl);
        SignaturePrinter methodSig = new SignaturePrinter(callback.target.method, callback.argNames);
        SignaturePrinter handlerSig = new SignaturePrinter(this.methodNode.name, callback.target.returnType, args, callback.argNames);
        handlerSig.setModifiers(this.methodNode);
        
        PrettyPrinter printer = new PrettyPrinter();
        printer.add("%20s : %s", "Target Class", this.classNode.name.replace('/', '.'));
        printer.add("%20s : %s", "Target Method", methodSig);
        printer.add("%20s : %d", "Target Max LOCALS", callback.target.maxLocals);
        printer.add("%20s : %d", "Initial Frame Size", callback.frameSize);
        printer.add("%20s : %s", "Callback Name", this.methodNode.name);
        printer.add("%20s : %s %s", "Instruction", callback.node.getClass().getSimpleName(), ASMHelper.getOpcodeName(callback.node.getOpcode()));
        printer.hr();
        if (callback.locals.length > callback.frameSize) {
            printer.add("  LOCAL                  TYPE  NAME");
            for (int l = 0; l < callback.locals.length; l++) {
                String marker = l == callback.frameSize ? ">" : " ";
                if (callback.locals[l] != null) {
                    printer.add("%s [%3d]  %20s  %-50s %s", marker, l, SignaturePrinter.getTypeName(callback.localTypes[l], false),
                            callback.locals[l].name, l >= callback.frameSize ? "<capture>" : "");
                } else {
                    boolean isTop = l > 0 && callback.localTypes[l - 1] != null && callback.localTypes[l - 1].getSize() > 1;
                    printer.add("%s [%3d]  %20s", marker, l, isTop ? "<top>" : "-");
                }
            }
            printer.hr();
        }
        printer.add().add("/**").add(" * Expected callback signature").add(" * /");
        printer.add("%s {", handlerSig);
        printer.add("    // Method body").add("}").add().print(System.err);
    }

    /**
     * @param callback callback handle
     */
    private void createCallbackInfo(final Callback callback) {
        this.dupReturnValue(callback);

        callback.add(new TypeInsnNode(Opcodes.NEW, callback.target.callbackInfoClass), true, false);
        callback.add(new InsnNode(Opcodes.DUP), true, true);
        
        this.invokeCallbackInfoCtor(callback);
        callback.add(new VarInsnNode(Opcodes.ASTORE, callback.marshallVar));
    }

    /**
     * If this is a ReturnEventInfo AND we are right before a RETURN opcode (so
     * we can expect the *original* return value to be on the stack, then we dup
     * the return value into a local var so we can push it later when we invoke
     * the ReturnEventInfo ctor
     * 
     * @param callback callback handle
     */
    private void dupReturnValue(final Callback callback) {
        if (!callback.isAtReturn) {
            return;
        }
        
        callback.add(new InsnNode(Opcodes.DUP));
        callback.add(new VarInsnNode(callback.target.returnType.getOpcode(Opcodes.ISTORE), callback.marshallVar));
    }

    /**
     * @param callback callback handle
     */
    protected void invokeCallbackInfoCtor(final Callback callback) {
        callback.add(new LdcInsnNode(callback.target.method.name), true, false);
        callback.add(new InsnNode(this.cancellable ? Opcodes.ICONST_1 : Opcodes.ICONST_0), true, false);

        if (callback.isAtReturn) {
            callback.add(new VarInsnNode(callback.target.returnType.getOpcode(Opcodes.ILOAD), callback.marshallVar));
            callback.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                    callback.target.callbackInfoClass, Injector.CTOR, CallbackInfo.getConstructorDescriptor(callback.target.returnType), false));
        } else {
            callback.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                    callback.target.callbackInfoClass, Injector.CTOR, CallbackInfo.getConstructorDescriptor(), false));
        }
    }

    /**
     * @param callback callback handle
     */
    private void invokeCallback(final Callback callback, final MethodNode callbackMethod) {
        // Push "this" onto the stack if the callback is not static
        if (!this.isStatic) {
            callback.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }

        // Push the target method's parameters onto the stack
        ASMHelper.loadArgs(callback.target.arguments, callback, this.isStatic ? 0 : 1);
        
        // Push the callback info onto the stack
        callback.add(new VarInsnNode(Opcodes.ALOAD, callback.marshallVar));
        
        // (Maybe) push the locals onto the stack
        if (callback.canCaptureLocals) {
            Locals.loadLocals(callback.localTypes, callback, callback.frameSize);
        }

        // Call the callback!
        // Set to private to avoid overriding callbacks from mixins to superclasses
        callbackMethod.access = Opcodes.ACC_PRIVATE;
        this.invokeMethod(callback, callbackMethod);
    }

    /**
     * if (e.isCancelled()) return e.getReturnValue();
     * 
     * @param callback callback handle
     */
    protected void injectCancellationCode(final Callback callback) {
        if (!this.cancellable) {
            return;
        }
        
        callback.add(new VarInsnNode(Opcodes.ALOAD, callback.marshallVar));
        callback.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, callback.target.callbackInfoClass, CallbackInfo.getIsCancelledMethodName(),
                CallbackInfo.getIsCancelledMethodSig(), false));

        LabelNode notCancelled = new LabelNode();
        callback.add(new JumpInsnNode(Opcodes.IFEQ, notCancelled));

        // If this is a void method, just injects a RETURN opcode, otherwise we
        // need to get the return value from the EventInfo
        this.injectReturnCode(callback);

        callback.add(notCancelled);
    }

    /**
     * Inject the appropriate return code for the method type
     * 
     * @param callback callback handle
     */
    protected void injectReturnCode(final Callback callback) {
        if (callback.target.returnType.equals(Type.VOID_TYPE)) {
            // Void method, so just return void
            callback.add(new InsnNode(Opcodes.RETURN));
        } else {
            // Non-void method, so work out which accessor to call to get the
            // return value, and return it
            callback.add(new VarInsnNode(Opcodes.ALOAD, callback.marshallVar));
            String accessor = CallbackInfoReturnable.getReturnAccessor(callback.target.returnType);
            String descriptor = CallbackInfoReturnable.getReturnDescriptor(callback.target.returnType);
            callback.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, callback.target.callbackInfoClass, accessor, descriptor, false));
            if (callback.target.returnType.getSort() == Type.OBJECT) {
                callback.add(new TypeInsnNode(Opcodes.CHECKCAST, callback.target.returnType.getInternalName()));
            }
            callback.add(new InsnNode(callback.target.returnType.getOpcode(Opcodes.IRETURN)));
        }
    }
    
    /**
     * Explicit to avoid creation of synthetic accessor
     * 
     * @return true if the target method is static
     */
    protected boolean isStatic() {
        return this.isStatic;
    }

    private static List<String> summariseLocals(String desc, int pos) {
        return CallbackInjector.summariseLocals(Type.getArgumentTypes(desc), pos);
    }

    private static List<String> summariseLocals(Type[] locals, int pos) {
        List<String> list = new ArrayList<String>();
        if (locals != null) {
            for (; pos < locals.length; pos++) {
                if (locals[pos] != null) {
                    list.add(locals[pos].toString());
                }
            }
        }
        return list;
    }
}
