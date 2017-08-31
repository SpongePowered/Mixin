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

/**
 * Adapter interface for legacy class transformers. Legacy class transformers
 * operate on raw byte arrays.
 */
public interface ILegacyClassTransformer extends ITransformer {
    
    /**
     * Get the identifier for this transformer, usually the class name but for
     * wrapped transformers this is the class name of the wrapped transformer
     * 
     * @return transformer's identifying name
     */
    public abstract String getName();
    
    /**
     * Get whether this transformer is excluded from delegation. Some
     * transformers (such as the mixin transformer itself) should not be
     * included in the delegation list because they are re-entrant or do not
     * need to run on incoming bytecode.
     * 
     * @return true if this transformer should be <em>excluded</em> from the
     *      transformer delegation list
     */
    public abstract boolean isDelegationExcluded();

    /**
     * Transform a class in byte array form.
     * 
     * @param name Class original name
     * @param transformedName Class name after being processed by the class name
     *      transformer
     * @param basicClass class byte array
     * @return transformed bytes
     */
    public abstract byte[] transformClassBytes(String name, String transformedName, byte[] basicClass);

}
