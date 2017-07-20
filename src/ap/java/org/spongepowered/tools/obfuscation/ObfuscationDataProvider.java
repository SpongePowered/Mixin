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

import java.util.List;

import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.obfuscation.mapping.IMapping;
import org.spongepowered.asm.obfuscation.mapping.IMapping.Type;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IObfuscationDataProvider;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;

/**
 * Implementation of obfuscation provider which queries all obfuscation
 * environments to return mappings for each source member
 */
public class ObfuscationDataProvider implements IObfuscationDataProvider {
    
    /**
     * Annotation processor
     */
    private final IMixinAnnotationProcessor ap;

    /**
     * Available obfuscation environments
     */
    private final List<ObfuscationEnvironment> environments;

    public ObfuscationDataProvider(IMixinAnnotationProcessor ap, List<ObfuscationEnvironment> environments) {
        this.ap = ap;
        this.environments = environments;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.IObfuscationManager
     *      #getObfEntryRecursive(
     *      org.spongepowered.asm.mixin.injection.struct.MemberInfo)
     */
    @Override
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
                obfData = this.<T>getObfEntryUsing(currentTarget, superClass);
                if (!obfData.isEmpty()) {
                    return ObfuscationDataProvider.applyParents(obfTargetNames, obfData);
                }
                
                for (TypeHandle iface : targetType.getInterfaces()) {
                    obfData = this.<T>getObfEntryUsing(currentTarget, iface);
                    if (!obfData.isEmpty()) {
                        return ObfuscationDataProvider.applyParents(obfTargetNames, obfData);
                    }
                }
                
                if (superClass == null) {
                    break;
                }
                
                currentTarget = currentTarget.move(superClass.getName());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return this.getObfEntry(targetMember);
        }
        return obfData;
    }

    /**
     * Returns an obf entry for the specified member relocated into the
     * specified target class. This is used by {@link #getObfEntryRecursive} to
     * searh for matching obf entries in super classes an interfaces.
     * 
     * @param targetMember target member to search for
     * @param targetClass new target class to check, if the supplied argument is
     *      <tt>null</tt> then an empty dataset is returned 
     * @return obfuscation data for the relocated member
     */
    private <T> ObfuscationData<T> getObfEntryUsing(MemberInfo targetMember, TypeHandle targetClass) {
        return targetClass == null ? new ObfuscationData<T>() : this.<T>getObfEntry(targetMember.move(targetClass.getName()));
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
            return (ObfuscationData<T>)this.getObfField(targetMember);
        }
        return (ObfuscationData<T>)this.getObfMethod(targetMember.asMethodMapping());
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> ObfuscationData<T> getObfEntry(IMapping<T> mapping) {
        if (mapping != null) {
            if (mapping.getType() == Type.FIELD) {
                return (ObfuscationData<T>)this.getObfField((MappingField)mapping);
            } else if (mapping.getType() == Type.METHOD) {
                return (ObfuscationData<T>)this.getObfMethod((MappingMethod)mapping);
            } 
        }
        
        return new ObfuscationData<T>();
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.IObfuscationManager
     *      #getObfMethodRecursive(
     *      org.spongepowered.asm.mixin.injection.struct.MemberInfo)
     */
    @Override
    public ObfuscationData<MappingMethod> getObfMethodRecursive(MemberInfo targetMember) {
        return this.<MappingMethod>getObfEntryRecursive(targetMember);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.IObfuscationManager
     *      #getObfMethod(
     *      org.spongepowered.asm.mixin.injection.struct.MemberInfo)
     */
    @Override
    public ObfuscationData<MappingMethod> getObfMethod(MemberInfo method) {
        return this.getRemappedMethod(method, method.isConstructor());
    }
    
    @Override
    public ObfuscationData<MappingMethod> getRemappedMethod(MemberInfo method) {
        return this.getRemappedMethod(method, true);
    }

    private ObfuscationData<MappingMethod> getRemappedMethod(MemberInfo method, boolean remapDescriptor) {
        ObfuscationData<MappingMethod> data = new ObfuscationData<MappingMethod>();
        
        for (ObfuscationEnvironment env : this.environments) {
            MappingMethod obfMethod = env.getObfMethod(method);
            if (obfMethod != null) {
                data.put(env.getType(), obfMethod);
            }
        }
        
        if (!data.isEmpty() || !remapDescriptor) {
            return data;
        }
        
        return this.remapDescriptor(data, method);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.interfaces.IObfuscationProvider
     *      #getObfMethod(
     *      org.spongepowered.asm.obfuscation.mapping.common.MappingMethod)
     */
    @Override
    public ObfuscationData<MappingMethod> getObfMethod(MappingMethod method) {
        return this.getRemappedMethod(method, method.isConstructor());
    }
    
    @Override
    public ObfuscationData<MappingMethod> getRemappedMethod(MappingMethod method) {
        return this.getRemappedMethod(method, true);
    }

    private ObfuscationData<MappingMethod> getRemappedMethod(MappingMethod method, boolean remapDescriptor) {
        ObfuscationData<MappingMethod> data = new ObfuscationData<MappingMethod>();
        
        for (ObfuscationEnvironment env : this.environments) {
            MappingMethod obfMethod = env.getObfMethod(method);
            if (obfMethod != null) {
                data.put(env.getType(), obfMethod);
            }
        }
        
        if (!data.isEmpty() || !remapDescriptor) {
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
    public ObfuscationData<MappingMethod> remapDescriptor(ObfuscationData<MappingMethod> data, MemberInfo method) {
        for (ObfuscationEnvironment env : this.environments) {
            MemberInfo obfMethod = env.remapDescriptor(method);
            if (obfMethod != null) {
                data.put(env.getType(), obfMethod.asMethodMapping());
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
    public ObfuscationData<MappingField> getObfFieldRecursive(MemberInfo targetMember) {
        return this.<MappingField>getObfEntryRecursive(targetMember);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.IObfuscationManager
     *      #getObfField(java.lang.String)
     */
    @Override
    public ObfuscationData<MappingField> getObfField(MemberInfo field) {
        return this.getObfField(field.asFieldMapping());
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.IObfuscationManager
     *      #getObfField(java.lang.String)
     */
    @Override
    public ObfuscationData<MappingField> getObfField(MappingField field) {
        ObfuscationData<MappingField> data = new ObfuscationData<MappingField>();
        
        for (ObfuscationEnvironment env : this.environments) {
            MappingField obfField = env.getObfField(field);
            if (obfField != null) {
                if (obfField.getDesc() == null && field.getDesc() != null) {
                    obfField = obfField.transform(env.remapDescriptor(field.getDesc()));
                }
                data.put(env.getType(), obfField);
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
        
        for (ObfuscationEnvironment env : this.environments) {
            String obfClass = env.getObfClass(className);
            if (obfClass != null) {
                data.put(env.getType(), obfClass);
            }
        }
        
        return data;
    }

    /**
     * Applies the supplied parent names to the supplied
     * 
     * @param parents parent class names
     * @param members members to reparent
     * @return members for passthrough
     */
    @SuppressWarnings("unchecked")
    private static <T> ObfuscationData<T> applyParents(ObfuscationData<String> parents, ObfuscationData<T> members) {
        for (ObfuscationType type : members) {
            String obfClass = parents.get(type);
            T obfMember = members.get(type);
            members.put(type, (T)MemberInfo.fromMapping((IMapping<?>)obfMember).move(obfClass).asMapping());
        }
        return members;
    }

}
