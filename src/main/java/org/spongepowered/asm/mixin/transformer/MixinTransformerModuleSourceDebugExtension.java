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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.util.ASMHelper;

public class MixinTransformerModuleSourceDebugExtension implements IMixinTransformerModule {

    @Override
    public void preApply(TargetClassContext context) {
    }

    @Override
    public void postApply(TargetClassContext context) {
        ClassNode classNode = context.getClassNode();

        Multimap<String, String> mixins = ArrayListMultimap.create();

        for (MethodNode method : classNode.methods) {
            AnnotationNode merged = ASMHelper.getVisibleAnnotation(method, MixinMerged.class);
            if (merged == null) {
                continue;
            }

            String owner = ASMHelper.getAnnotationValue(merged, "mixin");
            mixins.put(owner, method.name + method.desc);
        }

        StringBuilder builder = new StringBuilder("MIXIN\n");

        for (String mixin : mixins.keySet()) {
            builder.append(mixin);

            for (String method : mixins.get(mixin)) {
                builder.append(' ').append(method);
            }

            builder.append('\n');
        }

        classNode.sourceDebug = builder.toString();
    }

}
