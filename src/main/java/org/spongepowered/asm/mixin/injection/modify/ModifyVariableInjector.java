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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InjectionPoint.RestrictTargetLevel;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.code.InjectorTarget;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.struct.Target.Extension;
import org.spongepowered.asm.mixin.injection.throwables.InjectionError;
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

        public Context(InjectionInfo info, Type returnType, boolean argsOnly, Target target, AbstractInsnNode node) {
            super(info, returnType, argsOnly, target, node);
        }
        
    }
    
    /**
     * Specialised injection point which uses a target-aware search pattern
     */
    abstract static class LocalVariableInjectionPoint extends InjectionPoint {
        
        protected final IMixinContext mixin;
        
        LocalVariableInjectionPoint(InjectionPointData data) {
            super(data);
            this.mixin = data.getMixin();
        }

        @Override
        public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
            throw new InvalidInjectionException(this.mixin, this.getAtCode() + " injection point must be used in conjunction with @ModifyVariable");
        }
        
        abstract boolean find(InjectionInfo info, InsnList insns, Collection<AbstractInsnNode> nodes, Target target);

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
        super(info, "@ModifyVariable");
        this.discriminator = discriminator;
    }
    
    @Override
    protected boolean findTargetNodes(MethodNode into, InjectionPoint injectionPoint, InjectorTarget injectorTarget,
            Collection<AbstractInsnNode> nodes) {
        if (injectionPoint instanceof LocalVariableInjectionPoint) {
            return ((LocalVariableInjectionPoint)injectionPoint).find(this.info, injectorTarget.getSlice(injectionPoint), nodes,
                    injectorTarget.getTarget());
        }
        return injectionPoint.find(into.desc, injectorTarget.getSlice(injectionPoint), nodes);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.callback.BytecodeInjector
     *      #sanityCheck(org.spongepowered.asm.mixin.injection.callback.Target,
     *      java.util.List)
     */
    @Override
    protected void sanityCheck(Target target, List<InjectionPoint> injectionPoints) {
        super.sanityCheck(target, injectionPoints);
        
        int ordinal = this.discriminator.getOrdinal();
        if (ordinal < -1) {
            throw new InvalidInjectionException(this.info, "Invalid ordinal " + ordinal + " specified in " + this);
        }
        
        if (this.discriminator.getIndex() == 0 && !target.isStatic) {
            throw new InvalidInjectionException(this.info, "Invalid index 0 specified in non-static variable modifier " + this);
        }
    }
    
    /**
     * Generate a key which uniquely identifies the combination of return type,
     * frame type and target injection node so that injectors targetting the
     * same instruction still get unique contexts.
     * 
     * @param target Target method
     * @param node Target node
     * @return Key for storing/retrieving the injector context decoration
     */
    protected String getTargetNodeKey(Target target, InjectionNode node) {
        return String.format("localcontext(%s,%s,#%s)", this.returnType, this.discriminator.isArgsOnly() ? "argsOnly" : "fullFrame", node.getId());
    }
    
    @Override
    protected void preInject(Target target, InjectionNode node) {
        String key = this.getTargetNodeKey(target, node);
        if (node.hasDecoration(key)) {
            return; // already have a suitable context
        }
        Context context = new Context(this.info, this.returnType, this.discriminator.isArgsOnly(), target, node.getCurrentTarget());
        node.<Context>decorate(key, context);
    }
    
    /**
     * Do the injection
     */
    @Override
    protected void inject(Target target, InjectionNode node) {
        if (node.isReplaced()) {
            throw new InvalidInjectionException(this.info, "Variable modifier target for " + this + " was removed by another injector");
        }
        
        Context context = node.<Context>getDecoration(this.getTargetNodeKey(target, node));
        if (context == null) {
            throw new InjectionError(String.format(
                    "%s injector target is missing CONTEXT decoration for %s. PreInjection failure or illegal internal state change",
                    this.annotationType, this.info));
        }
        
        // If the context is being reused (because two identical injectors are targetting this node)
        // then the insns SHOULD have been drained by the previous insertBefore. If the list hasn't
        // been cleared for some reason then something probably went wrong during the previous inject
        if (context.insns.size() > 0) {
            throw new InjectionError(String.format(
                    "%s injector target has contaminated CONTEXT decoration for %s. Check for previous errors.",
                    this.annotationType, this.info));
        }
        
        if (this.discriminator.printLVT()) {
            this.printLocals(target, context);
        }
        
        this.checkTargetForNode(target, node, RestrictTargetLevel.ALLOW_ALL);
        
        InjectorData handler = new InjectorData(target, "handler", false);

        if (this.returnType == Type.VOID_TYPE) {
            throw new InvalidInjectionException(this.info, String.format(
                    "%s %s method %s from %s has an invalid signature, cannot return a VOID type.",
                    this.annotationType, handler, this, this.info.getMixin()));
        }

        this.validateParams(handler, this.returnType, this.returnType);
        
        Extension extraStack = target.extendStack();
        
        try {
            int local = this.discriminator.findLocal(context);
            if (local > -1) {
                this.inject(context, handler, extraStack, local);
            }
        } catch (InvalidImplicitDiscriminatorException ex) {
            if (this.discriminator.printLVT()) {
                this.info.addCallbackInvocation(this.methodNode);
                return;
            }
            throw new InvalidInjectionException(this.info, "Implicit variable modifier injection failed in " + this, ex);
        }
        
        extraStack.apply();
        target.insns.insertBefore(context.node, context.insns);
    }

    /**
     * Pretty-print local variable information to stderr
     */
    private void printLocals(Target target, Context context) {
        SignaturePrinter handlerSig = new SignaturePrinter(this.info.getMethodName(), this.returnType, this.methodArgs, new String[] { "var" });
        handlerSig.setModifiers(this.methodNode);

        String matchMode = "EXPLICIT (match by criteria)";
        if (this.discriminator.isImplicit(context)) {
            int candidateCount = context.getCandidateCount();
            matchMode = "IMPLICIT (match single) - " + (candidateCount == 1 ? "VALID (exactly 1 match)" : "INVALID (" + candidateCount + " matches)");
        }
        new PrettyPrinter()
            .kvWidth(20)
            .kv("Target Class", this.classNode.name.replace('/', '.'))
            .kv("Target Method", context.target.method.name)
            .kv("Callback Name", this.info.getMethodName())
            .kv("Capture Type", SignaturePrinter.getTypeName(this.returnType, false))
            .kv("Instruction", "[%d] %s %s", target.insns.indexOf(context.node), context.node.getClass().getSimpleName(),
                    Bytecode.getOpcodeName(context.node.getOpcode())).hr()
            .kv("Match mode", matchMode)
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
     * @param extraStack stack extension
     * @param local local variable to capture
     */
    private void inject(final Context context, InjectorData handler, Extension extraStack, final int local) {
        if (!this.isStatic) {
            context.insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            extraStack.add();
        }
        
        context.insns.add(new VarInsnNode(this.returnType.getOpcode(Opcodes.ILOAD), local));
        extraStack.add();

        if (handler.captureTargetArgs > 0) {
            this.pushArgs(handler.target.arguments, context.insns, handler.target.getArgIndices(), 0, handler.captureTargetArgs, extraStack);
        }
        
        this.invokeHandler(context.insns);
        context.insns.add(new VarInsnNode(this.returnType.getOpcode(Opcodes.ISTORE), local));
    }

}
