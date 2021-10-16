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
package org.spongepowered.tools.obfuscation.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

import javax.tools.Diagnostic.Kind;

import org.spongepowered.tools.obfuscation.ObfuscationType;
import org.spongepowered.tools.obfuscation.SupportedOptions;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerEx.MessageType;

import com.google.common.base.Joiner;

import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;

/**
 * Obfuscation service manager
 */
public final class ObfuscationServices {

    /**
     * Singleton
     */
    private static ObfuscationServices instance;
    
    /**
     * Service loader 
     */
    private final ServiceLoader<IObfuscationService> serviceLoader;
    
    /**
     * Initialised services 
     */
    private final Set<IObfuscationService> services = new HashSet<IObfuscationService>();
    
    /**
     * True if provider init was already completed
     */
    private boolean providerInitDone = false;
    
    /**
     * Singleton pattern
     */
    private ObfuscationServices() {
        this.serviceLoader = ServiceLoader.<IObfuscationService>load(IObfuscationService.class, this.getClass().getClassLoader());
    }
    
    /**
     * Singleton pattern, get or create the instance
     */
    public static ObfuscationServices getInstance() {
        if (ObfuscationServices.instance == null) {
            ObfuscationServices.instance = new ObfuscationServices();
        }
        return ObfuscationServices.instance;
    }
    
    /**
     * Initialise services
     * 
     * @param ap annotation processor
     */
    public void initProviders(IMixinAnnotationProcessor ap) {
        if (this.providerInitDone) {
            return;
        }
        this.providerInitDone = true;
        
        boolean defaultIsPresent = false;
        
        Map<String, Set<String>> supportedTypes = new LinkedHashMap<String, Set<String>>();
        
        try {
            for (IObfuscationService service : this.serviceLoader) {
                if (!this.services.contains(service)) {
                    this.services.add(service);
                    
                    String serviceName = service.getClass().getSimpleName();
//                    ap.printMessage(Kind.NOTE, "Preparing service " + serviceName);
                    Collection<ObfuscationTypeDescriptor> obfTypes = service.getObfuscationTypes(ap);
                    if (obfTypes != null) {
                        for (ObfuscationTypeDescriptor obfType : obfTypes) {
                            try {
                                ObfuscationType type = ObfuscationType.create(obfType, ap);
                                Set<String> types = supportedTypes.get(serviceName);
                                if (types == null) {
                                    supportedTypes.put(serviceName, types = new LinkedHashSet<String>());
                                }
                                types.add(type.getKey());
                                defaultIsPresent |= type.isDefault();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (ServiceConfigurationError serviceError) {
            ap.printMessage(Kind.ERROR, serviceError.getClass().getSimpleName() + ": " + serviceError.getMessage());
            serviceError.printStackTrace();
        }
        
        if (supportedTypes.size() > 0) {
            StringBuilder sb = new StringBuilder("Supported obfuscation types:");
            for (Entry<String, Set<String>> supportedType : supportedTypes.entrySet()) {
                sb.append(' ').append(supportedType.getKey()).append(" supports [").append(Joiner.on(',').join(supportedType.getValue())).append(']');
            }
            ap.printMessage(MessageType.INFO, sb.toString());
        }
        
        if (!defaultIsPresent) {
            String defaultEnv = ap.getOption(SupportedOptions.DEFAULT_OBFUSCATION_ENV);
            if (defaultEnv == null) {
                ap.printMessage(Kind.WARNING, "No default obfuscation environment was specified and \"" + ObfuscationType.DEFAULT_TYPE
                    + "\" is not available. Please ensure defaultObfuscationEnv is specified in your build configuration");
            } else {
                ap.printMessage(Kind.WARNING, "Specified default obfuscation environment \"" + defaultEnv.toLowerCase(Locale.ROOT)
                        + "\" was not defined. This probably means your build configuration is out of date or a required service is missing");
            }
        }
    }

    /**
     * Get the options supported by all available providers
     */
    public Set<String> getSupportedOptions() {
        Set<String> supportedOptions = new HashSet<String>();
        for (IObfuscationService provider : this.services) {
            Set<String> options = provider.getSupportedOptions();
            if (options != null) {
                supportedOptions.addAll(options);
            }
        }
        return supportedOptions;
    }
    
    /**
     * Get the service instance for the specified class from the service loader
     * 
     * @param serviceClass service class
     * @return service instance or null if no matching services were loaded
     */
    public IObfuscationService getService(Class<? extends IObfuscationService> serviceClass) {
        for (IObfuscationService service : this.services) {
            if (serviceClass.getName().equals(service.getClass().getName())) {
                return service;
            }
        }
        return null;
    }
}
