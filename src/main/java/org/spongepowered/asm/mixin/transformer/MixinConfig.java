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

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * Mixin configuration bundle
 */
class MixinConfig {
    
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
     * True to set the sourceFile property when applying mixins
     */
    @SerializedName("setSourceFile")
    private boolean setSourceFile = false;
    
    /**
     * Keep track of initialisation state 
     */
    private transient boolean initialised = false;
    
    /**
     * Spawn via GSON, no public ctor for you 
     */
    private MixinConfig() {}

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
        
        if (!this.mixinPackage.endsWith(".")) {
            this.mixinPackage += ".";
        }
        
        for (String mixinClass : this.mixinClasses) {
            try {
                MixinInfo mixin = new MixinInfo(this, mixinClass, true);
                for (String targetClass : mixin.getTargetClasses()) {
                    this.getMixinsFor(targetClass).add(mixin);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
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
            return new Gson().fromJson(new InputStreamReader(MixinConfig.class.getResourceAsStream("/" + configFile)), MixinConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(String.format("The specified configuration file '%s' was invalid or could not be read", configFile));
        }
    }
}
