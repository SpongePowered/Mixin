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
package org.spongepowered.tools.obfuscation.mcp;

import java.util.Collection;
import java.util.Set;

import org.spongepowered.tools.obfuscation.service.IObfuscationService;
import org.spongepowered.tools.obfuscation.service.ObfuscationTypeDescriptor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Default obfuscation service, provides MCP
 */
public class ObfuscationServiceMCP implements IObfuscationService {
    
    public static final String SEARGE                  = "searge";
    public static final String NOTCH                   = "notch";
    
    public static final String REOBF_SRG_FILE          = "reobfSrgFile";
    public static final String REOBF_EXTRA_SRG_FILES   = "reobfSrgFiles";
    public static final String REOBF_NOTCH_FILE        = "reobfNotchSrgFile";
    public static final String REOBF_EXTRA_NOTCH_FILES = "reobfNotchSrgFiles";
    public static final String OUT_SRG_SRG_FILE        = "outSrgFile";
    public static final String OUT_NOTCH_SRG_FILE      = "outNotchSrgFile";
    public static final String OUT_REFMAP_FILE         = "outRefMapFile";
    
    @Override
    public Set<String> getSupportedOptions() {
        return ImmutableSet.<String>of(
            ObfuscationServiceMCP.REOBF_SRG_FILE,        
            ObfuscationServiceMCP.REOBF_EXTRA_SRG_FILES,        
            ObfuscationServiceMCP.REOBF_NOTCH_FILE,
            ObfuscationServiceMCP.REOBF_EXTRA_NOTCH_FILES,        
            ObfuscationServiceMCP.OUT_SRG_SRG_FILE,        
            ObfuscationServiceMCP.OUT_NOTCH_SRG_FILE,        
            ObfuscationServiceMCP.OUT_REFMAP_FILE        
        );
    }

    @Override
    public Collection<ObfuscationTypeDescriptor> getObfuscationTypes() {
        return ImmutableList.<ObfuscationTypeDescriptor>of(
              new ObfuscationTypeDescriptor(
                  ObfuscationServiceMCP.SEARGE,
                  ObfuscationServiceMCP.REOBF_SRG_FILE,
                  ObfuscationServiceMCP.REOBF_EXTRA_SRG_FILES,
                  ObfuscationServiceMCP.OUT_SRG_SRG_FILE,
                  ObfuscationEnvironmentMCP.class
              ),
              new ObfuscationTypeDescriptor(
                  ObfuscationServiceMCP.NOTCH,
                  ObfuscationServiceMCP.REOBF_NOTCH_FILE,
                  ObfuscationServiceMCP.REOBF_EXTRA_NOTCH_FILES,
                  ObfuscationServiceMCP.OUT_NOTCH_SRG_FILE,
                  ObfuscationEnvironmentMCP.class
              )
        );
    }
    
}
