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
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;

import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.util.ReEntranceLock;

/**
 * Mixin Service interface. Mixin services connect the mixin subsytem to the
 * underlying environment. It is something of a god interface at present because
 * it contains all of the current functionality accessors for calling into
 * launchwrapper. In the future once support for modlauncher is added, it is
 * anticipated that the interface can be split down into sub-services which
 * handle different aspects of interacting with the environment.
 */
public interface IMixinService {
    
    /**
     * Get the friendly name for this service
     */
    public abstract String getName();

    /**
     * True if this service type is valid in the current environment
     */
    public abstract boolean isValid();
    
    /**
     * Called at subsystem boot
     */
    public abstract void prepare();

    /**
     * Get the initial subsystem phase
     */
    public abstract Phase getInitialPhase();
    
    /**
     * Called at the end of subsystem boot
     */
    public abstract void init();

    /**
     * Called whenever a new phase is started 
     */
    public abstract void beginPhase();

    /**
     * Check whether the supplied object is a valid boot source for mixin
     * environment
     * 
     * @param bootSource boot source
     */
    public abstract void checkEnv(Object bootSource);
    
    /**
     * Get the transformer re-entrance lock for this service, the transformer
     * uses this lock to track transformer re-entrance when co-operative load
     * and transform is performed by the service.
     */
    public abstract ReEntranceLock getReEntranceLock();
    
    /**
     * Return the class provider for this service
     */
    public abstract IClassProvider getClassProvider();
    
    /**
     * Get additional platform agents for this service 
     */
    public abstract Collection<String> getPlatformAgents();
    
    /**
     * Get a resource as a stream from the appropriate classloader, this is
     * delegated via the service so that the service can choose the correct
     * classloader from which to obtain the resource.
     * 
     * @param name resource path
     * @return input stream or null if resource not found
     */
    public abstract InputStream getResourceAsStream(String name);

    /**
     * Find a class in the service classloader
     * 
     * @param name class name
     * @return resultant class
     * @throws ClassNotFoundException if the class was not found
     */
    public abstract Class<?> findClass(final String name) throws ClassNotFoundException;

    /**
     * Register an invalid class with the service classloader
     * 
     * @param className invalid class name
     */
    public abstract void registerInvalidClass(String className);

    /**
     * Check whether the specified class was already loaded by the service
     * classloader
     * 
     * @param className class name to check
     * @return true if the class was already loaded
     */
    public abstract boolean isClassLoaded(String className);
    
    /**
     * Check whether the specified class is excluded in the service classloader
     * 
     * @param name class name to check
     * @param transformedName transformed name to check
     * @return true if the class is excluded in the service classloader
     */
    public abstract boolean isClassExcluded(String name, String transformedName);

    /**
     * Get the current classpath from the service classloader
     */
    public abstract URL[] getClassPath();

    /**
     * Get currently available transformers in the environment
     */
    public abstract Collection<ITransformer> getTransformers();

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

    /**
     * Get the detected side name for this environment
     */
    public abstract String getSideName();

}
