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

import org.spongepowered.asm.obfuscation.mapping.IMapping;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer.MappingSet.Pair;

/**
 * Reference implementation of mapping consumer
 */
class Mappings implements IMappingConsumer {
    
    /**
     * Exception thrown by {@link UniqueMappings} when a conflict occurs
     */
    public static class MappingConflictException extends RuntimeException {

        private static final long serialVersionUID = 1L;
        
        private final IMapping<?> oldMapping, newMapping;

        public MappingConflictException(IMapping<?> oldMapping, IMapping<?> newMapping) {
            this.oldMapping = oldMapping;
            this.newMapping = newMapping;
        }
        
        public IMapping<?> getOld() {
            return this.oldMapping;
        }
        
        public IMapping<?> getNew() {
            return this.newMapping;
        }
        
    }
    
    /**
     * A wrapper for mapping consumer which ensures all mappings added are
     * unique
     */
    static class UniqueMappings implements IMappingConsumer {
        
        private final IMappingConsumer mappings;
        
        private final Map<ObfuscationType, Map<MappingField, MappingField>> fields =
                new HashMap<ObfuscationType, Map<MappingField, MappingField>>();
        private final Map<ObfuscationType, Map<MappingMethod, MappingMethod>> methods =
                new HashMap<ObfuscationType, Map<MappingMethod, MappingMethod>>();

        public UniqueMappings(IMappingConsumer mappings) {
            this.mappings = mappings;
        }

        @Override
        public void clear() {
            this.clearMaps();
            this.mappings.clear();
        }

        protected void clearMaps() {
            this.fields.clear();
            this.methods.clear();
        }
        
        @Override
        public void addFieldMapping(ObfuscationType type, MappingField from, MappingField to) {
            if (!this.<MappingField>checkForExistingMapping(type, from, to, this.fields)) {
                this.mappings.addFieldMapping(type, from, to);
            }
        }

        @Override
        public void addMethodMapping(ObfuscationType type, MappingMethod from, MappingMethod to) {
            if (!this.<MappingMethod>checkForExistingMapping(type, from, to, this.methods)) {
                this.mappings.addMethodMapping(type, from, to);
            }
        }

        private <TMapping extends IMapping<TMapping>> boolean checkForExistingMapping(ObfuscationType type, TMapping from, TMapping to, 
                Map<ObfuscationType, Map<TMapping, TMapping>> mappings) throws MappingConflictException {
            Map<TMapping, TMapping> existingMappings = mappings.get(type);
            if (existingMappings == null) {
                existingMappings = new HashMap<TMapping, TMapping>();
                mappings.put(type, existingMappings);
            }
            TMapping existing = existingMappings.get(from);
            if (existing != null) {
                if (existing.equals(to)) {
                    return true;
                }
                throw new MappingConflictException(existing, to);
            }
            existingMappings.put(from, to);
            return false;
        }

        @Override
        public MappingSet<MappingField> getFieldMappings(ObfuscationType type) {
            return this.mappings.getFieldMappings(type);
        }

        @Override
        public MappingSet<MappingMethod> getMethodMappings(ObfuscationType type) {
            return this.mappings.getMethodMappings(type);
        }
        
    }

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
    
    private UniqueMappings unique;
    
    public Mappings() {
        this.init();
    }

    private void init() {
        for (ObfuscationType obfType : ObfuscationType.types()) {
            this.fieldMappings.put(obfType, new MappingSet<MappingField>());
            this.methodMappings.put(obfType, new MappingSet<MappingMethod>());
        }
    }
    
    public IMappingConsumer asUnique() {
        if (this.unique == null) {
            this.unique = new UniqueMappings(this);
        }
        return this.unique;
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
        if (this.unique != null) {
            this.unique.clearMaps();
        }
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
