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

import java.net.URI;

import org.spongepowered.asm.util.Constants.ManifestAttributes;

/**
 * Default platform agent, handles the mixin manifest keys such as
 * <tt>MixinConfigs</tt> and <tt>MixinTokenProviders</tt>.
 */
public class MixinPlatformAgentDefault extends MixinPlatformAgentAbstract {

    /**
     * @param manager platform manager
     * @param uri URI of the resource for this agent
     */
    public MixinPlatformAgentDefault(MixinPlatformManager manager, URI uri) {
        super(manager, uri);
    }

    @Override
    public void prepare() {
        @SuppressWarnings("deprecation")
        String compatibilityLevel = this.attributes.get(ManifestAttributes.COMPATIBILITY);
        if (compatibilityLevel != null) {
            this.manager.setCompatibilityLevel(compatibilityLevel);
        }
        
        String mixinConfigs = this.attributes.get(ManifestAttributes.MIXINCONFIGS);
        if (mixinConfigs != null) {
            for (String config : mixinConfigs.split(",")) {
                this.manager.addConfig(config.trim());
            }
        }
        
        String tokenProviders = this.attributes.get(ManifestAttributes.TOKENPROVIDERS);
        if (tokenProviders != null) {
            for (String provider : tokenProviders.split(",")) {
                this.manager.addTokenProvider(provider.trim());
            }
        }
    }
    
    @Override
    public void initPrimaryContainer() {
    }

    @Override
    public void inject() {
    }
    
    @Override
    public String getLaunchTarget() {
        return this.attributes.get(ManifestAttributes.MAINCLASS);
    }

}
