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

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.points.BeforeFieldAccess;
import org.spongepowered.asm.mixin.injection.points.BeforeNew;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;

import com.google.common.base.Joiner;
import com.google.common.collect.ObjectArrays;
import com.google.common.primitives.Ints;

/**
 * <p>A bytecode injector which allows a method call, field access or
 * <tt>new</tt> object creation to be redirected to the annotated handler
 * method. For method redirects, the handler method signature must match the
 * hooked method precisely <b>but</b> prepended with an arg of the owning
 * object's type to accept the object instance the method was going to be
 * invoked upon. For example when hooking the following call:</p>
 * 
 * <blockquote><pre>
 *   int abc = 0;
 *   int def = 1;
 *   Foo someObject = new Foo();
 *   
 *   // Hooking this method
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
 * <p>For field redirections, see the details in {@link Redirect} for the
 * required signature of the handler method.</p>
 * 
 * <p>For constructor redirections, the signature of the handler method should
 * match the constructor itself, return type should be of the type of object
 * being created.</p>
 */
public class RedirectInjector extends InvokeInjector {
    
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
        
        public boolean wildcard = false;
        
        public int injected = 0;
        
    }
    
    /**
     * Data bundle for invoke redirectors
     */
    static class RedirectedInvoke {
        
        final Target target;
        final MethodInsnNode node;
        final Type returnType;
        final Type[] args;
        final Type[] locals;
        
        boolean captureTargetArgs = false;
        
        RedirectedInvoke(Target target, MethodInsnNode node) {
            this.target = target;
            this.node = node;
            this.returnType = Type.getReturnType(node.desc);
            this.args = Type.getArgumentTypes(node.desc);
            this.locals = node.getOpcode() == Opcodes.INVOKESTATIC
                    ? this.args
                    : ObjectArrays.concat(Type.getType("L" + node.owner + ";"), this.args);
        }
    }
    
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
        
        int priority = info.getContext().getPriority();
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
    protected void addTargetNode(Target target, List<InjectionNode> myNodes, AbstractInsnNode insn, Set<InjectionPoint> nominators) {
        InjectionNode node = target.getInjectionNode(insn);
        ConstructorRedirectData ctorData = null;
        int fuzz = BeforeFieldAccess.ARRAY_SEARCH_FUZZ_DEFAULT;
        int opcode = 0;
        
        if (node != null ) {
            Meta other = node.getDecoration(Meta.KEY);
            
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
                ctorData = this.getCtorRedirect((BeforeNew)ip);
                ctorData.wildcard = !((BeforeNew)ip).hasDescriptor();
            } else if (ip instanceof BeforeFieldAccess) {
                BeforeFieldAccess bfa = (BeforeFieldAccess)ip;
                fuzz = bfa.getFuzzFactor();
                opcode = bfa.getArrayOpcode();
            }
        }
        
        InjectionNode targetNode = target.addInjectionNode(insn);
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
            this.checkTargetForNode(target, node);
            this.injectAtInvoke(target, node);
            return;
        }
        
        if (node.getCurrentTarget() instanceof FieldInsnNode) {
            this.checkTargetForNode(target, node);
            this.injectAtFieldAccess(target, node);
            return;
        }
        
        if (node.getCurrentTarget() instanceof TypeInsnNode && node.getCurrentTarget().getOpcode() == Opcodes.NEW) {
            if (!this.isStatic && target.isStatic) {
                throw new InvalidInjectionException(this.info, String.format(
                        "non-static callback method %s has a static target which is not supported", this));
            }
            this.injectAtConstructor(target, node);
            return;
        }
        
        throw new InvalidInjectionException(this.info, String.format("%s annotation on is targetting an invalid insn in %s in %s",
                this.annotationType, target, this));
    }

    protected boolean preInject(InjectionNode node) {
        Meta other = node.getDecoration(Meta.KEY);
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
                throw new InvalidInjectionException(this.info, String.format("%s ctor invocation was not found in %s", this.annotationType, target));
            }
        }
    }
    
    /**
     * Redirect a method invocation
     */
    @Override
    protected void injectAtInvoke(Target target, InjectionNode node) {
        RedirectedInvoke invoke = new RedirectedInvoke(target, (MethodInsnNode)node.getCurrentTarget());
        
        this.validateParams(invoke);
        
        InsnList insns = new InsnList();
        int extraLocals = Bytecode.getArgsSize(invoke.locals) + 1;
        int extraStack = 1; // Normally only need 1 extra stack pos to store target ref 
        int[] argMap = this.storeArgs(target, invoke.locals, insns, 0);
        if (invoke.captureTargetArgs) {
            int argSize = Bytecode.getArgsSize(target.arguments);
            extraLocals += argSize;
            extraStack += argSize;
            argMap = Ints.concat(argMap, target.getArgIndices());
        }
        AbstractInsnNode insn = this.invokeHandlerWithArgs(this.methodArgs, insns, argMap);
        target.replaceNode(invoke.node, insn, insns);
        target.addToLocals(extraLocals);
        target.addToStack(extraStack);
    }

    /**
     * Perform validation of an invoke handler parameters, each parameter in the
     * handler must match the expected type or be annotated with {@link Coerce}
     * and be a supported supertype of the incoming type.
     * 
     * @param invoke invocation being redirected
     */
    protected void validateParams(RedirectedInvoke invoke) {
        int argc = this.methodArgs.length;
        
        String description = String.format("%s handler method %s", this.annotationType, this);
        if (!invoke.returnType.equals(this.returnType)) {
            throw new InvalidInjectionException(this.info, String.format("%s has an invalid signature. Expected return type %s found %s",
                    description, this.returnType, invoke.returnType));
        }
        
        for (int index = 0; index < argc; index++) {
            Type toType = null;
            if (index >= this.methodArgs.length) {
                throw new InvalidInjectionException(this.info, String.format(
                        "%s has an invalid signature. Not enough arguments found for capture of target method args, expected %d but found %d",
                        description, argc, this.methodArgs.length));
            }
            
            Type fromType = this.methodArgs[index];
            
            if (index < invoke.locals.length) {
                toType = invoke.locals[index];
            } else {
                invoke.captureTargetArgs = true;
                argc = Math.max(argc, invoke.locals.length + invoke.target.arguments.length);
                int arg = index - invoke.locals.length;
                if (arg >= invoke.target.arguments.length) {
                    throw new InvalidInjectionException(this.info, String.format( 
                            "%s has an invalid signature. Found unexpected additional target argument with type %s at index %d",
                            description, fromType, index));
                }
                toType = invoke.target.arguments[arg];
            }
            
            AnnotationNode coerce = Annotations.getInvisibleParameter(this.methodNode, Coerce.class, index);
            
            if (fromType.equals(toType)) {
                if (coerce != null && this.info.getContext().getOption(Option.DEBUG_VERBOSE)) {
                    Injector.logger.warn("Redundant @Coerce on {} argument {}, {} is identical to {}", description, index, toType, fromType);
                }
                
                continue;
            }
            
            boolean canCoerce = Injector.canCoerce(fromType, toType);
            if (coerce == null) {
                throw new InvalidInjectionException(this.info, String.format(
                        "%s has an invalid signature. Found unexpected argument type %s at index %d, expected %s",
                        description, fromType, index, toType));
            }
            
            if (!canCoerce) {
                throw new InvalidInjectionException(this.info, String.format(
                        "%s has an invalid signature. Cannot @Coerce argument type %s at index %d to %s",
                        description, toType, index, fromType));
            }
        }
    }

    /**
     * Redirect a field get or set operation, or an array element access
     */
    private void injectAtFieldAccess(Target target, InjectionNode node) {
        final FieldInsnNode fieldNode = (FieldInsnNode)node.getCurrentTarget();
        final int opCode = fieldNode.getOpcode();
        final Type ownerType = Type.getType("L" + fieldNode.owner + ";");
        final Type fieldType = Type.getType(fieldNode.desc);
        
        int targetDimensions = (fieldType.getSort() == Type.ARRAY) ? fieldType.getDimensions() : 0;
        int handlerDimensions = (this.returnType.getSort() == Type.ARRAY) ? this.returnType.getDimensions() : 0;
        
        if (handlerDimensions > targetDimensions) {
            throw new InvalidInjectionException(this.info, "Dimensionality of handler method is greater than target array on " + this);
        } else if (handlerDimensions == 0 && targetDimensions > 0) {
            int fuzz = node.<Integer>getDecoration(RedirectInjector.KEY_FUZZ).intValue();
            int opcode = node.<Integer>getDecoration(RedirectInjector.KEY_OPCODE).intValue();
            this.injectAtArrayField(target, fieldNode, opCode, ownerType, fieldType, fuzz, opcode);
        } else {
            this.injectAtScalarField(target, fieldNode, opCode, ownerType, fieldType);
        }
    }

    /**
     * Redirect an array element access
     */
    private void injectAtArrayField(Target target, FieldInsnNode fieldNode, int opCode, Type ownerType, Type fieldType, int fuzz, int opcode) {
        Type elementType = fieldType.getElementType();
        if (opCode != Opcodes.GETSTATIC && opCode != Opcodes.GETFIELD) {
            throw new InvalidInjectionException(this.info, String.format("Unspported opcode %s for array access %s",
                    Bytecode.getOpcodeName(opCode), this.info));
        } else if (this.returnType.getSort() != Type.VOID) {
            if (opcode != Opcodes.ARRAYLENGTH) {
                opcode = elementType.getOpcode(Opcodes.IALOAD);
            }
            AbstractInsnNode varNode = BeforeFieldAccess.findArrayNode(target.insns, fieldNode, opcode, fuzz);
            this.injectAtGetArray(target, fieldNode, varNode, ownerType, fieldType);
        } else {
            AbstractInsnNode varNode = BeforeFieldAccess.findArrayNode(target.insns, fieldNode, elementType.getOpcode(Opcodes.IASTORE), fuzz);
            this.injectAtSetArray(target, fieldNode, varNode, ownerType, fieldType);
        }
    }
    
    /**
     * Array element read (xALOAD) or array.length (ARRAYLENGTH)
     */
    private void injectAtGetArray(Target target, FieldInsnNode fieldNode, AbstractInsnNode varNode, Type ownerType, Type fieldType) {
        String handlerDesc = RedirectInjector.getGetArrayHandlerDescriptor(varNode, this.returnType, fieldType);
        boolean withArgs = this.checkDescriptor(handlerDesc, target, "array getter");
        this.injectArrayRedirect(target, fieldNode, varNode, withArgs, "array getter");
    }

    /**
     * Array element write (xASTORE)
     */
    private void injectAtSetArray(Target target, FieldInsnNode fieldNode, AbstractInsnNode varNode, Type ownerType, Type fieldType) {
        String handlerDesc = Bytecode.generateDescriptor(null, (Object[])RedirectInjector.getArrayArgs(fieldType, 1, fieldType.getElementType()));
        boolean withArgs = this.checkDescriptor(handlerDesc, target, "array setter");
        this.injectArrayRedirect(target, fieldNode, varNode, withArgs, "array setter");
    }

    /**
     * The code for actually redirecting the array element is the same
     * regardless of whether it's a read or write because it just depends on the
     * actual handler signature, the correct arguments are already on the stack
     * thanks to the nature of xALOAD and xASTORE.
     * 
     * @param target target method
     * @param fieldNode field node
     * @param varNode array access node
     * @param withArgs true if the descriptor includes captured arguments from
     *      the target method signature
     * @param type description of access type for use in error messages
     */
    public void injectArrayRedirect(Target target, FieldInsnNode fieldNode, AbstractInsnNode varNode, boolean withArgs, String type) {
        if (varNode == null) {
            String advice = "";
            throw new InvalidInjectionException(this.info, String.format(
                    "Array element %s on %s could not locate a matching %s instruction in %s. %s",
                    this.annotationType, this, type, target, advice));
        }
        
        if (!this.isStatic) {
            target.insns.insertBefore(fieldNode, new VarInsnNode(Opcodes.ALOAD, 0));
            target.addToStack(1);
        }
        
        InsnList invokeInsns = new InsnList();
        if (withArgs) {
            this.pushArgs(target.arguments, invokeInsns, target.getArgIndices(), 0, target.arguments.length);
            target.addToStack(Bytecode.getArgsSize(target.arguments));
        }
        target.replaceNode(varNode, this.invokeHandler(invokeInsns), invokeInsns);
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
    public void injectAtScalarField(Target target, final FieldInsnNode fieldNode, int opCode, Type ownerType, Type fieldType) {
        AbstractInsnNode invoke = null;
        InsnList insns = new InsnList();
        if (opCode == Opcodes.GETSTATIC || opCode == Opcodes.GETFIELD) {
            invoke = this.injectAtGetField(insns, target, fieldNode, opCode == Opcodes.GETSTATIC, ownerType, fieldType);
        } else if (opCode == Opcodes.PUTSTATIC || opCode == Opcodes.PUTFIELD) {
            invoke = this.injectAtPutField(insns, target, fieldNode, opCode == Opcodes.PUTSTATIC, ownerType, fieldType);
        } else {
            throw new InvalidInjectionException(this.info, String.format("Unspported opcode %s for %s", Bytecode.getOpcodeName(opCode), this.info));
        }
        
        target.replaceNode(fieldNode, invoke, insns);
    }

    /**
     * Inject opcodes to redirect a field getter. The injection will vary based
     * on the staticness of the field and the handler thus there are four
     * possible scenarios based on the possible combinations of static on the
     * handler and the field itself.
     */
    private AbstractInsnNode injectAtGetField(InsnList insns, Target target, FieldInsnNode node, boolean staticField, Type owner, Type fieldType) {
        final String handlerDesc = staticField ? Bytecode.generateDescriptor(fieldType) : Bytecode.generateDescriptor(fieldType, owner);
        final boolean withArgs = this.checkDescriptor(handlerDesc, target, "getter");

        if (!this.isStatic) {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            if (!staticField) {
                insns.add(new InsnNode(Opcodes.SWAP));
            }
        }
        
        if (withArgs) {
            this.pushArgs(target.arguments, insns, target.getArgIndices(), 0, target.arguments.length);
            target.addToStack(Bytecode.getArgsSize(target.arguments));
        }
        
        target.addToStack(this.isStatic ? 0 : 1);
        return this.invokeHandler(insns);
    }

    /**
     * Inject opcodes to redirect a field setter. The injection will vary based
     * on the staticness of the field and the handler thus there are four
     * possible scenarios based on the possible combinations of static on the
     * handler and the field itself.
     */
    private AbstractInsnNode injectAtPutField(InsnList insns, Target target, FieldInsnNode node, boolean staticField, Type owner, Type fieldType) {
        String handlerDesc = staticField ? Bytecode.generateDescriptor(null, fieldType) : Bytecode.generateDescriptor(null, owner, fieldType);
        boolean withArgs = this.checkDescriptor(handlerDesc, target, "setter");

        if (!this.isStatic) {
            if (staticField) {
                insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insns.add(new InsnNode(Opcodes.SWAP));
            } else {
                int marshallVar = target.allocateLocals(fieldType.getSize());
                insns.add(new VarInsnNode(fieldType.getOpcode(Opcodes.ISTORE), marshallVar));
                insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insns.add(new InsnNode(Opcodes.SWAP));
                insns.add(new VarInsnNode(fieldType.getOpcode(Opcodes.ILOAD), marshallVar));
            }
        }
        
        if (withArgs) {
            this.pushArgs(target.arguments, insns, target.getArgIndices(), 0, target.arguments.length);
            target.addToStack(Bytecode.getArgsSize(target.arguments));
        }
        
        target.addToStack(!this.isStatic && !staticField ? 1 : 0);
        return this.invokeHandler(insns);
    }

    /**
     * Check that the handler descriptor matches the calculated descriptor for
     * the access being redirected.
     * 
     * @param desc computed descriptor
     * @param target target method
     * @param type redirector type in human-readable text, for error messages
     * @return true if the descriptor was found and includes target method args,
     *      false if the descriptor was found and does not capture target args
     */
    protected boolean checkDescriptor(String desc, Target target, String type) {
        if (this.methodNode.desc.equals(desc)) {
            return false;
        }
        
        int pos = desc.indexOf(')');
        String alternateDesc = String.format("%s%s%s", desc.substring(0, pos), Joiner.on("").join(target.arguments), desc.substring(pos));
        if (this.methodNode.desc.equals(alternateDesc)) {
            return true;
        }
        
        throw new InvalidInjectionException(this.info, String.format("%s method %s %s has an invalid signature. Expected %s but found %s",
                this.annotationType, type, this, desc, this.methodNode.desc));
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
        final MethodInsnNode initNode = target.findInitNodeFor(newNode);
        
        if (initNode == null) {
            if (!meta.wildcard) {
                throw new InvalidInjectionException(this.info, String.format("%s ctor invocation was not found in %s", this.annotationType, target));
            }
            return;
        }
        
        // True if the result of the object construction is being assigned
        boolean isAssigned = dupNode.getOpcode() == Opcodes.DUP;
        String desc = initNode.desc.replace(")V", ")L" + newNode.desc + ";");
        boolean withArgs = false;
        try {
            withArgs = this.checkDescriptor(desc, target, "constructor");
        } catch (InvalidInjectionException ex) {
            if (!meta.wildcard) {
                throw ex;
            }
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
        
        InsnList insns = new InsnList();
        if (withArgs) {
            this.pushArgs(target.arguments, insns, target.getArgIndices(), 0, target.arguments.length);
            target.addToStack(Bytecode.getArgsSize(target.arguments));
        }
        
        this.invokeHandler(insns);
        
        if (isAssigned) {
            // Do a null-check following the redirect to ensure that the handler
            // didn't return null. Since NEW cannot return null, this would break
            // the contract of the target method!
            LabelNode nullCheckSucceeded = new LabelNode();
            insns.add(new InsnNode(Opcodes.DUP));
            insns.add(new JumpInsnNode(Opcodes.IFNONNULL, nullCheckSucceeded));
            this.throwException(insns, "java/lang/NullPointerException", String.format("%s constructor handler %s returned null for %s",
                    this.annotationType, this, newNode.desc.replace('/', '.')));
            insns.add(nullCheckSucceeded);
            target.addToStack(1);
        } else {
            // Result is not assigned, so just pop it from the operand stack
            insns.add(new InsnNode(Opcodes.POP));
        }
        
        target.replaceNode(initNode, insns);
        meta.injected++;
    }

    private static String getGetArrayHandlerDescriptor(AbstractInsnNode varNode, Type returnType, Type fieldType) {
        if (varNode != null && varNode.getOpcode() == Opcodes.ARRAYLENGTH) {
            return Bytecode.generateDescriptor(Type.INT_TYPE, (Object[])RedirectInjector.getArrayArgs(fieldType, 0));
        }
        return Bytecode.generateDescriptor(returnType, (Object[])RedirectInjector.getArrayArgs(fieldType, 1));
    }

    private static Type[] getArrayArgs(Type fieldType, int extraDimensions, Type... extra) {
        int dimensions = fieldType.getDimensions() + extraDimensions;
        Type[] args = new Type[dimensions + extra.length];
        for (int i = 0; i < args.length; i++) {
            args[i] = i == 0 ? fieldType : i < dimensions ? Type.INT_TYPE : extra[dimensions - i];
        }
        return args;
    }

}
