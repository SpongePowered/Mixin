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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

import net.minecraftforge.srg2source.rangeapplier.MethodData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.tools.MirrorUtils;


/**
 * Information about a mixin stored during processing
 */
class AnnotatedMixin {
    
    /**
     * Convenience class to store information about an {@link org.spongepowered.asm.mixin.Shadow}ed member's names
     */
    class ShadowElementName {
        private final boolean hasPrefix;
        
        private final String prefix;
        
        private final String baseName;
        
        private final String originalName;
        
        private String obfuscated;
        
        ShadowElementName(Element method, AnnotationMirror shadow) {
            boolean hasPrefix = false;
            this.prefix = MirrorUtils.<String>getAnnotationValue(shadow, "prefix", "shadow$");
            String name = this.originalName = method.getSimpleName().toString();
            if (name.startsWith(this.prefix)) {
                hasPrefix = true;
                name = name.substring(this.prefix.length());
            }
            this.hasPrefix = hasPrefix;
            this.obfuscated = this.baseName = name;
        }
        
        @Override
        public String toString() {
            return this.baseName;
        }

        public ShadowElementName setObfuscatedName(String name) {
            this.obfuscated = name.substring(name.lastIndexOf('/') + 1);
            return this;
        }
        
        public String originalName() {
            return this.originalName;
        }

        public String prefix() {
            return this.hasPrefix ? this.prefix : "";
        }
        
        public String name() {
            return this.prefix(this.baseName);
        }
        
        public String obfuscated() {
            return this.prefix(this.obfuscated);
        }
        
        public String prefix(String name) {
            return this.hasPrefix ? this.prefix + name : name;
        }
    }
    
    private static final String CTOR = "<init>";
    
    /**
     * Manager
     */
    private final AnnotatedMixins mixins;
    
    /**
     * Mixin class
     */
    private final TypeElement mixin;
    
    /**
     * Specified targets
     */
    private final List<String> targets = new ArrayList<String>();
    
    /**
     * Target "reference" (bytecode name)
     */
    private final String targetRef;
    
    /**
     * Target type (for single-target mixins) 
     */
    private final TypeElement targetType;
    
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
    private final Set<String> fieldMappings = new LinkedHashSet<String>();
    
    /**
     * Stored (ordered) method mappings
     */
    private final Set<String> methodMappings = new LinkedHashSet<String>();
    
    public AnnotatedMixin(AnnotatedMixins mixins, TypeElement type) {
        AnnotationMirror annotation = MirrorUtils.getAnnotation(type, Mixin.class);
        
        this.mixins = mixins;
        this.mixin = type;
        this.classRef = type.getQualifiedName().toString().replace('.', '/');
        this.remap = AnnotatedMixins.getRemapValue(annotation);
        
        String targetRef = null;
        TypeElement targetType = null;
        try {
            List<AnnotationValue> targetList = MirrorUtils.<List<AnnotationValue>>getAnnotationValue(annotation);
            for (TypeMirror target : MirrorUtils.<TypeMirror>unfold(targetList)) {
                Element element = ((DeclaredType)target).asElement();
                String targetName = ((TypeElement)element).getQualifiedName().toString();
                this.targets.add(targetName);
                if (targetRef == null) {
                    targetRef = MirrorUtils.getInternalName((DeclaredType)target);
                    targetType = (TypeElement) ((DeclaredType)target).asElement(); 
                }
            }
        } catch (Exception ex) {
            this.mixins.printMessage(Kind.WARNING, "Error processing target list: " + ex.getClass().getSimpleName() + ": "+ ex.getMessage(), type);
        }
        
        this.targetRef = targetRef;
        this.targetType = targetType;
    }
    
    /**
     * Get the mixin class
     */
    public TypeElement getMixin() {
        return this.mixin;
    }
    
    /**
     * Get the mixin's targets
     */
    public List<String> getTargets() {
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
    public Set<String> getFieldMappings() {
        return this.fieldMappings;
    }
    
    /**
     * Get stored method mappings
     */
    public Set<String> getMethodMappings() {
        return this.methodMappings;
    }
    
    /**
     * Clear all stored mappings
     */
    public void clear() {
        this.fieldMappings.clear();
        this.methodMappings.clear();
    }

    /**
     * Validate method for {@link org.spongepowered.asm.mixin.Shadow} and {@link org.spongepowered.asm.mixin.Overwrite} registrations to check that
     * only a single target is registered. Mixins containing annotated methods with these annotations cannot be multi-targetted.
     */
    private boolean validateSingleTarget(String annotation, Element element) {
        if (this.targetRef == null) {
            this.mixins.printMessage(Kind.ERROR, "Mixin has multiple targets. " + annotation + " is not supported!", element);
            return false;
        }
        return true;
    }

    /**
     * Register an {@link org.spongepowered.asm.mixin.Overwrite} method
     */
    public void registerOverwrite(ExecutableElement method) {
        if (!this.validateSingleTarget("@Overwrite", method)) {
            return;
        }
        
        String mcpName = method.getSimpleName().toString();
        String mcpSignature = MirrorUtils.generateSignature(method);
        MethodData obfMethod = this.mixins.getObfMethod(new MethodData(this.targetRef + "/" + mcpName, mcpSignature));
        
        if (obfMethod == null) {
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
        
        this.addMethodMapping(mcpName, obfMethod.name, mcpSignature, obfMethod.sig);
    }

    /**
     * Register a {@link org.spongepowered.asm.mixin.Shadow} field
     */
    public void registerShadow(VariableElement field, AnnotationMirror shadow) {
        if (!this.validateSingleTarget("@Shadow", field)) {
            return;
        }
        
        ShadowElementName name = new ShadowElementName(field, shadow);
        String obfField = this.mixins.getObfField(this.targetRef + "/" + name);
        
        if (obfField == null) {
            this.mixins.printMessage(Kind.WARNING, "Unable to locate obfuscation mapping for @Shadow field", field);
            return;
        }

        this.addFieldMapping(name.setObfuscatedName(obfField));
    }

    /**
     * Register a {@link org.spongepowered.asm.mixin.Shadow} method
     */
    public void registerShadow(ExecutableElement method, AnnotationMirror shadow) {
        if (!this.validateSingleTarget("@Shadow", method)) {
            return;
        }
        
        ShadowElementName name = new ShadowElementName(method, shadow);
        String mcpSignature = MirrorUtils.generateSignature(method);
        MethodData obfMethod = this.mixins.getObfMethod(new MethodData(this.targetRef + "/" + name, mcpSignature));
        
        if (obfMethod == null) {
            this.mixins.printMessage(Kind.WARNING, "Unable to locate obfuscation mapping for @Shadow method", method);
            return;
        }
        
        this.addMethodMapping(name.setObfuscatedName(obfMethod.name), mcpSignature, obfMethod.sig);
    }

    /**
     * Register a {@link org.spongepowered.asm.mixin.injection.Inject} method
     */
    public void registerInjector(ExecutableElement method, AnnotationMirror inject) {
        if (!this.validateSingleTarget("@Inject", method)) {
            return;
        }
        
        String originalReference = MirrorUtils.<String>getAnnotationValue(inject, "method");
        MemberInfo targetMember = MemberInfo.parse(originalReference);
        if (targetMember.name == null) {
            return;
        }
        
        String desc = this.findDescriptor(this.targetType, targetMember);
        if (desc == null) {
            this.mixins.printMessage(Kind.WARNING, "Unable to determine signature for @Inject target method", method);
            return;
       }
        
        MethodData obfMethod = this.mixins.getObfMethod(new MethodData(this.targetRef + "/" + targetMember.name, desc));
        if (obfMethod == null) {
            Kind error = AnnotatedMixin.CTOR.equals(targetMember.name) ? Kind.WARNING : Kind.ERROR;
            this.mixins.printMessage(error, "No obfuscation mapping for @Inject target " + targetMember.name, method);
            return;
        }
        
        String obfName = obfMethod.name.substring(obfMethod.name.lastIndexOf('/') + 1);
        MemberInfo remappedReference = new MemberInfo(obfName, this.targetRef, obfMethod.sig, false);
        
        this.mixins.getReferenceMapper().addMapping(this.classRef, originalReference, remappedReference.toString());
    }

    /**
     * Register a {@link org.spongepowered.asm.mixin.injection.At} annotation and process the references
     */
    public void registerInjectionPoint(AnnotationMirror at, Element element) {
        String type = MirrorUtils.<String>getAnnotationValue(at, "value");
        String target = MirrorUtils.<String>getAnnotationValue(at, "target");
        this.remapReference(type + ".<target>", target, element);
        
        // Pattern for replacing references in args, not used yet
//        if ("SOMETYPE".equals(type)) {
//            Map<String, String> args = AnnotatedMixin.getAtArgs(at);
//            this.remapReference(type + ".args[target]", args.get("target"));
//        }
    }

    private void remapReference(String key, String target, Element element) {
        if (target == null) {
            return;
        }
        
        MemberInfo targetMember = MemberInfo.parse(target);
        if (!targetMember.isFullyQualified()) {
            String missing = "missing " + (targetMember.owner == null ? (targetMember.desc == null ? "owner and signature" : "owner") : "signature");
            this.mixins.printMessage(Kind.ERROR, "@At(" + key + ") is not fully qualified, " + missing, element);
            return;
        }
        
        MemberInfo remappedReference = null;
        if (targetMember.isField()) {
            String obfField = this.mixins.getObfField(targetMember.toSrg());
            if (obfField == null) {
                this.mixins.printMessage(Kind.WARNING, "Cannot find field mapping for @At(" + key + ") '" + target, element);
                return;
            }
            remappedReference = MemberInfo.fromSrgField(obfField, targetMember.desc);
        } else {
            MethodData obfMethod = this.mixins.getObfMethod(targetMember.asMethodData());
            if (obfMethod == null) {
                this.mixins.printMessage(Kind.WARNING, "Cannot find method mapping for @At(" + key + ") '" + target, element);
                return;
            }
            remappedReference = MemberInfo.fromSrgMethod(obfMethod);
        }
        
        this.mixins.getReferenceMapper().addMapping(this.classRef, target, remappedReference.toString());
    }

    /**
     * Add a field mapping to the local table
     */
    private void addFieldMapping(ShadowElementName name) {
        String mapping = String.format("FD: %s %s", this.classRef + "/" + name.name(), this.classRef + "/" + name.obfuscated());
        this.fieldMappings.add(mapping);
    }
    
    /**
     * Add a method mapping to the local table
     */
    private void addMethodMapping(ShadowElementName name, String mcpSignature, String obfSignature) {
        this.addMethodMapping(name.name(), name.obfuscated(), mcpSignature, obfSignature);
    }

    /**
     * Add a method mapping to the local table
     */
    private void addMethodMapping(String mcpName, String obfName, String mcpSignature, String obfSignature) {
        String mapping = String.format("MD: %s %s %s %s", this.classRef + "/" + mcpName, mcpSignature, this.classRef + "/" + obfName, obfSignature);
        this.methodMappings.add(mapping);
    }

    private String findDescriptor(TypeElement targetType, MemberInfo memberInfo) {
        String desc = memberInfo.desc;
        if (desc == null) {
            for (Element child : targetType.getEnclosedElements()) {
                if (child.getKind() != ElementKind.METHOD) {
                    continue;
                }
                
                if (child.getSimpleName().toString().equals(memberInfo.name)) {
                    desc = MirrorUtils.generateSignature((ExecutableElement)child);
                    break;
                }
            }
        }
        return desc;
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
