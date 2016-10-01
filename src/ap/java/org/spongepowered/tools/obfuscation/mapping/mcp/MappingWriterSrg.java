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
package org.spongepowered.tools.obfuscation.mapping.mcp;

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;

import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.ObfuscationType;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer.MappingSet;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer.MappingSet.Pair;
import org.spongepowered.tools.obfuscation.mapping.common.MappingWriter;

/**
 * Writer for SRG mappings
 */
public class MappingWriterSrg extends MappingWriter {
    
    public MappingWriterSrg(Messager messager, Filer filer) {
        super(messager, filer);
    }

    @Override
    public void write(final String output, ObfuscationType type, MappingSet<MappingField> fields, MappingSet<MappingMethod> methods) {
        if (output == null) {
            return;
        }

        PrintWriter writer = null;
        
        try {
            writer = this.openFileWriter(output, type + " output SRGs");
            this.writeFieldMappings(writer, fields);
            this.writeMethodMappings(writer, methods);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ex) {
                    // oh well
                }
            }
        }
    }

    protected void writeFieldMappings(PrintWriter writer, MappingSet<MappingField> fields) {
        for (Pair<MappingField> field : fields) {
            writer.println(this.formatFieldMapping(field));
        }
    }

    protected void writeMethodMappings(PrintWriter writer, MappingSet<MappingMethod> methods) {
        for (Pair<MappingMethod> method : methods) {
            writer.println(this.formatMethodMapping(method));
        }
    }
    
    protected String formatFieldMapping(Pair<MappingField> mapping) {
        return String.format("FD: %s/%s %s/%s", mapping.from.getOwner(), mapping.from.getName(), mapping.to.getOwner(), mapping.to.getName());
    }
    
    protected String formatMethodMapping(Pair<MappingMethod> mapping) {
        return String.format("MD: %s %s %s %s", mapping.from.getName(), mapping.from.getDesc(), mapping.to.getName(), mapping.to.getDesc());
    }

}
