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
import org.spongepowered.asm.mixin.refmap.ReferenceMapper;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.ObfuscationData;
import org.spongepowered.tools.obfuscation.ReferenceManager.ReferenceConflictException;

/**
 * Consumer for generated references, builds the refmap during an AP pass and
 * supports writing the generated refmap to file once the AP run is complete
 */
public interface IReferenceManager {
    
    /**
     * Set whether this reference manager should allow conflicts to be inserted
     * without raising an exception. Set to allow overrides to be written into
     * the refmap when necessary
     * 
     * @param allowConflicts allow conflicts without raising an exception
     */
    public void setAllowConflicts(boolean allowConflicts);
    
    /**
     * Get whether replacement mappings are allowed. Normally a mapping conflict
     * will raise a {@link ReferenceConflictException}.
     * 
     * @return true if conflicts are allowed
     */
    public boolean getAllowConflicts();

    /**
     * Write the generated refmap to file
     */
    public abstract void write();

    /**
     * Get the underlying reference mapper
     */
    public abstract ReferenceMapper getMapper();

    /**
     * Adds a method mapping to the internal refmap
     * 
     * @param className Mixin class name which owns the refmap entry
     * @param reference Original reference, as it appears in the annotation
     * @param obfMethodData Method data to add for this mapping
     */
    public abstract void addMethodMapping(String className, String reference, ObfuscationData<MappingMethod> obfMethodData);

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
    public abstract void addMethodMapping(String className, String reference, MemberInfo context, ObfuscationData<MappingMethod> obfMethodData);

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
    public abstract void addFieldMapping(String className, String reference, MemberInfo context, ObfuscationData<MappingField> obfFieldData);

    /**
     * Adds a class mapping to the internal refmap
     * 
     * @param className Mixin class name which owns the refmap entry
     * @param reference Original reference, as it appears in the annotation
     * @param obfClassData Class obf names
     */
    public abstract void addClassMapping(String className, String reference, ObfuscationData<String> obfClassData);

}
