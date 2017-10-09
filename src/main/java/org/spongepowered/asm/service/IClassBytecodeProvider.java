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

import java.io.IOException;

import org.spongepowered.asm.lib.tree.ClassNode;

/**
 * Interface for object which can provide class bytecode
 */
public interface IClassBytecodeProvider {

    /**
     * Retrieve class bytes using available classloaders, does not transform the
     * class
     * 
     * @param name class name
     * @param transformedName transformed class name
     * @return class bytes or null if not found
     * @throws IOException propagated
     */
    public abstract byte[] getClassBytes(String name, String transformedName) throws IOException;
    
    /**
     * Retrieve transformed class bytes by using available classloaders and
     * running transformer delegation chain on the result if the runTransformers
     * option is enabled 
     * 
     * @param name full class name
     * @param runTransformers true to run transformers on the loaded bytecode
     * @return transformed bytes
     * @throws ClassNotFoundException if class not found
     * @throws IOException propagated
     */
    public abstract byte[] getClassBytes(String name, boolean runTransformers) throws ClassNotFoundException, IOException;

    /**
     * Retrieve transformed class as an ASM tree
     * 
     * @param name full class name
     * @return tree
     * @throws ClassNotFoundException if class not found
     * @throws IOException propagated
     */
    public abstract ClassNode getClassNode(String name) throws ClassNotFoundException, IOException;

}
