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
package org.spongepowered.tools.obfuscation.mapping;

import java.io.File;
import java.io.IOException;

import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;

/**
 * A mapping provider stores raw mapping information for use by the AP, access
 * is delegated via the environment which provides enhanced lookup functionality
 * such as resolving members recursively in super classes. The mapping provider
 * is not required to provide such functionality and merely facilitates raw
 * mapping lookups from a particular source. 
 */
public interface IMappingProvider {

    /**
     * Clear this mapping provider, used to ensure the internal data is cleared
     * before beginning a {@link #read}
     */
    public abstract void clear();

    /**
     * Returns true if this mapping provider contains no mappings
     */
    public abstract boolean isEmpty();

    /**
     * Called multiple times by the environment. This method will be called for
     * each input file specified by the user. 
     * 
     * @param input input file to read
     * @throws IOException if an error occurs reading the input file or the file
     *      does not exist or cannot be opened
     */
    public abstract void read(File input) throws IOException;

    /**
     * Retrieve a method mapping from this provider. This method should return
     * <tt>null</tt> if no mapping is found
     * 
     * @param method method to find a mapping for
     * @return mapped method or <tt>null</tt> if not found
     */
    public abstract MappingMethod getMethodMapping(MappingMethod method);

    /**
     * Retrieve a field mapping from this provider. This method should return
     * <tt>null</tt> if no mapping is found
     * 
     * @param field field to find a mapping for
     * @return mapped field or <tt>null</tt> if not found
     */
    public abstract MappingField getFieldMapping(MappingField field);

    /**
     * Retrieve a class mapping from this provider. This method should return
     * <tt>null</tt> if no mapping is found
     * 
     * @param className name of the class to find a mapping for
     * @return mapped class name or <tt>null</tt> if not found
     */
    public abstract String getClassMapping(String className);

    /**
     * Retrieve a package mapping from this provider. This method should return
     * <tt>null</tt> if no mapping is found
     * 
     * @param packageName name of the package to find a mapping for
     * @return mapped package name or <tt>null</tt> if not found
     */
    public abstract String getPackageMapping(String packageName);
    
}
