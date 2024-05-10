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

import org.spongepowered.asm.mixin.gen.AccessorInfo;
import org.spongepowered.asm.mixin.gen.AccessorInfo.AccessorName;
import org.spongepowered.asm.mixin.gen.AccessorInfo.AccessorType;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorRemappable;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.tools.obfuscation.ReferenceManager.ReferenceConflictException;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerEx.MessageType;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.FieldHandle;
import org.spongepowered.tools.obfuscation.mirror.MethodHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeUtils;

import com.google.common.base.Strings;

/**
 * A module for {@link AnnotatedMixin} which handles accessors
 */
class AnnotatedMixinElementHandlerAccessor extends AnnotatedMixinElementHandler {

    /**
     * Accessor element
     */
    static class AnnotatedElementAccessor extends AnnotatedElementExecutable {

        protected final boolean shouldRemap;
        
        protected final TypeMirror returnType;

        protected String targetName;

        public AnnotatedElementAccessor(ExecutableElement element, AnnotationHandle annotation, IMixinContext context, boolean shouldRemap) {
            super(element, annotation, context, "value");
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

        public String getTargetDesc() {
            return TypeUtils.getInternalName(this.getTargetType());
        }

        public ITargetSelectorRemappable getContext() {
            return new MemberInfo(this.getTargetName(), null, this.getTargetDesc());
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
        
        public AnnotatedElementInvoker(ExecutableElement element, AnnotationHandle annotation, IMixinContext context, boolean shouldRemap) {
            super(element, annotation, context, shouldRemap);
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
                        && (Constants.CTOR.equals(accessorName.name) || target.getSimpleName().equalsIgnoreCase(accessorName.name))) {
                    this.type = AccessorType.OBJECT_FACTORY;
                    return;
                }
            }
        }
        
        @Override
        public String getAnnotationValue() {
            String value = super.getAnnotationValue();
            return (this.type == AccessorType.OBJECT_FACTORY && value == null) ? this.returnType.toString() : value;
        }
        
        @Override
        public boolean shouldRemap() {
            return (this.type == AccessorType.OBJECT_FACTORY
                    || this.type == AccessorType.METHOD_PROXY
                    || this.getAnnotationValue() != null) && super.shouldRemap();
        }

        @Override
        public String getTargetDesc() {
            return this.getDesc();
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

    /**
     * Register a new accessor
     *
     * @param elem accessor element
     */
    public void registerAccessor(AnnotatedElementAccessor elem) {
        if (elem.getAccessorType() == null) {
            elem.printMessage(this.ap, MessageType.ACCESSOR_TYPE_UNSUPPORTED, "Unsupported accessor type");
            return;
        }

        String targetName = this.getAccessorTargetName(elem);
        if (targetName == null) {
            elem.printMessage(this.ap, MessageType.ACCESSOR_NAME_UNRESOLVED, "Cannot inflect accessor target name");
            return;
        }
        elem.setTargetName(targetName);

        for (TypeHandle target : this.mixin.getTargets()) {
            try {
                elem.attach(target);
            } catch (Exception ex) {
                elem.printMessage(this.ap, MessageType.ACCESSOR_ATTACH_ERROR, ex.getMessage());
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
                elem.printMessage(this.ap, MessageType.ACCESSOR_TARGET_NOT_FOUND,
                        "Could not locate @Accessor target " + elem + " in target " + target);
                return;
            }
            
            targetField = new FieldHandle(target.getName(), elem.getTargetName(), elem.getTargetDesc());
        }

        if (!elem.shouldRemap()) {
            return;
        }

        ObfuscationData<MappingField> obfData = this.obf.getDataProvider().getObfField(targetField.asMapping(false).move(target.getName()));
        if (obfData.isEmpty()) {
            String info = this.mixin.isMultiTarget() ? " in target " + target : "";
            elem.printMessage(this.ap, MessageType.NO_OBFDATA_FOR_ACCESSOR,
                    "Unable to locate obfuscation mapping" + info + " for @Accessor target " + elem);
            return;
        }

        obfData = AnnotatedMixinElementHandler.<MappingField>stripOwnerData(obfData);

        try {
            this.obf.getReferenceManager().addFieldMapping(this.mixin.getClassRef(), elem.getTargetName(), elem.getContext(), obfData);
        } catch (ReferenceConflictException ex) {
            elem.printMessage(this.ap, MessageType.ACCESSOR_MAPPING_CONFLICT, "Mapping conflict for @Accessor target " + elem + ": "
                    + ex.getNew() + " for target " + target + " conflicts with existing mapping " + ex.getOld());
        }
    }

    private void registerInvokerForTarget(AnnotatedElementInvoker elem, TypeHandle target) {
        MethodHandle targetMethod = target.findMethod(elem.getTargetName(), elem.getTargetTypeName(), false);
        if (targetMethod == null) {
            if (!target.isImaginary()) {
                elem.printMessage(this.ap, MessageType.ACCESSOR_TARGET_NOT_FOUND,
                        "Could not locate @Invoker target " + elem + " in target " + target);
                return;
            }
            
            targetMethod = new MethodHandle(target, elem.getTargetName(), elem.getTargetDesc());
        }

        if (!elem.shouldRemap()) {
            return;
        }

        ObfuscationData<MappingMethod> obfData = this.obf.getDataProvider().getObfMethod(targetMethod.asMapping(false).move(target.getName()));
        if (obfData.isEmpty()) {
            String info = this.mixin.isMultiTarget() ? " in target " + target : "";
            elem.printMessage(this.ap, MessageType.NO_OBFDATA_FOR_ACCESSOR,
                    "Unable to locate obfuscation mapping" + info + " for @Accessor target " + elem);
            return;
        }

        obfData = AnnotatedMixinElementHandler.<MappingMethod>stripOwnerData(obfData);

        try {
            this.obf.getReferenceManager().addMethodMapping(this.mixin.getClassRef(), elem.getTargetName(), elem.getContext(), obfData);
        } catch (ReferenceConflictException ex) {
            elem.printMessage(this.ap, MessageType.ACCESSOR_MAPPING_CONFLICT, "Mapping conflict for @Invoker target " + elem + ": "
                    + ex.getNew() + " for target " + target + " conflicts with existing mapping " + ex.getOld());
        }
    }

    private void registerFactoryForTarget(AnnotatedElementInvoker elem, TypeHandle target) {
        String returnType = TypeUtils.getTypeName(elem.getReturnType());
        if (!returnType.equals(target.toString())) {
            elem.printMessage(this.ap, MessageType.FACTORY_INVOKER_RETURN_TYPE, "Invalid Factory @Invoker return type, expected "
                    + target + " but found " + returnType);
            return;
        }
        if (!elem.isStatic()) {
            elem.printMessage(this.ap, MessageType.FACTORY_INVOKER_NONSTATIC, "Factory @Invoker must be static");
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
        return AccessorInfo.inflectTarget(elem.getSimpleName(), elem.getAccessorType(), "", elem, false);
    }

}
