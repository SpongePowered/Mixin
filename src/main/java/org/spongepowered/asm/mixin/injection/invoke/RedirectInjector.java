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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.FieldInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.InsnNode;
import org.spongepowered.asm.lib.tree.JumpInsnNode;
import org.spongepowered.asm.lib.tree.LabelNode;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.TypeInsnNode;
import org.spongepowered.asm.lib.tree.VarInsnNode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.points.BeforeFieldAccess;
import org.spongepowered.asm.mixin.injection.points.BeforeNew;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Constants;

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
    private static final String KEY_WILD = "wildcard";
    private static final String KEY_FUZZ = "fuzz";

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
    class ConstructorRedirectData {
        
        public static final String KEY = "ctor";
        
        public int injected = 0;
        
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
        // Overridden so we can do this check later
    }

    @Override
    protected void addTargetNode(Target target, List<InjectionNode> myNodes, AbstractInsnNode insn, Set<InjectionPoint> nominators) {
        InjectionNode node = target.injectionNodes.get(insn);
        ConstructorRedirectData ctorData = null;
        int fuzz = BeforeFieldAccess.ARRAY_SEARCH_FUZZ_DEFAULT;
        
        if (node != null ) {
            Meta other = node.getDecoration(Meta.KEY);
            
            if (other != null && other.getOwner() != this) {
                if (other.priority >= this.meta.priority) {
                    Injector.logger.warn("{} conflict. Skipping {} with priority {}, already redirected by {} with priority {}",
                            this.annotationType, this.info, this.meta.priority, other.name, other.priority);
                    return;
                } else if (other.isFinal) {
                    throw new InvalidInjectionException(this.info, this.annotationType + " conflict: " + this
                            + " failed because target was already remapped by " + other.name);
                }
            }
        }
        
        for (InjectionPoint ip : nominators) {
            if (ip instanceof BeforeNew && !((BeforeNew)ip).hasDescriptor()) {
                ctorData = this.getCtorRedirect((BeforeNew)ip);
            } else if (ip instanceof BeforeFieldAccess) {
                fuzz = ((BeforeFieldAccess)ip).getFuzzFactor();
            }
        }
        
        InjectionNode targetNode = target.injectionNodes.add(insn);
        targetNode.decorate(Meta.KEY, this.meta);
        targetNode.decorate(RedirectInjector.KEY_NOMINATORS, nominators);
        if (insn instanceof TypeInsnNode && insn.getOpcode() == Opcodes.NEW) {
            targetNode.decorate(RedirectInjector.KEY_WILD, Boolean.valueOf(ctorData != null));
            targetNode.decorate(ConstructorRedirectData.KEY, ctorData);
        } else {
            targetNode.decorate(RedirectInjector.KEY_FUZZ, Integer.valueOf(fuzz));
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
            super.checkTarget(target);
            this.injectAtInvoke(target, node);
            return;
        }
        
        if (node.getCurrentTarget() instanceof FieldInsnNode) {
            super.checkTarget(target);
            this.injectAtFieldAccess(target, node);
            return;
        }
        
        if (node.getCurrentTarget() instanceof TypeInsnNode && node.getCurrentTarget().getOpcode() == Opcodes.NEW) {
            if (!this.isStatic && target.isStatic) {
                throw new InvalidInjectionException(this.info, "non-static callback method " + this + " has a static target which is not supported");
            }
            this.injectAtConstructor(target, node);
            return;
        }
        
        throw new InvalidInjectionException(this.info, this.annotationType + " annotation on is targetting an invalid insn in "
                + target + " in " + this);
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
            boolean wildcard = node.<Boolean>getDecoration(RedirectInjector.KEY_WILD).booleanValue();
            if (wildcard && meta.injected == 0) {
                throw new InvalidInjectionException(this.info, this.annotationType + " ctor invocation was not found in " + target);
            }
        }
    }
    
    /**
     * Redirect a method invocation
     */
    @Override
    protected void injectAtInvoke(Target target, InjectionNode node) {
        final MethodInsnNode methodNode = (MethodInsnNode)node.getCurrentTarget();
        final boolean targetIsStatic = methodNode.getOpcode() == Opcodes.INVOKESTATIC;
        final Type ownerType = Type.getType("L" + methodNode.owner + ";");
        final Type returnType = Type.getReturnType(methodNode.desc);
        final Type[] args = Type.getArgumentTypes(methodNode.desc);
        final Type[] stackVars = targetIsStatic ? args : ObjectArrays.concat(ownerType, args);
        boolean injectTargetParams = false;
        
        String desc = Injector.printArgs(stackVars) + returnType;
        if (!desc.equals(this.methodNode.desc)) {
            String alternateDesc = Injector.printArgs(ObjectArrays.concat(stackVars, target.arguments, Type.class)) + returnType;
            if (alternateDesc.equals(this.methodNode.desc)) {
                injectTargetParams = true;
            } else {
                throw new InvalidInjectionException(this.info, this.annotationType + " handler method " + this
                        + " has an invalid signature, expected " + desc + " found " + this.methodNode.desc);
            }
        }
        
        InsnList insns = new InsnList();
        int extraLocals = Bytecode.getArgsSize(stackVars) + 1;
        int extraStack = 1; // Normally only need 1 extra stack pos to store target ref 
        int[] argMap = this.storeArgs(target, stackVars, insns, 0);
        if (injectTargetParams) {
            int argSize = Bytecode.getArgsSize(target.arguments);
            extraLocals += argSize;
            extraStack += argSize;
            argMap = Ints.concat(argMap, target.argIndices);
        }
        AbstractInsnNode insn = this.invokeHandlerWithArgs(this.methodArgs, insns, argMap);
        target.replaceNode(methodNode, insn, insns);
        target.addToLocals(extraLocals);
        target.addToStack(extraStack);
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
            this.injectAtArrayField(target, fieldNode, opCode, ownerType, fieldType, fuzz);
        } else {
            this.injectAtScalarField(target, fieldNode, opCode, ownerType, fieldType);
        }
    }

    /**
     * Redirect an array element access
     */
    private void injectAtArrayField(Target target, FieldInsnNode fieldNode, int opCode, Type ownerType, Type fieldType, int fuzz) {
        Type elementType = fieldType.getElementType();
        if (opCode != Opcodes.GETSTATIC && opCode != Opcodes.GETFIELD) {
            throw new InvalidInjectionException(this.info, "Unspported opcode " + Bytecode.getOpcodeName(opCode) + " for array access " + this.info);
        } else if (this.returnType.getSort() != Type.VOID) {
            AbstractInsnNode varNode = BeforeFieldAccess.findArrayNode(target.insns, fieldNode, elementType.getOpcode(Opcodes.IALOAD), fuzz);
            this.injectAtGetArray(target, fieldNode, varNode, ownerType, fieldType);
        } else {
            AbstractInsnNode varNode = BeforeFieldAccess.findArrayNode(target.insns, fieldNode, elementType.getOpcode(Opcodes.IASTORE), fuzz);
            this.injectAtSetArray(target, fieldNode, varNode, ownerType, fieldType);
        }
    }
    
    /**
     * Array element read (xALOAD)
     */
    private void injectAtGetArray(Target target, FieldInsnNode fieldNode, AbstractInsnNode varNode, Type ownerType, Type fieldType) {
        String handlerDesc = Bytecode.generateDescriptor(this.returnType, (Object[])RedirectInjector.getArrayArgs(fieldType));
        boolean withArgs = this.checkDescriptor(handlerDesc, target, "array getter");
        this.injectArrayRedirect(target, fieldNode, varNode, withArgs, "array getter");
    }

    /**
     * Array element write (xASTORE)
     */
    private void injectAtSetArray(Target target, FieldInsnNode fieldNode, AbstractInsnNode varNode, Type ownerType, Type fieldType) {
        String handlerDesc = Bytecode.generateDescriptor(null, (Object[])RedirectInjector.getArrayArgs(fieldType, fieldType.getElementType()));
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
            throw new InvalidInjectionException(this.info, "Array element " + this.annotationType + " on " + this + " could not locate a matching "
                    + type + " instruction in " + target + ". " + advice);
        }
        
        if (!this.isStatic) {
            target.insns.insertBefore(fieldNode, new VarInsnNode(Opcodes.ALOAD, 0));
            target.addToStack(1);
        }
        
        InsnList invokeInsns = new InsnList();
        if (withArgs) {
            this.pushArgs(target.arguments, invokeInsns, target.argIndices, 0, target.arguments.length);
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
            throw new InvalidInjectionException(this.info, "Unspported opcode " + Bytecode.getOpcodeName(opCode) + " for " + this.info);
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
            this.pushArgs(target.arguments, insns, target.argIndices, 0, target.arguments.length);
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
            this.pushArgs(target.arguments, insns, target.argIndices, 0, target.arguments.length);
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
        
        throw new InvalidInjectionException(this.info, this.annotationType + " method " + type + " " + this
                + " has an invalid signature. Expected " + desc + " but found " + this.methodNode.desc);
    }

    protected void injectAtConstructor(Target target, InjectionNode node) {
        ConstructorRedirectData meta = node.<ConstructorRedirectData>getDecoration(ConstructorRedirectData.KEY);
        boolean wildcard = node.<Boolean>getDecoration(RedirectInjector.KEY_WILD).booleanValue();
        
        final TypeInsnNode newNode = (TypeInsnNode)node.getCurrentTarget();
        final AbstractInsnNode dupNode = target.get(target.indexOf(newNode) + 1);
        final MethodInsnNode initNode = this.findInitNode(target, newNode);
        
        if (initNode == null) {
            if (!wildcard) {
                throw new InvalidInjectionException(this.info, this.annotationType + " ctor invocation was not found in " + target);
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
            if (!wildcard) {
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
            this.pushArgs(target.arguments, insns, target.argIndices, 0, target.arguments.length);
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
            this.throwException(insns, "java/lang/NullPointerException", this.annotationType + " constructor handler " + this
                    + " returned null for " + newNode.desc.replace('/', '.'));
            insns.add(nullCheckSucceeded);
            target.addToStack(1);
        } else {
            // Result is not assigned, so just pop it from the operand stack
            insns.add(new InsnNode(Opcodes.POP));
        }
        
        target.replaceNode(initNode, insns);
        meta.injected++;
    }

    protected MethodInsnNode findInitNode(Target target, TypeInsnNode newNode) {
        int indexOf = target.indexOf(newNode);
        for (Iterator<AbstractInsnNode> iter = target.insns.iterator(indexOf); iter.hasNext();) {
            AbstractInsnNode insn = iter.next();
            if (insn instanceof MethodInsnNode && insn.getOpcode() == Opcodes.INVOKESPECIAL) {
                MethodInsnNode methodNode = (MethodInsnNode)insn;
                if (Constants.CTOR.equals(methodNode.name) && methodNode.owner.equals(newNode.desc)) {
                    return methodNode;
                }
            }
        }
        return null;
    }
    
    private static Type[] getArrayArgs(Type fieldType, Type... extra) {
        int dimensions = fieldType.getDimensions() + 1;
        Type[] args = new Type[dimensions + extra.length];
        for (int i = 0; i < args.length; i++) {
            args[i] = i == 0 ? fieldType : i < dimensions ? Type.INT_TYPE : extra[dimensions - i];
        }
        return args;
    }

}
