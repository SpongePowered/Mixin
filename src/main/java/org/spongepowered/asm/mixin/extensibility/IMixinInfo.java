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
package org.spongepowered.asm.mixin.extensibility;

import java.util.List;

import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;

/**
 * Interface for MixinInfo, used in extensibility API
 */
public interface IMixinInfo {
    
    /**
     * Get the config to which this mixin belongs
     * 
     * @return the mixin config
     */
    public IMixinConfig getConfig();
    
    /**
     * Get the simple name of the mixin
     * 
     * @return the simple name (mixin tail minus the package)
     */
    public abstract String getName();

    /**
     * Get the name of the mixin class
     * 
     * @return mixin class name
     */
    public abstract String getClassName();

   /**
    * Get the ref (internal name) of the mixin class
    * 
    * @return mixin class ref (internal name)
    */
    public abstract String getClassRef();

    /**
     * Get the class bytecode
     * 
     * @return mixin bytecode (raw bytecode after transformers)
     */
    public abstract byte[] getClassBytes();

    /**
     * True if the superclass of the mixin is <b>not</b> the direct superclass
     * of one or more targets.
     * 
     * @return true if the mixin has a detached superclass
     */
    public abstract boolean isDetachedSuper();

    /**
     * Get a new tree for the class bytecode
     * 
     * @param flags Flags to pass to the ClassReader
     * @return get a new ClassNode representing the mixin's bytecode
     */
    public abstract ClassNode getClassNode(int flags);

    /**
     * Get the target classes for this mixin
     * 
     * @return list of target classes
     */
    public abstract List<String> getTargetClasses();

    /**
     * Get the mixin priority
     * 
     * @return the priority
     */
    public abstract int getPriority();

    /**
     * Get the mixin phase
     * 
     * @return the phase
     */
    public abstract Phase getPhase();
    
}
