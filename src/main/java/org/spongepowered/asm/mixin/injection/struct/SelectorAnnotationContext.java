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

import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.util.asm.IAnnotationHandle;

/**
 * Narrowed context for selectors in annotations which are themselves inside
 * a parent annotation, for example &#064;At annotations within a parent
 * &#064;Inject annotation.
 */
public class SelectorAnnotationContext implements ISelectorContext {
    
    /**
     * The selector context of the parent, eg. the &#064;Inject context
     */
    private final ISelectorContext parent;
    
    /**
     * The annotation for this selector
     */
    private final IAnnotationHandle selectorAnnotation;
    
    /**
     * The coordinate for this selector
     */
    private final String selectorCoordinate;
    
    /**
     * Create a specialised context for a child annotation
     * 
     * @param parent Parent selector context, eg. context of the outer element
     *      such as &#064;Inject
     * @param selectorAnnotation Annotation for this selector, eg. &#064;At
     * @param selectorCoordinate Coordinate for this selector
     */
    public SelectorAnnotationContext(ISelectorContext parent, IAnnotationHandle selectorAnnotation, String selectorCoordinate) {
        this.parent = parent;
        this.selectorAnnotation = selectorAnnotation;
        this.selectorCoordinate = selectorCoordinate;
    }
    
    /**
     * Get the parent of this selector, must not return null
     * 
     * @return parent context
     */
    @Override
    public ISelectorContext getParent() {
        return this.parent;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ISelectorContext
     *      #getMixin()
     */
    @Override
    public IMixinContext getMixin() {
        return this.parent.getMixin();
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ISelectorContext
     *      #getMethod()
     */
    @Override
    public Object getMethod() {
        return this.parent.getMethod();
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ISelectorContext
     *      #getAnnotation()
     */
    @Override
    public IAnnotationHandle getAnnotation() {
        return this.parent.getAnnotation();
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ISelectorContext
     *      #getSelectorAnnotation()
     */
    @Override
    public IAnnotationHandle getSelectorAnnotation() {
        return this.selectorAnnotation;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ISelectorContext
     *      #getSelectorCoordinate()
     */
    @Override
    public String getSelectorCoordinate(boolean leaf) {
        return this.selectorCoordinate;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ISelectorContext
     *      #remap(java.lang.String)
     */
    @Override
    public String remap(String reference) {
        return this.parent.remap(reference);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ISelectorContext
     *      #getElementDescription()
     */
    @Override
    public String getElementDescription() {
        return String.format("%s in %s", this.selectorAnnotation, this.parent.getElementDescription());
    }

}
