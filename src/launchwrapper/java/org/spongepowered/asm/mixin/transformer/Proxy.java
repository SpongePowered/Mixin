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
package org.spongepowered.asm.mixin.transformer;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.service.ILegacyClassTransformer;
import org.spongepowered.asm.service.MixinService;

import net.minecraft.launchwrapper.IClassTransformer;

/**
 * Proxy transformer for the mixin transformer. These transformers are used
 * to allow the mixin transformer to be re-registered in the transformer
 * chain at a later stage in startup without having to fully re-initialise
 * the mixin transformer itself. Only the latest proxy to be instantiated
 * will actually provide callbacks to the underlying mixin transformer.
 */
public final class Proxy implements IClassTransformer, ILegacyClassTransformer {
    
    /**
     * All existing proxies
     */
    private static List<Proxy> proxies = new ArrayList<Proxy>();
    
    /**
     * Actual mixin transformer instance
     */
    private static MixinTransformer transformer = new MixinTransformer();
    
    /**
     * True if this is the active proxy, newer proxies disable their older
     * siblings
     */
    private boolean isActive = true;
    
    public Proxy() {
        for (Proxy proxy : Proxy.proxies) {
            proxy.isActive = false;
        }
        
        Proxy.proxies.add(this);
        MixinService.getService().getLogger("mixin").debug("Adding new mixin transformer proxy #{}", Proxy.proxies.size());
    }
    
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (this.isActive) {
            return Proxy.transformer.transformClassBytes(name, transformedName, basicClass);
        }
        
        return basicClass;
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public boolean isDelegationExcluded() {
        return true;
    }

    @Override
    public byte[] transformClassBytes(String name, String transformedName, byte[] basicClass) {
        if (this.isActive) {
            return Proxy.transformer.transformClassBytes(name, transformedName, basicClass);
        }
        
        return basicClass;
    }

}
