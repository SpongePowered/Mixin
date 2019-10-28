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
package org.spongepowered.tools.obfuscation.mapping.fg3;

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;

import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.ObfuscationType;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer.MappingSet.Pair;
import org.spongepowered.tools.obfuscation.mapping.mcp.MappingWriterSrg;

/**
 * Writer for SRG mappings
 */
public class MappingWriterTSrg extends MappingWriterSrg {
    
    private final MappingProviderTSrg provider;
    private final boolean mergeExisting;
    
    public MappingWriterTSrg(Messager messager, Filer filer, MappingProviderTSrg provider, boolean mergeExisting) {
        super(messager, filer);
        this.provider = provider;
        this.mergeExisting = mergeExisting;
    }

    @Override
    protected PrintWriter openFileWriter(String output, ObfuscationType type) throws IOException {
        return this.openFileWriter(output, type + " composite mappings");
    }

    @Override
    protected void writeHeader(PrintWriter writer) {
        if (this.mergeExisting) {
            for (String line : this.provider.getInputMappings()) {
                writer.println(line);
            }
        }
    }

    @Override
    protected String formatFieldMapping(Pair<MappingField> field) {
        return String.format("%s %s %s", field.from.getOwner(), field.from.getSimpleName(), field.to.getSimpleName());
    }
    
    @Override
    protected String formatMethodMapping(Pair<MappingMethod> method) {
        return String.format("%s %s %s %s", method.from.getOwner(), method.from.getSimpleName(), method.from.getDesc(), method.to.getSimpleName());
    }

}
