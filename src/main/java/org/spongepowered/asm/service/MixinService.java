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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.LoggerAdapterConsole;

import com.google.common.base.Joiner;
import com.google.common.collect.ObjectArrays;

/**
 * Provides access to the service layer which connects the mixin transformer to
 * a particular host environment. Host environments are implemented as services
 * implementing {@link IMixinService} in order to decouple them from mixin's
 * core. This allows us to support LegacyLauncher 
 */
public final class MixinService {
    
    /**
     * Since we want to log things during startup but need the service itself to
     * provide a logging implementation adapter, we need a place to store log
     * messages prior to the service startup which can later be flushed into the
     * logger itself if startup succeeds, or into the console if startup fails.
     */
    static class LogBuffer {
        
        public static class LogEntry {
            
            public String message;
            public Object[] params;
            public Throwable t;
            
            public LogEntry(String message, Object[] params, Throwable t) {
                this.message = message;
                this.params = params;
                this.t = t;
            }
            
        }
        
        private final List<LogEntry> buffer = new ArrayList<LogEntry>();
        
        private ILogger logger;

        synchronized void debug(String message, Object... params) {
            if (this.logger != null) {
                this.logger.debug(message, params);
                return;
            }
            this.buffer.add(new LogEntry(message, params, null));
        }

        synchronized void debug(String message, Throwable t) {
            if (this.logger != null) {
                this.logger.debug(message, t);
                return;
            }
            this.buffer.add(new LogEntry(message, new Object[0], t));
        }

        /**
         * Flush the contents of the buffer into the specified logger
         */
        synchronized void flush(ILogger logger) {
            for (LogEntry buffered : this.buffer) {
                if (buffered.t != null) {
                    logger.debug(buffered.message, ObjectArrays.concat(buffered.params, buffered.t));
                } else {
                    logger.debug(buffered.message, buffered.params);
                }
            }
            this.buffer.clear();
            this.logger = logger;
        }

    }

    /**
     * Log buffer for messages generated during service startup but before the
     * actual logger can be retrieved from the service, flushed once the service
     * is started
     */
    private static LogBuffer logBuffer = new LogBuffer();
    
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
     * Global Property Service
     */
    private IGlobalPropertyService propertyService;

    /**
     * Singleton pattern
     */
    private MixinService() {
        this.runBootServices();
    }

    private void runBootServices() {
        this.bootstrapServiceLoader = ServiceLoader.<IMixinServiceBootstrap>load(IMixinServiceBootstrap.class, this.getClass().getClassLoader());
        Iterator<IMixinServiceBootstrap> iter = this.bootstrapServiceLoader.iterator();
        while (iter.hasNext()) {
            try {
                IMixinServiceBootstrap bootService = iter.next();
                bootService.bootstrap();
                this.bootedServices.add(bootService.getServiceClassName());
            } catch (ServiceInitialisationException ex) {
                // Expected if service cannot start
                MixinService.logBuffer.debug("Mixin bootstrap service {} is not available: {}", ex.getStackTrace()[0].getClassName(),
                        ex.getMessage());
            } catch (Throwable th) {
                MixinService.logBuffer.debug("Catching {}:{} initialising service", th.getClass().getName(), th.getMessage(), th);
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
            try {
                this.service = this.initService();
                ILogger serviceLogger = this.service.getLogger("mixin");
                MixinService.logBuffer.flush(serviceLogger);
            } catch (Error err) {
                ILogger defaultLogger = MixinService.<ILogger>getDefaultLogger();
                MixinService.logBuffer.flush(defaultLogger);
                defaultLogger.error(err.getMessage(), err);
                throw err;
            }
        }
        return this.service;
    }

    private IMixinService initService() {
        this.serviceLoader = ServiceLoader.<IMixinService>load(IMixinService.class, this.getClass().getClassLoader());
        Iterator<IMixinService> iter = this.serviceLoader.iterator();
        List<String> badServices = new ArrayList<String>();
        int brokenServiceCount = 0;
        while (iter.hasNext()) {
            try {
                IMixinService service = iter.next();
                if (this.bootedServices.contains(service.getClass().getName())) {
                    MixinService.logBuffer.debug("MixinService [{}] was successfully booted in {}", service.getName(),
                            this.getClass().getClassLoader());
                }
                if (service.isValid()) {
                    return service;
                }
                MixinService.logBuffer.debug("MixinService [{}] is not valid", service.getName());
                badServices.add(String.format("INVALID[%s]", service.getName()));
            } catch (ServiceConfigurationError sce) {
//                sce.printStackTrace();
                brokenServiceCount++;
            } catch (Throwable th) {
                String faultingClassName = th.getStackTrace()[0].getClassName();
                MixinService.logBuffer.debug("MixinService [{}] failed initialisation: {}", faultingClassName, th.getMessage());
                int pos = faultingClassName.lastIndexOf('.');
                badServices.add(String.format("ERROR[%s]", pos < 0 ? faultingClassName : faultingClassName.substring(pos + 1)));
//                th.printStackTrace();
            }
        }
        
        String brokenServiceNote = brokenServiceCount == 0 ? "" : " and " + brokenServiceCount + " other invalid services.";
        throw new ServiceNotAvailableError("No mixin host service is available. Services: " + Joiner.on(", ").join(badServices) + brokenServiceNote);
    }

    /**
     * Blackboard
     */
    public static IGlobalPropertyService getGlobalPropertyService() {
        return MixinService.getInstance().getGlobalPropertyServiceInstance();
    }

    /**
     * Retrieves the GlobalPropertyService Instance... FactoryProviderBean...
     * Observer...InterfaceStream...Function...Broker... help me why won't it
     * stop
     */
    private IGlobalPropertyService getGlobalPropertyServiceInstance() {
        if (this.propertyService == null) {
            this.propertyService = this.initPropertyService();
        }
        return this.propertyService;
    }

    private IGlobalPropertyService initPropertyService() {
        ServiceLoader<IGlobalPropertyService> serviceLoader = ServiceLoader.<IGlobalPropertyService>load(IGlobalPropertyService.class,
                this.getClass().getClassLoader());
        
        Iterator<IGlobalPropertyService> iter = serviceLoader.iterator();
        while (iter.hasNext()) {
            try {
                IGlobalPropertyService service = iter.next();
                return service;
            } catch (ServiceConfigurationError serviceError) {
//                serviceError.printStackTrace();
            } catch (Throwable th) {
//                th.printStackTrace();
            }
        }
        throw new ServiceNotAvailableError("No mixin global property service is available");
    }

    /**
     * Returns Object so that ILogger is not classloaded until after it doesn't
     * matter any more
     */
    @SuppressWarnings("unchecked")
    private static <T> T getDefaultLogger() {
        return (T)new LoggerAdapterConsole("mixin").setDebugStream(System.err);
    }

}
