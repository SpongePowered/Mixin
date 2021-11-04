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

import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Bytecode.Visibility;

/**
 * A {@link MethodHandle} for classpath classes being read with ASM
 */
public class MethodHandleASM extends MethodHandle {

    /**
     * The method from ASM
     */
    private final MethodNode method;

    public MethodHandleASM(TypeHandle owner, MethodNode method) {
        super(owner, method.name, method.desc);
        this.method = method;
    }
    
    @Override
    public String getJavaSignature() {
        return TypeUtils.getJavaSignature(this.method.desc);
    }
    
    @Override
    public Visibility getVisibility() {
        return Bytecode.getVisibility(this.method);
    }

}
