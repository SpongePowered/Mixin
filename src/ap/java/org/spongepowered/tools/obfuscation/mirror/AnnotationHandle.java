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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/**
 * A wrapper for {@link AnnotationMirror} which provides a more convenient way
 * to access annotation values.
 */
public class AnnotationHandle {
    
    /**
     * Annotation being wrapped
     */
    private final AnnotationMirror annotation;
    
    /**
     * ctor
     * 
     * @param annotation annotation to wrap
     */
    private AnnotationHandle(AnnotationMirror annotation) {
        this.annotation = annotation;
    }
    
    /**
     * Return the wrapped mirror, only used to pass to Messager methods
     * 
     * @return annotation mirror (can be null)
     */
    public AnnotationMirror asMirror() {
        return this.annotation;
    }
    
    /**
     * Get whether the annotation mirror actually exists, if the mirror is null
     * returns false
     * 
     * @return true if the annotation exists
     */
    public boolean exists() {
        return this.annotation != null;
    }
    
    @Override
    public String toString() {
        if (this.annotation == null) {
            return "@{UnknownAnnotation}";
        }
        return "@" + this.annotation.getAnnotationType().asElement().getSimpleName();
    }
    
    /**
     * Get a value with the specified key from this annotation, return the
     * specified default value if the key is not set or is not present
     * 
     * @param key key
     * @param defaultValue value to return if the key is not set or not present
     * @return value or default if not set
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T getValue(String key, T defaultValue) {
        if (this.annotation == null) {
            return defaultValue;
        }
        
        AnnotationValue value = this.getAnnotationValue(key);
        if (defaultValue instanceof Enum && value != null) {
            VariableElement varValue = (VariableElement)value.getValue();
            if (varValue == null) {
                return defaultValue;
            }
            return (T)Enum.valueOf((Class<? extends Enum>)defaultValue.getClass(), varValue.getSimpleName().toString());
        }
        
        return value != null ? (T)value.getValue() : defaultValue;
    }

    /**
     * Get the annotation value or return null if not present or not set
     * 
     * @return value or null if not present or not set
     */
    public <T> T getValue() {
        return this.getValue("value", null);
    }
    
    /**
     * Get the annotation value with the specified key or return null if not
     * present or not set
     * 
     * @param key key to fetch
     * @return value or null if not present or not set
     */
    public <T> T getValue(String key) {
        return this.getValue(key, null);
    }

    /**
     * Get the primitive boolean value with the specified key or return null if
     * not present or not set
     * 
     * @param key key to fetch
     * @param defaultValue default value to return if value is not present
     * @return value or default if not present or not set
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return this.<Boolean>getValue(key, Boolean.valueOf(defaultValue)).booleanValue();
    }

    /**
     * Retrieve the annotation value as a list with values of the specified
     * type. Returns an empty list if the value is not present or not set.
     * 
     * @return list of values
     */
    public <T> List<T> getList() {
        return this.<T>getList("value");
    }
    
    /**
     * Retrieve the annotation value with the specified key as a list with
     * values of the specified type. Returns an empty list if the value is not
     * present or not set.
     * 
     * @param key key to fetch
     * @return list of values
     */
    public <T> List<T> getList(String key) {
        List<AnnotationValue> list = this.<List<AnnotationValue>>getValue(key, Collections.<AnnotationValue>emptyList());
        return AnnotationHandle.<T>unwrapAnnotationValueList(list);
    }

    /**
     * Retrieve an annotation key as a list of annotation handles
     * 
     * @param key key to fetch
     * @return list of annotations
     */
    public List<AnnotationHandle> getAnnotationList(String key) {
        List<AnnotationValue> list = this.<List<AnnotationValue>>getValue(key, null);
        if (list == null) {
            return Collections.<AnnotationHandle>emptyList();
        }
        
        List<AnnotationHandle> annotations = new ArrayList<AnnotationHandle>(list.size());
        for (AnnotationValue value : list) {
            annotations.add(new AnnotationHandle((AnnotationMirror)value.getValue()));
        }
        return annotations;
    }

    protected AnnotationValue getAnnotationValue(String key) {
        for (ExecutableElement elem : this.annotation.getElementValues().keySet()) {
            if (elem.getSimpleName().contentEquals(key)) {
                return this.annotation.getElementValues().get(elem);
            }
        }
        
        return null;
    }
    
    @SuppressWarnings("unchecked")
    protected static <T> List<T> unwrapAnnotationValueList(List<AnnotationValue> list) {
        if (list == null) {
            return Collections.<T>emptyList();
        }
        
        List<T> unfolded = new ArrayList<T>(list.size());
        for (AnnotationValue value : list) {
            unfolded.add((T)value.getValue());
        }
        
        return unfolded;
    }
    
    protected static AnnotationMirror getAnnotation(Element elem, Class<? extends Annotation> annotationClass) {
        if (elem == null) {
            return null;
        }
        
        List<? extends AnnotationMirror> annotations = elem.getAnnotationMirrors();
        
        if (annotations == null) {
            return null;
        }
        
        for (AnnotationMirror annotation : annotations) {
            Element element = annotation.getAnnotationType().asElement();
            if (!(element instanceof TypeElement)) {
                continue;
            }
            TypeElement annotationElement = (TypeElement)element;
            if (annotationElement.getQualifiedName().contentEquals(annotationClass.getName())) {
                return annotation;
            }
        }
        
        return null;
    }
    
    public static AnnotationHandle of(AnnotationMirror annotation) {
        return new AnnotationHandle(annotation);
    }

    public static AnnotationHandle of(Element elem, Class<? extends Annotation> annotationClass) {
        return new AnnotationHandle(AnnotationHandle.getAnnotation(elem, annotationClass));
    }
}
