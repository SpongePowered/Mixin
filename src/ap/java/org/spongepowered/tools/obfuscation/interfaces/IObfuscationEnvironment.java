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

import java.util.Collection;

import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer;

/**
 * An obfuscation environment provides facilities to fetch obfuscation mappings
 * of a particular type and also manage writing generated mappings of that type
 * to disk.
 */
public interface IObfuscationEnvironment {

    /**
     * Get an obfuscation mapping for a method, returns null if no mapping
     * exists for the specified method in this environment.
     * 
     * @param method method to locate a mapping for
     * @return remapped method or null if no mapping exists
     */
    public abstract MappingMethod getObfMethod(MemberInfo method);

    /**
     * Get an obfuscation mapping for a method, returns null if no mapping
     * exists for the specified method in this environment.
     * 
     * @param method method to locate a mapping for
     * @return remapped method or null if no mapping exists
     */
    public abstract MappingMethod getObfMethod(MappingMethod method);

    /**
     * Get an obfuscation mapping for a method, returns null if no mapping
     * exists for the specified method in this environment.
     * 
     * @param method method to locate a mapping for
     * @param lazyRemap in general, if a mapping is not located, an attempt will
     *      be made to remap the owner and descriptor of the method to account
     *      for the fact that classes appearing in the descriptor may need to be
     *      remapped even if the method name is not. Setting <tt>lazyRemap</tt>
     *      to <tt>true</tt> disables this behaviour and simply fails fast if no
     *      mapping is located
     * @return remapped method or null if no mapping exists
     */
    public abstract MappingMethod getObfMethod(MappingMethod method, boolean lazyRemap);

    /**
     * Get an obfuscation mapping for a field, returns null if no mapping
     * exists for the specified field in this environment.
     * 
     * @param field field to locate a mapping for
     * @return remapped field or null if no mapping exists
     */
    public abstract MappingField getObfField(MemberInfo field);

    /**
     * Get an obfuscation mapping for a field, returns null if no mapping
     * exists for the specified field in this environment.
     * 
     * @param field field to locate a mapping for
     * @return remapped field or null if no mapping exists
     */
    public abstract MappingField getObfField(MappingField field);

    /**
     * Get an obfuscation mapping for a field, returns null if no mapping
     * exists for the specified field in this environment.
     * 
     * @param field field to locate a mapping for
     * @param lazyRemap in general, if a mapping is not located, an attempt will
     *      be made to remap the owner and descriptor of the field to account
     *      for the fact that classes appearing in the descriptor may need to be
     *      remapped even if the field name is not. Setting <tt>lazyRemap</tt>
     *      to <tt>true</tt> disables this behaviour and simply fails fast if no
     *      mapping is located
     * @return remapped field or null if no mapping exists
     */
    public abstract MappingField getObfField(MappingField field, boolean lazyRemap);

    /**
     * Get an obfuscation mapping for a class, returns null if no mapping exists
     * for the specified class in this environment.
     * 
     * @param className Name of the class to remap (binary format)
     * @return remapped class name
     */
    public abstract String getObfClass(String className);

    /**
     * Remap only the owner and descriptor of the specified method
     * 
     * @param method method to remap
     * @return remapped method or null if no remapping occurred
     */
    public abstract MemberInfo remapDescriptor(MemberInfo method);

    /**
     * Remap a single descriptor in the context of this environment
     * 
     * @param desc descriptor to remap
     * @return remapped descriptor, may return the original descriptor if no
     *      remapping occurred
     */
    public abstract String remapDescriptor(String desc);

    /**
     * Write out accumulated mappings
     * 
     * @param consumers all consumers accumulated during the AP pass
     */
    public abstract void writeMappings(Collection<IMappingConsumer> consumers);

}
