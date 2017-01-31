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
package org.spongepowered.asm.obfuscation;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.extensibility.IRemapper;

/**
 * Mixin environment remapper chain. Contains all remappers for the current
 * environment to facilitate remapping via all registered remappers.
 */
public class RemapperChain implements IRemapper {
    
    private final List<IRemapper> remappers = new ArrayList<IRemapper>();
    
    @Override
    public String toString() {
        return String.format("RemapperChain[%d]", this.remappers.size());
    }
    
    /**
     * Add a new remapper to this chain
     * 
     * @param remapper remapper to add
     * @return fluent interface
     */
    public RemapperChain add(IRemapper remapper) {
        this.remappers.add(remapper);
        return this;
    }

    @Override
    public String mapMethodName(String owner, String name, String desc) {
        for (IRemapper remapper : this.remappers) {
            String newName = remapper.mapMethodName(owner, name, desc);
            if (newName != null && !newName.equals(name)) {
                name = newName;
            }
        }
        return name;
    }

    @Override
    public String mapFieldName(String owner, String name, String desc) {
        for (IRemapper remapper : this.remappers) {
            String newName = remapper.mapFieldName(owner, name, desc);
            if (newName != null && !newName.equals(name)) {
                name = newName;
            }
        }
        return name;
    }

    @Override
    public String map(String typeName) {
        for (IRemapper remapper : this.remappers) {
            String newName = remapper.map(typeName);
            if (newName != null && !newName.equals(typeName)) {
                typeName = newName;
            }
        }
        return typeName;
    }
    
    @Override
    public String unmap(String typeName) {
        for (IRemapper remapper : this.remappers) {
            String newName = remapper.unmap(typeName);
            if (newName != null && !newName.equals(typeName)) {
                typeName = newName;
            }
        }
        return typeName;
    }
    
    @Override
    public String mapDesc(String desc) {
        for (IRemapper remapper : this.remappers) {
            String newDesc = remapper.mapDesc(desc);
            if (newDesc != null && !newDesc.equals(desc)) {
                desc = newDesc;
            }
        }
        return desc;
    }
    
    @Override
    public String unmapDesc(String desc) {
        for (IRemapper remapper : this.remappers) {
            String newDesc = remapper.unmapDesc(desc);
            if (newDesc != null && !newDesc.equals(desc)) {
                desc = newDesc;
            }
        }
        return desc;
    }
}
