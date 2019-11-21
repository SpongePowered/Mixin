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

import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.util.IConsumer;

/**
 * Platform agent base class
 */
public abstract class MixinPlatformAgentAbstract implements IMixinPlatformAgent {

    /**
     * Logger 
     */
    protected static final Logger logger = LogManager.getLogger("mixin");
    
    protected MixinPlatformManager manager;
    
    /**
     * URI to the container
     */
    protected IContainerHandle handle;
    
    /**
     * Ctor
     */
    protected MixinPlatformAgentAbstract() {
    }
    
    @Override
    public AcceptResult accept(MixinPlatformManager manager, IContainerHandle handle) {
        this.manager = manager;
        this.handle = handle;
        return AcceptResult.ACCEPTED;
    }

    @Override
    public String getPhaseProvider() {
        return null;
    }
    
    @Override
    public void prepare() {
    }
    
    @Override
    public void initPrimaryContainer() {
    }

    @Override
    public void inject() {
    }

    @Override
    public String toString() {
        return String.format("PlatformAgent[%s:%s]", this.getClass().getSimpleName(), this.handle);
    }

    protected static String invokeStringMethod(ClassLoader classLoader, String className, String methodName) {
        try {
            Class<?> clazz = Class.forName(className, false, classLoader);
            Method method = clazz.getDeclaredMethod(methodName);
            return ((Enum<?>)method.invoke(null)).name();
        } catch (Exception ex) {
            return null;
        }
    }

    // AMS - Temp
    
    /**
     * Temp wiring. Called when the initial phase is spun up in the environment.
     * 
     * @param phase Initial phase
     * @param phaseConsumer Delegate for the service (or agents) to trigger
     *      later phases
     * @deprecated temporary
     */
    @Deprecated
    public void wire(Phase phase, IConsumer<Phase> phaseConsumer) {
    }
    
    /**
     * Called when the DEFAULT phase is started
     * @deprecated temporary 
     */
    @Deprecated
    public void unwire() {
    }
    
}
