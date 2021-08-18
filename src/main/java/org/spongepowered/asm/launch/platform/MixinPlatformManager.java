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
package org.spongepowered.asm.launch.platform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.CompatibilityLevel;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.service.ServiceVersionError;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.throwables.MixinError;

//import com.google.common.collect.ImmutableList;

/**
 * Handler for platform-specific behaviour required in different mixin
 * environments.
 */
public class MixinPlatformManager {

    private static final String DEFAULT_MAIN_CLASS = "net.minecraft.client.main.Main";
    
    /**
     * Make with the logging already
     */
    private static final ILogger logger = MixinService.getService().getLogger("mixin");
    
    /**
     * Bootstrap delegate 
     */
//    private final Delegate delegate;
    
    /**
     * Tweak containers
     */
    private final Map<IContainerHandle, MixinContainer> containers = new LinkedHashMap<IContainerHandle, MixinContainer>();
    
    /**
     * Connectors 
     */
    private final MixinConnectorManager connectors = new MixinConnectorManager();

    
    /**
     * Container for this tweaker 
     */
    private MixinContainer primaryContainer;
    
    /**
     * Tracks whether {@link #acceptOptions} was called yet, if true, causes new
     * agents to be <tt>prepare</tt>d immediately 
     */
    private boolean prepared = false;
    
    /**
     * Tracks whether {@link #inject} was called yet
     */
    private boolean injected;
    
    public MixinPlatformManager() { //Delegate delegate) {
//        this.delegate = delegate;
    }
    
    /**
     * Initialise the platform manager
     */
    public void init() {
        MixinPlatformManager.logger.debug("Initialising Mixin Platform Manager");

        IContainerHandle primaryContainerHandle = MixinService.getService().getPrimaryContainer();
        this.primaryContainer = this.addContainer(primaryContainerHandle);

        // Do an early scan to ensure preinit mixins are discovered
        this.scanForContainers();
    }
    
    /**
     * Get the phase provider classes from the primary container
     */
    public Collection<String> getPhaseProviderClasses() {
        Collection<String> phaseProviders = this.primaryContainer.getPhaseProviders();
        if (phaseProviders != null) {
            return Collections.<String>unmodifiableCollection(phaseProviders);
        }
        
        return Collections.<String>emptyList();
    }

    /**
     * Add a new container to this platform and return the new container (or an
     * existing container if the handle was previously registered)
     * 
     * @param handle Container handle to add
     * @return container for specified resource handle
     */
    public final MixinContainer addContainer(IContainerHandle handle) {
        MixinContainer existingContainer = this.containers.get(handle);
        if (existingContainer != null) {
            return existingContainer;
        }
        
        MixinContainer container = this.createContainerFor(handle);
        this.containers.put(handle, container);
        this.addNestedContainers(handle);
        return container;
    }

    private MixinContainer createContainerFor(IContainerHandle handle) {
        MixinPlatformManager.logger.debug("Adding mixin platform agents for container {}", handle);
        MixinContainer container = new MixinContainer(this, handle);
        if (this.prepared) {
            container.prepare();
        }
        return container;
    }

    private void addNestedContainers(IContainerHandle handle) {
        for (IContainerHandle nested : handle.getNestedContainers()) {
            if (!this.containers.containsKey(nested)) {
                this.addContainer(nested);
            }
        }
    }

    /**
     * Prepare all containers in this platform
     * 
     * @param args command-line arguments from tweaker
     */
    public final void prepare(CommandLineOptions args) {
        this.prepared = true;
        for (MixinContainer container : this.containers.values()) {
            container.prepare();
        }
        
        for (String config : args.getConfigs()) {
            this.addConfig(config);
        }
    }

    /**
     * Initialise the primary container and dispatch inject to all containers
     */
    public final void inject() {
        if (this.injected) {
            return;
        }
        this.injected = true;
        
        if (this.primaryContainer != null) {
            this.primaryContainer.initPrimaryContainer();
        }
        
        this.scanForContainers();
        MixinPlatformManager.logger.debug("inject() running with {} agents", this.containers.size());
        for (MixinContainer container : this.containers.values()) {
            try {
                container.inject();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        this.connectors.inject();
    }

    /**
     * Scan the current environment for mixin containers and add agents for them
     */
    private void scanForContainers() {
        Collection<IContainerHandle> mixinContainers = null;
        
        try {
            mixinContainers = MixinService.getService().getMixinContainers();
        } catch (AbstractMethodError ame) {
            throw new ServiceVersionError("Mixin service is out of date");
        }
        
        List<IContainerHandle> existingContainers = new ArrayList<IContainerHandle>(this.containers.keySet());
        for (IContainerHandle existingContainer : existingContainers) {
            this.addNestedContainers(existingContainer);
        }
        
        for (IContainerHandle handle : mixinContainers) {
            try {
                MixinPlatformManager.logger.debug("Adding agents for Mixin Container {}", handle);
                this.addContainer(handle);
            } catch (Exception ex) {
                ex.printStackTrace();
            } 
        }
    }

    /**
     * Queries all containers for launch target, returns null if no containers
     * specify a launch target
     */
    public String getLaunchTarget() {
        return MixinPlatformManager.DEFAULT_MAIN_CLASS;
    }

    /**
     * Set the desired compatibility level as a string, used by agents to set
     * compatibility level from jar manifest
     * 
     * @param level compatibility level as a string
     */
    @SuppressWarnings("deprecation")
    final void setCompatibilityLevel(String level) {
        try {
            CompatibilityLevel value = CompatibilityLevel.valueOf(level.toUpperCase(Locale.ROOT));
            MixinPlatformManager.logger.debug("Setting mixin compatibility level: {}", value);
            MixinEnvironment.setCompatibilityLevel(value);
        } catch (IllegalArgumentException ex) {
            MixinPlatformManager.logger.warn("Invalid compatibility level specified: {}", level);
        }
    }

    /**
     * Add a config from a jar manifest source or the command line. Supports
     * config declarations in the form <tt>filename.json</tt> or alternatively
     * <tt>filename.json&#064;PHASE</tt> where <tt>PHASE</tt> is a
     * case-sensitive string token representing an environment phase.
     * 
     * @param config config resource name, does not require a leading /
     */
    final void addConfig(String config) {
        if (config.endsWith(".json")) {
            MixinPlatformManager.logger.debug("Registering mixin config: {}", config);
            Mixins.addConfiguration(config);
        } else if (config.contains(".json@")) {
            throw new MixinError("Setting config phase via manifest is no longer supported: " + config + ". Specify target in config instead");
        }
    }
    
    /**
     * Add a token provider class from a jar manifest source. Supports either
     * bare class names in the form <tt>blah.package.ClassName</tt> or
     * alternatively <tt>blah.package.ClassName&#064;PHASE</tt> where
     * <tt>PHASE</tt> is a case-sensitive string token representing an
     * environment phase name.
     * 
     * @param provider provider class name, optionally suffixed with &#064;PHASE
     */
    final void addTokenProvider(String provider) {
        if (provider.contains("@")) {
            String[] parts = provider.split("@", 2);
            Phase phase = Phase.forName(parts[1]);
            if (phase != null) {
                MixinPlatformManager.logger.debug("Registering token provider class: {}", parts[0]);
                MixinEnvironment.getEnvironment(phase).registerTokenProviderClass(parts[0]);
            }
            return;
        }

        MixinEnvironment.getDefaultEnvironment().registerTokenProviderClass(provider);
    }
    
    /**
     * Add a mixin connector class for a jar manifest source. Supports only bare
     * class names.
     * 
     * @param connectorClass Name of the connector class to load
     */
    final void addConnector(String connectorClass) {
        this.connectors.addConnector(connectorClass);
    }

}
