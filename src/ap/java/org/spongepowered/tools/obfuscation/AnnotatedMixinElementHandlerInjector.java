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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.tools.Diagnostic.Kind;

import org.spongepowered.asm.mixin.injection.struct.InvalidMemberDescriptorException;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.tools.MirrorUtils;
import org.spongepowered.tools.obfuscation.ReferenceManager.ReferenceConflictException;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.struct.Message;

/**
 * A module for {@link AnnotatedMixin} whic handles injectors
 */
class AnnotatedMixinElementHandlerInjector extends AnnotatedMixinElementHandler {

    /**
     * Injector element
     */
    static class AnnotatedElementInjector extends AnnotatedElement<ExecutableElement> {
        
        private final boolean shouldRemap;

        public AnnotatedElementInjector(ExecutableElement element, AnnotationMirror annotation, boolean shouldRemap) {
            super(element, annotation);
            this.shouldRemap = shouldRemap;
        }
        
        public boolean shouldRemap() {
            return this.shouldRemap;
        }
        
        @Override
        public String toString() {
            return "@" + this.getAnnotation().getAnnotationType().asElement().getSimpleName();
        }
        
    }
    
    /**
     * Injection point element
     */
    static class AnnotatedElementInjectionPoint extends AnnotatedElement<ExecutableElement> {
        
        private final AnnotationMirror at;
        
        public AnnotatedElementInjectionPoint(ExecutableElement element, AnnotationMirror inject, AnnotationMirror at) {
            super(element, inject);
            this.at = at;
        }
        
        public AnnotationMirror getAt() {
            return this.at;
        }
        
    }
    
    AnnotatedMixinElementHandlerInjector(IMixinAnnotationProcessor ap, AnnotatedMixin mixin) {
        super(ap, mixin);
    }

    public Message registerInjector(AnnotatedElementInjector elem) {
        if (this.mixin.isInterface()) {
            this.ap.printMessage(Kind.ERROR, "Injector in interface is unsupported", elem.getElement());
        }
        
        String reference = MirrorUtils.<String>getAnnotationValue(elem.getAnnotation(), "method");
        MemberInfo targetMember = MemberInfo.parse(reference);
        if (targetMember.name == null) {
            return null;
        }

        try {
            targetMember.validate();
        } catch (InvalidMemberDescriptorException ex) {
            this.ap.printMessage(Kind.ERROR, ex.getMessage(), elem.getElement(), elem.getAnnotation());
        }
        
        if (targetMember.desc != null) {
            this.validateReferencedTarget(elem.getElement(), elem.getAnnotation(), targetMember, elem.toString());
        }
        
        if (!elem.shouldRemap()) {
            return null;
        }
        
        for (TypeHandle target : this.mixin.getTargets()) {
            Message message = this.registerInjector(elem, reference, targetMember, target);
            if (message != null) {
                return message;
            }
        }
        return null;
    }

    private Message registerInjector(AnnotatedElementInjector elem, String reference, MemberInfo targetMember, TypeHandle target) {
        String desc = target.findDescriptor(targetMember);
        if (desc == null) {
            Kind error = this.mixin.isMultiTarget() ? Kind.ERROR : Kind.WARNING;
            if (target.isImaginary()) {
                this.ap.printMessage(error, elem + " target requires method signature because enclosing type information for " 
                        + target + " is unavailable", elem.getElement(), elem.getAnnotation());
            } else if (!Constants.CTOR.equals(targetMember.name)) {
                this.ap.printMessage(error, "Unable to determine signature for " + elem + " target method",
                        elem.getElement(), elem.getAnnotation());
            }
            return null;
        }
        
        String targetName = elem + " target " + targetMember.name;
        MappingMethod targetMethod = new MappingMethod(target.getName(), targetMember.name, desc);
        ObfuscationData<MappingMethod> obfData = this.obf.getDataProvider().getObfMethod(targetMethod);
        if (obfData.isEmpty()) {
            Kind error = Constants.CTOR.equals(targetMember.name) ? Kind.WARNING : Kind.ERROR;
            return new Message(error, "No obfuscation mapping for " + targetName, elem.getElement(), elem.getAnnotation());
        }
        
        try {
            // If the original owner is unspecified, and the mixin is multi-target, we strip the owner from the obf mappings
            if (targetMember.owner == null && this.mixin.isMultiTarget()) {
                obfData = AnnotatedMixinElementHandler.<MappingMethod>stripOwnerData(obfData);
            }
            this.obf.getReferenceManager().addMethodMapping(this.classRef, reference, obfData);
        } catch (ReferenceConflictException ex) {
            String conflictType = this.mixin.isMultiTarget() ? "Multi-target" : "Target";
            this.ap.printMessage(Kind.ERROR, conflictType + " reference conflict for " + targetName + ": " + reference + " -> "
                    + ex.getNew() + " previously defined as " + ex.getOld(), elem.getElement(), elem.getAnnotation());
        }
        
        return null;
    }

    /**
     * Register a {@link org.spongepowered.asm.mixin.injection.At} annotation
     * and process the references
     */
    public int registerInjectionPoint(AnnotatedElementInjectionPoint elem) {
        if (this.mixin.isInterface()) {
            this.ap.printMessage(Kind.ERROR, "Injector in interface is unsupported", elem.getElement());
        }
        
        if (!AnnotatedMixins.getRemapValue(elem.getAt())) {
            return 0;
        }
        
        String type = MirrorUtils.<String>getAnnotationValue(elem.getAt(), "value");
        String target = MirrorUtils.<String>getAnnotationValue(elem.getAt(), "target");
        int remapped = this.remapReference(type + ".<target>", target, elem.getElement(), elem.getAnnotation(), elem.getAt()) ? 1 : 0;
        
        // Pattern for replacing references in args, not used yet
//        if ("SOMETYPE".equals(type)) {
//            Map<String, String> args = InjectorHandler.getAtArgs(e.getAt());
//            this.remapReference(type + ".args[target]", args.get("target"));
//        }
        
        return remapped;
    }

    static Map<String, String> getAtArgs(AnnotationMirror at) {
        Map<String, String> args = new HashMap<String, String>();
        List<AnnotationValue> argv = MirrorUtils.<List<AnnotationValue>>getAnnotationValue(at, "args");
        if (argv != null) {
            for (AnnotationValue av : argv) {
                String arg = (String)av.getValue();
                if (arg == null) {
                    continue;
                }
                int eqPos = arg.indexOf('=');
                if (eqPos > -1) {
                    args.put(arg.substring(0, eqPos), arg.substring(eqPos + 1));
                } else {
                    args.put(arg, "");
                }
            }
        }
        return args;
    }

}
