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
package org.spongepowered.tools.obfuscation;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer.MappingSet.Pair;

/**
 * Reference implementation of mapping consumer
 */
class Mappings implements IMappingConsumer {
    
    /**
     * Stored (ordered) field mappings
     */
    private final Map<ObfuscationType, MappingSet<MappingField>> fieldMappings =
            new HashMap<ObfuscationType, MappingSet<MappingField>>();
    
    /**
     * Stored (ordered) method mappings
     */
    private final Map<ObfuscationType, MappingSet<MappingMethod>> methodMappings =
            new HashMap<ObfuscationType, MappingSet<MappingMethod>>();
    
    
    public Mappings() {
        this.init();
    }

    private void init() {
        for (ObfuscationType obfType : ObfuscationType.types()) {
            this.fieldMappings.put(obfType, new MappingSet<MappingField>());
            this.methodMappings.put(obfType, new MappingSet<MappingMethod>());
        }
    }
    
    /**
     * Get stored field mappings
     */
    @Override
    public MappingSet<MappingField> getFieldMappings(ObfuscationType type) {
        MappingSet<MappingField> mappings = this.fieldMappings.get(type);
        return mappings != null ? mappings : new MappingSet<MappingField>();
    }
    
    /**
     * Get stored method mappings
     */
    @Override
    public MappingSet<MappingMethod> getMethodMappings(ObfuscationType type) {
        MappingSet<MappingMethod> mappings = this.methodMappings.get(type);
        return mappings != null ? mappings : new MappingSet<MappingMethod>();
    }

    /**
     * Clear all stored mappings
     */
    @Override
    public void clear() {
        this.fieldMappings.clear();
        this.methodMappings.clear();
        this.init();
    }

    @Override
    public void addFieldMapping(ObfuscationType type, MappingField from, MappingField to) {
        MappingSet<MappingField> mappings = this.fieldMappings.get(type);
        if (mappings == null) {
            mappings = new MappingSet<MappingField>();
            this.fieldMappings.put(type, mappings);
        }
        mappings.add(new Pair<MappingField>(from, to));
    }

    @Override
    public void addMethodMapping(ObfuscationType type, MappingMethod from, MappingMethod to) {
        MappingSet<MappingMethod> mappings = this.methodMappings.get(type);
        if (mappings == null) {
            mappings = new MappingSet<MappingMethod>();
            this.methodMappings.put(type, mappings);
        }
        mappings.add(new Pair<MappingMethod>(from, to));
    }

}
