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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;

import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.asm.obfuscation.mapping.mcp.MappingFieldSrg;
import org.spongepowered.tools.obfuscation.mapping.common.MappingProvider;

import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

/**
 * Ported from <strong>Srg2Source</strong> (
 * <a href=\"https://github.com/MinecraftForge/Srg2Source\">
 * github.com/MinecraftForge/Srg2Source</a>).
 */
public class MappingProviderSrg extends MappingProvider {

    public MappingProviderSrg(Messager messager, Filer filer) {
        super(messager, filer);
    }

    @Override
    public void read(final File input) throws IOException {
        // Locally scoped to avoid synthetic accessor
        final BiMap<String, String> packageMap = this.packageMap;
        final BiMap<String, String> classMap = this.classMap;
        final BiMap<MappingField, MappingField> fieldMap = this.fieldMap;
        final BiMap<MappingMethod, MappingMethod> methodMap = this.methodMap;

        Files.readLines(input, Charset.defaultCharset(), new LineProcessor<String>() {
            @Override
            public String getResult() {
                return null;
            }
            
            @Override
            public boolean processLine(String line) throws IOException {
                if (Strings.isNullOrEmpty(line) || line.startsWith("#")) {
                    return true;
                }
    
                String type = line.substring(0, 2);
                String[] args = line.substring(4).split(" ");
    
                if (type.equals("PK")) {
                    packageMap.forcePut(args[0], args[1]);
                } else if (type.equals("CL")) {
                    classMap.forcePut(args[0], args[1]);
                } else if (type.equals("FD")) {
                    fieldMap.forcePut(new MappingFieldSrg(args[0]).copy(), new MappingFieldSrg(args[1]).copy());
                } else if (type.equals("MD")) {
                    methodMap.forcePut(new MappingMethod(args[0], args[1]), new MappingMethod(args[2], args[3]));
                } else {
                    throw new MixinException("Invalid SRG file: " + input);
                }
                
                return true;
            }
        });
    }
    
    @Override
    public MappingField getFieldMapping(MappingField field) {
        // SRG fields do not have descriptors so strip the field descriptor before looking up
        if (field.getDesc() != null) {
            field = new MappingFieldSrg(field);
        }
        return this.fieldMap.get(field);
    }
}
