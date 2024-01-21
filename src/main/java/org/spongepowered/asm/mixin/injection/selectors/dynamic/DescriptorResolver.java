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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Descriptors;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.asm.util.Quantifier;
import org.spongepowered.asm.util.asm.IAnnotatedElement;
import org.spongepowered.asm.util.asm.IAnnotationHandle;

import com.google.common.base.Joiner;
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
    static final class Descriptor implements IResolvedDescriptor {
        
        /**
         * The set of coordinates which were searched, stored here so they can
         * be included in error messages if resolution fails
         */
        private final Set<String> searched;
        
        /**
         * The resolved descriptor
         */
        private final IAnnotationHandle desc;
        
        /**
         * Selector context
         */
        private final ISelectorContext context;
        
        /**
         * True if this is a debug descriptor 
         */
        private final boolean debug;
        
        Descriptor(Set<String> searched, IAnnotationHandle desc, ISelectorContext context) {
            this(searched, desc, context, false);
        }

        Descriptor(Set<String> searched, IAnnotationHandle desc, ISelectorContext context, boolean debug) {
            this.searched = searched;
            this.desc = desc;
            this.context = context;
            this.debug = debug;
        }
        
        /**
         * True if the resolution process was successful
         */
        @Override
        public boolean isResolved() {
            return this.desc != null;
        }
        
        /**
         * True if this is a debugging descriptor and shouldn't be treated as
         * fully resolved
         */
        @Override
        public boolean isDebug() {
            return this.debug;
        }
        
        /**
         * Get information about the resolution of this descriptor, for
         * inclusion in error messages when isResolved is false
         * 
         */
        @Override
        public String getResolutionInfo() {
            if (this.searched == null) {
                return "";
            }
            return String.format("Searched coordinates [ \"%s\" ]", Joiner.on("\", \"").join(this.searched));
        }
        
        /**
         * Gets the resolved descriptor annotation if successful, otherwise
         * returns null
         */
        @Override
        public IAnnotationHandle getAnnotation() {
            return this.desc;
        }
        
        @Override
        public String getId() {
            return this.desc != null ? this.desc.<String>getValue("id", "") : "";
        }

        @Override
        public Type getOwner() {
            if (this.desc == null) {
                return Type.VOID_TYPE;
            }
            Type ownerClass = this.desc.getTypeValue("owner");
            if (ownerClass != Type.VOID_TYPE) {
                return ownerClass;
            }
            return this.context != null ? Type.getObjectType(this.context.getMixin().getTargetClassRef()) : ownerClass;
        }

        @Override
        public String getName() {
            if (this.desc == null) {
                return "";
            }
            String value = this.desc.<String>getValue("value", "");
            if (!value.isEmpty()) {
                return value;
            }
            return this.desc.<String>getValue("name", "");
        }

        @Override
        public Type[] getArgs() {
            if (this.desc == null) {
                return new Type[0];
            }            
            List<Type> args = this.desc.getTypeList("args");
            return args.toArray(new Type[args.size()]);
        }

        @Override
        public Type getReturnType() {
            if (this.desc == null) {
                return Type.VOID_TYPE;
            }
            return this.desc.getTypeValue("ret");
        }
        
        @Override
        public Quantifier getMatches() {
            if (this.desc == null) {
                return Quantifier.DEFAULT;
            }
            
            int min = Math.max(0, this.desc != null ? this.desc.<Integer>getValue("min", 0) : 0);
            Integer max = this.desc != null ? this.desc.<Integer>getValue("max", null) : null;
            return new Quantifier(min, max != null ? (max > 0 ? max : Integer.MAX_VALUE) : -1);
        }
        
        @Override
        public List<IAnnotationHandle> getNext() {
            return this.desc != null ? this.desc.getAnnotationList("next") : Collections.<IAnnotationHandle>emptyList();
        }
        
    }
    
    /**
     * Observer for the resolution process, used to support printing resolution
     * path during debug or collecting resolution info for inclusion in error
     * messages
     */
    interface IResolverObserver {
        
        /**
         * Called when the resolver visits a coordinate on the specified member
         * 
         * @param coordinate coordinate being visited
         * @param element element being visited
         * @param detail diagnostic detail
         */
        public abstract void visit(String coordinate, Object element, String detail);

        /**
         * Get all of the searched coordinates, for inclusion in error messages
         */
        public abstract Set<String> getSearched();
        
        /**
         * Called after resolution completes
         */
        public abstract void postResolve();
        
    }
    
    /**
     * Basic resolver observer which just collects searched coordinates
     */
    static class ResolverObserverBasic implements IResolverObserver {
        
        private final Set<String> searched = new LinkedHashSet<String>();
        
        @Override
        public void visit(String coordinate, Object element, String detail) {
            this.searched.add(coordinate);
        }

        @Override
        public Set<String> getSearched() {
            return this.searched;
        }
        
        @Override
        public void postResolve() {
        }
        
    }

    /**
     * Debug resolver observer which prints resolution debug info after
     * resolution is complete
     */
    static class ResolverObserverDebug extends ResolverObserverBasic {
        
        private final PrettyPrinter printer = new PrettyPrinter();
        
        ResolverObserverDebug(ISelectorContext context) {
            this.printer.add("Searching for implicit descriptor").add(context).hr().table();
            this.printer.tr("Context Coordinate:", context.getSelectorCoordinate(true) + " (" + context.getSelectorCoordinate(false) + ")");
            this.printer.tr("Selector Annotation:", context.getSelectorAnnotation());
            this.printer.tr("Root Annotation:", context.getAnnotation());
            this.printer.tr("Method:", context.getMethod()).hr();
            this.printer.table("Search Coordinate", "Search Element", "Detail").th().hr();
        }
        
        @Override
        public void visit(String coordinate, Object element, String detail) {
            super.visit(coordinate, element, detail);
            this.printer.tr(coordinate, element, detail);
        }

        @Override
        public void postResolve() {
            this.printer.print();
        }
        
    }        

    /**
     * Special ID used to instruct the resolver to print its progress.
     */
    public static String PRINT_ID = "?";
    
    // No 
    private DescriptorResolver() {
    }
    
    public static IResolvedDescriptor resolve(IAnnotationHandle desc, ISelectorContext context) {
        return new Descriptor(Collections.<String>emptySet(), desc, context);
    }

    /**
     * Resolve the specified descriptor id starting at the specified context
     * 
     * @param id Id to resolve, can be empty if resolving by coordinate
     * @param context Selector context, usually from the source annotation
     * @return Resolution result
     */
    public static IResolvedDescriptor resolve(String id, ISelectorContext context) {
        boolean debug = false;
        IResolverObserver observer = new ResolverObserverBasic();
        if (!Strings.isNullOrEmpty(id)) {
            if (DescriptorResolver.PRINT_ID.equals(id)) {
                observer = new ResolverObserverDebug(context);
                id = "";
                debug = true;
            } else {
                observer.visit(id, "", "");
            }
        }
        
        IAnnotationHandle desc = DescriptorResolver.resolve(id, context, observer, context.getSelectorCoordinate(true));
        observer.postResolve();
        return new Descriptor(observer.getSearched(), desc, context, debug);
    }
    
    /**
     * Recursive function which checks an element and then recurses to parent
     */
    private static IAnnotationHandle resolve(String id, ISelectorContext context, IResolverObserver observer, String coordinate) {
        IAnnotationHandle annotation = Annotations.handleOf(context.getSelectorAnnotation());
        observer.visit(coordinate, annotation, annotation.toString() + ".desc");
        
        // First check if there's a "desc" value on the current annotation
        IAnnotationHandle resolved = DescriptorResolver.resolve(id, context, observer, coordinate, annotation.getAnnotationList("desc"));
        if (resolved != null) {
            return resolved;
        }

        // Next check with the current coordinates on the owning method
        resolved = DescriptorResolver.resolve(id, context, observer, coordinate, context.getMethod(), "method");
        if (resolved != null) {
            return resolved;
        }

        ISelectorContext root = DescriptorResolver.getRoot(context);
        String rootCoordinate = root.getSelectorCoordinate(false);
        String mixinCoordinate = (root != context || !coordinate.contains(".")) && !rootCoordinate.equals(coordinate)
                ? rootCoordinate + "." + coordinate : coordinate;

        // Next check with the current coordinates on the mixin
        resolved = DescriptorResolver.resolve(id, context, observer, mixinCoordinate, context.getMixin(), "mixin");
        if (resolved != null) {
            return resolved;
        }

        // Otherwise compute the parent coordinates and then recurse into the parent
        ISelectorContext parent = context.getParent();
        if (parent != null) {
            String parentCoordinate = parent.getSelectorCoordinate(false) + "." + coordinate;
            return DescriptorResolver.resolve(id, parent, observer, parentCoordinate);
        }
        
        // If all else fails, return failure
        return null;
    }

    /**
     * Attempt to resolve on an element
     */
    private static IAnnotationHandle resolve(String id, ISelectorContext context, IResolverObserver observer, String coordinate, Object element,
            String detail) {
        observer.visit(coordinate, element, detail);
        
        IAnnotationHandle descriptors = DescriptorResolver.getVisibleAnnotation(element, Descriptors.class);
        if (descriptors != null) {
            IAnnotationHandle resolved = DescriptorResolver.resolve(id, context, observer, coordinate, descriptors.getAnnotationList("value"));
            if (resolved != null) {
                return resolved;
            }
        }
        
        IAnnotationHandle descriptor = DescriptorResolver.getVisibleAnnotation(element, Desc.class);
        if (descriptor != null) {
            IAnnotationHandle resolved = DescriptorResolver.resolve(id, context, observer, coordinate, descriptor);
            if (resolved != null) {
                return resolved;
            }
        }
        
        return null;
    }

    /**
     * Attempt resolution using annotations from a particular element
     */
    private static IAnnotationHandle resolve(String id, ISelectorContext context, IResolverObserver observer, String coordinate,
            List<IAnnotationHandle> availableDescriptors) {
        if (availableDescriptors != null) {
            for (IAnnotationHandle desc : availableDescriptors) {
                IAnnotationHandle resolved = DescriptorResolver.resolve(id, context, observer, coordinate, desc);
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
    private static IAnnotationHandle resolve(String id, ISelectorContext context, IResolverObserver observer, String coordinate,
            IAnnotationHandle desc) {
        if (desc != null) {
            String descriptorId = desc.getValue("id", coordinate);
            boolean implicit = Strings.isNullOrEmpty(id);
            if ((implicit && descriptorId.equalsIgnoreCase(coordinate)) || (!implicit && descriptorId.equalsIgnoreCase(id))) {
                return desc;
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
            IAnnotationHandle annotation = ((IAnnotatedElement)element).getAnnotation(annotationClass);
            return annotation != null && annotation.exists() ? annotation : null;
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
