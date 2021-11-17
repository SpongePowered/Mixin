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
package org.spongepowered.asm.mixin.transformer.debug;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;
import java.util.jar.Manifest;

import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.logging.ILogger;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.jetbrains.java.decompiler.util.InterpreterUtil;
import org.spongepowered.asm.mixin.transformer.ext.IDecompiler;
import org.spongepowered.asm.service.MixinService;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;

/**
 * Wrapper for FernFlower to support runtime-decompilation of post-mixin classes
 */
public class RuntimeDecompiler extends IFernflowerLogger implements IDecompiler, IResultSaver {
    
    private static final Level[] SEVERITY_LEVELS = { Level.TRACE, Level.INFO, Level.WARN, Level.ERROR };
    
    private final Map<String, Object> options = ImmutableMap.<String, Object>builder()
            .put(IFernflowerPreferences.DECOMPILE_INNER,              "0")
            .put(IFernflowerPreferences.REMOVE_BRIDGE,                "0")
            .put(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, "1")
            .put(IFernflowerPreferences.ASCII_STRING_CHARACTERS,      "1")
            .put(IFernflowerPreferences.DECOMPILE_ENUM,               "1")
            .put(IFernflowerPreferences.HIDE_DEFAULT_CONSTRUCTOR,     "1")
            .put(IFernflowerPreferences.INDENT_STRING,                "    ")
            .build();

    private final File outputPath;

    protected final ILogger logger = MixinService.getService().getLogger("fernflower");

    public RuntimeDecompiler(File outputPath) {
        this.outputPath = outputPath;
        if (this.outputPath.exists()) {
            try {
                MoreFiles.deleteRecursively(this.outputPath.toPath(), RecursiveDeleteOption.ALLOW_INSECURE);
            } catch (IOException ex) {
                this.logger.debug("Error cleaning output directory: {}", ex.getMessage());
            }
        }
    }
    
    @Override
    public String toString() {
        try {
            URL codeSource = Fernflower.class.getProtectionDomain().getCodeSource().getLocation();
            File file = org.spongepowered.asm.util.Files.toFile(codeSource);
            return file.getName();
        } catch (Exception ex) {
            return "unknown source (classpath)";
        }
    }

    @Override
    public void decompile(final File file) {
        try {
            Fernflower fernflower = new Fernflower(new IBytecodeProvider() {
                
                private byte[] byteCode;
                
                @Override
                public byte[] getBytecode(String externalPath, String internalPath) throws IOException {
                    if (this.byteCode == null) {
                        this.byteCode = InterpreterUtil.getBytes(new File(externalPath));
                    }
                    return this.byteCode;
                }
                
            }, this, this.options, this);
            
            try {
                // New fernflower (including forgeflower)
                Method mdAddSource = fernflower.getClass().getDeclaredMethod("addSource", File.class);
                mdAddSource.invoke(fernflower, file);
            } catch (ReflectiveOperationException ex) {
                // Old fernflower
                fernflower.getStructContext().addSpace(file, true);
            }
            
            fernflower.decompileContext();
        } catch (Throwable ex) {
            this.logger.warn("Decompilation error while processing {}", file.getName());
        }
    }

    @Override
    public void saveFolder(String path) {
    }

    @Override
    public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
        File file = new File(this.outputPath, qualifiedName + ".java");
        file.getParentFile().mkdirs();
        try {
            this.logger.info("Writing {}", file.getAbsolutePath());
            Files.write(content, file, Charsets.UTF_8);
        } catch (IOException ex) {
            this.writeMessage("Cannot write source file " + file, ex);
        }
    }
    
    @Override
    public void startReadingClass(String className) {
        this.logger.info("Decompiling {}", className);
    }

    @Override
    public void writeMessage(String message, Severity severity) {
        this.logger.log(RuntimeDecompiler.SEVERITY_LEVELS[severity.ordinal()], message);
    }

    @Override
    public void writeMessage(String message, Throwable t) {
        this.logger.warn("{} {}: {}", message, t.getClass().getSimpleName(), t.getMessage());
    }
    
    @Override
    public void writeMessage(String message, Severity severity, Throwable t) {
        this.logger.log(RuntimeDecompiler.SEVERITY_LEVELS[severity.ordinal()], message, severity == Severity.ERROR ? t : null);
    }

    @Override
    public void copyFile(String source, String path, String entryName) {
    }

    @Override
    public void createArchive(String path, String archiveName, Manifest manifest) {
    }

    @Override
    public void saveDirEntry(String path, String archiveName, String entryName) {
    }

    @Override
    public void copyEntry(String source, String path, String archiveName, String entry) {
    }

    @Override
    public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
    }

    @Override
    public void closeArchive(String path, String archiveName) {
    }

}
