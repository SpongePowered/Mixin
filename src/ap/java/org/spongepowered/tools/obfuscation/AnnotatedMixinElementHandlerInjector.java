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
import java.util.Map;

import javax.lang.model.element.ExecutableElement;
import javax.tools.Diagnostic.Kind;

import org.spongepowered.asm.mixin.injection.struct.InvalidMemberDescriptorException;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.tools.obfuscation.ReferenceManager.ReferenceConflictException;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;
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

        public AnnotatedElementInjector(ExecutableElement element, AnnotationHandle annotation, boolean shouldRemap) {
            super(element, annotation);
            this.shouldRemap = shouldRemap;
        }
        
        public boolean shouldRemap() {
            return this.shouldRemap;
        }
        
        @Override
        public String toString() {
            return this.getAnnotation().toString();
        }
        
    }
    
    /**
     * Injection point element
     */
    static class AnnotatedElementInjectionPoint extends AnnotatedElement<ExecutableElement> {
        
        private final AnnotationHandle at;
        
        private Map<String, String> args;
        
        public AnnotatedElementInjectionPoint(ExecutableElement element, AnnotationHandle inject, AnnotationHandle at) {
            super(element, inject);
            this.at = at;
        }
        
        public AnnotationHandle getAt() {
            return this.at;
        }
        
        public String getAtArg(String key) {
            if (this.args == null) {
                this.args = new HashMap<String, String>();
                for (String arg : this.at.<String>getList("args")) {
                    if (arg == null) {
                        continue;
                    }
                    int eqPos = arg.indexOf('=');
                    if (eqPos > -1) {
                        this.args.put(arg.substring(0, eqPos), arg.substring(eqPos + 1));
                    } else {
                        this.args.put(arg, "");
                    }
                }
            }
            
            return this.args.get(key);
        }
    }
    
    AnnotatedMixinElementHandlerInjector(IMixinAnnotationProcessor ap, AnnotatedMixin mixin) {
        super(ap, mixin);
    }

    public Message registerInjector(AnnotatedElementInjector elem) {
        if (this.mixin.isInterface()) {
            this.ap.printMessage(Kind.ERROR, "Injector in interface is unsupported", elem.getElement());
        }
        
        String reference = elem.getAnnotation().<String>getValue("method");
        MemberInfo targetMember = MemberInfo.parse(reference);
        if (targetMember.name == null) {
            return null;
        }

        try {
            targetMember.validate();
        } catch (InvalidMemberDescriptorException ex) {
            elem.printMessage(this.ap, Kind.ERROR, ex.getMessage());
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
            if (target.isSimulated()) {
                elem.printMessage(this.ap, Kind.NOTE, elem + " target '" + reference + "' in @Pseudo mixin will not be obfuscated");
            } else if (target.isImaginary()) {
                elem.printMessage(this.ap, error, elem + " target requires method signature because enclosing type information for " 
                        + target + " is unavailable");
            } else if (!Constants.CTOR.equals(targetMember.name)) {
                elem.printMessage(this.ap, error, "Unable to determine signature for " + elem + " target method");
            }
            return null;
        }
        
        String targetName = elem + " target " + targetMember.name;
        MappingMethod targetMethod = target.getMappingMethod(targetMember.name, desc);
        ObfuscationData<MappingMethod> obfData = this.obf.getDataProvider().getObfMethod(targetMethod);
        if (obfData.isEmpty()) {
            if (target.isSimulated()) {
                obfData = this.obf.getDataProvider().getRemappedMethod(targetMethod);
            } else {
                Kind error = Constants.CTOR.equals(targetMember.name) ? Kind.WARNING : Kind.ERROR;
                return new Message(error, "No obfuscation mapping for " + targetName, elem.getElement(), elem.getAnnotation());
            }
        }
        
        try {
            // If the original owner is unspecified, and the mixin is multi-target, we strip the owner from the obf mappings
            if ((targetMember.owner == null && this.mixin.isMultiTarget()) || target.isSimulated()) {
                obfData = AnnotatedMixinElementHandler.<MappingMethod>stripOwnerData(obfData);
            }
            this.obf.getReferenceManager().addMethodMapping(this.classRef, reference, obfData);
        } catch (ReferenceConflictException ex) {
            String conflictType = this.mixin.isMultiTarget() ? "Multi-target" : "Target";
            elem.printMessage(this.ap, Kind.ERROR, conflictType + " reference conflict for " + targetName + ": " + reference + " -> "
                    + ex.getNew() + " previously defined as " + ex.getOld());
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
        
        if (!elem.getAt().getBoolean("remap", true)) {
            return 0;
        }
        
        String type = elem.getAt().<String>getValue("value");
        String target = elem.getAt().<String>getValue("target");
        int remapped = 0;
        
        if ("NEW".equals(type)) {
            remapped += this.remapNewTarget("@At(" + type + ".<target>)", target, elem) ? 1 : 0;
            remapped += this.remapNewTarget("@At(" + type + ".args[class])", elem.getAtArg("class"), elem) ? 1 : 0;
        } else {
            remapped += this.remapReference("@At(" + type + ".<target>)", target, elem) ? 1 : 0;
        }
        
        return remapped;
    }
    
    protected final boolean remapNewTarget(String subject, String reference, AnnotatedElementInjectionPoint elem) {
        if (reference == null) {
            return false;
        }

        MemberInfo member = MemberInfo.parse(reference);
        String target = member.toCtorType();
        
        if (target != null) {
            String desc = member.toCtorDesc();
            MappingMethod m = new MappingMethod(target, ".", desc != null ? desc : "()V");
            ObfuscationData<MappingMethod> remapped = this.obf.getDataProvider().getRemappedMethod(m);
            if (remapped.isEmpty()) {
                this.ap.printMessage(Kind.WARNING, "Cannot find class mapping for " + subject + " '" + target + "'", elem.getElement(),
                        elem.getAnnotation().asMirror());
                return false;
            }

            ObfuscationData<String> mappings = new ObfuscationData<String>();
            for (ObfuscationType type : remapped) {
                MappingMethod mapping = remapped.get(type);
                mappings.add(type, mapping.getDesc().replace(")V", ")L" + mapping.getOwner() + ";"));
            }
            
            this.obf.getReferenceManager().addClassMapping(this.classRef, reference, mappings);
        }        

        return true;
    }
    
    protected final boolean remapReference(String subject, String reference, AnnotatedElementInjectionPoint elem) {
        if (reference == null) {
            return false;
        }
        
        MemberInfo targetMember = MemberInfo.parse(reference);
        if (!targetMember.isFullyQualified()) {
            String missing = "missing " + (targetMember.owner == null ? (targetMember.desc == null ? "owner and signature" : "owner") : "signature");
            this.ap.printMessage(Kind.ERROR, subject + " is not fully qualified, " + missing, elem.getElement(), elem.getAnnotation().asMirror());
            return false;
        }
        
        try {
            targetMember.validate();
        } catch (InvalidMemberDescriptorException ex) {
            this.ap.printMessage(Kind.ERROR, ex.getMessage(), elem.getElement(), elem.getAnnotation().asMirror());
        }
        
        try {
            if (targetMember.isField()) {
                ObfuscationData<MappingField> obfFieldData = this.obf.getDataProvider().getObfFieldRecursive(targetMember);
                if (obfFieldData.isEmpty()) {
                    this.ap.printMessage(Kind.WARNING, "Cannot find field mapping for " + subject + " '" + reference + "'", elem.getElement(),
                            elem.getAnnotation().asMirror());
                    return false;
                }
                this.obf.getReferenceManager().addFieldMapping(this.classRef, reference, targetMember, obfFieldData);
            } else {
                ObfuscationData<MappingMethod> obfMethodData = this.obf.getDataProvider().getObfMethodRecursive(targetMember);
                if (obfMethodData.isEmpty()) {
                    if (targetMember.owner == null || !targetMember.owner.startsWith("java/lang/")) {
                        this.ap.printMessage(Kind.WARNING, "Cannot find method mapping for " + subject + " '" + reference + "'", elem.getElement(),
                                elem.getAnnotation().asMirror());
                        return false;
                    }
                }
                this.obf.getReferenceManager().addMethodMapping(this.classRef, reference, targetMember, obfMethodData);
            }
        } catch (ReferenceConflictException ex) {
            // Since references are fully-qualified, it shouldn't be possible for there to be multiple mappings, however
            // we catch and log the error in case something weird happens in the mapping provider
            this.ap.printMessage(Kind.ERROR, "Unexpected reference conflict for " + subject + ": " + reference + " -> "
                    + ex.getNew() + " previously defined as " + ex.getOld(), elem.getElement(), elem.getAnnotation().asMirror());
            return false;
        }
        
        return true;
    }

}
