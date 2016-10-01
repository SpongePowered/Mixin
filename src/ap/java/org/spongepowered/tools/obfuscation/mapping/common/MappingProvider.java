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
package org.spongepowered.tools.obfuscation.mapping.common;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;

import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.mapping.IMappingProvider;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Base class for mapping providers
 */
public abstract class MappingProvider implements IMappingProvider {
    
    protected final Messager messager;
    protected final Filer filer;

    protected final BiMap<String, String> packageMap = HashBiMap.create();
    protected final BiMap<String, String> classMap = HashBiMap.create();
    protected final BiMap<MappingField, MappingField> fieldMap = HashBiMap.create();
    protected final BiMap<MappingMethod, MappingMethod> methodMap = HashBiMap.create();

    public MappingProvider(Messager messager, Filer filer) {
        this.messager = messager;
        this.filer = filer;
    }

    @Override
    public void clear() {
        this.packageMap.clear();
        this.classMap.clear();
        this.fieldMap.clear();
        this.methodMap.clear();
    }

    @Override
    public boolean isEmpty() {
        return this.packageMap.isEmpty() && this.classMap.isEmpty() && this.fieldMap.isEmpty() && this.methodMap.isEmpty();
    }

    @Override
    public MappingMethod getMethodMapping(MappingMethod method) {
        return this.methodMap.get(method);
    }

    @Override
    public MappingField getFieldMapping(MappingField field) {
        return this.fieldMap.get(field);
    }

    @Override
    public String getClassMapping(String className) {
        return this.classMap.get(className);
    }

    @Override
    public String getPackageMapping(String packageName) {
        return this.packageMap.get(packageName);
    }

}
