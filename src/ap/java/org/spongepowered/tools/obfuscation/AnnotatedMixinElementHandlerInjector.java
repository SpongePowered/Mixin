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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;

import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorByName;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorConstructor;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorRemappable;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelector;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.injection.struct.InvalidMemberDescriptorException;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.ReferenceManager.ReferenceConflictException;
import org.spongepowered.tools.obfuscation.ext.SpecialPackages;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor.CompilerEnvironment;
import org.spongepowered.tools.obfuscation.interfaces.IReferenceManager;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeUtils;
import org.spongepowered.tools.obfuscation.struct.InjectorRemap;

/**
 * A module for {@link AnnotatedMixin} whic handles injectors
 */
class AnnotatedMixinElementHandlerInjector extends AnnotatedMixinElementHandler {

    /**
     * Injector element
     */
    static class AnnotatedElementInjector extends AnnotatedElement<ExecutableElement> {
        
        private final InjectorRemap state;

        public AnnotatedElementInjector(ExecutableElement element, AnnotationHandle annotation, InjectorRemap shouldRemap) {
            super(element, annotation);
            this.state = shouldRemap;
        }
        
        public boolean shouldRemap() {
            return this.state.shouldRemap();
        }
        
        public boolean hasCoerceArgument() {
            if (!this.annotation.toString().equals("@Inject")) {
                return false;
            }
            
            for (VariableElement param : this.element.getParameters()) {
                return AnnotationHandle.of(param, Coerce.class).exists();
            }
            
            return false;
        }
        
        public void addMessage(Diagnostic.Kind kind, CharSequence msg, Element element, AnnotationHandle annotation) {
            this.state.addMessage(kind, msg, element, annotation);
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
        
        private final InjectorRemap state;

        public AnnotatedElementInjectionPoint(ExecutableElement element, AnnotationHandle inject, AnnotationHandle at, InjectorRemap state) {
            super(element, inject);
            this.at = at;
            this.state = state;
        }
        
        public boolean shouldRemap() {
            return this.at.getBoolean("remap", this.state.shouldRemap());
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
        
        public void notifyRemapped() {
            this.state.notifyRemapped();
        }
    
    }
    
    private final TypeMirror VOID_TYPE;
    
    AnnotatedMixinElementHandlerInjector(IMixinAnnotationProcessor ap, AnnotatedMixin mixin) {
        super(ap, mixin);
        VOID_TYPE = this.ap.getProcessingEnvironment().getTypeUtils().getNoType(TypeKind.VOID);
    }

    public void registerInjector(AnnotatedElementInjector elem) {
        if (this.mixin.isInterface()) {
            this.ap.printMessage(Kind.ERROR, "Injector in interface is unsupported", elem.getElement());
        }
        
        List<String> references = new ArrayList<String>();
        List<ITargetSelector> selectors = new ArrayList<ITargetSelector>();
        
        for (String reference : elem.getAnnotation().<String>getList("method")) {
        	references.add(reference);
        	selectors.add(TargetSelector.parse(reference));
        }
        
        for (AnnotationHandle target : elem.getAnnotation().getAnnotationList("target")) {
        	TypeMirror ownerType = target.<TypeMirror>getValue("owner", VOID_TYPE);
        	String owner = null;
        	if (ownerType.getKind() == TypeKind.VOID)
        		owner = TypeUtils.getInternalName((DeclaredType)this.mixin.getTargets().get(0).getType());
        	else
        		owner = TypeUtils.getInternalName((DeclaredType)ownerType);
        	String name = target.<String>getValue();
        	String desc = TypeUtils.getDescriptor(target.<TypeMirror>getValue("ret", VOID_TYPE), target.<TypeMirror>getList("args"));
        	references.add(owner + '/' + name + desc);
        	selectors.add(new MemberInfo(name, owner, desc));
            //this.ap.printMessage(Kind.ERROR, "Annotated Descriptor: " + owner + '/' + name + desc, elem.getElement(), target.asMirror());
        }
        
        for (int x = 0; x < references.size(); x++) {
            String reference = references.get(x);
            ITargetSelector targetSelector = selectors.get(x);
            
            try {
                targetSelector.validate();
            } catch (InvalidMemberDescriptorException ex) {
                elem.printMessage(this.ap, Kind.ERROR, ex.getMessage());
            }

            if (!(targetSelector instanceof ITargetSelectorRemappable)) {
                continue;
            }
            
            ITargetSelectorRemappable targetMember = (ITargetSelectorRemappable)targetSelector;
            if (targetMember.getName() == null) {
                continue;
            }
            
            if (targetMember.getDesc() != null) {
                this.validateReferencedTarget(elem.getElement(), elem.getAnnotation(), targetMember, elem.toString());
            }
            
            if (!elem.shouldRemap()) {
                continue;
            }
            
            for (TypeHandle target : this.mixin.getTargets()) {
                if (!this.registerInjector(elem, reference, targetMember, target)) {
                    break;
                }
            }
        }
    }

    private boolean registerInjector(AnnotatedElementInjector elem, String reference, ITargetSelectorRemappable targetMember, TypeHandle target) {
        String desc = target.findDescriptor(targetMember);
        if (desc == null) {
            Kind error = this.mixin.isMultiTarget() ? Kind.ERROR : Kind.WARNING;
            if (target.isSimulated()) {
                elem.printMessage(this.ap, Kind.NOTE, elem + " target '" + reference + "' in @Pseudo mixin will not be obfuscated");
            } else if (target.isImaginary()) {
                elem.printMessage(this.ap, error, elem + " target requires method signature because enclosing type information for " 
                        + target + " is unavailable");
            } else if (!targetMember.isInitialiser()) {
                elem.printMessage(this.ap, error, "Unable to determine signature for " + elem + " target method");
            }
            return true;
        }
        
        String targetName = elem + " target " + targetMember.getName();
        MappingMethod targetMethod = target.getMappingMethod(targetMember.getName(), desc);
        ObfuscationData<MappingMethod> obfData = this.obf.getDataProvider().getObfMethod(targetMethod);
        if (obfData.isEmpty()) {
            if (target.isSimulated()) {
                obfData = this.obf.getDataProvider().getRemappedMethod(targetMethod);
            } else if (targetMember.isClassInitialiser()) {
                return true;
            } else {
                Kind error = targetMember.isConstructor() ? Kind.WARNING : Kind.ERROR;
                elem.addMessage(error, "No obfuscation mapping for " + targetName, elem.getElement(), elem.getAnnotation());
                return false;
            }
        }
        
        IReferenceManager refMap = this.obf.getReferenceManager();
        try {
            // If the original owner is unspecified, and the mixin is multi-target, we strip the owner from the obf mappings
            if ((targetMember.getOwner() == null && this.mixin.isMultiTarget()) || target.isSimulated()) {
                obfData = AnnotatedMixinElementHandler.<MappingMethod>stripOwnerData(obfData);
            }
            refMap.addMethodMapping(this.classRef, reference, obfData);
        } catch (ReferenceConflictException ex) {
            String conflictType = this.mixin.isMultiTarget() ? "Multi-target" : "Target";
            
            if (elem.hasCoerceArgument() && targetMember.getOwner() == null && targetMember.getDesc() == null) {
                ITargetSelector oldMember = TargetSelector.parse(ex.getOld());
                ITargetSelector newMember = TargetSelector.parse(ex.getNew());
                String oldName = oldMember instanceof ITargetSelectorByName ? ((ITargetSelectorByName)oldMember).getName() : oldMember.toString();
                String newName = newMember instanceof ITargetSelectorByName ? ((ITargetSelectorByName)newMember).getName() : newMember.toString();
                if (oldName != null && oldName.equals(newName)) {
                    obfData = AnnotatedMixinElementHandler.<MappingMethod>stripDescriptors(obfData);
                    refMap.setAllowConflicts(true);
                    refMap.addMethodMapping(this.classRef, reference, obfData);
                    refMap.setAllowConflicts(false);

                    // This is bad because in notch mappings, using the bare target name might cause everything to explode
                    elem.printMessage(this.ap, Kind.WARNING, "Coerced " + conflictType + " reference has conflicting descriptors for " + targetName
                            + ": Storing bare references " + obfData.values() + " in refMap");
                    return true;
                }
            }
            
            elem.printMessage(this.ap, Kind.ERROR, conflictType + " reference conflict for " + targetName + ": " + reference + " -> "
                    + ex.getNew() + " previously defined as " + ex.getOld());
        }
        
        return true;
    }

    /**
     * Register a {@link org.spongepowered.asm.mixin.injection.At} annotation
     * and process the references
     */
    public void registerInjectionPoint(AnnotatedElementInjectionPoint elem, String format) {
        if (this.mixin.isInterface()) {
            this.ap.printMessage(Kind.ERROR, "Injector in interface is unsupported", elem.getElement());
        }
        
        if (!elem.shouldRemap()) {
            return;
        }
        
        String type = InjectionPointData.parseType(elem.getAt().<String>getValue("value"));
        String target = elem.getAt().<String>getValue("target");
        
        if ("NEW".equals(type)) {
            this.remapNewTarget(String.format(format, type + ".<target>"), target, elem);
            this.remapNewTarget(String.format(format, type + ".args[class]"), elem.getAtArg("class"), elem);
        } else {
            this.remapReference(String.format(format, type + ".<target>"), target, elem);
        }
    }
    
    protected final void remapNewTarget(String subject, String reference, AnnotatedElementInjectionPoint elem) {
        if (reference == null) {
            return;
        }

        ITargetSelector selector = TargetSelector.parse(reference);
        if (selector instanceof ITargetSelectorConstructor) {
            ITargetSelectorConstructor member = (ITargetSelectorConstructor)selector;
            String target = member.toCtorType();
            
            if (target != null) {
                String desc = member.toCtorDesc();
                MappingMethod m = new MappingMethod(target, ".", desc != null ? desc : "()V");
                ObfuscationData<MappingMethod> remapped = this.obf.getDataProvider().getRemappedMethod(m);
                if (remapped.isEmpty() && !SpecialPackages.isExcludedPackage(member.toCtorType())) {
                    this.ap.printMessage(Kind.WARNING, "Cannot find class mapping for " + subject + " '" + target + "'", elem.getElement(),
                            elem.getAnnotation().asMirror(), SuppressedBy.MAPPING);
                    return;
                }
    
                ObfuscationData<String> mappings = new ObfuscationData<String>();
                for (ObfuscationType type : remapped) {
                    MappingMethod mapping = remapped.get(type);
                    if (desc == null) {
                        mappings.put(type, mapping.getOwner());
                    } else {
                        mappings.put(type, mapping.getDesc().replace(")V", ")L" + mapping.getOwner() + ";"));
                    }
                }
                
                this.obf.getReferenceManager().addClassMapping(this.classRef, reference, mappings);
            }
        }

        elem.notifyRemapped();
    }
    
    protected final void remapReference(String subject, String reference, AnnotatedElementInjectionPoint elem) {
        if (reference == null) {
            return;
        }

        ITargetSelector targetSelector = TargetSelector.parse(reference);
        if (!(targetSelector instanceof ITargetSelectorRemappable)) {
            return;
        }
        ITargetSelectorRemappable targetMember = (ITargetSelectorRemappable)targetSelector;

        // JDT supports hanging the error on the @At annotation directly, doing this in javac doesn't work 
        AnnotationMirror errorsOn = (this.ap.getCompilerEnvironment() == CompilerEnvironment.JDT ? elem.getAt() : elem.getAnnotation()).asMirror();
        
        if (!targetMember.isFullyQualified()) {
            String missing = targetMember.getOwner() == null ? (targetMember.getDesc() == null ? "owner and signature" : "owner") : "signature";
            this.ap.printMessage(Kind.ERROR, subject + " is not fully qualified, missing " + missing, elem.getElement(), errorsOn);
            return;
        }
        
        try {
            targetMember.validate();
        } catch (InvalidMemberDescriptorException ex) {
            this.ap.printMessage(Kind.ERROR, ex.getMessage(), elem.getElement(), errorsOn);
        }
        
        try {
            if (targetMember.isField()) {
                ObfuscationData<MappingField> obfFieldData = this.obf.getDataProvider().getObfFieldRecursive(targetMember);
                if (obfFieldData.isEmpty()) {
                    if (targetMember.getOwner() == null || !SpecialPackages.isExcludedPackage(targetMember.getOwner())) {
                        this.ap.printMessage(Kind.WARNING, "Cannot find field mapping for " + subject + " '" + reference + "'", elem.getElement(),
                                errorsOn, SuppressedBy.MAPPING);
                    }
                    return;
                }
                this.obf.getReferenceManager().addFieldMapping(this.classRef, reference, targetMember, obfFieldData);
            } else {
                ObfuscationData<MappingMethod> obfMethodData = this.obf.getDataProvider().getObfMethodRecursive(targetMember);
                if (obfMethodData.isEmpty()) {
                    if (targetMember.getOwner() == null || !SpecialPackages.isExcludedPackage(targetMember.getOwner())) {
                        this.ap.printMessage(Kind.WARNING, "Cannot find method mapping for " + subject + " '" + reference + "'", elem.getElement(),
                                errorsOn, SuppressedBy.MAPPING);
                    }
                    return;
                }
                this.obf.getReferenceManager().addMethodMapping(this.classRef, reference, targetMember, obfMethodData);
            }
        } catch (ReferenceConflictException ex) {
            // Since references are fully-qualified, it shouldn't be possible for there to be multiple mappings, however
            // we catch and log the error in case something weird happens in the mapping provider
            this.ap.printMessage(Kind.ERROR, "Unexpected reference conflict for " + subject + ": " + reference + " -> "
                    + ex.getNew() + " previously defined as " + ex.getOld(), elem.getElement(), errorsOn);
            return;
        }
        
        elem.notifyRemapped();
    }

}
