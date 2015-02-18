/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered.org <http://www.spongepowered.org>
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
package org.spongepowered.asm.mixin.transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.struct.ReferenceMapper;
import org.spongepowered.asm.mixin.injection.struct.Target;


/**
 * Data for applying a mixin. Keeps a copy of the mixin tree and also handles
 * any pre-transformations required by the mixin class itself such as method
 * renaming and any other pre-processing of the mixin bytecode.
 */
public class MixinData implements IReferenceMapperContext {
    
    /**
     * Mixin info
     */
    private final MixinInfo mixin;
    
    /**
     * Tree
     */
    private final ClassNode classNode;
    
    /**
     * 
     */
    private final ClassNode targetClass;
    
    /**
     * Target ClassInfo
     */
    private final ClassInfo targetClassInfo;
    
    /**
     * Information about methods in the target class, used to keep track of
     * transformations we apply
     */
    private final Map<String, Target> targetMethods = new HashMap<String, Target>();

    /**
     * ctor
     * 
     * @param info Mixin information
     * @param mixin Mixin classnode
     * @param target target class
     */
    MixinData(MixinInfo info, ClassNode mixin, ClassNode target) {
        this.mixin = info;
        this.classNode = mixin;
        this.targetClass = target;
        this.targetClassInfo = ClassInfo.forName(target.name);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.mixin.toString();
    }

    /**
     * Get the mixin tree
     * 
     * @return mixin tree
     */
    public ClassNode getClassNode() {
        return this.classNode;
    }
    
    /**
     * Get the mixin class name
     * 
     * @return the mixin class name
     */
    public String getClassName() {
        return this.mixin.getClassName();
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.IReferenceMapperContext
     *      #getClassRef()
     */
    @Override
    public String getClassRef() {
        return this.mixin.getClassRef();
    }

    /**
     * Get the target class reference
     * 
     * @return the reference of the target class (only valid on single-target
     *      mixins)
     */
    public String getTargetClassRef() {
        return this.targetClass.name;
    }
    
    /**
     * Get the target class
     * 
     * @return the target class
     */
    public ClassNode getTargetClass() {
        return this.targetClass;
    }
    
    /**
     * Get the target classinfo
     * 
     * @return the target class info 
     */
    public ClassInfo getTargetClassInfo() {
        return this.targetClassInfo;
    }
    
    /**
     * Get a target method handle from the target class
     * 
     * @param method method to get a target handle for
     * @return new or existing target handle for the supplied method
     */
    public Target getTargetMethod(MethodNode method) {
        if (!this.targetClass.methods.contains(method)) {
            throw new IllegalArgumentException("Invalid target method supplied to getTargetMethod()");
        }
        
        String targetName = method.name + method.desc;
        Target target = this.targetMethods.get(targetName);
        if (target == null) {
            target = new Target(method);
            this.targetMethods.put(targetName, target);
        }
        return target;
    }

    /**
     * Get the mixin priority
     * 
     * @return the priority (only meaningful in relation to other mixins)
     */
    public int getPriority() {
        return this.mixin.getPriority();
    }

    /**
     * Get all interfaces for this mixin
     * 
     * @return mixin interfaces
     */
    public Set<String> getInterfaces() {
        return this.mixin.getInterfaces();
    }

    /**
     * Get whether to propogate the source file attribute from a mixin onto the
     * target class
     * 
     * @return true if the sourcefile property should be set on the target class
     */
    public boolean shouldSetSourceFile() {
        return this.mixin.getParent().shouldSetSourceFile();
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.IReferenceMapperContext
     *      #getReferenceMapper()
     */
    @Override
    public ReferenceMapper getReferenceMapper() {
        return this.mixin.getParent().getReferenceMapper();
    }

    /**
     * Called immediately before the mixin is applied to targetClass
     * 
     * @param transformedName Target class's transformed name
     * @param targetClass Target class
     */
    public void preApply(String transformedName, ClassNode targetClass) {
        this.mixin.preApply(transformedName, targetClass);
    }

    /**
     * Called immediately after the mixin is applied to targetClass
     * 
     * @param transformedName Target class's transformed name
     * @param targetClass Target class
     */
    public void postApply(String transformedName, ClassNode targetClass) {
        this.mixin.postApply(transformedName, targetClass);
    }
}
