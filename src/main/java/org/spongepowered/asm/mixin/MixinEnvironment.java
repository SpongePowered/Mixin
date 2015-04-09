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
package org.spongepowered.asm.mixin;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.core.helpers.Booleans;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.util.PrettyPrinter;

import com.google.common.collect.ImmutableList;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;


/**
 * Env interaction for mixins
 */
public class MixinEnvironment {
    
    public static class Phase {
        
        public static final Phase PREINIT = new Phase(0, "PREINIT");
        
        public static final Phase DEFAULT = new Phase(1, "DEFAULT");
        
        static final List<Phase> phases = ImmutableList.of(
            Phase.PREINIT,
            Phase.DEFAULT
        );
        
        final int ordinal;
        
        final String name;
        
        private Phase(int ordinal, String name) {
            this.ordinal = ordinal;
            this.name = name;
        }
        
        @Override
        public String toString() {
            return this.name;
        }
    }
    
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

        protected final String getSideName() {
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
        CHECK_IMPLEMENTS(Option.CHECK_ALL, "interfaces");
        
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
        
        public Option getParent() {
            return this.parent;
        }
        
        public String getProperty() {
            return this.property;
        }
        
        protected boolean getValue() {
            return Booleans.parseBoolean(System.getProperty(this.property), false)
                    || (this.parent != null && this.parent.getValue());
        }
    }
    
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

    private static final String CONFIGS_KEY = "mixin.configs";
    private static final String TRANSFORMER_KEY = "mixin.transformer";
    
    private static Phase currentPhase = Phase.PREINIT;
    
    private static MixinEnvironment[] environments = new MixinEnvironment[Phase.phases.size()];
    
    private static MixinEnvironment currentEnvironment;
    
    private final Phase phase;
    
    private final String configsKey;
    
    private final boolean[] options;
    
    private Side side;
    
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
            this.options[option.ordinal()] = option.getValue();
        }
        
        if (this.getOption(Option.DEBUG_VERBOSE) && this.phase == Phase.PREINIT) {
            PrettyPrinter printer = new PrettyPrinter(32);
            printer.add("SpongePowered MIXIN (Verbose debugging enabled)").centre().hr();
            printer.add("%25s : %s", "Code source", this.getClass().getProtectionDomain().getCodeSource().getLocation());
            printer.add("%25s : %s", "Internal Version", version).hr();
            for (Option option : Option.values()) {
                printer.add("%25s : %s%s", option.property, option.parent == null ? "" : " - ", this.getOption(option));
            }
            printer.hr().add("%25s : %s", "Detected Side", this.getSide());
            printer.print(System.err);
        }
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

    public Object getActiveTransformer() {
        return Launch.blackboard.get(MixinEnvironment.TRANSFORMER_KEY);
    }

    public void setActiveTransformer(IClassTransformer transformer) {
        Launch.blackboard.put(MixinEnvironment.TRANSFORMER_KEY, transformer);        
    }
    
    public MixinEnvironment setSide(Side side) {
        if (side != null && this.getSide() == Side.UNKNOWN && side != Side.UNKNOWN) {
            this.side = side;
        }
        return this;
    }
    
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
    
    public String getVersion() {
        return (String)Launch.blackboard.get(MixinBootstrap.INIT_KEY);
    }

    public boolean getOption(Option option) {
        return this.options[option.ordinal()];
    }
    
    public void setOption(Option option, boolean value) {
        this.options[option.ordinal()] = value;
    }
    
    @Override
    public String toString() {
        return String.format("%s[%s]", this.getClass().getSimpleName(), this.phase);
    }
    
    public static MixinEnvironment getEnvironment(Phase phase) {
        if (MixinEnvironment.environments[phase.ordinal] == null) {
            MixinEnvironment.environments[phase.ordinal] = new MixinEnvironment(phase);
        }
        
        return MixinEnvironment.environments[phase.ordinal];
    }

    public static MixinEnvironment getDefaultEnvironment() {
        return MixinEnvironment.getEnvironment(Phase.DEFAULT);
    }

    public static MixinEnvironment getCurrentEnvironment() {
        if (MixinEnvironment.currentEnvironment == null) {
            MixinEnvironment.currentEnvironment = MixinEnvironment.getEnvironment(MixinEnvironment.currentPhase);
        }
        
        return MixinEnvironment.currentEnvironment;
    }
    
    static void gotoPhase(Phase phase) {
        MixinEnvironment.currentPhase = phase;
        MixinEnvironment.currentEnvironment = MixinEnvironment.getEnvironment(MixinEnvironment.currentPhase);
    }
}
