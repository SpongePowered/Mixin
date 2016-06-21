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
package org.spongepowered.asm.mixin.extensibility;

import java.util.Set;

import org.spongepowered.asm.mixin.MixinEnvironment;

/**
 * Interface for loaded mixin configurations
 */
public interface IMixinConfig {

    /**
     * Default priority for mixin configs and mixins
     */
    public static final int DEFAULT_PRIORITY = 1000;
    
    /**
     * Get the parent environment of this config
     * 
     * @return the parent environment
     */
    public abstract MixinEnvironment getEnvironment();

    /**
     * Get the name of the file from which this configuration object was
     * initialised
     * 
     * @return the config filename (resource name)
     */
    public abstract String getName();

    /**
     * Get the package containing all mixin classes
     * 
     * @return the base package name for this config
     */
    public abstract String getMixinPackage();

    /**
     * Get the priority
     * 
     * @return the priority
     */
    public abstract int getPriority();

    /**
     * Get the companion plugin, if available
     * 
     * @return the companion plugin instance or null if no plugin
     */
    public abstract IMixinConfigPlugin getPlugin();

    /**
     * True if this mixin is <em>required</em> (failure to apply a defined mixin
     * is an <em>error</em> condition).
     * 
     * @return true if this config is marked as required
     */
    public abstract boolean isRequired();

    /**
     * Get targets for this configuration
     * 
     * @return target classes of mixins in this config
     */
    public abstract Set<String> getTargets();

}
