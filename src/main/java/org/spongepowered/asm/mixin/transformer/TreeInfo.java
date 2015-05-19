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

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.MixinTransformer.ReEntranceState;

/**
 * Base class for "info" objects which use ASM tree API to do stuff, with things
 */
abstract class TreeInfo {
    
    private static final Logger logger = LogManager.getLogger("mixin");
    
    /**
     * Re-entrance lock
     */
    private static ReEntranceState lock;

    static void setLock(ReEntranceState lock) {
        TreeInfo.lock = lock;
    }

    static ClassNode getClassNode(String className) throws ClassNotFoundException, IOException {
        return TreeInfo.getClassNode(TreeInfo.loadClass(className, true), 0);
    }

    /**
     * Gets an ASM Tree for the supplied class bytecode
     * 
     * @param classBytes Class bytecode
     * @param flags ClassReader flags
     * @return ASM Tree view of the specified class 
     */
    protected static ClassNode getClassNode(byte[] classBytes, int flags) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(classBytes);
        classReader.accept(classNode, flags);
        return classNode;
    }
    
    /**
     * Loads class bytecode from the classpath
     * 
     * @param className Name of the class to load
     * @param runTransformers True to run the loaded bytecode through the
     *      delegate transformer chain
     * @return Transformed class bytecode for the specified class
     * @throws ClassNotFoundException if the specified class could not be loaded
     * @throws IOException if an error occurs whilst reading the specified class
     */
    protected static byte[] loadClass(String className, boolean runTransformers) throws ClassNotFoundException, IOException {
        String transformedName = className.replace('/', '.');
        String name = MixinEnvironment.getCurrentEnvironment().unmap(transformedName);
        byte[] classBytes = null;

        if ((classBytes = TreeInfo.getClassBytes(name, transformedName)) == null) {
            throw new ClassNotFoundException(String.format("The specified class '%s' was not found", transformedName));
        }

        if (runTransformers) {
            classBytes = TreeInfo.applyTransformers(name, transformedName, classBytes);
        }

        return classBytes;
    }

    /**
     * @param name Original class name
     * @param transformedName Name of the class to load
     * @return raw class bytecode
     * @throws IOException
     */
    private static byte[] getClassBytes(String name, String transformedName) throws IOException {
        byte[] classBytes = Launch.classLoader.getClassBytes(name);
        if (classBytes != null) {
            return classBytes;
        }
        
        URLClassLoader appClassLoader = (URLClassLoader)Launch.class.getClassLoader();
        
        InputStream classStream = null;
        try {
            final String resourcePath = transformedName.replace('.', '/').concat(".class");
            classStream = appClassLoader.getResourceAsStream(resourcePath);
            return IOUtils.toByteArray(classStream);
        } catch (Exception ex) {
            return null;
        } finally {
            IOUtils.closeQuietly(classStream);
        }
    }

    /**
     * Since we obtain the class bytes with getClassBytes(), we need to apply
     * the transformers ourself
     * 
     * @param name
     * @param transformedName
     * @param basicClass
     * @return class bytecode after processing by all registered transformers
     *      except the excluded transformers
     */
    private static byte[] applyTransformers(String name, String transformedName, byte[] basicClass) {
        MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();
        
        for (IClassTransformer transformer : environment.getTransformers()) {
            if (TreeInfo.lock != null) {
                // Clear the re-entrance semaphore
                TreeInfo.lock.clear();
            }
            
            basicClass = transformer.transform(name, transformedName, basicClass);

            if (TreeInfo.lock != null && TreeInfo.lock.isSet()) {
                // Also add it to the exclusion list so we can exclude it if the environment triggers a rebuild
                environment.addTransformerExclusion(transformer.getClass().getName());
                
                TreeInfo.lock.clear();
                TreeInfo.logger.info("A re-entrant transformer '{}' was detected and will no longer process meta class data",
                        transformer.getClass().getName());
            }
        }

        return basicClass;
    }
}
