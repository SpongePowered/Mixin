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
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Optional;

import org.spongepowered.asm.launch.IClassProcessor;
import org.spongepowered.asm.launch.platform.container.ContainerHandleModLauncher;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment.CompatibilityLevel;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.IMixinInternal;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.service.MixinServiceAbstract;
import org.spongepowered.asm.util.IConsumer;

import com.google.common.collect.ImmutableList;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.TypesafeMap;
import org.spongepowered.asm.util.VersionNumber;

/**
 * Mixin service for ModLauncher
 */
public class MixinServiceModLauncher extends MixinServiceAbstract {
    
    /**
     * Specification version to check for at startup
     */
    private static final VersionNumber MODLAUNCHER_4_SPECIFICATION_VERSION = VersionNumber.parse("4.0");
    
    /**
     * Specification version for ModLauncher versions &gt;= 9.0.4, yes this is
     * not a typo, the specification version (API version) is out of step with
     * the artefact version in a few previous cases (ML6 has specification
     * version 5.0 for example, and ML7 and ML8 both had specification version
     * 7.0).
     */
    private static final VersionNumber MODLAUNCHER_9_SPECIFICATION_VERSION = VersionNumber.parse("8.0");

    private static final String CONTAINER_PACKAGE = MixinServiceAbstract.LAUNCH_PACKAGE + "platform.container.";
    private static final String MODLAUNCHER_4_ROOT_CONTAINER_CLASS = MixinServiceModLauncher.CONTAINER_PACKAGE + "ContainerHandleModLauncher";
    private static final String MODLAUNCHER_9_ROOT_CONTAINER_CLASS = MixinServiceModLauncher.CONTAINER_PACKAGE + "ContainerHandleModLauncherEx";

    /**
     * Class provider, either uses hacky internals or provided service
     */
    private IClassProvider classProvider;
    
    /**
     * Bytecode provider, either uses hacky internals or provided service
     */
    private IClassBytecodeProvider bytecodeProvider;
    
    /**
     * Container for the mixin pipeline which is called by the launch plugin
     */
    private MixinTransformationHandler transformationHandler;
    
    /**
     * Class tracker, tracks class loads and registered invalid classes
     */
    private ModLauncherClassTracker classTracker;
    
    /**
     * Audit trail adapter 
     */
    private ModLauncherAuditTrail auditTrail;

    /**
     * Environment phase consumer, TEMP
     */
    private IConsumer<Phase> phaseConsumer;
    
    /**
     * Only allow onInit to be called once
     */
    private volatile boolean initialised;

    /**
     * Root container
     */
    private ContainerHandleModLauncher rootContainer;

    /**
     * Minimum compatibility level
     */
    private CompatibilityLevel minCompatibilityLevel = CompatibilityLevel.JAVA_8;
    
    public MixinServiceModLauncher() {
        VersionNumber apiVersion = MixinServiceModLauncher.getModLauncherApiVersion();
        if (apiVersion.compareTo(MODLAUNCHER_9_SPECIFICATION_VERSION) >= 0) {
            this.createRootContainer(MixinServiceModLauncher.MODLAUNCHER_9_ROOT_CONTAINER_CLASS);
            this.minCompatibilityLevel = CompatibilityLevel.JAVA_16;
        } else {
            this.createRootContainer(MixinServiceModLauncher.MODLAUNCHER_4_ROOT_CONTAINER_CLASS);
        }
    }

    /**
     * Begin init
     * 
     * @param bytecodeProvider bytecode provider
     */
    public void onInit(IClassBytecodeProvider bytecodeProvider) {
        if (this.initialised) {
            throw new IllegalStateException("Already initialised");
        }
        this.initialised = true;
        this.bytecodeProvider = bytecodeProvider;
    }
    
    private void createRootContainer(String rootContainerClassName) {
        try {
            Class<?> clRootContainer = this.getClassProvider().findClass(rootContainerClassName);
            Constructor<?> ctor = clRootContainer.getDeclaredConstructor(String.class);
            this.rootContainer = (ContainerHandleModLauncher)ctor.newInstance(this.getName());
        } catch (ReflectiveOperationException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Lifecycle event
     */
    public void onStartup() {
        this.phaseConsumer.accept(Phase.DEFAULT);
    }
    
    @Override
    public void offer(IMixinInternal internal) {
        if (internal instanceof IMixinTransformerFactory) {
            this.getTransformationHandler().offer((IMixinTransformerFactory)internal);
        }
        super.offer(internal);
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
     * @see org.spongepowered.asm.service.IMixinService
     *      #getMinCompatibilityLevel()
     */
    @Override
    public CompatibilityLevel getMinCompatibilityLevel() {
        return this.minCompatibilityLevel;
    }
    
    @Override
    protected ILogger createLogger(String name) {
        return new LoggerAdapterLog4j2(name);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#isValid()
     */
    @Override
    public boolean isValid() {
        try {
            VersionNumber apiVersion = MixinServiceModLauncher.getModLauncherApiVersion();
            if (apiVersion.compareTo(MixinServiceModLauncher.MODLAUNCHER_4_SPECIFICATION_VERSION) < 0) {
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
            this.classProvider = new ModLauncherClassProvider();
        }
        return this.classProvider;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getBytecodeProvider()
     */
    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        if (this.bytecodeProvider == null) {
            throw new IllegalStateException("Service initialisation incomplete");
        }
        return this.bytecodeProvider;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getTransformerProvider()
     */
    @Override
    public ITransformerProvider getTransformerProvider() {
        return null;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getClassTracker()
     */
    @Override
    public IClassTracker getClassTracker() {
        if (this.classTracker == null) {
            this.classTracker = new ModLauncherClassTracker();
        }
        return this.classTracker;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getAuditTrail()
     */
    @Override
    public IMixinAuditTrail getAuditTrail() {
        if (this.auditTrail == null) {
            this.auditTrail = new ModLauncherAuditTrail();
        }
        return this.auditTrail;
    }

    /**
     * Get (or create) the transformation handler
     */
    private MixinTransformationHandler getTransformationHandler() {
        if (this.transformationHandler == null) {
            this.transformationHandler = new MixinTransformationHandler();
        }
        return this.transformationHandler;
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
    public ContainerHandleModLauncher getPrimaryContainer() {
        return this.rootContainer;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getResourceAsStream(
     *      java.lang.String)
     */
    @Override
    public InputStream getResourceAsStream(String name) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
    }

    /**
     * Internal method to retrieve the class processors which will be called by
     * the launch plugin
     */
    public Collection<IClassProcessor> getProcessors() {
        return ImmutableList.<IClassProcessor>of(
            this.getTransformationHandler(),
            (IClassProcessor)this.getClassTracker()
        );
    }

    private static VersionNumber getModLauncherApiVersion() {
        TypesafeMap.Key<String> versionProperty = IEnvironment.Keys.MLSPEC_VERSION.get();
        Optional<String> version = Launcher.INSTANCE.environment().getProperty(versionProperty);

        // Fall back to the package information (this is not present when loaded as a module)
        if (!version.isPresent()) {
            version = Optional.ofNullable(ITransformationService.class.getPackage().getSpecificationVersion());
        }

        return version.map(VersionNumber::parse).orElse(VersionNumber.NONE);
    }

}
