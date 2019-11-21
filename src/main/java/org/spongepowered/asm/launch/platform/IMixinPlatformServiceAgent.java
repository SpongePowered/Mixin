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

import java.util.Collection;

import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.IConsumer;

/**
 * Service agents provide additional platform-specific extensions leveraged by
 * mixin services. Implementing this interface on an agent class will cause an
 * additional instance to be spun up by the service.
 */
public interface IMixinPlatformServiceAgent extends IMixinPlatformAgent {
    
    /**
     * Perform initialisation-stage logic for this agent 
     */
    public abstract void init();
    
    /**
     * Attempt to determine the side name from the current environment. Return
     * <tt>null</tt> if the side name cannot be determined by this agent. Return
     * side name or {@link Constants#SIDE_UNKNOWN} if the agent is able to
     * determine the side.
     */
    public abstract String getSideName();

    /**
     * Get environment-specific mixin containers
     */
    public abstract Collection<IContainerHandle> getMixinContainers();
    
    /**
     * Temp wiring
     * 
     * @param phase Initial phase
     * @param phaseConsumer Phase setter callback
     * @deprecated temporary
     */
    @Deprecated
    public abstract void wire(Phase phase, IConsumer<Phase> phaseConsumer);
    
    /**
     * Temp wiring - Called when the DEFAULT phase is started
     * @deprecated temporary 
     */
    @Deprecated
    public abstract void unwire();
    
}
