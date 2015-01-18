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
import java.io.InputStream;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Set;

import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import com.google.common.collect.ImmutableSet;

/**
 * Base class for "info" objects which use ASM tree API to do stuff, with things
 */
abstract class TreeInfo {
    
    private static final Set<String> excludeTransformers = ImmutableSet.<String>of(
        "net.minecraftforge.fml.common.asm.transformers.EventSubscriptionTransformer",
        "cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer",
        "net.minecraftforge.fml.common.asm.transformers.TerminalTransformer",
        "cpw.mods.fml.common.asm.transformers.TerminalTransformer"
    );
    
    private static IClassNameTransformer nameTransformer;
    
    static {
        for (IClassTransformer transformer : Launch.classLoader.getTransformers()) {
            if (transformer instanceof IClassNameTransformer) {
                TreeInfo.nameTransformer = (IClassNameTransformer) transformer;
            }
        }
    }

    static ClassNode getClassNode(String className) throws ClassNotFoundException, IOException {
        return TreeInfo.getClassNode(TreeInfo.loadClass(className, true), 0);
    }

    /**
     * @param classBytes
     * @param flags
     * @return ASM Tree view of the specified class 
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
     * @return Transformed class bytecode for the specified class
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
     * @param className Name of the class to load
     * @return raw class bytecode
     * @throws IOException
     */
    private static byte[] getClassBytes(String className) throws IOException {
        byte[] classBytes = Launch.classLoader.getClassBytes(TreeInfo.unmap(className));
        if (classBytes != null) {
            return classBytes;
        }
        
        URLClassLoader appClassLoader = (URLClassLoader)Launch.class.getClassLoader();
        
        InputStream classStream = null;
        try {
            final String resourcePath = className.replace('.', '/').concat(".class");
            classStream = appClassLoader.getResourceAsStream(resourcePath);
            return IOUtils.toByteArray(classStream);
        } catch (Exception ex) {
            return null;
        } finally {
            IOUtils.closeQuietly(classStream);
        }
    }

    /**
     * Since we obtain the class bytes with getClassBytes(), we need to apply the transformers ourself
     * 
     * @param name
     * @param basicClass
     * @return class bytecode after processing by all registered transformers except the excluded transformers
     */
    private static byte[] applyTransformers(String name, byte[] basicClass) {
        final List<IClassTransformer> transformers = Launch.classLoader.getTransformers();

        for (final IClassTransformer transformer : transformers) {
            if (!(transformer instanceof MixinTransformer) && !TreeInfo.excludeTransformers.contains(transformer.getClass().getName())) {
                basicClass = transformer.transform(name, name, basicClass);
            }
        }

        return basicClass;
    }

    /**
     * Map a class name back to its obfuscated counterpart 
     * 
     * @param className
     * @return obfuscated name for the specified deobfuscated reference
     */
    static String unmap(String className) {
        if (TreeInfo.nameTransformer != null) {
            return TreeInfo.nameTransformer.unmapClassName(className);
        }
        
        return className;
    }
}
