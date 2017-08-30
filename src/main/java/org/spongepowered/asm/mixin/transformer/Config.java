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

import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;

/**
 * Handle for marshalling mixin configs outside of the transformer package
 */
public class Config {
    
    /**
     * Config name, used as identity for the purposes of {@link #equals}
     */
    private final String name;
    
    /**
     * Config 
     */
    private final MixinConfig config;
    
    public Config(MixinConfig config) {
        this.name = config.getName();
        this.config = config;
    }
    
    public String getName() {
        return this.name;
    }

    /**
     * Access inner config
     */
    MixinConfig get() {
        return this.config;
    }

    /**
     * Get whether config has been visited
     */
    public boolean isVisited() {
        return this.config.isVisited();
    }

    /**
     * Get API-level config view
     */
    public IMixinConfig getConfig() {
        return this.config;
    }
    
    /**
     * Get environment for the config
     */
    public MixinEnvironment getEnvironment() {
        return this.config.getEnvironment();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.config.toString();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Config && this.name.equals(((Config)obj).name);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    /**
     * Factory method, create a config from the specified config file and fail
     * over to the specified environment if no selector is present in the config
     * 
     * @param configFile config resource
     * @param outer failover environment
     * @return new config or null if invalid config version
     */
    @Deprecated
    public static Config create(String configFile, MixinEnvironment outer) {
        return MixinConfig.create(configFile, outer);
    }

    /**
     * Factory method, create a config from the specified config resource
     * 
     * @param configFile config resource
     * @return new config or null if invalid config version 
     */
    public static Config create(String configFile) {
        return MixinConfig.create(configFile, MixinEnvironment.getDefaultEnvironment());
    }

}
