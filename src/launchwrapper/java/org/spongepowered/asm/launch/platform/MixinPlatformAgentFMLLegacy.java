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

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.spongepowered.asm.launch.GlobalProperties;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.extensibility.IRemapper;
import org.spongepowered.asm.service.mojang.MixinServiceLaunchWrapper;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.IConsumer;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * Platform agent for use under FML and LaunchWrapper.
 * 
 * <p>When FML is present we scan containers for the manifest entries which are
 * inhibited by the tweaker, in particular the <tt>FMLCorePlugin</tt> and
 * <tt>FMLCorePluginContainsFMLMod</tt> entries. This is required because FML
 * performs no further processing of containers if they contain a tweaker!</p>
 */
public class MixinPlatformAgentFMLLegacy extends MixinPlatformAgentAbstract implements IMixinPlatformServiceAgent {
    
    private static final String OLD_LAUNCH_HANDLER_CLASS = "cpw.mods.fml.relauncher.FMLLaunchHandler";
    private static final String NEW_LAUNCH_HANDLER_CLASS = "net.minecraftforge.fml.relauncher.FMLLaunchHandler";
    private static final String CLIENT_TWEAKER_TAIL = ".common.launcher.FMLTweaker";
    private static final String SERVER_TWEAKER_TAIL = ".common.launcher.FMLServerTweaker";
    private static final String GETSIDE_METHOD = "side";
    
    private static final String LOAD_CORE_MOD_METHOD = "loadCoreMod";
    private static final String GET_REPARSEABLE_COREMODS_METHOD = "getReparseableCoremods";
    private static final String CORE_MOD_MANAGER_CLASS = "net.minecraftforge.fml.relauncher.CoreModManager";
    private static final String CORE_MOD_MANAGER_CLASS_LEGACY = "cpw.mods.fml.relauncher.CoreModManager";
    private static final String GET_IGNORED_MODS_METHOD = "getIgnoredMods";
    private static final String GET_IGNORED_MODS_METHOD_LEGACY = "getLoadedCoremods";
    
    private static final String FML_REMAPPER_ADAPTER_CLASS = "org.spongepowered.asm.bridge.RemapperAdapterFML";
    private static final String FML_CMDLINE_COREMODS = "fml.coreMods.load";
    private static final String FML_PLUGIN_WRAPPER_CLASS = "FMLPluginWrapper";
    private static final String FML_CORE_MOD_INSTANCE_FIELD = "coreModInstance";

    private static final String MFATT_FORCELOADASMOD = "ForceLoadAsMod";
    private static final String MFATT_FMLCOREPLUGIN = "FMLCorePlugin";
    private static final String MFATT_COREMODCONTAINSMOD = "FMLCorePluginContainsFMLMod";
    
    private static final String FML_TWEAKER_DEOBF = "FMLDeobfTweaker";
    private static final String FML_TWEAKER_INJECTION = "FMLInjectionAndSortingTweaker";
    private static final String FML_TWEAKER_TERMINAL = "TerminalTweaker";

    /**
     * Coremod classes which have already been bootstrapped, so that we know not
     * to inject them
     */
    private static final Set<String> loadedCoreMods = new HashSet<String>();
    
    /**
     * Discover mods specified to FML on the command line (via JVM arg) at
     * startup so that we know to ignore them
     */
    static {
        for (String cmdLineCoreMod : System.getProperty(MixinPlatformAgentFMLLegacy.FML_CMDLINE_COREMODS, "").split(",")) {
            if (!cmdLineCoreMod.isEmpty()) {
                MixinPlatformAgentAbstract.logger.debug("FML platform agent will ignore coremod {} specified on the command line", cmdLineCoreMod);
                MixinPlatformAgentFMLLegacy.loadedCoreMods.add(cmdLineCoreMod);
            }
        }
    }

    /**
     * Container file
     */
    private File file;

    /**
     * Name of this container
     */
    private String fileName;

    /**
     * If running under FML, we will attempt to inject any coremod specified in
     * the metadata, FML's CoremodManager returns an ITweaker instance which is
     * the "handle" to the injected mod, we will need to proxy calls through to
     * the wrapper. If initialisation fails (for example if we are not running
     * under FML or if an FMLCorePlugin key is not specified in the metadata)
     * then this handle will be null.
     */
    private ITweaker coreModWrapper;
    
    /**
     * Core mod manager class
     */
    private Class<?> clCoreModManager;
    
    /**
     * True if this agent is initialised during pre-injection 
     */
    private boolean initInjectionState;

    @Override
    public boolean accept(MixinPlatformManager manager, IContainerHandle handle) {
        if (!(handle instanceof ContainerHandleURI) || !super.accept(manager, handle)) {
            return false;
        }
        
        this.file = ((ContainerHandleURI)handle).getFile();
        this.fileName = this.file.getName();
        this.coreModWrapper = this.initFMLCoreMod();

        return true;
    }

    /**
     * Attempts to initialise the FML coremod (if specified in the jar metadata)
     */
    private ITweaker initFMLCoreMod() {
        try {
            try {
                this.clCoreModManager = MixinPlatformAgentFMLLegacy.getCoreModManagerClass();
            } catch (ClassNotFoundException ex) {
                MixinPlatformAgentAbstract.logger.info("FML platform manager could not load class {}. Proceeding without FML support.",
                        ex.getMessage());
                return null;
            }

            if ("true".equalsIgnoreCase(this.handle.getAttribute(MixinPlatformAgentFMLLegacy.MFATT_FORCELOADASMOD))) {
                MixinPlatformAgentAbstract.logger.debug("ForceLoadAsMod was specified for {}, attempting force-load", this.fileName);
                this.loadAsMod();
            }

            return this.injectCorePlugin();
        } catch (Exception ex) {
            MixinPlatformAgentAbstract.logger.catching(ex);
            return null;
        }
    }

    /**
     * If the "force load as mod" manifest setting is set to true, this agent
     * will attempt to forcibly inject the container as a coremod by first
     * removing the container from fml's "loaded coremods" list causing
     * fml to subsequently re-evaluate the container for coremod candidacy.
     * 
     * <p>Further, if the "fml core plugin contains fml mod" flag is set in the
     * manifest, the container will be manually injected by this agent into the
     * "reparseable coremods" collection which will cause it to be crawled for
     * regular mods as well.</p> 
     */
    private void loadAsMod() {
        try {
            MixinPlatformAgentFMLLegacy.getIgnoredMods(this.clCoreModManager).remove(this.fileName);
        } catch (Exception ex) {
            MixinPlatformAgentAbstract.logger.catching(ex);
        }
        
        if (this.handle.getAttribute(MixinPlatformAgentFMLLegacy.MFATT_COREMODCONTAINSMOD) != null) {
            if (this.isIgnoredReparseable()) {
                MixinPlatformAgentAbstract.logger.debug(
                        "Ignoring request to add {} to reparseable coremod collection - it is a deobfuscated dependency", this.fileName);
                return;
            }
            this.addReparseableJar();
        }
    }

    private boolean isIgnoredReparseable() {
        return this.handle.toString().contains("deobfedDeps");
    }

    /**
     * Called by {@link #loadAsMod} if the "fml core plugin contains fml mod" is
     * set, adds this container to the "reparsable coremods" collection.
     */
    private void addReparseableJar() {
        try {
            Method mdGetReparsedCoremods = this.clCoreModManager.getDeclaredMethod(GlobalProperties.getString(
                    GlobalProperties.Keys.FML_GET_REPARSEABLE_COREMODS, MixinPlatformAgentFMLLegacy.GET_REPARSEABLE_COREMODS_METHOD));
            @SuppressWarnings("unchecked")
            List<String> reparsedCoremods = (List<String>)mdGetReparsedCoremods.invoke(null);
            if (!reparsedCoremods.contains(this.fileName)) {
                MixinPlatformAgentAbstract.logger.debug("Adding {} to reparseable coremod collection", this.fileName);
                reparsedCoremods.add(this.fileName);
            }
        } catch (Exception ex) {
            MixinPlatformAgentAbstract.logger.catching(ex);
        }
    }

    private ITweaker injectCorePlugin() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String coreModName = this.handle.getAttribute(MixinPlatformAgentFMLLegacy.MFATT_FMLCOREPLUGIN);
        if (coreModName == null) {
            return null;
        }
        
        if (this.isAlreadyInjected(coreModName)) {
            MixinPlatformAgentAbstract.logger.debug("{} has core plugin {}. Skipping because it was already injected.", this.fileName, coreModName);
            return null;
        }
        
        MixinPlatformAgentAbstract.logger.debug("{} has core plugin {}. Injecting it into FML for co-initialisation:", this.fileName, coreModName);
        Method mdLoadCoreMod = this.clCoreModManager.getDeclaredMethod(GlobalProperties.getString(GlobalProperties.Keys.FML_LOAD_CORE_MOD,
                MixinPlatformAgentFMLLegacy.LOAD_CORE_MOD_METHOD), LaunchClassLoader.class, String.class, File.class);
        mdLoadCoreMod.setAccessible(true);
        ITweaker wrapper = (ITweaker)mdLoadCoreMod.invoke(null, Launch.classLoader, coreModName, this.file);
        if (wrapper == null) {
            MixinPlatformAgentAbstract.logger.debug("Core plugin {} could not be loaded.", coreModName);
            return null;
        }
        
        // If the injection tweaker is queued, we are most likely in development
        // and will NOT need to co-init the coremod
        this.initInjectionState = MixinPlatformAgentFMLLegacy.isTweakerQueued(MixinPlatformAgentFMLLegacy.FML_TWEAKER_INJECTION);

        MixinPlatformAgentFMLLegacy.loadedCoreMods.add(coreModName);
        return wrapper;
    }
    
    private boolean isAlreadyInjected(String coreModName) {
        // Did we already inject this ourselves, or was it specified on the command line
        if (MixinPlatformAgentFMLLegacy.loadedCoreMods.contains(coreModName)) {
            return true;
        }
        
        // Was it already loaded, check the tweakers list
        try {
            List<ITweaker> tweakers = GlobalProperties.<List<ITweaker>>get(MixinServiceLaunchWrapper.BLACKBOARD_KEY_TWEAKS);
            if (tweakers == null) {
                return false;
            }
            
            for (ITweaker tweaker : tweakers) {
                Class<? extends ITweaker> tweakClass = tweaker.getClass();
                if (MixinPlatformAgentFMLLegacy.FML_PLUGIN_WRAPPER_CLASS.equals(tweakClass.getSimpleName())) {
                    Field fdCoreModInstance = tweakClass.getField(MixinPlatformAgentFMLLegacy.FML_CORE_MOD_INSTANCE_FIELD);
                    fdCoreModInstance.setAccessible(true);
                    Object coreMod = fdCoreModInstance.get(tweaker);
                    if (coreModName.equals(coreMod.getClass().getName())) {
                        return true;
                    }
                }
            }
        } catch (Exception ex) {
//            ex.printStackTrace();
        }

        return false;
    }

    @Override
    public String getPhaseProvider() {
        return MixinPlatformAgentFMLLegacy.class.getName() + "$PhaseProvider";
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.IMixinPlatformAgent#prepare()
     */
    @Override
    public void prepare() {
        this.initInjectionState |= MixinPlatformAgentFMLLegacy.isTweakerQueued(MixinPlatformAgentFMLLegacy.FML_TWEAKER_INJECTION);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.IMixinPlatformAgent
     *      #initPrimaryContainer()
     */
    @Override
    public void initPrimaryContainer() {
        if (this.clCoreModManager != null) {
//            MixinEnvironment.registerPhaseProvider(MixinPlatformAgentFMLLegacy.class.getName() + "$PhaseProvider");
            this.injectRemapper();
        }
    }

    private void injectRemapper() {
        try {
            MixinPlatformAgentAbstract.logger.debug("Creating FML remapper adapter: {}", MixinPlatformAgentFMLLegacy.FML_REMAPPER_ADAPTER_CLASS);
            Class<?> clFmlRemapperAdapter = Class.forName(MixinPlatformAgentFMLLegacy.FML_REMAPPER_ADAPTER_CLASS, true, Launch.classLoader);
            Method mdCreate = clFmlRemapperAdapter.getDeclaredMethod("create");
            IRemapper remapper = (IRemapper)mdCreate.invoke(null);
            MixinEnvironment.getDefaultEnvironment().getRemappers().add(remapper);
        } catch (Exception ex) {
            MixinPlatformAgentAbstract.logger.debug("Failed instancing FML remapper adapter, things will probably go horribly for notch-obf'd mods!");
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.platform.IMixinPlatformAgent#inject()
     */
    @Override
    public void inject() {
        if (this.coreModWrapper != null && this.checkForCoInitialisation()) {
            MixinPlatformAgentAbstract.logger.debug("FML agent is co-initiralising coremod instance {} for {}", this.coreModWrapper, this.handle);
            this.coreModWrapper.injectIntoClassLoader(Launch.classLoader);
        }
    }

    /**
     * Performs a naive check which attempts to discover whether we are pre or
     * post FML's main injection. If we are <i>pre</i>, then we must <b>not</b>
     * manually call <tt>injectIntoClassLoader</tt> on the wrapper because FML
     * will add the wrapper to the tweaker list itself. This occurs when mixin
     * tweaker is loaded explicitly.
     * 
     * <p>In the event that we are <i>post</i> FML's injection, then we must
     * instead call <tt>injectIntoClassLoader</tt> on the wrapper manually.</p>
     * 
     * @return true if FML was already injected
     */
    protected final boolean checkForCoInitialisation() {
        boolean injectionTweaker = MixinPlatformAgentFMLLegacy.isTweakerQueued(MixinPlatformAgentFMLLegacy.FML_TWEAKER_INJECTION);
        boolean terminalTweaker = MixinPlatformAgentFMLLegacy.isTweakerQueued(MixinPlatformAgentFMLLegacy.FML_TWEAKER_TERMINAL);
        if ((this.initInjectionState && terminalTweaker) || injectionTweaker) {
            MixinPlatformAgentAbstract.logger.debug("FML agent is skipping co-init for {} because FML will inject it normally", this.coreModWrapper);
            return false;
        }
        
        return !MixinPlatformAgentFMLLegacy.isTweakerQueued(MixinPlatformAgentFMLLegacy.FML_TWEAKER_DEOBF);
    }

    /**
     * Check whether a tweaker ending with <tt>tweakName</tt> has been enqueued
     * but not yet visited.
     * 
     * @param tweakerName Tweaker name to 
     * @return true if a tweaker with the specified name is queued
     */
    private static boolean isTweakerQueued(String tweakerName) {
        for (String tweaker : GlobalProperties.<List<String>>get(MixinServiceLaunchWrapper.BLACKBOARD_KEY_TWEAKCLASSES)) {
            if (tweaker.endsWith(tweakerName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Attempt to get the FML CoreModManager, tries the post-1.8 namespace first
     * and falls back to 1.7.10 if class lookup fails
     */
    private static Class<?> getCoreModManagerClass() throws ClassNotFoundException {
        try {
            return Class.forName(GlobalProperties.getString(
                    GlobalProperties.Keys.FML_CORE_MOD_MANAGER, MixinPlatformAgentFMLLegacy.CORE_MOD_MANAGER_CLASS));
        } catch (ClassNotFoundException ex) {
            return Class.forName(MixinPlatformAgentFMLLegacy.CORE_MOD_MANAGER_CLASS_LEGACY);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> getIgnoredMods(Class<?> clCoreModManager) throws IllegalAccessException, InvocationTargetException {
        Method mdGetIgnoredMods = null;
        
        try {
            mdGetIgnoredMods = clCoreModManager.getDeclaredMethod(GlobalProperties.getString(
                    GlobalProperties.Keys.FML_GET_IGNORED_MODS, MixinPlatformAgentFMLLegacy.GET_IGNORED_MODS_METHOD));
        } catch (NoSuchMethodException ex1) {
            try {
                // Legacy name
                mdGetIgnoredMods = clCoreModManager.getDeclaredMethod(MixinPlatformAgentFMLLegacy.GET_IGNORED_MODS_METHOD_LEGACY);
            } catch (NoSuchMethodException ex2) {
                MixinPlatformAgentAbstract.logger.catching(Level.DEBUG, ex2);
                return Collections.<String>emptyList();
            }
        }
        
        return (List<String>)mdGetIgnoredMods.invoke(null);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.platform.IMixinPlatformServiceAgent
     *      #init()
     */
    @Override
    public void init() {
        // Nothing to do here
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.platform.MixinPlatformAgentAbstract
     *      #getSideName()
     */
    @Override
    public String getSideName() {
        // Using this method first prevents us from accidentally loading FML
        // classes too early when using the tweaker in dev
        for (ITweaker tweaker : GlobalProperties.<List<ITweaker>>get(MixinServiceLaunchWrapper.BLACKBOARD_KEY_TWEAKS)) {
            if (tweaker.getClass().getName().endsWith(MixinPlatformAgentFMLLegacy.SERVER_TWEAKER_TAIL)) {
                return Constants.SIDE_SERVER;
            } else if (tweaker.getClass().getName().endsWith(MixinPlatformAgentFMLLegacy.CLIENT_TWEAKER_TAIL)) {
                return Constants.SIDE_CLIENT;
            }
        }

        String name = MixinPlatformAgentAbstract.invokeStringMethod(Launch.classLoader, MixinPlatformAgentFMLLegacy.NEW_LAUNCH_HANDLER_CLASS,
                MixinPlatformAgentFMLLegacy.GETSIDE_METHOD);
        if (name != null) {
            return name;
        }
        
        return MixinPlatformAgentAbstract.invokeStringMethod(Launch.classLoader, MixinPlatformAgentFMLLegacy.OLD_LAUNCH_HANDLER_CLASS,
                MixinPlatformAgentFMLLegacy.GETSIDE_METHOD);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.platform.IMixinPlatformServiceAgent
     *      #getMixinContainers()
     */
    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        return null;
    }

    // AMS - Temp. Legacy log watcher stuff moved here:
    
    @Override
    @Deprecated
    public void wire(Phase phase, IConsumer<Phase> phaseConsumer) {
        super.wire(phase, phaseConsumer);
        if (phase == Phase.PREINIT) {
            MixinPlatformAgentFMLLegacy.begin(phaseConsumer);
        }
    }
    
    
    /**
     * Called when the DEFAULT phase is started
     * @deprecated temporary 
     */
    @Override
    @Deprecated
    public void unwire() {
        MixinPlatformAgentFMLLegacy.end();
    }

    static MixinAppender appender;
    static org.apache.logging.log4j.core.Logger log;
    static Level oldLevel = null;
    
    static void begin(IConsumer<Phase> delegate) {
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
        
        MixinPlatformAgentFMLLegacy.log = (org.apache.logging.log4j.core.Logger)fmlLog;
        MixinPlatformAgentFMLLegacy.oldLevel = MixinPlatformAgentFMLLegacy.log.getLevel();
        
        MixinPlatformAgentFMLLegacy.appender = new MixinAppender(delegate);
        MixinPlatformAgentFMLLegacy.appender.start();
        MixinPlatformAgentFMLLegacy.log.addAppender(MixinPlatformAgentFMLLegacy.appender);
        
        MixinPlatformAgentFMLLegacy.log.setLevel(Level.ALL);
    }
    
    static void end() {
        if (MixinPlatformAgentFMLLegacy.log != null) {
            // remove appender, we're done watching for messages
            MixinPlatformAgentFMLLegacy.log.removeAppender(MixinPlatformAgentFMLLegacy.appender);
        }
    }

    /**
     * Temporary
     */
    static class MixinAppender extends AbstractAppender {

        private final IConsumer<Phase> delegate;

        MixinAppender(IConsumer<Phase> delegate) {
            super("MixinLogWatcherAppender", null, null);
            this.delegate = delegate;
        }

        @Override
        public void append(LogEvent event) {
            if (event.getLevel() != Level.DEBUG || !"Validating minecraft".equals(event.getMessage().getFormattedMessage())) {
                return;
            }
            
            // transition to INIT
            this.delegate.accept(Phase.INIT);

            // Only reset the log level if it's still ALL. If something
            // else changed the log level after we did, we don't want
            // overwrite that change. No null check is needed here
            // because the appender will not be injected if the log is
            // null
            if (MixinPlatformAgentFMLLegacy.log.getLevel() == Level.ALL) {
                MixinPlatformAgentFMLLegacy.log.setLevel(MixinPlatformAgentFMLLegacy.oldLevel);
            }
        }
        
    }

}
