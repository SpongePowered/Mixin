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

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic.Kind;

import org.spongepowered.asm.mixin.Interface.Remap;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.MethodHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeUtils;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;

/**
 * A module for {@link AnnotatedMixin} whic handles soft-implements clauses
 */
public class AnnotatedMixinElementHandlerSoftImplements extends AnnotatedMixinElementHandler {
    
    AnnotatedMixinElementHandlerSoftImplements(IMixinAnnotationProcessor ap, AnnotatedMixin mixin) {
        super(ap, mixin);
    }

    /**
     * Process a soft-implements annotation on a mixin. This causes the
     * interface declared in the annotation and all of its super-interfaces to
     * be enumerated for member methods. Any member methods which are discovered
     * in the mixin are then tested for remapability based on the strategy
     * defined in the soft-implements decoration
     * 
     * @param implementsAnnotation the &#064;Implements annotation on the
     *      element
     */
    public void process(AnnotationHandle implementsAnnotation) {
        if (!this.mixin.remap()) {
            return;
        }
        
        List<AnnotationHandle> interfaces = implementsAnnotation.getAnnotationList("value");
        
        // Derp?
        if (interfaces.size() < 1) {
            this.ap.printMessage(Kind.WARNING, "Empty @Implements annotation", this.mixin.getMixin(), implementsAnnotation.asMirror());
            return;
        }
        
        for (AnnotationHandle interfaceAnnotation : interfaces) {
            Remap remap = interfaceAnnotation.<Remap>getValue("remap", Remap.ALL);
            if (remap == Remap.NONE) {
                continue;
            }
            
            try {
                TypeHandle iface = new TypeHandle(interfaceAnnotation.<DeclaredType>getValue("iface"));
                String prefix = interfaceAnnotation.<String>getValue("prefix");
                this.processSoftImplements(remap, iface, prefix);
            } catch (Exception ex) {
                this.ap.printMessage(Kind.ERROR, "Unexpected error: " + ex.getClass().getName() + ": " + ex.getMessage(), this.mixin.getMixin(),
                        interfaceAnnotation.asMirror());
            }
        }
    }

    /**
     * Recursive function which processes methods in an interface and its parent
     * interfaces and adds mappings as necessary
     * 
     * @param remap Remapping strategy to use
     * @param iface Interface to enumerate
     * @param prefix Prefix declared in the soft-implements decoration
     */
    private void processSoftImplements(Remap remap, TypeHandle iface, String prefix) {
        for (ExecutableElement method : iface.<ExecutableElement>getEnclosedElements(ElementKind.METHOD)) {
            this.processMethod(remap, iface, prefix, method);
        }
        
        for (TypeHandle superInterface : iface.getInterfaces()) {
            this.processSoftImplements(remap, superInterface, prefix);
        }
    }

    /**
     * Process an interface method. Searches for the interface method with the
     * declared prefix and also searches without prefix if the <tt>ALL</tt>
     * strategy is selected
     * 
     * @param remap Remapping strategy to use
     * @param iface Interface to enumerate
     * @param prefix Prefix declared in the soft-implements decoration
     * @param method Interface method to search for
     */
    private void processMethod(Remap remap, TypeHandle iface, String prefix, ExecutableElement method) {
        String name = method.getSimpleName().toString();
        String sig = TypeUtils.getJavaSignature(method);
        String desc = TypeUtils.getDescriptor(method);
        
        if (remap != Remap.ONLY_PREFIXED) {
            MethodHandle mixinMethod = this.mixin.getHandle().findMethod(name, sig);
            if (mixinMethod != null) {
                this.addInterfaceMethodMapping(remap, iface, null, mixinMethod, name, desc);
            }
        }
        
        if (prefix != null) {
            MethodHandle prefixedMixinMethod = this.mixin.getHandle().findMethod(prefix + name, sig);
            if (prefixedMixinMethod != null) {
                this.addInterfaceMethodMapping(remap, iface, prefix, prefixedMixinMethod, name, desc);
            }
        }        
    }

    /**
     * Searches for obfuscation mappings for the specified interface method and
     * adds mappings to the output set if obfuscation mappings are found for the
     * method
     * 
     * @param remap Remapping strategy
     * @param iface Interface to enumerate
     * @param prefix Prefix declared in the soft-implements decoration
     * @param method Mixin method
     * @param name Undecorated interface method name
     * @param desc Interface method descriptor
     */
    private void addInterfaceMethodMapping(Remap remap, TypeHandle iface, String prefix, MethodHandle method, String name, String desc) {
        MappingMethod mapping = new MappingMethod(iface.getName(), name, desc);
        ObfuscationData<MappingMethod> obfData = this.obf.getDataProvider().getObfMethod(mapping);
        if (obfData.isEmpty()) {
            if (remap.forceRemap()) {
                this.ap.printMessage(Kind.ERROR, "No obfuscation mapping for soft-implementing method", method.getElement());
            }
            return;
        }
        this.addMethodMappings(method.getName(), desc, this.applyPrefix(obfData, prefix));
    }

    /**
     * Apply the specified name prefix to all mappings in an obfuscation data
     * set
     * 
     * @param data input data
     * @param prefix prefix to apply
     * @return modified mapping set or original mapping set if prefix is null
     */
    private ObfuscationData<MappingMethod> applyPrefix(ObfuscationData<MappingMethod> data, String prefix) {
        if (prefix == null) {
            return data;
        }

        ObfuscationData<MappingMethod> prefixed = new ObfuscationData<MappingMethod>();
        for (ObfuscationType type : data) {
            MappingMethod mapping = data.get(type);
            prefixed.put(type, mapping.addPrefix(prefix));
        }
        return prefixed;
    }

}
