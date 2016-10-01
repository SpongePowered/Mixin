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
package org.spongepowered.tools.obfuscation.json;

import java.util.Collection;
import java.util.Set;

import org.spongepowered.tools.obfuscation.mcp.ObfuscationServiceMCP;
import org.spongepowered.tools.obfuscation.service.IObfuscationService;
import org.spongepowered.tools.obfuscation.service.ObfuscationTypeDescriptor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * An example obfuscation service which writes mappings to JSON files. For the
 * purposes of the example, we actually use an SRG mapping source and a JSON
 * mapping writer. However in practice a custom mapping provider would be
 * employed as well.
 */
public class ObfuscationServiceJson implements IObfuscationService {
    
    public static final String KEY = "json";
    public static final String OUTPUT_JSON_FILE = "outputJson";
    
    @Override
    public Set<String> getSupportedOptions() {
        return ImmutableSet.<String>of(
            ObfuscationServiceMCP.REOBF_SRG_FILE, // Use SRG file for mapping source
            ObfuscationServiceJson.OUTPUT_JSON_FILE
        );
    }

    @Override
    public Collection<ObfuscationTypeDescriptor> getObfuscationTypes() {
        return ImmutableList.<ObfuscationTypeDescriptor>of(
            new ObfuscationTypeDescriptor(
                ObfuscationServiceJson.KEY,
                ObfuscationServiceMCP.REOBF_SRG_FILE,
                ObfuscationServiceJson.OUTPUT_JSON_FILE,
                ObfuscationEnvironmentJson.class
            )
        );
    }

}
