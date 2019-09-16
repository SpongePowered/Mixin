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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.service.modlauncher.MixinServiceModLauncher;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;

/**
 * Mixin launch plugin 
 */
public class MixinLaunchPlugin implements ILaunchPluginService {
    
    /**
     * Name used for ModLauncher mixin service components
     */
    public static final String NAME = "mixin";
    
    /**
     * Interceptor if present 
     */
    private IClassInterceptor interceptor;
    
    /**
     * Class processing components
     */
    private List<IClassProcessor> processors = new ArrayList<IClassProcessor>();
    
    /* (non-Javadoc)
     * @see cpw.mods.modlauncher.serviceapi.ILaunchPluginService#name()
     */
    @Override
    public String name() {
        return MixinLaunchPlugin.NAME;
    }
    
    /* (non-Javadoc)
     * @see cpw.mods.modlauncher.serviceapi.ILaunchPluginService
     *      #handlesClass(org.objectweb.asm.Type, boolean)
     */
    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
        if (this.interceptor != null) {
            // If the interceptor wants to handle this class, it has executive
            // power since this entry is almost certainly simulated. If it's 
            // not a simulated entry then something is probably not right. RIP?
            EnumSet<Phase> interceptorVote = this.interceptor.handlesClass(classType, isEmpty);
            if (interceptorVote != null) {
                return interceptorVote;
            }
        }

        // All processors can nominate phases, we aggregate the results
        EnumSet<Phase> phases = EnumSet.<Phase>noneOf(Phase.class);
        synchronized (this.processors) {
            for (IClassProcessor postProcessor : this.processors) {
                EnumSet<Phase> processorVote = postProcessor.handlesClass(classType, isEmpty);
                if (processorVote != null) {
                    phases.addAll(processorVote);
                }
            }
        }
        
        return phases;
    }

    /* (non-Javadoc)
     * @see cpw.mods.modlauncher.serviceapi.ILaunchPluginService
     *      #processClass(
     *      cpw.mods.modlauncher.serviceapi.ILaunchPluginService.Phase,
     *      org.objectweb.asm.tree.ClassNode, org.objectweb.asm.Type)
     */
    @Override
    public boolean processClass(Phase phase, ClassNode classNode, Type classType) {
        if (this.interceptor != null) {
            if (this.interceptor.processClass(phase, classNode, classType)) {
                // If the interceptor handles the class, then this is likely a
                // simulated entry. Return immediately and don't delegate to
                // other processors
                return true;
            }
        }
        
        boolean processed = false;
        
        synchronized (this.processors) {
            for (IClassProcessor postProcessor : this.processors) {
                processed |= postProcessor.processClass(phase, classNode, classType);
            }
        }
        
        return processed;
    }

    /* (non-Javadoc)
     * @see cpw.mods.modlauncher.serviceapi.ILaunchPluginService#addResource(
     *      java.nio.file.Path, java.lang.String)
     */
    @Override
    public void addResource(Path resource, String name) {
        // This is never called, I'm not sure what it's for
    }

    /* (non-Javadoc)
     * @see cpw.mods.modlauncher.serviceapi.ILaunchPluginService#getExtension()
     */
    @Override
    public <T> T getExtension() {
        return null;
    }
    
    /**
     * Initialisation routine, called as a lifecycle event from the
     * transformation service
     */
    void init(IEnvironment environment, Runnable startupListener) {
        IMixinService service = MixinService.getService();
        if (!(service instanceof MixinServiceModLauncher)) {
            throw new IllegalStateException("Unsupported service type for ModLauncher Mixin Service");
        }
        IClassBytecodeProvider bytecodeProvider = service.getBytecodeProvider();
        if (bytecodeProvider instanceof IClassInterceptor) {
            this.interceptor = (IClassInterceptor)bytecodeProvider;
        }
        MixinServiceModLauncher modLauncherService = (MixinServiceModLauncher)service;
        synchronized (this.processors) {
            this.processors.addAll(modLauncherService.getProcessors());
        }
        modLauncherService.onInit(startupListener);
    }

}
 