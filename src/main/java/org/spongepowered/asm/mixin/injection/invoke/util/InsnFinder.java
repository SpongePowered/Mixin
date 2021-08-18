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
package org.spongepowered.asm.mixin.injection.invoke.util;

import org.spongepowered.asm.logging.ILogger;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.service.MixinService;

/**
 * Utility class for finding instructions using static analysis
 */
public class InsnFinder {
    
    /**
     * Exception to be throw to quick-exit the analyser once result is found
     */
    static class AnalysisResultException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private AbstractInsnNode result;

        public AnalysisResultException(AbstractInsnNode popNode) {
            this.result = popNode;
        }
        
        public AbstractInsnNode getResult() {
            return this.result;
        }
        
    }
    
    /**
     * Current analyser state
     */
    enum AnalyzerState {
        
        /**
         * Searching for the candidate instruction 
         */
        SEARCH,
        
        /**
         * Walking through following instructions looking for matching pop 
         */
        ANALYSE,
        
        /**
         * Analysis complete, unwinding rest of method so analyser completes
         * normally 
         */
        COMPLETE
    
    }

    /**
     * Specialised {@link Analyzer} which searches for an instruction which pops
     * the value pushed by the supplied instruction.
     */
    static class PopAnalyzer extends Analyzer<BasicValue> {
        
        /**
         * Frame which is used as proxy to observe push/pop operations and find
         * the candidate nodes
         */
        class PopFrame extends Frame<BasicValue> {
            
            private AbstractInsnNode current;
            
            private AnalyzerState state = AnalyzerState.SEARCH;
            private int depth = 0;
    
            public PopFrame(int locals, int stack) {
                super(locals, stack);
            }
            
            @Override
            public void execute(AbstractInsnNode insn, Interpreter<BasicValue> interpreter) throws AnalyzerException {
                this.current = insn;
                super.execute(insn, interpreter);
            }
            
            @Override
            public void push(BasicValue value) throws IndexOutOfBoundsException {
                if (this.current == PopAnalyzer.this.node && this.state == AnalyzerState.SEARCH) {
                    this.state = AnalyzerState.ANALYSE;
                    this.depth++;
                } else if (this.state == AnalyzerState.ANALYSE) {
                    this.depth++;
                }
                super.push(value);
            }
            
            @Override
            public BasicValue pop() throws IndexOutOfBoundsException {
                if (this.state == AnalyzerState.ANALYSE) {
                    if (--this.depth == 0) {
                        this.state = AnalyzerState.COMPLETE;
                        throw new AnalysisResultException(this.current);
                    }
                }
                return super.pop();
            }
            
        }
    
        protected final AbstractInsnNode node;
    
        public PopAnalyzer(AbstractInsnNode node) {
            super(new BasicInterpreter());
            this.node = node;
        }
        
        @Override
        protected Frame<BasicValue> newFrame(final int locals, final int stack) {
            return new PopFrame(locals, stack);
        }
    }
    
    /**
     * Log more things
     */
    private static final ILogger logger = MixinService.getService().getLogger("mixin");
    
    /**
     * Find the instruction which pops the value pushed by the specified
     * instruction
     * 
     * @param target target method
     * @param node push node
     * @return pop instruction or null if not found
     */
    public AbstractInsnNode findPopInsn(Target target, AbstractInsnNode node) {
        try {
            new PopAnalyzer(node).analyze(target.classNode.name, target.method);
        } catch (AnalyzerException ex) {
            if (ex.getCause() instanceof AnalysisResultException) {
                return ((AnalysisResultException)ex.getCause()).getResult();
            }
            InsnFinder.logger.catching(ex);
        }
        return null;
    }

}
