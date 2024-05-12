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
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.points.BeforeNew;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.Locals.SyntheticLocalVariableNode;

/**
 * Information about the current injection target (method) which bundles common
 * injection context with the target method in order to allow injectors to
 * interoperate.
 */
public class Target implements Comparable<Target>, Iterable<AbstractInsnNode> {
    
    /**
     * A stateful extension of stack or locals space for a target method which
     * can be applied.
     * 
     * <p>Stack and locals extensions are generally localised to a particular
     * injection. This means they don't necessarily need to accumulate, it's
     * generally sufficient to expand the method's <em>original</em> stack or
     * locals size by the extension amount, with the largest extension
     * determining the final stack size of the method.</p>
     * 
     * <p>Before this mechanism, injectors generally needed to keep track of
     * their extension sizes via arbitrary integers, then apply them in one go.
     * However some injector logic made the flow of these integers hard to track
     * and lead to insidious bugs when values which should be combined were
     * instead applied sequentially. The minor performance trade-off of using
     * extensions to track these operations is covered by the increase in
     * robustness.</p>
     */
    public class Extension {
        
        /**
         * True if this is a locals extension, false for stack.
         */
        private final boolean locals;
        
        /**
         * The current 'depth' of this extension. Discrete operations maintain
         * their own size value. When an extension is applied this value resets
         * to zero so that the same extension can be used for multiple
         * operations.
         */
        private int size;
        
        Extension(boolean locals) {
            this.locals = locals;
        }

        /**
         * Add 1 to this extension
         * 
         * @return fluent interface
         */
        public Extension add() {
            this.size++;
            return this;
        }
        
        /**
         * Add the specified size to this extension
         * 
         * @param size size to add
         * @return fluent interface
         */
        public Extension add(int size) {
            this.size += size;
            return this;
        }
        
        /**
         * Add the size of the supplied type array to this extension. The size
         * of the supplied args will first be computed, then the result will be
         * added to the extension.
         * 
         * @param types Types to add
         * @return fluent interface
         */
        public Extension add(Type[] types) {
            return this.add(Bytecode.getArgsSize(types));
        }
        
        /**
         * Set the size of this extension
         * 
         * @param size Size to set
         * @return fluent interface
         */
        public Extension set(int size) {
            this.size = size;
            return this;
        }
        
        /**
         * Get the current size of this extension
         * 
         * @return fluent interface
         */
        public int get() {
            return this.size;
        }
        
        /**
         * Apply this extension to the target. This reduces the current size to
         * zero so that a new operation can begin if required.
         */
        public void apply() {
            if (this.locals) {
                Target.this.extendLocalsBy(this.size);
            } else {
                Target.this.extendStackBy(this.size);
            }
            this.size = 0;
        }
        
    }
    
    /**
     * Target class info
     */
    public final ClassInfo classInfo;

    /**
     * Target class node
     */
    public final ClassNode classNode;

    /**
     * Target method
     */
    public final MethodNode method;
    
    /**
     * Method instructions
     */
    public final InsnList insns;
    
    /**
     * True if the method is static 
     */
    public final boolean isStatic;
    
    /**
     * Method arguments
     */
    public final Type[] arguments;
    
    /**
     * Return type computed from the method descriptor 
     */
    public final Type returnType;

    /**
     * Method's (original) MAXS 
     */
    private final int maxStack;
    
    /**
     * Method's original max locals 
     */
    private final int maxLocals;
    
    /**
     * Nodes targetted by injectors 
     */
    private final InjectionNodes injectionNodes = new InjectionNodes();

    /**
     * Callback info class
     */
    private String callbackInfoClass;
    
    /**
     * Callback method descriptor based on this target 
     */
    private String callbackDescriptor;
    
    /**
     * Method argument slots 
     */
    private int[] argIndices;

    /**
     * Local variables used for argmap slots
     */
    private List<Integer> argMapVars;
    
    /**
     * Labels for LVT ranges, generated as needed 
     */
    private LabelNode start, end;

    /**
     * Make a new Target for the supplied method
     * 
     * @param method target method
     */
    Target(ClassInfo classInfo, ClassNode classNode, MethodNode method) {
        this.classInfo = classInfo;
        this.classNode = classNode;
        this.method = method;
        this.insns = method.instructions;
        this.isStatic = Bytecode.isStatic(method);
        this.arguments = Type.getArgumentTypes(method.desc);

        this.returnType = Type.getReturnType(method.desc);
        this.maxStack = method.maxStack;
        this.maxLocals = method.maxLocals;
    }
    
    /**
     * Add an injection node to this target if it does not already exist,
     * returns the existing node if it exists
     * 
     * @param node Instruction node to add
     * @return wrapper for the specified node
     */
    public InjectionNode addInjectionNode(AbstractInsnNode node) {
        return this.injectionNodes.add(node);
    }
    
    /**
     * Get an injection node from this collection if it already exists, returns
     * null if the node is not tracked
     * 
     * @param node instruction node
     * @return wrapper node or null if not tracked
     */
    public InjectionNode getInjectionNode(AbstractInsnNode node) {
        return this.injectionNodes.get(node);
    }
    
    /**
     * Get the target method name
     */
    public String getName() {
        return this.method.name;
    }
    
    /**
     * Get the target method descriptor
     */
    public String getDesc() {
        return this.method.desc;
    }
    
    /**
     * Get the target method signature
     */
    public String getSignature() {
        return this.method.signature;
    }
    
    /**
     * Get the original max locals of the method
     * 
     * @return the original max locals value
     */
    public int getMaxLocals() {
        return this.maxLocals;
    }
    
    /**
     * Get the original max stack of the method
     * 
     * @return the original max stack value
     */
    public int getMaxStack() {
        return this.maxStack;
    }
    
    /**
     * Get the current max locals of the method
     * 
     * @return the current max local value
     */
    public int getCurrentMaxLocals() {
        return this.method.maxLocals;
    }
    
    /**
     * Get the current max stack of the method
     * 
     * @return the current max stack value
     */
    public int getCurrentMaxStack() {
        return this.method.maxStack;
    }
    
    /**
     * Allocate a new local variable for the method
     * 
     * <p>Note that <b>allocating</b> a local differs from <em>extending</em>
     * locals (see {@link #extendLocals()}) in that the allocation immediately
     * increases the target max locals and returns the new index of the local
     * just allocated. <b>Extending</b> locals is used for temporary localised
     * extensions to the locals table for the purposes of injectors.</p>
     * 
     * @return the allocated local index
     */
    public int allocateLocal() {
        return this.allocateLocals(1);
    }
    
    /**
     * Allocate a number of new local variables for this method, returns the
     * first local variable index of the allocated range.
     * 
     * <p>Note that <b>allocating</b> locals differs from <em>extending</em>
     * locals (see {@link #extendLocals()}) in that the allocation immediately
     * increases the target max locals and returns the new index of the local
     * just allocated. <b>Extending</b> locals is used for temporary localised
     * extensions to the locals table for the purposes of injectors.</p>
     * 
     * @param locals number of locals to allocate
     * @return the first local variable index of the allocated range
     */
    public int allocateLocals(int locals) {
        int nextLocal = this.method.maxLocals;
        this.method.maxLocals += locals;
        return nextLocal;
    }
    
    /**
     * Creates a new locals extension. The returned object can be used to create
     * extensions to the method locals for an injector to use. Extensions are
     * not designed to overlap, and the largest extension for a method will
     * determine the amount by which the locals table is extended.
     * 
     * @return extension handle
     */
    public Extension extendLocals() {
        return new Extension(true);
    }
    
    /**
     * Creates a new stack extension. The returned object can be used to create
     * extensions to the method stack for an injector to use. Extensions are
     * not designed to overlap, and the largest extension for a method will
     * determine the amount by which the stack is extended.
     * 
     * @return extension handle
     */
    public Extension extendStack() {
        return new Extension(false);
    }

    /**
     * Allocate a number of new local variables for this method
     * 
     * @param locals number of locals to allocate
     */
    void extendLocalsBy(int locals) {
        this.setMaxLocals(this.maxLocals + locals);
    }

    /**
     * Set the maxlocals for this target to the specified value, the specfied
     * value must be higher than the original max locals
     * 
     * @param maxLocals max locals value to set
     */
    private void setMaxLocals(int maxLocals) {
        if (maxLocals > this.method.maxLocals) {
            this.method.maxLocals = maxLocals;
        }
    }

    /**
     * Allocate a number of new stack variables for this method
     * 
     * @param stack number of stack entries to allocate
     */
    void extendStackBy(int stack) {
        this.setMaxStack(this.maxStack + stack);
    }

    /**
     * Set the max stack size for this target to the specified value, the
     * specfied value must be higher than the original max stack
     * 
     * @param maxStack max stack value to set
     */
    private void setMaxStack(int maxStack) {
        if (maxStack > this.method.maxStack) {
            this.method.maxStack = maxStack;
        }
    }

    /**
     * Generate an array containing local indexes for the specified args,
     * returns an array of identical size to the supplied array with an
     * allocated local index in each corresponding position
     * 
     * @param args Argument types
     * @param start starting index
     * @return array containing a corresponding local arg index for each member
     *      of the supplied args array
     */
    public int[] generateArgMap(Type[] args, int start) {
        if (this.argMapVars == null) {
            this.argMapVars = new ArrayList<Integer>();
        }
        
        int[] argMap = new int[args.length];
        for (int arg = start, index = 0; arg < args.length; arg++) {
            int size = args[arg].getSize();
            argMap[arg] = this.allocateArgMapLocal(index, size);
            index += size;
        }
        return argMap;
    }

    /**
     * Allocates a local variable to be used for argmap. Since argmaps do not
     * overlap we can safely reuse local variables allocated for this purpose.
     * We do however need to deal with expanding the argmap allocation when
     * necessary, which is complicated slightly by the fact that some variables
     * occupy more than one local slot.
     * 
     * @param index Argmap index
     * @param size Size of variable
     * @return local offset to use
     */
    private int allocateArgMapLocal(int index, int size) {
        // Allocate extra space if we've reached the end
        if (index >= this.argMapVars.size()) {
            int base = this.allocateLocals(size);
            for (int offset = 0; offset < size; offset++) {
                this.argMapVars.add(Integer.valueOf(base + offset));
            }
            return base;
        }

        int local = this.argMapVars.get(index);
        
        // Allocate extra space if the variable is oversize
        if (size > 1 && index + size > this.argMapVars.size()) {
            int nextLocal = this.allocateLocals(1);
            if (nextLocal == local + 1) {
                // Next allocated was contiguous, so we can continue
                this.argMapVars.add(Integer.valueOf(nextLocal));
                return local;
            }
            
            // Next allocated was not contiguous, allocate a new local slot so
            // that indexes are contiguous
            this.argMapVars.set(index, Integer.valueOf(nextLocal));
            this.argMapVars.add(Integer.valueOf(this.allocateLocals(1)));
            return nextLocal;
        }

        return local;
    }
    
    /**
     * Get the argument indices for this target, calculated on first use
     * 
     * @return argument indices for this target
     */
    public int[] getArgIndices() {
        if (this.argIndices == null) {
            this.argIndices = this.calcArgIndices(this.isStatic ? 0 : 1);
        }
        return this.argIndices;
    }

    private int[] calcArgIndices(int local) {
        int[] argIndices = new int[this.arguments.length];
        for (int arg = 0; arg < this.arguments.length; arg++) {
            argIndices[arg] = local;
            local += this.arguments[arg].getSize();
        }
        return argIndices;
    }
    
    /**
     * Get the CallbackInfo class used for this target, based on the target
     * return type
     * 
     * @return CallbackInfo class name
     */
    public String getCallbackInfoClass() {
        if (this.callbackInfoClass == null) {
            this.callbackInfoClass = CallbackInfo.getCallInfoClassName(this.returnType);
        }
        return this.callbackInfoClass;
    }
    
    /**
     * Get "simple" callback descriptor (descriptor with only CallbackInfo)
     * 
     * @return generated descriptor
     */
    public String getSimpleCallbackDescriptor() {
        return String.format("(L%s;)V", this.getCallbackInfoClass());
    }
    
    /**
     * Get the callback descriptor
     * 
     * @param locals Local variable types
     * @param argumentTypes Argument types
     * @return generated descriptor
     */
    public String getCallbackDescriptor(final Type[] locals, Type[] argumentTypes) {
        return this.getCallbackDescriptor(false, locals, argumentTypes, 0, Short.MAX_VALUE);
    }

    /**
     * Get the callback descriptor
     * 
     * @param captureLocals True if the callback is capturing locals
     * @param locals Local variable types
     * @param argumentTypes Argument types
     * @param startIndex local index to start at
     * @param extra extra locals to include
     * @return generated descriptor
     */
    public String getCallbackDescriptor(final boolean captureLocals, final Type[] locals, Type[] argumentTypes, int startIndex, int extra) {
        if (this.callbackDescriptor == null) {
            this.callbackDescriptor = String.format("(%sL%s;)V", this.getDesc().substring(1, this.getDesc().indexOf(')')),
                    this.getCallbackInfoClass());
        }
        
        if (!captureLocals || locals == null) {
            return this.callbackDescriptor;
        }

        StringBuilder descriptor = new StringBuilder(this.callbackDescriptor.substring(0, this.callbackDescriptor.indexOf(')')));
        for (int l = startIndex; l < locals.length && extra > 0; l++) {
            if (locals[l] != null) {
                descriptor.append(locals[l].getDescriptor());
                extra--;
            }
        }

        return descriptor.append(")V").toString();
    }
    
    @Override
    public String toString() {
        return String.format("%s::%s%s", this.classNode.name, this.getName(), this.getDesc());
    }

    @Override
    public int compareTo(Target o) {
        if (o == null) {
            return Integer.MAX_VALUE;
        }
        return this.toString().compareTo(o.toString());
    }
    
    /**
     * Return the index of the specified instruction in this instruction list
     * 
     * @param node instruction to locate, must exist in the target
     * @return opcode index
     */
    public int indexOf(InjectionNode node) {
        return this.insns.indexOf(node.getCurrentTarget());
    }

    /**
     * Return the index of the specified instruction in this instruction list
     * 
     * @param insn instruction to locate, must exist in the target
     * @return opcode index
     */
    public int indexOf(AbstractInsnNode insn) {
        return this.insns.indexOf(insn);
    }
    
    /**
     * Return the instruction at the specified index
     * 
     * @param index opcode index
     * @return requested instruction
     */
    public AbstractInsnNode get(int index) {
        return this.insns.get(index);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<AbstractInsnNode> iterator() {
        return this.insns.iterator();
    }

    /**
     * Find the first <tt>&lt;init&gt;</tt> invocation after the specified
     * <tt>NEW</tt> insn 
     * 
     * @param newNode NEW insn
     * @param desc Descriptor to match
     * @return INVOKESPECIAL opcode of ctor, or null if not found
     */
    public MethodInsnNode findInitNodeFor(TypeInsnNode newNode, String desc) {
        return BeforeNew.findInitNodeFor(this.insns, newNode, desc);
    }
    
    /**
     * Insert the supplied instructions after the specified instruction 
     * 
     * @param location Instruction to insert before
     * @param insns Instructions to insert
     */
    public void insert(InjectionNode location, final InsnList insns) {
        this.insns.insert(location.getCurrentTarget(), insns);
    }
    
    /**
     * Insert the supplied instruction after the specified instruction 
     * 
     * @param location Instruction to insert before
     * @param insn Instruction to insert
     */
    public void insert(InjectionNode location, final AbstractInsnNode insn) {
        this.insns.insert(location.getCurrentTarget(), insn);
    }
    
    /**
     * Insert the supplied instructions after the specified instruction 
     * 
     * @param location Instruction to insert before
     * @param insns Instructions to insert
     */
    public void insert(AbstractInsnNode location, final InsnList insns) {
        this.insns.insert(location, insns);
    }

    /**
     * Insert the supplied instruction after the specified instruction 
     * 
     * @param location Instruction to insert before
     * @param insn Instruction to insert
     */
    public void insert(AbstractInsnNode location, final AbstractInsnNode insn) {
        this.insns.insert(location, insn);
    }

    /**
     * Insert the supplied instructions before the specified instruction 
     * 
     * @param location Instruction to insert before
     * @param insns Instructions to insert
     */
    public void insertBefore(InjectionNode location, final InsnList insns) {
        this.insns.insertBefore(location.getCurrentTarget(), insns);
    }

    /**
     * Insert the supplied instruction before the specified instruction 
     * 
     * @param location Instruction to insert before
     * @param insn Instruction to insert
     */
    public void insertBefore(InjectionNode location, final AbstractInsnNode insn) {
        this.insns.insertBefore(location.getCurrentTarget(), insn);
    }
    
    /**
     * Insert the supplied instructions before the specified instruction 
     * 
     * @param location Instruction to insert before
     * @param insns Instructions to insert
     */
    public void insertBefore(AbstractInsnNode location, final InsnList insns) {
        this.insns.insertBefore(location, insns);
    }
    
    /**
     * Insert the supplied instruction before the specified instruction 
     * 
     * @param location Instruction to insert before
     * @param insn Instruction to insert
     */
    public void insertBefore(AbstractInsnNode location, final AbstractInsnNode insn) {
        this.insns.insertBefore(location, insn);
    }
    
    /**
     * Replace an instruction in this target with the specified instruction and
     * mark the node as replaced for other injectors
     * 
     * @param location Instruction to replace
     * @param insn Instruction to replace with
     */
    public void replaceNode(AbstractInsnNode location, AbstractInsnNode insn) {
        this.insns.insertBefore(location, insn);
        this.insns.remove(location);
        this.injectionNodes.replace(location, insn);
    }
    
    /**
     * Replace an instruction in this target with the specified instructions and
     * mark the node as replaced with the specified champion node from the list.
     * 
     * @param location Instruction to replace
     * @param champion Instruction which notionally replaces the original insn
     * @param insns Instructions to actually insert (must contain champion)
     */
    public void replaceNode(AbstractInsnNode location, AbstractInsnNode champion, InsnList insns) {
        this.insns.insertBefore(location, insns);
        this.insns.remove(location);
        this.injectionNodes.replace(location, champion);
    }
    
    /**
     * Wrap instruction in this target with the specified instructions and mark
     * the node as replaced with the specified champion node from the list.
     * 
     * @param location Instruction to replace
     * @param champion Instruction which notionally replaces the original insn
     * @param before Instructions to actually insert (must contain champion)
     * @param after Instructions to insert after the specified location
     */
    public void wrapNode(AbstractInsnNode location, AbstractInsnNode champion, InsnList before, InsnList after) {
        this.insns.insertBefore(location, before);
        this.insns.insert(location, after);
        this.injectionNodes.replace(location, champion);
    }

    /**
     * Replace an instruction in this target with the specified instructions and
     * mark the original node as removed
     * 
     * @param location Instruction to replace
     * @param insns Instructions to replace with
     */
    public void replaceNode(AbstractInsnNode location, InsnList insns) {
        this.insns.insertBefore(location, insns);
        this.removeNode(location);
    }
    
    /**
     * Remove the specified instruction from the target and mark it as removed
     * for injections
     * 
     * @param insn instruction to remove
     */
    public void removeNode(AbstractInsnNode insn) {
        this.insns.remove(insn);
        this.injectionNodes.remove(insn);
    }

    /**
     * Add an entry to the target LVT
     * 
     * @param index local variable index
     * @param name local variable name
     * @param desc local variable type
     */
    public void addLocalVariable(int index, String name, String desc) {
        this.addLocalVariable(index, name, desc, null, null);
    }

    /**
     * Add an entry to the target LVT between the specified start and end labels
     * 
     * @param index local variable index
     * @param name local variable name
     * @param desc local variable type
     * @param from start of range
     * @param to end of range
     */
    public void addLocalVariable(int index, String name, String desc, LabelNode from, LabelNode to) {
        if (from == null) {
            from = this.getStartLabel();
        }
        
        if (to == null) {
            to = this.getEndLabel();
        }
        
        if (this.method.localVariables == null) {
            this.method.localVariables = new ArrayList<LocalVariableNode>();
        }
        
        for (Iterator<LocalVariableNode> iter = this.method.localVariables.iterator(); iter.hasNext();) {
            LocalVariableNode local = iter.next();
            if (local != null && local.index == index && from == local.start && to == local.end) {
                iter.remove();
            }
        }
        
        this.method.localVariables.add(new SyntheticLocalVariableNode(name, desc, null, from, to, index));
    }

    /**
     * Get a label which marks the very start of the method
     */
    private LabelNode getStartLabel() {
        if (this.start == null) {
            this.insns.insert(this.start = new LabelNode());
        }
        return this.start;
    }

    /**
     * Get a label which marks the very end of the method
     */
    private LabelNode getEndLabel() {
        if (this.end == null) {
            this.insns.add(this.end = new LabelNode());
        }
        return this.end;
    }

    public static Target of(ClassInfo classInfo, ClassNode classNode, MethodNode method) {
        if (method.name.equals(Constants.CTOR)) {
            return new Constructor(classInfo, classNode, method);
        }
        return new Target(classInfo, classNode, method);
    }

}
