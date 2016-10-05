package org.spongepowered.tools.obfuscation.model;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * Wrapper for {@link AnnotationMirror} with convenience functions
 */
public class AnnotationHandle {
    
    private final AnnotationMirror mirror;

    protected AnnotationHandle(AnnotationMirror mirror) {
        this.mirror = mirror;
    }

    public AnnotationMirror getMirror() {
        return this.mirror;
    }
    
    @Override
    public String toString() {
        return "@" + this.mirror.getAnnotationType().asElement().getSimpleName();
    }

    public <T> T getValue() {
        return AnnotationHandle.<T>getAnnotationValue(this.mirror, "value", null);
    }
    
    public <T> T getValue(String key) {
        return AnnotationHandle.<T>getAnnotationValue(this.mirror, key, null);
    }
    
    public <T> T getValue(String key, T defaultValue) {
        return AnnotationHandle.<T>getAnnotationValue(this.mirror, key, defaultValue);
    }
    
    public <T> List<T> getList() {
        return this.getList("value");
    }

    public <T> List<T> getList(String key) {
        return AnnotationHandle.<T>unfold(this.<List<AnnotationValue>>getValue(key, null));
    }
    
    public static AnnotationHandle of(ExecutableElement element, Class<? extends Annotation> annotationClass) {
        AnnotationMirror annotation = AnnotationHandle.getAnnotation(element, annotationClass);
        return annotation != null ? new AnnotationHandle(annotation) : null;
    }

    public static AnnotationHandle of(TypeElement element, Class<? extends Annotation> annotationClass) {
        AnnotationMirror annotation = AnnotationHandle.getAnnotation(element, annotationClass);
        return annotation != null ? new AnnotationHandle(annotation) : null;
    }

    public static AnnotationHandle of(Element element, Class<? extends Annotation> annotationClass) {
        AnnotationMirror annotation = AnnotationHandle.getAnnotation(element, annotationClass);
        return annotation != null ? new AnnotationHandle(annotation) : null;
    }

    private static AnnotationMirror getAnnotation(Element elem, Class<? extends Annotation> annotationClass) {
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
    private static <T> T getAnnotationValue(AnnotationMirror annotation, String key, T defaultValue) {
        if (annotation == null) {
            return defaultValue;
        }
        
        AnnotationValue value = AnnotationHandle.getAnnotationValue0(annotation, key);
        return value != null ? (T)value.getValue() : defaultValue;
    }

    public static <T> T getAnnotationValue(TypeElement elem, Class<? extends Annotation> annotationClass) {
        return AnnotationHandle.getAnnotationValue(elem, annotationClass, "value");
    }
    
    private static <T> T getAnnotationValue(TypeElement elem, Class<? extends Annotation> annotationClass, String key) {
        AnnotationMirror annotation = AnnotationHandle.getAnnotation(elem, annotationClass);
        return AnnotationHandle.getAnnotationValue(annotation, key, null);
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
    private static <T> List<T> unfold(List<AnnotationValue> list) {
        if (list == null) {
            return Collections.<T>emptyList();
        }
        
        List<T> unfolded = new ArrayList<T>(list.size());
        for (AnnotationValue value : list) {
            unfolded.add((T)value.getValue());
        }
        
        return unfolded;
    }
    
}
