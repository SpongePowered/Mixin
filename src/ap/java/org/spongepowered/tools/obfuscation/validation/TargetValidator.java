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
package org.spongepowered.tools.obfuscation.validation;

import java.util.Collection;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.spongepowered.tools.MirrorUtils;
import org.spongepowered.tools.obfuscation.IOptionProvider;
import org.spongepowered.tools.obfuscation.MixinValidator;
import org.spongepowered.tools.obfuscation.SupportedOptions;
import org.spongepowered.tools.obfuscation.TypeHandle;

/**
 * Validator which checks that the mixin targets are sane
 */
public class TargetValidator extends MixinValidator {

    /**
     * ctor
     * 
     * @param processingEnv Processing environment
     * @param messager Messager
     * @param options Option provider
     */
    public TargetValidator(ProcessingEnvironment processingEnv, Messager messager, IOptionProvider options) {
        super(processingEnv, messager, options, ValidationPass.LATE);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.MixinValidator
     *      #validate(javax.lang.model.element.TypeElement,
     *      javax.lang.model.element.AnnotationMirror, java.util.Collection)
     */
    @Override
    public boolean validate(TypeElement mixin, AnnotationMirror annotation, Collection<TypeHandle> targets) {
        if ("true".equalsIgnoreCase(this.options.getOption(SupportedOptions.DISABLE_TARGET_VALIDATOR))) {
            return true;
        }
        
        TypeMirror superClass = mixin.getSuperclass();
        
        for (TypeHandle target : targets) {
            TypeMirror targetType = target.getType();
            if (targetType != null && !this.validateSuperClass(targetType, superClass)) {
                this.error("Superclass " + superClass + " of " + mixin + " was not found in the hierarchy of target class " + targetType, mixin);
            }
        }
        
        return true;
    }

    private boolean validateSuperClass(TypeMirror targetType, TypeMirror superClass) {
        if (MirrorUtils.isAssignable(this.processingEnv, targetType, superClass)) {
            return true;
        }
        
        return this.validateSuperClassRecursive(targetType, superClass);
    }

    private boolean validateSuperClassRecursive(TypeMirror targetType, TypeMirror superClass) {
        if (!(targetType instanceof DeclaredType)) {
            return false;
        }
        
        TypeElement targetElement = (TypeElement)((DeclaredType)targetType).asElement();
        TypeMirror targetSuper = targetElement.getSuperclass();
        if (targetSuper.getKind() == TypeKind.NONE) {
            return false;
        }
        
        if (this.checkMixinsFor(targetSuper, superClass)) {
            return true;
        }
        
        return this.validateSuperClassRecursive(targetSuper, superClass);
    }

    private boolean checkMixinsFor(TypeMirror targetType, TypeMirror superClass) {
        for (TypeMirror mixinType : this.getMixinsTargeting(targetType)) {
            if (MirrorUtils.isAssignable(this.processingEnv, mixinType, superClass)) {
                return true;
            }
        }
        
        return false;
    }
}
