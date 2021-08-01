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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.throwables.CompanionPluginError;
import org.spongepowered.asm.service.IMixinService;

import com.google.common.base.Strings;

/**
 * Convenience wrapper for mixin config plugins
 */
class PluginHandle {
    
    /**
     * Compatibility mode for companion plugins
     */
    enum CompatibilityMode {

        /**
         * Companion plugin is compatible or unknown
         */
        NORMAL,
        
        /**
         * Companion plugin is outdated but running anyway, preApply and
         * postApply will be called via reflection
         */
        COMPATIBLE,
        
        /**
         * Companion plugin is disabled due to incompatibility
         */
        FAILED
    
    }
    
    private static final Logger logger = LogManager.getLogger("mixin");

    /**
     * Parent config which owns this plugin handle
     */
    private final MixinConfig parent;

    /**
     * Plugin instance, can be null
     */
    private final IMixinConfigPlugin plugin;
    
    /**
     * Compatibility mode for companion plugin
     */
    private CompatibilityMode mode = CompatibilityMode.NORMAL;
    
    /**
     * Reflection objects for calling legacy (pre 0.8) preApply and postApply
     */
    private Method mdPreApply, mdPostApply;

    PluginHandle(MixinConfig parent, IMixinService service, String pluginClassName) {
        IMixinConfigPlugin plugin = null;
        
        if (!Strings.isNullOrEmpty(pluginClassName)) {
            try {
                Class<?> pluginClass = service.getClassProvider().findClass(pluginClassName, true);
                plugin = (IMixinConfigPlugin)pluginClass.getDeclaredConstructor().newInstance();
            } catch (Throwable th) {
                PluginHandle.logger.error("Error loading companion plugin class [{}] for mixin config [{}]. The plugin may be out of date: {}:{}",
                        pluginClassName, parent, th.getClass().getSimpleName(), th.getMessage(), th);
                plugin = null;
            }
        }
        
        this.parent = parent;
        this.plugin = plugin;
    }

    IMixinConfigPlugin get() {
        return this.plugin;
    }
    
    boolean isAvailable() {
        return this.plugin != null;
    }

    void onLoad(String mixinPackage) {
        if (this.plugin != null) {
            this.plugin.onLoad(mixinPackage);
        }
    }

    String getRefMapperConfig() {
        return this.plugin != null ? this.plugin.getRefMapperConfig() : null;
    }

    List<String> getMixins() {
        return this.plugin != null ? this.plugin.getMixins() : null;
    }

    boolean shouldApplyMixin(String targetName, String className) {
        return this.plugin == null || this.plugin.shouldApplyMixin(targetName, className);
    }
    
    /**
     * Called immediately before the mixin is applied to targetClass
     */
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, MixinInfo mixinInfo) throws Exception {
        if (this.plugin == null) {
            return;
        }
        
        if (this.mode == CompatibilityMode.FAILED) {
            throw new IllegalStateException("Companion plugin failure for [" + this.parent + "] plugin [" + this.plugin.getClass() + "]");
        }
        
        if (this.mode == CompatibilityMode.COMPATIBLE) {
            try {
                this.applyLegacy(this.mdPreApply, targetClassName, targetClass, mixinClassName, mixinInfo);
            } catch (Exception ex) {
                this.mode = CompatibilityMode.FAILED;
                throw ex;
            }
            return;
        } 

        try {
            this.plugin.preApply(targetClassName, targetClass, mixinClassName, mixinInfo);
        } catch (AbstractMethodError ex) {
            this.mode = CompatibilityMode.COMPATIBLE;
            this.initReflection();
            this.preApply(targetClassName, targetClass, mixinClassName, mixinInfo);
        }
    }

    /**
     * Called immediately after the mixin is applied to targetClass
     */
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, MixinInfo mixinInfo) throws Exception {
        if (this.plugin == null) {
            return;
        }
        
        if (this.mode == CompatibilityMode.FAILED) {
            throw new IllegalStateException("Companion plugin failure for [" + this.parent + "] plugin [" + this.plugin.getClass() + "]");
        }
        
        if (this.mode == CompatibilityMode.COMPATIBLE) {
            try {
                this.applyLegacy(this.mdPostApply, targetClassName, targetClass, mixinClassName, mixinInfo);
            } catch (Exception ex) {
                this.mode = CompatibilityMode.FAILED;
                throw ex;
            }
            return;
        } 

        try {
            this.plugin.postApply(targetClassName, targetClass, mixinClassName, mixinInfo);
        } catch (AbstractMethodError ex) {
            this.mode = CompatibilityMode.COMPATIBLE;
            this.initReflection();
            this.postApply(targetClassName, targetClass, mixinClassName, mixinInfo);
        }
    }

    private void initReflection() {
        if (this.mdPreApply != null) {
            return;
        }
        
        try {
            Class<?> pluginClass = this.plugin.getClass();
            this.mdPreApply = pluginClass.getMethod("preApply", String.class, org.spongepowered.asm.lib.tree.ClassNode.class, String.class,
                    IMixinInfo.class);
            this.mdPostApply = pluginClass.getMethod("postApply", String.class, org.spongepowered.asm.lib.tree.ClassNode.class, String.class,
                    IMixinInfo.class);
        } catch (Throwable th) {
            PluginHandle.logger.catching(th);
        }
    }

    private void applyLegacy(Method method, String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        try {
            method.invoke(this.plugin, targetClassName, new org.spongepowered.asm.lib.tree.ClassNode(targetClass), mixinClassName, mixinInfo);
        } catch (LinkageError err) {
            throw new CompanionPluginError(this.apiError("Accessing [" + err.getMessage() + "]"), err);
        } catch (IllegalAccessException ex) {
            throw new CompanionPluginError(this.apiError("Fallback failed [" + ex.getMessage() + "]"), ex);
        } catch (IllegalArgumentException ex) {
            throw new CompanionPluginError(this.apiError("Fallback failed [" + ex.getMessage() + "]"), ex);
        } catch (InvocationTargetException ex) {
            Throwable th = ex.getCause() != null ? ex.getCause() : ex;
            throw new CompanionPluginError(this.apiError("Fallback failed [" + th.getMessage() + "]"), th);
        }
    }

    private String apiError(String message) {
        return String.format("Companion plugin attempted to use a deprected API in [%s] plugin [%s]: %s",
                this.parent, this.plugin.getClass().getName(), message);
    }

}
