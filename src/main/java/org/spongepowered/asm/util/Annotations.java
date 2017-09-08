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
package org.spongepowered.asm.util;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.FieldNode;
import org.spongepowered.asm.lib.tree.MethodNode;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * Utility class for working with ASM annotations
 */
public final class Annotations {
    
    private Annotations() {
        // Utility class
    }

    /**
     * Set a runtime-visible annotation of the specified class on the supplied
     * field node
     *
     * @param field Target field
     * @param annotationClass Type of annotation to search for
     * @param value Values (interleaved key/value pairs) to set
     */
    public static void setVisible(FieldNode field, Class<? extends Annotation> annotationClass, Object... value) {
        AnnotationNode node = Annotations.createNode(Type.getDescriptor(annotationClass), value);
        field.visibleAnnotations = Annotations.add(field.visibleAnnotations, node);
    }
    
    /**
     * Set an invisible annotation of the specified class on the supplied field
     * node
     *
     * @param field Target field
     * @param annotationClass Type of annotation to search for
     * @param value Values (interleaved key/value pairs) to set
     */
    public static void setInvisible(FieldNode field, Class<? extends Annotation> annotationClass, Object... value) {
        AnnotationNode node = Annotations.createNode(Type.getDescriptor(annotationClass), value);
        field.invisibleAnnotations = Annotations.add(field.invisibleAnnotations, node);
    }
    
    /**
     * Set a runtime-visible annotation of the specified class on the supplied
     * method node
     *
     * @param method Target method
     * @param annotationClass Type of annotation to search for
     * @param value Values (interleaved key/value pairs) to set
     */
    public static void setVisible(MethodNode method, Class<? extends Annotation> annotationClass, Object... value) {
        AnnotationNode node = Annotations.createNode(Type.getDescriptor(annotationClass), value);
        method.visibleAnnotations = Annotations.add(method.visibleAnnotations, node);
    }
    
    /**
     * Set a invisible annotation of the specified class on the supplied method
     * node
     *
     * @param method Target method
     * @param annotationClass Type of annotation to search for
     * @param value Values (interleaved key/value pairs) to set
     */
    public static void setInvisible(MethodNode method, Class<? extends Annotation> annotationClass, Object... value) {
        AnnotationNode node = Annotations.createNode(Type.getDescriptor(annotationClass), value);
        method.invisibleAnnotations = Annotations.add(method.invisibleAnnotations, node);
    }

    /**
     * Create a new annotation node with the supplied values
     * 
     * @param annotationType Name (internal name) of the annotation interface to
     *      create
     * @param value Interleaved key/value pairs. Keys must be strings
     * @return new annotation node
     */
    private static AnnotationNode createNode(String annotationType, Object... value) {
        AnnotationNode node = new AnnotationNode(annotationType);
        for (int pos = 0; pos < value.length - 1; pos += 2) {
            if (!(value[pos] instanceof String)) {
                throw new IllegalArgumentException("Annotation keys must be strings, found " + value[pos].getClass().getSimpleName()
                        + " with " + value[pos].toString() + " at index " + pos + " creating " + annotationType);
            }
            node.visit((String)value[pos], value[pos + 1]);
        }
        return node;
    }
    
    private static List<AnnotationNode> add(List<AnnotationNode> annotations, AnnotationNode node) {
        if (annotations == null) {
            annotations = new ArrayList<AnnotationNode>(1);
        } else {
            annotations.remove(Annotations.get(annotations, node.desc));
        }
        annotations.add(node);
        return annotations;
    }

    /**
     * Get a runtime-visible annotation of the specified class from the supplied
     * field node
     *
     * @param field Source field
     * @param annotationClass Type of annotation to search for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getVisible(FieldNode field, Class<? extends Annotation> annotationClass) {
        return Annotations.get(field.visibleAnnotations, Type.getDescriptor(annotationClass));
    }

    /**
     * Get an invisible annotation of the specified class from the supplied
     * field node
     *
     * @param field Source field
     * @param annotationClass Type of annotation to search for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getInvisible(FieldNode field, Class<? extends Annotation> annotationClass) {
        return Annotations.get(field.invisibleAnnotations, Type.getDescriptor(annotationClass));
    }

    /**
     * Get a runtime-visible annotation of the specified class from the supplied
     * method node
     *
     * @param method Source method
     * @param annotationClass Type of annotation to search for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getVisible(MethodNode method, Class<? extends Annotation> annotationClass) {
        return Annotations.get(method.visibleAnnotations, Type.getDescriptor(annotationClass));
    }

    /**
     * Get an invisible annotation of the specified class from the supplied
     * method node
     *
     * @param method Source method
     * @param annotationClass Type of annotation to search for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getInvisible(MethodNode method, Class<? extends Annotation> annotationClass) {
        return Annotations.get(method.invisibleAnnotations, Type.getDescriptor(annotationClass));
    }

    /**
     * Get a runtime-visible annotation of the specified class from the supplied
     * method node
     *
     * @param method Source method
     * @param annotationClasses Types of annotation to search for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getSingleVisible(MethodNode method, Class<? extends Annotation>... annotationClasses) {
        return Annotations.getSingle(method.visibleAnnotations, annotationClasses);
    }

    /**
     * Get an invisible annotation of the specified class from the supplied
     * method node
     *
     * @param method Source method
     * @param annotationClasses Types of annotation to search for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getSingleInvisible(MethodNode method, Class<? extends Annotation>... annotationClasses) {
        return Annotations.getSingle(method.invisibleAnnotations, annotationClasses);
    }

    /**
     * Get a runtime-visible annotation of the specified class from the supplied
     * class node
     *
     * @param classNode Source classNode
     * @param annotationClass Type of annotation to search for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getVisible(ClassNode classNode, Class<? extends Annotation> annotationClass) {
        return Annotations.get(classNode.visibleAnnotations, Type.getDescriptor(annotationClass));
    }

    /**
     * Get an invisible annotation of the specified class from the supplied
     * class node
     *
     * @param classNode Source classNode
     * @param annotationClass Type of annotation to search for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getInvisible(ClassNode classNode, Class<? extends Annotation> annotationClass) {
        return Annotations.get(classNode.invisibleAnnotations, Type.getDescriptor(annotationClass));
    }

    /**
     * Get a runtime-visible parameter annotation of the specified class from
     * the supplied method node
     *
     * @param method Source method
     * @param annotationClass Type of annotation to search for
     * @param paramIndex Index of the parameter to fetch annotation for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getVisibleParameter(MethodNode method, Class<? extends Annotation> annotationClass, int paramIndex) {
        return Annotations.getParameter(method.visibleParameterAnnotations, Type.getDescriptor(annotationClass), paramIndex);
    }

    /**
     * Get an invisible parameter annotation of the specified class from the
     * supplied method node
     *
     * @param method Source method
     * @param annotationClass Type of annotation to search for
     * @param paramIndex Index of the parameter to fetch annotation for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getInvisibleParameter(MethodNode method, Class<? extends Annotation> annotationClass, int paramIndex) {
        return Annotations.getParameter(method.invisibleParameterAnnotations, Type.getDescriptor(annotationClass), paramIndex);
    }

    /**
     * Get a parameter annotation of the specified class from the supplied
     * method node
     *
     * @param parameterAnnotations Annotations for the parameter
     * @param annotationType Type of annotation to search for
     * @param paramIndex Index of the parameter to fetch annotation for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getParameter(List<AnnotationNode>[] parameterAnnotations, String annotationType, int paramIndex) {
        if (parameterAnnotations == null || paramIndex < 0 || paramIndex >= parameterAnnotations.length) {
            return null;
        }
        
        return Annotations.get(parameterAnnotations[paramIndex], annotationType);
    }

    /**
     * Search for and return an annotation node matching the specified type
     * within the supplied
     * collection of annotation nodes
     *
     * @param annotations Haystack
     * @param annotationType Needle
     * @return matching annotation node or null if the annotation doesn't exist
     */
    public static AnnotationNode get(List<AnnotationNode> annotations, String annotationType) {
        if (annotations == null) {
            return null;
        }

        for (AnnotationNode annotation : annotations) {
            if (annotationType.equals(annotation.desc)) {
                return annotation;
            }
        }

        return null;
    }

    private static AnnotationNode getSingle(List<AnnotationNode> annotations, Class<? extends Annotation>[] annotationClasses) {
        List<AnnotationNode> nodes = new ArrayList<AnnotationNode>();
        for (Class<? extends Annotation> annotationClass : annotationClasses) {
            AnnotationNode annotation = Annotations.get(annotations, Type.getDescriptor(annotationClass));
            if (annotation != null) {
                nodes.add(annotation);
            }
        }
        
        int foundNodes = nodes.size();
        if (foundNodes > 1) {
            throw new IllegalArgumentException("Conflicting annotations found: " + Lists.transform(nodes, new Function<AnnotationNode, String>() {
                @Override public String apply(AnnotationNode input) {
                    return input.desc;
                }
            }));
        }
    
        return foundNodes == 0 ? null : nodes.get(0);
    }

    /**
     * Duck type the "value" entry (if any) of the specified annotation node
     *
     * @param <T> duck type
     * @param annotation Annotation node to query
     * @return duck-typed annotation value, null if missing, or inevitable
     *      {@link ClassCastException} if your duck is actually a rooster 
     */
    public static <T> T getValue(AnnotationNode annotation) {
        return Annotations.getValue(annotation, "value");
    }

    /**
     * Get the value of an annotation node and do pseudo-duck-typing via Java's
     * crappy generics
     *
     * @param <T> duck type
     * @param annotation Annotation node to query
     * @param key Key to search for
     * @param defaultValue Value to return if the specified key is not found or
     *      is null
     * @return duck-typed annotation value, null if missing, or inevitable
     *      {@link ClassCastException} if your duck is actually a rooster 
     */
    public static <T> T getValue(AnnotationNode annotation, String key, T defaultValue) {
        T returnValue = Annotations.getValue(annotation, key);
        return returnValue != null ? returnValue : defaultValue;
    }

    /**
     * Gets an annotation value or returns the default value of the annotation
     * if the annotation value is not present
     * 
     * @param <T> duck type
     * @param annotation Annotation node to query
     * @param key Key to search for
     * @param annotationClass Annotation class to query reflectively for the
     *      default value
     * @return Value of the specified annotation node, default value if not
     *      specified, or null if no value or default
     */
    @SuppressWarnings("unchecked")
    public static <T> T getValue(AnnotationNode annotation, String key, Class<?> annotationClass) {
        Preconditions.checkNotNull(annotationClass, "annotationClass cannot be null");
        T value = Annotations.getValue(annotation, key);
        if (value == null) {
            try {
                value = (T)annotationClass.getDeclaredMethod(key).getDefaultValue();
            } catch (NoSuchMethodException ex) {
                // Don't care
            }
        }
        return value;
    }

    /**
     * Get the value of an annotation node and do pseudo-duck-typing via Java's
     * crappy generics
     *
     * @param <T> duck type
     * @param annotation Annotation node to query
     * @param key Key to search for
     * @return duck-typed annotation value, null if missing, or inevitable
     *      {@link ClassCastException} if your duck is actually a rooster 
     */
    @SuppressWarnings("unchecked")
    public static <T> T getValue(AnnotationNode annotation, String key) {
        boolean getNextValue = false;

        if (annotation == null || annotation.values == null) {
            return null;
        }

        // Keys and value are stored in successive pairs, search for the key and if found return the following entry
        for (Object value : annotation.values) {
            if (getNextValue) {
                return (T) value;
            }
            if (value.equals(key)) {
                getNextValue = true;
            }
        }

        return null;
    }

    /**
     * Get the value of an annotation node as the specified enum, returns
     * defaultValue if the annotation value is not set
     *
     * @param <T> duck type
     * @param annotation Annotation node to query
     * @param key Key to search for
     * @param enumClass Class of enum containing the enum constant to search for
     * @param defaultValue Value to return if the specified key isn't found
     * @return duck-typed annotation value or defaultValue if missing
     */
    public static <T extends Enum<T>> T getValue(AnnotationNode annotation, String key, Class<T> enumClass, T defaultValue) {
        String[] value = Annotations.<String[]>getValue(annotation, key);
        if (value == null) {
            return defaultValue;
        }
        return Annotations.toEnumValue(enumClass, value);
    }

    /**
     * Return the specified annotation node value as a list of nodes
     * 
     * @param <T> list element type
     * @param annotation Annotation node to query
     * @param key Key to search for
     * @param notNull if true, return an empty list instead of null if the
     *      annotation value is absent
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> getValue(AnnotationNode annotation, String key, boolean notNull) {
        Object value = Annotations.<Object>getValue(annotation, key);
        if (value instanceof List) {
            return (List<T>)value;
        } else if (value != null) {
            List<T> list = new ArrayList<T>();
            list.add((T)value);
            return list;
        }
        return Collections.<T>emptyList();
    }
    
    /**
     * Return the specified annotation node value as a list of enums
     * 
     * @param <T> list element type
     * @param annotation Annotation node to query
     * @param key Key to search for
     * @param notNull if true, return an empty list instead of null if the
     *      annotation value is absent
     * @param enumClass Class of enum containing the enum constant to use
     */
    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> List<T> getValue(AnnotationNode annotation, String key, boolean notNull, Class<T> enumClass) {
        Object value = Annotations.<Object>getValue(annotation, key);
        if (value instanceof List) {
            for (ListIterator<Object> iter = ((List<Object>)value).listIterator(); iter.hasNext();) {
                iter.set(Annotations.toEnumValue(enumClass, (String[])iter.next()));
            }
            return (List<T>)value;
        } else if (value instanceof String[]) {
            List<T> list = new ArrayList<T>();
            list.add(Annotations.toEnumValue(enumClass, (String[])value));
            return list;
        }
        return Collections.<T>emptyList();
    }

    private static <T extends Enum<T>> T toEnumValue(Class<T> enumClass, String[] value) {
        if (!enumClass.getName().equals(Type.getType(value[0]).getClassName())) {
            throw new IllegalArgumentException("The supplied enum class does not match the stored enum value");
        }
        return Enum.valueOf(enumClass, value[1]);
    }

}
