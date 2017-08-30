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
package org.spongepowered.asm.mixin.struct;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.LineNumberNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.util.Bytecode;

/**
 * Structure which contains information about a SourceDebugExtension SMAP
 */
public class SourceMap {
    
    private static final String DEFAULT_STRATUM = "Mixin";
    private static final String NEWLINE = "\n";
    
    /**
     * Defines a source code file within a source map stratum
     */
    public static class File {
        
        /**
         * File index in stratum 
         */
        public final int id;
        
        /**
         * The base line offset for this stratum, line numbers in the output
         * will be offset by this amount from their originals
         */
        public final int lineOffset;
        
        /**
         * The size of this stratum (number of lines)
         */
        public final int size;
        
        /**
         * Actual source file name to include in the smap
         */
        public final String sourceFileName;

        /**
         * Full path to the source file 
         */
        public final String sourceFilePath;
        
        /**
         * Create a new SMAP Stratum
         * 
         * @param lineOffset line offset
         * @param size total lines
         * @param sourceFileName source file name
         */
        public File(int id, int lineOffset, int size, String sourceFileName) {
            this(id, lineOffset, size, sourceFileName, null);
        }
        
        /**
         * Create a new SMAP Stratum
         * 
         * @param lineOffset line offset
         * @param size total lines
         * @param sourceFileName source file name
         * @param sourceFilePath source path
         */
        public File(int id, int lineOffset, int size, String sourceFileName, String sourceFilePath) {
            this.id = id;
            this.lineOffset = lineOffset;
            this.size = size;
            this.sourceFileName = sourceFileName;
            this.sourceFilePath = sourceFilePath;
        }
        
        /**
         * Offset the line numbers in the target class node by the base
         * lineoffset for this stratum
         * 
         * @param classNode class to operate upon
         */
        public void applyOffset(ClassNode classNode) {
            for (MethodNode method : classNode.methods) {
                this.applyOffset(method);
            }
        }

        /**
         * Offset the line numbers in the target method node by the base
         * lineoffset for this stratum
         * 
         * @param method method to operate upon
         */
        public void applyOffset(MethodNode method) {
            for (Iterator<AbstractInsnNode> iter = method.instructions.iterator(); iter.hasNext();) {
                AbstractInsnNode node = iter.next();
                if (node instanceof LineNumberNode) {
                    ((LineNumberNode)node).line += this.lineOffset - 1;
                }
            }
        }

        void appendFile(StringBuilder sb) {
            if (this.sourceFilePath != null) {
                sb.append("+ ").append(this.id).append(" ").append(this.sourceFileName).append(SourceMap.NEWLINE);
                sb.append(this.sourceFilePath).append(SourceMap.NEWLINE);
            } else {
                sb.append(this.id).append(" ").append(this.sourceFileName).append(SourceMap.NEWLINE);
            }
        }

        /**
         * Append lines representing this File to the supplied StringBuilder
         * 
         * @param sb StringBuilder to append to
         */
        public void appendLines(StringBuilder sb) {
            sb.append("1#").append(this.id)         // Map line number 1 (onwards) in file number <index>
              .append(",").append(this.size)        // repeated <file.size> times (eg. lines 1 to <file.size + 1>)
              .append(":").append(this.lineOffset)  // to output line number lineOffset in the output file
              .append(SourceMap.NEWLINE);
        }
        
    }
    
    /**
     * Defines a source code stratum within the source map
     */
    static class Stratum {
        
        private static final String STRATUM_MARK = "*S";
        private static final String FILE_MARK = "*F";
        private static final String LINES_MARK = "*L";

        /**
         * Stratum name 
         */
        public final String name;
        
        private final Map<String, File> files = new LinkedHashMap<String, File>();
        
        public Stratum(String name) {
            this.name = name;
        }

        public File addFile(int lineOffset, int size, String sourceFileName, String sourceFilePath) {
            File file = this.files.get(sourceFilePath);
            if (file == null) {
                file = new File(this.files.size() + 1, lineOffset, size, sourceFileName, sourceFilePath);
                this.files.put(sourceFilePath, file);
            }
            return file;
        }

        void appendTo(StringBuilder sb) {
            sb.append(Stratum.STRATUM_MARK).append(" ").append(this.name).append(SourceMap.NEWLINE);
            
            sb.append(Stratum.FILE_MARK).append(SourceMap.NEWLINE);
            for (File file : this.files.values()) {
                file.appendFile(sb);
            }
            
            sb.append(Stratum.LINES_MARK).append(SourceMap.NEWLINE);
            for (File file : this.files.values()) {
                file.appendLines(sb);
            }
        }
        
    }
    
    /**
     * Original source file name
     */
    private final String sourceFile;

    /**
     * SMAP strata
     */
    private final Map<String, Stratum> strata = new LinkedHashMap<String, Stratum>();
    
    private int nextLineOffset = 1;
    
    private String defaultStratum = SourceMap.DEFAULT_STRATUM;

    public SourceMap(String sourceFile) {
        this.sourceFile = sourceFile;
    }
    
    /**
     * Get the original source file
     */
    public String getSourceFile() {
        return this.sourceFile;
    }

    /**
     * Get the generated source file
     */
    public String getPseudoGeneratedSourceFile() {
        return this.sourceFile.replace(".java", "$mixin.java");
    }
    
    /**
     * Add a file to this SourceMap in the default stratum
     * 
     * @param classNode class node to read details from
     * @return new File
     */
    public File addFile(ClassNode classNode) {
        return this.addFile(this.defaultStratum, classNode);
    }
    
    /**
     * Add a file to this SourceMap in the specified stratum
     * 
     * @param stratumName name of the stratum to add to
     * @param classNode class node to read file details from
     * @return new File
     */
    public File addFile(String stratumName, ClassNode classNode) {
        return this.addFile(stratumName, classNode.sourceFile, classNode.name + ".java", Bytecode.getMaxLineNumber(classNode, 500, 50));
    }
    
    /**
     * Add a file to this SourceMap in the default stratum
     * 
     * @param sourceFileName source filename
     * @param sourceFilePath path to source file
     * @param size number of lines to allocate
     * @return new File
     */
    public File addFile(String sourceFileName, String sourceFilePath, int size) {
        return this.addFile(this.defaultStratum, sourceFileName, sourceFilePath, size);
    }
    
    /**
     * Add a file to this SourceMap in the specified stratum
     * 
     * @param stratumName name of the stratum to add to
     * @param sourceFileName source filename
     * @param sourceFilePath path to source file
     * @param size number of lines to allocate
     * @return new File
     */
    public File addFile(String stratumName, String sourceFileName, String sourceFilePath, int size) {
        Stratum stratum = this.strata.get(stratumName);
        if (stratum == null) {
            stratum = new Stratum(stratumName);
            this.strata.put(stratumName, stratum);
        }
        
        File file = stratum.addFile(this.nextLineOffset, size, sourceFileName, sourceFilePath);
        this.nextLineOffset += size;
        return file;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        this.appendTo(sb);
        return sb.toString();
    }

    private void appendTo(StringBuilder sb) {
        // SMAP header
        sb.append("SMAP").append(SourceMap.NEWLINE);
        
        // Source file
        sb.append(this.getSourceFile()).append(SourceMap.NEWLINE);
        
        // Default stratum
        sb.append(this.defaultStratum).append(SourceMap.NEWLINE);
        for (Stratum stratum : this.strata.values()) {
            stratum.appendTo(sb);
        }
        
        // End
        sb.append("*E").append(SourceMap.NEWLINE);
    }
    
}
