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

import java.util.List;

import org.spongepowered.asm.service.ISyntheticClassRegistry;

/**
 * Interface for the mixin Extensions registry
 */
public interface IExtensionRegistry {

    /**
     * Get all extensions
     */
    public abstract List<IExtension> getExtensions();

    /**
     * Get all active extensions
     */
    public abstract List<IExtension> getActiveExtensions();

    /**
     * Get a specific extension
     * 
     * @param extensionClass extension class to look up
     * @param <T> extension type
     * @return extension instance or null
     */
    public abstract <T extends IExtension> T getExtension(Class<? extends IExtension> extensionClass);

    /**
     * Get the synthetic class registry
     */
    public abstract ISyntheticClassRegistry getSyntheticClassRegistry();

}
