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
package org.spongepowered.asm.service.modlauncher;

import java.util.EnumSet;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.IClassProcessor;
import org.spongepowered.asm.launch.MixinLaunchPlugin;
import org.spongepowered.asm.launch.Phases;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.ISyntheticClassInfo;
import org.spongepowered.asm.service.ISyntheticClassRegistry;

import com.google.common.base.Preconditions;

import cpw.mods.modlauncher.api.ITransformerActivity;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService.Phase;

/**
 * Class Processor which delegates to the main Mixin pipeline for mixin
 * application, post processing and synthetic class generation
 */
public class MixinTransformationHandler implements IClassProcessor {
    
    /**
     * Mixin transformer factory, from service
     */
    private IMixinTransformerFactory transformerFactory;
    
    /**
     * Transformer pipeline instance
     */
    private IMixinTransformer transformer;

    /**
     * Synthetic class registry, used so the processor knows when to respond to
     * empty class population requests
     */
    private ISyntheticClassRegistry registry;
    
    void offer(IMixinTransformerFactory transformerFactory) {
        Preconditions.checkNotNull(transformerFactory, "transformerFactory");
        this.transformerFactory = transformerFactory;
    }

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

        if (this.transformer == null) {
            if (this.transformerFactory == null) {
                throw new IllegalStateException("processClass called before transformer factory offered to transformation handler");
            }
            this.transformer = this.transformerFactory.createTransformer();
            this.registry = this.transformer.getExtensions().getSyntheticClassRegistry();
        }
        
        // Don't transform when the reason is mixin (side-loading in progress) 
        if (MixinLaunchPlugin.NAME.equals(reason)) {
            return false;
        }

        MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();
        ISyntheticClassInfo syntheticClass = this.registry.findSyntheticClass(classType.getClassName());
        if (syntheticClass != null) {
            return this.transformer.generateClass(environment, classType.getClassName(), classNode);
        }

        if (ITransformerActivity.COMPUTING_FRAMES_REASON.equals(reason)) {
            return this.transformer.computeFramesForClass(environment, classType.getClassName(), classNode);
        }
        
        return this.transformer.transformClass(environment, classType.getClassName(), classNode);
    }

}
