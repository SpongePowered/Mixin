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
import com.google.common.base.Strings;

/**
 * Stores information about a field mapping during AP runs
 */
public class MappingField implements IMapping<MappingField> {

    private final String owner;
    
    private final String name;
    
    private final String desc;

    public MappingField(String owner, String name) {
        this(owner, name, null);
    }
    
    public MappingField(String owner, String name, String desc) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }

    @Override
    public Type getType() {
        return Type.FIELD;
    }

    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public final String getSimpleName() {
        return this.name;
    }
    
    @Override
    public final String getOwner() {
        return this.owner;
    }
    
    @Override
    public final String getDesc() {
        return this.desc;
    }
    
    @Override
    public MappingField getSuper() {
        return null;
    }
    
    @Override
    public MappingField move(String newOwner) {
        return new MappingField(newOwner, this.getName(), this.getDesc());
    }
    
    @Override
    public MappingField remap(String newName) {
        return new MappingField(this.getOwner(), newName, this.getDesc());
    }
    
    @Override
    public MappingField transform(String newDesc) {
        return new MappingField(this.getOwner(), this.getName(), newDesc);
    }

    @Override
    public MappingField copy() {
        return new MappingField(this.getOwner(), this.getName(), this.getDesc());
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(this.toString());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof MappingField) {
            return Objects.equal(this.toString(), ((MappingField)obj).toString());
        }
        return false;
    }
    
    @Override
    public String serialise() {
        return this.toString();
    }

    @Override
    public String toString() {
        return String.format("L%s;%s:%s", this.getOwner(), this.getName(), Strings.nullToEmpty(this.getDesc()));
    }
}