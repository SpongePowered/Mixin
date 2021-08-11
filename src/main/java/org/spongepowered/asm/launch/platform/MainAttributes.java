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
package org.spongepowered.asm.launch.platform;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.spongepowered.asm.util.JavaVersion;

import com.google.common.io.ByteSource;

/**
 * "Main" attribute cache for a URI container, mainly to avoid constantly
 * opening jar files just to read odd values out of the manifest.
 */
public final class MainAttributes {
    
    private static final Map<URI, MainAttributes> instances = new HashMap<URI, MainAttributes>();
    
    /**
     * Manifest from jar
     */
    protected final Attributes attributes;
    
    private MainAttributes() {
        this.attributes = new Attributes();
    }

    private MainAttributes(URI codeSource) {
        this.attributes = MainAttributes.getAttributes(codeSource);
    }

    /**
     * Retrieve the value of attribute with the specified name, or null if not
     * present
     * 
     * @param name attribute name
     * @return attribute value or null if not present
     */
    public final String get(String name) {
        if (this.attributes != null) {
            return this.attributes.getValue(name);
        }
        return null;
    }
    
    /**
     * Retrieve the value of attribute with the specified name, or null if not
     * present
     * 
     * @param name attribute name
     * @return attribute value or null if not present
     */
    public final String get(Name name) {
        if (this.attributes != null) {
            return this.attributes.getValue(name);
        }
        return null;
    }
    
    private static Attributes getAttributes(URI codeSource) {
        if (codeSource == null) {
            return null;
        }
        
        if ("file".equals(codeSource.getScheme())) {
            File file = new File(codeSource);
            
            if (file.isFile()) {
                Attributes attributes = MainAttributes.getJarAttributes(file);
                if (attributes != null) {
                    return attributes;
                }
            } else if (file.isDirectory()) {
                Attributes attributes = MainAttributes.getDirAttributes(file);
                if (attributes != null) {
                    return attributes;
                }
            }
        } else if (JavaVersion.current() >= JavaVersion.JAVA_7) {
            // Can try getting the resource using nio if we are on Java 7 or later
            Attributes attributes = MainAttributes.getNioAttributes(codeSource);
            if (attributes != null) {
                return attributes;
            }
        }
        
        return new Attributes();
    }

    private static Attributes getJarAttributes(File jar) {
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(jar);
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                return manifest.getMainAttributes();
            }
        } catch (IOException ex) {
            // be quiet checkstyle
        } finally {
            try {
                if (jarFile != null) {
                    jarFile.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
        return null;
    }
    
    private static Attributes getDirAttributes(File dir) {
        File manifestFile = new File(dir, JarFile.MANIFEST_NAME);
        if (manifestFile.isFile()) {
            ByteSource source = com.google.common.io.Files.asByteSource(manifestFile);
            InputStream inputStream = null;
            try {
                inputStream = source.openBufferedStream();
                Manifest manifest = new Manifest(inputStream);
                return manifest.getMainAttributes();
            } catch (IOException ex) {
                // be quiet checkstyle
            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        
        return null;
    }
    
    private static Attributes getNioAttributes(URI uri) {
        try {
            Path manifestPath = Paths.get(uri).resolve(JarFile.MANIFEST_NAME);
            BufferedInputStream inputStream = null;
            try {
                inputStream = new BufferedInputStream(java.nio.file.Files.newInputStream(manifestPath));
                Manifest manifest = new Manifest(inputStream);
                return manifest.getMainAttributes();
            } catch (IOException ex) {
                // be quiet checkstyle
            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    // ignore
                }
            }
        } catch (FileSystemNotFoundException ex) {
            // No FS available?
            ex.printStackTrace();
        } catch (InvalidPathException ex) {
            ex.printStackTrace();
        }
        
        return null;
    }

    /**
     * Create a MainAttributes instance for the supplied jar file
     * 
     * @param jar jar file
     * @return MainAttributes instance
     */
    public static MainAttributes of(File jar) {
        return MainAttributes.of(jar.toURI());
    }

    /**
     * Create a MainAttributes instance for the supplied jar file
     * 
     * @param uri jar file location
     * @return MainAttributes instance
     */
    public static MainAttributes of(URI uri) {
        MainAttributes attributes = MainAttributes.instances.get(uri);
        if (attributes == null) {
            attributes = new MainAttributes(uri);
            MainAttributes.instances.put(uri, attributes);
        }
        return attributes;
    }
}
