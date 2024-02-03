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
package org.spongepowered.asm.mixin.injection.code;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;

/**
 * Interface for extensions for InsnList which provide additional context for
 * the InsnList. This is mainly to allow passing additional information to
 * <tt>InjectionPoint::{@link InjectionPoint#find}</tt> without breaking
 * backward compatibility.
 */
public interface IInsnListEx {
    
    /**
     * Type of special nodes supported by {@link #getSpecialNode}
     */
    public enum SpecialNodeType {
        
        /**
         * The delegate constructor call in a constructor
         */
        DELEGATE_CTOR,
        
        /**
         * The location for injected initialisers in a constructor 
         */
        INITIALISER_INJECTION_POINT,
        
        /**
         * The location after field initialisers but before the first
         * constructor body instruction, requires line numbers to be present in
         * the target class
         */
        CTOR_BODY
        
    }
    
    /**
     * Get the name of the target method
     */
    public abstract String getTargetName();
    
    /**
     * Get the descriptor of the target method
     */
    public abstract String getTargetDesc();
    
    /**
     * Get the signature of the target method
     */
    public abstract String getTargetSignature();
    
    /**
     * Get the access flags from the target method
     */
    public abstract int getTargetAccess();
    
    /**
     * Get whether the target method is static
     */
    public abstract boolean isTargetStatic();
    
    /**
     * Get whether the target method is a constructor
     */
    public abstract boolean isTargetConstructor();
    
    /**
     * Get whether the target method is a static initialiser
     */
    public abstract boolean isTargetStaticInitialiser();

    /**
     * Get - if available - the specified special node from the target. The
     * returned node is not guaranteed to be non-null.
     * 
     * @param type type of special node to fetch
     * @return the special node or null if not available
     */
    public abstract AbstractInsnNode getSpecialNode(SpecialNodeType type);
}
