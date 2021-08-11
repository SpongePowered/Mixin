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
package org.spongepowered.asm.mixin.transformer;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.transformer.MixinConfig.IListener;

/**
 * Coprocessors are parts of the mixin pipeline which aren't involved in
 * applying mixins but handle other transformation tasks <em>required</em> by
 * mixins.
 * 
 * <p>Such tasks as {@link MixinCoprocessorAccessor making accessor mixins
 * loadable (and transforming the accessors therein)}, {@link
 * MixinCoprocessorSyntheticInner exposing synthetic inner classes to all
 * consumers} and {@link MixinCoprocessorNestHost applying nest member
 * attributes to nest hosts which may themselves not be mixin targets} are
 * handled by different coprocessors.</p>
 * 
 * <p>These classes were previously encapsulated in a single companion class
 * called <tt>MixinPostProcessor</tt>, but the mixture of responsibilities of
 * that class made its role unclear and slightly schizophrenic.</p>
 */
abstract class MixinCoprocessor implements IListener {
    
    /**
     * The result of a specific coprocessor's action on a supplied class,
     * effectively a tuple of <tt>transformed</tt> and <tt>passthrough</tt>
     * which combines the normal <tt>transformed</tt> flag (which is returned to
     * the service so it knows whether the supplied class was altered or not)
     * and a second flag which indicates whether the class is eligible for
     * processing by the mixin pipeline: Results with <tt>passthrough</tt> set
     * will <b>not</b> be processed further by the mixin processor.   
     */
    enum ProcessResult {
        
        /**
         * This coprocessor does not take any action for the specified class 
         */
        NONE(false, false),
        
        /**
         * This coprocessor acted on the supplied bytecode, but does not change
         * the flow of the main mixin processor pipeline
         */
        TRANSFORMED(false, true),
        
        /**
         * This coprocessor did not transform the supplied bytecode, and mixins
         * should not be applied to this class
         */
        PASSTHROUGH_NONE(true, false),
        
        /**
         * This coprocessor acted on the supplied bytecode, and mixins should
         * not be applied to this class
         */
        PASSTHROUGH_TRANSFORMED(true, true);
        
        private boolean passthrough;
        
        private boolean transformed;

        private ProcessResult(boolean passthrough, boolean transformed) {
            this.passthrough = passthrough;
            this.transformed = transformed;
        }
        
        boolean isPassthrough() {
            return this.passthrough;
        }

        boolean isTransformed() {
            return this.transformed;
        }
        
        /**
         * Combine this result with the supplied result
         */
        ProcessResult with(ProcessResult other) {
            if (other == this) {
                return this;
            }
            return ProcessResult.of(this.passthrough || other.passthrough, this.transformed || other.transformed);
        }
        
        /**
         * Return a result which represents the supplied tuple of attributes
         */
        static ProcessResult of(boolean passthrough, boolean transformed) {
            if (passthrough) {
                return transformed ? ProcessResult.PASSTHROUGH_TRANSFORMED : ProcessResult.PASSTHROUGH_NONE;
            }
            return transformed ? ProcessResult.TRANSFORMED : ProcessResult.NONE;
        }
        
    }
    
    /**
     * Coprocessor name, for debugging only
     */
    abstract String getName();

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.MixinConfig.IListener
     *      #onPrepare(org.spongepowered.asm.mixin.transformer.MixinInfo)
     */
    @Override
    public void onPrepare(MixinInfo mixin) {
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.MixinConfig.IListener
     *      #onInit(org.spongepowered.asm.mixin.transformer.MixinInfo)
     */
    @Override
    public void onInit(MixinInfo mixin) {
    }
    
    /**
     * Process the supplied class. If the class is transformed, or should be
     * passed through (rather than treated as a mixin target) then this is
     * indicated by the return value
     * 
     * @param className Name of the target class
     * @param classNode Classnode of the target class
     * @return result indicating whether the class was transformed, and whether
     *      or not to passthrough instead of apply mixins
     */
    ProcessResult process(String className, ClassNode classNode) {
        return ProcessResult.NONE;
    }

    /**
     * Perform postprocessing actions on the supplied class. This is called for
     * all classes. For passthrough classes and classes which are not mixin 
     * targets this is called immediately after {@link process} is completed for
     * all coprocessors. For mixin targets this is called after mixins are
     * applied.
     * 
     * @param className Name of the target class
     * @param classNode Classnode of the target class
     * @return true if the coprocessor applied any transformations
     */
    boolean postProcess(String className, ClassNode classNode) {
        return false;
    }
    
}
