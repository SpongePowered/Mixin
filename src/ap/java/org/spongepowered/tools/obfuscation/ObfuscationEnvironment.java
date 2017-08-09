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
import java.util.Collection;
import java.util.List;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.asm.util.ObfuscationUtil;
import org.spongepowered.asm.util.ObfuscationUtil.IClassRemapper;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer;
import org.spongepowered.tools.obfuscation.mapping.IMappingProvider;
import org.spongepowered.tools.obfuscation.mapping.IMappingWriter;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer.MappingSet;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;
import org.spongepowered.tools.obfuscation.interfaces.IObfuscationEnvironment;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;

/**
 * Provides access to information relevant to a particular obfuscation
 * environment.
 * 
 * <p>We classify different types of possible obfuscation (eg. "searge",
 * "notch") as <em>obfuscation environments</em> and store related information
 * such as the input mappings here.</p>
 */
public abstract class ObfuscationEnvironment implements IObfuscationEnvironment {
    
    /**
     * Remapping proxy for remapping descriptors
     */
    final class RemapperProxy implements IClassRemapper {

        @Override
        public String map(String typeName) {
            if (ObfuscationEnvironment.this.mappingProvider == null) {
                return null;
            }
            return ObfuscationEnvironment.this.mappingProvider.getClassMapping(typeName);
        }

        @Override
        public String unmap(String typeName) {
            if (ObfuscationEnvironment.this.mappingProvider == null) {
                return null;
            }
            return ObfuscationEnvironment.this.mappingProvider.getClassMapping(typeName);
        }
        
    }
    
    /**
     * Type 
     */
    protected final ObfuscationType type;
    
    /**
     * Mapping provider
     */
    protected final IMappingProvider mappingProvider;
    
    protected final IMappingWriter mappingWriter;
    
    protected final RemapperProxy remapper = new RemapperProxy();

    /**
     * Annotation processor
     */
    protected final IMixinAnnotationProcessor ap;

    /**
     * Name of the resource to write generated mappings to
     */
    protected final String outFileName;
    
    /**
     * File containing the source mappings
     */
    protected final List<String> inFileNames;
    
    /**
     * True once we've tried to initialise the mappings, initially false so that
     * we can do mapping init lazily
     */
    private boolean initDone;

    protected ObfuscationEnvironment(ObfuscationType type) {
        this.type = type;
        this.ap = type.getAnnotationProcessor();
        
        this.inFileNames = type.getInputFileNames();
        this.outFileName = type.getOutputFileName();

        this.mappingProvider = this.getMappingProvider(this.ap, this.ap.getProcessingEnvironment().getFiler());
        this.mappingWriter = this.getMappingWriter(this.ap, this.ap.getProcessingEnvironment().getFiler());
    }
    
    @Override
    public String toString() {
        return this.type.toString();
    }
    
    protected abstract IMappingProvider getMappingProvider(Messager messager, Filer filer);
    
    protected abstract IMappingWriter getMappingWriter(Messager messager, Filer filer);
    
    private boolean initMappings() {
        if (!this.initDone) {
            this.initDone = true;
        
            if (this.inFileNames == null) {
                this.ap.printMessage(Kind.ERROR, "The " + this.type.getConfig().getInputFileOption()
                        + " argument was not supplied, obfuscation processing will not occur");
                return false;
            }
            
            int successCount = 0;
            
            for (String inputFileName : this.inFileNames) {
                File inputFile = new File(inputFileName);
                try {
                    if (inputFile.isFile()) {
                        this.ap.printMessage(Kind.NOTE, "Loading " + this.type + " mappings from " + inputFile.getAbsolutePath());
                        this.mappingProvider.read(inputFile);
                        successCount++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            
            if (successCount < 1) {
                this.ap.printMessage(Kind.ERROR, "No valid input files for " + this.type + " could be read, processing may not be sucessful.");
                this.mappingProvider.clear();
            }
        }
        
        return !this.mappingProvider.isEmpty();
    }

    /**
     * Get the type
     */
    public ObfuscationType getType() {
        return this.type;
    }

    /**
     * Get an obfuscation mapping for a method
     */
    @Override
    public MappingMethod getObfMethod(MemberInfo method) {
        MappingMethod obfd = this.getObfMethod(method.asMethodMapping());
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
    @Override
    public MappingMethod getObfMethod(MappingMethod method) {
        return this.getObfMethod(method, true);
    }

    /**
     * Get an obfuscation mapping for a method
     */
    @Override
    public MappingMethod getObfMethod(MappingMethod method, boolean lazyRemap) {
        if (this.initMappings()) {
            boolean remapped = true;
            MappingMethod mapping = null;
            for (MappingMethod md = method; md != null && mapping == null; md = md.getSuper()) {
                mapping = this.mappingProvider.getMethodMapping(md);
            }
            
            // If no obf mapping, we can attempt to remap the owner class
            if (mapping == null) {
                if (lazyRemap) {
                    return null;
                }
                mapping = method.copy();
                remapped = false;
            }
            String remappedOwner = this.getObfClass(mapping.getOwner());
            if (remappedOwner == null || remappedOwner.equals(method.getOwner()) || remappedOwner.equals(mapping.getOwner())) {
                return remapped ? mapping : null;
            }
            if (remapped) {
                return mapping.move(remappedOwner);
            }
            String desc = ObfuscationUtil.mapDescriptor(mapping.getDesc(), this.remapper);
            return new MappingMethod(remappedOwner, mapping.getSimpleName(), desc);
        }
        return null;
    }

    /**
     * Remap only the owner and descriptor of the specified method
     * 
     * @param method method to remap
     * @return remapped method or null if no remapping occurred
     */
    @Override
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
     * Remap a single descriptor in the context of this environment
     * 
     * @param desc descriptor to remap
     * @return remapped descriptor, may return the original descriptor if no
     *      remapping occurred
     */
    @Override
    public String remapDescriptor(String desc) {
        return ObfuscationUtil.mapDescriptor(desc, this.remapper);
    }
    
    /**
     * Get an obfuscation mapping for a field
     */
    @Override
    public MappingField getObfField(MemberInfo field) {
        return this.getObfField(field.asFieldMapping(), true);
    }
    
    /**
     * Get an obfuscation mapping for a field
     */
    @Override
    public MappingField getObfField(MappingField field) {
        return this.getObfField(field, true);
    }

    /**
     * Get an obfuscation mapping for a field
     */
    @Override
    public MappingField getObfField(MappingField field, boolean lazyRemap) {
        if (!this.initMappings()) {
            return null;
        }
        
        MappingField mapping = this.mappingProvider.getFieldMapping(field);
        // If no obf mapping, we can attempt to remap the owner class
        if (mapping == null) {
            if (lazyRemap) {
                return null;
            }
            mapping = field;
        }
        String remappedOwner = this.getObfClass(mapping.getOwner());
        if (remappedOwner == null || remappedOwner.equals(field.getOwner()) || remappedOwner.equals(mapping.getOwner())) {
            return mapping != field ? mapping : null;
        }
        return mapping.move(remappedOwner);
    }
    
    /**
     * Get an obfuscation mapping for a class
     */
    @Override
    public String getObfClass(String className) {
        if (!this.initMappings()) {
            return null;
        }
        return this.mappingProvider.getClassMapping(className);
    }

    /**
     * Write out generated mappings
     */
    @Override
    public void writeMappings(Collection<IMappingConsumer> consumers) {
        MappingSet<MappingField> fields = new MappingSet<MappingField>();
        MappingSet<MappingMethod> methods = new MappingSet<MappingMethod>();
        
        for (IMappingConsumer mappings : consumers) {
            fields.addAll(mappings.getFieldMappings(this.type));
            methods.addAll(mappings.getMethodMappings(this.type));
        }

        this.mappingWriter.write(this.outFileName, this.type, fields, methods);
    }

}
