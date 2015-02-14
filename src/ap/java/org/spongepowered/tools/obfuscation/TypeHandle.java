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

import java.util.Collections;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

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
}
