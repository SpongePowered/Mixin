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
package org.spongepowered.asm.bridge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.extensibility.IRemapper;
import org.spongepowered.asm.util.ObfuscationUtil;
import org.spongepowered.asm.util.ObfuscationUtil.IClassRemapper;

/**
 * Base impl of remapper
 */
public abstract class RemapperAdapter implements IRemapper, IClassRemapper {

    protected final Logger logger = LogManager.getLogger("mixin");
    protected final org.objectweb.asm.commons.Remapper remapper;
    
    public RemapperAdapter(org.objectweb.asm.commons.Remapper remapper) {
        this.remapper = remapper;
    }
    
    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String mapMethodName(String owner, String name, String desc) {
        this.logger.debug("{} is remapping method {}{} for {}", this, name, desc, owner);
        String newName = this.remapper.mapMethodName(owner, name, desc);
        if (!newName.equals(name)) {
            return newName;
        }
        String obfOwner = this.unmap(owner);
        String obfDesc = this.unmapDesc(desc);
        this.logger.debug("{} is remapping obfuscated method {}{} for {}", this, name, obfDesc, obfOwner);
        return this.remapper.mapMethodName(obfOwner, name, obfDesc);
    }

    @Override
    public String mapFieldName(String owner, String name, String desc) {
        this.logger.debug("{} is remapping field {}{} for {}", this, name, desc, owner);
        String newName = this.remapper.mapFieldName(owner, name, desc);
        if (!newName.equals(name)) {
            return newName;
        }
        String obfOwner = this.unmap(owner);
        String obfDesc = this.unmapDesc(desc);
        this.logger.debug("{} is remapping obfuscated field {}{} for {}", this, name, obfDesc, obfOwner);
        return this.remapper.mapFieldName(obfOwner, name, obfDesc);
    }

    @Override
    public String map(String typeName) {
        this.logger.debug("{} is remapping class {}", this, typeName);
        return this.remapper.map(typeName);
    }
    
    @Override
    public String unmap(String typeName) {
        return typeName;
    }
    
    @Override
    public String mapDesc(String desc) {
        return this.remapper.mapDesc(desc);
    }
    
    @Override
    public String unmapDesc(String desc) {
        return ObfuscationUtil.unmapDescriptor(desc, this);
    }

}
