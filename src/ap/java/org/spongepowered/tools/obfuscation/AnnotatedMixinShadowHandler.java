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

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;

import org.spongepowered.asm.obfuscation.SrgMethod;
import org.spongepowered.tools.MirrorUtils;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;

/**
 * A module for {@link AnnotatedMixin} which handles shadowed fields and methods
 */
class AnnotatedMixinShadowHandler extends AnnotatedMixinElementHandler {

    AnnotatedMixinShadowHandler(IMixinAnnotationProcessor ap, AnnotatedMixin mixin) {
        super(ap, mixin);
    }

    /**
     * Register a {@link org.spongepowered.asm.mixin.Shadow} field
     */
    public void registerShadow(VariableElement field, AnnotationMirror shadow, boolean remap) {
        ShadowElementName name = new ShadowElementName(field, shadow);
        this.validateTargetField(field, shadow, name, "@Shadow");
        
        if (!remap || !this.validateSingleTarget("@Shadow", field)) {
            return;
        }
        
        ObfuscationData<String> obfFieldData = this.obf.getObfField(this.mixin.getPrimaryTargetRef() + "/" + name);
        
        if (obfFieldData.isEmpty()) {
            this.ap.printMessage(Kind.WARNING, "Unable to locate obfuscation mapping for @Shadow field", field, shadow);
            return;
        }

        for (ObfuscationType type : obfFieldData) {
            String fieldName = obfFieldData.get(type);
            this.addFieldMapping(type, name.setObfuscatedName(fieldName));
        }
    }

    /**
     * Register a {@link org.spongepowered.asm.mixin.Shadow} method
     */
    public void registerShadow(ExecutableElement method, AnnotationMirror shadow, boolean remap) {
        ShadowElementName name = new ShadowElementName(method, shadow);
        this.validateTargetMethod(method, shadow, name, "@Shadow");
        
        if (!remap || !this.validateSingleTarget("@Shadow", method)) {
            return;
        }
        
        String mcpSignature = MirrorUtils.generateSignature(method);
        ObfuscationData<SrgMethod> obfData = this.obf.getObfMethod(new SrgMethod(this.mixin.getPrimaryTargetRef() + "/" + name, mcpSignature));
        
        if (obfData.isEmpty()) {
            this.ap.printMessage(Kind.WARNING, "Unable to locate obfuscation mapping for @Shadow method", method, shadow);
            return;
        }
        
        for (ObfuscationType type : obfData) {
            SrgMethod obfMethod = obfData.get(type);
            this.addMethodMapping(type, name.setObfuscatedName(obfMethod.getName()), mcpSignature, obfMethod.getDesc());
        }
    }

}
