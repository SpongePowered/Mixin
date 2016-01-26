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
import org.spongepowered.asm.mixin.injection.struct.ReferenceMapper;
import org.spongepowered.asm.obfuscation.SrgMethod;
import org.spongepowered.tools.obfuscation.ObfuscationData;
import org.spongepowered.tools.obfuscation.TypeHandle;

/**
 * Manages obfuscation things
 */
public interface IObfuscationManager {

    /**
     * Get the underlying reference mapper
     */
    public abstract ReferenceMapper getReferenceMapper();

    /**
     * Attempts to resolve an obfuscation entry by recursively enumerating
     * superclasses of the target member until a match is found. If a match is
     * found in a superclass then the reference is remapped to the original
     * owner class.
     * 
     * @param targetMember Member to search for
     * @return ObfuscationData with remapped owner class corresponding to the
     *      original owner class
     */
    public abstract <T> ObfuscationData<T> getObfEntryRecursive(MemberInfo targetMember);

    /**
     * Resolves a field or method reference to an ObfuscationData set
     * 
     * @param targetMember member to search for
     * @return obfuscation data (by type) for the supplied member
     */
    public abstract <T> ObfuscationData<T> getObfEntry(MemberInfo targetMember);

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
    public abstract ObfuscationData<SrgMethod> getObfMethodRecursive(MemberInfo method);

    /**
     * Get an obfuscation mapping for a method
     */
    public abstract ObfuscationData<SrgMethod> getObfMethod(MemberInfo method);

    /**
     * Get an obfuscation mapping for a method
     */
    public abstract ObfuscationData<SrgMethod> getObfMethod(SrgMethod method);

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
    public abstract ObfuscationData<String> getObfFieldRecursive(MemberInfo field);

    /**
     * Get an obfuscation mapping for a field
     */
    public abstract ObfuscationData<String> getObfField(String field);

    /**
     * Get an obfuscation mapping for a class
     */
    public abstract ObfuscationData<String> getObfClass(TypeHandle type);

    /**
     * Get an obfuscation mapping for a class
     */
    public abstract ObfuscationData<String> getObfClass(String className);

    /**
     * Adds a method mapping to the internal refmap
     * 
     * @param className Mixin class name which owns the refmap entry
     * @param reference Original reference, as it appears in the annotation
     * @param obfMethodData Method data to add for this mapping
     */
    public abstract void addMethodMapping(String className, String reference, ObfuscationData<SrgMethod> obfMethodData);

    /**
     * Adds a method mapping to the internal refmap, generates refmap entries
     * using the supplied parsed memberinfo as context
     * 
     * @param className Mixin class name which owns the refmap entry
     * @param reference Original reference, as it appears in the annotation
     * @param context The context for this mapping entry, remapped using the
     *      supplied obfuscation data
     * @param obfMethodData Method data to add for this mapping
     */
    public abstract void addMethodMapping(String className, String reference, MemberInfo context, ObfuscationData<SrgMethod> obfMethodData);

    /**
     * Adds a field mapping to the internal refmap, generates refmap entries
     * using the supplied parsed memberinfo as context
     * 
     * @param className Mixin class name which owns the refmap entry
     * @param reference Original reference, as it appears in the annotation
     * @param context The context for this mapping entry, remapped using the
     *      supplied obfuscation data
     * @param obfFieldData Field data to add for this mapping
     */
    public abstract void addFieldMapping(String className, String reference, MemberInfo context, ObfuscationData<String> obfFieldData);

    /**
     * Adds a class mapping to the internal refmap
     * 
     * @param className Mixin class name which owns the refmap entry
     * @param reference Original reference, as it appears in the annotation
     * @param obfClassData Class obf names
     */
    public abstract void addClassMapping(String className, String reference, ObfuscationData<String> obfClassData);

}
