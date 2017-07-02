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

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeUtils;

/**
 * Annotation processor which finds {@link Shadow} and {@link Overwrite}
 * annotations in mixin classes and generates new obfuscation mappings
 */
@SupportedAnnotationTypes({
    "org.spongepowered.asm.mixin.Mixin",
    "org.spongepowered.asm.mixin.Shadow",
    "org.spongepowered.asm.mixin.Overwrite",
    "org.spongepowered.asm.mixin.gen.Accessor",
    "org.spongepowered.asm.mixin.Implements"
})
public class MixinObfuscationProcessorTargets extends MixinObfuscationProcessor {
    
    /* (non-Javadoc)
     * @see javax.annotation.processing.AbstractProcessor
     *      #process(java.util.Set,
     *      javax.annotation.processing.RoundEnvironment)
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            this.postProcess(roundEnv);
            return true;
        }
        
        this.processMixins(roundEnv);
        this.processShadows(roundEnv);
        this.processOverwrites(roundEnv);
        this.processAccessors(roundEnv);
        this.processInvokers(roundEnv);
        this.processImplements(roundEnv);
        this.postProcess(roundEnv);
        
        return true;
    }
    
    @Override
    protected void postProcess(RoundEnvironment roundEnv) {
        super.postProcess(roundEnv);
        
        try {
            this.mixins.writeReferences();
            this.mixins.writeMappings();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Searches for {@link Shadow} annotations and registers them with their
     * parent mixins
     */
    private void processShadows(RoundEnvironment roundEnv) {
        for (Element elem : roundEnv.getElementsAnnotatedWith(Shadow.class)) {
            Element parent = elem.getEnclosingElement();
            if (!(parent instanceof TypeElement)) {
                this.mixins.printMessage(Kind.ERROR, "Unexpected parent with type " + TypeUtils.getElementType(parent), elem);
                continue;
            }
            
            AnnotationHandle shadow = AnnotationHandle.of(elem, Shadow.class);
            
            if (elem.getKind() == ElementKind.FIELD) {
                this.mixins.registerShadow((TypeElement)parent, (VariableElement)elem, shadow);
            } else if (elem.getKind() == ElementKind.METHOD) {
                this.mixins.registerShadow((TypeElement)parent, (ExecutableElement)elem, shadow);
            } else {
                this.mixins.printMessage(Kind.ERROR, "Element is not a method or field",  elem);
            }
        }
    }

    /**
     * Searches for {@link Overwrite} annotations and registers them with their
     * parent mixins
     */
    private void processOverwrites(RoundEnvironment roundEnv) {
        for (Element elem : roundEnv.getElementsAnnotatedWith(Overwrite.class)) {
            Element parent = elem.getEnclosingElement();
            if (!(parent instanceof TypeElement)) {
                this.mixins.printMessage(Kind.ERROR, "Unexpected parent with type " + TypeUtils.getElementType(parent), elem);
                continue;
            }
            
            if (elem.getKind() == ElementKind.METHOD) {
                this.mixins.registerOverwrite((TypeElement)parent, (ExecutableElement)elem);
            } else {
                this.mixins.printMessage(Kind.ERROR, "Element is not a method",  elem);
            }
        }
    }
    
    /**
     * Searches for {@link Accessor} annotations and registers them with their
     * parent mixins
     */
    private void processAccessors(RoundEnvironment roundEnv) {
        for (Element elem : roundEnv.getElementsAnnotatedWith(Accessor.class)) {
            Element parent = elem.getEnclosingElement();
            if (!(parent instanceof TypeElement)) {
                this.mixins.printMessage(Kind.ERROR, "Unexpected parent with type " + TypeUtils.getElementType(parent), elem);
                continue;
            }
            
            if (elem.getKind() == ElementKind.METHOD) {
                this.mixins.registerAccessor((TypeElement)parent, (ExecutableElement)elem);
            } else {
                this.mixins.printMessage(Kind.ERROR, "Element is not a method",  elem);
            }
        }
    }
    
    /**
     * Searches for {@link Invoker} annotations and registers them with their
     * parent mixins
     */
    private void processInvokers(RoundEnvironment roundEnv) {
        for (Element elem : roundEnv.getElementsAnnotatedWith(Invoker.class)) {
            Element parent = elem.getEnclosingElement();
            if (!(parent instanceof TypeElement)) {
                this.mixins.printMessage(Kind.ERROR, "Unexpected parent with type " + TypeUtils.getElementType(parent), elem);
                continue;
            }
            
            if (elem.getKind() == ElementKind.METHOD) {
                this.mixins.registerInvoker((TypeElement)parent, (ExecutableElement)elem);
            } else {
                this.mixins.printMessage(Kind.ERROR, "Element is not a method",  elem);
            }
        }
    }
    
    /**
     * Searches for {@link Implements} annotations and registers them with their
     * parent mixins
     */
    private void processImplements(RoundEnvironment roundEnv) {
        for (Element elem : roundEnv.getElementsAnnotatedWith(Implements.class)) {
            if (elem.getKind() == ElementKind.CLASS || elem.getKind() == ElementKind.INTERFACE) {
                AnnotationHandle implementsAnnotation = AnnotationHandle.of(elem, Implements.class);
                this.mixins.registerSoftImplements((TypeElement)elem, implementsAnnotation);
            } else {
                this.mixins.printMessage(Kind.ERROR, "Found an @Implements annotation on an element which is not a class or interface", elem);
            }
        }
    }
    
}
