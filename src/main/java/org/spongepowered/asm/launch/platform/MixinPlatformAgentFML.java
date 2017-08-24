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
import java.net.URI;
//import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.spongepowered.asm.launch.Blackboard;
import org.spongepowered.asm.mixin.MixinEnvironment;
//import org.spongepowered.asm.mixin.environment.IPhaseProvider;
//import org.spongepowered.asm.mixin.environment.PhaseDefinition;
//import org.spongepowered.asm.mixin.environment.phase.OnLogMessage;
import org.spongepowered.asm.mixin.extensibility.IRemapper;

//import com.google.common.collect.ImmutableList;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * Platform agent for use under FML.
 * 
 * <p>When FML is present we scan containers for the manifest entries which are
 * inhibited by the tweaker, in particular the <tt>FMLCorePlugin</tt> and
 * <tt>FMLCorePluginContainsFMLMod</tt> entries. This is required because FML
 * performs no further processing of containers if they contain a tweaker!</p>
 */
public class MixinPlatformAgentFML extends MixinPlatformAgentAbstract {
    
    private static final String LOAD_CORE_MOD_METHOD = "loadCoreMod";
    private static final String GET_REPARSEABLE_COREMODS_METHOD = "getReparseableCoremods";
    private static final String CORE_MOD_MANAGER_CLASS = "net.minecraftforge.fml.relauncher.CoreModManager";
    private static final String GET_IGNORED_MODS_METHOD = "getIgnoredMods";
    
    private static final String FML_REMAPPER_ADAPTER_CLASS = "org.spongepowered.asm.bridge.RemapperAdapterFML";
    private static final String FML_CMDLINE_COREMODS = "fml.coreMods.load";
    private static final String FML_PLUGIN_WRAPPER_CLASS = "FMLPluginWrapper";
    private static final String FML_COREMOD_INSTANCE_FIELD = "coreModInstance";

    private static final String MFATT_FORCELOADASMOD = "ForceLoadAsMod";
    private static final String MFATT_FMLCOREPLUGIN = "FMLCorePlugin";
    private static final String MFATT_COREMODCONTAINSMOD = "FMLCorePluginContainsFMLMod";

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
        for (String cmdLineCoreMod : System.getProperty(MixinPlatformAgentFML.FML_CMDLINE_COREMODS, "").split(",")) {
            if (!cmdLineCoreMod.isEmpty()) {
                MixinPlatformAgentAbstract.logger.debug("FML platform agent will ignore coremod {} specified on the command line", cmdLineCoreMod);
                MixinPlatformAgentFML.loadedCoreMods.add(cmdLineCoreMod);
            }
        }
    }
    
    /**
     * If running under FML, we will attempt to inject any coremod specified in
     * the metadata, FML's CoremodManager returns an ITweaker instance which is
     * the "handle" to the injected mod, we will need to proxy calls through to
     * the wrapper. If initialisation fails (for example if we are not running
     * under FML or if an FMLCorePlugin key is not specified in the metadata)
     * then this handle will be null.
     */
    private final ITweaker coreModWrapper;
    
    /**
     * Name of this container
     */
    private final String fileName;
    
    /**
     * Core mod manager class
     */
    private Class<?> clCoreModManager;

    /**
     * @param manager platform manager
     * @param uri URI of the resource for this agent
     */
    public MixinPlatformAgentFML(MixinPlatformManager manager, URI uri) {
        super(manager, uri);
        this.fileName = this.container.getName();
        this.coreModWrapper = this.initFMLCoreMod();
    }

    /**
     * Attempts to initialise the FML coremod (if specified in the jar metadata)
     */
    private ITweaker initFMLCoreMod() {
        try {
            try {
                this.clCoreModManager = MixinPlatformAgentFML.getCoreModManagerClass();
            } catch (ClassNotFoundException ex) {
                MixinPlatformAgentAbstract.logger.info("FML platform manager could not load class {}. Proceeding without FML support.",
                        ex.getMessage());
                return null;
            }

            if ("true".equalsIgnoreCase(this.attributes.get(MixinPlatformAgentFML.MFATT_FORCELOADASMOD))) {
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
            MixinPlatformAgentFML.getIgnoredMods(this.clCoreModManager).remove(this.fileName);
        } catch (Exception ex) {
            MixinPlatformAgentAbstract.logger.catching(ex);
        }
        
        if (this.attributes.get(MixinPlatformAgentFML.MFATT_COREMODCONTAINSMOD) != null) {    
            this.addReparseableJar();
        }
    }

    /**
     * Called by {@link #loadAsMod} if the "fml core plugin contains fml mod" is
     * set, adds this container to the "reparsable coremods" collection.
     */
    private void addReparseableJar() {
        try {
            Method mdGetReparsedCoremods = this.clCoreModManager.getDeclaredMethod(Blackboard.getString(
                    Blackboard.Keys.FML_GET_REPARSEABLE_COREMODS, MixinPlatformAgentFML.GET_REPARSEABLE_COREMODS_METHOD));
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
        String coreModName = this.attributes.get(MixinPlatformAgentFML.MFATT_FMLCOREPLUGIN);
        if (coreModName == null) {
            return null;
        }
        
        if (this.isAlreadyInjected(coreModName)) {
            MixinPlatformAgentAbstract.logger.debug("{} has core plugin {}. Skipping because it was already injected.", this.fileName, coreModName);
            return null;
        }
        
        MixinPlatformAgentAbstract.logger.debug("{} has core plugin {}. Injecting it into FML for co-initialisation:", this.fileName, coreModName);
        Method mdLoadCoreMod = this.clCoreModManager.getDeclaredMethod(Blackboard.getString(
                Blackboard.Keys.FML_LOAD_CORE_MOD, MixinPlatformAgentFML.LOAD_CORE_MOD_METHOD), LaunchClassLoader.class, String.class, File.class);
        mdLoadCoreMod.setAccessible(true);
        ITweaker wrapper = (ITweaker)mdLoadCoreMod.invoke(null, Launch.classLoader, coreModName, this.container);
        if (wrapper == null) {
            MixinPlatformAgentAbstract.logger.debug("Core plugin {} could not be loaded.", coreModName);
            return null;
        }

        MixinPlatformAgentFML.loadedCoreMods.add(coreModName);
        return wrapper;
    }
    
    private boolean isAlreadyInjected(String coreModName) {
        // Did we already inject this ourselves, or was it specified on the command line
        if (MixinPlatformAgentFML.loadedCoreMods.contains(coreModName)) {
            return true;
        }
        
        // Was it already loaded, check the tweakers list
        try {
            List<ITweaker> tweakers = Blackboard.<List<ITweaker>>get(Blackboard.Keys.TWEAKS);
            if (tweakers == null) {
                return false;
            }
            
            for (ITweaker tweaker : tweakers) {
                Class<? extends ITweaker> tweakClass = tweaker.getClass();
                if (MixinPlatformAgentFML.FML_PLUGIN_WRAPPER_CLASS.equals(tweakClass.getSimpleName())) {
                    Field fdCoreModInstance = tweakClass.getField(MixinPlatformAgentFML.FML_COREMOD_INSTANCE_FIELD);
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
        return MixinPlatformAgentFML.class.getName() + "$PhaseProvider";
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.IMixinPlatformAgent#prepare()
     */
    @Override
    public void prepare() {
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.IMixinPlatformAgent
     *      #initPrimaryContainer()
     */
    @Override
    public void initPrimaryContainer() {
        if (this.clCoreModManager != null) {
//            MixinEnvironment.registerPhaseProvider(MixinPlatformAgentFML.class.getName() + "$PhaseProvider");
            this.injectRemapper();
        }
    }

    private void injectRemapper() {
        try {
            MixinPlatformAgentAbstract.logger.debug("Creating FML remapper adapter: {}", MixinPlatformAgentFML.FML_REMAPPER_ADAPTER_CLASS);
            Class<?> clFmlRemapperAdapter = Class.forName(MixinPlatformAgentFML.FML_REMAPPER_ADAPTER_CLASS, true, Launch.classLoader);
            Method mdCreate = clFmlRemapperAdapter.getDeclaredMethod("create");
            IRemapper remapper = (IRemapper)mdCreate.invoke(null);
            MixinEnvironment.getDefaultEnvironment().getRemappers().add(remapper);
        } catch (Exception ex) {
            MixinPlatformAgentAbstract.logger.debug("Failed instancing FML remapper adapter, things will probably go horribly for notch-obf'd mods!");
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.IMixinPlatformAgent
     *     #injectIntoClassLoader(net.minecraft.launchwrapper.LaunchClassLoader)
     */
    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        if (this.coreModWrapper != null) {
            if (!this.isFMLInjected()) {
                MixinPlatformAgentAbstract.logger.debug("FML agent is co-initialising coremod instance {} for {}", this.coreModWrapper, this.uri);
                this.coreModWrapper.injectIntoClassLoader(classLoader);
            } else {
                MixinPlatformAgentAbstract.logger.debug("FML agent is skipping co-init for {} because FML already started", this.coreModWrapper);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.IMixinPlatformAgent#getLaunchTarget()
     */
    @Override
    public String getLaunchTarget() {
        return null;
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
    protected final boolean isFMLInjected() {
        for (String tweaker : Blackboard.<List<String>>get(Blackboard.Keys.TWEAKCLASSES)) {
            if (tweaker.endsWith("FMLDeobfTweaker")) {
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
            return Class.forName(Blackboard.getString(
                    Blackboard.Keys.FML_CORE_MOD_MANAGER, MixinPlatformAgentFML.CORE_MOD_MANAGER_CLASS));
        } catch (ClassNotFoundException ex) {
            return Class.forName("cpw.mods.fml.relauncher.CoreModManager");
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> getIgnoredMods(Class<?> clCoreModManager) throws IllegalAccessException, InvocationTargetException {
        Method mdGetIgnoredMods = null;
        
        try {
            mdGetIgnoredMods = clCoreModManager.getDeclaredMethod(Blackboard.getString(
                    Blackboard.Keys.FML_GET_IGNORED_MODS, MixinPlatformAgentFML.GET_IGNORED_MODS_METHOD));
        } catch (NoSuchMethodException ex1) {
            try {
                // Legacy name
                mdGetIgnoredMods = clCoreModManager.getDeclaredMethod("getLoadedCoremods");
            } catch (NoSuchMethodException ex2) {
                MixinPlatformAgentAbstract.logger.catching(Level.DEBUG, ex2);
                return Collections.<String>emptyList();
            }
        }
        
        return (List<String>)mdGetIgnoredMods.invoke(null);
    }

}
