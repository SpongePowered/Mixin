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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.spongepowered.asm.launch.GlobalProperties;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.extensibility.IEnvironmentTokenProvider;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.mixin.transformer.MixinTransformer;
import org.spongepowered.asm.obfuscation.RemapperChain;
import org.spongepowered.asm.service.ILegacyClassTransformer;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.ITransformer;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.ITokenProvider;
import org.spongepowered.asm.util.JavaVersion;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.asm.util.perf.Profiler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

/**
 * The mixin environment manages global state information for the mixin
 * subsystem.
 */
public final class MixinEnvironment implements ITokenProvider {
    
    /**
     * Environment phase, deliberately not implemented as an enum
     */
    public static final class Phase {
        
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
         * "Initialisation" phase, after FML's deobf transformer has loaded
         */
        public static final Phase INIT = new Phase(1, "INIT");
        
        /**
         * "Default" phase, during runtime
         */
        public static final Phase DEFAULT = new Phase(2, "DEFAULT");
        
        /**
         * All phases
         */
        static final List<Phase> phases = ImmutableList.of(
            Phase.PREINIT,
            Phase.INIT,
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
        
        /**
         * Environment for this phase 
         */
        private MixinEnvironment environment;
        
        private Phase(int ordinal, String name) {
            this.ordinal = ordinal;
            this.name = name;
        }
        
        @Override
        public String toString() {
            return this.name;
        }

        /**
         * Get a phase by name, returns <tt>null</tt> if no phases exist with
         * the specified name
         * 
         * @param name phase name to lookup
         * @return phase object or <tt>null</tt> if non existent
         */
        public static Phase forName(String name) {
            for (Phase phase : Phase.phases) {
                if (phase.name.equals(name)) {
                    return phase;
                }
            }
            return null;
        }

        MixinEnvironment getEnvironment() {
            if (this.ordinal < 0) {
                throw new IllegalArgumentException("Cannot access the NOT_INITIALISED environment");
            }
            
            if (this.environment == null) {
                this.environment = new MixinEnvironment(this);
            }

            return this.environment;
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
                String sideName = MixinService.getService().getSideName();
                return "CLIENT".equals(sideName);
            }
        },
        
        /**
         * (Dedicated) Server-side environment 
         */
        SERVER {
            @Override
            protected boolean detect() {
                String sideName = MixinService.getService().getSideName();
                return "SERVER".equals(sideName) || "DEDICATEDSERVER".equals(sideName);
            }
        };
        
        protected abstract boolean detect();
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
        DEBUG_EXPORT_FILTER(Option.DEBUG_EXPORT, "filter", false),
        
        /**
         * Allow fernflower to be disabled even if it is found on the classpath
         */
        DEBUG_EXPORT_DECOMPILE(Option.DEBUG_EXPORT, Inherit.ALLOW_OVERRIDE, "decompile"),
        
        /**
         * Run fernflower in a separate thread. In general this will allow
         * export to impact startup time much less (decompiling normally adds
         * about 20% to load times) with the trade-off that crashes may lead to
         * undecompiled exports.
         */
        DEBUG_EXPORT_DECOMPILE_THREADED(Option.DEBUG_EXPORT_DECOMPILE, Inherit.ALLOW_OVERRIDE, "async"),
        
        /**
         * By default, if the runtime export decompiler is active, mixin generic
         * signatures are merged into target classes. However this can cause
         * problems with some runtime subsystems which attempt to reify generics
         * using the signature data. Set this option to <tt>false</tt> to
         * disable generic signature merging. 
         */
        DEBUG_EXPORT_DECOMPILE_MERGESIGNATURES(Option.DEBUG_EXPORT_DECOMPILE, Inherit.ALLOW_OVERRIDE, "mergeGenericSignatures"),
        
        /**
         * Run the CheckClassAdapter on all classes after mixins are applied,
         * also enables stricter checks on mixins for use at dev-time, promotes
         * some warning-level messages to exceptions 
         */
        DEBUG_VERIFY(Option.DEBUG_ALL, "verify"),
        
        /**
         * Enable verbose mixin logging (elevates all DEBUG level messages to
         * INFO level) 
         */
        DEBUG_VERBOSE(Option.DEBUG_ALL, "verbose"),
        
        /**
         * Elevates failed injections to an error condition, see
         * {@link Inject#expect} for details
         */
        DEBUG_INJECTORS(Option.DEBUG_ALL, "countInjections"),
        
        /**
         * Enable strict checks
         */
        DEBUG_STRICT(Option.DEBUG_ALL, Inherit.INDEPENDENT, "strict"),
        
        /**
         * If false (default), {@link Unique} public methods merely raise a
         * warning when encountered and are not merged into the target. If true,
         * an exception is thrown instead
         */
        DEBUG_UNIQUE(Option.DEBUG_STRICT, "unique"),
        
        /**
         * Enable strict checking for mixin targets
         */
        DEBUG_TARGETS(Option.DEBUG_STRICT, "targets"),
        
        /**
         * Enable the performance profiler for all mixin operations (normally it
         * is only enabled during mixin prepare operations)
         */
        DEBUG_PROFILER(Option.DEBUG_ALL, Inherit.ALLOW_OVERRIDE, "profiler"),

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
         * If interface check is enabled, "strict mode" (default) applies the
         * implementation check even to abstract target classes. Setting this
         * option to <tt>false</tt> causes abstract targets to be skipped when
         * generating the implementation report.
         */
        CHECK_IMPLEMENTS_STRICT(Option.CHECK_IMPLEMENTS, Inherit.ALLOW_OVERRIDE, "strict"),
        
        /**
         * Ignore all constraints on mixin annotations, output warnings instead
         */
        IGNORE_CONSTRAINTS("ignoreConstraints"),

        /**
         * Enables the hot-swap agent
         */
        HOT_SWAP("hotSwap"),
        
        /**
         * Parent for environment settings
         */
        ENVIRONMENT(Inherit.ALWAYS_FALSE, "env"),
        
        /**
         * Force refmap obf type when required 
         */
        OBFUSCATION_TYPE(Option.ENVIRONMENT, Inherit.ALWAYS_FALSE, "obf"),
        
        /**
         * Disable refmap when required 
         */
        DISABLE_REFMAP(Option.ENVIRONMENT, Inherit.INDEPENDENT, "disableRefMap"),
        
        /**
         * Rather than disabling the refMap, you may wish to remap existing
         * refMaps at runtime. This can be achieved by setting this property and
         * supplying values for <tt>mixin.env.refMapRemappingFile</tt> and
         * <tt>mixin.env.refMapRemappingEnv</tt>. Though those properties can be
         * ignored if starting via <tt>GradleStart</tt> (this property is also
         * automatically enabled if loading via GradleStart). 
         */
        REFMAP_REMAP(Option.ENVIRONMENT, Inherit.INDEPENDENT, "remapRefMap"),
        
        /**
         * If <tt>mixin.env.remapRefMap</tt> is enabled, this setting can be
         * used to override the name of the SRG file to read mappings from. The
         * mappings must have a source type of <tt>searge</tt> and a target type
         * matching the current development environment. If the source type is
         * not <tt>searge</tt> then the <tt>mixin.env.refMapRemappingEnv</tt>
         * should be set to the correct source environment type.
         */
        REFMAP_REMAP_RESOURCE(Option.ENVIRONMENT, Inherit.INDEPENDENT, "refMapRemappingFile", ""),
        
        /**
         * When using <tt>mixin.env.refMapRemappingFile</tt>, this setting
         * overrides the default source environment (searge). However note that
         * the specified environment type must exist in the orignal refmap.
         */
        REFMAP_REMAP_SOURCE_ENV(Option.ENVIRONMENT, Inherit.INDEPENDENT, "refMapRemappingEnv", "searge"),
        
        /**
         * When <tt>mixin.env.remapRefMap</tt> is enabled and a refmap is
         * available for a mixin config, certain injection points are allowed to
         * fail over to a "permissive" match which ignores the member descriptor
         * in the refmap. To disable this behaviour, set this property to
         * <tt>false</tt>.
         */
        REFMAP_REMAP_ALLOW_PERMISSIVE(Option.ENVIRONMENT, Inherit.INDEPENDENT, "allowPermissiveMatch", true, "true"),
        
        /**
         * Globally ignore the "required" attribute of all configurations
         */
        IGNORE_REQUIRED(Option.ENVIRONMENT, Inherit.INDEPENDENT, "ignoreRequired"),

        /**
         * Default compatibility level to operate at
         */
        DEFAULT_COMPATIBILITY_LEVEL(Option.ENVIRONMENT, Inherit.INDEPENDENT, "compatLevel"),
        
        /**
         * Behaviour when the maximum defined {@link At#by} value is exceeded in
         * a mixin. Currently the behaviour is to <tt>warn</tt>. In later
         * versions of Mixin this may be promoted to <tt>error</tt>.
         * 
         * <p>Available values for this option are:</p>
         * 
         * <dl>
         *   <dt>ignore</dt>
         *   <dd>Pre-0.7 behaviour, no action is taken when a violation is
         *     encountered</dd>
         *   <dt>warn</dt>
         *   <dd>Current behaviour, a <tt>WARN</tt>-level message is raised for
         *     violations</dd>
         *   <dt>error</dt>
         *   <dd>Violations throw an exception</dd>
         * </dl>
         */
        SHIFT_BY_VIOLATION_BEHAVIOUR(Option.ENVIRONMENT, Inherit.INDEPENDENT, "shiftByViolation", "warn"),
        
        /**
         * Behaviour for initialiser injections, current supported options are
         * "default" and "safe"
         */
        INITIALISER_INJECTION_MODE("initialiserInjectionMode", "default");
        
        /**
         * Type of inheritance for options
         */
        private enum Inherit {
            
            /**
             * If the parent is set, this option will be set too.
             */
            INHERIT,
            
            /**
             * If the parent is set, this option will be set too. However
             * setting the option explicitly to <tt>false</tt> will override the
             * parent value. 
             */
            ALLOW_OVERRIDE,
            
            /**
             * This option ignores the value of the parent option, parent is
             * only used for grouping. 
             */
            INDEPENDENT,
            
            /**
             * This option is always <tt>false</tt>.
             */
            ALWAYS_FALSE
            
        }

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
         * Inheritance behaviour for this option 
         */
        final Inherit inheritance;

        /**
         * Java property name
         */
        final String property;
        
        /**
         * Default value for string properties
         */
        final String defaultValue;
        
        /**
         * Whether this property is boolean or not
         */
        final boolean isFlag;
        
        /**
         * Number of parents 
         */
        final int depth;

        private Option(String property) {
            this(null, property, true);
        }
        
        private Option(Inherit inheritance, String property) {
            this(null, inheritance, property, true);
        }
        
        private Option(String property, boolean flag) {
            this(null, property, flag);
        }

        private Option(String property, String defaultStringValue) {
            this(null, Inherit.INDEPENDENT, property, false, defaultStringValue);
        }
        
        private Option(Option parent, String property) {
            this(parent, Inherit.INHERIT, property, true);
        }
        
        private Option(Option parent, Inherit inheritance, String property) {
            this(parent, inheritance, property, true);
        }
        
        private Option(Option parent, String property, boolean isFlag) {
            this(parent, Inherit.INHERIT, property, isFlag, null);
        }
        
        private Option(Option parent, Inherit inheritance, String property, boolean isFlag) {
            this(parent, inheritance, property, isFlag, null);
        }
        
        private Option(Option parent, String property, String defaultStringValue) {
            this(parent, Inherit.INHERIT, property, false, defaultStringValue);
        }
        
        private Option(Option parent, Inherit inheritance, String property, String defaultStringValue) {
            this(parent, inheritance, property, false, defaultStringValue);
        }
        
        private Option(Option parent, Inherit inheritance, String property, boolean isFlag, String defaultStringValue) {
            this.parent = parent;
            this.inheritance = inheritance;
            this.property = (parent != null ? parent.property : Option.PREFIX) + "." + property;
            this.defaultValue = defaultStringValue;
            this.isFlag = isFlag;
            int depth = 0;
            for (; parent != null; depth++) {
                parent = parent.parent;
            }
            this.depth = depth;
        }
        
        Option getParent() {
            return this.parent;
        }
        
        String getProperty() {
            return this.property;
        }
        
        @Override
        public String toString() {
            return this.isFlag ? String.valueOf(this.getBooleanValue()) : this.getStringValue();
        }
        
        private boolean getLocalBooleanValue(boolean defaultValue) {
            return Boolean.parseBoolean(System.getProperty(this.property, Boolean.toString(defaultValue)));
        }
        
        private boolean getInheritedBooleanValue() {
            return this.parent != null && this.parent.getBooleanValue();
        }
        
        final boolean getBooleanValue() {
            if (this.inheritance == Inherit.ALWAYS_FALSE) {
                return false;
            }
            
            boolean local = this.getLocalBooleanValue(false);
            if (this.inheritance == Inherit.INDEPENDENT) {
                return local;
            }

            boolean inherited = local || this.getInheritedBooleanValue();
            return this.inheritance == Inherit.INHERIT ? inherited : this.getLocalBooleanValue(inherited);
        }

        final String getStringValue() {
            return (this.inheritance == Inherit.INDEPENDENT || this.parent == null || this.parent.getBooleanValue())
                    ? System.getProperty(this.property, this.defaultValue) : this.defaultValue;
        }

        @SuppressWarnings("unchecked")
        <E extends Enum<E>> E getEnumValue(E defaultValue) {
            String value = System.getProperty(this.property, defaultValue.name());
            try {
                return (E)Enum.valueOf(defaultValue.getClass(), value.toUpperCase());
            } catch (IllegalArgumentException ex) {
                return defaultValue;
            }
        }
    }
    
    /**
     * Operational compatibility level for the mixin subsystem
     */
    public static enum CompatibilityLevel {
        
        /**
         * Java 6 and above
         */
        JAVA_6(6, Opcodes.V1_6, false),
        
        /**
         * Java 7 and above
         */
        JAVA_7(7, Opcodes.V1_7, false) {

            @Override
            boolean isSupported() {
                return JavaVersion.current() >= 1.7;
            }
            
        },
        
        /**
         * Java 8 and above
         */
        JAVA_8(8, Opcodes.V1_8, true) {

            @Override
            boolean isSupported() {
                return JavaVersion.current() >= 1.8;
            }
            
        },
        
        /**
         * Java 9 and above
         */
        JAVA_9(9, CompatibilityLevel.CLASS_V1_9, true) {
            
            @Override
            boolean isSupported() {
                return false;
            }
            
        };
        
        // Temp, until ASM supports Java 9
        private static final int CLASS_V1_9 = 0 << 16 | 53;
        
        private final int ver;
        
        private final int classVersion;
        
        private final boolean supportsMethodsInInterfaces;
        
        private CompatibilityLevel maxCompatibleLevel;
        
        private CompatibilityLevel(int ver, int classVersion, boolean resolveMethodsInInterfaces) {
            this.ver = ver;
            this.classVersion = classVersion;
            this.supportsMethodsInInterfaces = resolveMethodsInInterfaces;
        }
        
        @SuppressWarnings("unused")
        private void setMaxCompatibleLevel(CompatibilityLevel maxCompatibleLevel) {
            this.maxCompatibleLevel = maxCompatibleLevel;
        }

        /**
         * Get whether this compatibility level is supported in the current
         * environment
         */
        boolean isSupported() {
            return true;
        }
        
        /**
         * Class version expected at this compatibility level
         */
        public int classVersion() {
            return this.classVersion;
        }
        
        /**
         * Get whether this environment supports non-abstract methods in
         * interfaces, true in Java 1.8 and above
         */
        public boolean supportsMethodsInInterfaces() {
            return this.supportsMethodsInInterfaces;
        }
        
        /**
         * Get whether this level is the same or greater than the specified
         * level
         * 
         * @param level level to compare to
         * @return true if this level is equal or higher the supplied level
         */
        public boolean isAtLeast(CompatibilityLevel level) {
            return level == null || this.ver >= level.ver; 
        }
        
        /**
         * Get whether this level can be elevated to the specified level
         * 
         * @param level desired level
         * @return true if this level supports elevation
         */
        public boolean canElevateTo(CompatibilityLevel level) {
            if (level == null || this.maxCompatibleLevel == null) {
                return true;
            }
            return level.ver <= this.maxCompatibleLevel.ver;
        }
        
        /**
         * True if this level can support the specified level
         * 
         * @param level desired level
         * @return true if the other level can be elevated to this level
         */
        public boolean canSupport(CompatibilityLevel level) {
            if (level == null) {
                return true;
            }
            
            return level.canElevateTo(this);
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
     * Temporary
     */
    static class MixinLogWatcher {

        static MixinAppender appender = new MixinAppender();
        static org.apache.logging.log4j.core.Logger log;
        static Level oldLevel = null;

        static void begin() {
            /*
             * In order to determine when to switch to the INIT phase, Mixin
             * relies on being able to detect a specific message
             * ("Validating minecraft") logged to FMLRelaunchLog.
             * However, this message is logged at the DEBUG level, which may
             * not be enabled depending on the launcher or game version.
             *
             * To ensure that Mixin is always able to detect this message,
             * we temporarily set the log level of FMLRelaunchLog to 'ALL'.
             * To minimize the overall impact, the log level is restored
             * (unless it was changed in the meantime) once MixinAppender
             * detects the message.
             */
            Logger fmlLog = LogManager.getLogger("FML");
            if (!(fmlLog instanceof org.apache.logging.log4j.core.Logger)) {
                return;
            }
            
            MixinLogWatcher.log = (org.apache.logging.log4j.core.Logger)fmlLog;
            MixinLogWatcher.oldLevel = MixinLogWatcher.log.getLevel();
            
            MixinLogWatcher.appender.start();
            MixinLogWatcher.log.addAppender(MixinLogWatcher.appender);
            
            MixinLogWatcher.log.setLevel(Level.ALL);
        }
        
        static void end() {
            if (MixinLogWatcher.log != null) {
                // remove appender, we're done watching for messages
                MixinLogWatcher.log.removeAppender(MixinLogWatcher.appender);
            }
        }

        /**
         * Temporary
         */
        static class MixinAppender extends AbstractAppender {

            MixinAppender() {
                super("MixinLogWatcherAppender", null, null);
            }

            @Override
            public void append(LogEvent event) {
                if (event.getLevel() != Level.DEBUG || !"Validating minecraft".equals(event.getMessage().getFormattedMessage())) {
                    return;
                }
                
                // transition to INIT
                MixinEnvironment.gotoPhase(Phase.INIT);

                // Only reset the log level if it's still ALL. If something
                // else changed the log level after we did, we don't want
                // overwrite that change. No null check is needed here
                // because the appender will not be injected if the log is
                // null
                if (MixinLogWatcher.log.getLevel() == Level.ALL) {
                    MixinLogWatcher.log.setLevel(MixinLogWatcher.oldLevel);
                }
            }
            
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
     * Current compatibility level
     */
    private static CompatibilityLevel compatibility = Option.DEFAULT_COMPATIBILITY_LEVEL.<CompatibilityLevel>getEnumValue(CompatibilityLevel.JAVA_6);
    
    /**
     * Show debug header info on first environment construction
     */
    private static boolean showHeader = true;
    
    /**
     * Logger 
     */
    private static final Logger logger = LogManager.getLogger("mixin");
    
    /**
     * Performance profiler 
     */
    private static final Profiler profiler = new Profiler();
    
    /**
     * Service 
     */
    private final IMixinService service;

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
     * Remappers for this environment 
     */
    private final RemapperChain remappers = new RemapperChain();

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
    private List<ILegacyClassTransformer> transformers;
    
    /**
     * Obfuscation context (refmap key to use in this environment) 
     */
    private String obfuscationContext = null;
    
    MixinEnvironment(Phase phase) {
        this.service = MixinService.getService();
        this.phase = phase;
        this.configsKey = GlobalProperties.Keys.CONFIGS + "." + this.phase.name.toLowerCase();
        
        // Sanity check
        Object version = this.getVersion();
        if (version == null || !MixinBootstrap.VERSION.equals(version)) {
            throw new MixinException("Environment conflict, mismatched versions or you didn't call MixinBootstrap.init()");
        }
        
        // More sanity check
        this.service.checkEnv(this);
        
        this.options = new boolean[Option.values().length];
        for (Option option : Option.values()) {
            this.options[option.ordinal()] = option.getBooleanValue();
        }
        
        if (MixinEnvironment.showHeader) {
            MixinEnvironment.showHeader = false;
            this.printHeader(version);
        }
    }

    private void printHeader(Object version) {
        String codeSource = this.getCodeSource();
        String serviceName = this.service.getName();
        Side side = this.getSide();
        MixinEnvironment.logger.info("SpongePowered MIXIN Subsystem Version={} Source={} Service={} Env={}", version, codeSource, serviceName, side);
        
        boolean verbose = this.getOption(Option.DEBUG_VERBOSE);
        if (verbose || this.getOption(Option.DEBUG_EXPORT) || this.getOption(Option.DEBUG_PROFILER)) {
            PrettyPrinter printer = new PrettyPrinter(32);
            printer.add("SpongePowered MIXIN%s", verbose ? " (Verbose debugging enabled)" : "").centre().hr();
            printer.kv("Code source", codeSource);
            printer.kv("Internal Version", version);
            printer.kv("Java 8 Supported", CompatibilityLevel.JAVA_8.isSupported()).hr();
            printer.kv("Service Name", serviceName);
            printer.kv("Service Class", this.service.getClass().getName()).hr();
            for (Option option : Option.values()) {
                StringBuilder indent = new StringBuilder();
                for (int i = 0; i < option.depth; i++) {
                    indent.append("- ");
                }
                printer.kv(option.property, "%s<%s>", indent, option);
            }
            printer.hr().kv("Detected Side", side);
            printer.print(System.err);
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
     * @deprecated no replacement
     */
    @Deprecated
    public List<String> getMixinConfigs() {
        List<String> mixinConfigs = GlobalProperties.<List<String>>get(this.configsKey);
        if (mixinConfigs == null) {
            mixinConfigs = new ArrayList<String>();
            GlobalProperties.put(this.configsKey, mixinConfigs);
        }
        return mixinConfigs;
    }
    
    /**
     * Add a mixin configuration to the blackboard
     * 
     * @param config Name of configuration resource to add
     * @return fluent interface
     * @deprecated use Mixins::addConfiguration instead
     */
    @Deprecated
    public MixinEnvironment addConfiguration(String config) {
        MixinEnvironment.logger.warn("MixinEnvironment::addConfiguration is deprecated and will be removed. Use Mixins::addConfiguration instead!");
        Mixins.addConfiguration(config, this);
        return this;
    }

    void registerConfig(String config) {
        List<String> configs = this.getMixinConfigs();
        if (!configs.contains(config)) {
            configs.add(config);
        }
    }

    /**
     * Add a new error handler class to this environment
     * 
     * @param handlerName Handler class to add
     * @return fluent interface
     * @deprecated use Mixins::registerErrorHandlerClass
     */
    @Deprecated
    public MixinEnvironment registerErrorHandlerClass(String handlerName) {
        Mixins.registerErrorHandlerClass(handlerName);
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
                        (Class<? extends IEnvironmentTokenProvider>)this.service.getClassProvider().findClass(providerName, true);
                IEnvironmentTokenProvider provider = providerClass.newInstance();
                this.registerTokenProvider(provider);
            } catch (Throwable th) {
                MixinEnvironment.logger.error("Error instantiating " + providerName, th);
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
            MixinEnvironment.logger.info("Adding new token provider {} to {}", providerName, this);
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
     * @deprecated use Mixins::getErrorHandlerClasses
     */
    @Deprecated
    public Set<String> getErrorHandlerClasses() {
        return Mixins.getErrorHandlerClasses();
    }

    /**
     * Get the active mixin transformer instance (if any)
     * 
     * @return active mixin transformer instance
     */
    public Object getActiveTransformer() {
        return GlobalProperties.get(GlobalProperties.Keys.TRANSFORMER);
    }

    /**
     * Set the mixin transformer instance
     * 
     * @param transformer Mixin Transformer
     */
    public void setActiveTransformer(ITransformer transformer) {
        if (transformer != null) {
            GlobalProperties.put(GlobalProperties.Keys.TRANSFORMER, transformer);        
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
        return GlobalProperties.<String>get(GlobalProperties.Keys.INIT);
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
     * Get the specified option from the current environment
     * 
     * @param option Option to get
     * @param defaultValue value to use if the user-defined value is invalid
     * @param <E> enum type
     * @return Option value
     */
    public <E extends Enum<E>> E getOption(Option option, E defaultValue) {
        return option.getEnumValue(defaultValue);
    }
    
    /**
     * Set the obfuscation context
     * 
     * @param context new context
     */
    public void setObfuscationContext(String context) {
        this.obfuscationContext = context;
    }
    
    /**
     * Get the current obfuscation context
     */
    public String getObfuscationContext() {
        return this.obfuscationContext;
    }
    
    /**
     * Get the current obfuscation context
     */
    public String getRefmapObfuscationContext() {
        String overrideObfuscationType = Option.OBFUSCATION_TYPE.getStringValue();
        if (overrideObfuscationType != null) {
            return overrideObfuscationType;
        }
        return this.obfuscationContext;
    }
    
    /**
     * Get the remapper chain for this environment
     */
    public RemapperChain getRemappers() {
        return this.remappers;
    }
    
    /**
     * Invoke a mixin environment audit process
     */
    public void audit() {
        Object activeTransformer = this.getActiveTransformer();
        if (activeTransformer instanceof MixinTransformer) {
            MixinTransformer transformer = (MixinTransformer)activeTransformer;
            transformer.audit(this);
        }
    }

    /**
     * Returns (and generates if necessary) the transformer delegation list for
     * this environment.
     * 
     * @return current transformer delegation list (read-only)
     */
    public List<ILegacyClassTransformer> getTransformers() {
        if (this.transformers == null) {
            this.buildTransformerDelegationList();
        }
        
        return Collections.<ILegacyClassTransformer>unmodifiableList(this.transformers);
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
     * Builds the transformer list to apply to loaded mixin bytecode. Since
     * generating this list requires inspecting each transformer by name (to
     * cope with the new wrapper functionality added by FML) we generate the
     * list just once per environment and cache the result.
     */
    private void buildTransformerDelegationList() {
        MixinEnvironment.logger.debug("Rebuilding transformer delegation list:");
        this.transformers = new ArrayList<ILegacyClassTransformer>();
        for (ITransformer transformer : this.service.getTransformers()) {
            if (!(transformer instanceof ILegacyClassTransformer)) {
                continue;
            }
            
            ILegacyClassTransformer legacyTransformer = (ILegacyClassTransformer)transformer;
            String transformerName = legacyTransformer.getName();
            boolean include = true;
            for (String excludeClass : MixinEnvironment.excludeTransformers) {
                if (transformerName.contains(excludeClass)) {
                    include = false;
                    break;
                }
            }
            if (include && !legacyTransformer.isDelegationExcluded()) {
                MixinEnvironment.logger.debug("  Adding:    {}", transformerName);
                this.transformers.add(legacyTransformer);
            } else {
                MixinEnvironment.logger.debug("  Excluding: {}", transformerName);
            }
        }

        MixinEnvironment.logger.debug("Transformer delegation list created with {} entries", this.transformers.size());
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
            MixinEnvironment env = MixinEnvironment.getEnvironment(phase);
            MixinEnvironment.getProfiler().setActive(env.getOption(Option.DEBUG_PROFILER));
            
            MixinLogWatcher.begin();
        }
    }
    
    /**
     * Get the mixin environment for the specified phase
     * 
     * @param phase phase to fetch environment for
     * @return the environment
     */
    public static MixinEnvironment getEnvironment(Phase phase) {
        if (phase == null) {
            return Phase.DEFAULT.getEnvironment();
        }
        return phase.getEnvironment();
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
     * Get the current compatibility level
     */
    public static CompatibilityLevel getCompatibilityLevel() {
        return MixinEnvironment.compatibility;
    }
    
    /**
     * Set desired compatibility level for the entire environment
     * 
     * @param level Level to set, ignored if less than the current level
     * @throws IllegalArgumentException if the specified level is not supported
     * @deprecated set compatibility level in configuration
     */
    @Deprecated
    public static void setCompatibilityLevel(CompatibilityLevel level) throws IllegalArgumentException {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (!"org.spongepowered.asm.mixin.transformer.MixinConfig".equals(stackTrace[2].getClassName())) {
            MixinEnvironment.logger.warn("MixinEnvironment::setCompatibilityLevel is deprecated and will be removed. Set level via config instead!");
        }
        
        if (level != MixinEnvironment.compatibility && level.isAtLeast(MixinEnvironment.compatibility)) {
            if (!level.isSupported()) {
                throw new IllegalArgumentException("The requested compatibility level " + level + " could not be set. Level is not supported");
            }
            
            MixinEnvironment.compatibility = level;
            MixinEnvironment.logger.info("Compatibility level set to {}", level);
        }
    }
    
    /**
     * Get the performance profiler
     * 
     * @return profiler
     */
    public static Profiler getProfiler() {
        return MixinEnvironment.profiler;
    }
    
    /**
     * Internal callback
     * 
     * @param phase phase to go to 
     */
    static void gotoPhase(Phase phase) {
        if (phase == null || phase.ordinal < 0) {
            throw new IllegalArgumentException("Cannot go to the specified phase, phase is null or invalid");
        }
        
        if (phase.ordinal > MixinEnvironment.getCurrentPhase().ordinal) {
            MixinService.getService().beginPhase();
        }
        
        if (phase == Phase.DEFAULT) {
            MixinLogWatcher.end();
        }
        
        MixinEnvironment.currentPhase = phase;
        MixinEnvironment.currentEnvironment = MixinEnvironment.getEnvironment(MixinEnvironment.getCurrentPhase());
    }
}
