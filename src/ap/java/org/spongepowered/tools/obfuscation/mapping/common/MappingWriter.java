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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.tools.Diagnostic.Kind;

import org.spongepowered.tools.obfuscation.mapping.IMappingWriter;

/**
 * Writer for mappings
 */
public abstract class MappingWriter implements IMappingWriter {
    
    private final Messager messager;
    
    private final Filer filer;

    public MappingWriter(Messager messager, Filer filer) {
        this.messager = messager;
        this.filer = filer;
    }

    /**
     * Open a writer for an output file
     * 
     * @param fileName output file name (resource path)
     * @param description description of file being requested, for logging
     * @return new PrintWriter for opened resource
     * @throws IOException if the resource cannot be opened
     */
    protected PrintWriter openFileWriter(String fileName, String description) throws IOException {
        if (fileName.matches("^.*[\\\\/:].*$")) {
            File outFile = new File(fileName);
            outFile.getParentFile().mkdirs();
            this.messager.printMessage(Kind.NOTE, "Writing " + description + " to " + outFile.getAbsolutePath());
            return new PrintWriter(outFile);
        }
        
        FileObject outResource = this.filer.createResource(StandardLocation.CLASS_OUTPUT, "", fileName);
        this.messager.printMessage(Kind.NOTE, "Writing " + description + " to " + new File(outResource.toUri()).getAbsolutePath());
        return new PrintWriter(outResource.openWriter());
    }

}
