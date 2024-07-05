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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InjectionPoint.RestrictTargetLevel;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.code.InjectorTarget;
import org.spongepowered.asm.mixin.injection.points.BeforeFieldAccess;
import org.spongepowered.asm.mixin.injection.points.BeforeNew;
import org.spongepowered.asm.mixin.injection.struct.ArgOffsets;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.struct.Target.Extension;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.SignaturePrinter;

import com.google.common.collect.ObjectArrays;
import com.google.common.primitives.Ints;

/**
 * <p>A bytecode injector which allows a method call, field access or
 * <tt>new</tt> object creation to be redirected to the annotated handler
 * method. For method redirects, the handler method signature must match the
 * hooked method precisely <b>but</b> prepended with an arg of the owning
 * object's type to accept the object instance the method was going to be
 * invoked upon. For more details see the javadoc for the {@link Redirect
 * &#64;Redirect} annotation.</p>
 * 
 * <p>For example when hooking the following call:</p>
 * 
 * <blockquote><pre>
 *   int abc = 0;
 *   int def = 1;
 *   Foo someObject = new Foo();
 *   
 *   <del>// Hooking this method</del>
 *   boolean xyz = someObject.bar(abc, def);</pre>
 * </blockquote>
 * 
 * <p>The signature of the redirected method should be:</p>
 * 
 * <blockquote>
 *      <pre>public boolean barProxy(Foo someObject, int abc, int def)</pre>
 * </blockquote>
 * 
 * <p>For obvious reasons this does not apply for static methods, for static
 * methods it is sufficient that the signature simply match the hooked method.
 * </p> 
 * 
 * <p>For field redirections, see the details in {@link Redirect &#64;Redirect}
 * for the required signature of the handler method.</p>
 * 
 * <p>For constructor redirections, the signature of the handler method should
 * match the constructor itself, return type should be of the type of object
 * being created.</p>
 */
public class RedirectInjector extends InvokeInjector {
    
    private static final String GET_CLASS_METHOD = "getClass";
    private static final String IS_ASSIGNABLE_FROM_METHOD = "isAssignableFrom";

    private static final String NPE = "java/lang/NullPointerException";
    
    private static final String KEY_NOMINATORS = "nominators";
    private static final String KEY_FUZZ = "fuzz";
    private static final String KEY_OPCODE = "opcode";

    /**
     * Meta decoration object for redirector target nodes
     */
    class Meta {
        
        public static final String KEY = "redirector";

        final int priority;
        
        final boolean isFinal;
        
        final String name;
        
        final String desc;
        
        public Meta(int priority, boolean isFinal, String name, String desc) {
            this.priority = priority;
            this.isFinal = isFinal;
            this.name = name;
            this.desc = desc;
        }

        RedirectInjector getOwner() {
            return RedirectInjector.this;
        }
        
    }
    
    /**
     * Meta decoration for wildcard ctor redirects
     */
    static class ConstructorRedirectData {
        
        public static final String KEY = "ctor";
        
        String desc = null;
        
        boolean wildcard = false;
        
        int injected = 0;
        
        InvalidInjectionException lastException;

        public void throwOrCollect(InvalidInjectionException ex) {
            if (!this.wildcard) {
                throw ex;
            }
            this.lastException = ex;
        }
        
    }
    
    /**
     * Data bundle for invoke redirectors
     */
    static class RedirectedInvokeData extends InjectorData {
        
        final MethodInsnNode node;
        final boolean isStatic;
        final Type returnType;
        final Type[] targetArgs;
        final Type[] handlerArgs;
        
        RedirectedInvokeData(Target target, MethodInsnNode node) {
            super(target);
            this.node = node;
            this.isStatic = node.getOpcode() == Opcodes.INVOKESTATIC;
            this.returnType = Type.getReturnType(node.desc);
            this.targetArgs = Type.getArgumentTypes(node.desc);
            this.handlerArgs = this.isStatic
                    ? this.targetArgs
                    : ObjectArrays.concat(Type.getObjectType(node.owner), this.targetArgs);
        }
        
    }
    
    /**
     * Data bundle for field redirectors
     */
    static class RedirectedFieldData extends InjectorData {

        final FieldInsnNode node;
        final int opcode;
        final Type owner;
        final Type type;
        final int dimensions;
        final boolean isStatic;
        final boolean isGetter;
        final boolean isSetter;
        
        // This is actually the return type for array access, might be int for
        // array length redirectors
        Type elementType;
        int extraDimensions = 1;
        
        RedirectedFieldData(Target target, FieldInsnNode node) {
            super(target);
            this.node = node;
            this.opcode = node.getOpcode();
            this.owner = Type.getObjectType(node.owner);
            this.type = Type.getType(node.desc);
            this.dimensions = (this.type.getSort() == Type.ARRAY) ? this.type.getDimensions() : 0;
            this.isStatic = this.opcode == Opcodes.GETSTATIC || this.opcode == Opcodes.PUTSTATIC;
            this.isGetter = this.opcode == Opcodes.GETSTATIC || this.opcode == Opcodes.GETFIELD;
            this.isSetter = this.opcode == Opcodes.PUTSTATIC || this.opcode == Opcodes.PUTFIELD;
            this.description = this.isGetter ? "field getter" : this.isSetter ? "field setter" : "handler";
        }

        int getTotalDimensions() {
            return this.dimensions + this.extraDimensions;
        }

        Type[] getArrayArgs(Type... extra) {
            int dimensions = this.getTotalDimensions();
            Type[] args = new Type[dimensions + extra.length];
            for (int i = 0; i < args.length; i++) {
                args[i] = i == 0 ? this.type : i < dimensions ? Type.INT_TYPE : extra[dimensions - i];
            }
            return args;
        }

    }
    
    /**
     * Meta is used to decorate the target node with information about this
     * injection
     */
    protected Meta meta;

    private Map<BeforeNew, ConstructorRedirectData> ctorRedirectors = new HashMap<BeforeNew, ConstructorRedirectData>();
    
    /**
     * @param info Injection info
     */
    public RedirectInjector(InjectionInfo info) {
        this(info, "@Redirect");
    }
    
    protected RedirectInjector(InjectionInfo info, String annotationType) {
        super(info, annotationType);
        
        int priority = info.getMixin().getPriority();
        boolean isFinal = Annotations.getVisible(this.methodNode, Final.class) != null;
        this.meta = new Meta(priority, isFinal, this.info.toString(), this.methodNode.desc);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.invoke.InvokeInjector
     *      #checkTarget(org.spongepowered.asm.mixin.injection.struct.Target)
     */
    @Override
    protected void checkTarget(Target target) {
        // Overridden so we can do this check later in a location-aware manner
    }

    @Override
    protected void addTargetNode(InjectorTarget injectorTarget, List<InjectionNode> myNodes, AbstractInsnNode insn, Set<InjectionPoint> nominators) {
        InjectionNode node = injectorTarget.getInjectionNode(insn);
        ConstructorRedirectData ctorData = null;
        int fuzz = BeforeFieldAccess.ARRAY_SEARCH_FUZZ_DEFAULT;
        int opcode = 0;
        
        if (insn instanceof MethodInsnNode && Constants.CTOR.equals(((MethodInsnNode)insn).name)) {
            throw new InvalidInjectionException(this.info, String.format("Illegal %s of constructor specified on %s",
                    this.annotationType, this));
        }
        
        if (node != null ) {
            Meta other = node.<Meta>getDecoration(Meta.KEY);
            
            if (other != null && other.getOwner() != this) {
                if (other.priority >= this.meta.priority) {
                    Injector.logger.warn("{} conflict. Skipping {} with priority {}, already redirected by {} with priority {}",
                            this.annotationType, this.info, this.meta.priority, other.name, other.priority);
                    return;
                } else if (other.isFinal) {
                    throw new InvalidInjectionException(this.info, String.format("%s conflict: %s failed because target was already remapped by %s",
                            this.annotationType, this, other.name));
                }
            }
        }
        
        for (InjectionPoint ip : nominators) {
            if (ip instanceof BeforeNew) {
                BeforeNew beforeNew = (BeforeNew)ip;
                ctorData = this.getCtorRedirect(beforeNew);
                ctorData.wildcard = !beforeNew.hasDescriptor();
                ctorData.desc = beforeNew.getDescriptor();
            } else if (ip instanceof BeforeFieldAccess) {
                BeforeFieldAccess bfa = (BeforeFieldAccess)ip;
                fuzz = bfa.getFuzzFactor();
                opcode = bfa.getArrayOpcode();
            }
        }
        
        InjectionNode targetNode = injectorTarget.addInjectionNode(insn);
        targetNode.decorate(Meta.KEY, this.meta);
        targetNode.decorate(RedirectInjector.KEY_NOMINATORS, nominators);
        if (insn instanceof TypeInsnNode && insn.getOpcode() == Opcodes.NEW) {
            targetNode.decorate(ConstructorRedirectData.KEY, ctorData);
        } else {
            targetNode.decorate(RedirectInjector.KEY_FUZZ, Integer.valueOf(fuzz));
            targetNode.decorate(RedirectInjector.KEY_OPCODE, Integer.valueOf(opcode));
        }
        myNodes.add(targetNode);
    }

    private ConstructorRedirectData getCtorRedirect(BeforeNew ip) {
        ConstructorRedirectData ctorRedirect = this.ctorRedirectors.get(ip);
        if (ctorRedirect == null) {
            ctorRedirect = new ConstructorRedirectData();
            this.ctorRedirectors.put(ip, ctorRedirect);
        }
        return ctorRedirect;
    }

    @Override
    protected void inject(Target target, InjectionNode node) {
        if (!this.preInject(node)) {
            return;
        }
            
        if (node.isReplaced()) {
            throw new UnsupportedOperationException("Redirector target failure for " + this.info);
        }
        
        if (node.getCurrentTarget() instanceof MethodInsnNode) {
            this.checkTargetForNode(target, node, RestrictTargetLevel.ALLOW_ALL);
            this.injectAtInvoke(target, node);
            return;
        }
        
        if (node.getCurrentTarget() instanceof FieldInsnNode) {
            this.checkTargetForNode(target, node, RestrictTargetLevel.ALLOW_ALL);
            this.injectAtFieldAccess(target, node);
            return;
        }
        
        if (node.getCurrentTarget() instanceof TypeInsnNode) {
            int opcode = node.getCurrentTarget().getOpcode();
            if (opcode == Opcodes.NEW) {
                if (!this.isStatic && target.isStatic) {
                    throw new InvalidInjectionException(this.info, String.format(
                            "non-static callback method %s has a static target which is not supported", this));
                }
                this.injectAtConstructor(target, node);
                return;
            } else if (opcode == Opcodes.INSTANCEOF) {
                this.checkTargetModifiers(target, false);
                this.injectAtInstanceOf(target, node);
                return;
            }
        }
        
        throw new InvalidInjectionException(this.info, String.format("%s annotation on is targetting an invalid insn in %s in %s",
                this.annotationType, target, this));
    }

    protected boolean preInject(InjectionNode node) {
        Meta other = node.<Meta>getDecoration(Meta.KEY);
        if (other.getOwner() != this) {
            Injector.logger.warn("{} conflict. Skipping {} with priority {}, already redirected by {} with priority {}",
                    this.annotationType, this.info, this.meta.priority, other.name, other.priority);
            return false;
        }
        return true;
    }
    
    @Override
    protected void postInject(Target target, InjectionNode node) {
        super.postInject(target, node);
        if (node.getOriginalTarget() instanceof TypeInsnNode && node.getOriginalTarget().getOpcode() == Opcodes.NEW) {
            ConstructorRedirectData meta = node.<ConstructorRedirectData>getDecoration(ConstructorRedirectData.KEY);
            if (meta.wildcard && meta.injected == 0) {
                throw new InvalidInjectionException(this.info, String.format("%s ctor invocation was not found in %s", this.annotationType, target),
                        meta.lastException);
            }
        }
    }
    
    /**
     * Redirect a method invocation
     */
    @Override
    protected void injectAtInvoke(Target target, InjectionNode node) {
        RedirectedInvokeData invoke = new RedirectedInvokeData(target, (MethodInsnNode)node.getCurrentTarget());
        
        this.validateParams(invoke, invoke.returnType, invoke.handlerArgs);
        
        InsnList insns = new InsnList();
        Extension extraLocals = target.extendLocals().add(invoke.handlerArgs).add(1);
        Extension extraStack = target.extendStack().add(1); // Normally only need 1 extra stack pos to store target ref 
        int[] argMap = this.storeArgs(target, invoke.handlerArgs, insns, 0);
        ArgOffsets offsets = new ArgOffsets(invoke.isStatic ? 0 : 1, invoke.targetArgs.length);
        if (invoke.captureTargetArgs > 0) {
            int argSize = Bytecode.getArgsSize(target.arguments, 0, invoke.captureTargetArgs);
            extraLocals.add(argSize);
            extraStack.add(argSize);
            // No need to truncate target arg indices, pushArgs ignores args which don't exist
            argMap = Ints.concat(argMap, target.getArgIndices());
        }
        AbstractInsnNode champion = this.invokeHandlerWithArgs(this.methodArgs, insns, argMap);
        if (invoke.coerceReturnType && invoke.returnType.getSort() >= Type.ARRAY) {
            insns.add(new TypeInsnNode(Opcodes.CHECKCAST, invoke.returnType.getInternalName()));
        }
        target.replaceNode(invoke.node, champion, insns);
        node.decorate(ArgOffsets.KEY, offsets);
        extraLocals.apply();
        extraStack.apply();
    }

    /**
     * Redirect a field get or set operation, or an array element access
     */
    private void injectAtFieldAccess(Target target, InjectionNode node) {
        RedirectedFieldData field = new RedirectedFieldData(target, (FieldInsnNode)node.getCurrentTarget());
        
        int handlerDimensions = (this.returnType.getSort() == Type.ARRAY) ? this.returnType.getDimensions() : 0;
        
        if (handlerDimensions > field.dimensions) {
            throw new InvalidInjectionException(this.info, "Dimensionality of handler method is greater than target array on " + this);
        } else if (handlerDimensions == 0 && field.dimensions > 0) {
            int fuzz = node.<Integer>getDecoration(RedirectInjector.KEY_FUZZ).intValue();
            int opcode = node.<Integer>getDecoration(RedirectInjector.KEY_OPCODE).intValue();
            this.injectAtArrayField(field, fuzz, opcode);
        } else {
            this.injectAtScalarField(field);
        }
    }

    /**
     * Redirect an array element access
     */
    private void injectAtArrayField(RedirectedFieldData field, int fuzz, int opcode) {
        Type elementType = field.type.getElementType();
        if (field.opcode != Opcodes.GETSTATIC && field.opcode != Opcodes.GETFIELD) {
            throw new InvalidInjectionException(this.info, String.format("Unspported opcode %s for array access %s",
                    Bytecode.getOpcodeName(field.opcode), this.info));
        } else if (this.returnType.getSort() != Type.VOID) {
            if (opcode != Opcodes.ARRAYLENGTH) {
                opcode = elementType.getOpcode(Opcodes.IALOAD);
            }
            AbstractInsnNode varNode = BeforeFieldAccess.findArrayNode(field.target.insns, field.node, opcode, fuzz);
            this.injectAtGetArray(field, varNode);
        } else {
            AbstractInsnNode varNode = BeforeFieldAccess.findArrayNode(field.target.insns, field.node, elementType.getOpcode(Opcodes.IASTORE), fuzz);
            this.injectAtSetArray(field, varNode);
        }
    }
    
    /**
     * Array element read (xALOAD) or array.length (ARRAYLENGTH)
     */
    private void injectAtGetArray(RedirectedFieldData field, AbstractInsnNode varNode) {
        field.description = "array getter";
        field.elementType = field.type.getElementType();
        
        if (varNode != null && varNode.getOpcode() == Opcodes.ARRAYLENGTH) {
            field.elementType = Type.INT_TYPE;
            field.extraDimensions = 0;
        }
        
        this.validateParams(field, field.elementType, field.getArrayArgs());
        this.injectArrayRedirect(field, varNode, "array getter");
    }

    /**
     * Array element write (xASTORE)
     */
    private void injectAtSetArray(RedirectedFieldData field, AbstractInsnNode varNode) {
        field.description = "array setter";
        Type elementType = field.type.getElementType();
        int valueArgIndex = field.getTotalDimensions();
        if (this.checkCoerce(valueArgIndex, elementType, String.format("%s array setter method %s from %s",
                this.annotationType, this, this.info.getMixin()), true)) {
            elementType = this.methodArgs[valueArgIndex];
        }
        
        this.validateParams(field, Type.VOID_TYPE, field.getArrayArgs(elementType));
        this.injectArrayRedirect(field, varNode, "array setter");
    }

    /**
     * The code for actually redirecting the array element is the same
     * regardless of whether it's a read or write because it just depends on the
     * actual handler signature, the correct arguments are already on the stack
     * thanks to the nature of xALOAD and xASTORE.
     * 
     * @param varNode array access node
     * @param type description of access type for use in error messages
     * @param target target method
     * @param fieldNode field node
     */
    private void injectArrayRedirect(RedirectedFieldData field, AbstractInsnNode varNode, String type) {
        if (varNode == null) {
            String advice = "";
            throw new InvalidInjectionException(this.info, String.format(
                    "Array element %s on %s could not locate a matching %s instruction in %s. %s",
                    this.annotationType, this, type, field.target, advice));
        }
        
        Extension extraStack = field.target.extendStack();
        
        if (!this.isStatic) {
            VarInsnNode loadThis = new VarInsnNode(Opcodes.ALOAD, 0);
            field.target.insns.insert(field.node, loadThis);
            field.target.insns.insert(loadThis, new InsnNode(Opcodes.SWAP));
            extraStack.add();
        }
        
        InsnList insns = new InsnList();
        if (field.captureTargetArgs > 0) {
            this.pushArgs(field.target.arguments, insns, field.target.getArgIndices(), 0, field.captureTargetArgs, extraStack);
        }
        extraStack.apply();
        AbstractInsnNode champion = this.invokeHandler(insns);
        if (field.coerceReturnType && field.type.getSort() >= Type.ARRAY) {
            insns.add(new TypeInsnNode(Opcodes.CHECKCAST, field.elementType.getInternalName()));
        }
        field.target.replaceNode(varNode, champion, insns);
    }

    /**
     * Redirect a field get or set
     * 
     * @param target target method
     * @param fieldNode field access node
     * @param opCode field access type
     * @param ownerType type of the field owner
     * @param fieldType field type
     */
    private void injectAtScalarField(RedirectedFieldData field) {
        AbstractInsnNode invoke = null;
        InsnList insns = new InsnList();
        if (field.isGetter) {
            invoke = this.injectAtGetField(field, insns);
        } else if (field.isSetter) {
            invoke = this.injectAtPutField(field, insns);
        } else {
            throw new InvalidInjectionException(this.info, String.format("Unspported opcode %s for %s",
                    Bytecode.getOpcodeName(field.opcode), this.info));
        }
        
        field.target.replaceNode(field.node, invoke, insns);
    }

    /**
     * Inject opcodes to redirect a field getter. The injection will vary based
     * on the staticness of the field and the handler thus there are four
     * possible scenarios based on the possible combinations of static on the
     * handler and the field itself.
     */
    private AbstractInsnNode injectAtGetField(RedirectedFieldData field, InsnList insns) {
        this.validateParams(field, field.type, field.isStatic ? null : field.owner);

        Extension extraStack = field.target.extendStack();
        
        if (!this.isStatic) {
            extraStack.add();
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            if (!field.isStatic) {
                insns.add(new InsnNode(Opcodes.SWAP));
            }
        }
        
        if (field.captureTargetArgs > 0) {
            this.pushArgs(field.target.arguments, insns, field.target.getArgIndices(), 0, field.captureTargetArgs, extraStack);
        }
        
        extraStack.apply();
        AbstractInsnNode champion = this.invokeHandler(insns);
        if (field.coerceReturnType && field.type.getSort() >= Type.ARRAY) {
            insns.add(new TypeInsnNode(Opcodes.CHECKCAST, field.type.getInternalName()));
        }
        return champion;
    }

    /**
     * Inject opcodes to redirect a field setter. The injection will vary based
     * on the staticness of the field and the handler thus there are four
     * possible scenarios based on the possible combinations of static on the
     * handler and the field itself.
     */
    private AbstractInsnNode injectAtPutField(RedirectedFieldData field, InsnList insns) {
        this.validateParams(field, Type.VOID_TYPE, field.isStatic ? null : field.owner, field.type);

        Extension extraStack = field.target.extendStack();
        
        if (!this.isStatic) {
            if (field.isStatic) {
                insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insns.add(new InsnNode(Opcodes.SWAP));
            } else {
                extraStack.add();
                int marshallVar = field.target.allocateLocals(field.type.getSize());
                insns.add(new VarInsnNode(field.type.getOpcode(Opcodes.ISTORE), marshallVar));
                insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insns.add(new InsnNode(Opcodes.SWAP));
                insns.add(new VarInsnNode(field.type.getOpcode(Opcodes.ILOAD), marshallVar));
            }
        }
        
        if (field.captureTargetArgs > 0) {
            this.pushArgs(field.target.arguments, insns, field.target.getArgIndices(), 0, field.captureTargetArgs, extraStack);
        }
        
        extraStack.apply();
        return this.invokeHandler(insns);
    }

    protected void injectAtConstructor(Target target, InjectionNode node) {
        ConstructorRedirectData meta = node.<ConstructorRedirectData>getDecoration(ConstructorRedirectData.KEY);
        
        if (meta == null) {
            // This should never happen, but let's display a less obscure error if it does
            throw new InvalidInjectionException(this.info, String.format(
                    "%s ctor redirector has no metadata, the injector failed a preprocessing phase", this.annotationType));
        }
        
        final TypeInsnNode newNode = (TypeInsnNode)node.getCurrentTarget();
        final AbstractInsnNode dupNode = target.get(target.indexOf(newNode) + 1);
        final MethodInsnNode initNode = target.findInitNodeFor(newNode, meta.desc);
        
        if (initNode == null) {
            meta.throwOrCollect(new InvalidInjectionException(this.info, String.format("%s ctor invocation was not found in %s",
                    this.annotationType, target)));
            return;
        }
        
        // True if the result of the object construction is being assigned
        boolean isAssigned = dupNode.getOpcode() == Opcodes.DUP;
        RedirectedInvokeData ctor = new RedirectedInvokeData(target, initNode);
        ctor.description = "factory";
        try {
            this.validateParams(ctor, Type.getObjectType(newNode.desc), ctor.targetArgs);
        } catch (InvalidInjectionException ex) {
            meta.throwOrCollect(ex);
            return;
        }
        
        if (isAssigned) {
            target.removeNode(dupNode);
        }
        
        if (this.isStatic) {
            target.removeNode(newNode);
        } else {
            target.replaceNode(newNode, new VarInsnNode(Opcodes.ALOAD, 0));
        }
        
        Extension extraStack = target.extendStack();
        InsnList insns = new InsnList();
        if (ctor.captureTargetArgs > 0) {
            this.pushArgs(target.arguments, insns, target.getArgIndices(), 0, ctor.captureTargetArgs, extraStack);
        }
        
        this.invokeHandler(insns);
        if (ctor.coerceReturnType) {
            insns.add(new TypeInsnNode(Opcodes.CHECKCAST, newNode.desc));
        }
        extraStack.apply();
        
        if (isAssigned) {
            // Do a null-check following the redirect to ensure that the handler
            // didn't return null. Since NEW cannot return null, this would break
            // the contract of the target method!
            this.doNullCheck(insns, extraStack, "constructor handler", newNode.desc.replace('/', '.'));
        } else {
            // Result is not assigned, so just pop it from the operand stack
            insns.add(new InsnNode(Opcodes.POP));
        }
        
        extraStack.apply();
        target.replaceNode(initNode, insns);
        meta.injected++;
    }

    protected void injectAtInstanceOf(Target target, InjectionNode node) {
        this.injectAtInstanceOf(target, (TypeInsnNode)node.getCurrentTarget());
    }

    protected void injectAtInstanceOf(Target target, TypeInsnNode typeNode) {
        if (this.returnType.getSort() == Type.BOOLEAN) {
            this.redirectInstanceOf(target, typeNode, false);
            return;
        }
        
        if (this.returnType.equals(Type.getType(Constants.CLASS_DESC))) {
            this.redirectInstanceOf(target, typeNode, true);
            return;
        }
        
        // This syntax is neat but the inconsistency might be a step too far
//        if (this.returnType.getSort() >= Type.ARRAY) {
//            this.modifyInstanceOfType(target, typeNode);
//            return;
//        }
        
        throw new InvalidInjectionException(this.info, String.format("%s on %s has an invalid signature. Found unexpected return type %s. INSTANCEOF"
                + " handler expects (Ljava/lang/Object;Ljava/lang/Class;)Z or (Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Class;",
                this.annotationType, this, SignaturePrinter.getTypeName(this.returnType)));
    }

    private void redirectInstanceOf(Target target, TypeInsnNode typeNode, boolean dynamic) {
        Extension extraStack = target.extendStack();
        final InsnList insns = new InsnList();
        InjectorData handler = new InjectorData(target, "instanceof handler", false /* do not coerce args */);
        this.validateParams(handler, this.returnType, Type.getType(Constants.OBJECT_DESC), Type.getType(Constants.CLASS_DESC));
        
        if (dynamic) {
            insns.add(new InsnNode(Opcodes.DUP));
            extraStack.add();
        }

        if (!this.isStatic) {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            insns.add(new InsnNode(Opcodes.SWAP));
            extraStack.add();
        }
        
        // Add the class type from the original instanceof check
        insns.add(new LdcInsnNode(Type.getObjectType(typeNode.desc)));
        extraStack.add();
        
        if (handler.captureTargetArgs > 0) {
            this.pushArgs(target.arguments, insns, target.getArgIndices(), 0, handler.captureTargetArgs, extraStack);
        }
        
        AbstractInsnNode champion = this.invokeHandler(insns);
        
        if (dynamic) {
            // First null-check the class value returned by the handler, if it's
            // null then the rest is going to go badly
            this.doNullCheck(insns, extraStack, "instanceof handler", "class type");
            
            // Now do a null-check on the reference and isAssignableFrom check
            this.checkIsAssignableFrom(insns, extraStack);
        }
        
        target.replaceNode(typeNode, champion, insns);
        extraStack.apply();
    }

    private void checkIsAssignableFrom(final InsnList insns, Extension extraStack) {
        LabelNode objectIsNull = new LabelNode();
        LabelNode checkComplete = new LabelNode();
        
        // Swap the values (we duped the ref above) and check for null. If
        // the reference is null, load FALSE per the contract of instanceof
        insns.add(new InsnNode(Opcodes.SWAP));
        insns.add(new InsnNode(Opcodes.DUP));
        extraStack.add();
        insns.add(new JumpInsnNode(Opcodes.IFNULL, objectIsNull));
        // If it's not null, call getClass on the reference and then use
        // isAssignableFrom on the result
        insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Constants.OBJECT, RedirectInjector.GET_CLASS_METHOD,
                "()" + Constants.CLASS_DESC, false));
        insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Constants.CLASS, RedirectInjector.IS_ASSIGNABLE_FROM_METHOD,
                "(" + Constants.CLASS_DESC + ")Z", false));
        insns.add(new JumpInsnNode(Opcodes.GOTO, checkComplete));
        
        insns.add(objectIsNull);
        insns.add(new InsnNode(Opcodes.POP)); // remove ref
        insns.add(new InsnNode(Opcodes.POP)); // remove class
        insns.add(new InsnNode(Opcodes.ICONST_0));
        insns.add(checkComplete);
        extraStack.add();
    }

//    private void modifyInstanceOfType(Target target, TypeInsnNode typeNode) {
//        if (this.methodArgs.length > 0) {
//            throw new InvalidInjectionException(this.info, String.format("%s on %s has an invalid signature. Found %d unexpected additional method"
//                    + "arguments, expected 0. INSTANCEOF handler expects ()Lthe/replacement/Type; or (Ljava/lang/Object;Ljava/lang/Class;)Z",
//                    this.annotationType, this, this.methodArgs.length));
//        }
//
//        // Already know that returnType is an object or array so no need to check again
//        typeNode.desc = this.returnType.getInternalName();
//        this.info.addCallbackInvocation(this.methodNode);
//    }

    private void doNullCheck(InsnList insns, Extension extraStack, String type, String value) {
        LabelNode nullCheckSucceeded = new LabelNode();
        insns.add(new InsnNode(Opcodes.DUP));
        insns.add(new JumpInsnNode(Opcodes.IFNONNULL, nullCheckSucceeded));
        this.throwException(insns, extraStack, RedirectInjector.NPE, String.format("%s %s %s returned null for %s",
                this.annotationType, type, this, value));
        insns.add(nullCheckSucceeded);
        extraStack.add();
    }
    
}
