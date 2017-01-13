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

import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;

/**
 * Couples {@link MethodSlice method slices} to a {@link Target} for injection
 * purposes.
 */
public class InjectorTarget {

    /**
     * Owner injector
     */
    private final InjectionInfo info;

    /**
     * Cache of slices
     */
    private final Map<String, ReadOnlyInsnList> cache = new HashMap<String, ReadOnlyInsnList>();

    /**
     * Target method data
     */
    private final Target target;

    /**
     * ctor
     * 
     * @param info owner
     * @param target target
     */
    public InjectorTarget(InjectionInfo info, Target target) {
        this.info = info;
        this.target = target;
    }
    
    /**
     * Get the target reference
     */
    public Target getTarget() {
        return this.target;
    }
    
    /**
     * Get the target method
     */
    public MethodNode getMethod() {
        return this.target.method;
    }
    
    /**
     * Get the slice instructions for the specified slice id
     * 
     * @param id slice id
     * @return insn slice
     */
    public InsnList getSlice(String id) {
        ReadOnlyInsnList slice = this.cache.get(id);
        if (slice == null) {
            MethodSlice sliceInfo = this.info.getSlice(id);
            if (sliceInfo != null) {
                slice = sliceInfo.getSlice(this.target.method);
            } else {
                // No slice exists so just wrap the method insns
                slice = new ReadOnlyInsnList(this.target.method.instructions);
            }
            this.cache.put(id, slice);
        }
        
        return slice;
    }

    /**
     * Get the slice instructions for the specified 
     * 
     * @param injectionPoint
     * @return
     */
    public InsnList getSlice(InjectionPoint injectionPoint) {
        return this.getSlice(injectionPoint.getSlice());
    }
    
    public void dispose() {
        for (ReadOnlyInsnList insns : this.cache.values()) {
            insns.dispose();
        }
        
        this.cache.clear();
    }
}

