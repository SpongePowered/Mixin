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
package org.spongepowered.asm.util.asm;

import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * MethodNode with some extra convenience functionality
 */
public class MethodNodeEx extends MethodNode {
    
    private final IMixinInfo owner;

    private final String originalName;
    
    public MethodNodeEx(int access, String name, String descriptor, String signature, String[] exceptions, IMixinInfo owner) {
        super(ASM.API_VERSION, access, name, descriptor, signature, exceptions);
        this.originalName = name;
        this.owner = owner;
    }
    
    @Override
    public String toString() {
        return String.format("%s%s", this.originalName, this.desc);
    }
    
    public String getQualifiedName() {
        return String.format("%s::%s", this.owner.getName(), this.originalName);
    }
    
    public String getOriginalName() {
        return this.originalName;
    }
    
    public IMixinInfo getOwner() {
        return this.owner;
    }

}
