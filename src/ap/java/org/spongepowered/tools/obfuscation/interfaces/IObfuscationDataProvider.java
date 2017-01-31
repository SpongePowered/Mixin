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

import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.obfuscation.mapping.IMapping;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.ObfuscationData;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;

/**
 * Provides obfuscation data to the annotation processor
 */
public interface IObfuscationDataProvider {

    /**
     * Attempts to resolve an obfuscation entry by recursively enumerating
     * superclasses of the target member until a match is found. If a match is
     * found in a superclass then the reference is remapped to the original
     * owner class.
     * 
     * @param targetMember Member to search for
     * @param <T> the type of the obfuscation mapping (MappingMethod for methods
     * @return ObfuscationData with remapped owner class corresponding to the
     *      original owner class
     *      or String for fields)     
     */
    public abstract <T> ObfuscationData<T> getObfEntryRecursive(MemberInfo targetMember);

    /**
     * Resolves a field or method reference to an ObfuscationData set
     * 
     * @param targetMember member to search for
     * @param <T> the type of the obfuscation mapping (MappingMethod for methods
     * @return obfuscation data (by type) for the supplied member
     *      or String for fields)     
     */
    public abstract <T> ObfuscationData<T> getObfEntry(MemberInfo targetMember);
    
    /**
     * Resolves a field or method reference to an ObfuscationData set
     * 
     * @param mapping member to search for
     * @param <T> the type of the obfuscation mapping (MappingMethod for methods
     * @return obfuscation data (by type) for the supplied member
     *      or String for fields)     
     */
    public abstract <T> ObfuscationData<T> getObfEntry(IMapping<T> mapping);

    /**
     * Attempts to resolve an obfuscated method by recursively enumerating
     * superclasses of the target method until a match is found. If a match is
     * found in a superclass then the method owner is remapped to the obfuscated
     * name of the original owner class.
     * 
     * @param method Method to search for
     * @return ObfuscationData with remapped owner class corresponding to the
     *      original owner class
     */
    public abstract ObfuscationData<MappingMethod> getObfMethodRecursive(MemberInfo method);

    /**
     * Get an obfuscation mapping for a method
     * 
     * @param method method to fetch obfuscation mapping for
     */
    public abstract ObfuscationData<MappingMethod> getObfMethod(MemberInfo method);

    /**
     * Get an obfuscation mapping for a method if an explicit mapping exists.
     * Where no direct mapping exists, remap the descriptor of the method only.
     * 
     * @param method method to fetch obfuscation mapping for
     */
    public abstract ObfuscationData<MappingMethod> getRemappedMethod(MemberInfo method);

    /**
     * Get an obfuscation mapping for a method
     * 
     * @param method method to fetch obfuscation mapping for
     */
    public abstract ObfuscationData<MappingMethod> getObfMethod(MappingMethod method);

    /**
     * Get an obfuscation mapping for a method if an explicit mapping exists.
     * Where no direct mapping exists, remap the descriptor of the method only.
     * 
     * @param method method to fetch obfuscation mapping for
     */
    public abstract ObfuscationData<MappingMethod> getRemappedMethod(MappingMethod method);

    /**
     * Attempts to resolve an obfuscated field by recursively enumerating
     * superclasses of the target field until a match is found. If a match is
     * found in a superclass then the field owner is remapped to the obfuscated
     * name of the original owner class.
     * 
     * @param field Field to search for
     * @return ObfuscationData with remapped owner class corresponding to the
     *      original owner class
     */
    public abstract ObfuscationData<MappingField> getObfFieldRecursive(MemberInfo field);

    /**
     * Get an obfuscation mapping for a field
     * 
     * @param field field to fetch obfuscation mapping for
     */
    public abstract ObfuscationData<MappingField> getObfField(MemberInfo field);

    /**
     * Get an obfuscation mapping for a field
     * 
     * @param field field to fetch obfuscation mapping for
     */
    public abstract ObfuscationData<MappingField> getObfField(MappingField field);

    /**
     * Get an obfuscation mapping for a class
     * 
     * @param type class type to fetch obfuscation mapping for
     */
    public abstract ObfuscationData<String> getObfClass(TypeHandle type);

    /**
     * Get an obfuscation mapping for a class
     * 
     * @param className class name to fetch obfuscation mapping for
     */
    public abstract ObfuscationData<String> getObfClass(String className);

}
