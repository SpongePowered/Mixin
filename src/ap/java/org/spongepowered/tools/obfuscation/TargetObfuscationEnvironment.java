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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.tools.Diagnostic.Kind;

import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.injection.struct.ReferenceMapper;

import net.minecraftforge.srg2source.rangeapplier.MethodData;
import net.minecraftforge.srg2source.rangeapplier.SrgContainer;

class TargetObfuscationEnvironment {
    
    /**
     * Type 
     */
    private final ObfuscationType type;

    /**
     * Messager
     */
    private final Messager messager;
    
    /**
     * Type handle provider
     */
    private final ITypeHandleProvider typeProvider;

    /**
     * Options
     */
    private final IOptionProvider options;

    /**
     * Name of the resource to write generated srgs to
     */
    private final String outSrgFileName;
    
    /**
     * File containing the reobfd srgs
     */
    private final String reobfSrgFileName;
    
    /**
     * Reference mapper for reference mapping 
     */
    private final ReferenceMapper refMapper;
    
    /**
     * SRG container for mcp->? mappings
     */
    private SrgContainer srgs;
    
    /**
     * True once we've tried to initialise the srgs, initially false so that we
     * can do srg init lazily
     */
    private boolean initDone;

    public TargetObfuscationEnvironment(ObfuscationType type, Messager messager, ITypeHandleProvider typeProvider, IOptionProvider options,
            ReferenceMapper refMapper) {
        this.type = type;
        this.messager = messager;
        this.typeProvider = typeProvider;
        this.options = options;
        this.refMapper = refMapper;
        
        this.reobfSrgFileName = type.getSrgFileName(options);
        this.outSrgFileName = type.getOutputSrgFileName(options);
    }
    
    private boolean initSrgs() {
        if (!this.initDone) {
            this.initDone = true;
        
            if (this.reobfSrgFileName == null) {
                this.messager.printMessage(Kind.ERROR, "The " + this.type.getSrgFileOption()
                    + " argument was not supplied, obfuscation processing will not occur");
                return false;
            }
            
            try {
                File reobfSrgFile = new File(this.reobfSrgFileName);
                this.messager.printMessage(Kind.NOTE, "Loading " + this.type + " mappings from " + reobfSrgFile.getAbsolutePath());
                this.srgs = new SrgContainer().readSrg(reobfSrgFile);
            } catch (Exception ex) {
                ex.printStackTrace();
                this.messager.printMessage(Kind.ERROR, "The specified " + this.type + " SRG file could not be read, processing cannot continue");
                this.srgs = null;
            }
        }
        
        return this.srgs != null;
    }

    /**
     * Get the type
     */
    public ObfuscationType getType() {
        return this.type;
    }

    public void addMapping(String className, String reference, String newReference) {
        this.refMapper.addMapping(this.type.getKey(), className, reference, newReference);
        if (this.type.isDefault(this.options)) {
            this.refMapper.addMapping(null, className, reference, newReference);
        }
    }

    /**
     * Get an obfuscation mapping for a method
     */
    public MethodData getObfMethod(MemberInfo method) {
        MethodData obfd = this.getObfMethod(method.asMethodData());
        if (obfd != null || !method.isFullyQualified()) {
            return obfd;
        }
        
        // Get a type handle for the declared method owner
        TypeHandle type = this.typeProvider.getTypeHandle(method.owner);
        if (type.isImaginary()) {
            return null;
        }
        
        // See if we can get the superclass from the reference
        TypeMirror superClass = type.getElement().getSuperclass();
        if (superClass.getKind() != TypeKind.DECLARED) {
            return null;
        }
        
        // Well we found it, let's inflect the class name and recurse the search
        String superClassName = ((TypeElement)((DeclaredType)superClass).asElement()).getQualifiedName().toString();
        return this.getObfMethod(new MemberInfo(method.name, superClassName.replace('.', '/'), method.desc, method.matchAll));
    }

    /**
     * Get an obfuscation mapping for a method
     */
    public MethodData getObfMethod(MethodData method) {
        if (this.initSrgs()) {
            return this.srgs.methodMap.get(method);
        }
        return null;
    }

    /**
     * Get an obfuscation mapping for a field
     */
    public String getObfField(String field) {
        if (this.initSrgs()) {
            return this.srgs.fieldMap.get(field);
        }
        return null;
    }

    /**
     * Write out generated srgs
     */
    public void writeSrgs(Filer filer, Map<String, AnnotatedMixin> mixins) {
        if (this.outSrgFileName == null) {
            return;
        }
        
        Set<String> fieldMappings = new LinkedHashSet<String>();
        Set<String> methodMappings = new LinkedHashSet<String>();
        
        for (AnnotatedMixin mixin : mixins.values()) {
            fieldMappings.addAll(mixin.getFieldMappings(this.type));
            methodMappings.addAll(mixin.getMethodMappings(this.type));
        }
        
        PrintWriter writer = null;
        
        try {
            writer = this.openFileWriter(filer, this.outSrgFileName, this.type + " output SRGs");
            for (String fieldMapping : fieldMappings) {
                writer.println(fieldMapping);
            }
            for (String methodMapping : methodMappings) {
                writer.println(methodMapping);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ex) {
                    // oh well
                }
            }
        }
    }
    
    /**
     * Open a writer for an output file
     */
    private PrintWriter openFileWriter(Filer filer, String fileName, String description) throws IOException {
        if (fileName.matches("^.*[\\\\/:].*$")) {
            File outSrgFile = new File(fileName);
            outSrgFile.getParentFile().mkdirs();
            this.messager.printMessage(Kind.NOTE, "Writing " + description + " to " + outSrgFile.getAbsolutePath());
            return new PrintWriter(outSrgFile);
        }
        
        FileObject outSrg = filer.createResource(StandardLocation.CLASS_OUTPUT, "", fileName);
        this.messager.printMessage(Kind.NOTE, "Writing " + description + " to " + new File(outSrg.toUri()).getAbsolutePath());
        return new PrintWriter(outSrg.openWriter());
    }

}
