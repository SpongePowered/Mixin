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
package org.spongepowered.tools.obfuscation.service;

import org.spongepowered.tools.obfuscation.ObfuscationEnvironment;
import org.spongepowered.tools.obfuscation.ObfuscationType;

/**
 * Describes the arguments for a particular obfuscation type, used as an args
 * object for creating {@link ObfuscationType}s
 */
public class ObfuscationTypeDescriptor {
    
    /**
     * Refmap key for this mapping type
     */
    private final String key;
    
    /**
     * The name of the Annotation Processor argument which contains the file
     * name to read the mappings for this obfuscation type from 
     */
    private final String inputFileArgName;
    
    /**
     * The name of the Annotation Processor argument which contains the file
     * name to read the extra mappings for this obfuscation type from 
     */
    private final String extraInputFilesArgName;
    
    /**
     * The name of the Annotation Processor argument which contains the file
     * name to write generated mappings to 
     */
    private final String outFileArgName;

    private final Class<? extends ObfuscationEnvironment> environmentType;

    public ObfuscationTypeDescriptor(String key, String inputFileArgName, String outFileArgName,
            Class<? extends ObfuscationEnvironment> environmentType) {
        this(key, inputFileArgName, null, outFileArgName, environmentType);
    }
    
    public ObfuscationTypeDescriptor(String key, String inputFileArgName, String extraInputFilesArgName, String outFileArgName,
            Class<? extends ObfuscationEnvironment> environmentType) {
        this.key = key;
        this.inputFileArgName = inputFileArgName;
        this.extraInputFilesArgName = extraInputFilesArgName;
        this.outFileArgName = outFileArgName;
        this.environmentType = environmentType;
    }
    
    /**
     * Get the key for this type, must be unique
     */
    public final String getKey() {
        return this.key;
    }
    
    /**
     * Get the name of the AP argument used to specify the input file 
     */
    public String getInputFileOption() {
        return this.inputFileArgName;
    }
    
    /**
     * Get the name of the AP argument used to specify extra input files 
     */
    public String getExtraInputFilesOption() {
        return this.extraInputFilesArgName;
    }
    
    /**
     * Get the name of the AP argument used to specify the output file 
     */
    public String getOutputFileOption() {
        return this.outFileArgName;
    }

    /**
     * Get the environment class
     */
    public Class<? extends ObfuscationEnvironment> getEnvironmentType() {
        return this.environmentType;
    }
}
