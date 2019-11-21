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

import java.util.Collection;
import java.util.Locale;

import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.util.Constants;

import cpw.mods.modlauncher.Environment;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;

/**
 * Platform agent for minecraft forge under ModLauncher, only detects the side
 */
public class MixinPlatformAgentMinecraftForge extends MixinPlatformAgentAbstract implements IMixinPlatformServiceAgent {

    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.platform.IMixinPlatformServiceAgent
     *      #init()
     */
    @Override
    public void init() {
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.platform.MixinPlatformAgentAbstract
     *      #accept(org.spongepowered.asm.launch.platform.MixinPlatformManager,
     *      org.spongepowered.asm.launch.platform.container.IContainerHandle)
     */
    @Override
    public AcceptResult accept(MixinPlatformManager manager, IContainerHandle handle) {
        // No containers plz
        return AcceptResult.REJECTED;
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
        return null;
    }

}
