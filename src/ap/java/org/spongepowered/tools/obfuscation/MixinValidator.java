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
package org.spongepowered.tools.obfuscation;

import java.util.Collection;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerSuppressible;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IMixinValidator;
import org.spongepowered.tools.obfuscation.interfaces.IOptionProvider;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;

/**
 * Base class for mixin validators
 */
public abstract class MixinValidator implements IMixinValidator {
    
    /**
     * Processing environment for this validator, used for raising compiler
     * errors and warnings 
     */
    protected final ProcessingEnvironment processingEnv;
    
    /**
     * Messager to use to output errors and warnings
     */
    protected final IMessagerSuppressible messager;
    
    /**
     * Option provider 
     */
    protected final IOptionProvider options;
    
    /**
     * Pass to run this validator in 
     */
    protected final ValidationPass pass;

    /**
     * ctor
     * 
     * @param ap Processing environment
     * @param pass Validation pass being performed
     */
    public MixinValidator(IMixinAnnotationProcessor ap, ValidationPass pass) {
        this.processingEnv = ap.getProcessingEnvironment();
        this.messager = ap;
        this.options = ap;
        this.pass = pass;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.IMixinValidator#validate(
     *      org.spongepowered.tools.obfuscation.IMixinValidator.ValidationPass,
     *      javax.lang.model.element.TypeElement,
     *      org.spongepowered.tools.obfuscation.AnnotationHandle,
     *      java.util.Collection)
     */
    @Override
    public final boolean validate(ValidationPass pass, TypeElement mixin, IAnnotationHandle annotation, Collection<TypeHandle> targets) {
        if (pass != this.pass) {
            return true;
        }
        
        return this.validate(mixin, annotation, targets);
    }

    protected abstract boolean validate(TypeElement mixin, IAnnotationHandle annotation, Collection<TypeHandle> targets);

    protected final Collection<TypeMirror> getMixinsTargeting(TypeMirror targetType) {
        return AnnotatedMixins.getMixinsForEnvironment(this.processingEnv).getMixinsTargeting(targetType);
    }
}
