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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.InvalidMixinException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Method;
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
     * All interfaces implemented by this mixin, including soft implementations
     */
    private final transient Set<String> interfaces = new HashSet<String>();

    /**
     * Interfaces soft-implemented using {@link Implements} 
     */
    private final transient List<InterfaceInfo> softImplements = new ArrayList<InterfaceInfo>();

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
        this.classInfo = ClassInfo.fromClassNode(classNode);
        this.priority = this.readPriority(classNode);
        this.targetClasses = this.readTargetClasses(classNode, suppressPlugin);
        this.targetClassNames = Collections.unmodifiableList(Lists.transform(this.targetClasses, Functions.toStringFunction()));
        this.detachedSuper = this.validateTargetClasses(classNode);
        this.validateMixin(classNode);
        this.readImplementations(classNode);
        this.prepare(classNode);
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
            targetClassName = targetClassName.replace('/', '.');
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
            
            if (!targetClass.hasSuperClass(classNode.superName, ClassInfo.Traversal.NONE)) {
                throw new InvalidMixinException("Super class '" + classNode.superName.replace('/', '.') + "' of " + this.name
                        + " was not found in the hierarchy of target class '" + targetClass + "'");
            }
            
            detached = true;
        }
        
        return detached;
    }

    /**
     * Performs pre-flight checks on the mixin
     * 
     * @param classNode
     */
    private void validateMixin(ClassNode classNode) {
        // isInner (shouldn't) return true for static inner classes
        if (this.classInfo.isInner()) {
            throw new InvalidMixinException("Inner class mixin must be declared static");
        }

        // Can't have remappable fields or methods on a multi-target mixin, because after obfuscation the fields will remap to conflicting names
        if (this.targetClasses.size() > 1) {
            for (FieldNode field : classNode.fields) {
                this.checkRemappable(Shadow.class, field.name, ASMHelper.getVisibleAnnotation(field, Shadow.class));
            }
            
            for (MethodNode method : classNode.methods) {
                this.checkRemappable(Shadow.class, method.name, ASMHelper.getVisibleAnnotation(method, Shadow.class));
                AnnotationNode overwrite = ASMHelper.getVisibleAnnotation(method, Overwrite.class);
                if (overwrite != null && ((method.access & Opcodes.ACC_STATIC) == 0 || (method.access & Opcodes.ACC_PUBLIC) == 0)) {
                    throw new InvalidMixinException("Found @Overwrite annotation on " + method.name + " in " + this);
                }
            }
        }
    }

    private void checkRemappable(Class<Shadow> annotationClass, String name, AnnotationNode annotation) {
        if (annotation != null && ASMHelper.getAnnotationValue(annotation, "remap", Boolean.TRUE)) {
            throw new InvalidMixinException("Found a remappable @" + annotationClass.getSimpleName() + " annotation on " + name + " in " + this);
        }
    }
    
    /**
     * Prepare the mixin, applies any pre-processing transformations
     */
    private ClassNode prepare(ClassNode classNode) {
        this.findRenamedMethods(classNode);
        this.transformMethods(classNode);
        return classNode;
    }

    /**
     * Read and process any {@link Implements} annotations on the mixin
     */
    private void readImplementations(ClassNode classNode) {
        this.interfaces.addAll(classNode.interfaces);
        
        AnnotationNode implementsAnnotation = ASMHelper.getInvisibleAnnotation(classNode, Implements.class);
        if (implementsAnnotation == null) {
            return;
        }
        
        List<AnnotationNode> interfaces = ASMHelper.getAnnotationValue(implementsAnnotation);
        if (interfaces == null) {
            return;
        }
        
        for (AnnotationNode interfaceNode : interfaces) {
            InterfaceInfo interfaceInfo = InterfaceInfo.fromAnnotation(interfaceNode);
            this.softImplements.add(interfaceInfo);
            this.interfaces.add(interfaceInfo.getInternalName());
        }
    }

    /**
     * Let's do this
     */
    private void findRenamedMethods(ClassNode classNode) {
        for (MethodNode mixinMethod : classNode.methods) {
            Method method = this.classInfo.findMethod(mixinMethod);
            
            AnnotationNode shadowAnnotation = ASMHelper.getVisibleAnnotation(mixinMethod, Shadow.class);
            if (shadowAnnotation != null) {
                String prefix = ASMHelper.<String>getAnnotationValue(shadowAnnotation, "prefix", Shadow.class);
                if (mixinMethod.name.startsWith(prefix)) {
                    String newName = mixinMethod.name.substring(prefix.length());
                    method.renameTo(newName);
                    mixinMethod.name = newName;
                }
            }
            
            for (InterfaceInfo iface : this.softImplements) {
                if (iface.renameMethod(mixinMethod)) {
                    method.renameTo(mixinMethod.name);
                }
            }
        }
    }

    /**
     * Apply discovered method renames to method invokations in the mixin
     */
    private void transformMethods(ClassNode classNode) {
        for (MethodNode mixinMethod : classNode.methods) {
            for (Iterator<AbstractInsnNode> iter = mixinMethod.instructions.iterator(); iter.hasNext();) {
                AbstractInsnNode insn = iter.next();
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode methodNode = (MethodInsnNode)insn;
                    Method method = this.classInfo.findMethodInHierarchy(methodNode, true);
                    if (method != null && method.isRenamed()) {
                        methodNode.name = method.getName();
                    }
                }
            }
        }
    }

    ClassInfo getClassInfo() {
        return this.classInfo;
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
     * Get all interfaces for this mixin
     * 
     * @return mixin interfaces
     */
    public Set<String> getInterfaces() {
        return this.interfaces;
    }

    /**
     * Get a new mixin data container for this info
     * 
     * @param target
     * @return
     */
    public MixinData createData(ClassNode target) {
        ClassNode classNode = this.getClassNode(ClassReader.EXPAND_FRAMES);
        return new MixinData(this, this.prepare(classNode), target);
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

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("%s:%s", this.parent.getName(), this.name);
    }
}
