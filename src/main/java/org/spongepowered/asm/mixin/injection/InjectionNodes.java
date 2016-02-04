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
package org.spongepowered.asm.mixin.injection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.util.ASMHelper;

public class InjectionNodes extends ArrayList<InjectionNodes.InjectionNode> {
    
    private static final long serialVersionUID = 1L;

    public static class InjectionNode implements Comparable<InjectionNode> {
        
        private final AbstractInsnNode originalTarget;
        
        private AbstractInsnNode currentTarget;
        
        private Map<Object, Object> decorations;
        
        public InjectionNode(AbstractInsnNode target) {
            this.currentTarget = this.originalTarget = target;
        }
        
        public AbstractInsnNode getOriginalTarget() {
            return this.originalTarget;
        }
        
        public AbstractInsnNode getCurrentTarget() {
            return this.currentTarget;
        }
        
        public void replace(AbstractInsnNode currentTarget) {
            this.currentTarget = currentTarget;
        }
        
        public void remove() {
            this.currentTarget = null;
        }
        
        public boolean matches(AbstractInsnNode node) {
            return this.originalTarget == node || this.currentTarget == node;
        }
        
        public boolean isReplaced() {
            return this.originalTarget != this.currentTarget;
        }

        public boolean isRemoved() {
            return this.currentTarget == null;
        }
        
        public <K, V> void decorate(K key, V value) {
            if (this.decorations == null) {
                this.decorations = new HashMap<Object, Object>();
            }
            this.decorations.put(key, value);
        }
        
        public <K> boolean hasDecoration(K key) {
            return this.decorations != null && this.decorations.get(key) != null;
        }
        
        @SuppressWarnings("unchecked")
        public <K, V> V getDecoration(K key) {
            return (V) (this.decorations == null ? null : this.decorations.get(key));
        }

        @Override
        public int compareTo(InjectionNode other) {
            return other == null ? Integer.MAX_VALUE : this.hashCode() - other.hashCode();
        }
        
        @Override
        public String toString() {
            return String.format("InjectionNode[%s]", ASMHelper.getNodeDescriptionForDebug(this.currentTarget));
        }
        
    }

    public InjectionNode add(AbstractInsnNode node) {
        InjectionNode injectionNode = this.get(node);
        if (injectionNode == null) {
            injectionNode = new InjectionNode(node);
            this.add(injectionNode);
        }
        return injectionNode;
    }
    
    public InjectionNode get(AbstractInsnNode node) {
        for (InjectionNode injectionNode : this) {
            if (injectionNode.matches(node)) {
                return injectionNode;
            }
        }
        return null;
    }
    
    public boolean contains(AbstractInsnNode node) {
        return this.get(node) != null;
    }

    public void replace(AbstractInsnNode oldNode, AbstractInsnNode newNode) {
        InjectionNode injectionNode = this.get(oldNode);
        if (injectionNode != null) {
            injectionNode.replace(newNode);
        }        
    }
    
    public void remove(AbstractInsnNode oldNode) {
        InjectionNode injectionNode = this.get(oldNode);
        if (injectionNode != null) {
            injectionNode.remove();
        }        
    }
    
}
