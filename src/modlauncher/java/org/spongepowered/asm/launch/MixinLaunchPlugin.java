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

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.CommandLineOptions;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.service.modlauncher.MixinServiceModLauncher;
import org.spongepowered.asm.service.modlauncher.ModLauncherAuditTrail;
import org.spongepowered.asm.transformers.MixinClassReader;

import com.google.common.io.Resources;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;

/**
 * Mixin launch plugin 
 */
public class MixinLaunchPlugin implements ILaunchPluginService, IClassBytecodeProvider {
    
    /**
     * Name used for ModLauncher mixin service components
     */
    public static final String NAME = "mixin";
    
    /**
     * Class processing components
     */
    private final List<IClassProcessor> processors = new ArrayList<IClassProcessor>();

    /**
     * Mixin config names specified on the command line 
     */
    private List<String> commandLineMixins;

    private ITransformerLoader transformerLoader;
    
    private MixinServiceModLauncher service;
    
    private ModLauncherAuditTrail auditTrail;
    
    /* (non-Javadoc)
     * @see cpw.mods.modlauncher.serviceapi.ILaunchPluginService#name()
     */
    @Override
    public String name() {
        return MixinLaunchPlugin.NAME;
    }
    
    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
        throw new IllegalStateException("Outdated ModLauncher");
    }
    
    @Override
    public boolean processClass(Phase phase, ClassNode classNode, Type classType) {
        throw new IllegalStateException("Outdated ModLauncher");
    }
    
    /* (non-Javadoc)
     * @see cpw.mods.modlauncher.serviceapi.ILaunchPluginService#handlesClass(
     *      org.objectweb.asm.Type, boolean, java.lang.String)
     */
    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty, final String reason) {
        if (MixinLaunchPlugin.NAME.equals(reason)) {
            return Phases.NONE;
        }
        
        // All processors can nominate phases, we aggregate the results
        EnumSet<Phase> phases = EnumSet.<Phase>noneOf(Phase.class);
        synchronized (this.processors) {
            for (IClassProcessor postProcessor : this.processors) {
                EnumSet<Phase> processorVote = postProcessor.handlesClass(classType, isEmpty, reason);
                if (processorVote != null) {
                    phases.addAll(processorVote);
                }
            }
        }
        
        return phases;
    }

    /* (non-Javadoc)
     * @see cpw.mods.modlauncher.serviceapi.ILaunchPluginService#processClass(
     *      cpw.mods.modlauncher.serviceapi.ILaunchPluginService.Phase,
     *      org.objectweb.asm.tree.ClassNode, org.objectweb.asm.Type,
     *      java.lang.String)
     */
    @Override
    public boolean processClass(Phase phase, ClassNode classNode, Type classType, String reason) {
        boolean processed = false;
        
        synchronized (this.processors) {
            for (IClassProcessor postProcessor : this.processors) {
                processed |= postProcessor.processClass(phase, classNode, classType, reason);
            }
        }
        
        return processed;
    }
    
    /**
     * Initialisation routine, called as a lifecycle event from the
     * transformation service
     */
    void init(IEnvironment environment, List<String> commandLineMixins) {
        IMixinService service = MixinService.getService();
        if (!(service instanceof MixinServiceModLauncher)) {
            throw new IllegalStateException("Unsupported service type for ModLauncher Mixin Service");
        }
        this.service = (MixinServiceModLauncher)service;
        this.auditTrail = (ModLauncherAuditTrail)this.service.getAuditTrail();
        synchronized (this.processors) {
            this.processors.addAll(this.service.getProcessors());
        }
        this.commandLineMixins = commandLineMixins;
        this.service.onInit(this);
    }
    
    /* (non-Javadoc)
     * @see cpw.mods.modlauncher.serviceapi.ILaunchPluginService
     *      #customAuditConsumer(java.lang.String, java.util.function.Consumer)
     */
    @Override
    public void customAuditConsumer(String className, Consumer<String[]> auditDataAcceptor) {
        if (this.auditTrail != null) {
            this.auditTrail.setConsumer(className, auditDataAcceptor);
        }
    }

    // @Override ModLauncher 4.0
    @Deprecated
    public void addResource(Path resource, String name) {
        this.service.getPrimaryContainer().addResource(name, resource);
    }
    
    // ModLauncher 7.0+
    @Override
    public void offerResource(Path resource, String name) {
        this.service.getPrimaryContainer().addResource(name, resource);
    }

    @Override
    public void addResources(List<Entry<String, Path>> resources) {
        this.service.getPrimaryContainer().addResources(resources);
    }

    /* (non-Javadoc)
     * @see cpw.mods.modlauncher.serviceapi.ILaunchPluginService#getExtension()
     */
    @Override
    public <T> T getExtension() {
        return null;
    }
    
    @Override
    public void initializeLaunch(ITransformerLoader transformerLoader, Path[] specialPaths) {
        this.transformerLoader = transformerLoader;
        MixinBootstrap.doInit(CommandLineOptions.of(this.commandLineMixins));
        MixinBootstrap.inject();
        this.service.onStartup();
    }

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        return this.getClassNode(name, true);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        if (!runTransformers) {
            throw new IllegalArgumentException("ModLauncher service does not currently support retrieval of untransformed bytecode");
        }
        
        byte[] classBytes;
        
        try {
            classBytes = this.transformerLoader.buildTransformedClassNodeFor(name);
        } catch (ClassNotFoundException ex) {
            URL url = Thread.currentThread().getContextClassLoader().getResource(name.replace('.', '/') + ".class");
            if (url == null) {
                throw ex;
            }
            try {
                classBytes = Resources.asByteSource(url).read();
            } catch (IOException ioex) {
                throw ex;
            }
        }
        
        if (classBytes == null) {
            throw new ClassNotFoundException(name.replace('/', '.'));
        }

        ClassNode classNode = new ClassNode();
        ClassReader classReader = new MixinClassReader(classBytes, name);
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
        return classNode;
    }

}
 