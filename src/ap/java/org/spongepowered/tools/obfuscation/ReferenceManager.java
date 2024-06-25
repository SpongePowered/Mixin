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
import java.net.URI;
import java.util.List;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorRemappable;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.refmap.ReferenceMapper;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IReferenceManager;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerEx.MessageType;

/**
 * Implementation of the reference manager
 */
public class ReferenceManager implements IReferenceManager {
    
    /**
     * Exception thrown when a reference conflict occurs
     */
    public static class ReferenceConflictException extends RuntimeException {

        private static final long serialVersionUID = 1L;
        
        private final String oldReference, newReference;

        public ReferenceConflictException(String oldReference, String newReference) {
            this.oldReference = oldReference;
            this.newReference = newReference;
        }
        
        public String getOld() {
            return this.oldReference;
        }
        
        public String getNew() {
            return this.newReference;
        }
        
    }

    /**
     * Annotation processor
     */
    private final IMixinAnnotationProcessor ap;
    
    /**
     * Name of the resource to write remapped refs to
     */
    private final String outRefMapFileName;

    /**
     * Available obfuscation environments
     */
    private final List<ObfuscationEnvironment> environments;
    
    /**
     * Reference mapper for reference mapping 
     */
    private final ReferenceMapper refMapper = new ReferenceMapper();
    
    private boolean allowConflicts;
    
    public ReferenceManager(IMixinAnnotationProcessor ap, List<ObfuscationEnvironment> environments) {
        this.ap = ap;
        this.environments = environments;
        this.outRefMapFileName = this.ap.getOption(SupportedOptions.OUT_REFMAP_FILE);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.interfaces.IReferenceManager
     *      #getAllowConflicts()
     */
    @Override
    public boolean getAllowConflicts() {
        return this.allowConflicts;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.interfaces.IReferenceManager
     *      #setAllowConflicts(boolean)
     */
    @Override
    public void setAllowConflicts(boolean allowConflicts) {
        this.allowConflicts = allowConflicts;
    }

    /**
     * Write out stored mappings
     */
    @Override
    public void write() {
        if (this.outRefMapFileName == null) {
            return;
        }
        
        PrintWriter writer = null;
        
        try {
            writer = this.newWriter(this.outRefMapFileName, "refmap");
            if (writer != null) {
                this.refMapper.write(writer);
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
    private PrintWriter newWriter(String fileName, String description) throws IOException {
        if (fileName.matches("^.*[\\\\/:].*$")) {
            File outFile = new File(fileName);
            outFile.getParentFile().mkdirs();
            this.ap.printMessage(MessageType.INFO, "Writing " + description + " to " + outFile.getAbsolutePath());
            return new PrintWriter(outFile);
        }
        
        try {
            Filer filer = this.ap.getProcessingEnvironment().getFiler();
            FileObject outResource = null;
            try {
                outResource = filer.createResource(StandardLocation.CLASS_OUTPUT, "", fileName);
            } catch (Exception ex) {
                // fileName is not a valid relative path?
                outResource = filer.createResource(StandardLocation.CLASS_OUTPUT, "", new File(fileName).getName());
            }
            
            URI resourceUri = outResource.toUri();
            String absolutePath = "file".equals(resourceUri.getScheme()) ? new File(resourceUri).getAbsolutePath() : resourceUri.toString();
            PrintWriter writer = new PrintWriter(outResource.openWriter());
            this.ap.printMessage(MessageType.INFO, "Writing " + description + " to (" + resourceUri.getScheme() + ") " + absolutePath);
            return writer;
        } catch (Exception ex) {
            this.ap.printMessage(MessageType.ERROR, "Cannot write " + description + " to (" + fileName + "): " + ex.getClass().getName()
                    + ": " + ex.getMessage());
            return null;
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.IObfuscationManager
     *      #getReferenceMapper()
     */
    @Override
    public ReferenceMapper getMapper() {
        return this.refMapper;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.interfaces.IObfuscationManager
     *      #addMethodMapping(java.lang.String, java.lang.String,
     *      org.spongepowered.tools.obfuscation.ObfuscationData)
     */
    @Override
    public void addMethodMapping(String className, String reference, ObfuscationData<MappingMethod> obfMethodData) {
        for (ObfuscationEnvironment env : this.environments) {
            MappingMethod obfMethod = obfMethodData.get(env.getType());
            if (obfMethod != null) {
                MemberInfo remappedReference = new MemberInfo(obfMethod);
                this.addMapping(env.getType(), className, reference, remappedReference.toString());
            }
        }
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.interfaces.IObfuscationManager
     *      #addMethodMapping(java.lang.String, java.lang.String,
     *      org.spongepowered.asm.mixin.injection.struct.MemberInfo,
     *      org.spongepowered.tools.obfuscation.ObfuscationData)
     */
    @Override
    public void addMethodMapping(String className, String reference, ITargetSelectorRemappable context,
            ObfuscationData<MappingMethod> obfMethodData) {
        for (ObfuscationEnvironment env : this.environments) {
            MappingMethod obfMethod = obfMethodData.get(env.getType());
            if (obfMethod != null) {
                ITargetSelectorRemappable remappedReference = context.remapUsing(obfMethod, true);
                this.addMapping(env.getType(), className, reference, remappedReference.toString());
            }
        }
    }
        
    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.interfaces.IObfuscationManager
     *      #addFieldMapping(java.lang.String, java.lang.String,
     *      org.spongepowered.asm.mixin.injection.struct.MemberInfo,
     *      org.spongepowered.tools.obfuscation.ObfuscationData)
     */
    @Override
    public void addFieldMapping(String className, String reference, ITargetSelectorRemappable context, ObfuscationData<MappingField> obfFieldData) {
        for (ObfuscationEnvironment env : this.environments) {
            MappingField obfField = obfFieldData.get(env.getType());
            if (obfField != null) {
                MemberInfo remappedReference = MemberInfo.fromMapping(obfField.transform(env.remapDescriptor(context.getDesc())));
                this.addMapping(env.getType(), className, reference, remappedReference.toString());
            }
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.interfaces.IObfuscationManager
     *      #addClassMapping(java.lang.String, java.lang.String,
     *      org.spongepowered.tools.obfuscation.ObfuscationData)
     */
    @Override
    public void addClassMapping(String className, String reference, ObfuscationData<String> obfClassData) {
        for (ObfuscationEnvironment env : this.environments) {
            String remapped = obfClassData.get(env.getType());
            if (remapped != null) {
                this.addMapping(env.getType(), className, reference, remapped);
            }
        }
    }

    protected void addMapping(ObfuscationType type, String className, String reference, String newReference) {
        String oldReference = this.refMapper.addMapping(type.getKey(), className, reference, newReference);
        if (type.isDefault()) {
            this.refMapper.addMapping(null, className, reference, newReference);
        }

        if (!this.allowConflicts && oldReference != null && !oldReference.equals(newReference)) {
            throw new ReferenceConflictException(oldReference, newReference);
        }
    }
}
