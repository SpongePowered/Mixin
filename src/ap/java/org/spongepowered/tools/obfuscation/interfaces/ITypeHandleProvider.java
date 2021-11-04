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

import javax.lang.model.type.TypeMirror;

import org.spongepowered.tools.obfuscation.mirror.TypeHandle;

/**
 * Manager object which cann supply {@link TypeHandle} instances
 */
public interface ITypeHandleProvider {

    /**
     * Generate a type handle for the specified type
     * 
     * @param name Type name (class name)
     * @return A new type handle or null if the type could not be found
     */
    public abstract TypeHandle getTypeHandle(String name);
    
    /**
     * Get a type handle for the specified type
     * 
     * @param type Type source, can be a String, TypeMirror, TypeElement or an
     *      existing TypeHandle
     * @return TypeHandle
     */
    public abstract TypeHandle getTypeHandle(Object type);

    /**
     * Generate a type handle for the specified type, simulate the target using
     * the supplied type
     * 
     * @param name Type name (class name)
     * @param simulatedTarget Simulation target
     * @return A new type handle
     */
    public abstract TypeHandle getSimulatedHandle(String name, TypeMirror simulatedTarget);
    
}
