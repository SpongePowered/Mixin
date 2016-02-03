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

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.FieldInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.InsnNode;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.InvalidInjectionException;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
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
     * @param info Injection info
     */
    public RedirectInjector(InjectionInfo info) {
        super(info, "@Redirect");
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.callback.BytecodeInjector
     *      #inject(org.spongepowered.asm.mixin.injection.callback.Target,
     *      org.objectweb.asm.tree.AbstractInsnNode)
     */
    @Override
    protected void inject(Target target, AbstractInsnNode node) {
        if (node instanceof MethodInsnNode) {
            this.injectAtInvoke(target, (MethodInsnNode)node);
            return;
        }
        
        if (node instanceof FieldInsnNode) {
            this.injectAtFieldAccess(target, (FieldInsnNode)node);
            return;
        }
        
        throw new InvalidInjectionException(this.info, this.annotationType + " annotation on is targetting an invalid insn in "
                + target + " in " + this);
    }

    /**
     * Redirect a method invocation
     */
    @Override
    protected void injectAtInvoke(Target target, MethodInsnNode node) {
        boolean injectTargetParams = false;
        boolean targetIsStatic = node.getOpcode() == Opcodes.INVOKESTATIC;
        Type ownerType = Type.getType("L" + node.owner + ";");
        Type returnType = Type.getReturnType(node.desc);
        Type[] args = Type.getArgumentTypes(node.desc);
        Type[] stackVars = targetIsStatic ? args : ObjectArrays.concat(ownerType, args);
        
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
        this.invokeHandlerWithArgs(this.methodArgs, insns, argMap);
        
        target.insns.insertBefore(node, insns);
        target.insns.remove(node);
        target.addToLocals(extraLocals);
        target.addToStack(extraStack);
    }

    /**
     * Redirect a field get or set operation
     */
    private void injectAtFieldAccess(Target target, FieldInsnNode node) {
        boolean staticField = node.getOpcode() == Opcodes.GETSTATIC || node.getOpcode() == Opcodes.PUTSTATIC;
        Type ownerType = Type.getType("L" + node.owner + ";");
        Type fieldType = Type.getType(node.desc);

        InsnList insns = new InsnList();
        if (node.getOpcode() == Opcodes.GETSTATIC || node.getOpcode() == Opcodes.GETFIELD) {
            this.injectAtGetField(insns, target, node, staticField, ownerType, fieldType);
        } else if (node.getOpcode() == Opcodes.PUTSTATIC || node.getOpcode() == Opcodes.PUTFIELD) {
            this.injectAtPutField(insns, target, node, staticField, ownerType, fieldType);
        } else {
            throw new InvalidInjectionException(this.info, "Unspported opcode " + node.getOpcode() + " on FieldInsnNode for " + this.info);
        }
        
        target.insns.insertBefore(node, insns);
        target.insns.remove(node);
    }

    /**
     * Inject opcodes to redirect a field getter. The injection will vary based
     * on the staticness of the field and the handler thus there are four
     * possible scenarios based on the possible combinations of static on the
     * handler and the field itself.
     */
    private void injectAtGetField(InsnList insns, Target target, FieldInsnNode node, boolean staticField, Type owner, Type fieldType) {
        String handlerDesc = staticField ? ASMHelper.generateDescriptor(fieldType) : ASMHelper.generateDescriptor(fieldType, owner);
        boolean withArgs = this.checkDescriptor(handlerDesc, target, "getter");

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
        
        this.invokeHandler(insns);
        target.addToStack(this.isStatic ? 0 : 1);
    }

    /**
     * Inject opcodes to redirect a field setter. The injection will vary based
     * on the staticness of the field and the handler thus there are four
     * possible scenarios based on the possible combinations of static on the
     * handler and the field itself.
     */
    private void injectAtPutField(InsnList insns, Target target, FieldInsnNode node, boolean staticField, Type owner, Type fieldType) {
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
        
        this.invokeHandler(insns);
        target.addToStack(!this.isStatic && !staticField ? 1 : 0);
    }

    /**
     * Check that the handler descriptor matches the calculated descriptor for
     * the field access being redirected.
     */
    private boolean checkDescriptor(String desc, Target target, String type) {
        if (this.methodNode.desc.equals(desc)) {
            return false;
        }
        
        int pos = desc.indexOf(')');
        String alternateDesc = String.format("%s%s%s", desc.substring(0, pos), Joiner.on("").join(target.arguments), desc.substring(pos));
        if (this.methodNode.desc.equals(alternateDesc)) {
            return true;
        }
        
        System.err.printf("%s\n", alternateDesc);
        
        throw new InvalidInjectionException(this.info, this.annotationType + " field " + type + " " + this
                + " has an invalid signature. Expected " + desc + " but found " + this.methodNode.desc);
    }

}
