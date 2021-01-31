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
package org.spongepowered.asm.mixin.injection.selectors.dynamic;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Descriptors;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.asm.IAnnotatedElement;
import org.spongepowered.asm.util.asm.IAnnotationHandle;

import com.google.common.base.Strings;

/**
 * Utility class which contains the logic for resolving descriptors
 * ({@link Desc} annotations) starting from an element and recursing up the tree
 * of parent elements looking for a matching descriptor
 */
public final class DescriptorResolver {
    
    /**
     * Resolution result
     */
    public static final class Result {
        
        /**
         * The set of coordinates which were searched, stored here so they can
         * be included in error messages if resolution fails
         */
        private final Set<String> searched;
        
        /**
         * The resolved descriptor
         */
        private final IAnnotationHandle desc;

        Result(Set<String> searched, IAnnotationHandle desc) {
            this.searched = searched;
            this.desc = desc;
        }
        
        /**
         * True if the resolution process was successful
         */
        public boolean isResolved() {
            return this.desc != null;
        }
        
        /**
         * Gets the resolved descriptor annotation if successful, otherwise
         * returns null
         */
        public IAnnotationHandle getAnnotation() {
            return this.desc;
        }
        
        /**
         * Gets the set of coordinates which were searched during resolution
         */
        public Set<String> getSearched() {
            return this.searched;
        }
        
    }
    
    // No 
    private DescriptorResolver() {
    }

    /**
     * Resolve the specified descriptor id starting at the specified context
     * 
     * @param id Id to resolve, can be empty if resolving by coordinate
     * @param context Selector context, usually from the source annotation
     * @return Resolution result
     */
    public static Result resolve(String id, ISelectorContext context) {
        Set<String> searched = new LinkedHashSet<String>();
        if (!Strings.isNullOrEmpty(id)) {
            searched.add(id);
        }
        return DescriptorResolver.resolve(id, context, searched, context.getSelectorCoordinate(true));
    }
    
    /**
     * Recursive function which checks an element and then recurses to parent
     */
    private static Result resolve(String id, ISelectorContext context, Set<String> searched, String coordinate) {
        searched.add(coordinate);
        
        IAnnotationHandle annotation = Annotations.handleOf(context.getSelectorAnnotation());
        
        // First check if there's a "desc" value on the current annotation
        Result resolved = DescriptorResolver.resolve(id, context, searched, coordinate, annotation.getAnnotationList("desc"));
        if (resolved != null) {
            return resolved;
        }

        // Next check with the current coordinates on the owning method
        resolved = DescriptorResolver.resolve(id, context, searched, coordinate, context.getMethod());
        if (resolved != null) {
            return resolved;
        }

        ISelectorContext root = DescriptorResolver.getRoot(context);
        String rootCoordinate = root.getSelectorCoordinate(false);
        String mixinCoordinate = !rootCoordinate.equals(coordinate) ? rootCoordinate + "." + coordinate : coordinate;

        // Next check with the current coordinates on the mixin
        resolved = DescriptorResolver.resolve(id, context, searched, mixinCoordinate, context.getMixin());
        if (resolved != null) {
            return resolved;
        }

        // Otherwise compute the parent coordinates and then recurse into the parent
        ISelectorContext parent = context.getParent();
        if (parent != null) {
            String parentCoordinate = parent.getSelectorCoordinate(false) + "." + coordinate;
            return DescriptorResolver.resolve(id, parent, searched, parentCoordinate);
        }
        
        // If all else fails, return failure
        return new Result(searched, null);
    }

    /**
     * Attempt to resolve on an element
     */
    private static Result resolve(String id, ISelectorContext context, Set<String> searched, String coordinate, Object element) {
        searched.add(coordinate);
        
        IAnnotationHandle descriptors = DescriptorResolver.getVisibleAnnotation(element, Descriptors.class);
        if (descriptors != null) {
            Result resolved = DescriptorResolver.resolve(id, context, searched, coordinate, descriptors.getAnnotationList("value"));
            if (resolved != null) {
                return resolved;
            }
        }
        
        IAnnotationHandle descriptor = DescriptorResolver.getVisibleAnnotation(element, Desc.class);
        if (descriptor != null) {
            Result resolved = DescriptorResolver.resolve(id, context, searched, coordinate, descriptor);
            if (resolved != null) {
                return resolved;
            }
        }
        
        return null;
    }

    /**
     * Attempt resolution using annotations from a particular element
     */
    private static Result resolve(String id, ISelectorContext context, Set<String> searched, String coordinate,
            List<IAnnotationHandle> availableDescriptors) {
        if (availableDescriptors != null) {
            searched.add(coordinate);
            for (IAnnotationHandle desc : availableDescriptors) {
                Result resolved = DescriptorResolver.resolve(id, context, searched, coordinate, desc);
                if (resolved != null) {
                    return resolved;
                }
            }
        }
        return null;
    }

    /**
     * Attempt resolution using an actual annotation
     */
    private static Result resolve(String id, ISelectorContext context, Set<String> searched, String coordinate, IAnnotationHandle desc) {
        if (desc != null) {
            String descriptorId = desc.getValue("id", coordinate);
            boolean implicit = Strings.isNullOrEmpty(id);
            if ((implicit && descriptorId.equalsIgnoreCase(coordinate)) || (!implicit && descriptorId.equalsIgnoreCase(id))) {
                return new Result(searched, desc);
            }
        }
        return null;
    }
    
    /**
     * Utility function, gets an annotation handle from the candidate elements
     * we are able to process
     * 
     * @param element Element to fetch annotations for
     * @param annotationClass Annotation we're searching for
     * @return Annotation handle or null if the source is null
     * @throws IllegalStateException if element is of an unsupported type
     */
    private static IAnnotationHandle getVisibleAnnotation(Object element, Class<? extends Annotation> annotationClass) {
        if (element instanceof MethodNode) {
            return Annotations.handleOf(Annotations.getVisible((MethodNode)element, annotationClass));
        } else if (element instanceof ClassNode) {
            return Annotations.handleOf(Annotations.getVisible((ClassNode)element, annotationClass));
        } else if (element instanceof MixinTargetContext) {
            return Annotations.handleOf(Annotations.getVisible(((MixinTargetContext)element).getClassNode(), annotationClass));
        } else if (element instanceof IAnnotatedElement) {
            return ((IAnnotatedElement)element).getAnnotation(annotationClass);
        } else if (element == null) {
            return null;
        }
        throw new IllegalStateException("Cannot read visible annotations from element with unknown type: " + element.getClass().getName());
    }
    
    private static ISelectorContext getRoot(ISelectorContext context) {
        ISelectorContext parent = context.getParent();
        while (parent != null) {
            context = parent;
            parent = context.getParent();
        }
        return context;
    }

}
