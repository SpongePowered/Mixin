/**
 * This file contributed from LiteLoader. Pending refactor. DO NOT ALTER THIS FILE.
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
import org.spongepowered.asm.mixin.injection.InjectionInfo;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InvalidInjectionException;
import org.spongepowered.asm.mixin.injection.points.BeforeReturn;
import org.spongepowered.asm.mixin.injection.tree.ReadOnlyInsnList;
import org.spongepowered.asm.util.ASMHelper;
import org.spongepowered.asm.util.ByteCodeUtilities;


public class CallbackInjector {
    
    private static final String CTOR = "<init>";

    class Target
    {
        final MethodNode method;
        final Type returnType;
        final int MAXS;
        
        final String callbackDescriptor;
        final String callInfoClass;
        
        Target(MethodNode method)
        {
            this.method             = method;
            this.returnType         = Type.getReturnType(method.desc);
            this.MAXS               = method.maxStack;
            this.callInfoClass      = CallbackInfo.getCallInfoClassName(this.returnType);
            this.callbackDescriptor = String.format("(%sL%s;)V", method.desc.substring(1, method.desc.indexOf(')')), this.callInfoClass);
        }

        String getCallbackDescriptor(final boolean captureLocals, final Type[] locals, Type[] argumentTypes, int startIndex)
        {
            if (!captureLocals) return this.callbackDescriptor;
            
            String descriptor = this.callbackDescriptor.substring(0, this.callbackDescriptor.indexOf(')'));
            for (int l = startIndex; l < locals.length; l++)
            {
                if (locals[l] != null) descriptor += locals[l].getDescriptor();
            }
            
            return descriptor + ")V";
        }
    }

    private final boolean isStatic;
    private final ClassNode classNode;
    private final MethodNode methodNode;
    private final boolean cancellable;
    private final boolean captureLocals;
    
    public CallbackInjector(InjectionInfo info)
    {
        this(info.getClassNode(), info.getMethod(), info.getCancellable(), info.getCaptureLocals());
    }
    
    public CallbackInjector(ClassNode classNode, MethodNode methodNode, boolean cancellable, boolean captureLocals)
    {
        this.classNode     = classNode;
        this.methodNode    = methodNode;
        this.cancellable   = cancellable;
        this.captureLocals = captureLocals;
        this.isStatic      = ASMHelper.methodIsStatic(methodNode);
    }
    
    public void injectInto(MethodNode into, List<InjectionPoint> injectionPoints)
    {
        this.sanityCheck(into, injectionPoints);
        
        for (AbstractInsnNode targetNode : this.findTargetNodes(into, injectionPoints))
        {
            Type[] localTypes = null;
            
            if (this.captureLocals)
            {
                LocalVariableNode[] locals = ByteCodeUtilities.getLocalsAt(this.classNode, into, targetNode);
                
                if (locals != null)
                {
                    localTypes = new Type[locals.length];
                    for (int l = 0; l < locals.length; l++)
                    {
                        if (locals[l] != null)
                        {
                            localTypes[l] = Type.getType(locals[l].desc);
                        }
                    }
                }

//                if (injectionPoint.logLocals())
//                {
//                    int startPos = ByteCodeUtilities.getFirstNonArgLocalIndex(into);
//                    
//                    LiteLoaderLogger.debug(ClassTransformer.HORIZONTAL_RULE);
//                    LiteLoaderLogger.debug("Logging local variables for " + injectionPoint);
//                    for (int i = startPos; i < locals.length; i++)
//                    {
//                        LocalVariableNode local = locals[i];
//                        if (local != null)
//                        {
//                            LiteLoaderLogger.debug("    Local[%d] %s %s", i, ByteCodeUtilities.getTypeName(Type.getType(local.desc)), local.name);
//                        }
//                    }
//                    LiteLoaderLogger.debug(ClassTransformer.HORIZONTAL_RULE);
//                }
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

        for (InjectionPoint injectionPoint : injectionPoints)
        {
            nodes.clear();
            if (injectionPoint.find(into.desc, insns, nodes))
            {
                targetNodes.addAll(nodes);
            }
        }
        return targetNodes;
    }

    private void sanityCheck(MethodNode target, List<InjectionPoint> injectionPoints)
    {
        if (ASMHelper.methodIsStatic(target) != this.isStatic)
        {
            throw new InvalidInjectionException("'static' modifier of callback method does not match target in " + this.methodNode.name);
        }
        
        if (CallbackInjector.CTOR.equals(target.name))
        {
            for (InjectionPoint injectionPoint : injectionPoints)
            {
                if (!injectionPoint.getClass().equals(BeforeReturn.class))
                {
                    throw new InvalidInjectionException("Found injection point type " + injectionPoint.getClass().getSimpleName() + " targetting a ctor in " + this.classNode.name + ". Only RETURN allowed for a ctor target");
                }
            }
        }
    }
    
    private void inject(Target target, final AbstractInsnNode targetNode, final Type[] locals)
    {
        Type[] arguments = Type.getArgumentTypes(target.method.desc);
        int initialFrameSize = ByteCodeUtilities.getFirstNonArgLocalIndex(arguments, !this.isStatic);

        boolean doCaptureLocals = this.captureLocals && locals != null && locals.length > initialFrameSize;
        String callbackDescriptor = target.getCallbackDescriptor(doCaptureLocals, locals, arguments, initialFrameSize);
        if (!callbackDescriptor.equals(this.methodNode.desc))
        {
            throw new InvalidInjectionException("Invalid descriptor on callback method, expected " + callbackDescriptor);
        }
        
        int ctorMAXS = 0, invokeMAXS = arguments.length + (doCaptureLocals ? locals.length - initialFrameSize : 0);
        
        // Marshall var is the local where we marshall the utility references we need during invoke of the callback
        int marshallVar = target.method.maxLocals++;
        
        InsnList insns = new InsnList();
        
        boolean pushReturnValue = false;
        
        // If this is a ReturnEventInfo AND we are right before a RETURN opcode (so we can expect the *original* return
        // value to be on the stack, then we dup the return value into a local var so we can push it later when we invoke 
        // the ReturnEventInfo ctor
        if (targetNode instanceof InsnNode && targetNode.getOpcode() >= Opcodes.IRETURN && targetNode.getOpcode() < Opcodes.RETURN)
        {
            pushReturnValue = true;
            insns.add(new InsnNode(Opcodes.DUP));
            insns.add(new VarInsnNode(target.returnType.getOpcode(Opcodes.ISTORE), marshallVar));
        }
        
        // Instance the EventInfo for this event
        insns.add(new TypeInsnNode(Opcodes.NEW, target.callInfoClass)); ctorMAXS++;
        insns.add(new InsnNode(Opcodes.DUP)); ctorMAXS++; invokeMAXS++;
        ctorMAXS += this.invokeCallbackInfoCtor(target, insns, this.cancellable, pushReturnValue, marshallVar);
        insns.add(new VarInsnNode(Opcodes.ASTORE, marshallVar));
        
        if (!this.isStatic)
        {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        
        // Call the event handler method in the proxy
        ByteCodeUtilities.loadArgs(arguments, insns, this.isStatic ? 0 : 1);
        insns.add(new VarInsnNode(Opcodes.ALOAD, marshallVar));
        if (doCaptureLocals)
        {
            ByteCodeUtilities.loadLocals(locals, insns, initialFrameSize);
        }
        insns.add(new MethodInsnNode(this.isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKESPECIAL, this.classNode.name, this.methodNode.name, this.methodNode.desc, false));
        
        if (this.cancellable)
        {
            // Inject the if (e.isCancelled()) return e.getReturnValue();
            this.injectCancellationCode(target, insns, targetNode, marshallVar);
        }
        
        // Inject our generated code into the method
        target.method.instructions.insertBefore(targetNode, insns);
        target.method.maxStack = Math.max(target.method.maxStack, Math.max(target.MAXS + ctorMAXS, target.MAXS + invokeMAXS));
    }

    protected int invokeCallbackInfoCtor(Target target, InsnList insns, boolean cancellable, boolean pushReturnValue, int marshallVar)
    {
        int ctorMAXS = 0;
        
        insns.add(new LdcInsnNode(target.method.name)); ctorMAXS++;
//        insns.add(target.methodIsStatic ? new InsnNode(Opcodes.ACONST_NULL) : new VarInsnNode(Opcodes.ALOAD, 0)); ctorMAXS++;
        insns.add(new InsnNode(cancellable ? Opcodes.ICONST_1 : Opcodes.ICONST_0)); ctorMAXS++;
        
        if (pushReturnValue)
        {
            insns.add(new VarInsnNode(target.returnType.getOpcode(Opcodes.ILOAD), marshallVar));
            insns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, target.callInfoClass, CallbackInjector.CTOR, CallbackInfo.getConstructorDescriptor(target.returnType), false));
        }
        else
        {
            insns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, target.callInfoClass, CallbackInjector.CTOR, CallbackInfo.getConstructorDescriptor(), false));
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
    protected void injectCancellationCode(Target target, final InsnList insns, final AbstractInsnNode targetNode, int marshallVar)
    {
        insns.add(new VarInsnNode(Opcodes.ALOAD, marshallVar));
        insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, target.callInfoClass, CallbackInfo.getIsCancelledMethodName(), CallbackInfo.getIsCancelledMethodSig(), false));

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
    protected void injectReturnCode(Target target, final InsnList insns, final AbstractInsnNode targetNode, int marshallVar)
    {
        if (target.returnType.equals(Type.VOID_TYPE))
        {
            // Void method, so just return void
            insns.add(new InsnNode(Opcodes.RETURN));
        }
        else
        {
            // Non-void method, so work out which accessor to call to get the return value, and return it
            insns.add(new VarInsnNode(Opcodes.ALOAD, marshallVar));
            String accessor = CallbackInfoReturnable.getReturnAccessor(target.returnType);
            String descriptor = CallbackInfoReturnable.getReturnDescriptor(target.returnType);
            insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, target.callInfoClass, accessor, descriptor, false));
            if (target.returnType.getSort() == Type.OBJECT)
            {
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, target.returnType.getInternalName()));
            }
            insns.add(new InsnNode(target.returnType.getOpcode(Opcodes.IRETURN)));
        }
    }
}
