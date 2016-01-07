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
package org.spongepowered.asm.mixin.injection.struct;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.launchwrapper.Launch;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class ReferenceMapper implements Serializable {
    
    private static final long serialVersionUID = 2L;

    public static final String DEFAULT_RESOURCE = "mixin.refmap.json";
    
    public static final ReferenceMapper DEFAULT_MAPPER = new ReferenceMapper(true);
    
    private final Map<String, Map<String, String>> mappings = Maps.newHashMap();
    
    private final Map<String, Map<String, Map<String, String>>> data = Maps.newHashMap();
    
    private final transient boolean readOnly; 
    
    private transient String context = null;
    
    public ReferenceMapper() {
        this(false);
    }
    
    private ReferenceMapper(boolean readOnly) {
        this.readOnly = readOnly;
    }
    
    public String getContext() {
        return this.context;
    }
    
    public void setContext(String context) {
        this.context = context;
    }
    
    public String remap(String className, String reference) {
        return this.remapWithContext(this.context, className, reference);
    }
    
    public String remapWithContext(String context, String className, String reference) {
        Map<String, Map<String, String>> mappings = this.mappings;
        if (context != null) {
            mappings = this.data.get(context);
            if (mappings == null) {
                mappings = this.mappings;
            }
        }
        return this.remap(mappings, className, reference);
    }
    
    private String remap(Map<String, Map<String, String>> mappings, String className, String reference) {
        if (className == null) {
            for (Map<String, String> mapping : mappings.values()) {
                if (mapping.containsKey(reference)) {
                    return mapping.get(reference);
                }
            }
        }
        
        Map<String, String> classMappings = mappings.get(className);
        if (classMappings == null) {
            return reference;
        }
        String remappedReference = classMappings.get(reference);
        return remappedReference != null ? remappedReference : reference;
    }
    
    public void addMapping(String context, String className, String reference, String newReference) {
        if (this.readOnly) {
            return;
        }
        Map<String, Map<String, String>> mappings = this.mappings;
        if (context != null) {
            mappings = this.data.get(context);
            if (mappings == null) {
                mappings = Maps.newHashMap();
                this.data.put(context, mappings);
            }
        }
        Map<String, String> classMappings = mappings.get(className);
        if (classMappings == null) {
            classMappings = new HashMap<String, String>();
            mappings.put(className, classMappings);
        }
        classMappings.put(reference, newReference);
    }
    
    public void write(Appendable writer) {
        new GsonBuilder().setPrettyPrinting().create().toJson(this, writer);
    }
    
    public static ReferenceMapper read(String resource) {
        Reader reader = null;
        try {
            reader = new InputStreamReader(Launch.classLoader.getResourceAsStream(resource));
            return ReferenceMapper.read(reader);
        } catch (Exception ex) {
            return ReferenceMapper.DEFAULT_MAPPER;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    // don't really care
                }
            }
        }
    }
    
    public static ReferenceMapper read(Reader reader) {
        return new Gson().fromJson(reader, ReferenceMapper.class);
    }
}
