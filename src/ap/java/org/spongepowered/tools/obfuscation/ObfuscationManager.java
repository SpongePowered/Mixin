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
package org.spongepowered.tools.obfuscation;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IObfuscationManager;
import org.spongepowered.tools.obfuscation.interfaces.IObfuscationDataProvider;
import org.spongepowered.tools.obfuscation.interfaces.IReferenceManager;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer;
import org.spongepowered.tools.obfuscation.service.ObfuscationServices;

/**
 * Obfuscation Manager for mixin Annotation Processor
 */
public class ObfuscationManager implements IObfuscationManager {
    
    /**
     * Annotation processor
     */
    private final IMixinAnnotationProcessor ap;
    
    /**
     * Available obfuscation environments
     */
    private final List<ObfuscationEnvironment> environments = new ArrayList<ObfuscationEnvironment>();
    
    /**
     * Obfuscation provider
     */
    private final IObfuscationDataProvider obfs;
    
    /**
     * Refmap manager
     */
    private final IReferenceManager refs;

    /**
     * Mapping consumers
     */
    private final List<IMappingConsumer> consumers = new ArrayList<IMappingConsumer>();
    
    private boolean initDone;
    
    public ObfuscationManager(IMixinAnnotationProcessor ap) {
        this.ap = ap;
        this.obfs = new ObfuscationDataProvider(ap, this.environments);
        this.refs = new ReferenceManager(ap, this.environments);
    }

    @Override
    public void init() {
        if (this.initDone) {
            return;
        }
        this.initDone = true;
        ObfuscationServices.getInstance().initProviders(this.ap);
        for (ObfuscationType obfType : ObfuscationType.types()) {
            if (obfType.isSupported()) {
                this.environments.add(obfType.createEnvironment());
            }
        }
    }

    @Override
    public IObfuscationDataProvider getDataProvider() {
        return this.obfs;
    }
    
    @Override
    public IReferenceManager getReferenceManager() {
        return this.refs;
    }
    
    @Override
    public IMappingConsumer createMappingConsumer() {
        Mappings mappings = new Mappings();
        this.consumers.add(mappings);
        return mappings;
    }
    
    @Override
    public List<ObfuscationEnvironment> getEnvironments() {
        return this.environments;
    }

    /**
     * Write out generated mappings
     */
    @Override
    public void writeMappings() {
        for (ObfuscationEnvironment env : this.environments) {
            env.writeMappings(this.consumers);
        }
    }

    /**
     * Write out stored mappings
     */
    @Override
    public void writeReferences() {
        this.refs.write();
    }

}