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
package org.spongepowered.asm.mixin.injection.callback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.*;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.Surrogate;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.points.BeforeReturn;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.throwables.InjectionError;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.Locals;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.asm.util.SignaturePrinter;

import com.google.common.base.Strings;

/**
 * This class is responsible for generating the bytecode for injected callbacks
 */
public class CallbackInjector extends Injector {
    
    /**
     * Struct to replace all the horrible state variables from before 
     */
    private class Callback extends InsnList {
        
        /**
         * Handler method
         */
        private final MethodNode handler;
        
        /**
         * HEAD instruction 
         */
        private final AbstractInsnNode head;
        
        /**
         * Target method handle
         */
        final Target target;
        
        /**
         * Target node, callback injected <b>before</b> this node 
         */
        final InjectionNode node;
        
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
         * Number of extra arguments above the initial frame size, expected to
         * be locals
         */
        final int extraArgs;

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
         * Arguments which require a type cast before being LOADed 
         */
//        final Type[] typeCasts;

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
         * Marshal var is the local where we marshal the utility references we
         * need during invoke of the callback, those being the current return
         * value for value-return scenarios (we store the topmost stack entry
         * and then push it into the ctor of the CallbackInfo) and also the
         * CallbackInfo reference itself (we use the same local var for these
         * two purposes because they don't exist at the same time).
         */
        private int marshalVar = -1;
        
        /**
         * True if this callback expects the target method arguments to be
         * passed in. Set to false if {@link #checkDescriptor} determines that
         * the "simple" descriptor matches. 
         */
        private boolean captureArgs = true;

        Callback(MethodNode handler, Target target, final InjectionNode node, final LocalVariableNode[] locals, boolean captureLocals) {
            this.handler = handler;
            this.target = target;
            this.head = target.insns.getFirst();
            this.node = node;
            this.locals = locals;
            this.localTypes = locals != null ? new Type[locals.length] : null;
            this.frameSize = Bytecode.getFirstNonArgLocalIndex(target.arguments, !CallbackInjector.this.isStatic());
            List<String> argNames = null;
            
            if (locals != null) {
                int baseArgIndex = CallbackInjector.this.isStatic() ? 0 : 1;
                argNames = new ArrayList<String>();
                for (int l = 0; l <= locals.length; l++) {
                    if (l == this.frameSize) {
                        argNames.add(target.returnType == Type.VOID_TYPE ? "ci" : "cir");
                    }
                    if (l < locals.length && locals[l] != null) {
                        this.localTypes[l] = Type.getType(locals[l].desc);
                        if (l >= baseArgIndex) {
                            argNames.add(CallbackInjector.meltSnowman(l, locals[l].name));
                        }
                    }
                }
            }
            
            // Calc number of args for the handler method, additional 1 is to ignore the CallbackInfo arg
            this.extraArgs = Math.max(0, Bytecode.getFirstNonArgLocalIndex(this.handler) - (this.frameSize + 1));
            this.argNames = argNames != null ? argNames.toArray(new String[argNames.size()]) : null;
            this.canCaptureLocals = captureLocals && locals != null && locals.length > this.frameSize;
            this.isAtReturn = this.node.getCurrentTarget() instanceof InsnNode && this.isValueReturnOpcode(this.node.getCurrentTarget().getOpcode());
            this.desc = target.getCallbackDescriptor(this.localTypes, target.arguments);
            this.descl = target.getCallbackDescriptor(true, this.localTypes, target.arguments, this.frameSize, this.extraArgs);
//            this.typeCasts = new Type[this.frameSize + this.extraArgs];

            this.invoke = target.arguments.length + (this.canCaptureLocals ? this.localTypes.length - this.frameSize : 0);
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
        
        String getDescriptorWithAllLocals() {
            return this.target.getCallbackDescriptor(true, this.localTypes, this.target.arguments, this.frameSize, Short.MAX_VALUE);
        }

        String getCallbackInfoConstructorDescriptor() {
            return this.isAtReturn ? CallbackInfo.getConstructorDescriptor(this.target.returnType) : CallbackInfo.getConstructorDescriptor();
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
            this.add(insn, ctorStack, invokeStack, false);
        }
        
        void add(AbstractInsnNode insn, boolean ctorStack, boolean invokeStack, boolean head) {
            if (head) {
                this.target.insns.insertBefore(this.head, insn);
            } else {
                this.add(insn);
            }
            this.ctor += (ctorStack ? 1 : 0);
            this.invoke += (invokeStack ? 1 : 0);
        }        
        
        /**
         * Inject our generated code into the method and set the max stack size
         * for the method based on our calculated values
         */
        void inject() {
            this.target.insertBefore(this.node, this);
            this.target.addToStack(Math.max(this.invoke, this.ctor));
        }

        boolean checkDescriptor(String desc) {
            if (this.getDescriptor().equals(desc)) {
                return true; // Descriptor matches exactly, this is good
            }
            
            if (this.target.getSimpleCallbackDescriptor().equals(desc) && !this.canCaptureLocals) {
                this.captureArgs = false;
                return true;
            }
            
            Type[] inTypes = Type.getArgumentTypes(desc);
            Type[] myTypes = Type.getArgumentTypes(this.descl);
            
            if (inTypes.length != myTypes.length) {
                return false;
            }
            
            for (int arg = 0; arg < myTypes.length; arg++) {
                Type type = inTypes[arg];
                if (type.equals(myTypes[arg])) {
                    continue; // Type matches
                }
                
                if (type.getSort() == Type.ARRAY) {
                    return false; // Array types must match exactly
                }

                if (Annotations.getInvisibleParameter(this.handler, Coerce.class, arg) == null) {
                    return false; // No @Coerce specified, types must match
                }

                if (!Injector.canCoerce(inTypes[arg], myTypes[arg])) {
//                    if (Injector.canCoerce(myTypes[arg], inTypes[arg])) {
//                        this.typeCasts[arg] = inTypes[arg];
//                    } else {
                        return false; // Can't coerce or cast source type to local type, give up
//                    }
                }
            }
            
            return true;
        }
        
        boolean captureArgs() {
            return this.captureArgs;
        }

        int marshalVar() {
            if (this.marshalVar < 0) {
                this.marshalVar = this.target.allocateLocal();
            }
            
            return this.marshalVar;
        }
        
    }
    
    /**
     * True if cancellable 
     */
    private final boolean cancellable;
    
    /**
     * Local variable capture behaviour
     */
    private final LocalCapture localCapture;
    
    /**
     * ID to return from callbackinfo 
     */
    private final String identifier;
    
    /**
     * Injection point ids
     */
    private final Map<Integer, String> ids = new HashMap<Integer, String>();
    
    /**
     * Total number of times this injector will be injected into the target. If
     * greater than 1 we will cache the generated CallbackInfo
     */
    private int totalInjections = 0;
    private int callbackInfoVar = -1;
    private String lastId, lastDesc;
    private Target lastTarget;
    private String callbackInfoClass;
    
    /**
     * Make a new CallbackInjector with the supplied args
     * 
     * @param info information about this injector
     * @param cancellable True if injections performed by this injector should
     *      be cancellable
     * @param localCapture Local variable capture behaviour
     */
    public CallbackInjector(InjectionInfo info, boolean cancellable, LocalCapture localCapture, String identifier) {
        super(info);
        this.cancellable = cancellable;
        this.localCapture = localCapture;
        this.identifier = identifier;
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

        if (Constants.CTOR.equals(target.method.name)) {
            for (InjectionPoint injectionPoint : injectionPoints) {
                if (!injectionPoint.getClass().equals(BeforeReturn.class)) {
                    throw new InvalidInjectionException(this.info, "Found injection point type " + injectionPoint.getClass().getSimpleName()
                            + " targetting a ctor in " + this + ". Only RETURN allowed for a ctor target");
                }
            }
        }
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.code.Injector#addTargetNode(
     *      org.spongepowered.asm.mixin.injection.struct.Target, java.util.List,
     *      org.spongepowered.asm.lib.tree.AbstractInsnNode, java.util.Set)
     */
    @Override
    protected void addTargetNode(Target target, List<InjectionNode> myNodes, AbstractInsnNode node, Set<InjectionPoint> nominators) {
        InjectionNode injectionNode = target.addInjectionNode(node);
        
        for (InjectionPoint ip : nominators) {
            String id = ip.getId();
            if (Strings.isNullOrEmpty(id)) {
                continue;
            }
            
            String existingId = this.ids.get(Integer.valueOf(injectionNode.getId()));
            if (existingId != null && !existingId.equals(id)) {
                Injector.logger.warn("Conflicting id for {} insn in {}, found id {} on {}, previously defined as {}", Bytecode.getOpcodeName(node),
                        target.toString(), id, this.info, existingId);
                break;
            }
            
            this.ids.put(Integer.valueOf(injectionNode.getId()), id);
        }
        
        myNodes.add(injectionNode);
        this.totalInjections++;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.callback.BytecodeInjector
     *      #inject(org.spongepowered.asm.mixin.injection.callback.Target,
     *      org.objectweb.asm.tree.AbstractInsnNode)
     */
    @Override
    protected void inject(Target target, InjectionNode node) {
        LocalVariableNode[] locals = null;

        if (this.localCapture.isCaptureLocals() || this.localCapture.isPrintLocals()) {
            locals = Locals.getLocalsAt(this.classNode, target.method, node.getCurrentTarget());
        }

        this.inject(new Callback(this.methodNode, target, node, locals, this.localCapture.isCaptureLocals()));
    }

    /**
     * Generate the actual bytecode for the callback
     * 
     * @param callback callback handle
     */
    private void inject(final Callback callback) {
        if (this.localCapture.isPrintLocals()) {
            this.printLocals(callback);
            this.info.addCallbackInvocation(this.methodNode);
            return;
        }
        
        // The actual callback method, to start with this is set to the handler
        // method but we will redirect to our generated handler if the signature
        // is invalid and we have to generate an error handler stub method
        MethodNode callbackMethod = this.methodNode;

        if (!callback.checkDescriptor(this.methodNode.desc)) {
            if (this.info.getTargets().size() > 1) {
                return; // Look for a match in other targets before failing
            }

            if (callback.canCaptureLocals) {
                // First check whether there is a compatible method in the class
                // the method must have an identical name and an appropriate
                // signature for the current locals. This allows silent failover
                // if changes to the local variable table are EXPECTED for some
                // reason.
                MethodNode surrogateHandler = Bytecode.findMethod(this.classNode, this.methodNode.name, callback.getDescriptor());
                if (surrogateHandler != null && Annotations.getVisible(surrogateHandler, Surrogate.class) != null) {
                    // Found a matching method, use it
                    callbackMethod = surrogateHandler;
                } else {
                    // No matching method, generate a message to bitch about it
                    String message = this.generateBadLVTMessage(callback);
                    
                    switch (this.localCapture) {
                        case CAPTURE_FAILEXCEPTION:
                            Injector.logger.error("Injection error: {}", message);
                            callbackMethod = this.generateErrorMethod(callback, "org/spongepowered/asm/mixin/injection/throwables/InjectionError",
                                    message);
                            break;
                        case CAPTURE_FAILSOFT:
                            Injector.logger.warn("Injection warning: {}", message);
                            return;
                        default:
                            Injector.logger.error("Critical injection failure: {}", message);
                            throw new InjectionError(message);
                    }
                }
            } else {
                // Check whether user is just using the wrong CallbackInfo type
                String returnableSig = this.methodNode.desc.replace(
                        "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;",
                        "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;");

                if (callback.checkDescriptor(returnableSig)) {
                    // Switching out CallbackInfo for CallbackInfoReturnable
                    // worked, so notify the user that they done derped
                    throw new InvalidInjectionException(this.info, "Invalid descriptor on " + this.info + "! CallbackInfoReturnable is required!");  
                }
                
                MethodNode surrogateHandler = Bytecode.findMethod(this.classNode, this.methodNode.name, callback.getDescriptor());
                if (surrogateHandler != null && Annotations.getVisible(surrogateHandler, Surrogate.class) != null) {
                    // Found a matching surrogate method, use it
                    callbackMethod = surrogateHandler;
                } else {
                    throw new InvalidInjectionException(this.info, "Invalid descriptor on " + this.info + "! Expected " + callback.getDescriptor()
                            + " but found " + this.methodNode.desc);
                }
            }
        }
        
        this.dupReturnValue(callback);
        if (this.cancellable || this.totalInjections > 1) {
            this.createCallbackInfo(callback, true);
        }
        this.invokeCallback(callback, callbackMethod);
        this.injectCancellationCode(callback);
        
        callback.inject();
        this.info.notifyInjected(callback.target);
    }

    /**
     * Generate the "bad local variable table" message
     * 
     * @param callback callback handle
     * @return generated message
     */
    private String generateBadLVTMessage(final Callback callback) {
        int position = callback.target.indexOf(callback.node);
        List<String> expected = CallbackInjector.summariseLocals(this.methodNode.desc, callback.target.arguments.length + 1);
        List<String> found = CallbackInjector.summariseLocals(callback.getDescriptorWithAllLocals(), callback.frameSize);
        return String.format("LVT in %s has incompatible changes at opcode %d in callback %s.\nExpected: %s\n   Found: %s",
                callback.target, position, this, expected, found);
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
        method.maxLocals = Bytecode.getFirstNonArgLocalIndex(Type.getArgumentTypes(callback.getDescriptor()), !this.isStatic);
        method.maxStack = 3;
        InsnList insns = method.instructions;
        insns.add(new TypeInsnNode(Opcodes.NEW, errorClass));
        insns.add(new InsnNode(Opcodes.DUP));
        insns.add(new LdcInsnNode(message));
        insns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, errorClass, Constants.CTOR, "(Ljava/lang/String;)V", false));
        insns.add(new InsnNode(Opcodes.ATHROW));
        return method;
    }
    
    /**
     * Pretty-print local variable information to stderr
     * 
     * @param callback callback handle
     */
    private void printLocals(final Callback callback) {
        Type[] args = Type.getArgumentTypes(callback.getDescriptorWithAllLocals());
        SignaturePrinter methodSig = new SignaturePrinter(callback.target.method, callback.argNames);
        SignaturePrinter handlerSig = new SignaturePrinter(this.methodNode.name, callback.target.returnType, args, callback.argNames);
        handlerSig.setModifiers(this.methodNode);
        
        PrettyPrinter printer = new PrettyPrinter();
        printer.kv("Target Class", this.classNode.name.replace('/', '.'));
        printer.kv("Target Method", methodSig);
        printer.kv("Target Max LOCALS", callback.target.getMaxLocals());
        printer.kv("Initial Frame Size", callback.frameSize);
        printer.kv("Callback Name", this.methodNode.name);
        printer.kv("Instruction", "%s %s", callback.node.getClass().getSimpleName(),
                Bytecode.getOpcodeName(callback.node.getCurrentTarget().getOpcode()));
        printer.hr();
        if (callback.locals.length > callback.frameSize) {
            printer.add("  %s  %20s  %s", "LOCAL", "TYPE", "NAME");
            for (int l = 0; l < callback.locals.length; l++) {
                String marker = l == callback.frameSize ? ">" : " ";
                if (callback.locals[l] != null) {
                    printer.add("%s [%3d]  %20s  %-50s %s", marker, l, SignaturePrinter.getTypeName(callback.localTypes[l], false),
                            CallbackInjector.meltSnowman(l, callback.locals[l].name), l >= callback.frameSize ? "<capture>" : "");
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
     * @param store store the callback info in a local variable
     */
    private void createCallbackInfo(final Callback callback, boolean store) {
        // Reset vars on new target
        if (callback.target != this.lastTarget) {
            this.lastId = null;
            this.lastDesc = null;
        }
        this.lastTarget = callback.target;

        String id = this.getIdentifier(callback);
        String desc = callback.getCallbackInfoConstructorDescriptor();
        
        // If ID and descriptor match, and if we're not handling a returnable or cancellable CI, just re-use the last one
        if (id.equals(this.lastId) && desc.equals(this.lastDesc) && !callback.isAtReturn && !this.cancellable) {
            return;
        }

        this.instanceCallbackInfo(callback, id, desc, store);
    }

    /**
     * @param callback callback handle
     */
    private void loadOrCreateCallbackInfo(final Callback callback) {
        if (this.cancellable || this.totalInjections > 1) {
            callback.add(new VarInsnNode(Opcodes.ALOAD, this.callbackInfoVar), false, true);
        } else {
            this.createCallbackInfo(callback, false);
        }
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
        callback.add(new VarInsnNode(callback.target.returnType.getOpcode(Opcodes.ISTORE), callback.marshalVar()));
    }

    /**
     * @param callback callback handle
     * @param id callback id
     * @param desc constructor descriptor
     * @param store true if storing in a local, false if this is happening at an
     *      invoke
     */
    protected void instanceCallbackInfo(final Callback callback, String id, String desc, boolean store) {
        this.lastId = id;
        this.lastDesc = desc;
        this.callbackInfoVar = callback.marshalVar();
        this.callbackInfoClass = callback.target.getCallbackInfoClass();
        
        // If we were going to store the CI anyway, and if we need it again, and if the current injection isn't at
        // return or cancellable, inject the CI creation at the method head so that it's available everywhere
        boolean head = store && this.totalInjections > 1 && !callback.isAtReturn && !this.cancellable;
        
        callback.add(new TypeInsnNode(Opcodes.NEW, this.callbackInfoClass), true, !store, head);
        callback.add(new InsnNode(Opcodes.DUP), true, true, head);
        callback.add(new LdcInsnNode(id), true, !store, head);
        callback.add(new InsnNode(this.cancellable ? Opcodes.ICONST_1 : Opcodes.ICONST_0), true, !store, head);

        if (callback.isAtReturn) {
            callback.add(new VarInsnNode(callback.target.returnType.getOpcode(Opcodes.ILOAD), callback.marshalVar()), true, !store);
            callback.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                    this.callbackInfoClass, Constants.CTOR, desc, false));
        } else {
            callback.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                    this.callbackInfoClass, Constants.CTOR, desc, false), false, false, head);
        }
        
        if (store) {
            callback.target.addLocalVariable(this.callbackInfoVar, "callbackInfo" + this.callbackInfoVar, "L" + this.callbackInfoClass + ";");
            callback.add(new VarInsnNode(Opcodes.ASTORE, this.callbackInfoVar), false, false, head);
        }
    }

    /**
     * @param callback callback handle
     */
    private void invokeCallback(final Callback callback, final MethodNode callbackMethod) {
        // Push "this" onto the stack if the callback is not static
        if (!this.isStatic) {
            callback.add(new VarInsnNode(Opcodes.ALOAD, 0), false, true);
        }

        // Push the target method's parameters onto the stack
        if (callback.captureArgs()) {
            Bytecode.loadArgs(callback.target.arguments, callback, this.isStatic ? 0 : 1, -1); //, callback.typeCasts);
        }
        
        // Push the callback info onto the stack
        this.loadOrCreateCallbackInfo(callback);
        
        // (Maybe) push the locals onto the stack
        if (callback.canCaptureLocals) {
            Locals.loadLocals(callback.localTypes, callback, callback.frameSize, callback.extraArgs);
        }
        
        // Call the callback!
        this.invokeHandler(callback, callbackMethod);
    }

    /**
     * Get the identifier to use for the specified callback. If an id was
     * specified by the end user on the annotation then use the value specified,
     * otherwise defaults to the target method name.
     * 
     * @param callback Callback being injected
     * @return Identifier to use
     */
    private String getIdentifier(Callback callback) {
        String baseId = Strings.isNullOrEmpty(this.identifier) ? callback.target.method.name : this.identifier;
        String locationId = this.ids.get(Integer.valueOf(callback.node.getId()));
        return baseId + (Strings.isNullOrEmpty(locationId) ? "" : ":" + locationId);
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
        
        callback.add(new VarInsnNode(Opcodes.ALOAD, this.callbackInfoVar));
        callback.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, this.callbackInfoClass, CallbackInfo.getIsCancelledMethodName(),
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
            callback.add(new VarInsnNode(Opcodes.ALOAD, callback.marshalVar()));
            String accessor = CallbackInfoReturnable.getReturnAccessor(callback.target.returnType);
            String descriptor = CallbackInfoReturnable.getReturnDescriptor(callback.target.returnType);
            callback.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, this.callbackInfoClass, accessor, descriptor, false));
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

    static String meltSnowman(int index, String varName) {
        return varName != null && Constants.UNICODE_SNOWMAN == varName.charAt(0) ? "var" + index : varName;
    }
    
}
