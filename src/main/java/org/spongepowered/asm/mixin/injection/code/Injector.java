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
package org.spongepowered.asm.mixin.injection.code;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InvalidInjectionException;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.ASMHelper;

import com.google.common.base.Joiner;

/**
 * Base class for bytecode injectors
 */
public abstract class Injector {

    /**
     * Injection info
     */
    protected InjectionInfo info;

    /**
     * Class node
     */
    protected final ClassNode classNode;
    
    /**
     * Callback method 
     */
    protected final MethodNode methodNode;
    
    /**
     * Arguments of the handler method 
     */
    protected final Type[] methodArgs;
    
    /**
     * Return type of the handler method 
     */
    protected final Type returnType;

    /**
     * True if the callback method is static
     */
    protected final boolean isStatic;

    /**
     * Make a new CallbackInjector for the supplied InjectionInfo
     * 
     * @param info Information about this injection
     */
    public Injector(InjectionInfo info) {
        this(info.getClassNode(), info.getMethod());
        this.info = info;
    }

    /**
     * Make a new CallbackInjector with the supplied args
     * 
     * @param classNode Class containing callback and target methods
     * @param methodNode Callback method
     */
    private Injector(ClassNode classNode, MethodNode methodNode) {
        this.classNode = classNode;
        this.methodNode = methodNode;
        this.methodArgs = Type.getArgumentTypes(this.methodNode.desc);
        this.returnType = Type.getReturnType(this.methodNode.desc);
        this.isStatic = ASMHelper.methodIsStatic(this.methodNode);
    }
    
    @Override
    public String toString() {
        return String.format("%s::%s", this.classNode.name, this.methodNode.name);
    }

    /**
     * Inject into the specified method at the specified injection points
     * 
     * @param target Target method to inject into
     * @param injectionPoints InjectionPoint instances which will identify
     *      target insns in the target method 
     */
    public final void injectInto(Target target, List<InjectionPoint> injectionPoints) {
        this.sanityCheck(target, injectionPoints);
        
        for (AbstractInsnNode node : this.findTargetNodes(target.method, injectionPoints)) {
            this.inject(target, node);
        }
    }

    /**
     * Use the supplied InjectionPoints to find target insns in the target
     * method
     * 
     * @param into Target method
     * @param injectionPoints List of injection points parsed from At
     *      annotations on the callback method
     * @return Target insn nodes in the target method
     */
    protected Set<AbstractInsnNode> findTargetNodes(MethodNode into, List<InjectionPoint> injectionPoints) {
        Set<AbstractInsnNode> targetNodes = new HashSet<AbstractInsnNode>();

        // Defensive objects, so that injectionPoint instances can't modify our working copies
        ReadOnlyInsnList insns = new ReadOnlyInsnList(into.instructions);
        Collection<AbstractInsnNode> nodes = new ArrayList<AbstractInsnNode>(32);

        for (InjectionPoint injectionPoint : injectionPoints) {
            nodes.clear();
            if (this.findTargetNodes(into, injectionPoint, insns, nodes)) {
                targetNodes.addAll(nodes);
            }
        }
        
        insns.dispose();
        return targetNodes;
    }

    protected boolean findTargetNodes(MethodNode into, InjectionPoint injectionPoint, InsnList insns, Collection<AbstractInsnNode> nodes) {
        return injectionPoint.find(into.desc, insns, nodes);
    }

    protected void sanityCheck(Target target, List<InjectionPoint> injectionPoints) {
        if (target.classNode != this.classNode) {
            throw new InvalidInjectionException(this.info, "Target class does not match injector class in " + this);
        }
    }

    protected abstract void inject(Target target, AbstractInsnNode node);

    /**
     * Invoke the handler method
     * 
     * @param insns Instruction list to inject into
     */
    protected void invokeHandler(InsnList insns) {
        this.invokeHandler(insns, this.methodNode);
    }

    /**
     * Invoke a handler method
     * 
     * @param insns Instruction list to inject into
     * @param handler Actual method to invoke (may be different if using a
     *      surrogate)
     */
    protected void invokeHandler(InsnList insns, MethodNode handler) {
        boolean isPrivate = (handler.access & Opcodes.ACC_PRIVATE) != 0;
        int invokeOpcode = this.isStatic ? Opcodes.INVOKESTATIC : isPrivate ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL;
        insns.add(new MethodInsnNode(invokeOpcode, this.classNode.name, handler.name, handler.desc, false));
        this.info.addCallbackInvocation(handler);
    }
    
    protected static String printArgs(Type[] args) {
        return "(" + Joiner.on("").join(args) + ")";
    }
    
    public static boolean canCoerce(Type from, Type to) {
        return Injector.canCoerce(from.getDescriptor(), to.getDescriptor());
    }
    
    public static boolean canCoerce(String from, String to) {
        if (from.length() > 1 || to.length() > 1) {
            return false;
        }
        
        return Injector.canCoerce(from.charAt(0), to.charAt(0));
    }

    public static boolean canCoerce(char from, char to) {
        return to == 'I' && "IBSCZ".indexOf(from) > -1;
    }

}
