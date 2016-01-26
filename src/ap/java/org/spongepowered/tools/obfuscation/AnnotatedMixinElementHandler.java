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

import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;

import org.spongepowered.asm.mixin.injection.struct.InvalidMemberDescriptorException;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.obfuscation.SrgMethod;
import org.spongepowered.asm.util.ConstraintParser;
import org.spongepowered.asm.util.ConstraintParser.Constraint;
import org.spongepowered.asm.util.ConstraintViolationException;
import org.spongepowered.asm.util.InvalidConstraintException;
import org.spongepowered.tools.MirrorUtils;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IObfuscationManager;

abstract class AnnotatedMixinElementHandler {
    
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
     * Mixin
     */
    protected final AnnotatedMixin mixin;

    protected final String classRef;

    /**
     * Annotation processor
     */
    protected final IMixinAnnotationProcessor ap;
    
    /**
     * Manager
     */
    protected final IObfuscationManager obf;

    AnnotatedMixinElementHandler(IMixinAnnotationProcessor ap, AnnotatedMixin mixin) {
        this.ap = ap;
        this.mixin = mixin;
        this.classRef = mixin.getClassRef();
        this.obf = ap.getObfuscationManager();
    }
    
    protected final boolean remapReference(String key, String target, Element element, AnnotationMirror inject, AnnotationMirror at) {
        if (target == null) {
            return false;
        }
        
        MemberInfo targetMember = MemberInfo.parse(target);
        if (!targetMember.isFullyQualified()) {
            String missing = "missing " + (targetMember.owner == null ? (targetMember.desc == null ? "owner and signature" : "owner") : "signature");
            this.ap.printMessage(Kind.ERROR, "@At(" + key + ") is not fully qualified, " + missing, element, inject);
            return false;
        }
        
        try {
            targetMember.validate();
        } catch (InvalidMemberDescriptorException ex) {
            this.ap.printMessage(Kind.ERROR, ex.getMessage(), element, inject);
        }
        
        if (targetMember.isField()) {
            ObfuscationData<String> obfFieldData = this.obf.getObfFieldRecursive(targetMember);
            if (obfFieldData.isEmpty()) {
                this.ap.printMessage(Kind.WARNING, "Cannot find field mapping for @At(" + key + ") '" + target + "'", element, inject);
                return false;
            }
            this.obf.addFieldMapping(this.classRef, target, targetMember, obfFieldData);
        } else {
            ObfuscationData<SrgMethod> obfMethodData = this.obf.getObfMethodRecursive(targetMember);
            if (obfMethodData.isEmpty()) {
                if (targetMember.owner == null || !targetMember.owner.startsWith("java/lang/")) {
                    this.ap.printMessage(Kind.WARNING, "Cannot find method mapping for @At(" + key + ") '" + target + "'", element, inject);
                }
            }
            this.obf.addMethodMapping(this.classRef, target, targetMember, obfMethodData);
        }
        return true;
    }
    
    /**
     * Add a field mapping to the local table
     */
    protected final void addFieldMapping(ObfuscationType type, ShadowElementName name) {
        String mapping = String.format("FD: %s %s", this.classRef + "/" + name.name(), this.classRef + "/" + name.obfuscated());
        this.mixin.addFieldMapping(type, mapping);
    }

    /**
     * Add a method mapping to the local table
     */
    protected final void addMethodMapping(ObfuscationType type, ShadowElementName name, String mcpSignature, String obfSignature) {
        this.addMethodMapping(type, name.name(), name.obfuscated(), mcpSignature, obfSignature);
    }

    /**
     * Add a method mapping to the local table
     */
    protected final void addMethodMapping(ObfuscationType type, String mcpName, String obfName, String mcpSignature, String obfSignature) {
        String mapping = String.format("MD: %s %s %s %s", this.classRef + "/" + mcpName, mcpSignature, this.classRef + "/" + obfName, obfSignature);
        this.mixin.addMethodMapping(type, mapping);
    }

    /**
     * Validate method for {@link org.spongepowered.asm.mixin.Shadow} and
     * {@link org.spongepowered.asm.mixin.Overwrite} registrations to check that
     * only a single target is registered. Mixins containing annotated methods
     * with these annotations cannot be multi-targetted.
     */
    protected final boolean validateSingleTarget(String annotation, Element element) {
        if (this.mixin.getPrimaryTargetRef() == null || this.mixin.getTargets().size() > 1) {
            this.ap.printMessage(Kind.ERROR, "Mixin with " + annotation + " members must have exactly one target.", element);
            this.ap.printMessage(Kind.ERROR, "Mixin with " + annotation + " members must have exactly one target.", this.mixin.getMixin());
            return false;
        }
        return true;
    }
    
    /**
     * Check constraints for the specified annotation based on token values in
     * the current environment
     * 
     * @param method Annotated method
     * @param annotation Annotation to check constraints
     */
    protected final void checkConstraints(ExecutableElement method, AnnotationMirror annotation) {
        try {
            Constraint constraint = ConstraintParser.parse(MirrorUtils.<String>getAnnotationValue(annotation, "constraints"));
            try {
                constraint.check(this.ap.getTokenProvider());
            } catch (ConstraintViolationException ex) {
                this.ap.printMessage(Kind.ERROR, ex.getMessage(), method, annotation);
            }
        } catch (InvalidConstraintException ex) {
            this.ap.printMessage(Kind.WARNING, ex.getMessage(), method, annotation);
        }
    }
    
    /**
     * Checks whether the specified method exists in all targets and raises
     * warnings where appropriate
     */
    protected final void validateTargetMethod(ExecutableElement method, AnnotationMirror annotation, AliasedElementName name, String type) {
        String signature = TypeHandle.getElementSignature(method);

        for (TypeHandle target : this.mixin.getTargets()) {
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
                this.ap.printMessage(Kind.WARNING, "Cannot find target for " + type + " method in " + target, method, annotation);
            }
        }
    }

    /**
     * Checks whether the specified field exists in all targets and raises
     * warnings where appropriate
     */
    protected final void validateTargetField(VariableElement field, AnnotationMirror shadow, AliasedElementName name, String type) {
        String fieldType = field.asType().toString();

        for (TypeHandle target : this.mixin.getTargets()) {
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
                this.ap.printMessage(Kind.WARNING, "Cannot find target for " + type + " field in " + target, field, shadow);
            }
        }
    }

    /**
     * Checks whether the referenced method exists in all targets and raises
     * warnings where appropriate
     */
    protected final void validateReferencedTarget(ExecutableElement method, AnnotationMirror inject, MemberInfo reference, String type) {
        String signature = reference.toDescriptor();
        
        for (TypeHandle target : this.mixin.getTargets()) {
            if (target.isImaginary()) {
                continue;
            }
            
            MethodHandle targetMethod = target.findMethod(reference.name, signature);
            if (targetMethod == null) {
                this.ap.printMessage(Kind.WARNING, "Cannot find target method for " + type + " in " + target, method, inject);
            }
        }            
    }

}
