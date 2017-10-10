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

import java.util.HashSet;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provides access to the service layer which connects the mixin transformer to
 * a particular host environment. Host environments are implemented as services
 * implementing {@link IMixinService} in order to decouple them from mixin's
 * core. This allows us to support LegacyLauncher 
 */
public final class MixinService {
    
    /**
     * Log all the things
     */
    private static final Logger logger = LogManager.getLogger("mixin");

    /**
     * Singleton
     */
    private static MixinService instance;
    
    private ServiceLoader<IMixinServiceBootstrap> bootstrapServiceLoader;
    
    private final Set<String> bootedServices = new HashSet<String>(); 

    /**
     * Service loader 
     */
    private ServiceLoader<IMixinService> serviceLoader;

    /**
     * Service
     */
    private IMixinService service = null;

    /**
     * Singleton pattern
     */
    private MixinService() {
        this.runBootServices();
    }

    private void runBootServices() {
        this.bootstrapServiceLoader = ServiceLoader.<IMixinServiceBootstrap>load(IMixinServiceBootstrap.class, this.getClass().getClassLoader());
        for (IMixinServiceBootstrap bootService : this.bootstrapServiceLoader) {
            try {
                bootService.bootstrap();
                this.bootedServices.add(bootService.getServiceClassName());
            } catch (Throwable th) {
                MixinService.logger.catching(th);
            }
        }
    }

    /**
     * Singleton pattern, get or create the instance
     */
    private static MixinService getInstance() {
        if (MixinService.instance == null) {
            MixinService.instance = new MixinService();
        }
        
        return MixinService.instance;
    }
    
    /**
     * Boot
     */
    public static void boot() {
        MixinService.getInstance();
    }
    
    public static IMixinService getService() {
        return MixinService.getInstance().getServiceInstance();
    }

    private synchronized IMixinService getServiceInstance() {
        if (this.service == null) {
            this.service = this.initService();
            if (this.service == null) {
                throw new ServiceNotAvailableError("No mixin host service is available");
            }
        }
        return this.service;
    }

    private IMixinService initService() {
        this.serviceLoader = ServiceLoader.<IMixinService>load(IMixinService.class, this.getClass().getClassLoader());
        Iterator<IMixinService> iter = this.serviceLoader.iterator();
        while (iter.hasNext()) {
            try {
                IMixinService service = iter.next();
                if (this.bootedServices.contains(service.getClass().getName())) {
                    MixinService.logger.debug("MixinService [{}] was successfully booted in {}", service.getName(), this.getClass().getClassLoader());
                }
                if (service.isValid()) {
                    return service;
                }
            } catch (ServiceConfigurationError serviceError) {
                serviceError.printStackTrace();
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
        return null;
    }

}
