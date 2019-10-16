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
package org.spongepowered.asm.service.mojang;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.service.IClassTracker;

import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * Utility class for reflecting into {@link LaunchClassLoader}. We <b>do not
 * write</b> anything of the classloader fields, but we need to be able to read
 * them to perform some validation tasks, and insert entries for mixin "classes"
 * into the invalid classes set.
 */
final class LaunchClassLoaderUtil implements IClassTracker {
    
    private static final String CACHED_CLASSES_FIELD = "cachedClasses";
    private static final String INVALID_CLASSES_FIELD = "invalidClasses";
    private static final String CLASS_LOADER_EXCEPTIONS_FIELD = "classLoaderExceptions";
    private static final String TRANSFORMER_EXCEPTIONS_FIELD = "transformerExceptions";
    
    /**
     * ClassLoader for this util
     */
    private final LaunchClassLoader classLoader;
    
    // Reflected fields
    private final Map<String, Class<?>> cachedClasses;
    private final Set<String> invalidClasses;
    private final Set<String> classLoaderExceptions;
    private final Set<String> transformerExceptions;

    /**
     * Singleton, use factory to get an instance
     * 
     * @param classLoader class loader
     */
    LaunchClassLoaderUtil(LaunchClassLoader classLoader) {
        this.classLoader = classLoader;
        this.cachedClasses = LaunchClassLoaderUtil.<Map<String, Class<?>>>getField(classLoader, LaunchClassLoaderUtil.CACHED_CLASSES_FIELD);
        this.invalidClasses = LaunchClassLoaderUtil.<Set<String>>getField(classLoader, LaunchClassLoaderUtil.INVALID_CLASSES_FIELD);
        this.classLoaderExceptions = LaunchClassLoaderUtil.<Set<String>>getField(classLoader, LaunchClassLoaderUtil.CLASS_LOADER_EXCEPTIONS_FIELD);
        this.transformerExceptions = LaunchClassLoaderUtil.<Set<String>>getField(classLoader, LaunchClassLoaderUtil.TRANSFORMER_EXCEPTIONS_FIELD);
    }

    /**
     * Get the classloader
     */
    LaunchClassLoader getClassLoader() {
        return this.classLoader;
    }
    
    /**
     * Get whether a class name exists in the cache (indicating it was loaded
     * via the inner loader
     * 
     * @param name class name
     * @return true if the class name exists in the cache
     */
    @Override
    public boolean isClassLoaded(String name) {
        return this.cachedClasses.containsKey(name);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getClassRestrictions(
     *      java.lang.String)
     */
    @Override
    public String getClassRestrictions(String className) {
        String restrictions = "";
        if (this.isClassClassLoaderExcluded(className, null)) {
            restrictions = "PACKAGE_CLASSLOADER_EXCLUSION";
        }
        if (this.isClassTransformerExcluded(className, null)) {
            restrictions = (restrictions.length() > 0 ? restrictions + "," : "") + "PACKAGE_TRANSFORMER_EXCLUSION";
        }
        return restrictions;
    }

    /**
     * Get whether the specified name or transformedName exist in either of the
     * exclusion lists
     * 
     * @param name class name
     * @param transformedName transformed class name
     * @return true if either exclusion list contains either of the names
     */
    boolean isClassExcluded(String name, String transformedName) {
        return this.isClassClassLoaderExcluded(name, transformedName) || this.isClassTransformerExcluded(name, transformedName);
    }

    /**
     * Get whether the specified name or transformedName exist in the
     * classloader exclusion list
     * 
     * @param name class name
     * @param transformedName transformed class name
     * @return true if the classloader exclusion list contains either of the
     *      names
     */
    boolean isClassClassLoaderExcluded(String name, String transformedName) {
        for (final String exception : this.getClassLoaderExceptions()) {
            if ((transformedName != null && transformedName.startsWith(exception)) || name.startsWith(exception)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Get whether the specified name or transformedName exist in the
     * transformer exclusion list
     * 
     * @param name class name
     * @param transformedName transformed class name
     * @return true if the transformer exclusion list contains either of the
     *      names
     */
    boolean isClassTransformerExcluded(String name, String transformedName) {
        for (final String exception : this.getTransformerExceptions()) {
            if ((transformedName != null && transformedName.startsWith(exception)) || name.startsWith(exception)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Stuff a class name directly into the invalidClasses set, this prevents
     * the loader from classloading the named class. This is used by the mixin
     * processor to prevent classloading of mixin classes
     * 
     * @param name class name
     */
    @Override
    public void registerInvalidClass(String name) {
        if (this.invalidClasses != null) {
            this.invalidClasses.add(name);
        }
    }
    
    /**
     * Get the classloader exclusions from the target classloader
     */
    Set<String> getClassLoaderExceptions() {
        if (this.classLoaderExceptions != null) {
            return this.classLoaderExceptions;
        }
        return Collections.<String>emptySet();
    }
    
    /**
     * Get the transformer exclusions from the target classloader
     */
    Set<String> getTransformerExceptions() {
        if (this.transformerExceptions != null) {
            return this.transformerExceptions;
        }
        return Collections.<String>emptySet();
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(LaunchClassLoader classLoader, String fieldName) {
        try {
            Field field = LaunchClassLoader.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T)field.get(classLoader);
        } catch (Exception ex) {
//            ex.printStackTrace();
        }
        return null;
    }

}
