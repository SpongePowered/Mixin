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
package org.spongepowered.asm.obfuscation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.spongepowered.asm.mixin.throwables.MixinException;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.io.Files;

import joptsimple.internal.Strings;

/**
 * Ported from <strong>Srg2Source</strong> (
 * <a href=\"https://github.com/MinecraftForge/Srg2Source\">
 * github.com/MinecraftForge/Srg2Source</a>).
 */
public class SrgContainer {

    private final BiMap<String, String> packageMap = HashBiMap.create();
    private final BiMap<String, String> classMap = HashBiMap.create();
    private final BiMap<SrgField, SrgField> fieldMap = HashBiMap.create();
    private final BiMap<SrgMethod, SrgMethod> methodMap = HashBiMap.create();

    public void readSrg(File srg) throws IOException {
        for (String line : Files.readLines(srg, Charset.defaultCharset())) {
            if (Strings.isNullOrEmpty(line) || line.startsWith("#")) {
                continue;
            }

            String type = line.substring(0, 2);
            String[] args = line.substring(4).split(" ");

            if (type.equals("PK")) {
                this.packageMap.forcePut(args[0], args[1]);
            } else if (type.equals("CL")) {
                this.classMap.forcePut(args[0], args[1]);
            } else if (type.equals("FD")) {
                this.fieldMap.forcePut(new SrgField(args[0]), new SrgField(args[1]));
            } else if (type.equals("MD")) {
                this.methodMap.forcePut(new SrgMethod(args[0], args[1]), new SrgMethod(args[2], args[3]));
            } else {
                throw new MixinException("Invalid SRG file: " + srg);
            }
        }
    }

    public SrgMethod getMethodMapping(SrgMethod methodName) {
        return this.methodMap.get(methodName);
    }

    public SrgField getFieldMapping(SrgField fieldName) {
        return this.fieldMap.get(fieldName);
    }

    public String getClassMapping(String className) {
        return this.classMap.get(className);
    }
    
    public String getPackageMapping(String packageName) {
        return this.packageMap.get(packageName);
    }
    
}
