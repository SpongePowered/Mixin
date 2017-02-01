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
package org.spongepowered.asm.mixin.injection.code;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.throwables.InvalidSliceException;
import org.spongepowered.asm.util.Annotations;

/**
 * Represents a collection of {@link MethodSlice}s, mapped by ID. Stored ids may
 * be different to declared slice ids because they are mapped by the underlying
 * injector. Some injectors only support a single slice.
 */
public final class MethodSlices {
    
    /**
     * Injector which owns this collection of slices
     */
    private final InjectionInfo info;
    
    /**
     * Available slices
     */
    private final Map<String, MethodSlice> slices = new HashMap<String, MethodSlice>(4);
    
    /**
     * ctor
     * 
     * @param info owner
     */
    private MethodSlices(InjectionInfo info) {
        this.info = info;
    }

    /**
     * Add a slice to this collection
     * 
     * @param slice slice to add
     */
    private void add(MethodSlice slice) {
        String id = this.info.getSliceId(slice.getId());
        if (this.slices.containsKey(id)) {
            throw new InvalidSliceException(this.info, slice + " has a duplicate id, '" + id + "' was already defined");
        }
        this.slices.put(id, slice);
    }
    
    /**
     * Fetch the slice with the specified id, returns null if no slice with the
     * supplied id is available
     * 
     * @param id slice id
     * @return matching slice or null
     */
    public MethodSlice get(String id) {
        return this.slices.get(id);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("MethodSlices%s", this.slices.keySet());
    }

    /**
     * Parse a collection of slices from the supplied injector
     * 
     * @param info owning injector
     * @return parsed slice collection
     */
    public static MethodSlices parse(InjectionInfo info) {
        MethodSlices slices = new MethodSlices(info);
        
        AnnotationNode annotation = info.getAnnotation();
        if (annotation != null) {
            for (AnnotationNode node : Annotations.<AnnotationNode>getValue(annotation, "slice", true)) {
                MethodSlice slice = MethodSlice.parse(info, node);
                slices.add(slice);
            }
        }
        
        return slices;
    }
    
}
