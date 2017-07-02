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
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.despector.Language;
import org.spongepowered.despector.ast.SourceSet;
import org.spongepowered.despector.ast.type.TypeEntry;
import org.spongepowered.despector.config.LibraryConfiguration;
import org.spongepowered.despector.decompiler.Decompiler;
import org.spongepowered.despector.decompiler.Decompilers;
import org.spongepowered.despector.emitter.Emitter;
import org.spongepowered.despector.emitter.Emitters;
import org.spongepowered.despector.emitter.format.EmitterFormat;
import org.spongepowered.despector.emitter.java.JavaEmitterContext;

/**
 * Host for Despector to support runtime-decompilation of post-mixin classes
 */
public class RuntimeDecompilerDespector implements IDecompiler {
    
    private final Decompiler decompiler;
    private final Emitter<JavaEmitterContext> emitter;
    private final EmitterFormat formatter;
    private final SourceSet source;

    private final File outputPath;

    protected final Logger logger = LogManager.getLogger("despector");

    public RuntimeDecompilerDespector(File outputPath) {
        this.outputPath = outputPath;
        if (this.outputPath.exists()) {
            try {
                FileUtils.deleteDirectory(this.outputPath);
            } catch (IOException ex) {
                this.logger.warn("Error cleaning output directory: {}", ex.getMessage());
            }
        }
        
        this.decompiler = Decompilers.get(Language.JAVA);
        this.source = new SourceSet();
        this.emitter = Emitters.<JavaEmitterContext>get(Language.JAVA);
        this.formatter = EmitterFormat.defaults();
        
        LibraryConfiguration.parallel = false;
    }

    @Override
    public void decompile(final File file) {
        try {
            TypeEntry type = this.decompiler.decompile(file, this.source);
            File out = new File(this.outputPath, type.getName() + Language.JAVA.getExtension(type));
            File outDir = out.getParentFile();
            if (!outDir.isDirectory()) {
                outDir.mkdirs();
            }
            FileWriter writer = null;
            try {
                writer = new FileWriter(out); 
                this.emitter.emit(new JavaEmitterContext(writer, this.formatter), type);
                writer.flush();
            } catch (Exception ex) {
//                ex.printStackTrace();
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        } catch (Throwable ex) {
            this.logger.warn("Decompilation error while processing {}", file.getName());
        }
    }
}
