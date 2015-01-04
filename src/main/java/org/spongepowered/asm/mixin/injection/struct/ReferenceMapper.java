/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered.org <http://www.spongepowered.org>
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class ReferenceMapper implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_RESOURCE = "mixin.refmap.json";

    private final Map<String, Map<String, String>> mappings;
    
    public ReferenceMapper() {
        this(new HashMap<String, Map<String, String>>());
    }

    public ReferenceMapper(Map<String, Map<String, String>> mappings) {
        this.mappings = mappings;
    }
    
    public String remap(String className, String reference) {
        if (className == null) {
            for (Map<String, String> mappings : this.mappings.values()) {
                if (mappings.containsKey(reference)) {
                    return mappings.get(reference);
                }
            }
        }
        
        Map<String, String> classMappings = this.mappings.get(className);
        if (classMappings == null) {
            return reference;
        }
        return classMappings.get(reference);
    }
    
    public void addMapping(String className, String reference, String newReference) {
        Map<String, String> classMappings = this.mappings.get(className);
        if (classMappings == null) {
            classMappings = new HashMap<String, String>();
            this.mappings.put(className, classMappings);
        }
        classMappings.put(reference, newReference);
    }
    
    public void write(Appendable writer) {
        new GsonBuilder().setPrettyPrinting().create().toJson(this, writer);
    }
    
    public static ReferenceMapper read(String resource) {
        Reader reader = null;
        try {
            reader = new InputStreamReader(ReferenceMapper.class.getResourceAsStream("/" + resource));
            return ReferenceMapper.read(reader);
        } catch (Exception ex) {
            return new ReferenceMapper();
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
