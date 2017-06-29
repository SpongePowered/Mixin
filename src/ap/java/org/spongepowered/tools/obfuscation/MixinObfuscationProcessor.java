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

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Base class for mixin annotation processor modules
 */
public abstract class MixinObfuscationProcessor extends AbstractProcessor {
    
    /**
     * Mixin info manager 
     */
    protected AnnotatedMixins mixins;

    /* (non-Javadoc)
     * @see javax.annotation.processing.AbstractProcessor
     *      #init(javax.annotation.processing.ProcessingEnvironment)
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.mixins = AnnotatedMixins.getMixinsForEnvironment(processingEnv);
    }

    /**
     * Searches and catalogues all annotated mixin classes
     * 
     * @param roundEnv round environment
     */
    protected void processMixins(RoundEnvironment roundEnv) {
        this.mixins.onPassStarted();
        
        for (Element elem : roundEnv.getElementsAnnotatedWith(Mixin.class)) {
            if (elem.getKind() == ElementKind.CLASS || elem.getKind() == ElementKind.INTERFACE) {
                this.mixins.registerMixin((TypeElement)elem);
            } else {
                this.mixins.printMessage(Kind.ERROR, "Found an @Mixin annotation on an element which is not a class or interface", elem);
            }
        }
    }
    
    protected void postProcess(RoundEnvironment roundEnv) {
        this.mixins.onPassCompleted(roundEnv);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        try {
            return SourceVersion.valueOf("RELEASE_8");
        } catch (IllegalArgumentException ex) {
            // Java 8 not supported
        }
        
        return super.getSupportedSourceVersion();
    }
    
    @Override
    public Set<String> getSupportedOptions() {
        return SupportedOptions.getAllOptions();
    }
    
}
