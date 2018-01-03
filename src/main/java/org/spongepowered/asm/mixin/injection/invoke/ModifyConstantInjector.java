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

import org.apache.logging.log4j.Level;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.FieldInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.InsnNode;
import org.spongepowered.asm.lib.tree.JumpInsnNode;
import org.spongepowered.asm.lib.tree.LocalVariableNode;
import org.spongepowered.asm.lib.tree.VarInsnNode;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.invoke.util.InsnFinder;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Locals;
import org.spongepowered.asm.util.SignaturePrinter;

/**
 * A bytecode injector which allows a specific
 */
public class ModifyConstantInjector extends RedirectInjector {
    
    /**
     * Offset between "implicit zero" opcodes and "explicit int comparison"
     * opcodes
     */
    private static final int OPCODE_OFFSET = Opcodes.IF_ICMPLT - Opcodes.IFLT;

    /**
     * @param info Injection info
     */
    public ModifyConstantInjector(InjectionInfo info) {
        super(info, "@ModifyConstant");
    }
    
    @Override
    protected void inject(Target target, InjectionNode node) {
        if (!this.preInject(node)) {
            return;
        }
            
        if (node.isReplaced()) {
            throw new UnsupportedOperationException("Target failure for " + this.info);
        }
        
        AbstractInsnNode targetNode = node.getCurrentTarget();
        if (targetNode instanceof JumpInsnNode) {
            this.checkTargetModifiers(target, false);
            this.injectExpandedConstantModifier(target, (JumpInsnNode)targetNode);
            return;
        }
        
        if (Bytecode.isConstant(targetNode)) {
            this.checkTargetModifiers(target, false);
            this.injectConstantModifier(target, targetNode);
            return;
        }
        
        throw new InvalidInjectionException(this.info, this.annotationType + " annotation is targetting an invalid insn in "
                + target + " in " + this);
    }
    
    /**
     * Injects a constant modifier at an implied-zero
     * 
     * @param target target method
     * @param jumpNode jump instruction (must be IFLT, IFGE, IFGT or IFLE)
     */
    private void injectExpandedConstantModifier(Target target, JumpInsnNode jumpNode) {
        int opcode = jumpNode.getOpcode();
        if (opcode < Opcodes.IFLT || opcode > Opcodes.IFLE) {
            throw new InvalidInjectionException(this.info, this.annotationType + " annotation selected an invalid opcode "
                    + Bytecode.getOpcodeName(opcode) + " in " + target + " in " + this); 
        }
        
        final InsnList insns = new InsnList();
        insns.add(new InsnNode(Opcodes.ICONST_0));
        AbstractInsnNode invoke = this.invokeConstantHandler(Type.getType("I"), target, insns, insns);
        insns.add(new JumpInsnNode(opcode + ModifyConstantInjector.OPCODE_OFFSET, jumpNode.label));
        target.replaceNode(jumpNode, invoke, insns);
        target.addToStack(1);
    }

    private void injectConstantModifier(Target target, AbstractInsnNode constNode) {
        final Type constantType = Bytecode.getConstantType(constNode);
        
        if (constantType.getSort() <= Type.INT && this.info.getContext().getOption(Option.DEBUG_VERBOSE)) {
            this.checkNarrowing(target, constNode, constantType);
        }
        
        final InsnList before = new InsnList();
        final InsnList after = new InsnList();
        AbstractInsnNode invoke = this.invokeConstantHandler(constantType, target, before, after);
        target.wrapNode(constNode, invoke, before, after);
    }

    private AbstractInsnNode invokeConstantHandler(Type constantType, Target target, InsnList before, InsnList after) {
        final String handlerDesc = Bytecode.generateDescriptor(constantType, constantType);
        final boolean withArgs = this.checkDescriptor(handlerDesc, target, "getter");

        if (!this.isStatic) {
            before.insert(new VarInsnNode(Opcodes.ALOAD, 0));
            target.addToStack(1);
        }
        
        if (withArgs) {
            this.pushArgs(target.arguments, after, target.getArgIndices(), 0, target.arguments.length);
            target.addToStack(Bytecode.getArgsSize(target.arguments));
        }
        
        return this.invokeHandler(after);
    }

    private void checkNarrowing(Target target, AbstractInsnNode constNode, Type constantType) {
        AbstractInsnNode pop = new InsnFinder().findPopInsn(target, constNode);

        if (pop == null) { // Not found, give up early
            return;
        } else if (pop instanceof FieldInsnNode) { // Integer return, check for narrowing conversion
            FieldInsnNode fieldNode = (FieldInsnNode)pop;
            Type fieldType = Type.getType(fieldNode.desc);
            this.checkNarrowing(target, constNode, constantType, fieldType, target.indexOf(pop), String.format("%s %s %s.%s",
                    Bytecode.getOpcodeName(pop), SignaturePrinter.getTypeName(fieldType, false), fieldNode.owner.replace('/', '.'), fieldNode.name));
        } else if (pop.getOpcode() == Opcodes.IRETURN) { // Integer return, check for narrowing conversion
            this.checkNarrowing(target, constNode, constantType, target.returnType, target.indexOf(pop), "RETURN "
                    + SignaturePrinter.getTypeName(target.returnType, false));
        } else if (pop.getOpcode() == Opcodes.ISTORE) { // Integer store, attempt to get the relevant local type
            int var = ((VarInsnNode)pop).var;
            LocalVariableNode localVar = Locals.getLocalVariableAt(target.classNode, target.method, pop, var);

            // Frankly this will not work in 90% of cases, it basically only works if the variable being assigned is actually
            // a method argument, and is pretty much never going to work for any other type of local variable
            if (localVar != null && localVar.desc != null) {
                String name = localVar.name != null ? localVar.name : "unnamed";
                Type localType = Type.getType(localVar.desc);
                this.checkNarrowing(target, constNode, constantType, localType, target.indexOf(pop), String.format("ISTORE[var=%d] %s %s", var, 
                        SignaturePrinter.getTypeName(localType, false), name));
            }
        }
    }

    private void checkNarrowing(Target target, AbstractInsnNode constNode, Type constantType, Type type, int index, String description) {
        int fromSort = constantType.getSort();
        int toSort = type.getSort();
        if (toSort < fromSort) {
            String fromType = SignaturePrinter.getTypeName(constantType, false);
            String toType = SignaturePrinter.getTypeName(type, false);
            String message = toSort == Type.BOOLEAN ? ". Implicit conversion to <boolean> can cause nondeterministic (JVM-specific) behaviour!" : "";
            Level level = toSort == Type.BOOLEAN ? Level.ERROR : Level.WARN;
            Injector.logger.log(level, "Narrowing conversion of <{}> to <{}> in {} target {} at opcode {} ({}){}", fromType, toType, this.info,
                    target, index, description, message);
        }
    }

}
