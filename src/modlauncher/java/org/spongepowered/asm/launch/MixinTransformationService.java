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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import com.google.common.collect.ImmutableList;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionSpecBuilder;

/**
 * Service for handling transforms mixin under ModLauncher
 */
public class MixinTransformationService implements ITransformationService {
    
    private ArgumentAcceptingOptionSpec<String> mixinsArgument;
    private List<String> commandLineMixins = new ArrayList<String>();
    private MixinLaunchPlugin plugin;
    
    @Override
    public String name() {
        return MixinLaunchPlugin.NAME;
    }
    
    @Override
    public void arguments(BiFunction<String, String, OptionSpecBuilder> argumentBuilder) {
        this.mixinsArgument = argumentBuilder.apply("config", "a mixin config to load")
                .withRequiredArg().ofType(String.class);
    }
    
    @Override
    public void argumentValues(OptionResult option) {
        this.commandLineMixins.addAll(option.values(this.mixinsArgument));
    }

    @Override
    public void onLoad(IEnvironment environment, Set<String> otherServices) throws IncompatibleEnvironmentException {
    }

    @Override
    public void initialize(IEnvironment environment) {
        Optional<ILaunchPluginService> plugin = environment.findLaunchPlugin(MixinLaunchPlugin.NAME);
        if (!plugin.isPresent()) {
            throw new MixinInitialisationError("Mixin Launch Plugin Service could not be located");
        }
        ILaunchPluginService launchPlugin = plugin.get();
        if (!(launchPlugin instanceof MixinLaunchPlugin)) {
            throw new MixinInitialisationError("Mixin Launch Plugin Service is present but not compatible");
        }
        this.plugin = (MixinLaunchPlugin)launchPlugin;
        
        MixinBootstrap.start();
        this.plugin.init(environment, this.commandLineMixins);
    }
    
    @Override
    public void beginScanning(IEnvironment environment) {
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<ITransformer> transformers() {
        return ImmutableList.<ITransformer>of();
    }
    
}
