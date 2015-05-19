/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered.org <http://www.spongepowered.org>
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
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.ASMHelper;

import com.google.common.base.Joiner;


/**
 * Base class for bytecode injectors
 */
public abstract class Injector {
    
    protected static final String CTOR = "<init>";

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
        this.isStatic = ASMHelper.methodIsStatic(this.methodNode);
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
            if (injectionPoint.find(into.desc, insns, nodes)) {
                targetNodes.addAll(nodes);
            }
        }
        
        insns.dispose();
        return targetNodes;
    }

    protected void sanityCheck(Target target, List<InjectionPoint> injectionPoints) {
        // stub for subclasses
    }

    protected abstract void inject(Target target, AbstractInsnNode node);

    /**
     * Invoke the handler method
     * 
     * @param insns Instruction list to inject into
     */
    protected void invokeHandler(InsnList insns) {
        this.invokeMethod(insns, this.methodNode);
    }

    protected void invokeMethod(InsnList insns, MethodNode methodNode) {
        boolean isPrivate = (methodNode.access & Opcodes.ACC_PRIVATE) != 0;
        int invokeOpcode = this.isStatic ? Opcodes.INVOKESTATIC : isPrivate ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL;
        insns.add(new MethodInsnNode(invokeOpcode, this.classNode.name, methodNode.name, methodNode.desc, false));
    }
    
    protected static String printArgs(Type[] args) {
        return "(" + Joiner.on("").join(args) + ")";
    }
}
