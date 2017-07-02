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
package org.spongepowered.tools.obfuscation.mirror;

import org.spongepowered.asm.obfuscation.mapping.IMapping;

/**
 * Abstract base class for element handles
 * 
 * @param <T> mapping type
 */
public abstract class MemberHandle<T extends IMapping<T>> {
    
    private final String owner;
    
    private final String name;
    
    private final String desc;

    protected MemberHandle(String owner, String name, String desc) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }
    
    public final String getOwner() {
        return this.owner;
    }
    
    public final String getName() {
        return this.name;
    }
    
    public final String getDesc() {
        return this.desc;
    }

    /**
     * Get the visibility level for this member
     */
    public abstract Visibility getVisibility();

    /**
     * Return this handle as a mapping
     * 
     * @param includeOwner include the owner in the generated mapping, false to
     *      only include name and desc
     * @return this handle as a mapping of appropriate type
     */
    public abstract T asMapping(boolean includeOwner);
}
