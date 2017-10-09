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

import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.GlobalProperties;
import org.spongepowered.asm.service.MixinService;

/**
 * A collection of {@link IMixinPlatformAgent} platform agents)
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
    
    private final Logger logger = LogManager.getLogger("mixin");
    
    private final URI uri;
    
    private final List<IMixinPlatformAgent> agents = new ArrayList<IMixinPlatformAgent>();

    public MixinContainer(MixinPlatformManager manager, URI uri) {
        this.uri = uri;
        
        for (String agentClass : MixinContainer.agentClasses) {
            try {
                @SuppressWarnings("unchecked")
                Class<IMixinPlatformAgent> clazz = (Class<IMixinPlatformAgent>)Class.forName(agentClass);
                Constructor<IMixinPlatformAgent> ctor = clazz.getDeclaredConstructor(MixinPlatformManager.class, URI.class);
                this.logger.debug("Instancing new {} for {}", clazz.getSimpleName(), this.uri);
                IMixinPlatformAgent agent = ctor.newInstance(manager, uri);
                this.agents.add(agent);
            } catch (Exception ex) {
                this.logger.catching(ex);
            }
        }
    }

    /**
     * 
     */
    public URI getURI() {
        return this.uri;
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
            this.logger.debug("Processing prepare() for {}", agent);
            agent.prepare();
        }
    }
    
    /**
     * If this container is the primary container, initialise agents in this
     * container as primary
     */
    public void initPrimaryContainer() {
        for (IMixinPlatformAgent agent : this.agents) {
            this.logger.debug("Processing launch tasks for {}", agent);
            agent.initPrimaryContainer();
        }
    }

    /**
     * Notify all agents to inject into classLoader
     */
    public void inject() {
        for (IMixinPlatformAgent agent : this.agents) {
            this.logger.debug("Processing inject() for {}", agent);
            agent.inject();
        }
    }

    /**
     * Analogue of <tt>ITweaker::getLaunchTarget</tt>, queries all agents and
     * returns first valid launch target. Returns null if no agents have launch
     * target.
     * 
     * @return launch target from agent or null
     */
    public String getLaunchTarget() {
        for (IMixinPlatformAgent agent : this.agents) {
            String launchTarget = agent.getLaunchTarget();
            if (launchTarget != null) {
                return launchTarget;
            }
        }
        return null;
    }
    
}
