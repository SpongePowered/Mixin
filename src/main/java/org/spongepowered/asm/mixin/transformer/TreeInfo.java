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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.MixinTransformer.ReEntranceState;

import com.google.common.collect.Sets;

/**
 * Base class for "info" objects which use ASM tree API to do stuff, with things
 */
abstract class TreeInfo {
    
    /**
     * Known re-entrant transformers, other re-entrant transformers will
     * detected automatically 
     */
    private static final Set<String> excludeTransformers = Sets.<String>newHashSet(
        "net.minecraftforge.fml.common.asm.transformers.EventSubscriptionTransformer",
        "cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer",
        "net.minecraftforge.fml.common.asm.transformers.TerminalTransformer",
        "cpw.mods.fml.common.asm.transformers.TerminalTransformer"
    );
    
    private static final Logger logger = LogManager.getLogger("mixin");
    
    /**
     * Local transformer chain, this consists of all transformers present at the
     * init phase with the exclusion of the mixin transformer itself and known
     * re-entrant transformers. Detected re-entrant transformers will be
     * subsequently removed.
     */
    private static List<IClassTransformer> transformers;
    
    /**
     * Class name transformer (if present)
     */
    private static IClassNameTransformer nameTransformer;
    
    /**
     * Current environment 
     */
    private static MixinEnvironment currentEnvironment;
    
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
        TreeInfo.checkEnvironment();
        
        String transformedName = className.replace('/', '.');
        String name = TreeInfo.unmap(transformedName);
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
        final List<IClassTransformer> transformers = TreeInfo.transformers;

        for (Iterator<IClassTransformer> iter = transformers.iterator(); iter.hasNext();) {
            IClassTransformer transformer = iter.next();

            if (TreeInfo.lock != null) {
                // Clear the re-entrance semaphore
                TreeInfo.lock.clear();
            }
            
            basicClass = transformer.transform(name, transformedName, basicClass);

            if (TreeInfo.lock != null && TreeInfo.lock.isSet()) {
                // If the re-entrance semaphore was raised during the last transformation then we can assume
                // that this transformer is re-entrant and remove it from our transformer chain.
                iter.remove();
                
                // Also add it to the exclusion list so we can exclude it if the environment triggers a rebuild
                TreeInfo.excludeTransformers.add(transformer.getClass().getName());
                
                TreeInfo.lock.clear();
                TreeInfo.logger.info("A re-entrant transformer '{}' was detected and will no longer process meta class data",
                        transformer.getClass().getName());
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
        IClassNameTransformer nameTransformer = TreeInfo.nameTransformer;
        if (nameTransformer != null) {
            return nameTransformer.unmapClassName(className);
        }
        
        return className;
    }

    /**
     * Check whether the current environment has changed
     */
    private static void checkEnvironment() {
        if (MixinEnvironment.getCurrentEnvironment() != TreeInfo.currentEnvironment) {
            TreeInfo.buildTransformerDelegationList();
            TreeInfo.currentEnvironment = MixinEnvironment.getCurrentEnvironment();
        }
    }

    /**
     * Builds the transformer list to apply to loaded mixin bytecode. Since
     * generating this list requires inspecting each transformer by name (to
     * cope with the new wrapper functionality added by FML) we generate the
     * list just once and cache the result.
     */
    private static void buildTransformerDelegationList() {
        TreeInfo.logger.debug("Building transformer delegation list:");
        TreeInfo.transformers = new ArrayList<IClassTransformer>();
        for (IClassTransformer transformer : Launch.classLoader.getTransformers()) {
            String transformerName = transformer.getClass().getName();
            boolean include = true;
            for (String excludeClass : TreeInfo.excludeTransformers) {
                if (transformerName.contains(excludeClass)) {
                    include = false;
                    break;
                }
            }
            if (include && !transformerName.contains(MixinTransformer.class.getName())) {
                TreeInfo.logger.debug("  Adding:    {}", transformerName);
                TreeInfo.transformers.add(transformer);
            } else {
                TreeInfo.logger.debug("  Excluding: {}", transformerName);
            }
        }

        TreeInfo.logger.debug("Transformer delegation list created with {} entries", TreeInfo.transformers.size());
        
        for (IClassTransformer transformer : Launch.classLoader.getTransformers()) {
            if (transformer instanceof IClassNameTransformer) {
                TreeInfo.logger.debug("Found name transformer: {}", transformer.getClass().getName());
                TreeInfo.nameTransformer = (IClassNameTransformer) transformer;
            }
        }
    }
}
