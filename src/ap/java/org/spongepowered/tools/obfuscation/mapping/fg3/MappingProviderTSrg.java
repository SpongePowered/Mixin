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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;

import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.asm.obfuscation.mapping.mcp.MappingFieldSrg;
import org.spongepowered.tools.obfuscation.mapping.common.MappingProvider;

import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.io.Files;

/**
 * TSRG Mapping provider
 */
public class MappingProviderTSrg extends MappingProvider {
    
    private List<String> inputMappings = new ArrayList<String>();

    public MappingProviderTSrg(Messager messager, Filer filer) {
        super(messager, filer);
    }

    @Override
    public void read(final File input) throws IOException {
        // Locally scoped to avoid synthetic accessor
        final BiMap<String, String> packageMap = this.packageMap;
        final BiMap<String, String> classMap = this.classMap;
        final BiMap<MappingField, MappingField> fieldMap = this.fieldMap;
        final BiMap<MappingMethod, MappingMethod> methodMap = this.methodMap;
        
        String fromClass = null, toClass = null;
        this.inputMappings.addAll(Files.readLines(input, Charset.defaultCharset()));
        
        for (String line : this.inputMappings) {
            if (Strings.isNullOrEmpty(line) || line.startsWith("#") || line.startsWith("tsrg2") || line.startsWith("\t\t")) {
                continue;
            }
            
            String[] parts = line.split(" ");
            if (line.startsWith("\t")) {
                if (fromClass == null) {
                    throw new IllegalStateException("Error parsing TSRG file, found member declaration with no class: " + line);
                }
                parts[0] = parts[0].substring(1);
                if (parts.length == 2) {
                    fieldMap.forcePut(new MappingField(fromClass, parts[0]), new MappingField(toClass, parts[1]));
                } else if (parts.length == 3) {
                    methodMap.forcePut(new MappingMethod(fromClass, parts[0], parts[1]), new MappingMethodLazy(toClass, parts[2],
                            parts[1], MappingProviderTSrg.this));
                } else {
                    throw new IllegalStateException("Error parsing TSRG file, too many arguments: " + line);
                }
            } else if (parts.length > 1) {
                String from = parts[0];
                if (parts.length == 2) {
                    String to = parts[1];
                    if (from.endsWith("/")) {
                        packageMap.forcePut(from.substring(0, from.length() - 1), to.substring(0, to.length() - 1));
                    } else {
                        classMap.forcePut(from, to);
                        fromClass = from;
                        toClass = to;
                    }
                } else if (parts.length > 2) {
                    String to = classMap.get(from);
                    if (to == null) {
                        throw new IllegalStateException("Error parsing TSRG file, found inline member before class mapping: " + line);
                    }
                    if (parts.length == 3) {
                        fieldMap.forcePut(new MappingField(from, parts[1]), new MappingField(to, parts[2]));
                    } else if (parts.length == 4) {
                        methodMap.forcePut(new MappingMethod(from, parts[1], parts[2]), new MappingMethodLazy(to, parts[3], parts[2],
                                MappingProviderTSrg.this));
                    } else {
                        throw new IllegalStateException("Error parsing TSRG file, too many arguments: " + line);
                    }
                }
            } else {
                throw new IllegalStateException("Error parsing TSRG, unrecognised directive: " + line);
            }
        }
    }
    
    @Override
    public MappingField getFieldMapping(MappingField field) {
        // SRG fields do not have descriptors so strip the field descriptor before looking up
        if (field.getDesc() != null) {
            field = new MappingFieldSrg(field);
        }
        return this.fieldMap.get(field);
    }

    List<String> getInputMappings() {
        return this.inputMappings;
    }
    
}
