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

import java.lang.reflect.Method;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.tools.Diagnostic.Kind;

import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.MirrorUtils;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;

/**
 * A module for {@link AnnotatedMixin} which handles method overwrites
 */
class AnnotatedMixinOverwriteHandler extends AnnotatedMixinElementHandler {
    
    /**
     * Overwrite element
     */
    static class AnnotatedElementOverwrite extends AnnotatedElement<ExecutableElement> {

        public AnnotatedElementOverwrite(ExecutableElement element, AnnotationMirror annotation) {
            super(element, annotation);
        }
        
    }
    
    AnnotatedMixinOverwriteHandler(IMixinAnnotationProcessor ap, AnnotatedMixin mixin) {
        super(ap, mixin);
    }

    public void registerOverwrite(AnnotatedElementOverwrite elem) {
        AliasedElementName name = new AliasedElementName(elem.getElement(), elem.getAnnotation());
        this.validateTargetMethod(elem.getElement(), elem.getAnnotation(), name, "@Overwrite");
        this.checkConstraints(elem.getElement(), elem.getAnnotation());
        
        if (!this.mixin.remap() || !this.validateSingleTarget("@Overwrite", elem.getElement())) {
            return;
        }
        
        String mcpName = elem.getElement().getSimpleName().toString();
        String mcpSignature = MirrorUtils.generateSignature(elem.getElement());
        MappingMethod targetMethod = new MappingMethod(this.mixin.getPrimaryTargetRef(), mcpName, mcpSignature);
        ObfuscationData<MappingMethod> obfData = this.obf.getDataProvider().getObfMethod(targetMethod);
        
        if (obfData.isEmpty()) {
            Kind error = Kind.ERROR;
            
            try {
                // Try to access isStatic from com.sun.tools.javac.code.Symbol
                Method md = elem.getElement().getClass().getMethod("isStatic");
                if (((Boolean)md.invoke(elem.getElement())).booleanValue()) {
                    error = Kind.WARNING;
                }
            } catch (Exception ex) {
                // well, we tried
            }
            
            this.ap.printMessage(error, "No obfuscation mapping for @Overwrite method", elem.getElement());
            return;
        }

        for (ObfuscationType type : obfData) {
            MappingMethod obfMethod = obfData.get(type);
            this.addMethodMapping(type, mcpName, obfMethod.getSimpleName(), mcpSignature, obfMethod.getDesc());
        }
        
        if (!"true".equalsIgnoreCase(this.ap.getOption(SupportedOptions.DISABLE_OVERWRITE_CHECKER))) {
            Kind overwriteErrorKind = "error".equalsIgnoreCase(this.ap.getOption(SupportedOptions.OVERWRITE_ERROR_LEVEL))
                    ? Kind.ERROR : Kind.WARNING;
            
            String javadoc = this.ap.getJavadocProvider().getJavadoc(elem.getElement());
            if (javadoc == null) {
                this.ap.printMessage(overwriteErrorKind, "@Overwrite is missing javadoc comment", elem.getElement());
                return;
            }
            
            if (!javadoc.toLowerCase().contains("@author")) {
                this.ap.printMessage(overwriteErrorKind, "@Overwrite is missing an @author tag", elem.getElement());
            }
            
            if (!javadoc.toLowerCase().contains("@reason")) {
                this.ap.printMessage(overwriteErrorKind, "@Overwrite is missing an @reason tag", elem.getElement());
            }
        }
    }

}
