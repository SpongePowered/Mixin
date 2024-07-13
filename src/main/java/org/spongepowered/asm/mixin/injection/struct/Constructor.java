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

import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.struct.Initialiser;
import org.spongepowered.asm.mixin.transformer.struct.InsnRange;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.asm.MarkerNode;
import org.spongepowered.asm.util.Bytecode.DelegateInitialiser;

/**
 * A {@link Target} which is a constructor
 */
public class Constructor extends Target {
    
    /**
     * Cached delegate initialiser call
     */
    private DelegateInitialiser delegateInitialiser;
    
    private MarkerNode initialiserInjectionPoint;
    private MarkerNode bodyStart;
    
    private final String targetName; 
    private final String targetSuperName;

    private Set<String> mixinInitialisedFields = new HashSet<String>();

    public Constructor(ClassInfo classInfo, ClassNode classNode, MethodNode method) {
        super(classInfo, classNode, method);
        
        this.targetName = this.classInfo.getName(); 
        this.targetSuperName = this.classInfo.getSuperName();
    }
    
    /**
     * Find the call to <tt>super()</tt> or <tt>this()</tt> in a constructor.
     * This attempts to locate the first call to <tt>&lt;init&gt;</tt> which
     * isn't an inline call to another object ctor being passed into the super
     * invocation.
     * 
     * @return Call to <tt>super()</tt>, <tt>this()</tt> or
     *      <tt>DelegateInitialiser.NONE</tt> if not found
     */
    public DelegateInitialiser findDelegateInitNode() {
        if (this.delegateInitialiser == null) {
            this.delegateInitialiser = Bytecode.findDelegateInit(this.method, this.classInfo.getSuperName(), this.classNode.name);
        }
        
        return this.delegateInitialiser;
    }
    
    /**
     * Get whether this constructor should have mixin initialisers injected into
     * it based on whether a delegate initialiser call is absent or is a call to
     * super()
     */
    public boolean isInjectable() {
        DelegateInitialiser delegateInit = this.findDelegateInitNode();
        return !delegateInit.isPresent || delegateInit.isSuper;
    }
    
    /**
     * Inspect an incoming initialiser to determine which fields are touched by
     * the mixin. This is then used in {@link #findInitialiserInjectionPoint} to
     * determine the instruction after which the mixin initialisers will be
     * placed
     * 
     * @param initialiser the initialiser to inspect
     */
    public void inspect(Initialiser initialiser) {
        if (this.initialiserInjectionPoint != null) {
            throw new IllegalStateException("Attempted to inspect an incoming initialiser after the injection point was already determined");
        }

        for (AbstractInsnNode initialiserInsn : initialiser.getInsns()) {
            if (initialiserInsn.getOpcode() == Opcodes.PUTFIELD) {
                this.mixinInitialisedFields.add(Constructor.fieldKey((FieldInsnNode)initialiserInsn));
            }
        }
    }

    /**
     * Find the injection point for injected initialiser insns in the target
     * ctor. This starts by assuming that initialiser instructions should be
     * placed immediately after the delegate initialiser call, but then searches
     * for field assignments and selects the last <em>unique</em> field
     * assignment in the ctor body which represents a reasonable heuristic for
     * the end of the existing initialisers. 
     * 
     * @param mode Injection mode for this specific environment
     * @return target node
     */
    public AbstractInsnNode findInitialiserInjectionPoint(Initialiser.InjectionMode mode) {
        if (this.initialiserInjectionPoint != null) {
            return this.initialiserInjectionPoint;
        }

        AbstractInsnNode lastInsn = null;
        for (Iterator<AbstractInsnNode> iter = this.insns.iterator(); iter.hasNext();) {
            AbstractInsnNode insn = iter.next();
            if (insn.getOpcode() == Opcodes.INVOKESPECIAL && Constants.CTOR.equals(((MethodInsnNode)insn).name)) {
                String owner = ((MethodInsnNode)insn).owner;
                if (owner.equals(this.targetName) || owner.equals(targetSuperName)) {
                    lastInsn = insn;
                    if (mode == Initialiser.InjectionMode.SAFE) {
                        break;
                    }
                }
            } else if (insn.getOpcode() == Opcodes.PUTFIELD && mode == Initialiser.InjectionMode.DEFAULT) {
                String key = Constructor.fieldKey((FieldInsnNode)insn);
                if (this.mixinInitialisedFields.contains(key)) {
                    lastInsn = insn;
                }
            }            
        }
        
        if (lastInsn == null) {
            return null;
        }
        
        this.initialiserInjectionPoint = new MarkerNode(MarkerNode.INITIALISER_TAIL);
        this.insert(lastInsn, this.initialiserInjectionPoint);
        return this.initialiserInjectionPoint;
    }

    /**
     * Find the injection point 
     */
    public AbstractInsnNode findFirstBodyInsn() {
        if (this.bodyStart == null) {
            this.bodyStart = new MarkerNode(MarkerNode.BODY_START);
            InsnRange range = Constructor.getRange(this.method);
            if (range.isValid()) {
                Deque<AbstractInsnNode> body = range.apply(this.insns, true);
                this.insertBefore(body.pop(), this.bodyStart);
            } else if (range.marker > -1) {
                this.insert(this.insns.get(range.marker), this.bodyStart);
            } else {
                this.bodyStart = null;
            }
        }
        return this.bodyStart;
    }

    private static String fieldKey(FieldInsnNode fieldNode) {
        return String.format("%s:%s", fieldNode.desc, fieldNode.name);
    }

    /**
     * Identifies line numbers in the supplied ctor which correspond to the
     * start and end of the method body.
     * 
     * @param ctor constructor to scan
     * @return range indicating the line numbers of the specified constructor
     *      and the position of the superclass ctor invocation
     */
    public static InsnRange getRange(MethodNode ctor) {
        boolean lineNumberIsValid = false;
        AbstractInsnNode endReturn = null;
        
        int line = 0, start = 0, end = 0, delegateIndex = -1;
        for (Iterator<AbstractInsnNode> iter = ctor.instructions.iterator(); iter.hasNext();) {
            AbstractInsnNode insn = iter.next();
            if (insn instanceof LineNumberNode) {
                line = ((LineNumberNode)insn).line;
                lineNumberIsValid = true;
            } else if (insn instanceof MethodInsnNode) {
                if (insn.getOpcode() == Opcodes.INVOKESPECIAL && Constants.CTOR.equals(((MethodInsnNode)insn).name) && delegateIndex == -1) {
                    delegateIndex = ctor.instructions.indexOf(insn);
                    start = line;
                }
            } else if (insn.getOpcode() == Opcodes.PUTFIELD) {
                lineNumberIsValid = false;
            } else if (insn.getOpcode() == Opcodes.RETURN) {
                if (lineNumberIsValid) {
                    end = line;
                } else {
                    end = start;
                    endReturn = insn;
                }
            }
        }
        
        if (endReturn != null) {
            LabelNode label = new LabelNode(new Label());
            ctor.instructions.insertBefore(endReturn, label);
            ctor.instructions.insertBefore(endReturn, new LineNumberNode(start, label));
        }
        
        return new InsnRange(start, end, delegateIndex);
    }
}
