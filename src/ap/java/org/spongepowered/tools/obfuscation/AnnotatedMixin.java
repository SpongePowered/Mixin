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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

import net.minecraftforge.srg2source.rangeapplier.MethodData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.struct.InvalidMemberDescriptorException;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.ConstraintParser;
import org.spongepowered.asm.util.ConstraintParser.Constraint;
import org.spongepowered.asm.util.ConstraintViolationException;
import org.spongepowered.asm.util.InvalidConstraintException;
import org.spongepowered.tools.MirrorUtils;
import org.spongepowered.tools.obfuscation.IMixinValidator.ValidationPass;
import org.spongepowered.tools.obfuscation.struct.Message;


/**
 * Information about a mixin stored during processing
 */
class AnnotatedMixin {
    
    /**
     * A name of an element which may have aliases
     */
    static class AliasedElementName {
        
        /**
         * The original name including any original prefix (the "actual" name) 
         */
        protected final String originalName;
        
        /**
         * Aliases declared by the annotation (if any), never null 
         */
        private final List<String> aliases;
        
        public AliasedElementName(Element element, AnnotationMirror annotation) {
            this.originalName = element.getSimpleName().toString();
            List<AnnotationValue> aliases = MirrorUtils.<List<AnnotationValue>>getAnnotationValue(annotation, "aliases");
            this.aliases = MirrorUtils.<String>unfold(aliases);
        }
        
        /**
         * Get whether this member has any aliases defined
         */
        public boolean hasAliases() {
            return this.aliases.size() > 0;
        }
        
        /**
         * Get this member's aliases
         */
        public List<String> getAliases() {
            return this.aliases;
        }
        
        /**
         * Gets the original name of the member (including prefix)
         */
        public String elementName() {
            return this.originalName;
        }

        public String baseName() {
            return this.originalName;
        }

    }
    
    /**
     * Convenience class to store information about an
     * {@link org.spongepowered.asm.mixin.Shadow}ed member's names
     */
    static class ShadowElementName extends AliasedElementName {
        
        /**
         * True if the real element is prefixed
         */
        private final boolean hasPrefix;
        
        /**
         * Expected prefix read from the annotation, this is set even if
         * {@link #hasPrefix} is false
         */
        private final String prefix;
        
        /**
         * The base name without the prefix
         */
        private final String baseName;
        
        /**
         * Obfuscated name (once determined) 
         */
        private String obfuscated;
        
        ShadowElementName(Element element, AnnotationMirror shadow) {
            super(element, shadow);
            
            this.prefix = MirrorUtils.<String>getAnnotationValue(shadow, "prefix", "shadow$");
            
            boolean hasPrefix = false;
            String name = this.originalName;
            if (name.startsWith(this.prefix)) {
                hasPrefix = true;
                name = name.substring(this.prefix.length());
            }
            
            this.hasPrefix = hasPrefix;
            this.obfuscated = this.baseName = name;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return this.baseName;
        }
        
        @Override
        public String baseName() {
            return this.baseName;
        }

        /**
         * Sets the obfuscated name for this element
         * 
         * @param name New name
         * @return fluent interface
         */
        public ShadowElementName setObfuscatedName(String name) {
            this.obfuscated = name.substring(name.lastIndexOf('/') + 1);
            return this;
        }

        /**
         * Get the prefix (if set), does not return the expected prefix
         */
        public String prefix() {
            return this.hasPrefix ? this.prefix : "";
        }
        
        /**
         * Get the base name
         */
        public String name() {
            return this.prefix(this.baseName);
        }
        
        /**
         * Gets the obfuscated name (including prefix where appropriate
         */
        public String obfuscated() {
            return this.prefix(this.obfuscated);
        }
        
        /**
         * Apply the prefix (if any) to the specified string
         * 
         * @param name String to prefix
         * @return Prefixed string or original string if no prefix
         */
        public String prefix(String name) {
            return this.hasPrefix ? this.prefix + name : name;
        }
    }
    
    /**
     * Mixin annotation
     */
    private final AnnotationMirror annotation;
    
   /**
     * Manager
     */
    private final AnnotatedMixins mixins;
    
    /**
     * Mixin class
     */
    private final TypeElement mixin;
    
    /**
     * Mixin class
     */
    private final TypeHandle handle;
    
    /**
     * Specified targets
     */
    private final List<TypeHandle> targets = new ArrayList<TypeHandle>();
    
    /**
     * Target "reference" (bytecode name)
     */
    private final String targetRef;
    
    /**
     * Target type (for single-target mixins) 
     */
    private final TypeHandle targetType;
    
    /**
     * Mixin class "reference" (bytecode name)
     */
    private final String classRef;
    
    /**
     * True if we will actually process remappings for this mixin
     */
    private final boolean remap;
    
    /**
     * Stored (ordered) field mappings
     */
    private final Map<ObfuscationType, Set<String>> fieldMappings = new HashMap<ObfuscationType, Set<String>>();
    
    /**
     * Stored (ordered) method mappings
     */
    private final Map<ObfuscationType, Set<String>> methodMappings = new HashMap<ObfuscationType, Set<String>>();

    public AnnotatedMixin(AnnotatedMixins mixins, TypeElement type) {
        
        this.annotation = MirrorUtils.getAnnotation(type, Mixin.class);
        this.mixins = mixins;
        this.mixin = type;
        this.handle = new TypeHandle(type);
        this.classRef = type.getQualifiedName().toString().replace('.', '/');
        
        TypeHandle primaryTarget = this.initTargets();
        if (primaryTarget != null) {
            this.targetRef = primaryTarget.getName();
            this.targetType = primaryTarget; 
        } else {
            this.targetRef = null;
            this.targetType = null;
        }

        this.remap = AnnotatedMixins.getRemapValue(this.annotation) && this.targets.size() > 0;
        
        for (ObfuscationType obfType : ObfuscationType.values()) {
            this.fieldMappings.put(obfType, new LinkedHashSet<String>());
            this.methodMappings.put(obfType, new LinkedHashSet<String>());
        }
    }

    AnnotatedMixin runValidators(ValidationPass pass, Collection<IMixinValidator> validators) {
        for (IMixinValidator validator : validators) {
            if (!validator.validate(pass, this.mixin, this.annotation, this.targets)) {
                break;
            }
        }
        
        return this;
    }

    private TypeHandle initTargets() {
        TypeHandle primaryTarget = null;
        
        // Public targets, referenced by class
        try {
            List<AnnotationValue> publicTargets = MirrorUtils.<List<AnnotationValue>>getAnnotationValue(this.annotation, "value",
                    Collections.<AnnotationValue>emptyList());
            for (TypeMirror target : MirrorUtils.<TypeMirror>unfold(publicTargets)) {
                TypeHandle type = new TypeHandle((DeclaredType)target);
                if (this.targets.contains(type)) {
                    continue;
                }
                this.targets.add(type);
                if (primaryTarget == null) {
                    primaryTarget = type;
                }
            }
        } catch (Exception ex) {
            this.mixins.printMessage(Kind.WARNING, "Error processing public targets: " + ex.getClass().getName() + ": " + ex.getMessage(), this);
        }
        
        // Private targets, referenced by name
        try {
            List<AnnotationValue> privateTargets = MirrorUtils.<List<AnnotationValue>>getAnnotationValue(this.annotation, "targets",
                    Collections.<AnnotationValue>emptyList());
            for (String privateTarget : MirrorUtils.<String>unfold(privateTargets)) {
                TypeHandle type = this.mixins.getTypeHandle(privateTarget);
                if (this.targets.contains(type)) {
                    continue;
                }
                if (type == null) {
                    this.mixins.printMessage(Kind.ERROR, "Mixin target " + privateTarget + " could not be found", this);
                    return null;
                } else if (type.isPublic()) {
                    this.mixins.printMessage(Kind.ERROR, "Mixin target " + privateTarget + " is public and must be specified in value", this);
                    return null;
                }
                this.targets.add(type);
                if (primaryTarget == null) {
                    primaryTarget = type;
                }
            }
        } catch (Exception ex) {
            this.mixins.printMessage(Kind.WARNING, "Error processing private targets: " + ex.getClass().getName() + ": " + ex.getMessage(), this);
        }
        
        if (primaryTarget == null) {
            this.mixins.printMessage(Kind.ERROR, "Mixin has no targets", this);
        }
        
        return primaryTarget;
    }
    
    @Override
    public String toString() {
        return this.mixin.getSimpleName().toString();
    }
    
    public AnnotationMirror getAnnotation() {
        return this.annotation;
    }
    
    /**
     * Get the mixin class
     */
    public TypeElement getMixin() {
        return this.mixin;
    }
    
    /**
     * Get the type handle for the mixin class
     */
    public TypeHandle getHandle() {
        return this.handle;
    }
    
    /**
     * Get the mixin's targets
     */
    public List<TypeHandle> getTargets() {
        return this.targets;
    }
    
    /**
     * Get whether to remap annotations in this mixin
     */
    public boolean remap() {
        return this.remap;
    }
    
    /**
     * Get stored field mappings
     */
    public Set<String> getFieldMappings(ObfuscationType type) {
        return this.fieldMappings.get(type);
    }
    
    /**
     * Get stored method mappings
     */
    public Set<String> getMethodMappings(ObfuscationType type) {
        return this.methodMappings.get(type);
    }
    
    /**
     * Clear all stored mappings
     */
    public void clear() {
        this.fieldMappings.clear();
        this.methodMappings.clear();
    }

    /**
     * Validate method for {@link org.spongepowered.asm.mixin.Shadow} and
     * {@link org.spongepowered.asm.mixin.Overwrite} registrations to check that
     * only a single target is registered. Mixins containing annotated methods
     * with these annotations cannot be multi-targetted.
     */
    private boolean validateSingleTarget(String annotation, Element element) {
        if (this.targetRef == null || this.targets.size() > 1) {
            this.mixins.printMessage(Kind.ERROR, "Mixin with " + annotation + " members must have exactly one target.", element);
            this.mixins.printMessage(Kind.ERROR, "Mixin with " + annotation + " members must have exactly one target.", this.mixin);
            return false;
        }
        return true;
    }

    /**
     * Register an {@link org.spongepowered.asm.mixin.Overwrite} method
     */
    public void registerOverwrite(ExecutableElement method, AnnotationMirror overwrite) {
        AliasedElementName name = new AliasedElementName(method, overwrite);
        this.validateTargetMethod(method, overwrite, name, "@Overwrite");
        this.checkConstraints(method, overwrite);
        
        if (!this.remap || !this.validateSingleTarget("@Overwrite", method)) {
            return;
        }
        
        String mcpName = method.getSimpleName().toString();
        String mcpSignature = MirrorUtils.generateSignature(method);
        ObfuscationData<MethodData> obfMethodData = this.mixins.getObfMethod(new MethodData(this.targetRef + "/" + mcpName, mcpSignature));
        
        if (obfMethodData.isEmpty()) {
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
            
            this.mixins.printMessage(error, "No obfuscation mapping for @Overwrite method", method);
            return;
        }

        for (ObfuscationType type : obfMethodData) {
            MethodData obfMethod = obfMethodData.get(type);
            String obfName = obfMethod.name.substring(obfMethod.name.lastIndexOf('/') + 1);
            this.addMethodMapping(type, mcpName, obfName, mcpSignature, obfMethod.sig);
        }
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
        
        ObfuscationData<String> obfFieldData = this.mixins.getObfField(this.targetRef + "/" + name);
        
        if (obfFieldData.isEmpty()) {
            this.mixins.printMessage(Kind.WARNING, "Unable to locate obfuscation mapping for @Shadow field", field, shadow);
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
        ObfuscationData<MethodData> obfMethodData = this.mixins.getObfMethod(new MethodData(this.targetRef + "/" + name, mcpSignature));
        
        if (obfMethodData.isEmpty()) {
            this.mixins.printMessage(Kind.WARNING, "Unable to locate obfuscation mapping for @Shadow method", method, shadow);
            return;
        }
        
        for (ObfuscationType type : obfMethodData) {
            MethodData obfMethod = obfMethodData.get(type);
            this.addMethodMapping(type, name.setObfuscatedName(obfMethod.name), mcpSignature, obfMethod.sig);
        }
    }

    /**
     * Register a {@link org.spongepowered.asm.mixin.injection.Inject} method
     * 
     * @param method Callback method
     * @param inject Inject annotation
     * @param remap 
     * @return
     */
    public Message registerInjector(ExecutableElement method, AnnotationMirror inject, boolean remap) {
        String originalReference = MirrorUtils.<String>getAnnotationValue(inject, "method");
        MemberInfo targetMember = MemberInfo.parse(originalReference);
        if (targetMember.name == null) {
            return null;
        }

        try {
            targetMember.validate();
        } catch (InvalidMemberDescriptorException ex) {
            this.mixins.printMessage(Kind.ERROR, ex.getMessage(), method, inject);
        }
        
        String type = "@" + inject.getAnnotationType().asElement().getSimpleName() + "";
        if (targetMember.desc != null) {
            this.validateReferencedTarget(method, inject, targetMember, type);
        }
        
        if (!remap || !this.validateSingleTarget(type, method)) {
            return null;
        }
        
        String desc = this.targetType.findDescriptor(targetMember);
        if (desc == null) {
            if (this.targetType.isImaginary()) {
                this.mixins.printMessage(Kind.WARNING, type + " target requires method signature because enclosing type information is unavailable",
                        method, inject);
            } else if (!Constants.INIT.equals(targetMember.name)) {
                this.mixins.printMessage(Kind.WARNING, "Unable to determine signature for " + type + " target method", method, inject);
            }
            return null;
       }
        
        ObfuscationData<MethodData> obfMethod = this.mixins.getObfMethod(new MethodData(this.targetRef + "/" + targetMember.name, desc));
        if (obfMethod.isEmpty()) {
            Kind error = Constants.INIT.equals(targetMember.name) ? Kind.WARNING : Kind.ERROR;
            return new Message(error, "No obfuscation mapping for " + type + " target " + targetMember.name, method, inject);
        }
        
        this.mixins.addMethodMapping(this.classRef, originalReference, obfMethod);
        return null;
    }

    /**
     * Register a {@link org.spongepowered.asm.mixin.injection.At} annotation
     * and process the references
     */
    public int registerInjectionPoint(Element element, AnnotationMirror inject, AnnotationMirror at) {
        if (!AnnotatedMixins.getRemapValue(at)) {
            return 0;
        }
        
        String type = MirrorUtils.<String>getAnnotationValue(at, "value");
        String target = MirrorUtils.<String>getAnnotationValue(at, "target");
        int remapped = this.remapReference(type + ".<target>", target, element, inject, at) ? 1 : 0;
        
        // Pattern for replacing references in args, not used yet
//        if ("SOMETYPE".equals(type)) {
//            Map<String, String> args = AnnotatedMixin.getAtArgs(at);
//            this.remapReference(type + ".args[target]", args.get("target"));
//        }
        
        return remapped;
    }

    private boolean remapReference(String key, String target, Element element, AnnotationMirror inject, AnnotationMirror at) {
        if (target == null) {
            return false;
        }
        
        MemberInfo targetMember = MemberInfo.parse(target);
        if (!targetMember.isFullyQualified()) {
            String missing = "missing " + (targetMember.owner == null ? (targetMember.desc == null ? "owner and signature" : "owner") : "signature");
            this.mixins.printMessage(Kind.ERROR, "@At(" + key + ") is not fully qualified, " + missing, element, inject);
            return false;
        }
        
        try {
            targetMember.validate();
        } catch (InvalidMemberDescriptorException ex) {
            this.mixins.printMessage(Kind.ERROR, ex.getMessage(), element, inject);
        }
        
        if (targetMember.isField()) {
            ObfuscationData<String> obfFieldData = this.getObfFieldRecursive(targetMember);
            if (obfFieldData.isEmpty()) {
                this.mixins.printMessage(Kind.WARNING, "Cannot find field mapping for @At(" + key + ") '" + target + "'", element, inject);
                return false;
            }
            this.mixins.addFieldMapping(this.classRef, target, targetMember, obfFieldData);
        } else {
            ObfuscationData<MethodData> obfMethodData = this.mixins.getObfMethod(targetMember);
            if (obfMethodData.isEmpty()) {
                if (targetMember.owner == null || !targetMember.owner.startsWith("java/lang/")) {
                    this.mixins.printMessage(Kind.WARNING, "Cannot find method mapping for @At(" + key + ") '" + target + "'", element, inject);
                }
            }
            this.mixins.addMethodMapping(this.classRef, target, targetMember, obfMethodData);
        }
        return true;
    }

    private ObfuscationData<String> getObfFieldRecursive(MemberInfo targetMember) {
        ObfuscationData<String> targetNames = this.mixins.getObfClass(targetMember.owner);
        ObfuscationData<String> obfFieldData = this.mixins.getObfField(targetMember.toSrg());
        try {
            while (obfFieldData.isEmpty()) {
                TypeHandle targetType = this.mixins.getTypeHandle(targetMember.owner);
                if (targetType == null) {
                    return obfFieldData;
                }
                TypeHandle superClass = targetType.getSuperclass();
                if (superClass == null) {
                    return obfFieldData;
                }
                targetMember = targetMember.move(superClass.getName());
                obfFieldData = this.mixins.getObfField(targetMember.toSrg());
                if (!obfFieldData.isEmpty()) {
                    for (ObfuscationType type : obfFieldData) {
                        obfFieldData.add(type, MemberInfo.fromSrgField(obfFieldData.get(type), "").move(targetNames.get(type)).toSrg());
                    }
                }
            }
        } catch (Exception ex) {
            // ???
        }
        return obfFieldData;
    }

    /**
     * Add a field mapping to the local table
     */
    private void addFieldMapping(ObfuscationType type, ShadowElementName name) {
        String mapping = String.format("FD: %s %s", this.classRef + "/" + name.name(), this.classRef + "/" + name.obfuscated());
        this.fieldMappings.get(type).add(mapping);
    }
    
    /**
     * Add a method mapping to the local table
     */
    private void addMethodMapping(ObfuscationType type, ShadowElementName name, String mcpSignature, String obfSignature) {
        this.addMethodMapping(type, name.name(), name.obfuscated(), mcpSignature, obfSignature);
    }

    /**
     * Add a method mapping to the local table
     */
    private void addMethodMapping(ObfuscationType type, String mcpName, String obfName, String mcpSignature, String obfSignature) {
        String mapping = String.format("MD: %s %s %s %s", this.classRef + "/" + mcpName, mcpSignature, this.classRef + "/" + obfName, obfSignature);
        this.methodMappings.get(type).add(mapping);
    }
    
    /**
     * Check constraints for the specified annotation based on token values in
     * the current environment
     * 
     * @param method Annotated method
     * @param annotation Annotation to check constraints
     */
    private void checkConstraints(ExecutableElement method, AnnotationMirror annotation) {
        try {
            Constraint constraint = ConstraintParser.parse(MirrorUtils.<String>getAnnotationValue(annotation, "constraints"));
            try {
                constraint.check(this.mixins);
            } catch (ConstraintViolationException ex) {
                this.mixins.printMessage(Kind.ERROR, ex.getMessage(), method, annotation);
            }
        } catch (InvalidConstraintException ex) {
            this.mixins.printMessage(Kind.WARNING, ex.getMessage(), method, annotation);
        }
    }

    /**
     * Checks whether the specified method exists in all targets and raises
     * warnings where appropriate
     */
    private void validateTargetMethod(ExecutableElement method, AnnotationMirror annotation, AliasedElementName name, String type) {
        String signature = TypeHandle.getElementSignature(method);

        for (TypeHandle target : this.targets) {
            if (target.isImaginary()) {
                continue;
            }
            
            // Find method as-is
            MethodHandle targetMethod = target.findMethod(method);
            if (targetMethod != null) {
                continue;
            }
            
            if (!name.baseName().equals(name.elementName())) {
                // Find method without prefix
                targetMethod = target.findMethod(name.baseName(), signature);
                if (targetMethod != null) {
                    continue;
                }
            }
            
            // Check aliases
            for (String alias : name.getAliases()) {
                if ((targetMethod = target.findMethod(alias, signature)) != null) {
                    break;
                }
            }
            
            if (targetMethod == null) {
                this.mixins.printMessage(Kind.WARNING, "Cannot find target for " + type + " method in " + target, method, annotation);
            }
        }
    }

    /**
     * Checks whether the specified field exists in all targets and raises
     * warnings where appropriate
     */
    private void validateTargetField(VariableElement field, AnnotationMirror shadow, AliasedElementName name, String type) {
        String fieldType = field.asType().toString();

        for (TypeHandle target : this.targets) {
            if (target.isImaginary()) {
                continue;
            }
            
            // Search for field
            FieldHandle targetField = target.findField(field);
            if (targetField != null) {
                continue;
            }
            
            // Try search by alias
            List<String> aliases = name.getAliases();
            for (String alias : aliases) {
                if ((targetField = target.findField(alias, fieldType)) != null) {
                    break;
                }
            }
            
            if (targetField == null) {
                this.mixins.printMessage(Kind.WARNING, "Cannot find target for " + type + " field in " + target, field, shadow);
            }
        }
    }

    /**
     * Checks whether the referenced method exists in all targets and raises
     * warnings where appropriate
     */
    private void validateReferencedTarget(ExecutableElement method, AnnotationMirror inject, MemberInfo reference, String type) {
        String signature = reference.toDescriptor();
        
        for (TypeHandle target : this.targets) {
            if (target.isImaginary()) {
                continue;
            }
            
            MethodHandle targetMethod = target.findMethod(reference.name, signature);
            if (targetMethod == null) {
                this.mixins.printMessage(Kind.WARNING, "Cannot find target method for " + type + " in " + target, method, inject);
            }
        }            
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
