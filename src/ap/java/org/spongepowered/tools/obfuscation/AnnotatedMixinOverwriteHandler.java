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

import org.spongepowered.tools.MirrorUtils;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;

import net.minecraftforge.srg2source.rangeapplier.MethodData;

class AnnotatedMixinOverwriteHandler extends AnnotatedMixinElementHandler {
    
    AnnotatedMixinOverwriteHandler(IMixinAnnotationProcessor ap, AnnotatedMixin mixin) {
        super(ap, mixin);
    }

    void registerOverwrite(ExecutableElement method, AnnotationMirror overwrite) {
        AliasedElementName name = new AliasedElementName(method, overwrite);
        this.validateTargetMethod(method, overwrite, name, "@Overwrite");
        this.checkConstraints(method, overwrite);
        
        if (!this.mixin.remap() || !this.validateSingleTarget("@Overwrite", method)) {
            return;
        }
        
        String mcpName = method.getSimpleName().toString();
        String mcpSignature = MirrorUtils.generateSignature(method);
        ObfuscationData<MethodData> obfData = this.obf.getObfMethod(new MethodData(this.mixin.getPrimaryTargetRef() + "/" + mcpName, mcpSignature));
        
        if (obfData.isEmpty()) {
            Kind error = Kind.ERROR;
            
            try {
                // Try to access isStatic from com.sun.tools.javac.code.Symbol
                Method md = method.getClass().getMethod("isStatic");
                if (((Boolean)md.invoke(method)).booleanValue()) {
                    error = Kind.WARNING;
                }
            } catch (Exception ex) {
                // well, we tried
            }
            
            this.ap.printMessage(error, "No obfuscation mapping for @Overwrite method", method);
            return;
        }

        for (ObfuscationType type : obfData) {
            MethodData obfMethod = obfData.get(type);
            String obfName = obfMethod.name.substring(obfMethod.name.lastIndexOf('/') + 1);
            this.addMethodMapping(type, mcpName, obfName, mcpSignature, obfMethod.sig);
        }
        
        if (!"true".equalsIgnoreCase(this.ap.getOption(SupportedOptions.DISABLE_OVERWRITE_CHECKER))) {
            Kind overwriteErrorKind = "error".equalsIgnoreCase(this.ap.getOption(SupportedOptions.OVERWRITE_ERROR_LEVEL))
                    ? Kind.ERROR : Kind.WARNING;
            
            String javadoc = this.ap.getJavadocProvider().getJavadoc(method);
            if (javadoc == null) {
                this.ap.printMessage(overwriteErrorKind, "@Overwrite is missing javadoc comment", method);
                return;
            }
            
            if (!javadoc.toLowerCase().contains("@author")) {
                this.ap.printMessage(overwriteErrorKind, "@Overwrite is missing an @author tag", method);
            }
            
            if (!javadoc.toLowerCase().contains("@reason")) {
                this.ap.printMessage(overwriteErrorKind, "@Overwrite is missing an @reason tag", method);
            }
        }
    }
    
}
