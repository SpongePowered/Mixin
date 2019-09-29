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
package org.spongepowered.asm.mixin.transformer;

import java.util.EnumSet;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.IClassProcessor;
import org.spongepowered.asm.launch.Phases;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.service.ISyntheticClassInfo;
import org.spongepowered.asm.service.ISyntheticClassRegistry;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService.Phase;

/**
 * Class Processor which delegates to the main Mixin pipeline for mixin
 * application, post processing and synthetic class generation
 */
public class MixinTransformationHandler implements IClassProcessor {

    /**
     * Lock for initialising the transformer
     */
    private final Object initialisationLock = new Object();
    
    /**
     * Transformer pipeline instance
     */
    private MixinTransformer transformer;

    /**
     * Synthetic class registry, used so the processor knows when to respond to
     * empty class population requests
     */
    private ISyntheticClassRegistry registry;
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.IClassProcessor#handlesClass(
     *      org.objectweb.asm.Type, boolean, java.lang.String)
     */
    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty, String reason) {
        if (!isEmpty) {
            return Phases.AFTER_ONLY;
        }
        
        if (this.registry == null) {
            return null;
        }            
        
        ISyntheticClassInfo syntheticClass = this.registry.findSyntheticClass(classType.getClassName());
        return syntheticClass != null ? Phases.AFTER_ONLY : null;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.IClassProcessor#processClass(
     *      cpw.mods.modlauncher.serviceapi.ILaunchPluginService.Phase,
     *      org.objectweb.asm.tree.ClassNode, org.objectweb.asm.Type,
     *      java.lang.String)
     */
    @Override
    public synchronized boolean processClass(Phase phase, ClassNode classNode, Type classType, String reason) {
        if (phase == Phase.BEFORE) {
            return false;
        }
        
        MixinTransformer transformer = null;
        if (this.transformer == null) {
            synchronized (this.initialisationLock) {
                transformer = this.transformer;
                if (transformer == null) {
                    transformer = this.transformer = new MixinTransformer();
                    this.registry = transformer.getExtensions().getSyntheticClassRegistry();
                }
            }
        } else {
            transformer = this.transformer;
        }

        MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();
        ISyntheticClassInfo syntheticClass = this.registry.findSyntheticClass(classType.getClassName());
        if (syntheticClass != null) {
            return transformer.generateClass(environment, classType.getClassName(), classNode);
        }
        return transformer.transformClass(environment, classType.getClassName(), classNode);
    }
}
