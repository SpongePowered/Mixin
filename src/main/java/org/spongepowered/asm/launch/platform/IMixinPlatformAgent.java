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

import org.spongepowered.asm.launch.platform.container.IContainerHandle;

/**
 * Base interface for platform agents. Platform agents are environment-specific
 * handlers which are used by the Mixin subsystem to perform platform-specific
 * tasks required by different environments without having to litter other parts
 * of the Mixin codebase with a bunch of environment-specific cruft.
 * 
 * <p>Platform Agents handle mixin environment tasks on a per-container basis,
 * with each container in the environment being assigned one of each available
 * type of agent to handle those tasks on behalf of the container.</p>
 */
public interface IMixinPlatformAgent {
    
    /**
     * Result type returned from {@link IMixinPlatformAgent#accept}
     */
    public enum AcceptResult {
        
        /**
         * The container was accepted
         */
        ACCEPTED,
        
        /**
         * The container was rejected
         */
        REJECTED,
        
        /**
         * The agent encountered an error and no further containers should be
         * offered
         */
        INVALID;
        
    }
    
    /**
     * Accept and bind to a container handle. This method is called for agents
     * hosted by {@link MixinContainer} and the agent should react accordingly.
     * If the agent is <em>not</em> able to delegate for container handles of
     * the supplied type, this method should return <tt>false</tt> to indicate
     * that the agent should not be added for this container.
     * 
     * @param manager platform manager instance
     * @param handle handle to container
     * @return AcceptResult representing this agent's acceptance of the supplied
     *      container
     */
    public abstract AcceptResult accept(MixinPlatformManager manager, IContainerHandle handle);

    /**
     * Get the phase provider for this agent
     */
    public abstract String getPhaseProvider();
    
    /**
     * Called during pre-initialisation, after all tweakers and tweak containers
     * have been added to the environment.
     */
    public abstract void prepare();
    
    /**
     * Called from <tt>inject</tt> in the parent tweaker but <b>only called on
     * the primary tweak container</b>. This is useful if the agent needs to
     * perform some environment-specific setup just once.
     */
    public abstract void initPrimaryContainer();
    
    /**
     * Called from <tt>inject</tt> in the parent tweaker
     */
    public abstract void inject();

}
