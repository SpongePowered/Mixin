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
package org.spongepowered.asm.util.asm;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Holder for {@link FieldNode} and {@link MethodNode} for consumers which can
 * handle both types (since they have a lot in common), with common accessors
 * for overlapping properties.
 * 
 * @param <TNode> node type
 */
public abstract class ElementNode<TNode> {
    
    /**
     * ElementNode for MethodNode
     */
    static class ElementNodeMethod extends ElementNode<MethodNode> {
        
        private MethodNode method;

        ElementNodeMethod(ClassNode owner, MethodNode method) {
            super(owner);
            this.method = method;
        }
        
        @Override
        public boolean isMethod() {
            return true;
        }
        
        @Override
        public MethodNode getMethod() {
            return this.method;
        }
        
        @Override
        public String getName() {
            return this.method.name;
        }
        
        @Override
        public String getDesc() {
            return this.method.desc;
        }
        
        @Override
        public String getSignature() {
            return this.method.signature;
        }

        @Override
        public MethodNode get() {
            return this.method;
        }
        
        @Override
        public String toString() {
            return String.format("MethodElement[%s%s]", this.method.name, this.method.desc);
        }
        
    }
    
    /**
     * ElementNode for FieldNode
     */
    static class ElementNodeField extends ElementNode<FieldNode> {
        
        private FieldNode field;

        ElementNodeField(ClassNode owner, FieldNode field) {
            super(owner);
            this.field = field;
        }
        
        @Override
        public boolean isField() {
            return true;
        }
        
        @Override
        public FieldNode getField() {
            return this.field;
        }
        
        @Override
        public String getName() {
            return this.field.name;
        }
        
        @Override
        public String getDesc() {
            return this.field.desc;
        }
        
        @Override
        public String getSignature() {
            return this.field.signature;
        }

        @Override
        public FieldNode get() {
            return this.field;
        }
        
        @Override
        public String toString() {
            return String.format("FieldElement[%s:%s]", this.field.name, this.field.desc);
        }

    }

    /**
     * The class which owns this element, can be <tt>null</tt>
     */
    private final ClassNode owner;
    
    protected ElementNode(ClassNode owner) {
        this.owner = owner;
    }
    
    /**
     * Get whether this element is a method
     */
    public boolean isMethod() {
        return false;
    }
    
    /**
     * get whether this element is a field
     */
    public boolean isField() {
        return false;
    }
    
    /**
     * Get the {@link MethodNode} if this member is a method, otherwise returns
     * <tt>null</tt>
     */
    public MethodNode getMethod() {
        return null;
    }

    /**
     * Get the {@link FieldNode} if this member is a field, otherwise returns
     * <tt>null</tt>
     */
    public FieldNode getField() {
        return null;
    }
    
    /**
     * Get the element's owner, can be <tt>null</tt>
     */
    public ClassNode getOwner() {
        return this.owner;
    }
    
    /**
     * Get the element owner's name, if this element has an owner, otherwise
     * returns <tt>null</tt>
     */
    public String getOwnerName() {
        return this.owner != null ? this.owner.name : null;
    }
    
    /**
     * Get the element name
     */
    public abstract String getName();
    
    /**
     * Get the element descriptor
     */
    public abstract String getDesc();
    
    /**
     * Get the element signature, can be <tt>null</tt>
     */
    public abstract String getSignature();
    
    /**
     * Returns the node with horrible duck typing
     */
    public abstract TNode get();
    
    /**
     * Create an ElementNode wrapper for the supplied method node
     * 
     * @param owner class which owns the method or <tt>null</tt>
     * @param method Method node to wrap
     * @return ElementNode
     */
    public static ElementNode<MethodNode> of(ClassNode owner, MethodNode method) {
        return new ElementNodeMethod(owner, method);
    }
    
    /**
     * Create an ElementNode wrapper for the supplied field node
     * 
     * @param owner class which owns the field or <tt>null</tt>
     * @param field Field node to wrap
     * @return ElementNode
     */
    public static ElementNode<FieldNode> of(ClassNode owner, FieldNode field) {
        return new ElementNodeField(owner, field);
    }
    
    /**
     * Create an ElementNode wrapper for the supplied node object
     * 
     * @param owner class which owns the node or <tt>null</tt>
     * @param node Node to wrap
     * @param <TNode> Node type
     * @return ElementNode
     * @throws IllegalArgumentException if the supplied argument is not a
     *      {@link MethodNode} or {@link FieldNode}
     */
    @SuppressWarnings("unchecked")
    public static <TNode> ElementNode<TNode> of(ClassNode owner, TNode node) {
        if (node instanceof MethodNode) {
            return (ElementNode<TNode>)new ElementNodeMethod(owner, (MethodNode)node);
        } else if (node instanceof FieldNode) {
            return (ElementNode<TNode>)new ElementNodeField(owner, (FieldNode)node);
        }
        throw new IllegalArgumentException("Could not create ElementNode for unknown node type: " + node.getClass().getName());
    }

    /**
     * Convert the supplied list of nodes to a list of wrapped ElementNodes
     * 
     * @param owner Owner of the supplied nodes, can be <tt>null</tt>
     * @param list List of nodes
     * @param <TNode> Node type
     * @return List of wrapped nodes
     */
    public static <TNode> List<ElementNode<TNode>> listOf(ClassNode owner, List<TNode> list) {
        List<ElementNode<TNode>> nodes = new ArrayList<ElementNode<TNode>>();
        for (TNode node : list) {
            nodes.add(ElementNode.<TNode>of(owner, node));
        }
        return nodes;
    }

    /**
     * Get a list of wrapped ElementNodes for the fields of the supplied owner
     * class
     * 
     * @param owner Class to get fields, must not be <tt>null</tt>
     * @return List of wrapped nodes
     */
    public static List<ElementNode<FieldNode>> fieldList(ClassNode owner) {
        List<ElementNode<FieldNode>> fields = new ArrayList<ElementNode<FieldNode>>();
        for (FieldNode field : owner.fields) {
            fields.add(new ElementNodeField(owner, field));
        }
        return fields;
    }

    /**
     * Get a list of wrapped ElementNodes for the methods of the supplied owner
     * class
     * 
     * @param owner Class to get methods, must not be <tt>null</tt>
     * @return List of wrapped nodes
     */
    public static List<ElementNode<MethodNode>> methodList(ClassNode owner) {
        List<ElementNode<MethodNode>> methods = new ArrayList<ElementNode<MethodNode>>();
        for (MethodNode method : owner.methods) {
            methods.add(new ElementNodeMethod(owner, method));
        }
        return methods;
    }
    
}
