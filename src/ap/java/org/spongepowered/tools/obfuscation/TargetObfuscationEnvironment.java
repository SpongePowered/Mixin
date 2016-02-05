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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.injection.struct.ReferenceMapper;
import org.spongepowered.asm.obfuscation.SrgMethod;
import org.spongepowered.asm.obfuscation.SrgContainer;
import org.spongepowered.asm.obfuscation.SrgField;
import org.spongepowered.asm.util.ObfuscationUtil;
import org.spongepowered.asm.util.ObfuscationUtil.IClassRemapper;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;

/**
 * Stores information relevant to a particular target obfuscation environment.
 * 
 * <p>We classify different types of possible obfuscation (eg. "searge", "notch"
 * ) as <em>obfuscation environments</em> and store related information such as 
 * the input SRG mappings, generated refmap, generated SRGs, here.</p>
 */
class TargetObfuscationEnvironment {
    
    /**
     * Remapping proxy for remapping descriptors
     */
    final class RemapperProxy implements IClassRemapper {

        @Override
        public String map(String typeName) {
            if (TargetObfuscationEnvironment.this.srgs == null) {
                return null;
            }
            return TargetObfuscationEnvironment.this.srgs.getClassMapping(typeName);
        }

        @Override
        public String unmap(String typeName) {
            if (TargetObfuscationEnvironment.this.srgs == null) {
                return null;
            }
            return TargetObfuscationEnvironment.this.srgs.getClassMapping(typeName);
        }
        
    }
    
    /**
     * SRG container for mcp->? mappings
     */
    protected SrgContainer srgs;
    
    private final RemapperProxy remapper = new RemapperProxy();
    
    /**
     * Type 
     */
    private final ObfuscationType type;

    /**
     * Annotation processor
     */
    private final IMixinAnnotationProcessor ap;

    /**
     * Name of the resource to write generated srgs to
     */
    private final String outSrgFileName;
    
    /**
     * File containing the reobfd srgs
     */
    private final List<String> reobfSrgFileNames;
    
    /**
     * Reference mapper for reference mapping 
     */
    private final ReferenceMapper refMapper;
    
    /**
     * True once we've tried to initialise the srgs, initially false so that we
     * can do srg init lazily
     */
    private boolean initDone;

    public TargetObfuscationEnvironment(IMixinAnnotationProcessor ap, ObfuscationType type, ReferenceMapper refMapper) {
        this.ap = ap;
        this.type = type;
        this.refMapper = refMapper;
        
        this.reobfSrgFileNames = type.getSrgFileNames(ap);
        this.outSrgFileName = type.getOutputSrgFileName(ap);
    }
    
    private boolean initSrgs() {
        if (!this.initDone) {
            this.initDone = true;
        
            if (this.reobfSrgFileNames == null) {
                this.ap.printMessage(Kind.ERROR, "The " + this.type.getSrgFileOption()
                    + " argument was not supplied, obfuscation processing will not occur");
                return false;
            }
            
            int successCount = 0;
            this.srgs = new SrgContainer();
            
            for (String srgFileName : this.reobfSrgFileNames) {
                File reobfSrgFile = new File(srgFileName);
                try {
                    if (reobfSrgFile.isFile()) {
                        this.ap.printMessage(Kind.NOTE, "Loading " + this.type + " mappings from " + reobfSrgFile.getAbsolutePath());
                        this.srgs.readSrg(reobfSrgFile);
                        successCount++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            
            if (successCount < 1) {
                this.ap.printMessage(Kind.ERROR, "No valid SRG files for " + this.type + " could be read, processing may not be sucessful.");
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
        if (this.type.isDefault(this.ap)) {
            this.refMapper.addMapping(null, className, reference, newReference);
        }
    }

    /**
     * Get an obfuscation mapping for a method
     */
    public SrgMethod getObfMethod(MemberInfo method) {
        SrgMethod obfd = this.getObfMethod(method.asSrgMethod());
        if (obfd != null || !method.isFullyQualified()) {
            return obfd;
        }
        
        // Get a type handle for the declared method owner
        TypeHandle type = this.ap.getTypeProvider().getTypeHandle(method.owner);
        if (type == null || type.isImaginary()) {
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
    public SrgMethod getObfMethod(SrgMethod method) {
        return this.getObfMethod(method, true);
    }

    /**
     * Get an obfuscation mapping for a method
     */
    public SrgMethod getObfMethod(SrgMethod method, boolean srgOnly) {
        if (this.initSrgs()) {
            boolean remapped = true;
            SrgMethod originalMethod = method.copy();
            SrgMethod methodMapping = this.srgs.getMethodMapping(method);
            // If no obf mapping, we can attempt to remap the owner class
            if (methodMapping == null) {
                if (srgOnly) {
                    return null;
                }
                methodMapping = originalMethod;
                remapped = false;
            }
            String remappedOwner = this.getObfClass(methodMapping.getOwner());
            if (remappedOwner == null || remappedOwner.equals(method.getOwner()) || remappedOwner.equals(methodMapping.getOwner())) {
                return remapped ? methodMapping : null;
            }
            if (remapped) {
                return methodMapping.move(remappedOwner);
            }
            return new SrgMethod(remappedOwner, methodMapping.getSimpleName(), ObfuscationUtil.mapDescriptor(methodMapping.getDesc(), this.remapper));
        }
        return null;
    }

    /**
     * Remap only the owner and descriptor of the specified method
     * 
     * @param method method to remap
     * @return remapped method or null if no remapping occurred
     */
    public MemberInfo remapDescriptor(MemberInfo method) {
        boolean transformed = false;
        
        String owner = method.owner;
        if (owner != null) {
            String newOwner = this.remapper.map(owner);
            if (newOwner != null) {
                owner = newOwner;
                transformed = true;
            }
        }
        
        String desc = method.desc;
        if (desc != null) {
            String newDesc = ObfuscationUtil.mapDescriptor(method.desc, this.remapper);
            if (!newDesc.equals(method.desc)) {
                desc = newDesc;
                transformed = true;
            }
        }
        
        return transformed ? new MemberInfo(method.name, owner, desc, method.matchAll) : null; 
    }
    
    /**
     * Get an obfuscation mapping for a field
     */
    public SrgField getObfField(String field) {
        return this.getObfField(field, true);
    }

    /**
     * Get an obfuscation mapping for a field
     */
    public SrgField getObfField(String field, boolean srgOnly) {
        if (this.initSrgs()) {
            SrgField originalField = new SrgField(field);
            SrgField fieldMapping = this.srgs.getFieldMapping(originalField);
            // If no obf mapping, we can attempt to remap the owner class
            if (fieldMapping == null) {
                if (srgOnly) {
                    return null;
                }
                fieldMapping = originalField;
            }
            String remappedOwner = this.getObfClass(fieldMapping.getOwner());
            if (remappedOwner == null || remappedOwner.equals(originalField.getOwner()) || remappedOwner.equals(fieldMapping.getOwner())) {
                return fieldMapping != originalField ? fieldMapping : null;
            }
            return fieldMapping.move(remappedOwner);
        }
        return null;
    }
    
    /**
     * Get an obfuscation mapping for a class
     */
    public String getObfClass(String className) {
        if (this.initSrgs()) {
            return this.srgs.getClassMapping(className);
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
            this.ap.printMessage(Kind.NOTE, "Writing " + description + " to " + outSrgFile.getAbsolutePath());
            return new PrintWriter(outSrgFile);
        }
        
        FileObject outSrg = filer.createResource(StandardLocation.CLASS_OUTPUT, "", fileName);
        this.ap.printMessage(Kind.NOTE, "Writing " + description + " to " + new File(outSrg.toUri()).getAbsolutePath());
        return new PrintWriter(outSrg.openWriter());
    }

}
