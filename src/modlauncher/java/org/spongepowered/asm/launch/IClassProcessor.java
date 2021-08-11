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
package org.spongepowered.asm.launch;

import java.util.EnumSet;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService.Phase;

/**
 * Interface for shunting classes through other parts of the Mixin subsystem
 * when they arrive at the plugin
 */
public interface IClassProcessor {
    
    /**
     * If this class processor wants to receive the {@link ClassNode} into
     * {@link #processClass}. Returning <tt>null</tt> is allowed in delegate to
     * later processors.
     *      
     * @param classType the class to consider
     * @param isEmpty if the class is empty at present (indicates no backing
     *      file found)
     * @param reason the reason for processing
     * @return the set of Phases the plugin wishes to be called back with
     */
    public abstract EnumSet<Phase> handlesClass(Type classType, boolean isEmpty, String reason);

    
    /**
     * Callback from the mixin plugin
     * 
     * @param phase The phase of the supplied class node
     * @param classNode the classnode to process
     * @param classType the name of the class
     * @param reason the reason for processing
     * @return true if the class was processed
     */
    public abstract boolean processClass(Phase phase, ClassNode classNode, Type classType, String reason);
    
    /**
     * Returns whether this generator can generate the specified class
     * 
     * @param classType Class to generate
     * @return true if this generator can generate the class
     */
    public abstract boolean generatesClass(Type classType);
    
    /**
     * Generate the specified class
     * 
     * @param classType Class to generate
     * @param classNode ClassNode to populate with the new class data
     * @return true if the class was generated
     */
    public abstract boolean generateClass(Type classType, ClassNode classNode);

}
