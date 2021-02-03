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
package org.spongepowered.asm.mixin.injection.selectors;

import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.util.asm.IAnnotationHandle;

/**
 * Context passed to a {@link ITargetSelector Target Selector} in order to
 * support context-sensitive behaviour for the selector. For example obtaining
 * the name of the source mixin, or the element containing the selector itself.
 */
public interface ISelectorContext {

    /**
     * Get the parent (outer/containing) context. Can be null if this is a root
     * element such as an injector method, in which case the parent is the mixin
     * instead.
     */
    public abstract ISelectorContext getParent();

    /**
     * Get the mixin containing this selector.
     */
    public abstract IMixinContext getMixin();
    
    /**
     * Get the root method upon which this selector is operating, usually the
     * injector method 
     */
    public abstract Object getMethod();
    
    /**
     * Get the root annotation for the selector, for example the injector or
     * accessor annotation
     */
    public abstract IAnnotationHandle getAnnotation();

    /**
     * Get the annotation for this selector, for example the &#064;At annotation
     */
    public abstract IAnnotationHandle getSelectorAnnotation();
    
    /**
     * Get the coordinate for this selector. This is the local coordinate of the
     * selector without any parent parts. Parent parts are prepended by
     * consumers during the resolve process. 
     * 
     * @param leaf True if getting the coordinate when this element is a leaf,
     *      false if getting the coordinate when it is a parent
     */
    public abstract String getSelectorCoordinate(boolean leaf);

    /**
     * Remap a reference in the context of this selector, usually via the local
     * refmap of the mixin configuration but can be overridden as needed, for 
     * example to provide selector-local remap behaviour.
     * 
     * @param reference Reference to remap
     * @return Remapped reference or original reference if not remapped, must
     *      not return null!
     */
    public abstract String remap(String reference);

}
