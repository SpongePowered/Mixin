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
package org.spongepowered.tools.obfuscation.fg3;

import java.util.Collection;
import java.util.Set;

import org.spongepowered.tools.obfuscation.SupportedOptions;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.service.IObfuscationService;
import org.spongepowered.tools.obfuscation.service.ObfuscationTypeDescriptor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;

/**
 * Obfuscation service for ForgeGradle 3.+
 */
public class ObfuscationServiceFG3 implements IObfuscationService {
    
    public static final String SEARGE                = "searge";

    public static final String REOBF_TSRG_FILE       = "reobfTsrgFile";
    public static final String OUT_TSRG_SRG_FILE     = "outTsrgFile";
    public static final String TSRG_OUTPUT_BEHAVIOUR = "mergeBehaviour";
    
    @Override
    public Set<String> getSupportedOptions() {
        return ImmutableSet.<String>of(
            ObfuscationServiceFG3.REOBF_TSRG_FILE,        
            ObfuscationServiceFG3.OUT_TSRG_SRG_FILE,        
            ObfuscationServiceFG3.TSRG_OUTPUT_BEHAVIOUR
        );
    }

    @Override
    public Collection<ObfuscationTypeDescriptor> getObfuscationTypes(IMixinAnnotationProcessor ap) {
        Builder<ObfuscationTypeDescriptor> list = ImmutableList.<ObfuscationTypeDescriptor>builder();
        if (ap.getOptions(SupportedOptions.MAPPING_TYPES).contains("tsrg")) {
            list.add(
              new ObfuscationTypeDescriptor(
                  ObfuscationServiceFG3.SEARGE,
                  ObfuscationServiceFG3.REOBF_TSRG_FILE,
                  null,
                  ObfuscationServiceFG3.OUT_TSRG_SRG_FILE,
                  ObfuscationEnvironmentFG3.class
              )
            );
        }
        return list.build();
    }
    
}
