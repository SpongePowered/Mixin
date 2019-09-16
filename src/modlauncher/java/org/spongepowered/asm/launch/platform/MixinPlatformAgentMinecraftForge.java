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

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.util.Constants;

import com.google.common.collect.ImmutableSet;

import cpw.mods.modlauncher.Environment;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;

/**
 * Platform agent for minecraft forge under ModLauncher
 */
public class MixinPlatformAgentMinecraftForge extends MixinPlatformAgentAbstract implements IMixinPlatformServiceAgent {

    private static final String FML_LOADING_PACKAGE = "net.minecraftforge.fml.loading.";
    private static final String FML_DISCOVERY_PACKAGE = MixinPlatformAgentMinecraftForge.FML_LOADING_PACKAGE + "moddiscovery.";
    
    private static final String LAUNCHER_VERSION_CLASS = MixinPlatformAgentMinecraftForge.FML_LOADING_PACKAGE + "LauncherVersion";
    private static final String LOADING_MOD_LIST_CLASS = MixinPlatformAgentMinecraftForge.FML_LOADING_PACKAGE + "LoadingModList";
    private static final String MOD_FILE_INFO_CLASS = MixinPlatformAgentMinecraftForge.FML_DISCOVERY_PACKAGE + "ModFileInfo";
    private static final String MOD_FILE_CLASS = MixinPlatformAgentMinecraftForge.FML_DISCOVERY_PACKAGE + "ModFile";
    
    private static final String GET_MOD_LIST_METHOD = "get";
    private static final String GET_MOD_FILES_METHOD = "getModFiles";
    private static final String GET_MOD_FILE_METHOD = "getFile";
    private static final String GET_FILE_PATH_METHOD = "getFilePath";
    
    private ContainerHandleVirtual rootContainer;
    
    @Override
    public void init() {
        try {
            Class.forName(MixinPlatformAgentMinecraftForge.LAUNCHER_VERSION_CLASS);
        } catch (Exception ex) {
            MixinPlatformAgentAbstract.logger.debug("FML Launcher Version class was not found, assuming FML is not present");
            return;
        }
        
        String activity = "Initialising FML containers";
        try {
            // Normally I'd cache these reflection objects, but this should only get called once
            activity = "Resolving FML LoadingModList class";
            Class<?> clModList = Class.forName(MixinPlatformAgentMinecraftForge.LOADING_MOD_LIST_CLASS);
            activity = "Resolving FML ModFileInfo class";
            Class<?> clModFileInfo = Class.forName(MixinPlatformAgentMinecraftForge.MOD_FILE_INFO_CLASS);
            activity = "Resolving FML ModFile class";
            Class<?> clModFile = Class.forName(MixinPlatformAgentMinecraftForge.MOD_FILE_CLASS);

            activity = "Resolving FML LoadingModList::get method";
            Method mdGetModListInstance = clModList.getDeclaredMethod(MixinPlatformAgentMinecraftForge.GET_MOD_LIST_METHOD);
            activity = "Resolving FML LoadingModList::getModFiles method";
            Method mdGetModFiles = clModList.getDeclaredMethod(MixinPlatformAgentMinecraftForge.GET_MOD_FILES_METHOD);
            activity = "Resolving FML ModFileInfo::getFile method";
            Method mdGetModFile = clModFileInfo.getDeclaredMethod(MixinPlatformAgentMinecraftForge.GET_MOD_FILE_METHOD);
            activity = "Resolving FML ModFile::getFilePath method";
            Method mdGetFilePath = clModFile.getDeclaredMethod(MixinPlatformAgentMinecraftForge.GET_FILE_PATH_METHOD);
            
            activity = "Calling FML LoadingModList::get method to obtain LoadingModList singleton";
            Object loadingModList = mdGetModListInstance.invoke(null);
            activity = "Calling FML LoadingModList::getModFiles method to obtain list of mod candidates";
            List<?> modlist = (List<?>)mdGetModFiles.invoke(loadingModList);
            for (Object modFileInfo : modlist) {
                activity = "Calling ModFileInfo::getFile method to obtain mod file info";
                Object modFile = mdGetModFile.invoke(modFileInfo);
                activity = "Calling ModFile::getFilePath method to obtain mod file location";
                Path path = (Path)mdGetFilePath.invoke(modFile);
                activity = "Creating ContainerHandleURI instance to contain FML mod: " + path;
                this.rootContainer.add(new ContainerHandleURI(path.toUri()));
            }
            
        } catch (Exception ex) {
            MixinPlatformAgentAbstract.logger.error("Error reading FML mod list contents during activity: {}", activity);
            MixinPlatformAgentAbstract.logger.catching(ex);
        }
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.platform.MixinPlatformAgentAbstract
     *      #accept(org.spongepowered.asm.launch.platform.MixinPlatformManager,
     *      org.spongepowered.asm.launch.platform.container.IContainerHandle)
     */
    @Override
    public boolean accept(MixinPlatformManager manager, IContainerHandle handle) {
        // No containers plz
        return false;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.platform.IMixinPlatformAgent
     *      #getSideName()
     */
    @Override
    public String getSideName() {
        Environment environment = Launcher.INSTANCE.environment();
        final String launchTarget = environment.getProperty(IEnvironment.Keys.LAUNCHTARGET.get()).orElse("missing").toLowerCase(Locale.ROOT);
        if (launchTarget.contains("server")) {
            return Constants.SIDE_SERVER;
        }
        if (launchTarget.contains("client")) {
            return Constants.SIDE_CLIENT;
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.platform.IMixinPlatformServiceAgent
     *      #getMixinContainers()
     */
    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        if (this.rootContainer == null) {
            this.rootContainer = new ContainerHandleVirtual("forge");
        }
        return ImmutableSet.<IContainerHandle>of(this.rootContainer);
    }

}
