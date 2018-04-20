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
package org.spongepowered.asm.mixin.refmap;

import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;

/**
 * Context for performing reference mapping
 */
public interface IMixinContext {
    
    /**
     * Get the mixin info
     */
    public abstract IMixinInfo getMixin();
    
    /**
     * Get the mixin transformer extension manager
     */
    public abstract Extensions getExtensions();
    
    /**
     * Get the mixin class name
     * 
     * @return the mixin class name
     */
    public abstract String getClassName();

    /**
     * Get the internal mixin class name
     * 
     * @return internal class name
     */
    public abstract String getClassRef();

    /**
     * Get the internal name of the target class for this context
     * 
     * @return internal target class name
     */
    public abstract String getTargetClassRef();

    /**
     * Get the reference mapper for this mixin
     * 
     * @return ReferenceMapper instance (can be null)
     */
    public abstract IReferenceMapper getReferenceMapper();

    /**
     * Retrieve the value of the specified <tt>option</tt> from the environment
     * this mixin belongs to.
     * 
     * @param option option to check
     * @return option value
     */
    public abstract boolean getOption(Option option);

    /**
     * Get the priority of the mixin
     */
    public abstract int getPriority();

    /**
     * Obtain a {@link Target} method handle for a method in the target, this is
     * used by consumers to manipulate the bytecode in a target method in a
     * controlled manner.
     * 
     * @param method method node to wrap
     * @return target method
     */
    public abstract Target getTargetMethod(MethodNode method);

}
