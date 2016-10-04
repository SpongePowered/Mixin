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
package org.spongepowered.tools.obfuscation.interfaces;

import java.util.List;

import org.spongepowered.tools.obfuscation.ObfuscationEnvironment;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer;

/**
 * Manages obfuscation things
 */
public interface IObfuscationManager {

    /**
     * Initialise the obfuscation environments
     */
    public abstract void init();

    /**
     * Get the obfuscation mapping source
     */
    public abstract IObfuscationDataProvider getDataProvider();
    
    /**
     * Get the reference manager
     */
    public abstract IReferenceManager getReferenceManager();

    /**
     * Create a new mapping consumer
     */
    public abstract IMappingConsumer createMappingConsumer();

    /**
     * Get available obfuscation environments within this manager
     */
    public abstract List<ObfuscationEnvironment> getEnvironments();

    /**
     * Write out generated mappings to the target environments
     */
    public abstract void writeMappings();

    /**
     * Write out generated refmap 
     */
    public abstract void writeReferences();
    
}
