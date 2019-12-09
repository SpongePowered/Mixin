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

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.gen.AccessorInfo;
import org.spongepowered.asm.mixin.gen.AccessorInfo.AccessorName;
import org.spongepowered.asm.mixin.gen.AccessorInfo.AccessorType;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorRemappable;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.refmap.ReferenceMapper;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.tools.obfuscation.ReferenceManager.ReferenceConflictException;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.FieldHandle;
import org.spongepowered.tools.obfuscation.mirror.MethodHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeUtils;
import org.spongepowered.tools.obfuscation.mirror.TypeUtils.Equivalency;
import org.spongepowered.tools.obfuscation.mirror.TypeUtils.EquivalencyResult;

import com.google.common.base.Strings;

/**
 * A module for {@link AnnotatedMixin} which handles accessors
 */
public class AnnotatedMixinElementHandlerAccessor extends AnnotatedMixinElementHandler implements IMixinContext {

    /**
     * Accessor element
     */
    static class AnnotatedElementAccessor extends AnnotatedElement<ExecutableElement> {

        protected final boolean shouldRemap;

        protected final TypeMirror returnType;

        protected String targetName;

        public AnnotatedElementAccessor(ExecutableElement element, AnnotationHandle annotation, boolean shouldRemap) {
            super(element, annotation);
            this.shouldRemap = shouldRemap;
            this.returnType = this.getElement().getReturnType();
        }

        public void attach(TypeHandle target) {
        }

        public boolean shouldRemap() {
            return this.shouldRemap;
        }

        public String getAnnotationValue() {
            return this.getAnnotation().<String>getValue();
        }

        public TypeMirror getTargetType() {
            switch (this.getAccessorType()) {
                case FIELD_GETTER:
                    return this.returnType;
                case FIELD_SETTER:
                    return this.getElement().getParameters().get(0).asType();
                default:
                    return null;
            }
        }

        public String getTargetTypeName() {
            return TypeUtils.getTypeName(this.getTargetType());
        }

        public String getAccessorDesc() {
            return TypeUtils.getInternalName(this.getTargetType());
        }

        public ITargetSelectorRemappable getContext() {
            return new MemberInfo(this.getTargetName(), null, this.getAccessorDesc());
        }

        public AccessorType getAccessorType() {
            return this.returnType.getKind() == TypeKind.VOID ? AccessorType.FIELD_SETTER : AccessorType.FIELD_GETTER;
        }

        public void setTargetName(String targetName) {
            this.targetName = targetName;
        }

        public String getTargetName() {
            return this.targetName;
        }
        
        public TypeMirror getReturnType() {
            return this.returnType;
        }
        
        public boolean isStatic() {
            return this.element.getModifiers().contains(Modifier.STATIC);
        }

        @Override
        public String toString() {
            return this.targetName != null ? this.targetName : "<invalid>";
        }
    
    }

    /**
     * Invoker element
     */
    static class AnnotatedElementInvoker extends AnnotatedElementAccessor {
        
        private AccessorType type = AccessorType.METHOD_PROXY;

        public AnnotatedElementInvoker(ExecutableElement element, AnnotationHandle annotation, boolean shouldRemap) {
            super(element, annotation, shouldRemap);
        }
        
        @Override
        public void attach(TypeHandle target) {
            this.type = AccessorType.METHOD_PROXY;
            if (this.returnType.getKind() != TypeKind.DECLARED) {
                return;
            }
            
            String specifiedName = this.getAnnotationValue();
            if (specifiedName != null) {
                if (Constants.CTOR.equals(specifiedName) || target.getName().equals(specifiedName.replace('.',  '/'))) {
                    this.type = AccessorType.OBJECT_FACTORY;
                }
                return;
            }

            AccessorName accessorName = AccessorName.of(this.getSimpleName(), false);
            if (accessorName == null) {
                return;
            }
            
            for (String prefix : AccessorType.OBJECT_FACTORY.getExpectedPrefixes()) {
                if (prefix.equals(accessorName.prefix)
                        && (Constants.CTOR.equals(accessorName.name) || target.getSimpleName().equals(accessorName.name))) {
                    this.type = AccessorType.OBJECT_FACTORY;
                    return;
                }
            }
        }
        
        @Override
        public boolean shouldRemap() {
            return (this.type == AccessorType.METHOD_PROXY || this.getAnnotationValue() != null) && super.shouldRemap();
        }

        @Override
        public String getAccessorDesc() {
            return TypeUtils.getDescriptor(this.getElement());
        }

        @Override
        public AccessorType getAccessorType() {
            return this.type;
        }

        @Override
        public String getTargetTypeName() {
            return TypeUtils.getJavaSignature(this.getElement());
        }

    }

    public AnnotatedMixinElementHandlerAccessor(IMixinAnnotationProcessor ap, AnnotatedMixin mixin) {
        super(ap, mixin);
    }

    @Override
    public ReferenceMapper getReferenceMapper() {
        return null;
    }
    
    @Override
    public String getClassName() {
        return this.mixin.getClassRef().replace('/', '.');
    }

    @Override
    public String getClassRef() {
        return this.mixin.getClassRef();
    }
    
    @Override
    public String getTargetClassRef() {
        throw new UnsupportedOperationException("Target class not available at compile time");
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
    public Target getTargetMethod(MethodNode into) {
        throw new UnsupportedOperationException("Target not available at compile time");
    }

    /**
     * Register a new accessor
     *
     * @param elem accessor element
     */
    public void registerAccessor(AnnotatedElementAccessor elem) {
        if (elem.getAccessorType() == null) {
            elem.printMessage(this.ap, Kind.WARNING, "Unsupported accessor type");
            return;
        }

        String targetName = this.getAccessorTargetName(elem);
        if (targetName == null) {
            elem.printMessage(this.ap, Kind.WARNING, "Cannot inflect accessor target name");
            return;
        }
        elem.setTargetName(targetName);

        for (TypeHandle target : this.mixin.getTargets()) {
            try {
                elem.attach(target);
            } catch (Exception ex) {
                elem.printMessage(this.ap, Kind.ERROR, ex.getMessage());
                continue;
            }
            if (elem.getAccessorType() == AccessorType.OBJECT_FACTORY) {
                this.registerFactoryForTarget((AnnotatedElementInvoker)elem, target);
            } else if (elem.getAccessorType() == AccessorType.METHOD_PROXY) {
                this.registerInvokerForTarget((AnnotatedElementInvoker)elem, target);
            } else {
                this.registerAccessorForTarget(elem, target);
            }
        }
    }

    private void registerAccessorForTarget(AnnotatedElementAccessor elem, TypeHandle target) {
        FieldHandle targetField = target.findField(elem.getTargetName(), elem.getTargetTypeName(), false);
        if (targetField == null) {
            if (!target.isImaginary()) {
                elem.printMessage(this.ap, Kind.ERROR, "Could not locate @Accessor target " + elem + " in target " + target);
                return;
            }
            
            targetField = new FieldHandle(target.getName(), elem.getTargetName(), elem.getDesc());
        }

        if (!elem.shouldRemap()) {
            return;
        }

        ObfuscationData<MappingField> obfData = this.obf.getDataProvider().getObfField(targetField.asMapping(false).move(target.getName()));
        if (obfData.isEmpty()) {
            String info = this.mixin.isMultiTarget() ? " in target " + target : "";
            elem.printMessage(this.ap, Kind.WARNING, "Unable to locate obfuscation mapping" + info + " for @Accessor target " + elem);
            return;
        }

        obfData = AnnotatedMixinElementHandler.<MappingField>stripOwnerData(obfData);

        try {
            this.obf.getReferenceManager().addFieldMapping(this.mixin.getClassRef(), elem.getTargetName(), elem.getContext(), obfData);
        } catch (ReferenceConflictException ex) {
            elem.printMessage(this.ap, Kind.ERROR, "Mapping conflict for @Accessor target " + elem + ": " + ex.getNew() + " for target "
                    + target + " conflicts with existing mapping " + ex.getOld());
        }
    }

    private void registerInvokerForTarget(AnnotatedElementInvoker elem, TypeHandle target) {
        MethodHandle targetMethod = target.findMethod(elem.getTargetName(), elem.getTargetTypeName(), false);
        if (targetMethod == null) {
            if (!target.isImaginary()) {
                elem.printMessage(this.ap, Kind.ERROR, "Could not locate @Invoker target " + elem + " in target " + target);
                return;
            }
            
            targetMethod = new MethodHandle(target, elem.getTargetName(), elem.getDesc());
        }

        if (!elem.shouldRemap()) {
            return;
        }

        ObfuscationData<MappingMethod> obfData = this.obf.getDataProvider().getObfMethod(targetMethod.asMapping(false).move(target.getName()));
        if (obfData.isEmpty()) {
            String info = this.mixin.isMultiTarget() ? " in target " + target : "";
            elem.printMessage(this.ap, Kind.WARNING, "Unable to locate obfuscation mapping" + info + " for @Accessor target " + elem);
            return;
        }

        obfData = AnnotatedMixinElementHandler.<MappingMethod>stripOwnerData(obfData);

        try {
            this.obf.getReferenceManager().addMethodMapping(this.mixin.getClassRef(), elem.getTargetName(), elem.getContext(), obfData);
        } catch (ReferenceConflictException ex) {
            elem.printMessage(this.ap, Kind.ERROR, "Mapping conflict for @Invoker target " + elem + ": " + ex.getNew() + " for target "
                    + target + " conflicts with existing mapping " + ex.getOld());
        }
    }

    private void registerFactoryForTarget(AnnotatedElementInvoker elem, TypeHandle target) {
        EquivalencyResult equivalency = TypeUtils.isEquivalentType(this.ap.getProcessingEnvironment(), elem.getReturnType(), target.getType());
        if (equivalency.type != Equivalency.EQUIVALENT) {
            if (equivalency.type == Equivalency.EQUIVALENT_BUT_RAW && equivalency.rawType == 1) {
                elem.printMessage(this.ap, Kind.WARNING, "Raw return type for Factory @Invoker", SuppressedBy.RAW_TYPES);
            } else if (equivalency.type == Equivalency.BOUNDS_MISMATCH) {
                elem.printMessage(this.ap, Kind.ERROR, "Invalid Factory @Invoker return type, generic type arguments of " + target.getType()
                        + " are incompatible with " + elem.getReturnType() + ". " + equivalency);
                return;
            } else {
                elem.printMessage(this.ap, Kind.ERROR, "Invalid Factory @Invoker return type, expected " + target.getType() + " but found "
                        + elem.getReturnType());
                return;
            }
        }
        if (!elem.isStatic()) {
            elem.printMessage(this.ap, Kind.ERROR, "Factory @Invoker must be static");
            return;
        }

        if (!elem.shouldRemap()) {
            return;
        }

        ObfuscationData<String> obfData = this.obf.getDataProvider().getObfClass(elem.getAnnotationValue().replace('.', '/'));
        this.obf.getReferenceManager().addClassMapping(this.mixin.getClassRef(), elem.getAnnotationValue(), obfData);
    }

    private String getAccessorTargetName(AnnotatedElementAccessor elem) {
        String value = elem.getAnnotationValue();
        if (Strings.isNullOrEmpty(value)) {
            return this.inflectAccessorTarget(elem);
        }
        return value;
    }

    private String inflectAccessorTarget(AnnotatedElementAccessor elem) {
        return AccessorInfo.inflectTarget(elem.getSimpleName(), elem.getAccessorType(), "", this, false);
    }

}
