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
package org.spongepowered.asm.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.spongepowered.asm.launch.platform.IMixinPlatformAgent;
import org.spongepowered.asm.launch.platform.IMixinPlatformServiceAgent;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.LoggerAdapterDefault;
import org.spongepowered.asm.mixin.MixinEnvironment.CompatibilityLevel;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.IConsumer;
import org.spongepowered.asm.util.ReEntranceLock;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * Mixin Service base class
 */
public abstract class MixinServiceAbstract implements IMixinService {
    
    // Consts
    protected static final String LAUNCH_PACKAGE = "org.spongepowered.asm.launch.";
    protected static final String MIXIN_PACKAGE = "org.spongepowered.asm.mixin.";
    protected static final String SERVICE_PACKAGE = "org.spongepowered.asm.service.";

    /**
     * Logger adapter, replacement for log4j2 logger as services should use
     * their own loggers now in order to avoid contamination
     */
    private static ILogger logger;

    /**
     * Cached logger adapters 
     */
    private static final Map<String, ILogger> loggers = new HashMap<String, ILogger>();

    /**
     * Transformer re-entrance lock, shared between the mixin transformer and
     * the metadata service
     */
    protected final ReEntranceLock lock = new ReEntranceLock(1);
    
    /**
     * All internals offered to this service
     */
    private final Map<Class<IMixinInternal>, IMixinInternal> internals = new HashMap<Class<IMixinInternal>, IMixinInternal>();
    
    /**
     * Service agent instances 
     */
    private List<IMixinPlatformServiceAgent> serviceAgents;

    /**
     * Detected side name
     */
    private String sideName;
    
    protected MixinServiceAbstract() {
        if (MixinServiceAbstract.logger == null) {
            MixinServiceAbstract.logger = this.getLogger("mixin");
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#prepare()
     */
    @Override
    public void prepare() {
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getInitialPhase()
     */
    @Override
    public Phase getInitialPhase() {
        return Phase.PREINIT;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService
     *      #getMinCompatibilityLevel()
     */
    @Override
    public CompatibilityLevel getMinCompatibilityLevel() {
        return null;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService
     *      #getMaxCompatibilityLevel()
     */
    @Override
    public CompatibilityLevel getMaxCompatibilityLevel() {
        return null;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService
     *      #offer(org.spongepowered.asm.service.IMixinInternal)
     */
    @Override
    public void offer(IMixinInternal internal) {
        this.registerInternal(internal, internal.getClass());
    }
    
    @SuppressWarnings("unchecked")
    private void registerInternal(IMixinInternal internal, Class<?> clazz) {
        for (Class<?> iface : clazz.getInterfaces()) {
            if (iface == IMixinInternal.class) {
                this.internals.put((Class<IMixinInternal>)clazz, internal);
            }
            this.registerInternal(internal, iface);
        }
    }

    @SuppressWarnings("unchecked")
    protected final <T extends IMixinInternal> T getInternal(Class<T> type) {
        for (Class<IMixinInternal> internalType : this.internals.keySet()) {
            if (type.isAssignableFrom(internalType)) {
                return (T)this.internals.get(internalType);
            }
        }
        
        return null;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#init()
     */
    @Override
    public void init() {
        for (IMixinPlatformServiceAgent agent : this.getServiceAgents()) {
            agent.init();
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#beginPhase()
     */
    @Override
    public void beginPhase() {
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService
     *      #checkEnv(java.lang.Object)
     */
    @Override
    public void checkEnv(Object bootSource) {
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getReEntranceLock()
     */
    @Override
    public ReEntranceLock getReEntranceLock() {
        return this.lock;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getMixinContainers()
     */
    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        Builder<IContainerHandle> list = ImmutableList.<IContainerHandle>builder();
        this.getContainersFromAgents(list);
        return list.build();
    }

    /**
     * Collect mixin containers from platform agents
     */
    protected final void getContainersFromAgents(Builder<IContainerHandle> list) {
        for (IMixinPlatformServiceAgent agent : this.getServiceAgents()) {
            Collection<IContainerHandle> containers = agent.getMixinContainers();
            if (containers != null) {
                list.addAll(containers);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getSideName()
     */
    @Override
    public final String getSideName() {
        if (this.sideName != null) {
            return this.sideName;
        }
        
        for (IMixinPlatformServiceAgent agent : this.getServiceAgents()) {
            try {
                String side = agent.getSideName();
                if (side != null) {
                    return this.sideName = side;
                }
            } catch (Exception ex) {
                MixinServiceAbstract.logger.catching(ex);
            }
        }
        
        return Constants.SIDE_UNKNOWN;
    }

    private List<IMixinPlatformServiceAgent> getServiceAgents() {
        if (this.serviceAgents != null) {
            return this.serviceAgents;
        }
        this.serviceAgents = new ArrayList<IMixinPlatformServiceAgent>();
        for (String agentClassName : this.getPlatformAgents()) {
            try {
                @SuppressWarnings("unchecked")
                Class<IMixinPlatformAgent> agentClass = (Class<IMixinPlatformAgent>)this.getClassProvider().findClass(agentClassName, false);
                IMixinPlatformAgent agent = agentClass.getDeclaredConstructor().newInstance();
                if (agent instanceof IMixinPlatformServiceAgent) {
                    this.serviceAgents.add((IMixinPlatformServiceAgent)agent);
                }
            } catch (Exception ex) {
                // Bad?
                ex.printStackTrace();
            }
        }
        return this.serviceAgents;
    }
    
    @Override
    public synchronized ILogger getLogger(final String name) {
        ILogger logger = MixinServiceAbstract.loggers.get(name);
        if (logger == null) {
            MixinServiceAbstract.loggers.put(name, logger = this.createLogger(name));
        }
        return logger;
    }

    protected ILogger createLogger(final String name) {
        return new LoggerAdapterDefault(name);
    }

    // AMS - TEMP WIRING TO AVOID THE COMPLEXITY OF MERGING MULTIPHASE WITH 0.8
    
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
        for (IMixinPlatformServiceAgent agent : this.getServiceAgents()) {
            agent.wire(phase, phaseConsumer);
        }
    }

    /**
     * Temp wiring. Called when the default phase is started in the environment.
     * @deprecated temporary
     */
    @Deprecated
    public void unwire() {
        for (IMixinPlatformServiceAgent agent : this.getServiceAgents()) {
            agent.unwire();
        }
    }

}
