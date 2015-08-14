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
package org.spongepowered.asm.mixin;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.helpers.Booleans;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.extensibility.IEnvironmentTokenProvider;
import org.spongepowered.asm.mixin.transformer.MixinTransformer;
import org.spongepowered.asm.util.ITokenProvider;
import org.spongepowered.asm.util.PrettyPrinter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;


/**
 * The mixin environment manages global state information for the mixin
 * subsystem.
 */
public class MixinEnvironment implements ITokenProvider {
    
    /**
     * Environment phase, deliberately not implemented as an enum
     */
    public static class Phase {
        
        /**
         * Not initialised phase 
         */
        static final Phase NOT_INITIALISED = new Phase(-1, "NOT_INITIALISED");
        
        /**
         * "Pre initialisation" phase, everything before the tweak system begins
         * to load the game
         */
        public static final Phase PREINIT = new Phase(0, "PREINIT");
        
        /**
         * "Default" phase, during runtime
         */
        public static final Phase DEFAULT = new Phase(1, "DEFAULT");
        
        /**
         * All phases
         */
        static final List<Phase> phases = ImmutableList.of(
            Phase.PREINIT,
            Phase.DEFAULT
        );
        
        /**
         * Phase ordinal
         */
        final int ordinal;
        
        /**
         * Phase name
         */
        final String name;
        
        private Phase(int ordinal, String name) {
            this.ordinal = ordinal;
            this.name = name;
        }
        
        @Override
        public String toString() {
            return this.name;
        }

        public static Phase forName(String name) {
            for (Phase phase : Phase.phases) {
                if (phase.name.equals(name)) {
                    return phase;
                }
            }
            return null;
        }
    }
    
    /**
     * Represents a "side", client or dedicated server
     */
    public static enum Side {
        
        /**
         * The environment was unable to determine current side
         */
        UNKNOWN {
            @Override
            protected boolean detect() {
                return false;
            }
        },
        
        /**
         * Client-side environment 
         */
        CLIENT {
            @Override
            protected boolean detect() {
                String sideName = this.getSideName();
                return "CLIENT".equals(sideName);
            }
        },
        
        /**
         * (Dedicated) Server-side environment 
         */
        SERVER {
            @Override
            protected boolean detect() {
                String sideName = this.getSideName();
                return "SERVER".equals(sideName) || "DEDICATEDSERVER".equals(sideName);
            }
        };
        
        protected abstract boolean detect();

        @SuppressWarnings("unchecked")
        protected final String getSideName() {
            // Using this method first prevents us from accidentally loading FML classes
            // too early when using the tweaker in dev
            for (ITweaker tweaker : (List<ITweaker>)Launch.blackboard.get("Tweaks")) {
                if (tweaker.getClass().getName().endsWith(".common.launcher.FMLServerTweaker")) {
                    return "SERVER";
                } else if (tweaker.getClass().getName().endsWith(".common.launcher.FMLTweaker")) {
                    return "CLIENT";
                }
            }
            
            String name = this.getSideName("net.minecraftforge.fml.relauncher.FMLLaunchHandler", "side");
            if (name != null) {
                return name;
            }
            
            name = this.getSideName("cpw.mods.fml.relauncher.FMLLaunchHandler", "side");
            if (name != null) {
                return name;
            }
            
            name = this.getSideName("com.mumfrey.liteloader.core.LiteLoader", "getEnvironmentType");
            if (name != null) {
                return name;
            }
            
            return "UNKNOWN";
        }

        private String getSideName(String className, String methodName) {
            try {
                Class<?> clazz = Class.forName(className, false, Launch.classLoader);
                Method method = clazz.getDeclaredMethod(methodName);
                return ((Enum<?>)method.invoke(null)).name();
            } catch (Exception ex) {
                return null;
            }
        }
    }
    
    /**
     * Mixin options
     */
    public static enum Option {
        
        /**
         * Enable all debugging options
         */
        DEBUG_ALL("debug"),
        
        /**
         * Enable post-mixin class export. This causes all classes to be written
         * to the .mixin.out directory within the runtime directory
         * <em>after</em> mixins are applied, for debugging purposes. 
         */
        DEBUG_EXPORT(Option.DEBUG_ALL, "export"),
        
        /**
         * Export filter, if omitted allows all transformed classes to be
         * exported. If specified, acts as a filter for class names to export
         * and only matching classes will be exported. This is useful when using
         * Fernflower as exporting can be otherwise very slow. The following
         * wildcards are allowed:
         * 
         * <dl>
         *   <dt>*</dt><dd>Matches one or more characters except dot (.)</dd>
         *   <dt>**</dt><dd>Matches any number of characters</dd>
         *   <dt>?</dt><dd>Matches exactly one character</dd>
         * </dl>
         */
        DEBUG_EXPORT_FILTER(Option.DEBUG_EXPORT, "filter"),
        
        /**
         * Run the CheckClassAdapter on all classes after mixins are applied 
         */
        DEBUG_VERIFY(Option.DEBUG_ALL, "verify"),
        
        /**
         * Enable verbose mixin logging (elevates all DEBUG level messages to
         * INFO level) 
         */
        DEBUG_VERBOSE(Option.DEBUG_ALL, "verbose"),
        
        /**
         * Dumps the bytecode for the target class to disk when mixin
         * application fails
         */
        DUMP_TARGET_ON_FAILURE("dumpTargetOnFailure"),
        
        /**
         * Enable all checks 
         */
        CHECK_ALL("checks"),
        
        /**
         * Checks that all declared interface methods are implemented on a class
         * after mixin application.
         */
        CHECK_IMPLEMENTS(Option.CHECK_ALL, "interfaces"),
        
        /**
         * Ignore all constraints on mixin annotations, output warnings instead
         */
        IGNORE_CONSTRAINTS("ignoreConstraints");
        
        /**
         * Prefix for mixin options
         */
        private static final String PREFIX = "mixin";
        
        /**
         * Parent option to this option, if non-null then this option is enabled
         * if 
         */
        final Option parent;
        
        /**
         * Java property name
         */
        final String property;

        private Option(String property) {
            this(null, property);
        }
        
        private Option(Option parent, String property) {
            this.parent = parent;
            this.property = (parent != null ? parent.property : Option.PREFIX) + "." + property;
        }
        
        Option getParent() {
            return this.parent;
        }
        
        String getProperty() {
            return this.property;
        }
        
        boolean getBooleanValue() {
            return Booleans.parseBoolean(System.getProperty(this.property), false)
                    || (this.parent != null && this.parent.getBooleanValue());
        }
        
        String getStringValue() {
            return (this.parent == null || this.parent.getBooleanValue()) ? System.getProperty(this.property) : null;
        }
    }
    
    /**
     * Tweaker used to notify the environment when we transition from preinit to
     * default
     */
    public static class EnvironmentStateTweaker implements ITweaker {

        @Override
        public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        }

        @Override
        public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        }

        @Override
        public String getLaunchTarget() {
            return "";
        }

        @Override
        public String[] getLaunchArguments() {
            MixinEnvironment.gotoPhase(Phase.DEFAULT);
            return new String[0];
        }
        
    }
    
    /**
     * Wrapper for providing a natural sorting order for providers
     */
    static class TokenProviderWrapper implements Comparable<TokenProviderWrapper> {
        
        private static int nextOrder = 0;
        
        private final int priority, order;
        
        private final IEnvironmentTokenProvider provider;

        private final MixinEnvironment environment;
        
        public TokenProviderWrapper(IEnvironmentTokenProvider provider, MixinEnvironment environment) {
            this.provider = provider;
            this.environment = environment;
            this.order = TokenProviderWrapper.nextOrder++;
            this.priority = provider.getPriority();
        }

        @Override
        public int compareTo(TokenProviderWrapper other) {
            if (other == null) {
                return 0;
            }
            if (other.priority == this.priority) {
                return other.order - this.order;
            }
            return (other.priority - this.priority);
        }
        
        public IEnvironmentTokenProvider getProvider() {
            return this.provider;
        }
        
        Integer getToken(String token) {
            return this.provider.getToken(token, this.environment);
        }

    }
    
    /**
     * Known re-entrant transformers, other re-entrant transformers will
     * detected automatically 
     */
    private static final Set<String> excludeTransformers = Sets.<String>newHashSet(
        "net.minecraftforge.fml.common.asm.transformers.EventSubscriptionTransformer",
        "cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer",
        "net.minecraftforge.fml.common.asm.transformers.TerminalTransformer",
        "cpw.mods.fml.common.asm.transformers.TerminalTransformer"
    );

    // Blackboard keys
    private static final String CONFIGS_KEY = "mixin.configs";
    private static final String TRANSFORMER_KEY = "mixin.transformer";
    
    /**
     * Array of all (real) environments, indexed by ordinal
     */
    private static final MixinEnvironment[] environments = new MixinEnvironment[Phase.phases.size()];
    
    /**
     * Currently active environment
     */
    private static MixinEnvironment currentEnvironment;

    /**
     * Current (active) environment phase, set to NOT_INITIALISED until the
     * phases have been populated
     */
    private static Phase currentPhase = Phase.NOT_INITIALISED;
    
    /**
     * Show debug header info on first environment construction
     */
    private static boolean showHeader = true;
    
    /**
     * Logger 
     */
    private final Logger logger = LogManager.getLogger("mixin");

    /**
     * The phase for this environment
     */
    private final Phase phase;
    
    /**
     * The blackboard key for this environment's configs
     */
    private final String configsKey;
    
    /**
     * This environment's options
     */
    private final boolean[] options;
    
    /**
     * Error handlers for environment
     */
    private final Set<String> errorHandlers = new LinkedHashSet<String>();
    
    /**
     * List of token provider classes
     */
    private final Set<String> tokenProviderClasses = new HashSet<String>();
    
    /**
     * List of token providers in this environment 
     */
    private final List<TokenProviderWrapper> tokenProviders = new ArrayList<TokenProviderWrapper>();
    
    /**
     * Internal tokens defined by this environment
     */
    private final Map<String, Integer> internalTokens = new HashMap<String, Integer>();

    /**
     * Detected side 
     */
    private Side side;
    
    /**
     * Local transformer chain, this consists of all transformers present at the
     * init phase with the exclusion of the mixin transformer itself and known
     * re-entrant transformers. Detected re-entrant transformers will be
     * subsequently removed.
     */
    private List<IClassTransformer> transformers;
    
    /**
     * Class name transformer (if present)
     */
    private IClassNameTransformer nameTransformer;
    
    private MixinEnvironment(Phase phase) {
        this.phase = phase;
        this.configsKey = MixinEnvironment.CONFIGS_KEY + "." + this.phase.name.toLowerCase();
        
        // Sanity check
        Object version = Launch.blackboard.get(MixinBootstrap.INIT_KEY);
        if (version == null || !MixinBootstrap.VERSION.equals(version)) {
            throw new RuntimeException("Environment conflict, mismatched versions or you didn't call MixinBootstrap.init()");
        }
        
        // Also sanity check
        if (this.getClass().getClassLoader() != Launch.class.getClassLoader()) {
            throw new RuntimeException("Attempted to init the mixin environment in the wrong classloader");
        }
        
        this.options = new boolean[Option.values().length];
        for (Option option : Option.values()) {
            this.options[option.ordinal()] = option.getBooleanValue();
        }
        
        if (MixinEnvironment.showHeader) {
            MixinEnvironment.showHeader = false;
            
            Side side = this.getSide();
            String codeSource = this.getCodeSource();
            this.logger.info("SpongePowered MIXIN Subsystem Version={} Source={} Env={}", version, codeSource, side);
            
            if (this.getOption(Option.DEBUG_VERBOSE)) {
                PrettyPrinter printer = new PrettyPrinter(32);
                printer.add("SpongePowered MIXIN (Verbose debugging enabled)").centre().hr();
                printer.add("%25s : %s", "Code source", codeSource);
                printer.add("%25s : %s", "Internal Version", version).hr();
                for (Option option : Option.values()) {
                    printer.add("%25s : %s%s", option.property, option.parent == null ? "" : " - ", this.getOption(option));
                }
                printer.hr().add("%25s : %s", "Detected Side", side);
                printer.print(System.err);
            }
        }
    }

    private String getCodeSource() {
        try {
            return this.getClass().getProtectionDomain().getCodeSource().getLocation().toString();
        } catch (Throwable th) {
            return "Unknown";
        }
    }
    
    /**
     * Get the phase for this environment
     * 
     * @return the phase
     */
    public Phase getPhase() {
        return this.phase;
    }
    
    /**
     * Get mixin configurations from the blackboard
     * 
     * @return list of registered mixin configs
     */
    public List<String> getMixinConfigs() {
        @SuppressWarnings("unchecked")
        List<String> mixinConfigs = (List<String>) Launch.blackboard.get(this.configsKey);
        if (mixinConfigs == null) {
            mixinConfigs = new ArrayList<String>();
            Launch.blackboard.put(this.configsKey, mixinConfigs);
        }
        return mixinConfigs;
    }
    
    /**
     * Add a mixin configuration to the blackboard
     * 
     * @param config Name of configuration resource to add
     * @return fluent interface
     */
    public MixinEnvironment addConfiguration(String config) {
        List<String> configs = this.getMixinConfigs();
        if (!configs.contains(config)) {
            configs.add(config);
        }
        return this;
    }
    
    /**
     * Add a new error handler class to this environment
     * 
     * @param handlerName Handler class to add
     * @return fluent interface
     */
    public MixinEnvironment registerErrorHandlerClass(String handlerName) {
        this.errorHandlers.add(handlerName);
        return this;
    }
    
    /**
     * Add a new token provider class to this environment
     * 
     * @param providerName Class name of the token provider to add
     * @return fluent interface
     */
    public MixinEnvironment registerTokenProviderClass(String providerName) {
        if (!this.tokenProviderClasses.contains(providerName)) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends IEnvironmentTokenProvider> providerClass =
                    (Class<? extends IEnvironmentTokenProvider>)Class.forName(providerName, true, Launch.classLoader);
                IEnvironmentTokenProvider provider = providerClass.newInstance();
                this.registerTokenProvider(provider);
            } catch (Throwable th) {
                this.logger.error("Error instantiating " + providerName, th);
            }
        }
        return this;
    }

    /**
     * Add a new token provider to this environment
     * 
     * @param provider Token provider to add
     * @return fluent interface
     */
    public MixinEnvironment registerTokenProvider(IEnvironmentTokenProvider provider) {
        if (provider != null && !this.tokenProviderClasses.contains(provider.getClass().getName())) {
            String providerName = provider.getClass().getName();
            TokenProviderWrapper wrapper = new TokenProviderWrapper(provider, this);
            this.logger.info("Adding new token provider {} to {}", providerName, this);
            this.tokenProviders.add(wrapper);
            this.tokenProviderClasses.add(providerName);
            Collections.sort(this.tokenProviders);
        }
        
        return this;
    }
    
    /**
     * Get a token value from this environment
     * 
     * @param token Token to fetch
     * @return token value or null if the token is not present in the
     *      environment
     */
    @Override
    public Integer getToken(String token) {
        token = token.toUpperCase();
        
        for (TokenProviderWrapper provider : this.tokenProviders) {
            Integer value = provider.getToken(token);
            if (value != null) {
                return value;
            }
        }
        
        return this.internalTokens.get(token);
    }
    
    /**
     * Get all registered error handlers for this environment
     * 
     * @return set of error handler class names
     */
    public Set<String> getErrorHandlerClasses() {
        return Collections.<String>unmodifiableSet(this.errorHandlers);
    }

    /**
     * Get the active mixin transformer instance (if any)
     * 
     * @return active mixin transformer instance
     */
    public Object getActiveTransformer() {
        return Launch.blackboard.get(MixinEnvironment.TRANSFORMER_KEY);
    }

    /**
     * Set the mixin transformer instance
     * 
     * @param transformer Mixin Transformer
     */
    public void setActiveTransformer(IClassTransformer transformer) {
        if (transformer != null) {
            Launch.blackboard.put(MixinEnvironment.TRANSFORMER_KEY, transformer);        
        }
    }
    
    /**
     * Allows a third party to set the side if the side is currently UNKNOWN
     * 
     * @param side Side to set to
     * @return fluent interface
     */
    public MixinEnvironment setSide(Side side) {
        if (side != null && this.getSide() == Side.UNKNOWN && side != Side.UNKNOWN) {
            this.side = side;
        }
        return this;
    }
    
    /**
     * Get (and detect if necessary) the current side  
     * 
     * @return current side (or UNKNOWN if could not be determined)
     */
    public Side getSide() {
        if (this.side == null) {
            for (Side side : Side.values()) {
                if (side.detect()) {
                    this.side = side;
                    break;
                }
            }
        }
        
        return this.side != null ? this.side : Side.UNKNOWN;
    }
    
    /**
     * Get the current mixin subsystem version
     * 
     * @return current version
     */
    public String getVersion() {
        return (String)Launch.blackboard.get(MixinBootstrap.INIT_KEY);
    }

    /**
     * Get the specified option from the current environment
     * 
     * @param option Option to get
     * @return Option value
     */
    public boolean getOption(Option option) {
        return this.options[option.ordinal()];
    }
    
    /**
     * Set the specified option for this environment
     * 
     * @param option Option to set
     * @param value New option value
     */
    public void setOption(Option option, boolean value) {
        this.options[option.ordinal()] = value;
    }

    /**
     * Get the specified option from the current environment
     * 
     * @param option Option to get
     * @return Option value
     */
    public String getOptionValue(Option option) {
        return option.getStringValue();
    }
    
    /**
     * Invoke a mixin environment audit process
     */
    public void audit() {
        Object activeTransformer = this.getActiveTransformer();
        if (activeTransformer instanceof MixinTransformer) {
            MixinTransformer transformer = (MixinTransformer)activeTransformer;
            transformer.audit();
        }
    }

    /**
     * Returns (and generates if necessary) the transformer delegation list for
     * this environment.
     * 
     * @return current transformer delegation list (read-only)
     */
    public List<IClassTransformer> getTransformers() {
        if (this.transformers == null) {
            this.buildTransformerDelegationList();
        }
        
        return Collections.unmodifiableList(this.transformers);
    }

    /**
     * Adds a transformer to the transformer exclusions list
     * 
     * @param name Class transformer exclusion to add
     */
    public void addTransformerExclusion(String name) {
        MixinEnvironment.excludeTransformers.add(name);
        
        // Force rebuild of the list
        this.transformers = null;
    }

    /**
     * Map a class name back to its obfuscated counterpart 
     * 
     * @param className class name to unmap
     * @return obfuscated name for the specified deobfuscated reference
     */
    public String unmap(String className) {
        if (this.transformers == null) {
            this.buildTransformerDelegationList();
        }
        
        if (this.nameTransformer != null) {
            return this.nameTransformer.unmapClassName(className);
        }
        
        return className;
    }

    /**
     * Builds the transformer list to apply to loaded mixin bytecode. Since
     * generating this list requires inspecting each transformer by name (to
     * cope with the new wrapper functionality added by FML) we generate the
     * list just once per environment and cache the result.
     */
    private void buildTransformerDelegationList() {
        this.logger.debug("Rebuilding transformer delegation list:");
        this.transformers = new ArrayList<IClassTransformer>();
        for (IClassTransformer transformer : Launch.classLoader.getTransformers()) {
            String transformerName = transformer.getClass().getName();
            boolean include = true;
            for (String excludeClass : MixinEnvironment.excludeTransformers) {
                if (transformerName.contains(excludeClass)) {
                    include = false;
                    break;
                }
            }
            if (include && !transformerName.contains(MixinTransformer.class.getName())) {
                this.logger.debug("  Adding:    {}", transformerName);
                this.transformers.add(transformer);
            } else {
                this.logger.debug("  Excluding: {}", transformerName);
            }
        }

        this.logger.debug("Transformer delegation list created with {} entries", this.transformers.size());
        
        for (IClassTransformer transformer : Launch.classLoader.getTransformers()) {
            if (transformer instanceof IClassNameTransformer) {
                this.logger.debug("Found name transformer: {}", transformer.getClass().getName());
                this.nameTransformer = (IClassNameTransformer) transformer;
            }
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("%s[%s]", this.getClass().getSimpleName(), this.phase);
    }
    
    /**
     * Get the current phase, triggers initialisation if necessary
     */
    private static Phase getCurrentPhase() {
        if (MixinEnvironment.currentPhase == Phase.NOT_INITIALISED) {
            MixinEnvironment.init(Phase.PREINIT);
        }
        
        return MixinEnvironment.currentPhase;
    }
    
    /**
     * Initialise the mixin environment in the specified phase
     * 
     * @param phase initial phase
     */
    public static void init(Phase phase) {
        if (MixinEnvironment.currentPhase == Phase.NOT_INITIALISED) {
            MixinEnvironment.currentPhase = phase;
            MixinEnvironment.getEnvironment(phase);
        }
    }
    
    /**
     * Get the mixin environment for the specified phase
     * 
     * @param phase phase to fetch environment for
     * @return the environment
     */
    public static MixinEnvironment getEnvironment(Phase phase) {
        if (phase.ordinal < 0) {
            throw new IllegalArgumentException("Cannot access the UNINITIALISED environment");
        }
        
        if (MixinEnvironment.environments[phase.ordinal] == null) {
            MixinEnvironment.environments[phase.ordinal] = new MixinEnvironment(phase);
        }
        
        return MixinEnvironment.environments[phase.ordinal];
    }

    /**
     * Gets the default environment
     * 
     * @return the {@link Phase#DEFAULT DEFAULT} environment
     */
    public static MixinEnvironment getDefaultEnvironment() {
        return MixinEnvironment.getEnvironment(Phase.DEFAULT);
    }

    /**
     * Gets the current environment
     * 
     * @return the currently active environment
     */
    public static MixinEnvironment getCurrentEnvironment() {
        if (MixinEnvironment.currentEnvironment == null) {
            MixinEnvironment.currentEnvironment = MixinEnvironment.getEnvironment(MixinEnvironment.getCurrentPhase());
        }
        
        return MixinEnvironment.currentEnvironment;
    }

    /**
     * Internal callback
     * 
     * @param phase
     */
    static void gotoPhase(Phase phase) {
        if (phase == null || phase.ordinal < 0) {
            throw new IllegalArgumentException("Cannot go to the specified phase, phase is null or invalid");
        }
        
        if (phase.ordinal > getCurrentPhase().ordinal) {
            MixinBootstrap.addProxy();
        }
        
        MixinEnvironment.currentPhase = phase;
        MixinEnvironment.currentEnvironment = MixinEnvironment.getEnvironment(MixinEnvironment.getCurrentPhase());
    }
}
