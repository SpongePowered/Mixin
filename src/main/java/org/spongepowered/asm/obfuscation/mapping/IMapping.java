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
package org.spongepowered.asm.obfuscation.mapping;

/**
 * Base class for member mapping entries
 * 
 * @param <TMapping> type of this member, for transformation method return types
 */
public interface IMapping<TMapping> {

    /**
     * Type of mapping
     */
    public enum Type {
        FIELD,
        METHOD,
        CLASS,
        PACKAGE
    }
    
    public abstract Type getType();
    
    public abstract TMapping move(String newOwner);

    public abstract TMapping remap(String newName);
    
    public abstract TMapping transform(String newDesc);
    
    public abstract TMapping copy();

    public abstract String getName();

    public abstract String getSimpleName();

    public abstract String getOwner();

    public abstract String getDesc();

    public abstract String serialise();
    
}
