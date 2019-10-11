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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.connect.IMixinConnector;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.MixinService;

/**
 * Manager for Mixin containers bootstrapping via {@link IMixinConnector}
 */
public class MixinConnectorManager {
    
    /**
     * Logging to the max
     */
    private static final Logger logger = LogManager.getLogger("mixin");
    
    private final Set<String> connectorClasses = new LinkedHashSet<String>();

    private final List<IMixinConnector> connectors = new ArrayList<IMixinConnector>();

    MixinConnectorManager() {
    }

    void addConnector(String connectorClass) {
        this.connectorClasses.add(connectorClass);
    }

    void inject() {
        this.loadConnectors();
        this.initConnectors();
    }

    @SuppressWarnings("unchecked")
    void loadConnectors() {
        IClassProvider classProvider = MixinService.getService().getClassProvider();
        
        for (String connectorClassName : this.connectorClasses) {
            Class<IMixinConnector> connectorClass = null; 
            try {
                Class<?> clazz = classProvider.findClass(connectorClassName);
                if (!IMixinConnector.class.isAssignableFrom(clazz)) {
                    MixinConnectorManager.logger.error("Mixin Connector [" + connectorClassName + "] does not implement IMixinConnector");
                    continue;
                }
                connectorClass = (Class<IMixinConnector>)clazz;
            } catch (ClassNotFoundException ex) {
                MixinConnectorManager.logger.catching(ex);
                continue;
            }
            
            try {
                IMixinConnector connector = connectorClass.newInstance();
                this.connectors.add(connector);
                MixinConnectorManager.logger.info("Successfully loaded Mixin Connector [" + connectorClassName + "]");
            } catch (ReflectiveOperationException ex) {
                MixinConnectorManager.logger.warn("Error loading Mixin Connector [" + connectorClassName + "]", ex);
            }
        }
        this.connectorClasses.clear();
    }
    
    void initConnectors() {
        for (IMixinConnector connector : this.connectors) {
            try {
                connector.connect();
            } catch (Exception ex) {
                MixinConnectorManager.logger.warn("Error initialising Mixin Connector [" + connector.getClass().getName() + "]", ex);
            }
        }
    }

}
