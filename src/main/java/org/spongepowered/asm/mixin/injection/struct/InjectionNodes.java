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
package org.spongepowered.asm.mixin.injection.struct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.util.Bytecode;

/**
 * Used to keep track of instruction nodes in a {@link Target} method which are
 * targetted by various types of injector. This collection is populated during
 * the first injector pass and allows injectors to keep track of their targets
 * even when the target method is being manipulated by other injectors.
 */
public class InjectionNodes extends ArrayList<InjectionNodes.InjectionNode> {
    
    /**
     * Never gonna give you up 
     */
    private static final long serialVersionUID = 1L;

    /**
     * A node targetted by one or more injectors. Using this wrapper allows
     * injectors to be aware of when their target node is removed or replace by
     * another injector. It also allows injectors to decorate certain nodes with
     * custom metadata to allow arbitration between injectors to take place.
     */
    public static class InjectionNode implements Comparable<InjectionNode> {
        
        /**
         * Next unique id 
         */
        private static int nextId = 0;
        
        /**
         * Injection node unique id
         */
        private final int id;
        
        /**
         * The original node targetted
         */
        private final AbstractInsnNode originalTarget;
        
        /**
         * Initially set to the {@link #originalTarget}, if an injector removes
         * or replaces this node then points to the current target. Can be null
         * if the node is removed.
         */
        private AbstractInsnNode currentTarget;
        
        /**
         * Injector decorations on this node
         */
        private Map<String, Object> decorations;
        
        /**
         * Create a new node wrapper for the specified target node
         * 
         * @param node target node
         */
        public InjectionNode(AbstractInsnNode node) {
            this.currentTarget = this.originalTarget = node;
            this.id = InjectionNode.nextId++;
        }
        
        /**
         * Get the unique id for this injector
         */
        public int getId() {
            return this.id;
        }
        
        /**
         * Get the original target of this node
         */
        public AbstractInsnNode getOriginalTarget() {
            return this.originalTarget;
        }
        
        /**
         * Get the current target of this node, can be null if the node was
         * replaced
         */
        public AbstractInsnNode getCurrentTarget() {
            return this.currentTarget;
        }
        
        /**
         * Replace this node with the specified target
         * 
         * @param target new node
         */
        public InjectionNode replace(AbstractInsnNode target) {
            this.currentTarget = target;
            return this;
        }
        
        /**
         * Remove the node
         */
        public InjectionNode remove() {
            this.currentTarget = null;
            return this;
        }
        
        /**
         * Checks whether the original or current target of this node match the
         * specified node
         * 
         * @param node node to check
         * @return true if the supplied node matches either of this node's
         *      internal identities
         */
        public boolean matches(AbstractInsnNode node) {
            return this.originalTarget == node || this.currentTarget == node;
        }
        
        /**
         * Get whether this node has been replaced
         */
        public boolean isReplaced() {
            return this.originalTarget != this.currentTarget;
        }

        /**
         * Get whether this node has been removed
         */
        public boolean isRemoved() {
            return this.currentTarget == null;
        }
        
        /**
         * Decorate this node with arbitrary metadata for injector arbitration
         * 
         * @param key meta key
         * @param value meta value
         * @param <V> value type
         */
        public <V> InjectionNode decorate(String key, V value) {
            if (this.decorations == null) {
                this.decorations = new HashMap<String, Object>();
            }
            this.decorations.put(key, value);
            return this;
        }
        
        /**
         * Get whether this node is decorated with the specified key
         * 
         * @param key meta key
         * @return true if the specified decoration exists
         */
        public boolean hasDecoration(String key) {
            return this.decorations != null && this.decorations.get(key) != null;
        }
        
        /**
         * Get the specified decoration
         * 
         * @param key meta key
         * @return decoration value or null if absent
         * @param <V> value type
         */
        @SuppressWarnings("unchecked")
        public <V> V getDecoration(String key) {
            return (V) (this.decorations == null ? null : this.decorations.get(key));
        }

        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        public int compareTo(InjectionNode other) {
            return other == null ? Integer.MAX_VALUE : this.hashCode() - other.hashCode();
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return String.format("InjectionNode[%s]", Bytecode.describeNode(this.currentTarget).replaceAll("\\s+", " "));
        }
        
    }

    /**
     * Add a tracked node to this collection if it does not already exist
     * 
     * @param node Instruction node to add
     * @return wrapper for the specified node
     */
    public InjectionNode add(AbstractInsnNode node) {
        InjectionNode injectionNode = this.get(node);
        if (injectionNode == null) {
            injectionNode = new InjectionNode(node);
            this.add(injectionNode);
        }
        return injectionNode;
    }
    
    /**
     * Get a tracked node from this collection if it already exists, returns
     * null if the node is not tracked
     * 
     * @param node instruction node
     * @return wrapper node or null if not tracked
     */
    public InjectionNode get(AbstractInsnNode node) {
        for (InjectionNode injectionNode : this) {
            if (injectionNode.matches(node)) {
                return injectionNode;
            }
        }
        return null;
    }
    
    /**
     * Get whether this collection contains a mapping for the specified insn
     * 
     * @param node instruction node to check
     * @return true if a wrapper exists for the node
     */
    public boolean contains(AbstractInsnNode node) {
        return this.get(node) != null;
    }

    /**
     * Replace the specified node with the new node, does not update the wrapper
     * if no wrapper exists for <tt>oldNode</tt>
     * 
     * @param oldNode node being replaced
     * @param newNode node to replace with
     */
    public void replace(AbstractInsnNode oldNode, AbstractInsnNode newNode) {
        InjectionNode injectionNode = this.get(oldNode);
        if (injectionNode != null) {
            injectionNode.replace(newNode);
        }        
    }
    
    /**
     * Mark the specified node as removed, does not update the wrapper if no
     * wrapper exists
     * 
     * @param node node being removed
     */
    public void remove(AbstractInsnNode node) {
        InjectionNode injectionNode = this.get(node);
        if (injectionNode != null) {
            injectionNode.remove();
        }        
    }
    
}
