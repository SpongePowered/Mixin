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
import java.util.Map;
import java.util.jar.Manifest;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.jetbrains.java.decompiler.util.InterpreterUtil;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

/**
 * Wrapper for FernFlower to support runtime-decompilation of post-mixin classes
 */
public class RuntimeDecompiler extends IFernflowerLogger implements IDecompiler, IResultSaver {
    
    private final Map<String, Object> options = ImmutableMap.<String, Object>builder()
        .put("din", "0").put("rbr", "0").put("dgs", "1").put("asc", "1")
        .put("den", "1").put("hdc", "1").put("ind", "    ").build();

    private final File outputPath;

    private final Logger logger = LogManager.getLogger("fernflower");

    public RuntimeDecompiler(File outputPath) {
        this.outputPath = outputPath;
        if (this.outputPath.exists()) {
            try {
                FileUtils.deleteDirectory(this.outputPath);
            } catch (IOException ex) {
                this.logger.warn("Error cleaning output directory", ex);
            }
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
            
            fernflower.getStructContext().addSpace(file, true);
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
        this.logger.info(message);
    }

    @Override
    public void writeMessage(String message, Throwable t) {
        this.logger.info(message, t);
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
