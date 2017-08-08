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
package org.spongepowered.asm.mixin.refmap;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

/**
 * This adapter is designed to address a problem with mixins when "deobfCompile"
 * dependencies are used in a project which is using newer MCP mappings than the
 * ones the imported dependency was compiled with.
 * 
 * <p>Before now, refMaps in deobfCompile dependencies had to be disabled
 * because the obfuscated mappings were no use in a development environment.
 * However there existed no "mcp-to-different-version-of-mcp" mappings to use
 * instead.</p>
 * 
 * <p>This class leverages the fact that mappings are provided into the
 * environment by GradleStart and consumes SRG mappings in the imported refMaps
 * and converts them on-the-fly using srg-to-mcp mappings. This allows refMaps
 * to be normalised to the current MCP environment.</p>
 * 
 * <p>Note that this class takes a naïve approach to remapping on the basis that
 * searge names are unique and can thus be remapped with a straightforward dumb
 * string replacement. Whilst the input environment and mappings are
 * customisable via the appropriate environment vars, this fact should be taken
 * into account if a different mapping environment is to be used.</p>
 * 
 * <p>All lookups are straightforward string replacements using <em>all</em>
 * values in the map, this basically means this is probably pretty slow, but I
 * don't care because the number of mappings processed is usually pretty small
 * (few hundred at most) and this is only used in dev where we don't actually
 * care about speed. Some performance is gained (approx 10ms per lookup) by
 * caching the transformed descriptors.</p>
 */
public final class RemappingReferenceMapper implements IReferenceMapper {
    
    /**
     * This is the default GradleStart-injected system property which tells us
     * the location of the srg-to-mcp mappings
     */
    private static final String DEFAULT_RESOURCE_PATH_PROPERTY = "net.minecraftforge.gradle.GradleStart.srg.srg-mcp";
    
    /**
     * The default environment name to map <em>from</em> 
     */
    private static final String DEFAULT_MAPPING_ENV = "searge";
    
    /**
     * Logger
     */
    private static final Logger logger = LogManager.getLogger("mixin");

    /**
     * Loaded srgs, stored as a mapping of filename to mappings. Global cache so
     * that we only need to load each mapping file once.
     */
    private static final Map<String, Map<String, String>> srgs = new HashMap<String, Map<String,String>>();
    
    /**
     * The "inner" refmap, this is the original refmap specified in the config
     */
    private final IReferenceMapper refMap;
    
    /**
     * The loaded mappings, retrieved from {@link #srgs} by filename
     */
    private final Map<String, String> mappings;
    
    /**
     * Cache of transformed mappings. Each transformation takes about 10ms so
     * this saves us time when the same mapping is used repeatedly 
     */
    private final Map<String, Map<String, String>> cache = new HashMap<String, Map<String, String>>();
    
    private RemappingReferenceMapper(MixinEnvironment env, IReferenceMapper refMap) {
        this.refMap = refMap;
        this.refMap.setContext(RemappingReferenceMapper.getMappingEnv(env));
        
        String resource = RemappingReferenceMapper.getResource(env);
        this.mappings = RemappingReferenceMapper.loadSrgs(resource);

        RemappingReferenceMapper.logger.info("Remapping refMap {} using {}", refMap.getResourceName(), resource);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.refmap.IReferenceMapper#isDefault()
     */
    @Override
    public boolean isDefault() {
        return this.refMap.isDefault();
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.refmap.IReferenceMapper
     *      #getResourceName()
     */
    @Override
    public String getResourceName() {
        return this.refMap.getResourceName();
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.refmap.IReferenceMapper#getStatus()
     */
    @Override
    public String getStatus() {
        return this.refMap.getStatus();
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.refmap.IReferenceMapper#getContext()
     */
    @Override
    public String getContext() {
        return this.refMap.getContext();
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.refmap.IReferenceMapper#setContext(
     *      java.lang.String)
     */
    @Override
    public void setContext(String context) {
        // Nope
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.refmap.IReferenceMapper#remap(
     *      java.lang.String, java.lang.String)
     */
    @Override
    public String remap(String className, String reference) {
        Map<String, String> classCache = this.getCache(className);
        String remapped = classCache.get(reference);
        if (remapped == null) {
            remapped = this.refMap.remap(className, reference);
            for (Entry<String, String> entry : this.mappings.entrySet()) {
                remapped = remapped.replace(entry.getKey(), entry.getValue());
            }
            classCache.put(reference, remapped);
        }
        return remapped;
    }

    private Map<String, String> getCache(String className) {
        Map<String, String> classCache = this.cache.get(className);
        if (classCache == null) {
            classCache = new HashMap<String, String>();
            this.cache.put(className, classCache);
        }
        return classCache;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.refmap.IReferenceMapper
     *      #remapWithContext(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    @Override
    public String remapWithContext(String context, String className, String reference) {
        return this.refMap.remapWithContext(context, className, reference);
    }

    /**
     * Read srgs from the specified file resource. The mappings are cached
     * internally so this will only read the file the first time it is called
     * with a particulare filename.
     * 
     * @param fileName srg file to read
     * @return srgs read from file or empty map if the file could not be read
     */
    private static Map<String, String> loadSrgs(String fileName) {
        if (RemappingReferenceMapper.srgs.containsKey(fileName)) {
            return RemappingReferenceMapper.srgs.get(fileName);
        }
        
        final Map<String, String> map = new HashMap<String, String>();
        RemappingReferenceMapper.srgs.put(fileName, map);

        File file = new File(fileName);
        if (!file.isFile()) {
            return map;
        }
                
        try {
            Files.readLines(file, Charsets.UTF_8, new LineProcessor<Object>() {
                
                @Override
                public Object getResult() {
                    return null;
                }

                @Override
                public boolean processLine(String line) throws IOException {
                    if (Strings.isNullOrEmpty(line) || line.startsWith("#")) {
                        return true;
                    }
                    int fromPos = 0, toPos = 0;
                    if ((toPos = line.startsWith("MD: ") ? 2 : line.startsWith("FD: ") ? 1 : 0) > 0) {
                        String[] entries = line.substring(4).split(" ", 4);
                        map.put(
                            entries[fromPos].substring(entries[fromPos].lastIndexOf('/') + 1),
                            entries[toPos].substring(entries[toPos].lastIndexOf('/') + 1)
                        );
                    }
                    return true;
                }
            });
        } catch (IOException ex) {
            RemappingReferenceMapper.logger.warn("Could not read input SRG file: {}", fileName);
            RemappingReferenceMapper.logger.catching(ex);
        }
        
        return map;
    }
    
    /**
     * Wrap the specified refmap in a remapping adapter using settings in the
     * supplied environment
     * 
     * @param env environment to read configuration from
     * @param refMap refmap to wrap
     * @return wrapped refmap or original refmap is srg data is not available
     */
    public static IReferenceMapper of(MixinEnvironment env, IReferenceMapper refMap) {
        if (!refMap.isDefault() && RemappingReferenceMapper.hasData(env)) {
            return new RemappingReferenceMapper(env, refMap);
        }
        return refMap;
    }

    private static boolean hasData(MixinEnvironment env) {
        String fileName = RemappingReferenceMapper.getResource(env);
        return fileName != null && new File(fileName).exists();
    }

    private static String getResource(MixinEnvironment env) {
        String resource = env.getOptionValue(Option.REFMAP_REMAP_RESOURCE);
        return Strings.isNullOrEmpty(resource) ? System.getProperty(RemappingReferenceMapper.DEFAULT_RESOURCE_PATH_PROPERTY) : resource;
    }

    private static String getMappingEnv(MixinEnvironment env) {
        String resource = env.getOptionValue(Option.REFMAP_REMAP_SOURCE_ENV);
        return Strings.isNullOrEmpty(resource) ? RemappingReferenceMapper.DEFAULT_MAPPING_ENV : resource;
    }

}
