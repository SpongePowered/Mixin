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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;

import com.google.common.collect.ImmutableSet;


public class FakeElement implements TypeElement {
    
    private final NestingKind nesting;

    private final Element parent;
    
    private final Name name;
    
    private final List<Element> children = new ArrayList<Element>();

    private final TypeMirror typeMirror;

    public FakeElement(Element parent, final String name) {
        this.nesting = name.matches("^[0-9].*$") ? NestingKind.ANONYMOUS : NestingKind.MEMBER;
        this.parent = parent;
        this.name = new Name() {
            @Override
            public int length() {
                return name.length();
            }

            @Override
            public char charAt(int index) {
                return name.charAt(index);
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return name.substring(start, end);
            }

            @Override
            public boolean contentEquals(CharSequence cs) {
                return name.equals(cs);
            }
        };
        
        if (parent instanceof FakeElement) {
            ((FakeElement)parent).children.add(this);
        }
        
        this.typeMirror = new FakeType(this);
    }
    
    public FakeElement addChild(String name) {
        return new FakeElement(this, name);
    }

    @Override
    public TypeMirror asType() {
        return this.typeMirror;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.CLASS;
    }

    @Override
    public List<? extends AnnotationMirror> getAnnotationMirrors() {
        return Collections.<AnnotationMirror>emptyList();
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return null;
    }

    @Override
    public Set<Modifier> getModifiers() {
        return ImmutableSet.<Modifier>of(
            Modifier.PRIVATE
        );
    }

    @Override
    public Name getSimpleName() {
        return this.name;
    }

    @Override
    public Element getEnclosingElement() {
        return this.parent;
    }

    @Override
    public List<? extends Element> getEnclosedElements() {
        return this.children;
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> v, P p) {
        return null;
    }

    @Override
    public NestingKind getNestingKind() {
        return this.nesting;
    }

    @Override
    public Name getQualifiedName() {
        return null;
    }

    @Override
    public TypeMirror getSuperclass() {
        return null;
    }

    @Override
    public List<? extends TypeMirror> getInterfaces() {
        return null;
    }

    @Override
    public List<? extends TypeParameterElement> getTypeParameters() {
        return null;
    }

}
