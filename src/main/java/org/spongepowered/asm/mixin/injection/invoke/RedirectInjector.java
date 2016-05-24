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
import org.spongepowered.asm.lib.tree.FieldInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.InsnNode;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.VarInsnNode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.ASMHelper;

import com.google.common.base.Joiner;
import com.google.common.collect.ObjectArrays;
import com.google.common.primitives.Ints;

/**
 * <p>A bytecode injector which allows a method call or field access to be
 * redirected to the annotated handler method. For method redirects, the handler
 * method signature must match the hooked method precisely <b>but</b> prepended
 * with an arg of the owning object's type to accept the object instance the
 * method was going to be invoked on. For example when hooking the following
 * call:</p>
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
 */
public class RedirectInjector extends InvokeInjector {
    
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
    
    protected Meta meta;

    /**
     * @param info Injection info
     */
    public RedirectInjector(InjectionInfo info) {
        this(info, "@Redirect");
    }
    
    protected RedirectInjector(InjectionInfo info, String annotationType) {
        super(info, annotationType);
        
        int priority = info.getContext().getPriority();
        boolean isFinal = ASMHelper.getVisibleAnnotation(this.methodNode, Final.class) != null;
        this.meta = new Meta(priority, isFinal, this.info.toString(), this.methodNode.desc);
    }
    
    @Override
    protected void addTargetNode(Target target, List<InjectionNode> myNodes, AbstractInsnNode insn) {
        InjectionNode node = target.injectionNodes.get(insn);
        
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
        
        myNodes.add(target.injectionNodes.add(insn).decorate(Meta.KEY, this.meta));
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
            this.injectAtInvoke(target, node);
            return;
        }
        
        if (node.getCurrentTarget() instanceof FieldInsnNode) {
            this.injectAtFieldAccess(target, node);
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
        int extraLocals = ASMHelper.getArgsSize(stackVars) + 1;
        int extraStack = 1; // Normally only need 1 extra stack pos to store target ref 
        int[] argMap = this.storeArgs(target, stackVars, insns, 0);
        if (injectTargetParams) {
            int argSize = ASMHelper.getArgsSize(target.arguments);
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
     * Redirect a field get or set operation
     */
    private void injectAtFieldAccess(Target target, InjectionNode node) {
        final FieldInsnNode fieldNode = (FieldInsnNode)node.getCurrentTarget();
        final int opCode = fieldNode.getOpcode();
        final boolean staticField = opCode == Opcodes.GETSTATIC || opCode == Opcodes.PUTSTATIC;
        final Type ownerType = Type.getType("L" + fieldNode.owner + ";");
        final Type fieldType = Type.getType(fieldNode.desc);

        AbstractInsnNode invoke = null;
        InsnList insns = new InsnList();
        if (opCode == Opcodes.GETSTATIC || opCode == Opcodes.GETFIELD) {
            invoke = this.injectAtGetField(insns, target, fieldNode, staticField, ownerType, fieldType);
        } else if (opCode == Opcodes.PUTSTATIC || opCode == Opcodes.PUTFIELD) {
            invoke = this.injectAtPutField(insns, target, fieldNode, staticField, ownerType, fieldType);
        } else {
            throw new InvalidInjectionException(this.info, "Unspported opcode " + opCode + " on FieldInsnNode for " + this.info);
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
        final String handlerDesc = staticField ? ASMHelper.generateDescriptor(fieldType) : ASMHelper.generateDescriptor(fieldType, owner);
        final boolean withArgs = this.checkDescriptor(handlerDesc, target, "getter");

        if (!this.isStatic) {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            if (!staticField) {
                insns.add(new InsnNode(Opcodes.SWAP));
            }
        }
        
        if (withArgs) {
            this.pushArgs(target.arguments, insns, target.argIndices, 0, target.arguments.length);
            target.addToStack(ASMHelper.getArgsSize(target.arguments));
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
        String handlerDesc = staticField ? ASMHelper.generateDescriptor(null, fieldType) : ASMHelper.generateDescriptor(null, owner, fieldType);
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
            target.addToStack(ASMHelper.getArgsSize(target.arguments));
        }
        
        target.addToStack(!this.isStatic && !staticField ? 1 : 0);
        return this.invokeHandler(insns);
    }

    /**
     * Check that the handler descriptor matches the calculated descriptor for
     * the field access being redirected.
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

}
