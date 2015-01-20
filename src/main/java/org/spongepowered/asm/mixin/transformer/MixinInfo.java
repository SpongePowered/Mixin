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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.InvalidMixinException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.util.ASMHelper;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;

/**
 * Runtime information bundle about a mixin
 */
class MixinInfo extends TreeInfo implements Comparable<MixinInfo>, IMixinInfo {
    
    /**
     * Global order of mixin infos, used to determine ordering between mixins
     * with equivalent priority
     */
    static int mixinOrder = 0;
    
    /**
     * Logger
     */
    private final transient Logger logger = LogManager.getLogger("mixin");
    
    /**
     * Parent configuration which declares this mixin 
     */
    private final transient MixinConfig parent;
    
    /**
     * Simple name 
     */
    private final String name;

    /**
     * Name of the mixin class itself, dotted notation
     */
    private final String className;

    /**
     * Mixin ClassInfo
     */
    private final transient ClassInfo classInfo;
    
    /**
     * Mixin priority, read from the {@link Mixin} annotation on the mixin class
     */
    private final int priority;
    
    /**
     * Mixin targets, read from the {@link Mixin} annotation on the mixin class
     */
    private final List<ClassInfo> targetClasses;
    
    /**
     * Names of target classes 
     */
    private final List<String> targetClassNames;
    
    /**
     * True if the superclass of the mixin is <b>not</b> the direct superclass
     * of one or more targets 
     */
    private final boolean detachedSuper;
    
    /**
     * Intrinsic order (for sorting mixins with identical priority)
     */
    private final transient int order = MixinInfo.mixinOrder++;
    
    /**
     * Mixin bytes (read once, generate tree on demand)
     */
    private final transient byte[] mixinBytes;

    /**
     * Configuration plugin
     */
    private final transient IMixinConfigPlugin plugin;

    /**
     * Internal ctor, called by {@link MixinConfig}
     * 
     * @param parent
     * @param mixinName
     * @param runTransformers
     * @param plugin 
     * @param suppressPlugin 
     * @throws ClassNotFoundException 
     */
    MixinInfo(MixinConfig parent, String mixinName, boolean runTransformers, IMixinConfigPlugin plugin, boolean suppressPlugin)
            throws ClassNotFoundException {
        this.parent = parent;
        this.name = mixinName;
        this.className = parent.getMixinPackage() + mixinName;
        this.plugin = plugin;
        
        // Read the class bytes and transform
        this.mixinBytes = this.loadMixinClass(this.className, runTransformers);
        
        ClassNode classNode = this.getClassNode(0);
        this.classInfo = this.readInfo(classNode);
        this.priority = this.readPriority(classNode);
        this.targetClasses = this.readTargetClasses(classNode, suppressPlugin);
        this.targetClassNames = Collections.unmodifiableList(Lists.transform(this.targetClasses, Functions.toStringFunction()));
        this.detachedSuper = this.validateTargetClasses(classNode);
    }

    /**
     * Initialises the class info and performs some pre-flight checks on the
     * mixin
     * 
     * @param classNode
     * @return
     */
    private ClassInfo readInfo(ClassNode classNode) {
        ClassInfo classInfo = ClassInfo.fromClassNode(classNode);
        
        // isInner (shouldn't) return true for static inner classes
        if (classInfo.isInner()) {
            throw new InvalidMixinException("Inner class mixin must be declared static");
        }
        
        return classInfo;
    }

    /**
     * Read the target class names from the {@link Mixin} annotation
     * 
     * @param classNode
     * @param suppressPlugin 
     * @return
     */
    private List<ClassInfo> readTargetClasses(ClassNode classNode, boolean suppressPlugin) {
        AnnotationNode mixin = ASMHelper.getInvisibleAnnotation(classNode, Mixin.class);
        if (mixin == null) {
            throw new InvalidMixinException(String.format("The mixin '%s' is missing an @Mixin annotation", this.className));
        }
        
        List<ClassInfo> targets = new ArrayList<ClassInfo>();
        List<Type> publicTargets = ASMHelper.getAnnotationValue(mixin, "value");
        List<String> privateTargets = ASMHelper.getAnnotationValue(mixin, "targets");

        if (publicTargets != null) {
            this.readTargets(targets, Lists.transform(publicTargets, new Function<Type, String>() {
                @Override
                public String apply(Type input) {
                    return input.getClassName();
                };
            }), suppressPlugin, false);
        }
        
        if (privateTargets != null) {
            this.readTargets(targets, privateTargets, suppressPlugin, true);
        }
        
        return targets;
    }

    /**
     * Reads a target list into the outTargets list
     */
    private void readTargets(List<ClassInfo> outTargets, List<String> inTargets, boolean suppressPlugin, boolean checkPublic) {
        for (String targetClassName : inTargets) {
            if (this.plugin == null || suppressPlugin || this.plugin.shouldApplyMixin(targetClassName, this.className)) {
                ClassInfo targetInfo = ClassInfo.forName(targetClassName);
                if (targetInfo.isInterface()) {
                    throw new InvalidMixinException("@Mixin target " + targetClassName + " is an interface in " + this);
                }
                if (checkPublic && targetInfo.isPublic()) {
                    throw new InvalidMixinException("@Mixin target " + targetClassName + " is public in " + this + " and must be specified in value");
                }
                if (!outTargets.contains(targetInfo)) {
                    outTargets.add(targetInfo);
                    targetInfo.addMixin(this);
                }
            }
        }
    }

    /**
     * Read the priority from the {@link Mixin} annotation
     * 
     * @param classNode
     * @return
     */
    private int readPriority(ClassNode classNode) {
        AnnotationNode mixin = ASMHelper.getInvisibleAnnotation(classNode, Mixin.class);
        if (mixin == null) {
            throw new InvalidMixinException(String.format("The mixin '%s' is missing an @Mixin annotation", this.className));
        }
        
        Integer priority = ASMHelper.getAnnotationValue(mixin, "priority");
        return priority == null ? 1000 : priority.intValue();
    }
    
    private boolean validateTargetClasses(ClassNode classNode) {
        boolean detached = false;
        
        for (ClassInfo targetClass : this.targetClasses) {
            
            if (classNode.superName.equals(targetClass.getSuperName())) {
                continue;
            }
            
            if (!targetClass.hasSuperClass(classNode.superName)) {
                throw new InvalidMixinException("Super class '" + classNode.superName.replace('/', '.') + "' of " + this.name
                        + " was not found in the hierarchy of target class '" + targetClass + "'");
            }
            
            detached = true;
        }
        
        return detached;
    }

    /**
     * Get the parent config which declares this mixin
     */
    public MixinConfig getParent() {
        return this.parent;
    }
    
    /**
     * Get the simple name of the mixin
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Get the name of the mixin class
     */
    @Override
    public String getClassName() {
        return this.className;
    }
    
    /**
     * Get the ref (internal name) of the mixin class
     */
    @Override
    public String getClassRef() {
        return this.classInfo.getName();
    }

    /**
     * Get the class bytecode
     */
    @Override
    public byte[] getClassBytes() {
        return this.mixinBytes;
    }
    
    /**
     * True if the superclass of the mixin is <b>not</b> the direct superclass
     * of one or more targets
     */
    @Override
    public boolean isDetachedSuper() {
        return this.detachedSuper;
    }

    /**
     * Get a new tree for the class bytecode
     */
    @Override
    public ClassNode getClassNode(int flags) {
        MixinClassNode classNode = new MixinClassNode(this);
        ClassReader classReader = new ClassReader(this.mixinBytes);
        classReader.accept(classNode, flags);
        return classNode;
    }
    
    /**
     * Get the target class names for this mixin
     */
    @Override
    public List<String> getTargetClasses() {
        return this.targetClassNames;
    }
    
    /**
     * Get the target class list for this mixin
     */
    public List<ClassInfo> getTargets() {
        return Collections.unmodifiableList(this.targetClasses);
    }
    
    /**
     * Get the mixin priority
     */
    @Override
    public int getPriority() {
        return this.priority;
    }
    
    /**
     * Get a new mixin data container for this info
     * 
     * @param target
     * @return
     */
    public MixinData createData(ClassNode target) {
        return new MixinData(this, target);
    }

    /**
     * @param mixinClassName
     * @param runTransformers
     * @return
     * @throws ClassNotFoundException 
     */
    private byte[] loadMixinClass(String mixinClassName, boolean runTransformers) throws ClassNotFoundException {
        byte[] mixinBytes = null;

        try {
            mixinBytes = TreeInfo.loadClass(mixinClassName, runTransformers);
        } catch (ClassNotFoundException ex) {
            throw new ClassNotFoundException(String.format("The specified mixin '%s' was not found", mixinClassName));
        } catch (IOException ex) {
            this.logger.warn("Failed to load mixin %s, the specified mixin will not be applied", mixinClassName);
            throw new InvalidMixinException("An error was encountered whilst loading the mixin class", ex);
        }

        return mixinBytes;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(MixinInfo other) {
        if (other == null) {
            return 0;
        }
        if (other.priority == this.priority) {
            return this.order - other.order;
        }
        return (this.priority - other.priority);
    }

    /**
     * Called immediately before the mixin is applied to targetClass
     */
    public void preApply(String transformedName, ClassNode targetClass) {
        if (this.plugin != null) {
            this.plugin.preApply(transformedName, targetClass, this.className, this);
        }
    }

    /**
     * Called immediately after the mixin is applied to targetClass
     */
    public void postApply(String transformedName, ClassNode targetClass) {
        if (this.plugin != null) {
            this.plugin.postApply(transformedName, targetClass, this.className, this);
        }
    }
}
