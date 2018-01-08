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

import java.util.Deque;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InjectionPoint.Selector;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.throwables.InjectionError;
import org.spongepowered.asm.mixin.injection.throwables.InvalidSliceException;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Annotations;

import com.google.common.base.Strings;

/**
 * Stores information about a defined method slice for a particular injector.
 */
public final class MethodSlice {
    
    /**
     * A read-only wrapper for an {@link InsnList} which only allows the segment
     * identified by {@link #start} and {@link #end} to be accessed. In essence
     * this class provides a <em>view</em> of the underlying InsnList.
     */
    static final class InsnListSlice extends ReadOnlyInsnList { 
    
        /**
         * ListIterator for the slice view, wraps an iterator returned by the
         * underlying {@link InsnList} and ensures that consumers can only
         * traverse the slice.
         * 
         * <p>Note that this doesn't handle changes in the underlying InsnList
         * which occur after instatiation, care should be taken not to modify
         * the list via other means whilst this iterator is in use.</p>
         */
        static class SliceIterator implements ListIterator<AbstractInsnNode> {
            
            /**
             * The underlying ListIterator returned by the parent list
             */
            private final ListIterator<AbstractInsnNode> iter;
            
            /**
             * Brackets
             */
            private int start, end;
            
            /**
             * Virtual index, used to keep track of bounds
             */
            private int index;
    
            public SliceIterator(ListIterator<AbstractInsnNode> iter, int start, int end, int index) {
                this.iter = iter;
                this.start = start;
                this.end = end;
                this.index = index;
            }
    
            /* (non-Javadoc)
             * @see java.util.ListIterator#hasNext()
             */
            @Override
            public boolean hasNext() {
                return this.index <= this.end && this.iter.hasNext();
            }
    
            /* (non-Javadoc)
             * @see java.util.ListIterator#next()
             */
            @Override
            public AbstractInsnNode next() {
                if (this.index > this.end) {
                    throw new NoSuchElementException();
                }
                this.index++;
                return this.iter.next();
            }
    
            /* (non-Javadoc)
             * @see java.util.ListIterator#hasPrevious()
             */
            @Override
            public boolean hasPrevious() {
                return this.index > this.start ;
            }
    
            /* (non-Javadoc)
             * @see java.util.ListIterator#previous()
             */
            @Override
            public AbstractInsnNode previous() {
                if (this.index <= this.start) {
                    throw new NoSuchElementException();
                }
                this.index--;
                return this.iter.previous();
            }
    
            /* (non-Javadoc)
             * @see java.util.ListIterator#nextIndex()
             */
            @Override
            public int nextIndex() {
                return this.index - this.start;
            }
    
            /* (non-Javadoc)
             * @see java.util.ListIterator#previousIndex()
             */
            @Override
            public int previousIndex() {
                return this.index - this.start - 1;
            }
    
            /* (non-Javadoc)
             * @see java.util.ListIterator#remove()
             */
            @Override
            public void remove() {
                throw new UnsupportedOperationException("Cannot remove insn from slice");
            }
    
            /* (non-Javadoc)
             * @see java.util.ListIterator#set(java.lang.Object)
             */
            @Override
            public void set(AbstractInsnNode e) {
                throw new UnsupportedOperationException("Cannot set insn using slice");
            }
    
            /* (non-Javadoc)
             * @see java.util.ListIterator#add(java.lang.Object)
             */
            @Override
            public void add(AbstractInsnNode e) {
                throw new UnsupportedOperationException("Cannot add insn using slice");
            }
        }
    
        /**
         * Brackets
         */
        private final int start, end;
        
        protected InsnListSlice(InsnList inner, int start, int end) {
            super(inner);
            
            // Start and end are validated prior to construction
            this.start = start;
            this.end = end;
        }
        
        /* (non-Javadoc)
         * @see org.spongepowered.asm.mixin.injection.code.ReadOnlyInsnList
         *      #iterator()
         */
        @Override
        public ListIterator<AbstractInsnNode> iterator() {
            return this.iterator(0);
        }
        
        /* (non-Javadoc)
         * @see org.spongepowered.asm.mixin.injection.code.ReadOnlyInsnList
         *      #iterator(int)
         */
        @Override
        public ListIterator<AbstractInsnNode> iterator(int index) {
            // Return the bracketed iterator
            return new SliceIterator(super.iterator(this.start + index), this.start, this.end, this.start + index);
        }

        /* (non-Javadoc)
         * @see org.spongepowered.asm.mixin.injection.code.ReadOnlyInsnList
         *      #toArray()
         */
        @Override
        public AbstractInsnNode[] toArray() {
            AbstractInsnNode[] all = super.toArray();
            AbstractInsnNode[] subset = new AbstractInsnNode[this.size()];
            System.arraycopy(all, this.start, subset, 0, subset.length);
            return subset;
        }

        /* (non-Javadoc)
         * @see org.spongepowered.asm.mixin.injection.code.ReadOnlyInsnList
         *      #size()
         */
        @Override
        public int size() {
            return (this.end - this.start) + 1;
        }

        /* (non-Javadoc)
         * @see org.spongepowered.asm.mixin.injection.code.ReadOnlyInsnList
         *      #getFirst()
         */
        @Override
        public AbstractInsnNode getFirst() {
            return super.get(this.start);
        }

        /* (non-Javadoc)
         * @see org.spongepowered.asm.mixin.injection.code.ReadOnlyInsnList
         *      #getLast()
         */
        @Override
        public AbstractInsnNode getLast() {
            return super.get(this.end);
        }

        /* (non-Javadoc)
         * @see org.spongepowered.asm.mixin.injection.code.ReadOnlyInsnList
         *      #get(int)
         */
        @Override
        public AbstractInsnNode get(int index) {
            return super.get(this.start + index);
        }

        /* (non-Javadoc)
         * @see org.spongepowered.asm.mixin.injection.code.ReadOnlyInsnList
         *      #contains(org.spongepowered.asm.lib.tree.AbstractInsnNode)
         */
        @Override
        public boolean contains(AbstractInsnNode insn) {
            for (AbstractInsnNode node : this.toArray()) {
                if (node == insn) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns the index of the specified instruction in the slice, use
         * {@link #realIndexOf} to determine the index of the instruction in the
         * underlying InsnList.
         * 
         * @param insn Instruction to inspect
         * @return instruction's index in the list
         */
        @Override
        public int indexOf(AbstractInsnNode insn) {
            int index = super.indexOf(insn);
            return index >= this.start && index <= this.end ? index - this.start : -1;
        }
        
        /**
         * Returns the index of the instruction in the underlying InsnLis
         * 
         * @param insn Instruction to inspect
         * @return instruction's index in the list
         */
        public int realIndexOf(AbstractInsnNode insn) {
            return super.indexOf(insn);
        }
        
    }
    
    /**
     * Make with the logging already
     */
    private static final Logger logger = LogManager.getLogger("mixin");

    /**
     * Owner of this slice
     */
    private final ISliceContext owner;
    
    /**
     * Slice ID as declared, slice ID in the parent {@link MethodSlices}
     * collection may be different
     */
    private final String id;
    
    /**
     * Injection point which defines the start of this slice (inclusive), may be
     * null as long as {@link #to} is not null
     */
    private final InjectionPoint from;
    
    /**
     * Injection point which defines the end of this slice (inclusive), may be
     * null as long as {@link #from} is not null
     */
    private final InjectionPoint to;
    
    /**
     * Descriptive name of the slice, used in exceptions
     */
    private final String name;

    /**
     * ctor
     * 
     * @param owner owner
     * @param id declared id
     * @param from start point, may be null as long as {@link #to} is not null
     * @param to end point, may be null as long as {@link #from} is not null
     */
    private MethodSlice(ISliceContext owner, String id, InjectionPoint from, InjectionPoint to) {
        if (from == null && to == null) {
            throw new InvalidSliceException(owner, String.format("%s is redundant. No 'from' or 'to' value specified", this));
        }

        this.owner = owner;
        this.id = Strings.nullToEmpty(id);
        this.from = from;
        this.to = to;
        this.name = MethodSlice.getSliceName(id);
    }
    
    /**
     * Get the <em>declared</em> id of this slice 
     */
    public String getId() {
        return this.id;
    }
    
    /**
     * Get a sliced insn list based on the parameters specified in this slice
     * 
     * @param method method to slice
     * @return read only slice
     */
    public ReadOnlyInsnList getSlice(MethodNode method) {
        int max = method.instructions.size() - 1;
        int start = this.find(method, this.from, 0, 0, this.name + "(from)");
        int end = this.find(method, this.to, max, start, this.name + "(to)");
        
        if (start > end) {
            throw new InvalidSliceException(this.owner, String.format("%s is negative size. Range(%d -> %d)", this.describe(), start, end));
        }
        
        if (start < 0 || end < 0 || start > max || end > max) {
            throw new InjectionError("Unexpected critical error in " + this + ": out of bounds start=" + start + " end=" + end + " lim=" + max);
        }
        
        if (start == 0 && end == max) {
            return new ReadOnlyInsnList(method.instructions);
        }
        
        return new InsnListSlice(method.instructions, start, end);
    }

    /**
     * Runs the specified injection point as a query on the method and returns
     * the index of the instruction matching the query. Returns the default
     * value if the query returns zero results.
     * 
     * @param method Method to query
     * @param injectionPoint Query to run
     * @param defaultValue Value to return if injection point is null (open
     *      ended)
     * @param failValue Value to use if query fails
     * @param description Description for error message
     * @return matching insn index
     */
    private int find(MethodNode method, InjectionPoint injectionPoint, int defaultValue, int failValue, String description) {
        if (injectionPoint == null) {
            return defaultValue;
        }
        
        Deque<AbstractInsnNode> nodes = new LinkedList<AbstractInsnNode>();
        ReadOnlyInsnList insns = new ReadOnlyInsnList(method.instructions);
        boolean result = injectionPoint.find(method.desc, insns, nodes);
        Selector select = injectionPoint.getSelector();
        if (nodes.size() != 1 && select == Selector.ONE) {
            throw new InvalidSliceException(this.owner, String.format("%s requires 1 result but found %d", this.describe(description), nodes.size()));
        }
        
        if (!result) {
            if (this.owner.getContext().getOption(Option.DEBUG_VERBOSE)) {
                MethodSlice.logger.warn("{} did not match any instructions", this.describe(description));
            }
            return failValue;
        }
        
        return method.instructions.indexOf(select == Selector.FIRST ? nodes.getFirst() : nodes.getLast());
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.describe();
    }

    private String describe() {
        return this.describe(this.name);
    }

    private String describe(String description) {
        return MethodSlice.describeSlice(description, this.owner);
    }

    private static String describeSlice(String description, ISliceContext owner) {
        String annotation = Bytecode.getSimpleName(owner.getAnnotation());
        MethodNode method = owner.getMethod();
        return String.format("%s->%s(%s)::%s%s", owner.getContext(), annotation, description, method.name, method.desc);
    }

    private static String getSliceName(String id) {
        return String.format("@Slice[%s]", Strings.nullToEmpty(id));
    }

    /**
     * Parses the supplied annotation into a MethodSlice
     * 
     * @param owner Owner injection info
     * @param slice Annotation to parse
     * @return parsed MethodSlice
     */
    public static MethodSlice parse(ISliceContext owner, Slice slice) {
        String id = slice.id();
        
        At from = slice.from();
        At to = slice.to();
        
        InjectionPoint fromPoint = from != null ? InjectionPoint.parse(owner, from) : null;
        InjectionPoint toPoint = to != null ? InjectionPoint.parse(owner, to) : null;
        
        return new MethodSlice(owner, id, fromPoint, toPoint);
    }

    /**
     * Parses the supplied annotation into a MethodSlice
     * 
     * @param info Owner injection info
     * @param node Annotation to parse
     * @return parsed MethodSlice
     */
    public static MethodSlice parse(ISliceContext info, AnnotationNode node) {
        String id = Annotations.<String>getValue(node, "id");
        
        AnnotationNode from = Annotations.<AnnotationNode>getValue(node, "from");
        AnnotationNode to = Annotations.<AnnotationNode>getValue(node, "to");
        
        InjectionPoint fromPoint = from != null ? InjectionPoint.parse(info, from) : null;
        InjectionPoint toPoint = to != null ? InjectionPoint.parse(info, to) : null;
        
        return new MethodSlice(info, id, fromPoint, toPoint);
    }

}
