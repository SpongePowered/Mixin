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

import java.util.List;

import org.objectweb.asm.Type;
import org.spongepowered.asm.util.Quantifier;
import org.spongepowered.asm.util.asm.IAnnotationHandle;

/**
 * A resolved descriptor, or rather the result of resolving a descriptor. If the
 * descriptor was not resolved then {@link #isResolved} returns <tt>false</tt>.
 */
public interface IResolvedDescriptor {
    
    /**
     * Get whether the descriptor was successfully resolved
     */
    public abstract boolean isResolved();

    /**
     * Get the resolved descriptor, or null if the descriptor was not resolved
     */
    public abstract IAnnotationHandle getAnnotation();

    /**
     * Get information about the resolution process (eg. visited implicit
     * coordinates) as a human-readable string for inclusion in error messages.
     */
    public abstract String getResolutionInfo();

    /**
     * Get the ID of the resolved descriptor, should match the query
     */
    public abstract String getId();

    /**
     * Get the owner from the resolved descriptor, returns <tt>void</tt> if the
     * descriptor was not resolved.
     */
    public abstract Type getOwner();

    /**
     * Get the name from the resolved descriptor, returns an empty string if the
     * descriptor was not resolved.
     */
    public abstract String getName();

    /**
     * Get the arguments from the resolved descriptor, returns an empty array if
     * the descriptor was not resolved.
     */
    public abstract Type[] getArgs();

    /**
     * Get the return type from the resolved descriptor, returns <tt>void</tt>
     * if the descriptor was not resolved.
     */
    public abstract Type getReturnType();
    
    /**
     * Get the specified matches values from the resolved descriptor.
     */
    public abstract Quantifier getMatches();
    
    /**
     * Get the values specified for next, if any
     */
    public abstract List<IAnnotationHandle> getNext();

}
