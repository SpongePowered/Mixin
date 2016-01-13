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
import org.spongepowered.tools.MirrorUtils;

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
        this.pkg = MirrorUtils.getPackage(element);
        this.name = MirrorUtils.getInternalName(element);
        this.element = element;
    }
    
    /**
     * Ctor for real elements, instanced via a type mirror
     * 
     * @param type
     */
    public TypeHandle(DeclaredType type) {
        this((TypeElement)type.asElement());
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.name.replace('/', '.');
    }
    
    /**
     * Returns the fully qualified class name
     */
    public String getName() {
        return this.name;
    }
    
    /**
     * Returns the enclosing package element
     */
    public PackageElement getPackage() {
        return this.pkg;
    }

    /**
     * Returns the actual element (returns null for imaginary elements)
     */
    public TypeElement getElement() {
        return this.element;
    }

    /**
     * Returns enclosed elements (methods, fields, etc.)
     */
    public List<? extends Element> getEnclosedElements() {
        return this.element != null ? this.element.getEnclosedElements() : Collections.<Element>emptyList();
    }

    /**
     * Returns the enclosed element as a type mirror, or null if this is an
     * imaginary type
     */
    public TypeMirror getType() {
        return this.element != null ? this.element.asType() : null;
    }
    
    /**
     * Returns the enclosed element's superclass if available, or null if this
     * class does not have a superclass
     */
    public TypeHandle getSuperclass() {
        if (this.element == null) {
            return null;
        }
        
        TypeMirror superClass = this.element.getSuperclass();
        if (superClass == null || superClass.getKind() == TypeKind.NONE) {
            return null;
        }
        
        return new TypeHandle((DeclaredType)superClass);
    }

    /**
     * Get whether the element is probably public
     */
    public boolean isPublic() {
        return this.element != null ? this.element.getModifiers().contains(Modifier.PUBLIC) : false;
    }
    
    /**
     * Get whether the element is imaginary (inaccessible via mirror)
     */
    public boolean isImaginary() {
        return this.element == null;
    }
    
    public TypeReference getReference() {
        if (this.reference == null) {
            this.reference = new TypeReference(this);
        }
        return this.reference;
    }

    public String findDescriptor(MemberInfo memberInfo) {
        String desc = memberInfo.desc;
        if (desc == null) {
            for (Element child : this.getEnclosedElements()) {
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

    /**
     * Find a member field in this type which matches the name and declared type
     * of the supplied element
     * 
     * @param element Element to match
     * @return handle to the discovered field if matched or null if no match
     */
    public FieldHandle findField(VariableElement element) {
        return this.findField(element.getSimpleName().toString(), element.asType().toString());
    }
    
    /**
     * Find a member field in this type which matches the name and declared type
     * specified
     * 
     * @param name Field name to search for
     * @param type Field descriptor (java-style)
     * @return handle to the discovered field if matched or null if no match
     */
    public FieldHandle findField(String name, String type) {
        String rawType = TypeHandle.stripGenerics(type);

        for (Element element : this.getEnclosedElements()) {
            if (element.getKind() != ElementKind.FIELD) {
                continue;
            }
            
            VariableElement field = (VariableElement)element;
            if (this.compareElement(field, name, type)) {
                return new FieldHandle(field);
            } else if (this.compareElement(field, name, rawType)) {
                return new FieldHandle(field, true);
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
    public MethodHandle findMethod(ExecutableElement element) {
        return this.findMethod(element.getSimpleName().toString(), TypeHandle.getElementSignature(element));
    }

    /**
     * Find a member method in this type which matches the name and signature
     * specified
     * 
     * @param name Method name to search for
     * @param signature Method signature
     * @return handle to the discovered method if matched or null if no match
     */
    public MethodHandle findMethod(String name, String signature) {
        String rawSignature = TypeHandle.stripGenerics(signature);

        for (Element element : this.getEnclosedElements()) {
            switch (element.getKind()) {
                case CONSTRUCTOR:
                case METHOD:
                    ExecutableElement method = (ExecutableElement)element;
                    if (this.compareElement(method, name, signature) || this.compareElement(method, name, rawSignature)) {
                        return new MethodHandle(method);
                    }
                    
                    break;
//                case STATIC_INIT:  // TODO?
//                    break;
                default:
                    break;
            }
            
        }
        
        return null;
    }
    
    private boolean compareElement(Element elem, String name, String type) {
        try {
            String elementName = elem.getSimpleName().toString();
            String elementType = TypeHandle.getElementSignature(elem);
            return name.equals(elementName) && (type.length() == 0 || type.equals(elementType));
        } catch (NullPointerException ex) {
            return false;
        }
    }
    
    static String getElementSignature(Element element) {
        if (element instanceof ExecutableElement) {
            ExecutableElement method = (ExecutableElement)element;
            StringBuilder desc = new StringBuilder().append("(");
            boolean extra = false;
            for (VariableElement arg : method.getParameters()) {
                if (extra) {
                    desc.append(',');
                }
                desc.append(arg.asType().toString());
                extra = true;
            }
            desc.append(')').append(method.getReturnType().toString());
            return desc.toString();
        }
        
        return element.asType().toString();
    }

    static String stripGenerics(String type) {
        StringBuilder sb = new StringBuilder();
        for (int pos = 0, depth = 0; pos < type.length(); pos++) {
            char c = type.charAt(pos);
            if (c == '<') {
                depth++;
            }
            if (depth == 0) {
                sb.append(c);
            } else if (c == '>') {
                depth--;
            }
        }
        return sb.toString();
    }

}
