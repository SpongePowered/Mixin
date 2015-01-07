/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered.org <http://www.spongepowered.org>
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
package org.spongepowered.asm.mixin.transformer;

import java.io.IOException;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

/**
 * Base class for "info" objects which use ASM tree API to do stuff, with things
 */
abstract class TreeInfo {
    
    static ClassNode getClassNode(String className) throws ClassNotFoundException, IOException {
        return TreeInfo.getClassNode(TreeInfo.loadClass(className, true), 0);
    }

    /**
     * @param classBytes
     * @param flags
     * @return
     */
    static ClassNode getClassNode(byte[] classBytes, int flags) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(classBytes);
        classReader.accept(classNode, flags);
        return classNode;
    }
    
    /**
     * @param className
     * @param runTransformers
     * @return
     */
    static byte[] loadClass(String className, boolean runTransformers) throws ClassNotFoundException, IOException {
        className = className.replace('/', '.');
        byte[] classBytes = null;

        if ((classBytes = TreeInfo.getClassBytes(className)) == null) {
            throw new ClassNotFoundException(String.format("The specified class '%s' was not found", className));
        }

        if (runTransformers) {
            classBytes = TreeInfo.applyTransformers(className, classBytes);
        }

        return classBytes;
    }

    /**
     * @param mixinClassName
     * @return
     * @throws IOException
     */
    private static byte[] getClassBytes(String mixinClassName) throws IOException {
        return Launch.classLoader.getClassBytes(mixinClassName);
    }

    /**
     * Since we obtain the class bytes with getClassBytes(), we need to apply the transformers ourself
     * 
     * @param name
     * @param basicClass
     * @return
     */
    private static byte[] applyTransformers(String name, byte[] basicClass) {
        final List<IClassTransformer> transformers = Launch.classLoader.getTransformers();

        for (final IClassTransformer transformer : transformers) {
            if (!(transformer instanceof MixinTransformer)) {
                basicClass = transformer.transform(name, name, basicClass);
            }
        }

        return basicClass;
    }
}
