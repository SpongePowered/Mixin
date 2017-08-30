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
package org.spongepowered.asm.mixin.transformer.ext;

import org.spongepowered.asm.mixin.MixinEnvironment;

/**
 * Mixin Transformer extension interface for pre- and post-processors
 */
public interface IExtension {
    
    /**
     * Check whether this extension is active for the specified environment
     * 
     * @param environment current environment
     * @return true if the module should be active in the specified environment
     */
    public abstract boolean checkActive(MixinEnvironment environment);

    /**
     * Called before the mixins are applied
     * 
     * @param context Target class context
     */
    public abstract void preApply(ITargetClassContext context);

    /**
     * Called after the mixins are applied
     * 
     * @param context Target class context
     */
    public abstract void postApply(ITargetClassContext context);

    /**
     * Called when a class needs to be exported
     * 
     * @param env Environment
     * @param name Class name
     * @param force True to export even if the current environment settings
     *      would normally disable it
     * @param bytes Bytes to export
     */
    public abstract void export(MixinEnvironment env, String name, boolean force, byte[] bytes);

}
