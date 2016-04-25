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
package org.spongepowered.asm.mixin.transformer;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinInitialisationError;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.struct.ReferenceMapper;
import org.spongepowered.asm.mixin.transformer.debug.IHotSwap;
import org.spongepowered.asm.util.VersionNumber;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import net.minecraft.launchwrapper.Launch;

/**
 * Mixin configuration bundle
 */
class MixinConfig implements Comparable<MixinConfig>, IMixinConfig {
    
    /**
     * Wrapper for injection options
     */
    static class InjectorOptions {
        
        @SerializedName("defaultRequire")
        int defaultRequireValue = 0;
        
        @SerializedName("defaultGroup")
        String defaultGroup = "default";
        
    }
    
    /**
     * Global order of mixin configs, used to determine ordering between configs
     * with equivalent priority
     */
    private static int configOrder = 0;

    /**
     * Global list of mixin classes, so we can skip any duplicates
     */
    private static final Set<String> globalMixinList = new HashSet<String>();

    /**
     * Log even more things
     */
    private final Logger logger = LogManager.getLogger("mixin");
    
    /**
     * Map of mixin target classes to mixin infos
     */
    private final transient Map<String, List<MixinInfo>> mixinMapping = new HashMap<String, List<MixinInfo>>();
    
    /**
     * Targets for this configuration which haven't been mixed yet 
     */
    private final transient Set<String> unhandledTargets = new HashSet<String>();
    
    /**
     * All mixins loaded by this config 
     */
    private final transient List<MixinInfo> mixins = new ArrayList<MixinInfo>();
    
    /**
     * Synthetic inner classes for mixins in this set
     */
    private final transient Set<String> syntheticInnerClasses = new HashSet<String>();

    /**
     * Minimum version of the mixin subsystem required to correctly apply mixins
     * in this configuration. 
     */
    @SerializedName("minVersion")
    private String version; 
    
    /**
     * Determines whether failures in this mixin config are considered terminal
     * errors. Use this setting to indicate that failing to apply a mixin in
     * this config is a critical error and should cause the game to shutdown.
     */
    @SerializedName("required")
    private boolean required;
    
    /**
     * Configuration priority
     */
    @SerializedName("priority")
    private int priority = IMixinConfig.DEFAULT_PRIORITY;
    
    /**
     * Default mixin priority. By default, mixins get a priority of 
     * {@link IMixinConfig#DEFAULT_PRIORITY DEFAULT_PRIORITY} unless a different
     * value is specified in the annotation. This setting allows the base 
     * priority for all mixins in this config to be set to an alternate value.
     */
    @SerializedName("mixinPriority")
    private int mixinPriority = IMixinConfig.DEFAULT_PRIORITY;

    /**
     * Package containing all mixins. This package will be monitored by the
     * transformer so that we can explode if some dummy tries to reference a
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
     * Mixin classes to load ONLY on dedicated server, mixinPackage will be
     * prepended
     */
    @SerializedName("server")
    private List<String> mixinClassesServer;
    
    /**
     * True to set the sourceFile property when applying mixins
     */
    @SerializedName("setSourceFile")
    private boolean setSourceFile = false;
    
    /**
     * The path to the reference map resource to use for this configuration
     */
    @SerializedName("refmap")
    private String refMapperConfig;
    
    /**
     * True to output "mixing in" messages at INFO level rather than DEBUG 
     */
    @SerializedName("verbose")
    private boolean verboseLogging;
    
    /**
     * Intrinsic order (for sorting configurations with identical priority)
     */
    private final transient int order = MixinConfig.configOrder++;

    /**
     * Parent environment 
     */
    private transient MixinEnvironment env;
    
    /**
     * Name of the file this config was initialised from
     */
    private transient String name;
    
    /**
     * Name of the {@link IMixinConfigPlugin} to hook onto this MixinConfig 
     */
    @SerializedName("plugin")
    private String pluginClassName;
    
    /**
     * Injector options 
     */
    @SerializedName("injectors")
    private InjectorOptions injectorOptions = new InjectorOptions();
    
    /**
     * Config plugin, if supplied
     */
    private transient IMixinConfigPlugin plugin;
    
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
     * 
     * @param env Parent environment
     * @param name Mixin config name
     * @return true if the config was successfully initialised and should be
     *      returned, or false if initialisation failed and the config should
     *      be discarded
     */
    private boolean onLoad(MixinEnvironment env, String name) {
        this.env = env;
        this.name = name;
        
        VersionNumber minVersion = VersionNumber.parse(this.version);
        VersionNumber curVersion = VersionNumber.parse(env.getVersion());
        if (minVersion.compareTo(curVersion) > 0) {
            this.logger.warn("Mixin config {} requires mixin subsystem version {} but {} was found. The mixin config will not be applied.",
                    this.name, minVersion, curVersion);
            
            if (this.required) {
                throw new MixinInitialisationError("Required mixin config " + this.name + " requires mixin subsystem version " + minVersion);
            }
            
            return false;
        }
        
        if (this.pluginClassName != null) {
            try {
                Class<?> pluginClass = Class.forName(this.pluginClassName, true, Launch.classLoader);
                this.plugin = (IMixinConfigPlugin)pluginClass.newInstance();
                
                if (this.plugin != null) {
                    this.plugin.onLoad(this.mixinPackage);
                }
            } catch (Throwable th) {
                th.printStackTrace();
                this.plugin = null;
            }
        }

        if (!this.mixinPackage.endsWith(".")) {
            this.mixinPackage += ".";
        }
        
        if (this.refMapperConfig == null) {
            if (this.plugin != null) {
                this.refMapperConfig = this.plugin.getRefMapperConfig();
            }
            
            if (this.refMapperConfig == null) {
                this.refMapperConfig = ReferenceMapper.DEFAULT_RESOURCE;
            }
        }
        
        this.refMapper = ReferenceMapper.read(this.refMapperConfig);
        this.verboseLogging |= env.getOption(Option.DEBUG_VERBOSE);
        
        return true;
    }

    /**
     * <p>Initialisation routine. It's important that we call this routine as
     * late as possible. In general we want to call it on the first call to
     * transform() in the parent transformer. At the very least we want to be
     * called <em>after</em> all the transformers for the current environment
     * have been spawned, because we will run the mixin bytecode through the
     * transformer chain and naturally we want this to happen at a point when we
     * can be reasonably sure that all transfomers have loaded.</p>
     * 
     * <p>For this reason we will invoke the initialisation on the first call to
     * either the <em>hasMixinsFor()</em> or <em>getMixinsFor()</em> methods.
     * </p>
     */
    void initialise(IHotSwap hotSwapper) {
        if (this.initialised) {
            return;
        }
        this.initialised = true;
        
        this.initialiseMixins(this.mixinClasses, false, hotSwapper);
        
        switch (this.env.getSide()) {
            case CLIENT:
                this.initialiseMixins(this.mixinClassesClient, false, hotSwapper);
                break;
            case SERVER:
                this.initialiseMixins(this.mixinClassesServer, false, hotSwapper);
                break;
            case UNKNOWN:
                //$FALL-THROUGH$
            default:
                this.logger.warn("Mixin environment was unable to detect the current side, sided mixins will not be applied");
                break;
        }
    }
    
    void postInitialise(IHotSwap hotSwapper) {
        if (this.plugin != null) {
            List<String> pluginMixins = this.plugin.getMixins();
            this.initialiseMixins(pluginMixins, true, hotSwapper);
        }
        
        for (Iterator<MixinInfo> iter = this.mixins.iterator(); iter.hasNext();) {
            MixinInfo mixin = iter.next();
            try {
                mixin.validate();
                for (String innerClass : mixin.getSyntheticInnerClasses()) {
                    this.syntheticInnerClasses.add(innerClass.replace('/', '.'));
                }
            } catch (InvalidMixinException ex) {
                this.logger.error(ex.getMixin() + ": " + ex.getMessage(), ex);
                this.removeMixin(mixin);
                iter.remove();
            } catch (Exception ex) {
                this.logger.error(ex.getMessage(), ex);
                this.removeMixin(mixin);
                iter.remove();
            }
        }
    }

    private void removeMixin(MixinInfo remove) {
        for (List<MixinInfo> mixinsFor : this.mixinMapping.values()) {
            for (Iterator<MixinInfo> iter = mixinsFor.iterator(); iter.hasNext();) {
                if (remove == iter.next()) {
                    iter.remove();
                }
            }
        }
    }

    private void initialiseMixins(List<String> mixinClasses, boolean suppressPlugin, IHotSwap hotSwapper) {
        if (mixinClasses == null) {
            return;
        }
        
        for (String mixinClass : mixinClasses) {
            String fqMixinClass = this.mixinPackage + mixinClass;
            
            if (mixinClass == null || MixinConfig.globalMixinList.contains(fqMixinClass)) {
                continue;
            }
            
            MixinInfo mixin = null;
            
            try {
                mixin = new MixinInfo(this, mixinClass, true, this.plugin, suppressPlugin);
                if (mixin.getTargetClasses().size() > 0) {
                    MixinConfig.globalMixinList.add(fqMixinClass);
                    for (String targetClass : mixin.getTargetClasses()) {
                        String targetClassName = targetClass.replace('/', '.');
                        this.mixinsFor(targetClassName).add(mixin);
                        this.unhandledTargets.add(targetClassName);
                    }
                    if (hotSwapper != null) {
                        hotSwapper.registerMixinClass(mixin.getClassName());
                    }
                    this.mixins.add(mixin);
                }
            } catch (InvalidMixinException ex) {
                if (this.required) {
                    throw ex;
                }
                this.logger.error(ex.getMessage(), ex);
            } catch (Exception ex) {
                if (this.required) {
                    throw new InvalidMixinException(mixin, "Error initialising mixin " + mixin + " - " + ex.getClass() + ": " + ex.getMessage(), ex);
                }
                this.logger.error(ex.getMessage(), ex);
            }
        }
    }

    void postApply(String transformedName, ClassNode targetClass) {
        this.unhandledTargets.remove(transformedName);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.IMixinConfig#isRequired()
     */
    @Override
    public boolean isRequired() {
        return this.required;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.extensibility.IMixinConfig
     *      #getEnvironment()
     */
    @Override
    public MixinEnvironment getEnvironment() {
        return this.env;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.IMixinConfig#getName()
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Get the package containing all mixin classes
     */
    @Override
    public String getMixinPackage() {
        return this.mixinPackage;
    }
    
    /**
     * Get the priority
     */
    @Override
    public int getPriority() {
        return this.priority;
    }

    /**
     * Get the default priority for mixins in this config. Values specified in
     * the mixin annotation still override this value
     */
    public int getDefaultMixinPriority() {
        return this.mixinPriority;
    }
    
    /**
     * Get the defined value for the {@link Inject#require} parameter on
     * injectors defined in mixins in this configuration.
     * 
     * @return default require value
     */
    public int getDefaultRequiredInjections() {
        return this.injectorOptions.defaultRequireValue;
    }
    
    /**
     * Get the defined injector group for injectors
     * 
     * @return default group name
     */
    public String getDefaultInjectorGroup() {
        String defaultGroup = this.injectorOptions.defaultGroup;
        return defaultGroup != null && !defaultGroup.isEmpty() ? defaultGroup : "default";
    }
    
    /**
     * Get the number of mixins in this config, for debug logging
     * 
     * @return total enumerated mixins in set
     */
    int getDeclaredMixinCount() {
        return MixinConfig.getCollectionSize(this.mixinClasses, this.mixinClassesClient, this.mixinClassesServer);
    }

    /**
     * Get the number of mixins actually initialised, for debug logging
     * 
     * @return total enumerated mixins in set
     */
    int getMixinCount() {
        return this.mixins.size();
    }

    /**
     * Get the list of mixin classes we will be applying
     */
    public List<String> getClasses() {
        return Collections.<String>unmodifiableList(this.mixinClasses);
    }

    /**
     * Get whether to propogate the source file attribute from a mixin onto the
     * target class
     */
    public boolean shouldSetSourceFile() {
        return this.setSourceFile;
    }
    
    /**
     * Get the reference remapper for injectors
     */
    public ReferenceMapper getReferenceMapper() {
        if (this.env.getOption(Option.DISABLE_REFMAP)) {
            return ReferenceMapper.DEFAULT_MAPPER;
        }
        this.refMapper.setContext(this.env.getRefmapObfuscationContext());
        return this.refMapper;
    }
    
    String remapClassName(String className, String reference) {
//        String remapped = this.plugin != null ? this.plugin.remap(className, reference) : null;
//        if (remapped != null) {
//            return remapped;
//        }
        return this.getReferenceMapper().remap(className, reference);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.IMixinConfig#getPlugin()
     */
    @Override
    public IMixinConfigPlugin getPlugin() {
        return this.plugin;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.IMixinConfig#getTargets()
     */
    @Override
    public Set<String> getTargets() {
        return Collections.<String>unmodifiableSet(this.mixinMapping.keySet());
    }
    
    /**
     * Get targets for this configuration
     */
    public Set<String> getUnhandledTargets() {
        return Collections.<String>unmodifiableSet(this.unhandledTargets);
    }
    
    /**
     * Get the logging level for this config
     */
    public Level getLoggingLevel() {
        return this.verboseLogging ? Level.INFO : Level.DEBUG;
    }

    /**
     * Get whether this config's package matches the supplied class name
     * 
     * @param className Class name to check
     * @return True if the specified class name is in this config's mixin
     *      package
     */
    public boolean packageMatch(String className) {
        return className.startsWith(this.mixinPackage);
    }

    /**
     * Get whether this config can allow passthrough for the specified class
     * 
     * @param className Class name to check
     * @return True if the specified class name is in this config's passthrough
     *      set
     */
    public boolean canPassThrough(String className) {
        return this.syntheticInnerClasses.contains(className);
    }

    /**
     * Check whether this configuration bundle has a mixin for the specified
     * class
     * 
     * @param targetClass
     * @return true if this bundle contains any mixins for the specified target
     */
    public boolean hasMixinsFor(String targetClass) {
        return this.mixinMapping.containsKey(targetClass);
    }
    
    /**
     * Get mixins for the specified target class
     * 
     * @param targetClass
     * @return mixins for the specified target
     */
    public List<MixinInfo> getMixinsFor(String targetClass) {
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
     * Updates a mixin with new bytecode
     *
     * @param mixinClass Name of the mixin class
     * @param bytes New bytecode
     * @return List of classes that need to be updated
     */
    public List<String> reloadMixin(String mixinClass, byte[] bytes) {
        for (Iterator<MixinInfo> iter = this.mixins.iterator(); iter.hasNext();) {
            MixinInfo mixin = iter.next();
            if (mixin.getClassName().equals(mixinClass)) {
                mixin.reloadMixin(bytes);
                return mixin.getTargetClasses();
            }
        }
        return Collections.<String>emptyList();
    }
    
    @Override
    public String toString() {
        return this.name;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(MixinConfig other) {
        if (other == null) {
            return 0;
        }
        if (other.priority == this.priority) {
            return this.order - other.order;
        }
        return (this.priority - other.priority);
    }

    /**
     * Factory method, creates a new mixin configuration bundle from the
     * specified configFile, which must be accessible on the classpath
     * 
     * @param configFile
     * @param env parent environment
     * @return
     */
    static MixinConfig create(String configFile, MixinEnvironment env) {
        try {
            MixinConfig config = new Gson().fromJson(new InputStreamReader(Launch.classLoader.getResourceAsStream(configFile)), MixinConfig.class);
            if (config.onLoad(env, configFile)) {
                return config;
            }
            return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(String.format("The specified configuration file '%s' was invalid or could not be read", configFile));
        }
    }

    private static int getCollectionSize(Collection<?>... collections) {
        int total = 0;
        for (Collection<?> collection : collections) {
            if (collection != null) {
                total += collection.size();
            }
        }
        return total;
    }

}
