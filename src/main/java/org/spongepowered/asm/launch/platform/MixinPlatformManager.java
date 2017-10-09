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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.spongepowered.asm.launch.MixinBootstrap.Delegate;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.CompatibilityLevel;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.mixin.Mixins;

//import com.google.common.collect.ImmutableList;

/**
 * Handler for platform-specific behaviour required in different mixin
 * environments.
 */
public class MixinPlatformManager {

    private static final String DEFAULT_MAIN_CLASS = "net.minecraft.client.main.Main";
    private static final String MIXIN_TWEAKER_CLASS = "org.spongepowered.asm.launch.MixinTweaker";
    
    /**
     * Make with the logging already
     */
    private static final Logger logger = LogManager.getLogger("mixin");
    
    /**
     * Bootstrap delegate 
     */
//    private final Delegate delegate;
    
    /**
     * Tweak containers
     */
    private final Map<URI, MixinContainer> containers = new LinkedHashMap<URI, MixinContainer>();
    
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
     * Initialise this platform manager by scanning the classpath
     */
    public void init() {
        MixinPlatformManager.logger.debug("Initialising Mixin Platform Manager");
                
        // Add agents for the tweak container 
        URI uri = null;
        try {
            uri = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
            if (uri != null) {
                MixinPlatformManager.logger.debug("Mixin platform: primary container is {}", uri);
                this.primaryContainer = this.addContainer(uri);
            }
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
        }
        
        // Do an early scan to ensure preinit mixins are discovered
        this.scanClasspath();
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
     * Add a new URI to this platform and return the new container (or an
     * existing container if the URI was previously registered)
     * 
     * @param uri URI to add
     * @return container for specified URI
     */
    public final MixinContainer addContainer(URI uri) {
        MixinContainer existingContainer = this.containers.get(uri);
        if (existingContainer != null) {
            return existingContainer;
        }
        
        MixinPlatformManager.logger.debug("Adding mixin platform agents for container {}", uri);
        MixinContainer container = new MixinContainer(this, uri);
        this.containers.put(uri, container);
        
        if (this.prepared) {
            container.prepare();
        }
        return container;
    }

    /**
     * Prepare all containers in this platform
     * 
     * @param args command-line arguments from tweaker
     */
    public final void prepare(List<String> args) {
        this.prepared = true;
        for (MixinContainer container : this.containers.values()) {
            container.prepare();
        }
        if (args != null) {
            this.parseArgs(args);
        } else {
            String argv = System.getProperty("sun.java.command");
            if (argv != null) {
                this.parseArgs(Arrays.asList(argv.split(" "))); 
            }            
        }
    }

    /**
     * Read and parse command-line arguments
     * 
     * @param args command-line arguments
     */
    private void parseArgs(List<String> args) {
        boolean captureNext = false;
        for (String arg : args) {
            if (captureNext) {
                this.addConfig(arg);
            }
            captureNext = "--mixin".equals(arg);
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
        
        this.scanClasspath();
        MixinPlatformManager.logger.debug("inject() running with {} agents", this.containers.size());
        for (MixinContainer container : this.containers.values()) {
            try {
                container.inject();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Scan the classpath for mixin containers (containers which declare the
     * mixin tweaker in their manifest) and add agents for them
     */
    private void scanClasspath() {
        URL[] sources = MixinService.getService().getClassProvider().getClassPath();
        for (URL url : sources) {
            try {
                URI uri = url.toURI();
                if (this.containers.containsKey(uri)) {
                    continue;
                }
                MixinPlatformManager.logger.debug("Scanning {} for mixin tweaker", uri);
                if (!"file".equals(uri.getScheme()) || !new File(uri).exists()) {
                    continue;
                }
                MainAttributes attributes = MainAttributes.of(uri);
                String tweaker = attributes.get(Constants.ManifestAttributes.TWEAKER);
                if (MixinPlatformManager.MIXIN_TWEAKER_CLASS.equals(tweaker)) {
                    MixinPlatformManager.logger.debug("{} contains a mixin tweaker, adding agents", uri);
                    this.addContainer(uri);
                }
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
        for (MixinContainer container : this.containers.values()) {
            String mainClass = container.getLaunchTarget();
            if (mainClass != null) {
                return mainClass;
            }
        }
        
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
            CompatibilityLevel value = CompatibilityLevel.valueOf(level.toUpperCase());
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
    @SuppressWarnings("deprecation")
    final void addConfig(String config) {
        if (config.endsWith(".json")) {
            MixinPlatformManager.logger.debug("Registering mixin config: {}", config);
            Mixins.addConfiguration(config);
        } else if (config.contains(".json@")) {
            int pos = config.indexOf(".json@");
            String phaseName = config.substring(pos + 6);
            config = config.substring(0, pos + 5);
            Phase phase = Phase.forName(phaseName);
            if (phase != null) {
                MixinPlatformManager.logger.warn("Setting config phase via manifest is deprecated: {}. Specify target in config instead", config);
                MixinPlatformManager.logger.debug("Registering mixin config: {}", config);
                MixinEnvironment.getEnvironment(phase).addConfiguration(config);
            }
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

}
