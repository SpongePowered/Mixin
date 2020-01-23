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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint.AtCode;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator.Context;
import org.spongepowered.asm.mixin.injection.modify.ModifyVariableInjector.LocalVariableInjectionPoint;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
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
public class BeforeLoadLocal extends LocalVariableInjectionPoint {
    
    /**
     * Keeps track of state within {@link #find}
     */
    class SearchState {
        
        private static final int INVALID_IMPLICIT = -2;
        
        /**
         * Print LVT search, be permissive 
         */
        private final boolean print;

        /**
         * The current ordinal 
         */
        private int currentOrdinal = 0;
        
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
        
        SearchState() {
            this.print = BeforeLoadLocal.this.discriminator.printLVT();
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
        
        void check(Target target, Collection<AbstractInsnNode> nodes, AbstractInsnNode insn) {
            Context context = new Context(BeforeLoadLocal.this.returnType, BeforeLoadLocal.this.discriminator.isArgsOnly(), target, insn);
            int local = SearchState.INVALID_IMPLICIT;
            
            try {
                local = BeforeLoadLocal.this.discriminator.findLocal(context);
            } catch (InvalidImplicitDiscriminatorException ex) {
                BeforeLoadLocal.this.addMessage("%s has invalid IMPLICIT discriminator for opcode %d in %s: %s",
                        BeforeLoadLocal.this.toString(context), target.indexOf(insn), target, ex.getMessage());
            }
            
            this.pendingCheck = false;
            if (local != this.varNode.var && (local > SearchState.INVALID_IMPLICIT || !this.print)) {
                this.varNode = null;
                return;
            }
            
            if (BeforeLoadLocal.this.ordinal == -1 || BeforeLoadLocal.this.ordinal == this.currentOrdinal) {
                nodes.add(insn);
                this.found = true;
            }

            this.currentOrdinal++;
            this.varNode = null;
        }
        
    }

    /**
     * Return type of the handler, also the type of the local variable we're
     * interested in
     */
    protected final Type returnType;
    
    /**
     * Discriminator, parsed from parent annotation
     */
    protected final LocalVariableDiscriminator discriminator;
    
    /**
     * Target opcode, inflected from return type
     */
    protected final int opcode;
    
    /**
     * Target ordinal 
     */
    protected final int ordinal;
    
    /**
     * True if this injection point should capture the opcode after a matching
     * opcode, used by {@link AfterStoreLocal}.
     */
    private boolean opcodeAfter;
    
    protected BeforeLoadLocal(InjectionPointData data) {
        this(data, Opcodes.ILOAD, false);
    }

    protected BeforeLoadLocal(InjectionPointData data, int opcode, boolean opcodeAfter) {
        super(data);
        this.returnType = data.getMethodReturnType();
        this.discriminator = data.getLocalVariableDiscriminator();
        this.opcode = data.getOpcode(this.returnType.getOpcode(opcode));
        this.ordinal = data.getOrdinal();
        this.opcodeAfter = opcodeAfter;
    }

    @Override
    boolean find(InjectionInfo info, Target target, Collection<AbstractInsnNode> nodes) {
        SearchState state = new SearchState();

        ListIterator<AbstractInsnNode> iter = target.method.instructions.iterator();
        while (iter.hasNext()) {
            AbstractInsnNode insn = iter.next();
            if (state.isPendingCheck()) {
                state.check(target, nodes, insn);
            } else  if (insn instanceof VarInsnNode && insn.getOpcode() == this.opcode && (this.ordinal == -1 || !state.success())) {
                state.register((VarInsnNode)insn);
                if (this.opcodeAfter) {
                    state.setPendingCheck();
                } else {
                    state.check(target, nodes, insn);
                }
            }
        }

        return state.success();
    }

    // No synthetic
    @Override
    protected void addMessage(String format, Object... args) {
        super.addMessage(format, args);
    }
    
    @Override
    public String toString() {
        return String.format("@At(\"%s\" %s)", this.getAtCode(), this.discriminator.toString());
    }
    
    public String toString(Context context) {
        return String.format("@At(\"%s\" %s)", this.getAtCode(), this.discriminator.toString(context));
    }
    
}
