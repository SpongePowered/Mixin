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
package org.spongepowered.asm.launch;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.spongepowered.asm.mixin.MixinEnvironment;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;


/**
 * TweakClass for running mixins in production. Being a tweaker ensures that we
 * get injected into the AppClassLoader but does mean that we will need to
 * inject the FML coremod by hand if running under FML.
 */
public class MixinTweaker implements ITweaker {
    
    private static final String MFATT_MIXINCONFIGS = "MixinConfigs";
    private static final String MFATT_FORCELOADASMOD = "ForceLoadAsMod";
    private static final String MFATT_FMLCOREPLUGIN = "FMLCorePlugin";
    private static final String MFATT_COREMODCONTAINSMOD = "FMLCorePluginContainsFMLMod";
    private static final String MFATT_MAINCLASS = "Main-Class";

    /**
     * File containing this tweaker
     */
    private final File container;
    
    /**
     * If running under FML, we will attempt to inject any coremod specified in
     * the metadata, FML's CoremodManager returns an ITweaker instance which is
     * the "handle" to the injected mod, we will need to proxy calls through to
     * the wrapper. If initialisation fails (for example if we are not running
     * under FML or if an FMLCorePlugin key is not specified in the metadata)
     * then this handle will be NULL and the tweaker will attempt to start the
     * Mixin subsystem automatically by looking for a MixinConfigs key in the
     * jar metadata, this should be a comma-separated list of mixin config JSON
     * file names.
     */
    private ITweaker fmlWrapper;

    /**
     * Hello world
     */
    public MixinTweaker() {
        MixinBootstrap.preInit();
        this.container = this.findJarFile();
        this.fmlWrapper = this.initFMLCoreMod();
    }

    /**
     * Find and return the file containing this class
     */
    private File findJarFile() {
        URI uri = null;
        try {
            uri = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
        }
        return uri != null ? new File(uri) : null;
    }

    /* (non-Javadoc)
     * @see net.minecraft.launchwrapper.ITweaker#acceptOptions(java.util.List,
     *      java.io.File, java.io.File, java.lang.String)
     */
    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        
        MixinBootstrap.register();

        if (this.fmlWrapper == null) {
            String mixinConfigs = this.getManifestAttribute(MixinTweaker.MFATT_MIXINCONFIGS);
            if (mixinConfigs == null) {
                return;
            }
            
            for (String config : mixinConfigs.split(",")) {
                if (config.endsWith(".json")) {
                    MixinEnvironment.getDefaultEnvironment().addConfiguration(config);
                }
            }
        }
    }

    /**
     * Attempts to initialise the FML coremod (if specified in the jar metadata)
     */
    @SuppressWarnings("unchecked")
    private ITweaker initFMLCoreMod() {
        try {

            String jarName = this.container.getName();
            Class<?> coreModManager = this.getCoreModManagerClass();

            if ("true".equalsIgnoreCase(this.getManifestAttribute(MixinTweaker.MFATT_FORCELOADASMOD))) {
                try {
                    Method mdGetLoadedCoremods = coreModManager.getDeclaredMethod("getLoadedCoremods");
                    List<String> loadedCoremods = (List<String>)mdGetLoadedCoremods.invoke(null);
                    loadedCoremods.remove(jarName);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                
                if (this.getManifestAttribute(MixinTweaker.MFATT_COREMODCONTAINSMOD) != null) {    
                    try {
                        Method mdGetReparsedCoremods = coreModManager.getDeclaredMethod("getReparseableCoremods");
                        List<String> reparsedCoremods = (List<String>)mdGetReparsedCoremods.invoke(null);
                        if (!reparsedCoremods.contains(jarName)) {
                            reparsedCoremods.add(jarName);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            String coreModName = this.getManifestAttribute(MixinTweaker.MFATT_FMLCOREPLUGIN);
            if (coreModName == null) {
                return null;
            }

            Method mdLoadCoreMod = coreModManager.getDeclaredMethod("loadCoreMod", LaunchClassLoader.class, String.class, File.class);
            mdLoadCoreMod.setAccessible(true);
            ITweaker wrapper = (ITweaker)mdLoadCoreMod.invoke(null, Launch.classLoader, coreModName, this.container);
            if (wrapper == null) {
                return null;
            }

            return wrapper;
        } catch (Exception ex) {
            // ex.printStackTrace();
        }
        return null;
    }

    /**
     * Attempt to get the FML CoreModManager, tries the post-1.8 namespace first
     * and falls back to 1.7.10 if class lookup fails
     */
    private Class<?> getCoreModManagerClass() throws ClassNotFoundException {
        try {
            return Class.forName("net.minecraftforge.fml.relauncher.CoreModManager");
        } catch (ClassNotFoundException ex) {
            return Class.forName("cpw.mods.fml.relauncher.CoreModManager");
        }
    }

    /* (non-Javadoc)
     * @see net.minecraft.launchwrapper.ITweaker#injectIntoClassLoader(
     *      net.minecraft.launchwrapper.LaunchClassLoader)
     */
    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        if (this.fmlWrapper != null) {
            this.fmlWrapper.injectIntoClassLoader(Launch.classLoader);
        }
    }

    /* (non-Javadoc)
     * @see net.minecraft.launchwrapper.ITweaker#getLaunchTarget()
     */
    @Override
    public String getLaunchTarget() {
        String mainClass = this.getManifestAttribute(MixinTweaker.MFATT_MAINCLASS);
        return mainClass != null ? mainClass : "net.minecraft.client.main.Main";
    }

    /* (non-Javadoc)
     * @see net.minecraft.launchwrapper.ITweaker#getLaunchArguments()
     */
    @Override
    public String[] getLaunchArguments() {
        return new String[]{};
    }

    private String getManifestAttribute(String key) {
        if (this.container == null) {
            return null;
        }
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(this.container);
            Attributes manifestAttributes = jarFile.getManifest().getMainAttributes();
            return manifestAttributes.getValue(key);
        } catch (IOException ex) {
            // be quiet checkstyle
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException ex) {
                    // this could be an issue later on :(
                }
            }
        }
        return null;
    }
}
