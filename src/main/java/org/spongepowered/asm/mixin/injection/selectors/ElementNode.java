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
package org.spongepowered.asm.mixin.injection.selectors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.util.Handles;

import com.google.common.base.Strings;

/**
 * Wrapper for all node types supported by {@link ITargetSelector target
 * selectors} ({@link FieldNode}, {@link MethodNode}, {@link FieldInsnNode},
 * {@link MethodInsnNode} and {@link InvokeDynamicInsnNode}) which allows access
 * to common properties of things which are basically "arbitrary node with
 * owner, name and descriptor"
 * 
 * @param <TNode> node type
 */
public abstract class ElementNode<TNode> {
    
    /**
     * Element node type, returned by <tt>getType</tt> so consumers don't need
     * to do instanceof checks, and allows switching on element type in a more
     * expressive way
     */
    public static enum NodeType {
        
        /**
         * None or unknown type 
         */
        UNDEFINED(false, false, false),
        
        /**
         * A method node 
         */
        METHOD(true, false, false),
        
        /**
         * A field node 
         */
        FIELD(false, true, false),
        
        /**
         * An invoke instruction 
         */
        METHOD_INSN(false, false, true),
        
        /**
         * A get or put field instruction 
         */
        FIELD_INSN(false, false, true),
        
        /**
         * An INVOKEDYNAMIC instruction
         */
        INVOKEDYNAMIC_INSN(false, false, true);
        
        /**
         * Whether this node holds a method, implies that calling <tt>getMethod
         * </tt> will return a value
         */
        public final boolean hasMethod;
        
        /**
         * Whether this node holds a field, implies that calling <tt>getField
         * </tt> will return a value
         */
        public final boolean hasField;
        
        /**
         * Whether this node holds an insn, implies that calling <tt>getInsn
         * </tt> will return a value
         */
        public final boolean hasInsn;

        private NodeType(boolean isMethod, boolean isField, boolean isInsn) {
            this.hasMethod = isMethod;
            this.hasField = isField;
            this.hasInsn = isInsn;
        }
        
    }
    
    /**
     * ElementNode for MethodNode
     */
    static class ElementNodeMethod extends ElementNode<MethodNode> {

        private final ClassNode owner;

        private final MethodNode method;

        ElementNodeMethod(ClassNode owner, MethodNode method) {
            this.owner = owner;
            this.method = method;
        }
        
        @Override
        public NodeType getType() {
            return NodeType.METHOD;
        }
        
        @Override
        public MethodNode getMethod() {
            return this.method;
        }
        
        @Override
        public String getOwner() {
            return this.owner != null ? this.owner.name : null;
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
        public boolean equals(Object obj) {
            return this.method.equals(obj);
        }
        
        @Override
        public int hashCode() {
            return this.method.hashCode();
        }
        
    }
    
    /**
     * ElementNode for FieldNode
     */
    static class ElementNodeField extends ElementNode<FieldNode> {

        private final ClassNode owner;

        private final FieldNode field;

        ElementNodeField(ClassNode owner, FieldNode field) {
            this.owner = owner;
            this.field = field;
        }
        
        @Override
        public NodeType getType() {
            return NodeType.FIELD;
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
        public String getOwner() {
            return this.owner != null ? this.owner.name : null;
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
        public boolean equals(Object obj) {
            return this.field.equals(obj);
        }
        
        @Override
        public int hashCode() {
            return this.field.hashCode();
        }

    }
    
    /**
     * ElementNode for MethodInsnNode
     */
    static class ElementNodeMethodInsn extends ElementNode<MethodInsnNode> {
        
        private MethodInsnNode insn;
        
        ElementNodeMethodInsn(MethodInsnNode method) {
            this.insn = method;
        }
        
        @Override
        public NodeType getType() {
            return NodeType.METHOD_INSN;
        }
        
        @Override
        public AbstractInsnNode getInsn() {
            return this.insn;
        }
        
        @Override
        public String getOwner() {
            return this.insn.owner;
        }

        @Override
        public String getName() {
            return this.insn.name;
        }
        
        @Override
        public String getDesc() {
            return this.insn.desc;
        }
        
        @Override
        public String getSignature() {
            return null;
        }
        
        @Override
        public MethodInsnNode get() {
            return this.insn;
        }
        
        @Override
        public boolean equals(Object obj) {
            return this.insn.equals(obj);
        }
        
        @Override
        public int hashCode() {
            return this.insn.hashCode();
        }

    }
    
    /**
     * ElementNode for InvokeDynamicInsnNode
     */
    static class ElementNodeInvokeDynamicInsn extends ElementNode<InvokeDynamicInsnNode> {
        
        private InvokeDynamicInsnNode insn;
        
        private Type samMethodType;
        
        private Handle implMethod;
        
        private Type instantiatedMethodType;
        
        ElementNodeInvokeDynamicInsn(InvokeDynamicInsnNode invokeDynamic) {
            this.insn = invokeDynamic;
            
            if (invokeDynamic.bsmArgs != null && invokeDynamic.bsmArgs.length > 1) {
                Object samMethodType = invokeDynamic.bsmArgs[0];
                Object implMethod = invokeDynamic.bsmArgs[1];
                Object instantiatedMethodType = invokeDynamic.bsmArgs[2];
                if (samMethodType instanceof Type && implMethod instanceof Handle && instantiatedMethodType instanceof Type) {
                    this.samMethodType = (Type)samMethodType;
                    this.implMethod = (Handle)implMethod;
                    this.instantiatedMethodType = (Type)instantiatedMethodType;
                }
            }
        }
        
        @Override
        public NodeType getType() {
            return NodeType.INVOKEDYNAMIC_INSN;
        }
        
        @Override
        public boolean isField() {
            return this.implMethod != null && Handles.isField(this.implMethod);
        }
        
        @Override
        public AbstractInsnNode getInsn() {
            return this.insn;
        }
        
        @Override
        public String getOwner() {
            return this.implMethod != null ? this.implMethod.getOwner() : this.insn.name;
        }

        @Override
        public String getName() {
            return this.insn.name;
        }
        
        @Override
        public String getSyntheticName() {
            return this.implMethod != null ? this.implMethod.getName() : this.insn.name;
        }
        
        @Override
        public String getDesc() {
            return this.implMethod != null ? this.implMethod.getDesc() : this.insn.desc;
        }
        
        @Override
        public String getDelegateDesc() {
            return this.samMethodType != null ? this.samMethodType.getDescriptor() : this.getDesc();
        }
        
        @Override
        public String getImplDesc() {
            return this.instantiatedMethodType != null ? this.instantiatedMethodType.getDescriptor() : this.getDesc();
        }
        
        @Override
        public String getSignature() {
            return null;
        }
        
        @Override
        public InvokeDynamicInsnNode get() {
            return this.insn;
        }
        
        @Override
        public boolean equals(Object obj) {
            return this.insn.equals(obj);
        }
        
        @Override
        public int hashCode() {
            return this.insn.hashCode();
        }

    }
    
    /**
     * ElementNode for FieldInsnNode
     */
    static class ElementNodeFieldInsn extends ElementNode<FieldInsnNode> {
        
        private FieldInsnNode insn;
        
        ElementNodeFieldInsn(FieldInsnNode field) {
            this.insn = field;
        }
        
        @Override
        public NodeType getType() {
            return NodeType.FIELD_INSN;
        }
        
        @Override
        public boolean isField() {
            return true;
        }

        @Override
        public AbstractInsnNode getInsn() {
            return this.insn;
        }
        
        @Override
        public String getOwner() {
            return this.insn.owner;
        }

        @Override
        public String getName() {
            return this.insn.name;
        }
        
        @Override
        public String getDesc() {
            return this.insn.desc;
        }
        
        @Override
        public String getSignature() {
            return null;
        }
        
        @Override
        public FieldInsnNode get() {
            return this.insn;
        }
        
        @Override
        public boolean equals(Object obj) {
            return this.insn.equals(obj);
        }
        
        @Override
        public int hashCode() {
            return this.insn.hashCode();
        }

    }
    
    /**
     * Wrapper iterator for method insns
     */
    static class ElementNodeIterator implements Iterator<ElementNode<AbstractInsnNode>> {
        
        private final Iterator<AbstractInsnNode> iter;
        
        private final boolean filterDynamic;
        
        ElementNodeIterator(Iterator<AbstractInsnNode> iter, boolean filterDynamic) {
            this.iter = iter;
            this.filterDynamic = filterDynamic;
        }

        @Override
        public boolean hasNext() {
            return this.iter.hasNext();
        }

        @Override
        public ElementNode<AbstractInsnNode> next() {
            AbstractInsnNode elem = this.iter.next();
            return !this.filterDynamic || (elem != null && elem.getOpcode() == Opcodes.INVOKEDYNAMIC) ? ElementNode.of(elem) : null;
        }
        
    }
    
    /**
     * Wrapper for InsnList which returns node iterator
     */
    static class ElementNodeIterable implements Iterable<ElementNode<AbstractInsnNode>> {
        
        private final Iterable<AbstractInsnNode> iterable;

        private final boolean filterDynamic;
        
        public ElementNodeIterable(Iterable<AbstractInsnNode> iterable, boolean filterDynamic) {
            this.iterable = iterable;
            this.filterDynamic = filterDynamic;
}

        @Override
        public Iterator<ElementNode<AbstractInsnNode>> iterator() {
            return new ElementNodeIterator(this.iterable.iterator(), this.filterDynamic);
        }
        
    }
    
    /**
     * Get whether this element is a field type and the descriptor is a bare
     * type descriptor without arguments. Otherwise assumes the descriptor is a
     * method descriptor. 
     */
    public boolean isField() {
        return false;
    }
    
    /**
     * Get the type of this ElementNode, the return value can be used to
     * determine which accessor ({@link #getMethod}, {@link #getField}
     */
    public abstract NodeType getType();
    
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
     * Get the {@link AbstractInsnNode instruction} if this member is an insn,
     * otherwise returns <tt>null</tt>
     */
    public AbstractInsnNode getInsn() {
        return null;
    }
    
    /**
     * Get the element owner's name, if this element has an owner, otherwise
     * returns <tt>null</tt>
     */
    public abstract String getOwner();
    
    /**
     * Get the element name
     */
    public abstract String getName();
    
    /**
     * Get the synthetic element name. For INVOKEDYNAMIC elements this is the
     * real name of the lambda method implementing the delegate.
     */
    public String getSyntheticName() {
        return this.getName();
    }
    
    /**
     * Get the element descriptor. For INVOKEDYNAMIC this is the full descriptor
     * of the lambda (including prepended captures).
     */
    public abstract String getDesc();
    
    /**
     * For INVOKEDYNAMIC elements, returns original descriptor of the delegate. 
     */
    public String getDelegateDesc() {
        return this.getDesc();
    }
    
    /**
     * For INVOKEDYNAMIC elements, returns specialised descriptor of the
     * delegate (lambda descriptor without prepended captures), can be the same
     * as the delegate descriptor or more specialised. 
     */
    public String getImplDesc() {
        return this.getDesc();
    }
    
    /**
     * Get the element signature, can be <tt>null</tt>
     */
    public abstract String getSignature();
    
    /**
     * Returns the node with horrible duck typing
     */
    public abstract TNode get();
    
    @Override
    public String toString() {
        String desc = Strings.nullToEmpty(this.getDesc());
        if (!desc.isEmpty() && this.isField()) {
            desc = ":" + desc;
        }
        String owner = Strings.nullToEmpty(this.getOwner());
        if (!owner.isEmpty()) {
            owner = "L" + owner + ";";
        }
        return String.format("%s%s%s", owner, Strings.nullToEmpty(this.getName()), desc);
    }

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
        if (node instanceof ElementNode) {
            return (ElementNode<TNode>)node;
        } else if (node instanceof MethodNode) {
            return (ElementNode<TNode>)new ElementNodeMethod(owner, (MethodNode)node);
        } else if (node instanceof FieldNode) {
            return (ElementNode<TNode>)new ElementNodeField(owner, (FieldNode)node);
        } else if (node instanceof MethodInsnNode) {
            return (ElementNode<TNode>)new ElementNodeMethodInsn((MethodInsnNode)node);
        } else if (node instanceof InvokeDynamicInsnNode) {
            return (ElementNode<TNode>)new ElementNodeInvokeDynamicInsn((InvokeDynamicInsnNode)node);
        } else if (node instanceof FieldInsnNode) {
            return (ElementNode<TNode>)new ElementNodeFieldInsn((FieldInsnNode)node);
        }
        throw new IllegalArgumentException("Could not create ElementNode for unknown node type: " + node.getClass().getName());
    }
    
    /**
     * Create an ElementNode wrapper for the supplied node object
     * 
     * @param node Node to wrap
     * @param <TNode> Node type
     * @return ElementNode
     * @throws IllegalArgumentException if the supplied argument is not a
     *      {@link MethodNode} or {@link FieldNode}
     */
    @SuppressWarnings("unchecked")
    public static <TNode extends AbstractInsnNode> ElementNode<TNode> of(TNode node) {
        if (node instanceof MethodInsnNode) {
            return (ElementNode<TNode>)new ElementNodeMethodInsn((MethodInsnNode)node);
        } else if (node instanceof InvokeDynamicInsnNode) {
            return (ElementNode<TNode>)new ElementNodeInvokeDynamicInsn((InvokeDynamicInsnNode)node);
        } else if (node instanceof FieldInsnNode) {
            return (ElementNode<TNode>)new ElementNodeFieldInsn((FieldInsnNode)node);
        }
        return null;
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
    
    /**
     * Get a wrapped version of the supplied insn list which returns element
     * nodes for each (supported) instruction (FieldInsnNode, MethodInsnNode and
     * InvokeDynamicInsnNode). 
     * 
     * @param insns Insn list to wrap
     * @return Wrapper for insn list
     */
    public static Iterable<ElementNode<AbstractInsnNode>> insnList(InsnList insns) {
        return new ElementNodeIterable(insns, false);
    }
    
    /**
     * Get a wrapped version of the supplied insn list which returns element
     * nodes for every INVOKEDYNAMIC instruction only
     * 
     * @param insns Insn list to wrap
     * @return Wrapper for insn list
     */
    public static Iterable<ElementNode<AbstractInsnNode>> dynamicInsnList(InsnList insns) {
        return new ElementNodeIterable(insns, true);
    }
    
}
