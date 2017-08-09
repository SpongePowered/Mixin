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
import org.spongepowered.asm.util.Constants;

import com.google.common.base.Objects;

/**
 * Stores information about a method mapping during AP runs
 */
public class MappingMethod implements IMapping<MappingMethod> {

    private final String owner;
    private final String name;
    private final String desc;

    public MappingMethod(String fullyQualifiedName, String desc) {
        this(MappingMethod.getOwnerFromName(fullyQualifiedName), MappingMethod.getBaseName(fullyQualifiedName), desc);
    }

    public MappingMethod(String owner, String simpleName, String desc) {
        this.owner = owner;
        this.name = simpleName;
        this.desc = desc;
    }
    
    @Override
    public Type getType() {
        return Type.METHOD;
    }
    
    @Override
    public String getName() {
        if (this.name == null) {
            return null;
        }
        return (this.owner != null ? this.owner + "/" : "") + this.name;
    }
    
    @Override
    public String getSimpleName() {
        return this.name;
    }
    
    @Override
    public String getOwner() {
        return this.owner;
    }
    
    @Override
    public String getDesc() {
        return this.desc;
    }
    
    @Override
    public MappingMethod getSuper() {
        return null;
    }
    
    public boolean isConstructor() {
        return Constants.CTOR.equals(this.name);
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
        return new MappingMethod(this.getOwner(), this.getSimpleName(), newDesc);
    }

    @Override
    public MappingMethod copy() {
        return new MappingMethod(this.getOwner(), this.getSimpleName(), this.getDesc());
    }
    
    /**
     * Return a clone of this mapping with the supplied prefix added. Returns
     * this object if the prefix matches the existing name.
     * 
     * @param prefix prefix to prepend
     * @return cloned mapping
     */
    public MappingMethod addPrefix(String prefix) {
        String simpleName = this.getSimpleName();
        if (simpleName == null || simpleName.startsWith(prefix)) {
            return this;
        }
        return new MappingMethod(this.getOwner(), prefix + simpleName, this.getDesc());
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
        String desc = this.getDesc();
        return String.format("%s%s%s", this.getName(), desc != null ? " " : "", desc != null ? desc : "");
    }
    
    private static String getBaseName(String name) {
        if (name == null) {
            return null;
        }
        int pos = name.lastIndexOf('/');
        return pos > -1 ? name.substring(pos + 1) : name;
    }

    private static String getOwnerFromName(String name) {
        if (name == null) {
            return null;
        }
        int pos = name.lastIndexOf('/');
        return pos > -1 ? name.substring(0, pos) : null;
    }

}
