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
package org.spongepowered.asm.launch;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IRemapper;

/**
 * Launch agent for use under FML.
 * 
 * <p>When FML is present we scan containers for the manifest entries which are
 * inhibited by the tweaker, in particular the <tt>FMLCorePlugin</tt> and
 * <tt>FMLCorePluginContainsFMLMod</tt> entries. This is required because FML
 * performs no further processing of containers if they contain a tweaker!</p>
 */
public class MixinLaunchAgentFML extends MixinLaunchAgentAbstract {

    private static final String MFATT_FORCELOADASMOD = "ForceLoadAsMod";
    private static final String MFATT_FMLCOREPLUGIN = "FMLCorePlugin";
    private static final String MFATT_COREMODCONTAINSMOD = "FMLCorePluginContainsFMLMod";
    
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
     * @param uri
     */
    public MixinLaunchAgentFML(URI uri) {
        super(uri);
        this.fileName = this.container.getName();
        this.coreModWrapper = this.initFMLCoreMod();
    }

    /**
     * Attempts to initialise the FML coremod (if specified in the jar metadata)
     */
    private ITweaker initFMLCoreMod() {
        try {
            this.clCoreModManager = MixinLaunchAgentFML.getCoreModManagerClass();

            if ("true".equalsIgnoreCase(this.attributes.get(MixinLaunchAgentFML.MFATT_FORCELOADASMOD))) {
                this.loadAsMod();
            }

            return this.injectCorePlugin();
        } catch (Exception ex) {
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
            Method mdGetLoadedCoremods = this.clCoreModManager.getDeclaredMethod("getLoadedCoremods");
            @SuppressWarnings("unchecked")
            List<String> loadedCoremods = (List<String>)mdGetLoadedCoremods.invoke(null);
            loadedCoremods.remove(this.fileName);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        if (this.attributes.get(MixinLaunchAgentFML.MFATT_COREMODCONTAINSMOD) != null) {    
            this.addReparseableJar();
        }
    }

    /**
     * Called by {@link #loadAsMod} if the "fml core plugin contains fml mod" is
     * set, adds this container to the "reparsable coremods" collection.
     */
    private void addReparseableJar() {
        try {
            Method mdGetReparsedCoremods = this.clCoreModManager.getDeclaredMethod("getReparseableCoremods");
            @SuppressWarnings("unchecked")
            List<String> reparsedCoremods = (List<String>)mdGetReparsedCoremods.invoke(null);
            if (!reparsedCoremods.contains(this.fileName)) {
                reparsedCoremods.add(this.fileName);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private ITweaker injectCorePlugin() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String coreModName = this.attributes.get(MixinLaunchAgentFML.MFATT_FMLCOREPLUGIN);
        if (coreModName == null) {
            return null;
        }

        Method mdLoadCoreMod = this.clCoreModManager.getDeclaredMethod("loadCoreMod", LaunchClassLoader.class, String.class, File.class);
        mdLoadCoreMod.setAccessible(true);
        ITweaker wrapper = (ITweaker)mdLoadCoreMod.invoke(null, Launch.classLoader, coreModName, this.container);
        if (wrapper == null) {
            return null;
        }

        return wrapper;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.IMixinLaunchAgent#prepare()
     */
    @Override
    public void prepare() {
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.IMixinLaunchAgent
     *      #initPrimaryContainer()
     */
    @Override
    public void initPrimaryContainer() {
        if (this.clCoreModManager != null) {
            this.injectRemapper();
        }
    }

    private void injectRemapper() {
        try {
            Class<?> clFmlRemapperAdapter = Class.forName("org.spongepowered.asm.bridge.RemapperAdapterFML", true, Launch.classLoader);
            Method mdCreate = clFmlRemapperAdapter.getDeclaredMethod("create");
            IRemapper remapper = (IRemapper)mdCreate.invoke(null);
            MixinEnvironment.getDefaultEnvironment().getRemappers().add(remapper);
        } catch (Exception ex) {
            this.logger.debug("Failed instancing remapper adapter for FML, things will probably go horribly for notch-obf'd mods!");
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.IMixinLaunchAgent
     *     #injectIntoClassLoader(net.minecraft.launchwrapper.LaunchClassLoader)
     */
    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        if (this.coreModWrapper != null && !this.isFMLInjected()) {
            this.coreModWrapper.injectIntoClassLoader(classLoader);
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.IMixinLaunchAgent#getLaunchTarget()
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
    @SuppressWarnings("unchecked")
    protected final boolean isFMLInjected() {
        for (String tweaker : (List<String>)Launch.blackboard.get("TweakClasses")) {
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
            return Class.forName("net.minecraftforge.fml.relauncher.CoreModManager");
        } catch (ClassNotFoundException ex) {
            return Class.forName("cpw.mods.fml.relauncher.CoreModManager");
        }
    }

}
