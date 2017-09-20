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
package org.spongepowered.asm.mixin.injection.modify;

import java.util.Collection;
import java.util.ListIterator;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint.AtCode;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.modify.ModifyVariableInjector.ContextualInjectionPoint;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.injection.struct.Target;

/**
 * <p>This injection point is a companion for the {@link ModifyVariable}
 * injector which searches for LOAD operations which match the local variables
 * described by the injector's defined discriminators.</p>
 * 
 * <p>This allows you consumers to specify an injection immediately before a
 * local variable is accessed in a method. Specify an <tt>ordinal</tt> of <tt>n
 * </tt> to match the <em>n + 1<sup>th</sup></em> access of the variable in
 * question.</p>
 * 
 * <dl>
 *   <dt>ordinal</dt>
 *   <dd>The ordinal position of the LOAD opcode for the matching local variable
 *   to search for, if not specified then the injection point returns <em>all
 *   </em> opcodes for which the parent annotation's discriminators match. The
 *   default value is <b>-1</b> which supresses ordinal checking.</dd>
 * </dl>
 * 
 * <p>Example:</p>
 * <blockquote><pre>
 *   &#064;ModifyVariable(
 *       method = "md",
 *       ordinal = 1,
 *       at = &#064;At(
 *           value = "LOAD",
 *           ordinal = 0
 *       )
 *   )</pre>
 * </blockquote>
 * <p>Note that if <em>value</em> is the only parameter specified, it can be
 * omitted:</p> 
 * <blockquote><pre>
 *   &#064;At("LOAD")</pre>
 * </blockquote>
 */
@AtCode("LOAD")
public class BeforeLoadLocal extends ContextualInjectionPoint {
    
    /**
     * Keeps track of state within {@link #find}
     */
    static class SearchState {
        
        /**
         * Print LVT search, be permissive 
         */
        private final boolean print;
        
        /**
         * The target ordinal from the injection point 
         */
        private final int targetOrdinal;
        
        /**
         * The current ordinal 
         */
        private int ordinal = 0;
        
        /**
         * Flag to defer a {@link check} to the next opcode, to honour the after
         * semantics of {@link AfterStoreLocal}. 
         */
        private boolean pendingCheck = false;
        
        /**
         * True if one or more opcodes was matched 
         */
        private boolean found = false;
        
        /**
         * Var node, captured for when deferring processing to the next opcode 
         */
        private VarInsnNode varNode;
        
        SearchState(int targetOrdinal, boolean print) {
            this.targetOrdinal = targetOrdinal;
            this.print = print;
        }

        boolean success() {
            return this.found;
        }
        
        boolean isPendingCheck() {
            return this.pendingCheck;
        }
        
        void setPendingCheck() {
            this.pendingCheck = true;
        }
        
        void register(VarInsnNode node) {
            this.varNode = node;
        }
        
        void check(Collection<AbstractInsnNode> nodes, AbstractInsnNode insn, int local) {
            this.pendingCheck = false;
            if (local != this.varNode.var && (local > -2 || !this.print)) {
                return;
            }
            
            if (this.targetOrdinal == -1 || this.targetOrdinal == this.ordinal) {
                nodes.add(insn);
                this.found = true;
            }

            this.ordinal++;
            this.varNode = null;
        }
        
    }

    /**
     * Return type of the handler, also the type of the local variable we're
     * interested in
     */
    private final Type returnType;
    
    /**
     * Discriminator, parsed from parent annotation
     */
    private final LocalVariableDiscriminator discriminator;
    
    /**
     * Target opcode, inflected from return type
     */
    private final int opcode;
    
    /**
     * Target ordinal 
     */
    private final int ordinal;
    
    /**
     * True if this injection point should capture the opcode after a matching
     * opcode, used by {@link AfterStoreLocal}.
     */
    private boolean opcodeAfter;
    
    protected BeforeLoadLocal(InjectionPointData data) {
        this(data, Opcodes.ILOAD, false);
    }
    
    protected BeforeLoadLocal(InjectionPointData data,
            int opcode, boolean opcodeAfter) {
        super(data.getContext());
        this.returnType = data.getMethodReturnType();
        this.discriminator = data.getLocalVariableDiscriminator();
        this.opcode = data.getOpcode(this.returnType.getOpcode(opcode));
        this.ordinal = data.getOrdinal();
        this.opcodeAfter = opcodeAfter;
    }

    @Override
    boolean find(Target target, Collection<AbstractInsnNode> nodes) {
        SearchState state = new SearchState(this.ordinal, this.discriminator.printLVT());

        ListIterator<AbstractInsnNode> iter = target.method.instructions.iterator();
        while (iter.hasNext()) {
            AbstractInsnNode insn = iter.next();
            if (state.isPendingCheck()) {
                int local = this.discriminator.findLocal(this.returnType, this.discriminator.isArgsOnly(), target, insn);
                state.check(nodes, insn, local);
            } else  if (insn instanceof VarInsnNode && insn.getOpcode() == this.opcode && (this.ordinal == -1 || !state.success())) {
                state.register((VarInsnNode)insn);
                if (this.opcodeAfter) {
                    state.setPendingCheck();
                } else {
                    int local = this.discriminator.findLocal(this.returnType, this.discriminator.isArgsOnly(), target, insn);
                    state.check(nodes, insn, local);
                }
            }
        }

        return state.success();
    }
    
}
