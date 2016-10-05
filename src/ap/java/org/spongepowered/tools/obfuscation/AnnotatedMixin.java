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
import java.util.Collection;
import java.util.List;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.tools.obfuscation.AnnotatedMixinInjectorHandler.AnnotatedElementInjectionPoint;
import org.spongepowered.tools.obfuscation.AnnotatedMixinInjectorHandler.AnnotatedElementInjector;
import org.spongepowered.tools.obfuscation.AnnotatedMixinOverwriteHandler.AnnotatedElementOverwrite;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IMixinValidator;
import org.spongepowered.tools.obfuscation.interfaces.IMixinValidator.ValidationPass;
import org.spongepowered.tools.obfuscation.interfaces.IObfuscationManager;
import org.spongepowered.tools.obfuscation.interfaces.ITypeHandleProvider;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer;
import org.spongepowered.tools.obfuscation.model.AnnotationHandle;
import org.spongepowered.tools.obfuscation.model.TypeHandle;
import org.spongepowered.tools.obfuscation.struct.Message;

/**
 * Information about a mixin stored during processing
 */
class AnnotatedMixin {
    
    /**
     * Mixin annotation
     */
    private final AnnotationHandle annotation;
    
    /**
     * Messager 
     */
    private final Messager messager;
    
    /**
     * Type handle provider
     */
    private final ITypeHandleProvider typeProvider;
    
    /**
     * Manager
     */
    private final IObfuscationManager obf;
    
    /**
     * Generated mappings 
     */
    private final IMappingConsumer mappings;

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
     * Target type (for single-target mixins) 
     */
    private final TypeHandle primaryTarget;
    
    /**
     * Mixin class "reference" (bytecode name)
     */
    private final String classRef;
    
    /**
     * True if we will actually process remappings for this mixin
     */
    private final boolean remap;
 
    /**
     * Overwrite handler
     */
    private final AnnotatedMixinOverwriteHandler overwrites;
    
    /**
     * Shadow handler
     */
    private final AnnotatedMixinShadowHandler shadows;
    
    /**
     * Injector handler
     */
    private final AnnotatedMixinInjectorHandler injectors;

    public AnnotatedMixin(IMixinAnnotationProcessor ap, TypeElement type) {
        this.typeProvider = ap.getTypeProvider();
        this.obf = ap.getObfuscationManager();
        this.mappings = this.obf.createMappingConsumer();
        this.messager = ap;
        this.mixin = type;
        this.handle = new TypeHandle(type);
        this.annotation = this.handle.getAnnotation(Mixin.class);
        this.classRef = type.getQualifiedName().toString().replace('.', '/');
        this.primaryTarget = this.initTargets();
        this.remap = AnnotatedMixins.getRemapValue(this.annotation) && this.targets.size() > 0;
        
        this.overwrites = new AnnotatedMixinOverwriteHandler(ap, this);
        this.shadows = new AnnotatedMixinShadowHandler(ap, this);
        this.injectors = new AnnotatedMixinInjectorHandler(ap, this);
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
            for (TypeMirror target : this.annotation.<TypeMirror>getList()) {
                TypeHandle type = new TypeHandle((DeclaredType)target);
                if (this.targets.contains(type)) {
                    continue;
                }
                this.addTarget(type);
                if (primaryTarget == null) {
                    primaryTarget = type;
                }
            }
        } catch (Exception ex) {
            this.printMessage(Kind.WARNING, "Error processing public targets: " + ex.getClass().getName() + ": " + ex.getMessage(), this);
        }
        
        // Private targets, referenced by name
        try {
            for (String privateTarget : this.annotation.<String>getList("targets")) {
                TypeHandle type = this.typeProvider.getTypeHandle(privateTarget);
                if (this.targets.contains(type)) {
                    continue;
                }
                if (type == null) {
                    this.printMessage(Kind.ERROR, "Mixin target " + privateTarget + " could not be found", this);
                    return null;
                } else if (type.isPublic()) {
                    this.printMessage(Kind.WARNING, "Mixin target " + privateTarget + " is public and must be specified in value", this);
                    return null;
                }
                this.addSoftTarget(type, privateTarget);
                if (primaryTarget == null) {
                    primaryTarget = type;
                }
            }
        } catch (Exception ex) {
            this.printMessage(Kind.WARNING, "Error processing private targets: " + ex.getClass().getName() + ": " + ex.getMessage(), this);
        }
        
        if (primaryTarget == null) {
            this.printMessage(Kind.ERROR, "Mixin has no targets", this);
        }
        
        return primaryTarget;
    }

    /**
     * Print a message to the AP messager
     */
    private void printMessage(Kind kind, CharSequence msg, AnnotatedMixin mixin) {
        this.messager.printMessage(kind, msg, this.mixin, this.annotation.getMirror());
    }

    private void addSoftTarget(TypeHandle type, String reference) {
        ObfuscationData<String> obfClassData = this.obf.getDataProvider().getObfClass(type);
        if (!obfClassData.isEmpty()) {
            this.obf.getReferenceManager().addClassMapping(this.classRef, reference, obfClassData);
        }
        
        this.addTarget(type);
    }
    
    private void addTarget(TypeHandle type) {
        this.targets.add(type);
    }

    @Override
    public String toString() {
        return this.mixin.getSimpleName().toString();
    }
    
    public AnnotationHandle getAnnotation() {
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
     * Get the mixin class reference
     */
    public String getClassRef() {
        return this.classRef;
    }
    
    /**
     * Get whether this is an interface mixin
     */
    public boolean isInterface() {
        return this.mixin.getKind() == ElementKind.INTERFACE;
    }
    
    /**
     * Get the <em>primary</em> target
     */
    @Deprecated
    public TypeHandle getPrimaryTarget() {
        return this.primaryTarget;
    }
    
    /**
     * Get the mixin's targets
     */
    public List<TypeHandle> getTargets() {
        return this.targets;
    }
    
    /**
     * Get whether this is a multi-target mixin
     */
    public boolean isMultiTarget() {
        return this.targets.size() > 1;
    }
    
    /**
     * Get whether to remap annotations in this mixin
     */
    public boolean remap() {
        return this.remap;
    }
    
    public IMappingConsumer getMappings() {
        return this.mappings;
    }
    
    public void registerOverwrite(ExecutableElement method, AnnotationHandle overwrite) {
        this.overwrites.registerOverwrite(new AnnotatedElementOverwrite(method, overwrite));
    }

    public void registerShadow(VariableElement field, AnnotationHandle shadow, boolean shouldRemap) {
        this.shadows.registerShadow(this.shadows.new AnnotatedElementShadowField(field, shadow, shouldRemap));
    }

    public void registerShadow(ExecutableElement method, AnnotationHandle shadow, boolean shouldRemap) {
        this.shadows.registerShadow(this.shadows.new AnnotatedElementShadowMethod(method, shadow, shouldRemap));
    }

    public Message registerInjector(ExecutableElement method, AnnotationHandle inject, boolean shouldRemap) {
        return this.injectors.registerInjector(new AnnotatedElementInjector(method, inject, shouldRemap));
    }

    public int registerInjectionPoint(ExecutableElement element, AnnotationHandle inject, AnnotationHandle at) {
        return this.injectors.registerInjectionPoint(new AnnotatedElementInjectionPoint(element, inject, at));
    }
    
}
