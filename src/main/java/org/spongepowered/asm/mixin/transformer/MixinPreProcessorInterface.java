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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.CompatibilityLevel;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Method;
import org.spongepowered.asm.mixin.transformer.MixinInfo.MixinClassNode;
import org.spongepowered.asm.mixin.transformer.MixinInfo.MixinMethodNode;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidInterfaceMixinException;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.LanguageFeatures;

/**
 * Bytecode preprocessor for interface mixins, simply performs some additional
 * verification for things which are unsupported in interfaces
 */
class MixinPreProcessorInterface extends MixinPreProcessorStandard {
    
    /**
     * Ctor
     * 
     * @param mixin Mixin info
     * @param classNode Mixin classnode
     */
    MixinPreProcessorInterface(MixinInfo mixin, MixinClassNode classNode) {
        super(mixin, classNode);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.MixinPreProcessor
     *      #prepareMethod(org.objectweb.asm.tree.MethodNode,
     *      org.spongepowered.asm.mixin.transformer.ClassInfo.Method)
     */
    @Override
    protected void prepareMethod(MixinMethodNode mixinMethod, Method method) {
        // Userland interfaces should not have non-public methods except for lambda bodies
        if (!Bytecode.hasFlag(mixinMethod, Opcodes.ACC_PUBLIC)) {
            if (!Bytecode.hasFlag(mixinMethod, Opcodes.ACC_SYNTHETIC)) {
                throw new InvalidInterfaceMixinException(this.mixin, String.format("Interface mixin contains a non-public method! Found %s in %s",
                        method, this.mixin));
            }
            CompatibilityLevel requiredLevel = CompatibilityLevel.requiredFor(LanguageFeatures.PRIVATE_SYNTHETIC_METHODS_IN_INTERFACES);
            if (MixinEnvironment.getCompatibilityLevel().isLessThan(requiredLevel)) {
                throw new InvalidInterfaceMixinException(this.mixin, String.format(
                        "Interface mixin contains a synthetic private method but compatibility level %s is required! Found %s in %s",
                        requiredLevel, method, this.mixin));
            }
        }
        
        super.prepareMethod(mixinMethod, method);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.MixinPreProcessor
     *      #validateField(
     *      org.spongepowered.asm.mixin.transformer.MixinTargetContext,
     *      org.objectweb.asm.tree.FieldNode,
     *      org.objectweb.asm.tree.AnnotationNode)
     */
    @Override
    protected boolean validateField(MixinTargetContext context, FieldNode field, AnnotationNode shadow) {
        if (!Bytecode.isStatic(field)) {
            throw new InvalidInterfaceMixinException(this.mixin, String.format("Interface mixin contains an instance field! Found %s in %s",
                    field.name, this.mixin));
        }
        
        return super.validateField(context, field, shadow);
    }
    
}
