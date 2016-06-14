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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.CompatibilityLevel;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.Mixins;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * TweakClass for running mixins in production. Being a tweaker ensures that we
 * get injected into the AppClassLoader but does mean that we will need to
 * inject the FML coremod by hand if running under FML.
 */
public class MixinTweaker implements ITweaker {
    
    private static final String MFATT_TWEAKER = "TweakClass";
    private static final String DEFAULT_MAIN_CLASS = "net.minecraft.client.main.Main";
    
    /**
     * Make with the logging already
     */
    private static final Logger logger = LogManager.getLogger("mixin");
    
    /**
     * Tweak containers
     */
    private final Map<URI, MixinTweakContainer> containers = new LinkedHashMap<URI, MixinTweakContainer>();
    
    /**
     * Container for this tweaker 
     */
    private MixinTweakContainer primaryContainer;
    
    /**
     * Tracks whether {@link #acceptOptions} was called yet, if true, causes new
     * launch agents to be <tt>prepare</tt>d immediately 
     */
    private boolean prepared = false;
    
    /**
     * Hello world
     */
    public MixinTweaker() {
        MixinBootstrap.preInit();
        
        // Add agents for the tweak container 
        URI uri = null;
        try {
            uri = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
            if (uri != null) {
                this.primaryContainer = this.addContainer(uri);
            }
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
        }
    }
    
    public final MixinTweakContainer addContainer(URI uri) {
        MixinTweakContainer existingContainer = this.containers.get(uri);
        if (existingContainer != null) {
            return existingContainer;
        }
        
        MixinTweaker.logger.debug("Adding mixin launch agents for container {}", uri);
        MixinTweakContainer container = new MixinTweakContainer(uri);
        this.containers.put(uri, container);
        
        if (this.prepared) {
            container.prepare();
        }
        return container;
    }

    /* (non-Javadoc)
     * @see net.minecraft.launchwrapper.ITweaker#acceptOptions(java.util.List,
     *      java.io.File, java.io.File, java.lang.String)
     */
    @Override
    public final void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        MixinBootstrap.register();
        this.prepared = true;
        for (MixinTweakContainer container : this.containers.values()) {
            container.prepare();
        }
        this.parseArgs(args);
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
                MixinTweaker.addConfig(arg);
            }
            captureNext = "--mixin".equals(arg);
        }
    }

    /* (non-Javadoc)
     * @see net.minecraft.launchwrapper.ITweaker#injectIntoClassLoader(
     *      net.minecraft.launchwrapper.LaunchClassLoader)
     */
    @Override
    public final void injectIntoClassLoader(LaunchClassLoader classLoader) {
        if (this.primaryContainer != null) {
            this.primaryContainer.initPrimaryContainer();
        }
        
        this.scanClasspath();
        MixinTweaker.logger.debug("injectIntoClassLoader running with {} agents", this.containers.size());
        for (MixinTweakContainer container : this.containers.values()) {
            try {
                container.injectIntoClassLoader(classLoader);
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
        for (URL url : Launch.classLoader.getSources()) {
            try {
                URI uri = url.toURI();
                if (this.containers.containsKey(uri)) {
                    continue;
                }
                MixinTweaker.logger.debug("Scanning {} for mixin tweaker", uri);
                if (!"file".equals(uri.getScheme()) || !new File(uri).exists()) {
                    continue;
                }
                MainAttributes attributes = MainAttributes.of(uri);
                String tweaker = attributes.get(MixinTweaker.MFATT_TWEAKER);
                if (MixinTweaker.class.getName().equals(tweaker)) {
                    MixinTweaker.logger.debug("{} contains a mixin tweaker, adding agents", uri);
                    this.addContainer(uri);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } 
        }
    }

    /* (non-Javadoc)
     * @see net.minecraft.launchwrapper.ITweaker#getLaunchTarget()
     */
    @Override
    public String getLaunchTarget() {
        for (MixinTweakContainer container : this.containers.values()) {
            String mainClass = container.getLaunchTarget();
            if (mainClass != null) {
                return mainClass;
            }
        }
        
        return MixinTweaker.DEFAULT_MAIN_CLASS;
    }

    /* (non-Javadoc)
     * @see net.minecraft.launchwrapper.ITweaker#getLaunchArguments()
     */
    @Override
    public String[] getLaunchArguments() {
        return new String[]{};
    }
    
    /**
     * Set the desired compatibility level as a string, used by agents to set
     * compatibility level from jar manifest
     * 
     * @param level compatibility level as a string
     */
    @Deprecated
    static void setCompatibilityLevel(String level) {
        try {
            CompatibilityLevel value = CompatibilityLevel.valueOf(level.toUpperCase());
            MixinTweaker.logger.debug("Setting mixin compatibility level: {}", value);
            MixinEnvironment.setCompatibilityLevel(value);
        } catch (IllegalArgumentException ex) {
            MixinTweaker.logger.warn("Invalid compatibility level specified: {}", level);
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
    static void addConfig(String config) {
        if (config.endsWith(".json")) {
            MixinTweaker.logger.debug("Registering mixin config: {}", config);
            Mixins.addConfiguration(config);
        } else if (config.contains(".json@")) {
            int pos = config.indexOf(".json@");
            String phaseName = config.substring(pos + 6);
            config = config.substring(0, pos + 5);
            Phase phase = Phase.forName(phaseName);
            if (phase != null) {
                MixinTweaker.logger.warn("Setting config phase via manifest is deprecated: {}. Specify target in config instead", config);
                MixinTweaker.logger.debug("Registering mixin config: {}", config);
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
    static void addTokenProvider(String provider) {
        if (provider.contains("@")) {
            String[] parts = provider.split("@", 2);
            Phase phase = Phase.forName(parts[1]);
            if (phase != null) {
                MixinTweaker.logger.debug("Registering token provider class: {}", parts[0]);
                MixinEnvironment.getEnvironment(phase).registerTokenProviderClass(parts[0]);
            }
            return;
        }

        MixinEnvironment.getDefaultEnvironment().registerTokenProviderClass(provider);
    }
    
}
