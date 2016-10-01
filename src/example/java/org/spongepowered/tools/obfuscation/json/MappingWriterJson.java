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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;

import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.ObfuscationType;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer.MappingSet;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer.MappingSet.Pair;
import org.spongepowered.tools.obfuscation.mapping.common.MappingWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Uses Gson to write mappings to output JSON file
 */
public class MappingWriterJson extends MappingWriter {

    public MappingWriterJson(Messager messager, Filer filer) {
        super(messager, filer);
    }

    @Override
    public void write(String output, ObfuscationType type, MappingSet<MappingField> fields, MappingSet<MappingMethod> methods) {
        if (output == null) {
            return;
        }
        
        Map<String, String> fieldMappings = new LinkedHashMap<String, String>();
        for (Pair<MappingField> field : fields) {
            fieldMappings.put(field.from.serialise(), field.to.serialise());
        }

        Map<String, String> methodMappings = new LinkedHashMap<String, String>();
        for (Pair<MappingMethod> method : methods) {
            methodMappings.put(method.from.serialise(), method.to.serialise());
        }
        
        Map<String, Map<String, String>> mappings = new LinkedHashMap<String, Map<String, String>>();
        mappings.put("fields", fieldMappings);
        mappings.put("methods", methodMappings);
        
        this.writeJson(output, type, mappings);
    }

    protected void writeJson(String output, ObfuscationType type, Map<String, Map<String, String>> mappings) {
        PrintWriter writer = null;
        
        try {
            writer = this.openFileWriter(output, type + " output");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(mappings, writer);
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
}
