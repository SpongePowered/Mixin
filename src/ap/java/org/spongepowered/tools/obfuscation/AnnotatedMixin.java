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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.struct.SelectorAnnotationContext;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.refmap.ReferenceMapper;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.util.asm.IAnnotatedElement;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.tools.obfuscation.AnnotatedMixinElementHandlerAccessor.AnnotatedElementAccessor;
import org.spongepowered.tools.obfuscation.AnnotatedMixinElementHandlerAccessor.AnnotatedElementInvoker;
import org.spongepowered.tools.obfuscation.AnnotatedMixinElementHandlerInjector.AnnotatedElementInjectionPoint;
import org.spongepowered.tools.obfuscation.AnnotatedMixinElementHandlerInjector.AnnotatedElementInjector;
import org.spongepowered.tools.obfuscation.AnnotatedMixinElementHandlerInjector.AnnotatedElementSliceInjectionPoint;
import org.spongepowered.tools.obfuscation.AnnotatedMixinElementHandlerOverwrite.AnnotatedElementOverwrite;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerEx.MessageType;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerSuppressible;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IMixinValidator;
import org.spongepowered.tools.obfuscation.interfaces.IMixinValidator.ValidationPass;
import org.spongepowered.tools.obfuscation.interfaces.IObfuscationManager;
import org.spongepowered.tools.obfuscation.interfaces.ITypeHandleProvider;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.MethodHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeUtils;
import org.spongepowered.tools.obfuscation.struct.InjectorRemap;

import com.google.common.base.Strings;

/**
 * Information about a mixin stored during processing
 */
class AnnotatedMixin implements IMixinContext, IAnnotatedElement {

    /**
     * Mixin annotation
     */
    private final IAnnotationHandle annotation;

    /**
     * Messager
     */
    private final IMessagerSuppressible messager;

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
     * Methods 
     */
    private final List<MethodHandle> methods;

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
     * True if the target class is allowed to not exist at compile time, we will
     * simulate the target in order to do as much validation as is feasible.
     *
     * <p>Implies <tt>remap=false</tt></p>
     */
    private final boolean virtual;

    /**
     * Overwrite handler
     */
    private final AnnotatedMixinElementHandlerOverwrite overwrites;

    /**
     * Shadow handler
     */
    private final AnnotatedMixinElementHandlerShadow shadows;

    /**
     * Injector handler
     */
    private final AnnotatedMixinElementHandlerInjector injectors;

    /**
     * Accessor handler
     */
    private final AnnotatedMixinElementHandlerAccessor accessors;

    /**
     * Soft implementation handler;
     */
    private final AnnotatedMixinElementHandlerSoftImplements softImplements;
    
    /**
     * True once the FINAL 
     */
    private boolean validated = false;

    public AnnotatedMixin(IMixinAnnotationProcessor ap, TypeElement type) {
        this.typeProvider = ap.getTypeProvider();
        this.obf = ap.getObfuscationManager();
        this.mappings = this.obf.createMappingConsumer();
        this.messager = ap;
        this.mixin = type;
        this.handle = new TypeHandle(type);
        this.methods = new ArrayList<MethodHandle>(this.handle.getMethods());
        this.virtual = this.handle.getAnnotation(Pseudo.class).exists();
        this.annotation = this.handle.getAnnotation(Mixin.class);
        this.classRef = TypeUtils.getInternalName(type);
        this.primaryTarget = this.initTargets(ap);
        this.remap = this.annotation.getBoolean("remap", true) && this.targets.size() > 0;

        this.overwrites = new AnnotatedMixinElementHandlerOverwrite(ap, this);
        this.shadows = new AnnotatedMixinElementHandlerShadow(ap, this);
        this.injectors = new AnnotatedMixinElementHandlerInjector(ap, this);
        this.accessors = new AnnotatedMixinElementHandlerAccessor(ap, this);
        this.softImplements = new AnnotatedMixinElementHandlerSoftImplements(ap, this);
    }

    AnnotatedMixin runValidators(ValidationPass pass, Collection<IMixinValidator> validators) {
        for (IMixinValidator validator : validators) {
            if (!validator.validate(pass, this.mixin, this.annotation, this.targets)) {
                break;
            }
        }
        
        if (pass == ValidationPass.FINAL && !this.validated) {
            this.validated = true;
            this.runFinalValidation();
        }

        return this;
    }

    private TypeHandle initTargets(IMixinAnnotationProcessor ap) {
        TypeHandle primaryTarget = null;

        // Public targets, referenced by class
        try {
            for (Object target : this.annotation.<Object>getList()) {
                TypeHandle type = this.typeProvider.getTypeHandle(target);
                if (type == null || this.targets.contains(type)) {
                    continue;
                }
                this.addTarget(type);
                if (primaryTarget == null) {
                    primaryTarget = type;
                }
            }
        } catch (Exception ex) {
            this.printMessage(MessageType.WARNING, "Error processing public targets: " + ex.getClass().getName() + ": " + ex.getMessage());
        }

        // Private targets, referenced by name
        try {
            for (String softTarget : this.annotation.<String>getList("targets")) {
                TypeHandle type = this.typeProvider.getTypeHandle(softTarget);
                if (this.targets.contains(type)) {
                    continue;
                }
                if (this.virtual) {
                    type = this.typeProvider.getSimulatedHandle(softTarget, this.mixin.asType());
                } else if (type == null) {
                    this.printMessage(MessageType.MIXIN_SOFT_TARGET_NOT_FOUND, "Mixin target " + softTarget + " could not be found");
                    if (MessageType.MIXIN_SOFT_TARGET_NOT_FOUND.isError()) {
                        return null;
                    }
                    type = this.typeProvider.getSimulatedHandle(softTarget, this.mixin.asType());
                } else if (type.isImaginary()) {
                    this.printMessage(MessageType.MIXIN_SOFT_TARGET_NOT_RESOLVED, "Mixin target " + softTarget + " could not be fully resolved.",
                            SuppressedBy.UNRESOLVABLE_TARGET);
                    if (MessageType.MIXIN_SOFT_TARGET_NOT_RESOLVED.isError()) {
                        return null;
                    }
                } else if (type.isPublic()) {
                    SuppressedBy suppressedBy = (type.getPackage().isUnnamed()) ? SuppressedBy.DEFAULT_PACKAGE : SuppressedBy.PUBLIC_TARGET;
                    String must = MessageType.MIXIN_SOFT_TARGET_IS_PUBLIC.isError() ? "must" : "should";
                    this.printMessage(MessageType.MIXIN_SOFT_TARGET_IS_PUBLIC, "Mixin target " + softTarget
                            + " is public and " + must + " be specified in value", suppressedBy);
                    if (MessageType.MIXIN_SOFT_TARGET_IS_PUBLIC.isError()) {
                        return null;
                    }
                }
                this.addSoftTarget(type, softTarget);
                if (primaryTarget == null) {
                    primaryTarget = type;
                }
            }
        } catch (Exception ex) {
            this.printMessage(MessageType.WARNING, "Error processing private targets: " + ex.getClass().getName() + ": " + ex.getMessage());
        }

        if (primaryTarget == null) {
            this.printMessage(MessageType.MIXIN_NO_TARGETS, "Mixin has no targets");
        }

        return primaryTarget;
    }

    /**
     * Print a message to the AP messager
     */
    private void printMessage(MessageType type, CharSequence msg) {
        this.messager.printMessage(type, msg, this.mixin, AnnotationHandle.asMirror(this.annotation));
    }
    
    /**
     * Print a suppressible message to the AP messager
     */
    private void printMessage(MessageType type, CharSequence msg, SuppressedBy suppressedBy) {
        this.messager.printMessage(type, msg, this.mixin, AnnotationHandle.asMirror(this.annotation), suppressedBy);
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

    public IAnnotationHandle getAnnotation() {
        return this.annotation;
    }

    /**
     * Get the mixin class
     */
    public TypeElement getMixinElement() {
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
    @Override
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

    private void runFinalValidation() {
        for (MethodHandle method : this.methods) {
            this.overwrites.registerMerge(method);
        }
    }

    private void removeMethod(ExecutableElement method) {
        MethodHandle handle = null;
        for (MethodHandle methodHandle : this.methods) {
            if (methodHandle.getElement() == method) {
                handle = methodHandle;
            }
        }
        if (handle != null) {
            this.methods.remove(handle);
        }
    }

    public void registerOverwrite(ExecutableElement method, AnnotationHandle overwrite, boolean shouldRemap) {
        this.removeMethod(method);
        this.overwrites.registerOverwrite(new AnnotatedElementOverwrite(method, overwrite, shouldRemap));
    }

    public void registerShadow(VariableElement field, AnnotationHandle shadow, boolean shouldRemap) {
        this.shadows.registerShadow(this.shadows.new AnnotatedElementShadowField(field, shadow, shouldRemap));
    }

    public void registerShadow(ExecutableElement method, AnnotationHandle shadow, boolean shouldRemap) {
        this.removeMethod(method);
        this.shadows.registerShadow(this.shadows.new AnnotatedElementShadowMethod(method, shadow, shouldRemap));
    }

    public void registerInjector(ExecutableElement method, AnnotationHandle inject, InjectorRemap remap) {
        this.removeMethod(method);
        AnnotatedElementInjector injectorElement = new AnnotatedElementInjector(method, inject, this, remap);
        this.injectors.registerInjector(injectorElement);

        List<IAnnotationHandle> ats = inject.getAnnotationList("at");
        for (IAnnotationHandle at : ats) {
            this.registerInjectionPoint(method, inject, "at", (AnnotationHandle)at, remap, "@At(%s)");
        }

        List<IAnnotationHandle> slices = inject.getAnnotationList("slice");
        for (IAnnotationHandle slice : slices) {
            String id = slice.<String>getValue("id", "");
            String coord = "slice";
            if (!Strings.isNullOrEmpty(id)) {
                coord += "." + id;
            }
            SelectorAnnotationContext sliceContext = new SelectorAnnotationContext(injectorElement, slice, coord);

            IAnnotationHandle from = slice.getAnnotation("from");
            if (from != null) {
                this.registerSliceInjectionPoint(method, inject, "from", (AnnotationHandle)from, remap, "@Slice[" + id + "](from=@At(%s))",
                        sliceContext);
            }

            IAnnotationHandle to = slice.getAnnotation("to");
            if (to != null) {
                this.registerSliceInjectionPoint(method, inject, "to", (AnnotationHandle)to, remap, "@Slice[" + id + "](to=@At(%s))", sliceContext);
            }
        }
    }

    public void registerInjectionPoint(ExecutableElement element, AnnotationHandle inject, String selectorCoordinate, AnnotationHandle at,
            InjectorRemap remap, String format) {
        this.injectors.registerInjectionPoint(new AnnotatedElementInjectionPoint(element, inject, this, selectorCoordinate, at, remap), format);
    }

    public void registerSliceInjectionPoint(ExecutableElement element, AnnotationHandle inject, String selectorCoordinate, AnnotationHandle at,
            InjectorRemap remap, String format, ISelectorContext parentContext) {
        this.injectors.registerInjectionPoint(new AnnotatedElementSliceInjectionPoint(element, inject, this, selectorCoordinate, at, remap,
                parentContext), format);
    }
    
    public void registerAccessor(ExecutableElement element, AnnotationHandle accessor, boolean shouldRemap) {
        this.removeMethod(element);
        this.accessors.registerAccessor(new AnnotatedElementAccessor(element, accessor, this, shouldRemap));
    }

    public void registerInvoker(ExecutableElement element, AnnotationHandle invoker, boolean shouldRemap) {
        this.removeMethod(element);
        this.accessors.registerAccessor(new AnnotatedElementInvoker(element, invoker, this, shouldRemap));
    }

    public void registerSoftImplements(AnnotationHandle implementsAnnotation) {
        this.softImplements.process(implementsAnnotation);
    }

    @Override
    public ReferenceMapper getReferenceMapper() {
        return null;
    }
    
    @Override
    public String getClassName() {
        return this.getClassRef().replace('/', '.');
    }

    @Override
    public String getTargetClassRef() {
        return this.primaryTarget.getName();
    }

    @Override
    public IMixinInfo getMixin() {
        throw new UnsupportedOperationException("MixinInfo not available at compile time");
    }
    
    @Override
    public Extensions getExtensions() {
        throw new UnsupportedOperationException("Mixin Extensions not available at compile time");
    }

    @Override
    public boolean getOption(Option option) {
        throw new UnsupportedOperationException("Options not available at compile time");
    }

    @Override
    public int getPriority() {
        throw new UnsupportedOperationException("Priority not available at compile time");
    }

    @Override
    public IAnnotationHandle getAnnotation(Class<? extends Annotation> annotationClass) {
        return AnnotationHandle.of(this.mixin, annotationClass);
    }

}
