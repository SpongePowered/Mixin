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
package org.spongepowered.asm.mixin.injection.struct;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.IInjectionPointContext;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.asm.IAnnotationHandle;

/**
 * Specialised SelectorAnnotationContext for injection points
 */
public class InjectionPointAnnotationContext extends SelectorAnnotationContext implements IInjectionPointContext {

    /**
     * Parent context
     */
    private IInjectionPointContext parentContext;

    public InjectionPointAnnotationContext(IInjectionPointContext parent, IAnnotationHandle selectorAnnotation, String selectorCoordinate) {
        super(parent, selectorAnnotation, selectorCoordinate);
        this.parentContext = parent;
    }

    public InjectionPointAnnotationContext(IInjectionPointContext parent, AnnotationNode selectorAnnotation, String selectorCoordinate) {
        super(parent, Annotations.handleOf(selectorAnnotation), selectorCoordinate);
        this.parentContext = parent;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.util.IMessageSink
     *      #addMessage(java.lang.String, java.lang.Object[])
     */
    @Override
    public void addMessage(String format, Object... args) {
        this.parentContext.addMessage(format, args);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.struct
     *      .SelectorAnnotationContext#getMethod()
     */
    @Override
    public MethodNode getMethod() {
        return this.parentContext.getMethod();
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.IInjectionPointContext
     *      #getAnnotationNode()
     */
    @Override
    public AnnotationNode getAnnotationNode() {
        return this.parentContext.getAnnotationNode();
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.struct
     *      .SelectorAnnotationContext#getAnnotation()
     */
    @Override
    public IAnnotationHandle getAnnotation() {
        return this.parentContext.getAnnotation();
    }
    
    @Override
    public String toString() {
        return String.format("%s->%s(%s)", this.parentContext, this.getSelectorAnnotation(), 
                this.getSelectorCoordinate(false));
    }

}
