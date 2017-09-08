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
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.mirror.mapping.ResolvableMappingMethod;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * A wrapper for TypeElement which gives us a soft-failover mechanism when
 * dealing with classes that are inaccessible via mirror (such as anonymous
 * inner classes).
 */
public class TypeHandle {
    
    /**
     * Internal class name (FQ) 
     */
    private final String name;
    
    /**
     * Enclosing package, used on imaginary elements to perform at least
     * rudimentary validation
     */
    private final PackageElement pkg;
    
    /**
     * Actual type element, this is null for inaccessible classes
     */
    private final TypeElement element;
    
    /**
     * Reference to this handle, for serialisation 
     */
    private TypeReference reference;

    /**
     * Ctor for imaginary elements, require the enclosing package and the FQ
     * name
     * 
     * @param pkg Package
     * @param name FQ class name
     */
    public TypeHandle(PackageElement pkg, String name) {
        this.name = name.replace('.', '/');
        this.pkg = pkg;
        this.element = null;
    }
    
    /**
     * Ctor for real elements
     * 
     * @param element ze element
     */
    public TypeHandle(TypeElement element) {
        this.pkg = TypeUtils.getPackage(element);
        this.name = TypeUtils.getInternalName(element);
        this.element = element;
    }
    
    /**
     * Ctor for real elements, instanced via a type mirror
     * 
     * @param type type
     */
    public TypeHandle(DeclaredType type) {
        this((TypeElement)type.asElement());
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public final String toString() {
        return this.name.replace('/', '.');
    }
    
    /**
     * Returns the fully qualified class name
     */
    public final String getName() {
        return this.name;
    }
    
    /**
     * Returns the enclosing package element
     */
    public final PackageElement getPackage() {
        return this.pkg;
    }

    /**
     * Returns the actual element (returns null for imaginary elements)
     */
    public final TypeElement getElement() {
        return this.element;
    }
    
    /**
     * Returns the actual element (returns simulated value for imaginary
     * elements)
     */
    protected TypeElement getTargetElement() {
        return this.element;
    }

    /**
     * Get an annotation handle for the specified annotation on this type
     * 
     * @param annotationClass type of annotation to search for
     * @return new annotation handle, call <tt>exists</tt> on the returned
     *      handle to determine whether the annotation is present
     */
    public AnnotationHandle getAnnotation(Class<? extends Annotation> annotationClass) {
        return AnnotationHandle.of(this.getTargetElement(), annotationClass);
    }

    /**
     * Returns enclosed elements (methods, fields, etc.)
     */
    public final List<? extends Element> getEnclosedElements() {
        return TypeHandle.getEnclosedElements(this.getTargetElement());
    }
    
    /**
     * Returns enclosed elements (methods, fields, etc.) of a particular type
     * 
     * @param kind types of element to return
     * @param <T> list element type
     */
    public <T extends Element> List<T> getEnclosedElements(ElementKind... kind) {
        return TypeHandle.getEnclosedElements(this.getTargetElement(), kind);
    }

    /**
     * Returns the enclosed element as a type mirror, or null if this is an
     * imaginary type
     */
    public TypeMirror getType() {
        return this.getTargetElement() != null ? this.getTargetElement().asType() : null;
    }
    
    /**
     * Returns the enclosed element's superclass if available, or null if this
     * class does not have a superclass
     */
    public TypeHandle getSuperclass() {
        TypeElement targetElement = this.getTargetElement();
        if (targetElement == null) {
            return null;
        }
        
        TypeMirror superClass = targetElement.getSuperclass();
        if (superClass == null || superClass.getKind() == TypeKind.NONE) {
            return null;
        }
        
        return new TypeHandle((DeclaredType)superClass);
    }
    
    /**
     * Get interfaces directly implemented by this type
     */
    public List<TypeHandle> getInterfaces() {
        if (this.getTargetElement() == null) {
            return Collections.<TypeHandle>emptyList();
        }
        
        Builder<TypeHandle> list = ImmutableList.<TypeHandle>builder();
        for (TypeMirror iface : this.getTargetElement().getInterfaces()) {
            list.add(new TypeHandle((DeclaredType)iface));
        }
        
        return list.build();
    }

    /**
     * Get whether the element is probably public
     */
    public boolean isPublic() {
        return this.getTargetElement() != null && this.getTargetElement().getModifiers().contains(Modifier.PUBLIC);
    }
    
    /**
     * Get whether the element is imaginary (inaccessible via mirror)
     */
    public boolean isImaginary() {
        return this.getTargetElement() == null;
    }
    
    /**
     * Get whether this handle is simulated
     */
    public boolean isSimulated() {
        return false;
    }
    
    /**
     * Get the TypeReference for this type, used for serialisation
     */
    public final TypeReference getReference() {
        if (this.reference == null) {
            this.reference = new TypeReference(this);
        }
        return this.reference;
    }

    /**
     * Return a method as a remapping candidate, usually returns a method owned
     * by this class but for simulated handles we resolve the reference in the
     * class hierarchy of the simulated target (eg. self)
     * 
     * @param name Method name
     * @param desc Method descriptor
     * @return this handle as a mapping method
     */
    public MappingMethod getMappingMethod(String name, String desc) {
        return new ResolvableMappingMethod(this, name, desc);
    }

    /**
     * Find a descriptor for the supplied MemberInfo
     * 
     * @param memberInfo MemberInfo to use as search term
     * @return descriptor or null if no matching member could be located
     */
    public String findDescriptor(MemberInfo memberInfo) {
        String desc = memberInfo.desc;
        if (desc == null) {
            for (ExecutableElement method : this.<ExecutableElement>getEnclosedElements(ElementKind.METHOD)) {
                if (method.getSimpleName().toString().equals(memberInfo.name)) {
                    desc = TypeUtils.getDescriptor(method);
                    break;
                }
            }
        }
        return desc;
    }

    /**
     * Find a member field in this type which matches the name and declared type
     * of the supplied element
     * 
     * @param element Element to match
     * @return handle to the discovered field if matched or null if no match
     */
    public final FieldHandle findField(VariableElement element) {
        return this.findField(element, true);
    }
    
    /**
     * Find a member field in this type which matches the name and declared type
     * of the supplied element
     * 
     * @param element Element to match
     * @param caseSensitive True if case-sensitive comparison should be used
     * @return handle to the discovered field if matched or null if no match
     */
    public final FieldHandle findField(VariableElement element, boolean caseSensitive) {
        return this.findField(element.getSimpleName().toString(), TypeUtils.getTypeName(element.asType()), caseSensitive);
    }
    
    /**
     * Find a member field in this type which matches the name and declared type
     * specified
     * 
     * @param name Field name to search for
     * @param type Field descriptor (java-style)
     * @return handle to the discovered field if matched or null if no match
     */
    public final FieldHandle findField(String name, String type) {
        return this.findField(name, type, true);
    }
    
    /**
     * Find a member field in this type which matches the name and declared type
     * specified
     * 
     * @param name Field name to search for
     * @param type Field descriptor (java-style)
     * @param caseSensitive True if case-sensitive comparison should be used
     * @return handle to the discovered field if matched or null if no match
     */
    public FieldHandle findField(String name, String type, boolean caseSensitive) {
        String rawType = TypeUtils.stripGenerics(type);

        for (VariableElement field : this.<VariableElement>getEnclosedElements(ElementKind.FIELD)) {
            if (TypeHandle.compareElement(field, name, type, caseSensitive)) {
                return new FieldHandle(this.getTargetElement(), field);
            } else if (TypeHandle.compareElement(field, name, rawType, caseSensitive)) {
                return new FieldHandle(this.getTargetElement(), field, true);
            }                
        }
        
        return null;
    }
    
    /**
     * Find a member method in this type which matches the name and declared
     * type of the supplied element
     * 
     * @param element Element to match
     * @return handle to the discovered method if matched or null if no match
     */
    public final MethodHandle findMethod(ExecutableElement element) {
        return this.findMethod(element, true);
    }

    /**
     * Find a member method in this type which matches the name and declared
     * type of the supplied element
     * 
     * @param element Element to match
     * @param caseSensitive True if case-sensitive comparison should be used
     * @return handle to the discovered method if matched or null if no match
     */
    public final MethodHandle findMethod(ExecutableElement element, boolean caseSensitive) {
        return this.findMethod(element.getSimpleName().toString(), TypeUtils.getJavaSignature(element), caseSensitive);
    }

    /**
     * Find a member method in this type which matches the name and signature
     * specified
     * 
     * @param name Method name to search for
     * @param signature Method signature
     * @return handle to the discovered method if matched or null if no match
     */
    public final MethodHandle findMethod(String name, String signature) {
        return this.findMethod(name, signature, true);
    }
    
    /**
     * Find a member method in this type which matches the name and signature
     * specified
     * 
     * @param name Method name to search for
     * @param signature Method signature
     * @param matchCase True if case-sensitive comparison should be used
     * @return handle to the discovered method if matched or null if no match
     */
    public MethodHandle findMethod(String name, String signature, boolean matchCase) {
        String rawSignature = TypeUtils.stripGenerics(signature);
        return TypeHandle.findMethod(this, name, signature, rawSignature, matchCase);
    }

    protected static MethodHandle findMethod(TypeHandle target, String name, String signature, String rawSignature, boolean matchCase) {
        for (ExecutableElement method : TypeHandle.<ExecutableElement>getEnclosedElements(target.getTargetElement(),
                ElementKind.CONSTRUCTOR, ElementKind.METHOD)) {
            if (TypeHandle.compareElement(method, name, signature, matchCase) || TypeHandle.compareElement(method, name, rawSignature, matchCase)) {
                return new MethodHandle(target, method);
            }
        }
        return null;
    }
    
    protected static boolean compareElement(Element elem, String name, String type, boolean matchCase) {
        try {
            String elementName = elem.getSimpleName().toString();
            String elementType = TypeUtils.getJavaSignature(elem);
            String rawElementType = TypeUtils.stripGenerics(elementType);
            boolean compared = matchCase ? name.equals(elementName) : name.equalsIgnoreCase(elementName);
            return compared && (type.length() == 0 || type.equals(elementType) || type.equals(rawElementType));
        } catch (NullPointerException ex) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T extends Element> List<T> getEnclosedElements(TypeElement targetElement, ElementKind... kind) {
        if (kind == null || kind.length < 1) {
            return (List<T>)TypeHandle.getEnclosedElements(targetElement);
        }
        
        if (targetElement == null) {
            return Collections.<T>emptyList();
        }
        
        Builder<T> list = ImmutableList.<T>builder();
        for (Element elem : targetElement.getEnclosedElements()) {
            for (ElementKind ek : kind) {
                if (elem.getKind() == ek) {
                    list.add((T)elem);
                    break;
                }
            }
        }

        return list.build();
    }

    protected static List<? extends Element> getEnclosedElements(TypeElement targetElement) {
        return targetElement != null ? targetElement.getEnclosedElements() : Collections.<Element>emptyList();
    }

}
