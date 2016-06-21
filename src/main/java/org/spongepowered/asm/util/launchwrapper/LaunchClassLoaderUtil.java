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
package org.spongepowered.asm.util.launchwrapper;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * Utility class for reflecting into {@link LaunchClassLoader}. We <b>do not
 * write</b> anything of the classloader fields, but we need to be able to read
 * them to perform some validation tasks, and insert entries for mixin "classes"
 * into the invalid classes set.
 */
public final class LaunchClassLoaderUtil {
    
    /**
     * ClassLoader -> util mapping
     */
    private static final Map<LaunchClassLoader, LaunchClassLoaderUtil> utils = new HashMap<LaunchClassLoader, LaunchClassLoaderUtil>();
    
    /**
     * ClassLoader for this util
     */
    private final LaunchClassLoader classLoader;
    
    // Reflected fields
    private Map<String, Class<?>> cachedClasses;
    private final Set<String> invalidClasses;
    private final Set<String> classLoaderExceptions;
    private final Set<String> transformerExceptions;

    /**
     * Singleton, use factory to get an instance
     * 
     * @param classLoader class loader
     */
    private LaunchClassLoaderUtil(LaunchClassLoader classLoader) {
        this.classLoader = classLoader;
        this.cachedClasses = LaunchClassLoaderUtil.<Map<String, Class<?>>>getField(classLoader, "cachedClasses");
        this.invalidClasses = LaunchClassLoaderUtil.<Set<String>>getField(classLoader, "invalidClasses");
        this.classLoaderExceptions = LaunchClassLoaderUtil.<Set<String>>getField(classLoader, "classLoaderExceptions");
        this.transformerExceptions = LaunchClassLoaderUtil.<Set<String>>getField(classLoader, "transformerExceptions");
    }
    
    /**
     * Get the classloader
     */
    public LaunchClassLoader getClassLoader() {
        return this.classLoader;
    }
    
    /**
     * Get all loaded class names from the cache
     */
    public Set<String> getLoadedClasses() {
        return this.getLoadedClasses(null);
    }
    
    /**
     * Get the names of loaded classes from the cache, filter using the supplied
     * filter string
     * 
     * @param filter filter string or null
     * @return set of class names
     */
    public Set<String> getLoadedClasses(String filter) {
        Set<String> loadedClasses = new HashSet<String>();
        for (String className : this.cachedClasses.keySet()) {
            if (filter == null || className.startsWith(filter)) {
                loadedClasses.add(className);
            }
        }
        return loadedClasses;
    }
    
    /**
     * Get whether a class name exists in the cache (indicating it was loaded
     * via the inner loader
     * 
     * @param name class name
     * @return true if the class name exists in the cache
     */
    public boolean isClassLoaded(String name) {
        if (this.cachedClasses != null && this.cachedClasses.containsKey(name)) {
            return true;
        }
        
        return false;
    }

    /**
     * Get whether the specified name or transformedName exist in either of the
     * exclusion lists
     * 
     * @param name class name
     * @param transformedName transformed class name
     * @return true if either exclusion list contains either of the names
     */
    public boolean isClassExcluded(String name, String transformedName) {
        for (final String exception : this.getClassLoaderExceptions()) {
            if (transformedName.startsWith(exception) || name.startsWith(exception)) {
                return true;
            }
        }
        
        for (final String exception : this.getTransformerExceptions()) {
            if (transformedName.startsWith(exception) || name.startsWith(exception)) {
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
    public void registerInvalidClass(String name) {
        if (this.invalidClasses != null) {
            this.invalidClasses.add(name);
        }
    }
    
    public Set<String> getClassLoaderExceptions() {
        if (this.classLoaderExceptions != null) {
            return this.classLoaderExceptions;
        }
        return Collections.<String>emptySet();
    }
    
    public Set<String> getTransformerExceptions() {
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
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Get the utility class for the supplied classloader
     * 
     * @param classLoader classLoader to fetch utility wrapper for
     * @return utility wrapper
     */
    public static LaunchClassLoaderUtil forClassLoader(LaunchClassLoader classLoader) {
        LaunchClassLoaderUtil util = LaunchClassLoaderUtil.utils.get(classLoader);
        if (util == null) {
            util = new LaunchClassLoaderUtil(classLoader);
            LaunchClassLoaderUtil.utils.put(classLoader, util);
        }
        return util;
    }
}
