/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered.org <http://www.spongepowered.org>
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
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;

import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.tools.MirrorUtils;

/**
 * Annotation processor which finds {@link Shadow} and {@link Overwrite} annotations in mixin classes and generates SRG mappings
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes({ "org.spongepowered.asm.mixin.Mixin", "org.spongepowered.asm.mixin.Shadow", "org.spongepowered.asm.mixin.Overwrite" })
@SupportedOptions({ "reobfSrgFile", "outSrgFile" })
public class TargetObfuscationProcessor extends MixinProcessor {
    
    /* (non-Javadoc)
     * @see javax.annotation.processing.AbstractProcessor#process(java.util.Set, javax.annotation.processing.RoundEnvironment)
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }
        
        this.processMixins(roundEnv);
        this.processShadows(roundEnv);
        this.processOverwrites(roundEnv);
        
        try {
            this.mixins.writeSrgs();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        return true;
    }

    /**
     * Searches for {@link Shadow} annotations and registers them with their parent mixins
     */
    private void processShadows(RoundEnvironment roundEnv) {
        for (Element elem : roundEnv.getElementsAnnotatedWith(Shadow.class)) {
            Element parent = elem.getEnclosingElement();
            if (!(parent instanceof TypeElement)) {
                throw new IllegalStateException("@Shadow element has unexpected parent with type " + MirrorUtils.getElementType(parent));
            }
            
            AnnotationMirror shadow = MirrorUtils.getAnnotation(elem, Shadow.class);
            
            if (elem.getKind() == ElementKind.FIELD) {
                this.mixins.registerShadow((TypeElement)parent, (VariableElement)elem, shadow);
            } else if (elem.getKind() == ElementKind.METHOD) {
                this.mixins.registerShadow((TypeElement)parent, (ExecutableElement)elem, shadow);
            } else {
                this.processingEnv.getMessager().printMessage(Kind.WARNING,
                        "Found an @Shadow annotation on an element which is not a method or field: " + elem.toString());
            }
        }
    }

    /**
     * Searches for {@link Overwrite} annotations and registers them with their parent mixins
     */
    private void processOverwrites(RoundEnvironment roundEnv) {
        for (Element elem : roundEnv.getElementsAnnotatedWith(Overwrite.class)) {
            Element parent = elem.getEnclosingElement();
            if (!(parent instanceof TypeElement)) {
                throw new IllegalStateException("@Overwrite element has unexpected parent with type " + MirrorUtils.getElementType(parent));
            }
            
            if (elem.getKind() == ElementKind.METHOD) {
                this.mixins.registerOverwrite((TypeElement)parent, (ExecutableElement)elem);
            } else {
                this.processingEnv.getMessager().printMessage(Kind.WARNING,
                        "Found an @Overwrite annotation on an element which is not a method: " + elem.toString());
            }
        }
    }
}
