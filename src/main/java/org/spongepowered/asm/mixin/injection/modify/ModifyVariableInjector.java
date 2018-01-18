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
package org.spongepowered.asm.mixin.injection.modify;

import java.util.Collection;
import java.util.List;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.lib.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.asm.util.SignaturePrinter;

/**
 * A bytecode injector which allows a single local variable in the target method
 * to be captured and altered. See also {@link LocalVariableDiscriminator} and
 * {@link ModifyVariable}.
 */
public class ModifyVariableInjector extends Injector {
        
    /**
     * Target context information
     */
    static class Context extends LocalVariableDiscriminator.Context {
        
        /**
         * Instructions to inject 
         */
        final InsnList insns = new InsnList();

        public Context(Type returnType, boolean argsOnly, Target target, AbstractInsnNode node) {
            super(returnType, argsOnly, target, node);
        }
        
    }
    
    /**
     * Specialised injection point which uses a target-aware search pattern
     */
    abstract static class ContextualInjectionPoint extends InjectionPoint {
        
        protected final IMixinContext context;

        ContextualInjectionPoint(IMixinContext context) {
            this.context = context;
        }

        @Override
        public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
            throw new InvalidInjectionException(this.context, this.getAtCode() + " injection point must be used in conjunction with @ModifyVariable");
        }

        abstract boolean find(Target target, Collection<AbstractInsnNode> nodes);
        
    }
    
    /**
     * True to consider only method args
     */
    private final LocalVariableDiscriminator discriminator;

    /**
     * @param info Injection info
     * @param discriminator discriminator
     */
    public ModifyVariableInjector(InjectionInfo info, LocalVariableDiscriminator discriminator) {
        super(info);
        this.discriminator = discriminator;
    }
    
    @Override
    protected boolean findTargetNodes(MethodNode into, InjectionPoint injectionPoint, InsnList insns, Collection<AbstractInsnNode> nodes) {
        if (injectionPoint instanceof ContextualInjectionPoint) {
            Target target = this.info.getContext().getTargetMethod(into);
            return ((ContextualInjectionPoint)injectionPoint).find(target, nodes);
        }
        return injectionPoint.find(into.desc, insns, nodes);
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
            throw new InvalidInjectionException(this.info, "'static' of variable modifier method does not match target in " + this);
        }
        
        int ordinal = this.discriminator.getOrdinal();
        if (ordinal < -1) {
            throw new InvalidInjectionException(this.info, "Invalid ordinal " + ordinal + " specified in " + this);
        }
        
        if (this.discriminator.getIndex() == 0 && !this.isStatic) {
            throw new InvalidInjectionException(this.info, "Invalid index 0 specified in non-static variable modifier " + this);
        }
    }
    
    /**
     * Do the injection
     */
    @Override
    protected void inject(Target target, InjectionNode node) {
        if (node.isReplaced()) {
            throw new InvalidInjectionException(this.info, "Variable modifier target for " + this + " was removed by another injector");
        }
        
        Context context = new Context(this.returnType, this.discriminator.isArgsOnly(), target, node.getCurrentTarget());
        
        if (this.discriminator.printLVT()) {
            this.printLocals(context);
        }
        
        String handlerDesc = Bytecode.getDescriptor(new Type[] { this.returnType }, this.returnType);
        if (!handlerDesc.equals(this.methodNode.desc)) {
            throw new InvalidInjectionException(this.info, "Variable modifier " + this + " has an invalid signature, expected " + handlerDesc
                    + " but found " + this.methodNode.desc);
        }
        
        try {
            int local = this.discriminator.findLocal(context);
            if (local > -1) {
                this.inject(context, local);
            }
        } catch (InvalidImplicitDiscriminatorException ex) {
            if (this.discriminator.printLVT()) {
                this.info.addCallbackInvocation(this.methodNode);
                return;
            }
            throw new InvalidInjectionException(this.info, "Implicit variable modifier injection failed in " + this, ex);
        }
        
        target.insns.insertBefore(context.node, context.insns);
        target.addToStack(this.isStatic ? 1 : 2);
    }

    /**
     * Pretty-print local variable information to stderr
     */
    private void printLocals(final Context context) {
        SignaturePrinter handlerSig = new SignaturePrinter(this.methodNode.name, this.returnType, this.methodArgs, new String[] { "var" });
        handlerSig.setModifiers(this.methodNode);

        new PrettyPrinter()
            .kvWidth(20)
            .kv("Target Class", this.classNode.name.replace('/', '.'))
            .kv("Target Method", context.target.method.name)
            .kv("Callback Name", this.methodNode.name)
            .kv("Capture Type", SignaturePrinter.getTypeName(this.returnType, false))
            .kv("Instruction", "%s %s", context.node.getClass().getSimpleName(), Bytecode.getOpcodeName(context.node.getOpcode())).hr()
            .kv("Match mode", this.discriminator.isImplicit(context) ? "IMPLICIT (match single)" : "EXPLICIT (match by criteria)")
            .kv("Match ordinal", this.discriminator.getOrdinal() < 0 ? "any" : this.discriminator.getOrdinal())
            .kv("Match index", this.discriminator.getIndex() < context.baseArgIndex ? "any" : this.discriminator.getIndex())
            .kv("Match name(s)", this.discriminator.hasNames() ? this.discriminator.getNames() : "any")
            .kv("Args only", this.discriminator.isArgsOnly()).hr()
            .add(context)
            .print(System.err);
    }
    
    /**
     * Perform the injection
     * 
     * @param context target context
     * @param local local variable to capture
     */
    private void inject(final Context context, final int local) {
        if (!this.isStatic) {
            context.insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        
        context.insns.add(new VarInsnNode(this.returnType.getOpcode(Opcodes.ILOAD), local));
        this.invokeHandler(context.insns);
        context.insns.add(new VarInsnNode(this.returnType.getOpcode(Opcodes.ISTORE), local));
    }

}
