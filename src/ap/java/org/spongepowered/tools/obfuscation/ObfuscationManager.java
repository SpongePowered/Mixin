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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.injection.struct.ReferenceMapper;
import org.spongepowered.asm.obfuscation.MethodData;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IObfuscationManager;

public class ObfuscationManager implements IObfuscationManager {
    
    /**
     * Annotation processor
     */
    private final IMixinAnnotationProcessor ap;
    
    /**
     * Name of the resource to write remapped refs to
     */
    private final String outRefMapFileName;
    
    /**
     * Target obfuscation environments
     */
    private final List<TargetObfuscationEnvironment> targetEnvironments = new ArrayList<TargetObfuscationEnvironment>();
    
    /**
     * Reference mapper for reference mapping 
     */
    private final ReferenceMapper refMapper = new ReferenceMapper();
    
    public ObfuscationManager(IMixinAnnotationProcessor ap) {
        this.ap = ap;
        this.outRefMapFileName = this.ap.getOption(SupportedOptions.OUT_REFMAP_FILE);
        for (ObfuscationType obfType : ObfuscationType.values()) {
            if (obfType.isSupported(this.ap)) {
                this.targetEnvironments.add(new TargetObfuscationEnvironment(ap, obfType, this.refMapper));
            }
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.IObfuscationManager
     *      #getReferenceMapper()
     */
    @Override
    public ReferenceMapper getReferenceMapper() {
        return this.refMapper;
    }

    /**
     * Write out generated srgs
     */
    public void writeSrgs(Map<String, AnnotatedMixin> mixins) {
        for (TargetObfuscationEnvironment targetEnv : this.targetEnvironments) {
            targetEnv.writeSrgs(this.ap.getProcessingEnvironment().getFiler(), mixins);
        }
    }

    /**
     * Write out stored mappings
     */
    public void writeRefs() {
        if (this.outRefMapFileName == null) {
            return;
        }
        
        PrintWriter writer = null;
        
        try {
            writer = this.newWriter(this.outRefMapFileName, "refmap");
            this.refMapper.write(writer);
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
            File outSrgFile = new File(fileName);
            outSrgFile.getParentFile().mkdirs();
            this.ap.printMessage(Kind.NOTE, "Writing " + description + " to " + outSrgFile.getAbsolutePath());
            return new PrintWriter(outSrgFile);
        }
        
        FileObject outSrg = this.ap.getProcessingEnvironment().getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", fileName);
        this.ap.printMessage(Kind.NOTE, "Writing " + description + " to " + new File(outSrg.toUri()).getAbsolutePath());
        return new PrintWriter(outSrg.openWriter());
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.IObfuscationManager
     *      #getObfEntryRecursive(
     *      org.spongepowered.asm.mixin.injection.struct.MemberInfo)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> ObfuscationData<T> getObfEntryRecursive(final MemberInfo targetMember) {
        MemberInfo currentTarget = targetMember;
        ObfuscationData<String> obfTargetNames = this.getObfClass(currentTarget.owner);
        ObfuscationData<T> obfData = this.getObfEntry(currentTarget);
        try {
            while (obfData.isEmpty()) {
                TypeHandle targetType = this.ap.getTypeProvider().getTypeHandle(currentTarget.owner);
                if (targetType == null) {
                    return obfData;
                }
                TypeHandle superClass = targetType.getSuperclass();
                if (superClass == null) {
                    return obfData;
                }
                currentTarget = currentTarget.move(superClass.getName());
                obfData = this.getObfEntry(currentTarget);
                if (!obfData.isEmpty()) {
                    for (ObfuscationType type : obfData) {
                        String obfClass = obfTargetNames.get(type);
                        T obfMember = obfData.get(type);
                        if (currentTarget.isField()) {
                            obfData.add(type, (T)MemberInfo.fromSrgField(obfMember.toString(), "").move(obfClass).toSrg());
                        } else {
                            obfData.add(type, (T)MemberInfo.fromSrgMethod((MethodData)obfMember).move(obfClass).asMethodData());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            return this.getObfEntry(targetMember);
        }
        return obfData;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.IObfuscationManager
     *      #getObfEntry(
     *      org.spongepowered.asm.mixin.injection.struct.MemberInfo)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> ObfuscationData<T> getObfEntry(MemberInfo targetMember) {
        if (targetMember.isField()) {
            return (ObfuscationData<T>)this.getObfField(targetMember.toSrg());
        }
        return (ObfuscationData<T>)this.getObfMethod(targetMember.asMethodData());
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.IObfuscationManager
     *      #getObfMethodRecursive(
     *      org.spongepowered.asm.mixin.injection.struct.MemberInfo)
     */
    @Override
    public ObfuscationData<MethodData> getObfMethodRecursive(MemberInfo targetMember) {
        return this.<MethodData>getObfEntryRecursive(targetMember);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.IObfuscationManager
     *      #getObfMethod(
     *      org.spongepowered.asm.mixin.injection.struct.MemberInfo)
     */
    @Override
    public ObfuscationData<MethodData> getObfMethod(MemberInfo method) {
        ObfuscationData<MethodData> data = new ObfuscationData<MethodData>();
        
        for (TargetObfuscationEnvironment targetEnv : this.targetEnvironments) {
            MethodData obfMethod = targetEnv.getObfMethod(method);
            if (obfMethod != null) {
                data.add(targetEnv.getType(), obfMethod);
            }
        }
        
        if (!data.isEmpty() || !Constants.INIT.equals(method.name)) {
            return data;
        }
        
        return this.remapDescriptor(data, method);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.IObfuscationManager
     *      #getObfMethod(net.minecraftforge.srg2source.rangeapplier.MethodData)
     */
    @Override
    public ObfuscationData<MethodData> getObfMethod(MethodData method) {
        ObfuscationData<MethodData> data = new ObfuscationData<MethodData>();
        
        for (TargetObfuscationEnvironment targetEnv : this.targetEnvironments) {
            MethodData obfMethod = targetEnv.getObfMethod(method);
            if (obfMethod != null) {
                data.add(targetEnv.getType(), obfMethod);
            }
        }
        
        if (!data.isEmpty() || !Constants.INIT.equals(method.getSimpleName())) {
            return data;
        }
        
        return this.remapDescriptor(data, new MemberInfo(method));
    }

    /**
     * Remap a method owner and descriptor only, used for remapping ctors 
     * 
     * @param data Output method data collection
     * @param method Method to remap
     * @return data 
     */
    public ObfuscationData<MethodData> remapDescriptor(ObfuscationData<MethodData> data, MemberInfo method) {
        for (TargetObfuscationEnvironment targetEnv : this.targetEnvironments) {
            MemberInfo obfMethod = targetEnv.remapDescriptor(method);
            if (obfMethod != null) {
                data.add(targetEnv.getType(), obfMethod.asMethodData());
            }
        }

        return data;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.IObfuscationManager
     *      #getObfFieldRecursive(
     *      org.spongepowered.asm.mixin.injection.struct.MemberInfo)
     */
    @Override
    public ObfuscationData<String> getObfFieldRecursive(MemberInfo targetMember) {
        return this.<String>getObfEntryRecursive(targetMember);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.IObfuscationManager
     *      #getObfField(java.lang.String)
     */
    @Override
    public ObfuscationData<String> getObfField(String field) {
        ObfuscationData<String> data = new ObfuscationData<String>();
        
        for (TargetObfuscationEnvironment targetEnv : this.targetEnvironments) {
            String obfField = targetEnv.getObfField(field);
            if (obfField != null) {
                data.add(targetEnv.getType(), obfField);
            }
        }
        
        return data;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.IObfuscationManager
     *      #getObfClass(org.spongepowered.tools.obfuscation.TypeHandle)
     */
    @Override
    public ObfuscationData<String> getObfClass(TypeHandle type) {
        return this.getObfClass(type.getName());
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.IObfuscationManager
     *      #getObfClass(java.lang.String)
     */
    @Override
    public ObfuscationData<String> getObfClass(String className) {
        ObfuscationData<String> data = new ObfuscationData<String>(className);
        
        for (TargetObfuscationEnvironment targetEnv : this.targetEnvironments) {
            String obfClass = targetEnv.getObfClass(className);
            if (obfClass != null) {
                data.add(targetEnv.getType(), obfClass);
            }
        }
        
        return data;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.interfaces.IObfuscationManager
     *      #addMethodMapping(java.lang.String, java.lang.String,
     *      org.spongepowered.tools.obfuscation.ObfuscationData)
     */
    @Override
    public void addMethodMapping(String className, String reference, ObfuscationData<MethodData> obfMethodData) {
        for (TargetObfuscationEnvironment targetEnv : this.targetEnvironments) {
            MethodData obfMethod = obfMethodData.get(targetEnv.getType());
            if (obfMethod != null) {
                MemberInfo remappedReference = new MemberInfo(obfMethod);
                targetEnv.addMapping(className, reference, remappedReference.toString());
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
    public void addMethodMapping(String className, String reference, MemberInfo context, ObfuscationData<MethodData> obfMethodData) {
        for (TargetObfuscationEnvironment targetEnv : this.targetEnvironments) {
            MethodData obfMethod = obfMethodData.get(targetEnv.getType());
            if (obfMethod != null) {
                MemberInfo remappedReference = context.remapUsing(obfMethod, true);
                targetEnv.addMapping(className, reference, remappedReference.toString());
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
    public void addFieldMapping(String className, String reference, MemberInfo context, ObfuscationData<String> obfFieldData) {
        for (TargetObfuscationEnvironment targetEnv : this.targetEnvironments) {
            String obfField = obfFieldData.get(targetEnv.getType());
            if (obfField != null) {
                MemberInfo remappedReference = MemberInfo.fromSrgField(obfField, context.desc);
                String remapped = remappedReference.toString();
                targetEnv.addMapping(className, reference, remapped);
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
        for (TargetObfuscationEnvironment targetEnv : this.targetEnvironments) {
            String remapped = obfClassData.get(targetEnv.getType());
            if (remapped != null) {
                targetEnv.addMapping(className, reference, remapped);
            }
        }
    }
    
}
