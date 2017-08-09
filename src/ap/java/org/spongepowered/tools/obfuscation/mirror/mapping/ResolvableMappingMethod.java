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
package org.spongepowered.tools.obfuscation.mirror.mapping;

import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeUtils;

/**
 * A mapping method obtained from a {@link TypeHandle}. The context for this
 * mapping allows references in superclasses to be resolved when necessary since
 * the hierarchy information is available.
 */
public final class ResolvableMappingMethod extends MappingMethod {
    
    private final TypeHandle ownerHandle;

    public ResolvableMappingMethod(TypeHandle owner, String name, String desc) {
        super(owner.getName(), name, desc);
        this.ownerHandle = owner;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.obfuscation.mapping.common.MappingMethod
     *      #getSuper()
     */
    @Override
    public MappingMethod getSuper() {
        if (this.ownerHandle == null) {
            return super.getSuper();
        }
        
        String name = this.getSimpleName();
        String desc = this.getDesc();
        String signature = TypeUtils.getJavaSignature(desc);
        
        TypeHandle superClass = this.ownerHandle.getSuperclass();
        if (superClass != null) {
            // Check whether superclass contains the method and return it
            if (superClass.findMethod(name, signature) != null) {
                return superClass.getMappingMethod(name, desc);
            }
        }
        
        // Check whether any interfaces contain the method and return it
        for (TypeHandle iface : this.ownerHandle.getInterfaces()) {
            if (iface.findMethod(name, signature) != null) {
                return iface.getMappingMethod(name, desc);
            }
        }
        
        // Otherwise if superclass is defined, try to look further up the hierarchy
        if (superClass != null) {
            return superClass.getMappingMethod(name, desc).getSuper();
        }
        
        return super.getSuper();
    }
    
    /**
     * Specialised version of <tt>move</tt> which allows this resolvable method
     * to be reparented to a new type handle
     * 
     * @param newOwner new owner
     * @return remapped method
     */
    public MappingMethod move(TypeHandle newOwner) {
        return new ResolvableMappingMethod(newOwner, this.getSimpleName(), this.getDesc());
    }

    @Override
    public MappingMethod remap(String newName) {
        return new ResolvableMappingMethod(this.ownerHandle, newName, this.getDesc());
    }
    
    @Override
    public MappingMethod transform(String newDesc) {
        return new ResolvableMappingMethod(this.ownerHandle, this.getSimpleName(), newDesc);
    }

    @Override
    public MappingMethod copy() {
        return new ResolvableMappingMethod(this.ownerHandle, this.getSimpleName(), this.getDesc());
    }

}
