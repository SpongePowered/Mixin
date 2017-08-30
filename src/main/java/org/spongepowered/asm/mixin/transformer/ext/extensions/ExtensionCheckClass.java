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
package org.spongepowered.asm.mixin.transformer.ext.extensions;

import org.spongepowered.asm.lib.ClassWriter;
import org.spongepowered.asm.lib.util.CheckClassAdapter;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;
import org.spongepowered.asm.transformers.MixinClassWriter;

/**
 * Mixin transformer module which runs CheckClassAdapter on the post-mixin
 * bytecode
 */
public class ExtensionCheckClass implements IExtension {
    
    /**
     * Exception thrown when checkclass fails
     */
    public static class ValidationFailedException extends MixinException {

        private static final long serialVersionUID = 1L;

        public ValidationFailedException(String message, Throwable cause) {
            super(message, cause);
        }

        public ValidationFailedException(String message) {
            super(message);
        }

        public ValidationFailedException(Throwable cause) {
            super(cause);
        }
        
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.ext.IExtension#checkActive(
     *      org.spongepowered.asm.mixin.MixinEnvironment)
     */
    @Override
    public boolean checkActive(MixinEnvironment environment) {
        return environment.getOption(Option.DEBUG_VERIFY);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.IMixinTransformerModule
     *     #preApply(org.spongepowered.asm.mixin.transformer.TargetClassContext)
     */
    @Override
    public void preApply(ITargetClassContext context) {
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.IMixinTransformerModule
     *    #postApply(org.spongepowered.asm.mixin.transformer.TargetClassContext)
     */
    @Override
    public void postApply(ITargetClassContext context) {
        try {
            context.getClassNode().accept(new CheckClassAdapter(new MixinClassWriter(ClassWriter.COMPUTE_FRAMES)));
        } catch (RuntimeException ex) {
            throw new ValidationFailedException(ex.getMessage(), ex);
        }
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.ext.IExtension#export(
     *      org.spongepowered.asm.mixin.MixinEnvironment, java.lang.String,
     *      boolean, byte[])
     */
    @Override
    public void export(MixinEnvironment env, String name, boolean force, byte[] bytes) {
    }

}
