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
package org.spongepowered.asm.mixin.transformer;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.throwables.MixinError;
import org.spongepowered.asm.service.ISyntheticClassInfo;
import org.spongepowered.asm.service.ISyntheticClassRegistry;

/**
 * Implementation of synthetic class registry. This exists to maintain a list of
 * classes that the {@link MixinTransformer mixin pipeline} needs to accept in
 * order to generate. This used to be done on an on-demand basis under
 * LaunchWrapper but under ModLauncher we need to handle this more delicately
 * so this registry simply acts as a bridge between pipeline elements which need
 * to generate synthetic classes, and the entry point to the processing
 * pipeline.
 */
class SyntheticClassRegistry implements ISyntheticClassRegistry {
    
    /**
     * Map of class name to {@link ISyntheticClassInfo} structs
     */
    private final Map<String, ISyntheticClassInfo> classes = new HashMap<String, ISyntheticClassInfo>();

    SyntheticClassRegistry() {
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.ISyntheticClassRegistry
     *      #findSyntheticClass(java.lang.String)
     */
    @Override
    public ISyntheticClassInfo findSyntheticClass(String name) {
        if (name == null) {
            return null;
        }
        return this.classes.get(name.replace('.', '/'));
    }
    
    /**
     * Package-private
     */
    void registerSyntheticClass(ISyntheticClassInfo sci) {
        String name = sci.getName();
        ISyntheticClassInfo info = this.classes.get(name);
        if (info != null) {
            if (info == sci) {
                return;
            }
            throw new MixinError("Synthetic class with name " + name + " was already registered by " + info.getMixin()
                + ". Duplicate being registered by " + sci.getMixin());
        }
        this.classes.put(name, sci);
    }

}
