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
package org.spongepowered.tools.obfuscation.mirror;

import java.lang.annotation.Annotation;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.asm.util.SignaturePrinter;

/**
 * A simulated type handle, used with virtual (pseudo) mixins. For obfuscation
 * purposes, we have to use some kind of context to resolve target members so
 * that appropriate refmaps can be generated. For this purpose we use the mixin
 * itself as the context in order to allow us to look up members in superclasses
 * and superinterfaces of the mixin (in the hope that we can locate targets
 * there. If we cannot achieve this, then remapping will have to be done by hand
 */
public class TypeHandleSimulated extends TypeHandle {
    
    private final TypeElement simulatedType;

    public TypeHandleSimulated(String name, TypeMirror type) {
        this(TypeUtils.getPackage(type), name, type);
    }
    
    public TypeHandleSimulated(PackageElement pkg, String name, TypeMirror type) {
        super(pkg, name);
        this.simulatedType = (TypeElement)((DeclaredType)type).asElement();
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.mirror.TypeHandle
     *      #getTargetElement()
     */
    @Override
    protected TypeElement getTargetElement() {
        return this.simulatedType;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.mirror.TypeHandle#isPublic()
     */
    @Override
    public boolean isPublic() {
        // We have no idea
        return true;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.mirror.TypeHandle#isImaginary()
     */
    @Override
    public boolean isImaginary() {
        // it actually is, but we need the AP to assume that it isn't
        return false;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.mirror.TypeHandle#isSimulated()
     */
    @Override
    public boolean isSimulated() {
        // hell yeah
        return true;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.mirror.TypeHandle
     *      #getAnnotation(java.lang.Class)
     */
    @Override
    public AnnotationHandle getAnnotation(Class<? extends Annotation> annotationClass) {
        // nope
        return null;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.mirror.TypeHandle
     *      #getSuperclass()
     */
    @Override
    public TypeHandle getSuperclass() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.mirror.TypeHandle
     *  #findDescriptor(org.spongepowered.asm.mixin.injection.struct.MemberInfo)
     */
    @Override
    public String findDescriptor(MemberInfo memberInfo) {
        // Identity, refs need to be FQ
        return memberInfo != null ? memberInfo.desc : null;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.mirror.TypeHandle
     *      #findField(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public FieldHandle findField(String name, String type, boolean caseSensitive) {
        return new FieldHandle(null, name, type);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.mirror.TypeHandle
     *      #findMethod(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public MethodHandle findMethod(String name, String desc, boolean caseSensitive) {
        // assume we find it
        return new MethodHandle(null, name, desc);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.mirror.TypeHandle
     *      #getMappingMethod(java.lang.String, java.lang.String)
     */
    @Override
    public MappingMethod getMappingMethod(String name, String desc) {
        // Transform the MemberInfo descriptor into a signature
        String signature = new SignaturePrinter(name, desc).setFullyQualified(true).toDescriptor();
        String rawSignature = TypeUtils.stripGenerics(signature);
        
        // Try to locate a member anywhere in the hierarchy which matches
        MethodHandle method = TypeHandleSimulated.findMethodRecursive(this, name, signature, rawSignature, true);
        
        // If we find one, return it otherwise just simulate the method
        return method != null ? method.asMapping(true) : super.getMappingMethod(name, desc);

    }

    private static MethodHandle findMethodRecursive(TypeHandle target, String name, String signature, String rawSignature, boolean matchCase) {
        TypeElement elem = target.getTargetElement();
        if (elem == null) {
            return null;
        }
        
        MethodHandle method = TypeHandle.findMethod(target, name, signature, rawSignature, matchCase);
        if (method != null) {
            return method;
        }
        
        for (TypeMirror iface : elem.getInterfaces()) {
            method = TypeHandleSimulated.findMethodRecursive(iface, name, signature, rawSignature, matchCase);
            if (method != null) {
                return method;
            }
        }
        
        TypeMirror superClass = elem.getSuperclass();
        if (superClass == null || superClass.getKind() == TypeKind.NONE) {
            return null;
        }
        
        return TypeHandleSimulated.findMethodRecursive(superClass, name, signature, rawSignature, matchCase);
    }

    private static MethodHandle findMethodRecursive(TypeMirror target, String name, String signature, String rawSignature, boolean matchCase) {
        if (!(target instanceof DeclaredType)) {
            return null;
        }
        TypeElement element = (TypeElement)((DeclaredType)target).asElement();
        return TypeHandleSimulated.findMethodRecursive(new TypeHandle(element), name, signature, rawSignature, matchCase);
    }
    
}
