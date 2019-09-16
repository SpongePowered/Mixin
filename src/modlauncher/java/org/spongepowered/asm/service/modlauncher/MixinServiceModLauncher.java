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

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.spongepowered.asm.launch.IClassProcessor;
import org.spongepowered.asm.launch.platform.container.ContainerHandleModLauncher;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.transformer.MixinTransformationHandler;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.ITransformer;
import org.spongepowered.asm.service.MixinServiceAbstract;
import org.spongepowered.asm.service.modlauncher.ext.IModLauncherClassBytecodeProvider;
import org.spongepowered.asm.service.modlauncher.ext.IModLauncherClassProvider;
import org.spongepowered.asm.util.IConsumer;

import com.google.common.collect.ImmutableList;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.TransformingClassLoader;
import cpw.mods.modlauncher.api.ITransformationService;

/**
 * Mixin service for ModLauncher
 */
public class MixinServiceModLauncher extends MixinServiceAbstract {
    
    /**
     * Specification version to check for at startup
     */
    private static final String MODLAUNCHER_SPECIFICATION_VERSION = "1.0";

    /**
     * Class provider, either uses hacky internals or provided service
     */
    private IModLauncherClassProvider classProvider;
    
    /**
     * Bytecode provider, either uses hacky internals or provided service
     */
    private IModLauncherClassBytecodeProvider bytecodeProvider;
    
    /**
     * Container for the mixin pipeline which is called by the launch plugin
     */
    private MixinTransformationHandler transformationHandler;
    
    /**
     * Class tracker, tracks class loads and registered invalid classes
     */
    private ModLauncherClassTracker classTracker;

    /**
     * Environment phase consumer, TEMP
     */
    private IConsumer<Phase> phaseConsumer;
    
    /**
     * Only allow onInit to be called once
     */
    private volatile boolean initialised;
    
    /**
     * Begin init
     * 
     * @param startupListener Lifecyle listener
     */
    public void onInit(Runnable startupListener) {
        if (this.initialised) {
            throw new IllegalStateException("Already initialised");
        }
        this.initialised = true;
        Internals.getInstance().registerStartupListener(startupListener);
        Internals.getInstance().registerStartupListener(MixinServiceModLauncher.this::onStartup);
    }
    
    /**
     * Lifecycle event
     */
    private void onStartup() {
        this.phaseConsumer.accept(Phase.DEFAULT);
    }

    // TEMP
    @SuppressWarnings("deprecation")
    @Override
    public void wire(Phase phase, IConsumer<Phase> phaseConsumer) {
        super.wire(phase, phaseConsumer);
        this.phaseConsumer = phaseConsumer;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getName()
     */
    @Override
    public String getName() {
        return "ModLauncher";
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#isValid()
     */
    @Override
    public boolean isValid() {
        try {
            Launcher.INSTANCE.hashCode();
            final Package pkg = ITransformationService.class.getPackage();
            if (!pkg.isCompatibleWith(MixinServiceModLauncher.MODLAUNCHER_SPECIFICATION_VERSION)) {
                return false;
            }
        } catch (Throwable th) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getClassProvider()
     */
    @Override
    public IClassProvider getClassProvider() {
        if (this.classProvider == null) {
            this.classProvider = MixinServiceModLauncher.createClassProvider();
        }
        return this.classProvider;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getBytecodeProvider()
     */
    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        if (this.bytecodeProvider == null) {
            this.bytecodeProvider = MixinServiceModLauncher.createBytecodeProvider();
        }
        return this.bytecodeProvider;
    }

    /**
     * Get (or create) the transformation handler
     */
    private IClassProcessor getTransformationHandler() {
        if (this.transformationHandler == null) {
            this.transformationHandler = new MixinTransformationHandler();
        }
        return this.transformationHandler;
    }

    /**
     * Get (or create) the class tracker
     */
    private ModLauncherClassTracker getClassTracker() {
        if (this.classTracker == null) {
            this.classTracker = new ModLauncherClassTracker();
        }
        return this.classTracker;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getPlatformAgents()
     */
    @Override
    public Collection<String> getPlatformAgents() {
        return ImmutableList.<String>of(
            "org.spongepowered.asm.launch.platform.MixinPlatformAgentMinecraftForge"
        );
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getPrimaryContainer()
     */
    @Override
    public IContainerHandle getPrimaryContainer() {
        return new ContainerHandleModLauncher(this.getName());
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getResourceAsStream(
     *      java.lang.String)
     */
    @Override
    public InputStream getResourceAsStream(String name) {
        TransformingClassLoader tcl = Internals.getInstance().getTransformingClassLoader();
        if (tcl != null) {
            return tcl.getResourceAsStream(name);
        }
        
        // Probably not what we want :/
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#registerInvalidClass(
     *      java.lang.String)
     */
    @Override
    public void registerInvalidClass(String className) {
        this.getClassTracker().registerInvalidClass(className);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#isClassLoaded(
     *      java.lang.String)
     */
    @Override
    public boolean isClassLoaded(String className) {
        return this.getClassTracker().isClassLoaded(className);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getClassRestrictions(
     *      java.lang.String)
     */
    @Override
    public String getClassRestrictions(String className) {
        return this.getClassTracker().getClassRestrictions(className);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getTransformers()
     */
    @Override
    public Collection<ITransformer> getTransformers() {
        return ImmutableList.<ITransformer>of();
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService
     *      #getDelegatedTransformers()
     */
    @Override
    public Collection<ITransformer> getDelegatedTransformers() {
        return ImmutableList.<ITransformer>of();
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#addTransformerExclusion(
     *      java.lang.String)
     */
    @Override
    public void addTransformerExclusion(String name) {
        // TODO ?
    }

    /**
     * Internal method to retrieve the class processors which will be called by
     * the launch plugin
     */
    public Collection<IClassProcessor> getProcessors() {
        return ImmutableList.<IClassProcessor>of(
            this.getTransformationHandler(),
            this.getClassTracker()
        );
    }

    private static IModLauncherClassProvider createClassProvider() {
        IModLauncherClassProvider service =
                MixinServiceModLauncher.<IModLauncherClassProvider>loadFirstAvailableService(IModLauncherClassProvider.class);
        return service != null ? service : new ModLauncherClassProvider();
    }

    private static IModLauncherClassBytecodeProvider createBytecodeProvider() {
        IModLauncherClassBytecodeProvider service =
                MixinServiceModLauncher.<IModLauncherClassBytecodeProvider>loadFirstAvailableService(IModLauncherClassBytecodeProvider.class);
        return service != null ? service : new ModLauncherBytecodeProvider();
    }

    private static <TService> TService loadFirstAvailableService(Class<TService> serviceClass) {
        Iterator<TService> loader = ServiceLoader.<TService>load(serviceClass).iterator();
        if (loader.hasNext()) {
            try {
                return loader.next();
            } catch (ServiceConfigurationError ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

}
