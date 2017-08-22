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

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;

import org.spongepowered.asm.obfuscation.mapping.IMapping;
import org.spongepowered.asm.obfuscation.mapping.IMapping.Type;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.Mappings.MappingConflictException;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IObfuscationDataProvider;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;

/**
 * A module for {@link AnnotatedMixin} which handles shadowed fields and methods
 */
class AnnotatedMixinElementHandlerShadow extends AnnotatedMixinElementHandler {
    
    /**
     * A shadow element to be processed
     * 
     * @param <E> Type of inner element
     * @param <M> Mapping type of mapping used by this element
     */
    abstract static class AnnotatedElementShadow<E extends Element, M extends IMapping<M>> extends AnnotatedElement<E> {
        
        private final boolean shouldRemap;
        
        private final ShadowElementName name;
        
        private final Type type;

        protected AnnotatedElementShadow(E element, AnnotationHandle annotation, boolean shouldRemap, Type type) {
            super(element, annotation);
            this.shouldRemap = shouldRemap;
            this.name = new ShadowElementName(element, annotation);
            this.type = type;
        }
        
        public boolean shouldRemap() {
            return this.shouldRemap;
        }
        
        public ShadowElementName getName() {
            return this.name;
        }

        public Type getElementType() {
            return this.type;
        }

        @Override
        public String toString() {
            return this.getElementType().name().toLowerCase();
        }
        
        public ShadowElementName setObfuscatedName(IMapping<?> name) {
            return this.setObfuscatedName(name.getSimpleName());
        }
        
        public ShadowElementName setObfuscatedName(String name) {
            return this.getName().setObfuscatedName(name);
        }
        
        public ObfuscationData<M> getObfuscationData(IObfuscationDataProvider provider, TypeHandle owner) {
            return provider.getObfEntry(this.getMapping(owner, this.getName().toString(), this.getDesc()));
        }
        
        public abstract M getMapping(TypeHandle owner, String name, String desc);

        public abstract void addMapping(ObfuscationType type, IMapping<?> remapped);

    }
    
    /**
     * Shadow field element
     */
    class AnnotatedElementShadowField extends AnnotatedElementShadow<VariableElement, MappingField> {

        public AnnotatedElementShadowField(VariableElement element, AnnotationHandle annotation, boolean shouldRemap) {
            super(element, annotation, shouldRemap, Type.FIELD);
        }
        
        @Override
        public MappingField getMapping(TypeHandle owner, String name, String desc) {
            return new MappingField(owner.getName(), name, desc);
        }
        
        @Override
        public void addMapping(ObfuscationType type, IMapping<?> remapped) {
            AnnotatedMixinElementHandlerShadow.this.addFieldMapping(type, this.setObfuscatedName(remapped), this.getDesc(), remapped.getDesc());
        }

    }
    
    /**
     * Shadow method element
     */
    class AnnotatedElementShadowMethod extends AnnotatedElementShadow<ExecutableElement, MappingMethod> {

        public AnnotatedElementShadowMethod(ExecutableElement element, AnnotationHandle annotation, boolean shouldRemap) {
            super(element, annotation, shouldRemap, Type.METHOD);
        }
        
        @Override
        public MappingMethod getMapping(TypeHandle owner, String name, String desc) {
            return owner.getMappingMethod(name, desc);
        }
        
        @Override
        public void addMapping(ObfuscationType type, IMapping<?> remapped) {
            AnnotatedMixinElementHandlerShadow.this.addMethodMapping(type, this.setObfuscatedName(remapped), this.getDesc(), remapped.getDesc());
        }

    }

    AnnotatedMixinElementHandlerShadow(IMixinAnnotationProcessor ap, AnnotatedMixin mixin) {
        super(ap, mixin);
    }

    /**
     * Register a {@link org.spongepowered.asm.mixin.Shadow} field or method
     */
    public void registerShadow(AnnotatedElementShadow<?, ?> elem) {
        this.validateTarget(elem.getElement(), elem.getAnnotation(), elem.getName(), "@Shadow");
        
        if (!elem.shouldRemap()) {
            return;
        }
        
        for (TypeHandle target : this.mixin.getTargets()) {
            this.registerShadowForTarget(elem, target);
        }
    }

    private void registerShadowForTarget(AnnotatedElementShadow<?, ?> elem, TypeHandle target) {
        ObfuscationData<? extends IMapping<?>> obfData = elem.getObfuscationData(this.obf.getDataProvider(), target);
        
        if (obfData.isEmpty()) {
            String info = this.mixin.isMultiTarget() ? " in target " + target : "";
            if (target.isSimulated()) {
                elem.printMessage(this.ap, Kind.WARNING, "Unable to locate obfuscation mapping" + info + " for @Shadow " + elem);
            } else {
                elem.printMessage(this.ap, Kind.WARNING, "Unable to locate obfuscation mapping" + info + " for @Shadow " + elem);
            }
            return;
        }

        for (ObfuscationType type : obfData) {
            try {
                elem.addMapping(type, obfData.get(type));
            } catch (MappingConflictException ex) {
                elem.printMessage(this.ap, Kind.ERROR, "Mapping conflict for @Shadow " + elem + ": " + ex.getNew().getSimpleName() + " for target "
                        + target + " conflicts with existing mapping " + ex.getOld().getSimpleName());
            }
        }
    }

}
