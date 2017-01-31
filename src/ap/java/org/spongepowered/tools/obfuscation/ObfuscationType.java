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

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IOptionProvider;
import org.spongepowered.tools.obfuscation.mcp.ObfuscationServiceMCP;
import org.spongepowered.tools.obfuscation.service.ObfuscationTypeDescriptor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * Obfuscation types supported by the annotation processor
 */
public final class ObfuscationType {
    
    /**
     * Available obfuscation types indexed by key
     */
    private static final Map<String, ObfuscationType> types = new LinkedHashMap<String, ObfuscationType>();
    
    /**
     * Key for this type
     */
    private final String key;

    /**
     * Descriptor contains the majority of the metadata for the obfuscation type
     */
    private final ObfuscationTypeDescriptor descriptor;
    
    /**
     * Annotation Processor
     */
    private final IMixinAnnotationProcessor ap;
    
    /**
     * Option provider
     */
    private final IOptionProvider options;
    
    private ObfuscationType(ObfuscationTypeDescriptor descriptor, IMixinAnnotationProcessor ap) {
        this.key = descriptor.getKey();
        this.descriptor = descriptor;
        this.ap = ap;
        this.options = ap;
    }
    
    /**
     * Create obfuscation environment instance for this obfuscation type
     */
    public final ObfuscationEnvironment createEnvironment() {
        try {
            Class<? extends ObfuscationEnvironment> cls = this.descriptor.getEnvironmentType();
            Constructor<? extends ObfuscationEnvironment> ctor = cls.getDeclaredConstructor(ObfuscationType.class);
            ctor.setAccessible(true);
            return ctor.newInstance(this);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    @Override
    public String toString() {
        return this.key;
    }
    
    
    public String getKey() {
        return this.key;
    }

    public ObfuscationTypeDescriptor getConfig() {
        return this.descriptor;
    }
    
    public IMixinAnnotationProcessor getAnnotationProcessor() {
        return this.ap;
    }
    
    /**
     * Get whether this is ithe default obfuscation environment
     */
    public boolean isDefault() {
        String defaultEnv = this.options.getOption(SupportedOptions.DEFAULT_OBFUSCATION_ENV);
        return (defaultEnv == null && this.key.equals(ObfuscationServiceMCP.SEARGE))
                || (defaultEnv != null && this.key.equals(defaultEnv.toLowerCase()));
    }
    
    /**
     * Get whether this obfuscation type has data available
     */
    public boolean isSupported() {
        return this.getInputFileNames().size() > 0;
    }
    
    /**
     * Get the input file names specified for this obfuscation type
     */
    public List<String> getInputFileNames() {
        Builder<String> builder = ImmutableList.<String>builder();
        
        String inputFile = this.options.getOption(this.descriptor.getInputFileOption());
        if (inputFile != null) {
            builder.add(inputFile);
        }
        
        String extraInputFiles = this.options.getOption(this.descriptor.getExtraInputFilesOption());
        if (extraInputFiles != null) {
            for (String extraInputFile : extraInputFiles.split(";")) {
                builder.add(extraInputFile.trim());
            }
        }
        
        return builder.build();
    }
    
    /**
     * Get the output filenames specified for this obfuscation type
     */
    public String getOutputFileName() {
        return this.options.getOption(this.descriptor.getOutputFileOption());
    }

    /**
     * All available obfuscation types
     */
    public static Iterable<ObfuscationType> types() {
        return ObfuscationType.types.values();
    }
    
    /**
     * Create a new obfuscation type from the supplied descriptor
     * 
     * @param descriptor obfuscation type metadata
     * @param ap annotation processor
     * @return new obfuscation type
     */
    public static ObfuscationType create(ObfuscationTypeDescriptor descriptor, IMixinAnnotationProcessor ap) {
        String key = descriptor.getKey();
        if (ObfuscationType.types.containsKey(key)) {
            throw new IllegalArgumentException("Obfuscation type with key " + key + " was already registered");
        }
        ObfuscationType type = new ObfuscationType(descriptor, ap);
        ObfuscationType.types.put(key, type);
        return type;
    }
    
    /**
     * Retrieve an obfuscation type by key
     * 
     * @param key obfuscation type key to retrieve
     * @return obfuscation type or <tt>null</tt> if no matching type is
     *      available
     */
    public static ObfuscationType get(String key) {
        ObfuscationType type = ObfuscationType.types.get(key);
        if (type == null) {
            throw new IllegalArgumentException("Obfuscation type with key " + key + " was not registered");
        }
        return type;
    }
}
