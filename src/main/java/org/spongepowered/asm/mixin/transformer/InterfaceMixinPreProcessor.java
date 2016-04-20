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

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.FieldNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Method;

/**
 * Bytecode preprocessor for interface mixins, simply performs some additional
 * verification for things which are unsupported in interfaces
 */
class InterfaceMixinPreProcessor extends MixinPreProcessor {
    
    /**
     * Ctor
     * 
     * @param mixin Mixin info
     * @param classNode Mixin classnode
     */
    InterfaceMixinPreProcessor(MixinInfo mixin, ClassNode classNode) {
        super(mixin, classNode);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.MixinPreProcessor
     *      #prepareMethod(org.spongepowered.asm.lib.tree.MethodNode,
     *      org.spongepowered.asm.mixin.transformer.ClassInfo.Method)
     */
    @Override
    protected void prepareMethod(MethodNode mixinMethod, Method method) {
        // I have no idea how the hell you'd compile this, but we make sure anyway!
        if (!MixinApplicator.hasFlag(mixinMethod, Opcodes.ACC_PUBLIC)) {
            throw new InvalidInterfaceMixinException(this.mixin, "Interface mixin contains a non-public method! Found " + method + " in "
                    + this.mixin);
        }
        
        super.prepareMethod(mixinMethod, method);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.MixinPreProcessor
     *      #validateField(
     *      org.spongepowered.asm.mixin.transformer.MixinTargetContext,
     *      org.spongepowered.asm.lib.tree.FieldNode,
     *      org.spongepowered.asm.lib.tree.AnnotationNode)
     */
    @Override
    protected boolean validateField(MixinTargetContext context, FieldNode field, AnnotationNode shadow) {
        if (!MixinApplicator.hasFlag(field, Opcodes.ACC_STATIC)) {
            throw new InvalidInterfaceMixinException(this.mixin, "Interface mixin contains an instance field! Found " + field.name + " in "
                    + this.mixin);
        }
        
        return super.validateField(context, field, shadow);
    }
    
}
