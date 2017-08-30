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
package org.spongepowered.asm.mixin.transformer.ext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.MixinTransformer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * Mixin transformer extensions and common modules such as class generators
 */
public final class Extensions {
    
    /**
     * Mixin transformer
     */
    private final MixinTransformer transformer;
    
    /**
     * All transformer extensions
     */
    private final List<IExtension> extensions = new ArrayList<IExtension>();
    
    /**
     * Map of extension class types to extension instances, used to fetch
     * generators using {@link #getExtension}
     */
    private final Map<Class<? extends IExtension>, IExtension> extensionMap
            = new HashMap<Class<? extends IExtension>, IExtension>();

    /**
     * Modules which generate synthetic classes required by mixins 
     */
    private final List<IClassGenerator> generators = new ArrayList<IClassGenerator>();
    
    /**
     * Read-only view of available generators
     */
    private final List<IClassGenerator> generatorsView = Collections.<IClassGenerator>unmodifiableList(this.generators);
    
    /**
     * Map of generator class types to generator instances, used to fetch
     * generators using {@link #getGenerator}
     */
    private final Map<Class<? extends IClassGenerator>, IClassGenerator> generatorMap
            = new HashMap<Class<? extends IClassGenerator>, IClassGenerator>();
    
    /**
     * Active transformer extensions
     */
    private List<IExtension> activeExtensions = Collections.<IExtension>emptyList();

    public Extensions(MixinTransformer transformer) {
        this.transformer = transformer;
    }
    
    public MixinTransformer getTransformer() {
        return this.transformer;
    }
    
    /**
     * Add a new transformer extension
     * 
     * @param extension extension to add
     */
    public void add(IExtension extension) {
        this.extensions.add(extension);
        this.extensionMap.put(extension.getClass(), extension);
    }
    
    /**
     * Get all extensions
     */
    public List<IExtension> getExtensions() {
        return Collections.<IExtension>unmodifiableList(this.extensions);
    }
    
    /**
     * Get all active extensions
     */
    public List<IExtension> getActiveExtensions() {
        return this.activeExtensions;
    }
    
    /**
     * Get a specific extension
     * 
     * @param extensionClass extension class to look up
     * @param <T> extension type
     * @return extension instance or null
     */
    @SuppressWarnings("unchecked")
    public <T extends IExtension> T getExtension(Class<? extends IExtension> extensionClass) {
        return (T)Extensions.<IExtension>lookup(extensionClass, this.extensionMap, this.extensions);
    } 

    /**
     * Selectively activate extensions based on the current environment
     * 
     * @param environment current environment
     */
    public void select(MixinEnvironment environment) {
        Builder<IExtension> activeExtensions = ImmutableList.<IExtension>builder();

        for (IExtension extension : this.extensions) {
            if (extension.checkActive(environment)) {
                activeExtensions.add(extension);
            }
        }
        
        this.activeExtensions = activeExtensions.build();
    }

    /**
     * Process tasks before mixin application
     * 
     * @param context Target class context
     */
    public void preApply(ITargetClassContext context) {
        for (IExtension extension : this.activeExtensions) {
            extension.preApply(context);
        }
    }

    /**
     * Process tasks after mixin application
     * 
     * @param context Target class context
     */
    public void postApply(ITargetClassContext context) {
        for (IExtension extension : this.activeExtensions) {
            extension.postApply(context);
        }
    }

    /**
     * Export class bytecode to disk
     * 
     * @param env Environment
     * @param name Class name
     * @param force True to export even if the current environment settings
     *      would normally disable it
     * @param bytes Bytes to export
     */
    public void export(MixinEnvironment env, String name, boolean force, byte[] bytes) {
        for (IExtension extension : this.activeExtensions) {
            extension.export(env, name, force, bytes);
        }
    }

    /**
     * Add a new generator to the mixin extensions
     * 
     * @param generator generator to add
     */
    public void add(IClassGenerator generator) {
        this.generators.add(generator);
        this.generatorMap.put(generator.getClass(), generator);
    }
    
    /**
     * Get all active generators
     */
    public List<IClassGenerator> getGenerators() {
        return this.generatorsView;
    }

    /**
     * @param generatorClass generator class or interface to look up
     * @param <T> genenerator class for duck typing
     * @return generator
     */
    @SuppressWarnings("unchecked")
    public <T extends IClassGenerator> T getGenerator(Class<? extends IClassGenerator> generatorClass) {
        return (T)Extensions.<IClassGenerator>lookup(generatorClass, this.generatorMap, this.generators);
    } 

    private static <T> T lookup(Class<? extends T> extensionClass, Map<Class<? extends T>, T> map, List<T> list) {
        T extension = map.get(extensionClass);
        if (extension == null) {
            for (T classGenerator : list) {
                if (extensionClass.isAssignableFrom(classGenerator.getClass())) {
                    extension = classGenerator;
                    break;
                }
            }
            
            if (extension == null) {
                throw new IllegalArgumentException("Extension for <" + extensionClass.getName() + "> could not be found");
            }
            
            map.put(extensionClass, extension);
        }
        
        return extension; 
    } 

}
