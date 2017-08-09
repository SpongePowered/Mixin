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

import javax.lang.model.element.ExecutableElement;
import javax.tools.Diagnostic.Kind;

import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.Mappings.MappingConflictException;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;

/**
 * A module for {@link AnnotatedMixin} which handles method overwrites
 */
class AnnotatedMixinElementHandlerOverwrite extends AnnotatedMixinElementHandler {
    
    /**
     * Overwrite element
     */
    static class AnnotatedElementOverwrite extends AnnotatedElement<ExecutableElement> {
        
        private final boolean shouldRemap;
        
        public AnnotatedElementOverwrite(ExecutableElement element, AnnotationHandle annotation, boolean shouldRemap) {
            super(element, annotation);
            this.shouldRemap = shouldRemap;
        }
        
        public boolean shouldRemap() {
            return this.shouldRemap;
        }

    }
    
    AnnotatedMixinElementHandlerOverwrite(IMixinAnnotationProcessor ap, AnnotatedMixin mixin) {
        super(ap, mixin);
    }

    public void registerMerge(ExecutableElement method) {
        this.validateTargetMethod(method, null, new AliasedElementName(method, AnnotationHandle.MISSING), "overwrite", true, true);
    }

    public void registerOverwrite(AnnotatedElementOverwrite elem) {
        AliasedElementName name = new AliasedElementName(elem.getElement(), elem.getAnnotation());
        this.validateTargetMethod(elem.getElement(), elem.getAnnotation(), name, "@Overwrite", true, false);
        this.checkConstraints(elem.getElement(), elem.getAnnotation());
        
        if (elem.shouldRemap()) {
            for (TypeHandle target : this.mixin.getTargets()) {
                if (!this.registerOverwriteForTarget(elem, target)) {
                    return;
                }
            }
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

    private boolean registerOverwriteForTarget(AnnotatedElementOverwrite elem, TypeHandle target) {
        MappingMethod targetMethod = target.getMappingMethod(elem.getSimpleName(), elem.getDesc());
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
            return false;
        }

        try {
            this.addMethodMappings(elem.getSimpleName(), elem.getDesc(), obfData);
        } catch (MappingConflictException ex) {
            elem.printMessage(this.ap, Kind.ERROR, "Mapping conflict for @Overwrite method: " + ex.getNew().getSimpleName() + " for target " + target 
                    + " conflicts with existing mapping " + ex.getOld().getSimpleName());
            return false;
        }
        return true;
    }

}
