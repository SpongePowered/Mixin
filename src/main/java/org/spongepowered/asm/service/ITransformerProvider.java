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
package org.spongepowered.asm.service;

import java.util.Collection;

/**
 * Interface for interacting with transforming classloaders. This service
 * component is entirely optional and services can elect to return <tt>null</tt>
 * if the platform does not support interacting with the transformer chain (or
 * doesn't <em>have</em> a transformer chain!)
 */
public interface ITransformerProvider {

    /**
     * Get currently available transformers in the environment
     */
    public abstract Collection<ITransformer> getTransformers();

    /**
     * Get currently available transformers in the environment
     */
    public abstract Collection<ITransformer> getDelegatedTransformers();
    
    /**
     * Adds a transformer to the transformer exclusions list
     * 
     * @param name Class transformer exclusion to add
     */
    public abstract void addTransformerExclusion(String name);

}
