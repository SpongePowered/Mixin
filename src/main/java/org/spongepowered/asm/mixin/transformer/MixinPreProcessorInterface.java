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
import org.spongepowered.asm.mixin.MixinEnvironment.Feature;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Method;
import org.spongepowered.asm.mixin.transformer.MixinInfo.MixinClassNode;
import org.spongepowered.asm.mixin.transformer.MixinInfo.MixinMethodNode;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidInterfaceMixinException;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Bytecode.Visibility;
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
        boolean isPublic = Bytecode.hasFlag(mixinMethod, Opcodes.ACC_PUBLIC);
        Feature injectorsInInterfaceMixins = Feature.INJECTORS_IN_INTERFACE_MIXINS;
        CompatibilityLevel currentLevel = MixinEnvironment.getCompatibilityLevel();
        CompatibilityLevel requiredLevelSynthetic = CompatibilityLevel.requiredFor(LanguageFeatures.PRIVATE_SYNTHETIC_METHODS_IN_INTERFACES);

        if (!isPublic && mixinMethod.isSynthetic()) {
            if (currentLevel.isLessThan(requiredLevelSynthetic)) {
                throw new InvalidInterfaceMixinException(this.mixin, String.format(
                        "Interface mixin contains a synthetic private method but compatibility level %s is required! Found %s in %s",
                        requiredLevelSynthetic, method, this.mixin));
            }
            // Private synthetic is ok, do not process further
            return;
        }
        
        if (!isPublic) {
            CompatibilityLevel requiredLevelPrivate = CompatibilityLevel.requiredFor(LanguageFeatures.PRIVATE_METHODS_IN_INTERFACES);
            if (currentLevel.isLessThan(requiredLevelPrivate)) {
                throw new InvalidInterfaceMixinException(this.mixin, String.format(
                        "Interface mixin contains a private method but compatibility level %s is required! Found %s in %s",
                        requiredLevelPrivate, method, this.mixin));
            }
        }

        AnnotationNode injectorAnnotation = InjectionInfo.getInjectorAnnotation(this.mixin, mixinMethod);
        if (injectorAnnotation == null) {
            super.prepareMethod(mixinMethod, method);
            return;
        }
        
        if (injectorsInInterfaceMixins.isAvailable() && !injectorsInInterfaceMixins.isEnabled()) {
            throw new InvalidInterfaceMixinException(this.mixin, String.format(
                    "Interface mixin contains an injector but Feature.INJECTORS_IN_INTERFACE_MIXINS is disabled! Found %s in %s",
                    method, this.mixin));
        }
        
        // Make injectors private synthetic if the current runtime supports it
        if (isPublic
                && !currentLevel.supports(LanguageFeatures.PRIVATE_METHODS_IN_INTERFACES)
                && currentLevel.supports(LanguageFeatures.PRIVATE_SYNTHETIC_METHODS_IN_INTERFACES)) {
            Bytecode.setVisibility(mixinMethod, Visibility.PRIVATE);
            mixinMethod.access |= Opcodes.ACC_SYNTHETIC;
        }
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
