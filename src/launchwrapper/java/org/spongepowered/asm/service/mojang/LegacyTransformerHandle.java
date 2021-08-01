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
package org.spongepowered.asm.service.mojang;

import java.lang.annotation.Annotation;

import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.ILegacyClassTransformer;
import org.spongepowered.asm.service.MixinService;

import net.minecraft.launchwrapper.IClassTransformer;

/**
 * A handle for a legacy {@link IClassTransformer} for processing as a legacy
 * transformer
 */
class LegacyTransformerHandle implements ILegacyClassTransformer {
    
    /**
     * Wrapped transformer
     */
    private final IClassTransformer transformer;
    
    LegacyTransformerHandle(IClassTransformer transformer) {
        this.transformer = transformer;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.ILegacyClassTransformer#getName()
     */
    @Override
    public String getName() {
        return this.transformer.getClass().getName();
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.ILegacyClassTransformer
     *      #isDelegationExcluded()
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean isDelegationExcluded() {
        try {
            IClassProvider classProvider = MixinService.getService().getClassProvider();
            Class<? extends Annotation> clResource = (Class<? extends Annotation>)classProvider.findClass("javax.annotation.Resource");
            return this.transformer.getClass().getAnnotation(clResource) != null;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.ILegacyClassTransformer
     *      #transformClassBytes(java.lang.String, java.lang.String, byte[])
     */
    @Override
    public byte[] transformClassBytes(String name, String transformedName, byte[] basicClass) {
        return this.transformer.transform(name, transformedName, basicClass);
    }
    
}
