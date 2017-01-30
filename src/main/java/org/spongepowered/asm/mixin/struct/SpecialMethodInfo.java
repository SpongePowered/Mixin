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
package org.spongepowered.asm.mixin.struct;

import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.IInjectionPointContext;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

/**
 * Information about a special mixin method such as an injector or accessor
 */
public abstract class SpecialMethodInfo implements IInjectionPointContext {

    /**
     * Annotation on the method
     */
    protected final AnnotationNode annotation;
    
    /**
     * Class
     */
    protected final ClassNode classNode;
    
    /**
     * Annotated method
     */
    protected final MethodNode method;

    /**
     * Mixin data
     */
    protected final MixinTargetContext mixin;
    
    public SpecialMethodInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        this.mixin = mixin;
        this.method = method;
        this.annotation = annotation;
        this.classNode = mixin.getTargetClassNode();
    }
    
    /**
     * Get the mixin target context for this injection
     * 
     * @return the target context
     */
    @Override
    public final IMixinContext getContext() {
        return this.mixin;
    }
    
    /**
     * Get the annotation which this InjectionInfo was created from
     *  
     * @return The annotation which this InjectionInfo was created from 
     */
    @Override
    public final AnnotationNode getAnnotation() {
        return this.annotation;
    }

    /**
     * Get the class node for this injection
     * 
     * @return the class containing the injector and the target
     */
    public final ClassNode getClassNode() {
        return this.classNode;
    }

    /**
     * Get method being called
     * 
     * @return injector method
     */
    @Override
    public final MethodNode getMethod() {
        return this.method;
    }

}
