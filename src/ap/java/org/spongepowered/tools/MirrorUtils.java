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
package org.spongepowered.tools;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

/**
 * Convenience functions for mirror
 */
public abstract class MirrorUtils {

    private static final String OBJECT_SIG = "java.lang.Object";
    private static final String OBJECT_REF = "java/lang/Object";

    // No instances for you
    private MirrorUtils() {}

    public static AnnotationMirror getAnnotation(Element elem, Class<? extends Annotation> annotationClass) {
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

    @SuppressWarnings("unchecked")
    public static <T> T getAnnotationValue(AnnotationMirror annotation, String key, T defaultValue) {
        if (annotation == null) {
            return defaultValue;
        }
        
        AnnotationValue value = MirrorUtils.getAnnotationValue0(annotation, key);
        return value != null ? (T)value.getValue() : defaultValue;
    }

    public static <T> T getAnnotationValue(TypeElement elem, Class<? extends Annotation> annotationClass, String key, T defaultValue) {
        AnnotationMirror annotation = MirrorUtils.getAnnotation(elem, annotationClass);
        return MirrorUtils.getAnnotationValue(annotation, key, defaultValue);
    }
    
    public static <T> T getAnnotationValue(AnnotationMirror annotation) {
        return MirrorUtils.getAnnotationValue(annotation, "value", null);
    }
    
    public static <T> T getAnnotationValue(AnnotationMirror annotation, String key) {
        return MirrorUtils.getAnnotationValue(annotation, key, null);
    }
    
    public static <T> T getAnnotationValue(TypeElement elem, Class<? extends Annotation> annotationClass) {
        return MirrorUtils.getAnnotationValue(elem, annotationClass, "value");
    }
    
    public static <T> T getAnnotationValue(TypeElement elem, Class<? extends Annotation> annotationClass, String key) {
        AnnotationMirror annotation = MirrorUtils.getAnnotation(elem, annotationClass);
        return MirrorUtils.getAnnotationValue(annotation, key, null);
    }
    
    private static AnnotationValue getAnnotationValue0(AnnotationMirror annotation, String key) {
        for (ExecutableElement elem : annotation.getElementValues().keySet()) {
            if (elem.getSimpleName().contentEquals(key)) {
                return annotation.getElementValues().get(elem);
            }
        }
        
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> List<T> unfold(List<AnnotationValue> list) {
        if (list == null) {
            return Collections.<T>emptyList();
        }
        
        List<T> unfolded = new ArrayList<T>(list.size());
        for (AnnotationValue value : list) {
            unfolded.add((T)value.getValue());
        }
        
        return unfolded;
    }
    
    public static PackageElement getPackage(TypeElement elem) {
        Element parent = elem.getEnclosingElement();
        while (parent != null && !(parent instanceof PackageElement)) {
            parent = parent.getEnclosingElement();
        }
        return (PackageElement)parent;
    }

    public static String getElementType(Element parent) {
        if (parent instanceof TypeElement) {
            return "TypeElement";
        } else if (parent instanceof ExecutableElement) {
            return "ExecutableElement";
        } else if (parent instanceof VariableElement) {
            return "VariableElement";
        } else if (parent instanceof PackageElement) {
            return "PackageElement";
        } else if (parent instanceof TypeParameterElement) {
            return "TypeParameterElement";
        }
        
        return parent.getClass().getSimpleName();
    }
    
    public static String getJavaSignature(Element element) {
        if (element instanceof ExecutableElement) {
            ExecutableElement method = (ExecutableElement)element;
            StringBuilder desc = new StringBuilder().append("(");
            boolean extra = false;
            for (VariableElement arg : method.getParameters()) {
                if (extra) {
                    desc.append(',');
                }
                desc.append(MirrorUtils.getTypeName(arg.asType()));
                extra = true;
            }
            desc.append(')').append(method.getReturnType().toString());
            return desc.toString();
        }
        return MirrorUtils.getTypeName(element.asType());
    }

    public static String stripGenerics(String type) {
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

    public static String getTypeName(TypeMirror type) {
        switch (type.getKind()) {
            case ARRAY:    return MirrorUtils.getTypeName(((ArrayType)type).getComponentType()) + "[]";
            case DECLARED: return MirrorUtils.getTypeName((DeclaredType)type);
            case TYPEVAR:  return MirrorUtils.getTypeName(MirrorUtils.getUpperBound(type));
            case ERROR:    return MirrorUtils.OBJECT_SIG;
            default:       return type.toString();
        }
    }

    public static String getTypeName(DeclaredType type) {
        if (type == null) {
            return MirrorUtils.OBJECT_SIG;
        }
        return MirrorUtils.getTypeName((TypeElement)type.asElement());
    }

    private static String getTypeName(TypeElement elem) {
        return MirrorUtils.getInternalName(elem).replace('/', '.').replace('$', '.');
    }

    public static String generateSignature(ExecutableElement method) {
        StringBuilder signature = new StringBuilder();
        
        for (VariableElement var : method.getParameters()) {
            signature.append(MirrorUtils.getInternalName(var));
        }
        
        return String.format("(%s)%s", signature, MirrorUtils.getInternalName(method.getReturnType()));
    }
    
    public static String getInternalName(VariableElement var) {
        return MirrorUtils.getInternalName(var.asType());
    }
    
    public static String getInternalName(TypeMirror type) {
        switch (type.getKind()) {
            case ARRAY:    return "[" + MirrorUtils.getInternalName(((ArrayType)type).getComponentType());
            case DECLARED: return "L" + MirrorUtils.getInternalName((DeclaredType)type) + ";";
            case TYPEVAR:  return "L" + MirrorUtils.getInternalName(MirrorUtils.getUpperBound(type)) + ";";
            case BOOLEAN:  return "Z";
            case BYTE:     return "B";
            case CHAR:     return "C";
            case DOUBLE:   return "D";
            case FLOAT:    return "F";
            case INT:      return "I";
            case LONG:     return "J";
            case SHORT:    return "S";
            case VOID:     return "V";
            // TODO figure out a better way to not crash when we get here
            case ERROR:    return "L" + MirrorUtils.OBJECT_REF + ";";
            default:
        }

        throw new IllegalArgumentException("Unable to parse type symbol " + type + " with " + type.getKind() + " to equivalent bytecode type");
    }
    
    private static DeclaredType getUpperBound(TypeMirror type) {
        if (type instanceof TypeVariable) {
            try {
                return (DeclaredType)((TypeVariable)type).getUpperBound();
            } catch (Exception ex) {
                throw new IllegalArgumentException("Unable to compute upper bound of type symbol " + type);
            }
        }
        return null;
    }

    public static String getInternalName(DeclaredType type) {
        if (type == null) {
            return MirrorUtils.OBJECT_REF;
        }
        return MirrorUtils.getInternalName((TypeElement)type.asElement());
    }

    public static String getInternalName(TypeElement elem) {
        StringBuilder reference = new StringBuilder();
        reference.append(elem.getSimpleName());
        Element parent = elem.getEnclosingElement();
        while (parent != null) {
            if (parent instanceof TypeElement) {
                reference.insert(0, "$").insert(0, parent.getSimpleName());
            } else if (parent instanceof PackageElement) {
                reference.insert(0, "/").insert(0, ((PackageElement)parent).getQualifiedName().toString().replace('.', '/'));
            }
            parent = parent.getEnclosingElement();
        }
        return reference.toString();
    }

    public static boolean isAssignable(ProcessingEnvironment processingEnv, TypeMirror targetType, TypeMirror superClass) {
        boolean assignable = processingEnv.getTypeUtils().isAssignable(targetType, superClass);
        if (!assignable && targetType instanceof DeclaredType && superClass instanceof DeclaredType) {
            TypeMirror rawTargetType = MirrorUtils.toRawType(processingEnv, (DeclaredType)targetType);
            TypeMirror rawSuperType = MirrorUtils.toRawType(processingEnv, (DeclaredType)superClass);
            return processingEnv.getTypeUtils().isAssignable(rawTargetType, rawSuperType);
        }
        
        return assignable;
    }

    private static TypeMirror toRawType(ProcessingEnvironment processingEnv, DeclaredType targetType) {
        return processingEnv.getElementUtils().getTypeElement(((TypeElement)targetType.asElement()).getQualifiedName()).asType();
    }
    
}
