/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered.org <http://www.spongepowered.org>
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
package org.spongepowered.asm.mixin.transformer;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.launchwrapper.Launch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.struct.ReferenceMapper;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * Mixin configuration bundle
 */
class MixinConfig {

    /**
     * Log even more things
     */
    private final Logger logger = LogManager.getLogger("mixin");
    
    /**
     * Map of mixin target classes to mixin infos
     */
    private final transient Map<String, List<MixinInfo>> mixinMapping = new HashMap<String, List<MixinInfo>>();
    
    /**
     * Package containing all mixins. This package will be monitored by the transformer so that we can explode if some dummy tries to reference a
     * mixin class directly.
     */
    @SerializedName("package")
    private String mixinPackage;
    
    /**
     * Mixin classes to load, mixinPackage will be prepended
     */
    @SerializedName("mixins")
    private List<String> mixinClasses;
    
    /**
     * Mixin classes to load ONLY on client, mixinPackage will be prepended
     */
    @SerializedName("client")
    private List<String> mixinClassesClient;
    
    /**
     * Mixin classes to load ONLY on dedicated server, mixinPackage will be prepended
     */
    @SerializedName("server")
    private List<String> mixinClassesServer;
    
    /**
     * True to set the sourceFile property when applying mixins
     */
    @SerializedName("setSourceFile")
    private boolean setSourceFile = false;
    
    /**
     * True to set the sourceFile property when applying mixins
     */
    @SerializedName("referenceMap")
    private String refMapperConfig;
    
    /**
     * Name of the file this config was initialised from
     */
    private transient String name;
    
    /**
     * Reference mapper for injectors
     */
    private transient ReferenceMapper refMapper;

    /**
     * Keep track of initialisation state 
     */
    private transient boolean initialised = false;
    
    /**
     * Spawn via GSON, no public ctor for you 
     */
    private MixinConfig() {}

    /**
     * Called immediately after deserialisation 
     */
    private void onLoad(String name) {
        this.name = name;
        
        if (!this.mixinPackage.endsWith(".")) {
            this.mixinPackage += ".";
        }
        
        Launch.classLoader.addClassLoaderExclusion(this.mixinPackage);
        
        if (this.refMapperConfig == null) {
            this.refMapperConfig = ReferenceMapper.DEFAULT_RESOURCE;
        }
        
        this.refMapper = ReferenceMapper.read(this.refMapperConfig);
    }

    /**
     * <p>Initialisation routine. It's important that we call this routine as late as possible. In general we want to call it on the first call to
     * transform() in the parent transformer. At the very least we want to be called <em>after</em> all the transformers for the current environment
     * have been spawned, because we will run the mixin bytecode through the transformer chain and naturally we want this to happen at a point when
     * we can be reasonably sure that all transfomers have loaded.</p>
     * 
     * <p>For this reason we will invoke the initialisation on the first call to either the <em>hasMixinsFor()</em> or <em>getMixinsFor()</em>
     * methods.</p>
     */
    private void initialise() {
        this.initialised = true;
        
        this.initialiseSide(this.mixinClasses);
        
        switch (MixinEnvironment.getCurrentEnvironment().getSide()) {
            case CLIENT :
                this.initialiseSide(this.mixinClassesClient);
                break;
            case SERVER :
                this.initialiseSide(this.mixinClassesServer);
                break;
            case UNKNOWN :
                this.logger.warn("Mixin environment was unable to detect the current side, sided mixins will not be applied");
                break;
        }
    }

    private void initialiseSide(List<String> mixinClasses) {
        if (mixinClasses == null) {
            return;
        }
        
        for (String mixinClass : mixinClasses) {
            if (mixinClass == null) {
                continue;
            }
            try {
                MixinInfo mixin = new MixinInfo(this, mixinClass, true);
                for (String targetClass : mixin.getTargetClasses()) {
                    this.mixinsFor(targetClass).add(mixin);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Get the name of the file from which this configuration object was initialised
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the package containing all mixin classes
     */
    public String getMixinPackage() {
        return this.mixinPackage;
    }
    
    /**
     * Get the list of mixin classes we will be applying
     */
    public List<String> getClasses() {
        return this.mixinClasses;
    }

    /**
     * Get whether to propogate the source file attribute from a mixin onto the target class
     */
    public boolean shouldSetSourceFile() {
        return this.setSourceFile;
    }
    
    /**
     * Get the reference remapper for injectors
     */
    public ReferenceMapper getReferenceMapper() {
        return this.refMapper;
    }
    
    /**
     * Check whether this configuration bundle has a mixin for the specified class
     * 
     * @param targetClass
     * @return
     */
    public boolean hasMixinsFor(String targetClass) {
        if (!this.initialised) {
            this.initialise();
        }
        
        return this.mixinMapping.containsKey(targetClass);
    }
    
    /**
     * Get mixins for the specified target class
     * 
     * @param targetClass
     * @return
     */
    public List<MixinInfo> getMixinsFor(String targetClass) {
        if (!this.initialised) {
            this.initialise();
        }
        
        return this.mixinsFor(targetClass);
    }

    private List<MixinInfo> mixinsFor(String targetClass) {
        List<MixinInfo> mixins = this.mixinMapping.get(targetClass);
        if (mixins == null) {
            mixins = new ArrayList<MixinInfo>();
            this.mixinMapping.put(targetClass, mixins);
        }
        return mixins;
    }
    
    /**
     * Factory method, creates a new mixin configuration bundle from the specified configFile, which must be accessible on the classpath
     * 
     * @param configFile
     * @return
     */
    static MixinConfig create(String configFile) {
        try {
            MixinConfig config = new Gson().fromJson(new InputStreamReader(Launch.classLoader.getResourceAsStream(configFile)), MixinConfig.class);
            config.onLoad(configFile);
            return config;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(String.format("The specified configuration file '%s' was invalid or could not be read", configFile));
        }
    }
}
