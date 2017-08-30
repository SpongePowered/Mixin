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
package org.spongepowered.asm.mixin.transformer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.struct.MemberRef;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Method;

/**
 * Base for class context objects
 */
abstract class ClassContext {
    
    /**
     * Methods in the this class which have been upgraded from
     * <tt>private</tt> to either <tt>protected</tt> or <tt>public</tt> and
     * therefore require the invocation opcode to be upgraded from INVOKESPECIAL
     * to INVOKEVIRTUAL.  
     */
    private final Set<Method> upgradedMethods = new HashSet<Method>();
    
    /**
     * Get the internal class name
     * 
     * @return class reference
     */
    abstract String getClassRef();

    /**
     * Get the class tree for this context
     * 
     * @return tree
     */
    abstract ClassNode getClassNode();

    /**
     * Get the meta class for this context
     * 
     * @return ClassInfo
     */
    abstract ClassInfo getClassInfo();
    
    /**
     * Add a method to this context which is private in the Mixin but has higher
     * visibility in the target class.
     * 
     * @param method method to add
     */
    void addUpgradedMethod(MethodNode method) {
        Method md = this.getClassInfo().findMethod(method);
        if (md == null) {
            // wat
            throw new IllegalStateException("Meta method for " + method.name + " not located in " + this);
        }
        this.upgradedMethods.add(md);
    }
    
    protected void upgradeMethods() {
        for (MethodNode method : this.getClassNode().methods) {
            this.upgradeMethod(method);
        }
    }

    private void upgradeMethod(MethodNode method) {
        for (Iterator<AbstractInsnNode> iter = method.instructions.iterator(); iter.hasNext();) {
            AbstractInsnNode insn = iter.next();
            if (!(insn instanceof MethodInsnNode)) {
                continue;
            }
            
            MemberRef methodRef = new MemberRef.Method((MethodInsnNode)insn);
            if (methodRef.getOwner().equals(this.getClassRef())) {
                Method md = this.getClassInfo().findMethod(methodRef.getName(), methodRef.getDesc(), ClassInfo.INCLUDE_ALL);
                this.upgradeMethodRef(method, methodRef, md);
            }
        }
    }

    protected void upgradeMethodRef(MethodNode containingMethod, MemberRef methodRef, Method method) {
        if (methodRef.getOpcode() != Opcodes.INVOKESPECIAL) {
            return;
        }
        
        if (this.upgradedMethods.contains(method)) {
            methodRef.setOpcode(Opcodes.INVOKEVIRTUAL);
        }
    }

}
