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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.FieldNode;
import org.spongepowered.asm.lib.tree.InnerClassNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
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
    
    static final Set<String> invalidClasses = MixinInfo.$getInvalidClassesSet();
    
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
     * Synthetic inner classes
     */
    private final transient Set<String> syntheticInnerClasses = new HashSet<String>();
    
    /**
     * The environment phase in which this mixin was initialised
     */
    private final transient Phase phase;
    
    /**
     * Initial ClassNode created for mixin validation, not used for actual
     * application 
     */
    private transient ClassNode validationClassNode;
    
    /**
     * True if the superclass of the mixin is <b>not</b> the direct superclass
     * of one or more targets 
     */
    private boolean detachedSuper;

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
        this.phase = MixinEnvironment.getCurrentEnvironment().getPhase();
        
        // Read the class bytes and transform
        this.mixinBytes = this.loadMixinClass(this.className, runTransformers);
        
        ClassNode classNode = this.getClassNode(0);
        this.priority = this.readPriority(classNode);
        this.targetClasses = this.readTargetClasses(classNode, suppressPlugin);
        this.targetClassNames = Collections.unmodifiableList(Lists.transform(this.targetClasses, Functions.toStringFunction()));
        this.validationClassNode = classNode;
        this.classInfo = ClassInfo.fromClassNode(classNode);
    }
    
    void validate() {
        this.detachedSuper = this.validateTargetClasses(this.validationClassNode);
        this.validateMixin(this.validationClassNode);
        this.readImplementations(this.validationClassNode);
        this.readInnerClasses(this.validationClassNode);
        new MixinPreProcessor(this, this.validationClassNode).prepare();
        this.validationClassNode = null;
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
            throw new InvalidMixinException(this, String.format("The mixin '%s' is missing an @Mixin annotation", this.className));
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
                if (targetInfo == null) {
                    throw new RuntimeException("@Mixin target " + targetClassName + " was not found " + this);
                }
                if (targetInfo.isInterface()) {
                    throw new InvalidMixinException(this, "@Mixin target " + targetClassName + " is an interface in " + this);
                }
                if (checkPublic && targetInfo.isPublic()) {
                    throw new InvalidMixinException(this, "@Mixin target " + targetClassName + " is public in " + this
                            + " and must be specified in value");
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
            throw new InvalidMixinException(this, String.format("The mixin '%s' is missing an @Mixin annotation", this.className));
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
            
            if (!targetClass.hasSuperClass(classNode.superName, ClassInfo.Traversal.IMMEDIATE)) {
                throw new InvalidMixinException(this, "Super class '" + classNode.superName.replace('/', '.') + "' of " + this.name
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
        if (!this.classInfo.isProbablyStatic()) {
            throw new InvalidMixinException(this, "Inner class mixin must be declared static");
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
                    throw new InvalidMixinException(this, "Found @Overwrite annotation on " + method.name + " in " + this);
                }
            }
        }
    }

    private void checkRemappable(Class<Shadow> annotationClass, String name, AnnotationNode annotation) {
        if (annotation != null && ASMHelper.getAnnotationValue(annotation, "remap", Boolean.TRUE)) {
            throw new InvalidMixinException(this, "Found a remappable @" + annotationClass.getSimpleName() + " annotation on " + name
                    + " in " + this);
        }
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
            InterfaceInfo interfaceInfo = InterfaceInfo.fromAnnotation(this, interfaceNode);
            this.softImplements.add(interfaceInfo);
            this.interfaces.add(interfaceInfo.getInternalName());
            this.classInfo.addInterface(interfaceInfo.getInternalName());
        }
    }

    /**
     * Read inner class definitions for the class and locate any synthetic inner
     * classes so that we can add them to the passthrough set in our parent
     * config.
     */
    private void readInnerClasses(ClassNode classNode) {
        for (InnerClassNode inner : classNode.innerClasses) {
            ClassInfo innerClass = ClassInfo.forName(inner.name);
            if (innerClass.isSynthetic() && innerClass.isProbablyStatic()) {
                if ((inner.outerName != null && inner.outerName.equals(this.classInfo.getName())) || inner.name.startsWith(classNode.name + "$")) {
                    this.syntheticInnerClasses.add(inner.name);
                } else {
                    throw new InvalidMixinException(this, "Unhandled synthetic inner class found: " + inner.name);
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
    @Override
    public IMixinConfig getConfig() {
        return this.parent;
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
     * Get the logging level for this mixin
     */
    public Level getLoggingLevel() {
        return this.parent.getLoggingLevel();
    }
    
    /**
     * Get the phase in which this mixin was initialised
     */
    @Override
    public Phase getPhase() {
        return this.phase;
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
     * Get the soft implementations for this mixin
     */
    List<InterfaceInfo> getSoftImplements() {
        return Collections.unmodifiableList(this.softImplements);
    }

    /**
     * Get the synthetic inner classes for this mixin
     */
    public Set<String> getSyntheticInnerClasses() {
        return Collections.unmodifiableSet(this.syntheticInnerClasses);
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
     * Get a new mixin target context object for the specified target
     * 
     * @param target
     * @return
     */
    public MixinTargetContext createContextFor(ClassNode target) {
        return new MixinPreProcessor(this, this.getClassNode(ClassReader.EXPAND_FRAMES)).createContextFor(target);
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
            throw new InvalidMixinException(this, "An error was encountered whilst loading the mixin class", ex);
        }
        
        // Inject the mixin class name into the LaunchClassLoader's invalid
        // classes set so that any classes referencing the mixin directly will
        // cause the game to crash
        if (MixinInfo.invalidClasses != null) {
            MixinInfo.invalidClasses.add(mixinClassName);
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
        
        this.parent.postApply(transformedName, targetClass);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("%s:%s", this.parent.getName(), this.name);
    }

    @SuppressWarnings("unchecked")
    private static Set<String> $getInvalidClassesSet() {
        try {
            Field invalidClasses = LaunchClassLoader.class.getDeclaredField("invalidClasses");
            invalidClasses.setAccessible(true);
            return (Set<String>)invalidClasses.get(Launch.classLoader);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
