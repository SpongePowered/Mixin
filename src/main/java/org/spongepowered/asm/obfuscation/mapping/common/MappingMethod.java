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
package org.spongepowered.asm.obfuscation.mapping.common;

import org.spongepowered.asm.obfuscation.mapping.IMapping;

import com.google.common.base.Objects;

/**
 * Stores information about a method mapping during AP runs
 */
public class MappingMethod implements IMapping<MappingMethod> {

    private final String name;
    private final String desc;

    public MappingMethod(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }
    
    public MappingMethod(String owner, String simpleName, String desc) {
        this.name = MappingMethod.createName(owner, simpleName);
        this.desc = desc;
    }
    
    @Override
    public Type getType() {
        return Type.METHOD;
    }
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public String getSimpleName() {
        if (this.name == null) {
            return null;
        }
        int pos = this.name.lastIndexOf('/');
        return pos > -1 ? this.name.substring(pos + 1) : this.name;
    }
    
    @Override
    public String getOwner() {
        if (this.name == null) {
            return null;
        }
        int pos = this.name.lastIndexOf('/');
        return pos > -1 ? this.name.substring(0, pos) : null;
    }
    
    @Override
    public String getDesc() {
        return this.desc;
    }
    
    @Override
    public MappingMethod move(String newOwner) {
        return new MappingMethod(newOwner, this.getSimpleName(), this.getDesc());
    }
    
    @Override
    public MappingMethod remap(String newName) {
        return new MappingMethod(this.getOwner(), newName, this.getDesc());
    }
    
    @Override
    public MappingMethod transform(String newDesc) {
        return new MappingMethod(this.getName(), newDesc);
    }

    @Override
    public MappingMethod copy() {
        return new MappingMethod(this.getName(), this.getDesc());
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(this.getName(), this.getDesc());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof MappingMethod) {
            return Objects.equal(this.name, ((MappingMethod)obj).name) && Objects.equal(this.desc, ((MappingMethod)obj).desc);
        }
        return false;
    }
    
    @Override
    public String serialise() {
        return this.toString();
    }

    @Override
    public String toString() {
        return String.format("%s %s", this.name, this.desc);
    }
    
    private static String createName(String owner, String simpleName) {
        return (owner != null ? owner + "/" : "") + simpleName;
    }

}
