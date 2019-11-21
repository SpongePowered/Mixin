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
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.GlobalProperties;
import org.spongepowered.asm.launch.platform.IMixinPlatformAgent.AcceptResult;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.service.MixinService;

/**
 * A collection of {@link IMixinPlatformAgent} platform agents) for a particular
 * container
 */
public class MixinContainer {

    private static final List<String> agentClasses = new ArrayList<String>();
    
    static {
        GlobalProperties.put(GlobalProperties.Keys.AGENTS, MixinContainer.agentClasses);
        for (String agent : MixinService.getService().getPlatformAgents()) {
            MixinContainer.agentClasses.add(agent);
        }
        MixinContainer.agentClasses.add("org.spongepowered.asm.launch.platform.MixinPlatformAgentDefault");
    }
    
    private static final Logger logger = LogManager.getLogger("mixin");
    
    private final IContainerHandle handle;
    
    private final List<IMixinPlatformAgent> agents = new ArrayList<IMixinPlatformAgent>();

    public MixinContainer(MixinPlatformManager manager, IContainerHandle handle) {
        this.handle = handle;
        
        for (Iterator<String> iter = MixinContainer.agentClasses.iterator(); iter.hasNext();) {
            String agentClass = iter.next();
            try {
                @SuppressWarnings("unchecked")
                Class<IMixinPlatformAgent> clazz = (Class<IMixinPlatformAgent>)Class.forName(agentClass);
                String simpleName = clazz.getSimpleName();
                
                MixinContainer.logger.debug("Instancing new {} for {}", simpleName, this.handle);
                IMixinPlatformAgent agent = clazz.newInstance();
                
                AcceptResult acceptAction = agent.accept(manager, this.handle);
                if (acceptAction == AcceptResult.ACCEPTED) {
                    this.agents.add(agent);
                } else if (acceptAction == AcceptResult.INVALID) {
                    iter.remove();
                    continue;
                }
                
                MixinContainer.logger.debug("{} {} container {}", simpleName, acceptAction.name().toLowerCase(), this.handle);
            } catch (InstantiationException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException)cause;
                }
                throw new RuntimeException(cause);
            } catch (ReflectiveOperationException ex) {
                MixinContainer.logger.catching(ex);
            }
        }
    }

    /**
     * 
     */
    public IContainerHandle getDescriptor() {
        return this.handle;
    }

    /**
     * Get phase provider names from all agents in this container
     */
    public Collection<String> getPhaseProviders() {
        List<String> phaseProviders = new ArrayList<String>();
        for (IMixinPlatformAgent agent : this.agents) {
            String phaseProvider = agent.getPhaseProvider();
            if (phaseProvider != null) {
                phaseProviders.add(phaseProvider);
            }
        }
        return phaseProviders;
    }

    /**
     * Prepare agents in this container
     */
    public void prepare() {
        for (IMixinPlatformAgent agent : this.agents) {
            MixinContainer.logger.debug("Processing prepare() for {}", agent);
            agent.prepare();
        }
    }
    
    /**
     * If this container is the primary container, initialise agents in this
     * container as primary
     */
    public void initPrimaryContainer() {
        for (IMixinPlatformAgent agent : this.agents) {
            MixinContainer.logger.debug("Processing launch tasks for {}", agent);
            agent.initPrimaryContainer();
        }
    }

    /**
     * Notify all agents to inject into classLoader
     */
    public void inject() {
        for (IMixinPlatformAgent agent : this.agents) {
            MixinContainer.logger.debug("Processing inject() for {}", agent);
            agent.inject();
        }
    }
    
}
