/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered.org <http://www.spongepowered.org>
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

import java.util.SortedSet;

import org.spongepowered.asm.lib.ClassWriter;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.util.CheckClassAdapter;


/**
 * Mixin transformer module which runs CheckClassAdapter on the post-mixin
 * bytecode
 */
public class MixinTransformerModuleCheckClass implements IMixinTransformerModule {

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.IMixinTransformerModule
     *      #preTransform(java.lang.String, org.objectweb.asm.tree.ClassNode,
     *      java.util.SortedSet)
     */
    @Override
    public void preApply(String transformedName, ClassNode targetClass, SortedSet<MixinInfo> mixins) {
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.IMixinTransformerModule
     *      #postTransform(java.lang.String, org.objectweb.asm.tree.ClassNode,
     *      java.util.SortedSet)
     */
    @Override
    public void postApply(String transformedName, ClassNode targetClass, SortedSet<MixinInfo> mixins) {
        targetClass.accept(new CheckClassAdapter(new MixinClassWriter(ClassWriter.COMPUTE_FRAMES)));
    }

}
