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
package org.spongepowered.asm.mixin;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.GlobalProperties;
import org.spongepowered.asm.mixin.transformer.Config;

/**
 * Entry point for registering global mixin resources. Compatibility with
 * pre-0.6 versions is maintained via the methods on {@link MixinEnvironment}
 * delegating to the methods here.
 */
public final class Mixins {
    
    /**
     * Logger 
     */
    private static final Logger logger = LogManager.getLogger("mixin");

    /**
     * GlobalProperties key storing mixin configs which are pending
     */
    private static final String CONFIGS_KEY = GlobalProperties.Keys.CONFIGS + ".queue";
    
    /**
     * Error handlers for environment
     */
    private static final Set<String> errorHandlers = new LinkedHashSet<String>();
    
    private Mixins() {}
    
    /**
     * Add multiple configurations
     * 
     * @param configFiles config resources to add
     */
    public static void addConfigurations(String... configFiles) {
        MixinEnvironment fallback = MixinEnvironment.getDefaultEnvironment();
        for (String configFile : configFiles) {
            Mixins.createConfiguration(configFile, fallback);
        }
    }
    
    /**
     * Add a mixin configuration resource
     * 
     * @param configFile path to configuration resource
     */
    public static void addConfiguration(String configFile) {
        Mixins.createConfiguration(configFile, MixinEnvironment.getDefaultEnvironment());
    }
    
    @Deprecated
    static void addConfiguration(String configFile, MixinEnvironment fallback) {
        Mixins.createConfiguration(configFile, fallback);
    }

    @SuppressWarnings("deprecation")
    private static void createConfiguration(String configFile, MixinEnvironment fallback) {
        Config config = null;
        
        try {
            config = Config.create(configFile, fallback);
        } catch (Exception ex) {
            Mixins.logger.error("Error encountered reading mixin config " + configFile + ": " + ex.getClass().getName() + " " + ex.getMessage(), ex);
        }
        
        Mixins.registerConfiguration(config);
    }

    private static void registerConfiguration(Config config) {
        if (config == null) {
            return;
        }
        
        MixinEnvironment env = config.getEnvironment();
        if (env != null) {
            env.registerConfig(config.getName());
        }
        Mixins.getConfigs().add(config);
    }
    
    /**
     * Get the number of "unvisited" configurations available. This is the
     * number of configurations which have been added since the last selection
     * attempt.
     * 
     * <p>If the transformer has already entered a phase but no mixins have yet
     * been applied, it is safe to visit any additional configs which were
     * registered in the mean time and may wish to apply to the current phase.
     * This is particularly true during the PREINIT phase, which by necessity
     * must start as soon as the first class is transformed after bootstrapping,
     * but may not have any valid mixins until later in the actual preinit
     * process due to the order in which things are discovered.
     * 
     * @return unvisited config count
     */
    public static int getUnvisitedCount() {
        int count = 0;
        for (Config config : Mixins.getConfigs()) {
            if (!config.isVisited()) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Get current pending configs set, only configs which have yet to be
     * consumed are present in this set
     */
    public static Set<Config> getConfigs() {
        Set<Config> mixinConfigs = GlobalProperties.<Set<Config>>get(Mixins.CONFIGS_KEY);
        if (mixinConfigs == null) {
            mixinConfigs = new LinkedHashSet<Config>();
            GlobalProperties.put(Mixins.CONFIGS_KEY, mixinConfigs);
        }
        return mixinConfigs;
    }

    /**
     * Register a gloabl error handler class
     * 
     * @param handlerName Fully qualified class name
     */
    public static void registerErrorHandlerClass(String handlerName) {
        if (handlerName != null) {
            Mixins.errorHandlers.add(handlerName);
        }
    }

    /**
     * Get current error handlers
     */
    public static Set<String> getErrorHandlerClasses() {
        return Collections.<String>unmodifiableSet(Mixins.errorHandlers);
    }

}
