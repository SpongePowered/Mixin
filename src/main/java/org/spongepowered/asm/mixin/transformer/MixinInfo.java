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
package org.spongepowered.asm.mixin.transformer;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.logging.ILogger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.CompatibilityLevel;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.Surrogate;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Method;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.mixin.transformer.throwables.MixinReloadException;
import org.spongepowered.asm.mixin.transformer.throwables.MixinTargetAlreadyLoadedException;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.LanguageFeatures;
import org.spongepowered.asm.util.asm.ASM;
import org.spongepowered.asm.util.asm.MethodNodeEx;
import org.spongepowered.asm.util.perf.Profiler;
import org.spongepowered.asm.util.perf.Profiler.Section;

import com.google.common.base.Functions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Runtime information bundle about a mixin
 */
class MixinInfo implements Comparable<MixinInfo>, IMixinInfo {
    
    /**
     * Class variant, used to determine subtype
     */
    enum Variant {
        
        /**
         * Standard mixin
         */
        STANDARD,
        
        /**
         * Interface mixin
         */
        INTERFACE,
        
        /**
         * Accessor mixin (interface mixin containing only accessors)
         */
        ACCESSOR,
        
        /**
         * Type proxy
         */
        PROXY
        
    }
    
    /**
     * A MethodNode in a mixin
     */
    class MixinMethodNode extends MethodNodeEx {
        
        public MixinMethodNode(int access, String name, String desc, String signature, String[] exceptions) {
            super(access, name, desc, signature, exceptions, MixinInfo.this);
        }
        
        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
            // Create a shallow copy of the bootstrap method args because the
            // base implementation just passes the array by reference. This
            // causes any changes applied to the cloned classnode to leak into
            // the "master" ClassNode! 
            Object[] bsmArgs = new Object[bootstrapMethodArguments.length];
            System.arraycopy(bootstrapMethodArguments, 0, bsmArgs, 0, bootstrapMethodArguments.length);
            this.instructions.add(new InvokeDynamicInsnNode(name, descriptor, bootstrapMethodHandle, bsmArgs));
        }

        public boolean isInjector() {
            return (this.getInjectorAnnotation() != null || this.isSurrogate());
        }

        public boolean isSurrogate() {
            return this.getVisibleAnnotation(Surrogate.class) != null;
        }

        public boolean isSynthetic() {
            return Bytecode.hasFlag(this, Opcodes.ACC_SYNTHETIC);
        }

        public AnnotationNode getVisibleAnnotation(Class<? extends Annotation> annotationClass) {
            return Annotations.getVisible(this, annotationClass);
        }

        public AnnotationNode getInjectorAnnotation() {
            return InjectionInfo.getInjectorAnnotation(MixinInfo.this, this);
        }

    }
    
    /**
     * ClassNode for a MixinInfo
     */
    class MixinClassNode extends ClassNode {
        
        public final List<MixinMethodNode> mixinMethods;
        
        MixinClassNode(MixinInfo mixin) {
            this(ASM.API_VERSION);
        }
        
        @SuppressWarnings("unchecked")
        protected MixinClassNode(int api) {
            super(api);
            this.mixinMethods = (List<MixinMethodNode>)(Object)this.methods;
        }
        
        public MixinInfo getMixin() {
            return MixinInfo.this;
        }
        
        public List<FieldNode> getFields() {
            return new ArrayList<FieldNode>(this.fields);
        }
        
        @Override
        public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
            MethodNode method = new MixinMethodNode(access, name, desc, signature, exceptions);
            this.methods.add(method);
            return method;
        }
        
    }

    /**
     * Mixin preparation/parse/validation state
     */
    class State {
        
        /**
         * Initial ClassNode passed in to the creation of this state object
         */
        private final ClassNode classNode;
        
        /**
         * Mixin ClassInfo
         */
        private final ClassInfo classInfo;

        /**
         * True if the superclass of the mixin is <b>not</b> the direct
         * superclass of one or more targets
         */
        private boolean detachedSuper;

        /**
         * True if this mixin is decorated with {@link Unique}
         */
        private boolean unique;

        /**
         * All interfaces implemented by this mixin, including soft
         * implementations
         */
        protected final Set<String> interfaces = new HashSet<String>();

        /**
         * Interfaces soft-implemented using {@link Implements}
         */
        protected final List<InterfaceInfo> softImplements = new ArrayList<InterfaceInfo>();

        /**
         * Synthetic inner classes
         */
        protected final Set<String> syntheticInnerClasses = new HashSet<String>();
        
        /**
         * Non-synthetic inner classes
         */
        protected final Set<String> innerClasses = new HashSet<String>();
        
        /**
         * Initial ClassNode created for mixin validation, not used for actual
         * application
         */
        protected MixinClassNode validationClassNode;

        State(ClassNode classNode) {
            this(classNode, null);
        }

        State(ClassNode classNode, ClassInfo classInfo) {
            this.classNode = classNode;
            this.connect();
            this.classInfo = classInfo != null ? classInfo : ClassInfo.fromClassNode(this.getValidationClassNode());
        }

        protected void connect() {
            this.validationClassNode = this.createClassNode(0);
        }

        protected void complete() {
            this.validationClassNode = null;
        }

        ClassInfo getClassInfo() {
            return this.classInfo;
        }
        
        ClassNode getClassNode() {
            return this.classNode;
        }

        MixinClassNode getValidationClassNode() {
            if (this.validationClassNode == null) {
                throw new IllegalStateException("Attempted a validation task after validation is complete on " + this + " in " + MixinInfo.this);
            }
            return this.validationClassNode;
        }

        boolean isDetachedSuper() {
            return this.detachedSuper;
        }
        
        boolean isUnique() {
            return this.unique;
        }

        List<? extends InterfaceInfo> getSoftImplements() {
            return this.softImplements;
        }
        
        Set<String> getSyntheticInnerClasses() {
            return this.syntheticInnerClasses;
        }
        
        Set<String> getInnerClasses() {
            return this.innerClasses;
        }

        Set<String> getInterfaces() {
            return this.interfaces;
        }

        /**
         * Gets a new tree from the bytecode
         *
         * @param flags Flags passed into classReader
         * @return Tree representing the bytecode
         */
        MixinClassNode createClassNode(int flags) {
            MixinClassNode mixinClassNode = new MixinClassNode(MixinInfo.this);
            this.classNode.accept(mixinClassNode);
            return mixinClassNode;
        }

        /**
         * Performs pre-flight checks on the mixin
         * 
         * @param type Mixin Type
         * @param targetClasses Mixin's target classes
         */
        void validate(SubType type, List<ClassInfo> targetClasses) {
            MixinClassNode classNode = this.getValidationClassNode();
            
            MixinPreProcessorStandard preProcessor = type.createPreProcessor(classNode).prepare(MixinInfo.this.getExtensions());
            for (ClassInfo target : targetClasses) {
                preProcessor.conform(target);
            }
            
            type.validate(this, targetClasses);

            this.detachedSuper = type.isDetachedSuper();
            this.unique = Annotations.getVisible(classNode, Unique.class) != null;

            // Pre-flight checks
            this.validateInner();
            this.validateClassFeatures();
            this.validateRemappables(targetClasses);

            // Read information from the mixin
            this.readImplementations(type);
            this.readInnerClasses();
            
            // Takeoff validation
            this.validateChanges(type, targetClasses);
            
            // Null out the validation classnode
            this.complete();
        }

        private void validateInner() {
            // isInner (shouldn't) return true for static inner classes
            if (!this.classInfo.isProbablyStatic()) {
                throw new InvalidMixinException(MixinInfo.this, "Inner class mixin must be declared static");
            }
        }

        private void validateClassFeatures() {
            CompatibilityLevel compatibilityLevel = MixinEnvironment.getCompatibilityLevel();
            int requiredLanguageFeatures = LanguageFeatures.scan(this.validationClassNode);
            if (requiredLanguageFeatures == 0 || compatibilityLevel.supports(requiredLanguageFeatures)) {
                return;
            }

            int missingFeatures = requiredLanguageFeatures & ~compatibilityLevel.getLanguageFeatures();
            CompatibilityLevel minRequiredLevel = CompatibilityLevel.requiredFor(requiredLanguageFeatures);
            
            throw new InvalidMixinException(MixinInfo.this, String.format(
                    "Unsupported mixin, %s requires the following unsupported language features: %s, these features require compatibility level %s",
                    MixinInfo.this, LanguageFeatures.format(missingFeatures), minRequiredLevel != null ? minRequiredLevel.toString() : "UNKNOWN"));
        }

        private void validateRemappables(List<ClassInfo> targetClasses) {
            // Can't have remappable fields or methods on a multi-target mixin, because after obfuscation the fields will remap to conflicting names
            if (targetClasses.size() > 1) {
                for (FieldNode field : this.validationClassNode.fields) {
                    this.validateRemappable(Shadow.class, field.name, Annotations.getVisible(field, Shadow.class));
                }
                
                for (MethodNode method : this.validationClassNode.methods) {
                    this.validateRemappable(Shadow.class, method.name, Annotations.getVisible(method, Shadow.class));
                    AnnotationNode overwrite = Annotations.getVisible(method, Overwrite.class);
                    if (overwrite != null && ((method.access & Opcodes.ACC_STATIC) == 0 || (method.access & Opcodes.ACC_PUBLIC) == 0)) {
                        throw new InvalidMixinException(MixinInfo.this, "Found @Overwrite annotation on " + method.name + " in " + MixinInfo.this);
                    }
                }
            }
        }
        
        private void validateRemappable(Class<Shadow> annotationClass, String name, AnnotationNode annotation) {
            if (annotation != null && Annotations.getValue(annotation, "remap", Boolean.TRUE)) {
                throw new InvalidMixinException(MixinInfo.this, "Found a remappable @" + annotationClass.getSimpleName() + " annotation on " + name
                        + " in " + this);
            }
        }
        
        /**
         * Read and process any {@link Implements} annotations on the mixin
         */
        void readImplementations(SubType type) {
            this.interfaces.addAll(this.validationClassNode.interfaces);
            this.interfaces.addAll(type.getInterfaces());
            
            AnnotationNode implementsAnnotation = Annotations.getInvisible(this.validationClassNode, Implements.class);
            if (implementsAnnotation == null) {
                return;
            }
            
            List<AnnotationNode> interfaces = Annotations.getValue(implementsAnnotation);
            if (interfaces == null) {
                return;
            }
            
            for (AnnotationNode interfaceNode : interfaces) {
                InterfaceInfo interfaceInfo = InterfaceInfo.fromAnnotation(MixinInfo.this, interfaceNode);
                this.softImplements.add(interfaceInfo);
                this.interfaces.add(interfaceInfo.getInternalName());
                // only add interface if its initial initialisation
                if (!(this instanceof Reloaded)) {
                    this.classInfo.addInterface(interfaceInfo.getInternalName());
                }
            }
        }

        /**
         * Read inner class definitions for the class and locate any synthetic
         * inner classes so that we can add them to the passthrough set in our
         * parent config.
         */
        void readInnerClasses() {
            for (InnerClassNode inner : this.validationClassNode.innerClasses) {
                ClassInfo innerClass = ClassInfo.forName(inner.name);
                if ((inner.outerName != null && inner.outerName.equals(this.classInfo.getName()))
                        || inner.name.startsWith(this.validationClassNode.name + "$")) {
                    if (innerClass.isProbablyStatic() && innerClass.isSynthetic()) {
                        this.syntheticInnerClasses.add(inner.name);
                    } else if (!innerClass.isMixin()) {
                        this.innerClasses.add(inner.name);
                    }
                }
            }
        }
        
        protected void validateChanges(SubType type, List<ClassInfo> targetClasses) {
            type.createPreProcessor(this.validationClassNode).prepare(MixinInfo.this.getExtensions());
        }
    }

    /**
     * State use when hotswap reloading a mixin
     */
    class Reloaded extends State {
        
        /**
         * The previous validation state to compare the changes to
         */
        private final State previous;

        Reloaded(State previous, ClassNode classNode) {
            super(classNode, previous.getClassInfo());
            this.previous = previous;
        }

        /**
         * Validates that the changes are allowed to be made, these restrictions
         * only exits while reloading mixins.
         */
        @Override
        protected void validateChanges(SubType type, List<ClassInfo> targetClasses) {
            if (!this.syntheticInnerClasses.equals(this.previous.syntheticInnerClasses)) {
                throw new MixinReloadException(MixinInfo.this, "Cannot change inner classes");
            }
            if (!this.interfaces.equals(this.previous.interfaces)) {
                throw new MixinReloadException(MixinInfo.this, "Cannot change interfaces");
            }
            if (!new HashSet<InterfaceInfo>(this.softImplements).equals(new HashSet<InterfaceInfo>(this.previous.softImplements))) {
                throw new MixinReloadException(MixinInfo.this, "Cannot change soft interfaces");
            }
            List<ClassInfo> targets = MixinInfo.this.readTargetClasses(this.validationClassNode, true);
            if (!new HashSet<ClassInfo>(targets).equals(new HashSet<ClassInfo>(targetClasses))) {
                throw new MixinReloadException(MixinInfo.this, "Cannot change target classes");
            }
            int priority = MixinInfo.this.readPriority(this.validationClassNode);
            if (priority != MixinInfo.this.getPriority()) {
                throw new MixinReloadException(MixinInfo.this, "Cannot change mixin priority");
            }
        }
    }
    
    /**
     * Mixin sub-type, eg. standard mixin, interface mixin, accessor
     */
    abstract static class SubType {
        
        /**
         * Outer
         */
        protected final MixinInfo mixin;
        
        /**
         * String representation of annotation type, for use in messages
         */
        protected final String annotationType;
        
        /**
         * Target of this mixin subtype must be an interface, false for a class 
         */
        protected final boolean targetMustBeInterface;
        
        /**
         * Detached super, parsed out during validation
         */
        protected boolean detached;
        
        SubType(MixinInfo info, String annotationType, boolean targetMustBeInterface) {
            this.mixin = info;
            this.annotationType = annotationType;
            this.targetMustBeInterface = targetMustBeInterface;
        }
        
        Collection<String> getInterfaces() {
            return Collections.<String>emptyList();
        }

        /**
         * Get whether this mixin is detached super, must call {@link #validate}
         * first
         * 
         * @return true if super is detached
         */
        boolean isDetachedSuper() {
            return this.detached;
        }

        /**
         * True if this mixin class can actually be classloaded
         * 
         * @return whether this subtype is directly classloadable (supports
         *      classloader pinholing)
         */
        boolean isLoadable() {
            return false;
        }

        /**
         * Validate a single target before adding
         * 
         * @param targetName target class name
         * @param targetInfo information about the target class
         */
        void validateTarget(String targetName, ClassInfo targetInfo) {
            boolean targetIsInterface = targetInfo.isInterface();
            if (targetIsInterface != this.targetMustBeInterface) {
                String not = targetIsInterface ? "" : "not ";
                throw new InvalidMixinException(this.mixin, this.annotationType + " target type mismatch: " + targetName
                        + " is " + not + "an interface in " + this);
            }
        }
        
        abstract void validate(State state, List<ClassInfo> targetClasses);

        abstract MixinPreProcessorStandard createPreProcessor(MixinClassNode classNode);

        /**
         * A standard mixin
         */
        static class Standard extends SubType {
            
            Standard(MixinInfo info) {
                super(info, "@Mixin", false);
            }
            
            @Override
            void validate(State state, List<ClassInfo> targetClasses) {
                ClassNode classNode = state.getValidationClassNode();
                
                for (ClassInfo targetClass : targetClasses) {
                    if (classNode.superName.equals(targetClass.getSuperName())) {
                        continue;
                    }
                    
                    if (!targetClass.hasSuperClass(classNode.superName, ClassInfo.Traversal.SUPER)) {
                        ClassInfo superClass = ClassInfo.forName(classNode.superName);
                        if (superClass.isMixin()) {
                            // If superclass is a mixin, check for hierarchy derp
                            for (ClassInfo superTarget : superClass.getTargets()) {
                                if (targetClasses.contains(superTarget)) {
                                    throw new InvalidMixinException(this.mixin, "Illegal hierarchy detected. Derived mixin " + this
                                            + " targets the same class " + superTarget.getClassName() + " as its superclass "
                                            + superClass.getClassName());
                                }
                            }
                        }
                        
                        throw new InvalidMixinException(this.mixin, "Super class '" + classNode.superName.replace('/', '.') + "' of "
                                + this.mixin.getName() + " was not found in the hierarchy of target class '" + targetClass + "'");
                    }
                    
                    this.detached = true;
                }
            }

            @Override
            MixinPreProcessorStandard createPreProcessor(MixinClassNode classNode) {
                return new MixinPreProcessorStandard(this.mixin, classNode);
            }
        }
        
        /**
         * An interface mixin
         */
        static class Interface extends SubType {
            
            Interface(MixinInfo info) {
                super(info, "@Mixin", true);
            }
            
            @Override
            void validate(State state, List<ClassInfo> targetClasses) {
                if (!MixinEnvironment.getCompatibilityLevel().supports(LanguageFeatures.METHODS_IN_INTERFACES)) {
                    throw new InvalidMixinException(this.mixin, "Interface mixin not supported in current enviromnment");
                }
                
                ClassNode classNode = state.getValidationClassNode();
                
                if (!"java/lang/Object".equals(classNode.superName)) {
                    throw new InvalidMixinException(this.mixin, "Super class of " + this + " is invalid, found " 
                            + classNode.superName.replace('/', '.'));
                }
            }
            
            @Override
            MixinPreProcessorStandard createPreProcessor(MixinClassNode classNode) {
                return new MixinPreProcessorInterface(this.mixin, classNode);
            }
            
        }
        
        /**
         * An accessor mixin
         */
        static class Accessor extends SubType {
            
            private final Collection<String> interfaces = new ArrayList<String>();

            Accessor(MixinInfo info) {
                super(info, "@Mixin", false);
                this.interfaces.add(info.getClassRef());
            }
            
            @Override
            boolean isLoadable() {
                return true;
            }
            
            @Override
            Collection<String> getInterfaces() {
                return this.interfaces;
            }
            
            @Override
            void validateTarget(String targetName, ClassInfo targetInfo) {
                boolean targetIsInterface = targetInfo.isInterface();
                if (targetIsInterface && !MixinEnvironment.getCompatibilityLevel().supports(LanguageFeatures.METHODS_IN_INTERFACES)) {
                    throw new InvalidMixinException(this.mixin, "Accessor mixin targetting an interface is not supported in current enviromnment");
                }
            }
            
            @Override
            void validate(State state, List<ClassInfo> targetClasses) {
                ClassNode classNode = state.getValidationClassNode();
                
                if (!"java/lang/Object".equals(classNode.superName)) {
                    throw new InvalidMixinException(this.mixin, "Super class of " + this + " is invalid, found " 
                            + classNode.superName.replace('/', '.'));
                }
            }
            
            @Override
            MixinPreProcessorStandard createPreProcessor(MixinClassNode classNode) {
                return new MixinPreProcessorAccessor(this.mixin, classNode);
            }
        }

        static SubType getTypeFor(MixinInfo mixin) {
            Variant variant = MixinInfo.getVariant(mixin.getClassInfo());
            switch (variant) {
                case STANDARD:
                    return new SubType.Standard(mixin);
                case INTERFACE:
                    return new SubType.Interface(mixin);
                case ACCESSOR:
                    return new SubType.Accessor(mixin);
                default:
                    throw new IllegalStateException("Unsupported Mixin variant " + variant + " for " + mixin);
            }
        }

    }
    
    /**
     * Handle for a declared target on a mixin.
     */
    static final class DeclaredTarget {
        
        final String name;
        
        final boolean isPrivate;

        private DeclaredTarget(String name, boolean isPrivate) {
            this.name = name;
            this.isPrivate = isPrivate;
        }
        
        @Override
        public String toString() {
            return this.name;
        }

        static DeclaredTarget of(Object target, MixinInfo info) {
            if (target instanceof String) {
                String remappedName = info.remapClassName((String)target);
                return remappedName != null ? new DeclaredTarget(remappedName, true) : null;
            } else if (target instanceof Type) {
                return new DeclaredTarget(((Type)target).getClassName(), false);
            }
            return null;
        }
        
    }
    
    /**
     * Global order of mixin infos, used to determine ordering between mixins
     * with equivalent priority
     */
    static int mixinOrder = 0;
    
    /**
     * Logger
     */
    private final transient ILogger logger = MixinService.getService().getLogger("mixin");
    
    /**
     * Profiler 
     */
    private final transient Profiler profiler = Profiler.getProfiler("mixin");
    
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
     * Mixin priority, read from the {@link Mixin} annotation on the mixin class
     */
    private final int priority;
    
    /**
     * True if the mixin is annotated with {@link Pseudo} 
     */
    private final boolean virtual;
    
    /**
     * Mixin targets, read from the {@link Mixin} annotation on the mixin class
     * but not yet parsed in the current environment
     */
    private final transient List<DeclaredTarget> declaredTargets;
    
    /**
     * Mixin targets, read from the {@link Mixin} annotation on the mixin class
     */
    private final transient List<ClassInfo> targetClasses = new ArrayList<ClassInfo>();
    
    /**
     * Names of target classes 
     */
    private final List<String> targetClassNames = new ArrayList<String>();
    
    /**
     * Intrinsic order (for sorting mixins with identical priority)
     */
    private final transient int order = MixinInfo.mixinOrder++;
    
    /**
     * Service 
     */
    private final transient IMixinService service;

    /**
     * Configuration plugin
     */
    private final transient PluginHandle plugin;

    /**
     * The environment phase in which this mixin was initialised
     */
    private final transient Phase phase;
    
    /**
     * Cached class info 
     */
    private final transient ClassInfo info;
    
    /**
     * Mixin type 
     */
    private final transient SubType type;
    
    /**
     * Strict target checks enabled
     */
    private final transient boolean strict;
    
    /**
     * Transformer extensions manager 
     */
    private final transient Extensions extensions;

    /**
     * Holds state that currently is not fully initialised or validated
     */
    private transient State pendingState;

    /**
     * Holds the current validated state
     */
    private transient State state;

    /**
     * Internal ctor, called by {@link MixinConfig}
     * 
     * @param parent configuration which owns this mixin, the parent
     * @param name name of this mixin (class name stub)
     * @param plugin mixin config companion plugin handle
     * @param ignorePlugin true to prevent the plugin from filtering targets of
     *      this mixin
     */
    MixinInfo(IMixinService service, MixinConfig parent, String name, PluginHandle plugin, boolean ignorePlugin, Extensions extensions) {
        this.service = service;
        this.parent = parent;
        this.name = name;
        this.className = parent.getMixinPackage() + name;
        this.plugin = plugin;
        this.phase = parent.getEnvironment().getPhase();
        this.strict = parent.getEnvironment().getOption(Option.DEBUG_TARGETS);
        this.extensions = extensions;
        
        // Read the class bytes and transform
        try {
            ClassNode mixinClassNode = this.loadMixinClass(this.className);
            this.pendingState = new State(mixinClassNode);
            this.info = this.pendingState.getClassInfo();
            this.type = SubType.getTypeFor(this);
        } catch (InvalidMixinException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidMixinException(this, ex.getMessage(), ex);
        }
        
        if (!this.type.isLoadable()) {
            // Inject the mixin class name into the LaunchClassLoader's invalid
            // classes set so that any classes referencing the mixin directly will
            // cause the game to crash
            IClassTracker tracker = this.service.getClassTracker();
            if (tracker != null) {
                tracker.registerInvalidClass(this.className);
            }
        }
        
        // Read the class bytes and transform
        try {
            this.priority = this.readPriority(this.pendingState.getClassNode());
            this.virtual = this.readPseudo(this.pendingState.getValidationClassNode());
            this.declaredTargets = this.readDeclaredTargets(this.pendingState.getValidationClassNode(), ignorePlugin);
        } catch (InvalidMixinException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidMixinException(this, ex);
        }
    }

    /**
     * Parse the declared targets from the annotation into ClassInfo instances
     * and perform initial validation of each target
     */
    void parseTargets() {
        try {
            this.targetClasses.addAll(this.readTargetClasses(this.declaredTargets));
            this.targetClassNames.addAll(Lists.transform(this.targetClasses, Functions.toStringFunction()));
        } catch (InvalidMixinException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidMixinException(this, ex);
        }
    }

    /**
     * Run validation pass
     */
    void validate() {
        if (this.pendingState == null) {
            throw new IllegalStateException("No pending validation state for " + this);
        }
        
        try {
            this.pendingState.validate(this.type, this.targetClasses);
            this.state = this.pendingState;
        } finally {
            this.pendingState = null;
        }
    }

    /**
     * Read the declared target class names from the {@link Mixin} annotation
     * 
     * @param classNode mixin classnode
     * @param ignorePlugin true to suppress plugin filtering targets
     * @return target class list read from classNode
     */
    protected List<DeclaredTarget> readDeclaredTargets(MixinClassNode classNode, boolean ignorePlugin) {
        if (classNode == null) {
            return Collections.<DeclaredTarget>emptyList();
        }
        
        AnnotationNode mixin = Annotations.getInvisible(classNode, Mixin.class);
        if (mixin == null) {
            throw new InvalidMixinException(this, String.format("The mixin '%s' is missing an @Mixin annotation", this.className));
        }
        
        IClassTracker tracker = this.service.getClassTracker();
        List<DeclaredTarget> declaredTargets = new ArrayList<DeclaredTarget>();
        for (Object target : this.readTargets(mixin)) {
            DeclaredTarget declaredTarget = DeclaredTarget.of(target, this);
            if (declaredTarget == null) {
                continue;
            }
            if (tracker != null && tracker.isClassLoaded(declaredTarget.name) && !this.isReloading()) {
                String message = String.format("Critical problem: %s target %s was loaded too early.", this, declaredTarget.name);
                if (this.parent.isRequired()) {
                    throw new MixinTargetAlreadyLoadedException(this, message, declaredTarget.name);
                }
                this.logger.error(message);
            }
            
            if (this.shouldApplyMixin(ignorePlugin, declaredTarget.name)) {
                declaredTargets.add(declaredTarget);
            }
        }
        return declaredTargets;
    }

    /**
     * Combine the public and private mixin targets from the supplied annotation
     * and return them as an interable collection
     * 
     * @param mixin mixin annotation
     * @return target list
     */
    private Iterable<Object> readTargets(AnnotationNode mixin) {
        Iterable<Object> publicTargets = Annotations.getValue(mixin, "value");
        Iterable<Object> privateTargets = Annotations.getValue(mixin, "targets");
        if (publicTargets == null && privateTargets == null) {
            return Collections.<Object>emptyList();
        }
        if (publicTargets == null) {
            return privateTargets;
        }
        return privateTargets == null ? publicTargets : Iterables.concat(publicTargets, privateTargets);
    }

    /**
     * Check whether this mixin should apply to the specified taret
     * 
     * @param ignorePlugin true to ignore the config plugin
     * @param targetName target class name
     * @return true if the mixin should be a pplied
     */
    private boolean shouldApplyMixin(boolean ignorePlugin, String targetName) {
        Section pluginTimer = this.profiler.begin("plugin");
        boolean result = ignorePlugin || this.plugin.shouldApplyMixin(targetName, this.className);
        pluginTimer.end();
        return result;
    }

    /**
     * Read and parse target classes from the supplied class node
     * 
     * @param classNode class node to parse
     * @param ignorePlugin true to ignore the config plugin when deciding
     *      whether to apply declared targets
     * @return new list of target classes
     */
    List<ClassInfo> readTargetClasses(MixinClassNode classNode, boolean ignorePlugin) {
        return this.readTargetClasses(this.readDeclaredTargets(classNode, ignorePlugin));
    }

    private List<ClassInfo> readTargetClasses(List<DeclaredTarget> declaredTargets) throws InvalidMixinException {
        List<ClassInfo> targetClasses = new ArrayList<ClassInfo>();
        for (DeclaredTarget target : declaredTargets) {
            ClassInfo targetClass = this.getTargetClass(target);
            if (targetClass != null) {
                targetClasses.add(targetClass);
                targetClass.addMixin(this);
            }
        }
        return targetClasses;
    }

    private ClassInfo getTargetClass(DeclaredTarget target) throws InvalidMixinException {
        ClassInfo targetInfo = ClassInfo.forName(target.name);
        if (targetInfo == null) {
            if (this.isVirtual()) {
                this.logger.debug("Skipping virtual target {} for {}", target.name, this);
            } else {
                this.handleTargetError(String.format("@Mixin target %s was not found %s", target.name, this), false);
            }
            return null;
        }
        this.type.validateTarget(target.name, targetInfo);
        if (target.isPrivate && targetInfo.isReallyPublic() && !this.isVirtual()) {
            this.handleTargetError(String.format("@Mixin target %s is public in %s and should be specified in value", target.name, this), true);
        }
        return targetInfo;
    }

    private void handleTargetError(String message, boolean verboseOnly) {
        if (this.strict) {
            this.logger.error(message);
            throw new InvalidMixinException(this, message);
        }
        this.logger.log(verboseOnly && !this.parent.isVerboseLogging() ? Level.DEBUG : Level.WARN, message);
    }

    /**
     * Read the priority from the {@link Mixin} annotation
     * 
     * @param classNode mixin classnode
     * @return priority read from classNode
     */
    protected int readPriority(ClassNode classNode) {
        if (classNode == null) {
            return this.parent.getDefaultMixinPriority();
        }
        
        AnnotationNode mixin = Annotations.getInvisible(classNode, Mixin.class);
        if (mixin == null) {
            throw new InvalidMixinException(this, String.format("The mixin '%s' is missing an @Mixin annotation", this.className));
        }
        
        Integer priority = Annotations.getValue(mixin, "priority");
        return priority == null ? this.parent.getDefaultMixinPriority() : priority.intValue();
    }

    protected boolean readPseudo(ClassNode classNode) {
        return Annotations.getInvisible(classNode, Pseudo.class) != null;
    }

    private boolean isReloading() {
        return this.pendingState instanceof Reloaded;
    }
    
    String remapClassName(String className) {
        return this.parent.remapClassName(this.getClassRef(), className);
    }

    public boolean hasDeclaredTarget(String targetClass) {
        for (DeclaredTarget declaredTarget : this.declaredTargets) {
            if (targetClass.equals(declaredTarget.name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Current state, either the validated state or the uninitialised state if
     * the mixin is initialising for the first time. Should never return null.
     */
    private State getState() {
        return this.state != null ? this.state : this.pendingState;
    }

    /**
     * Get the ClassInfo for the mixin class
     */
    ClassInfo getClassInfo() {
        return this.info;
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
    MixinConfig getParent() {
        return this.parent;
    }
    
    /**
     * Get the mixin priority
     */
    @Override
    public int getPriority() {
        return this.priority;
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
        return this.getClassInfo().getName();
    }

    /**
     * Get the class bytecode
     */
    @Override
    public byte[] getClassBytes() {
//        return this.getState().getClassBytes();
        throw new RuntimeException("NO");
    }
    
    /**
     * True if the superclass of the mixin is <b>not</b> the direct superclass
     * of one or more targets
     */
    @Override
    public boolean isDetachedSuper() {
        return this.getState().isDetachedSuper();
    }

    /**
     * True if this mixin is decorated with {@link Unique}
     */
    public boolean isUnique() {
        return this.getState().isUnique();
    }
    
    /**
     * True if this mixin is decorated with {@link Pseudo}
     */
    public boolean isVirtual() {
        return this.virtual;
    }
    
    /**
     * True if the mixin class is actually class-loadable
     */
    public boolean isAccessor() {
        return this.type instanceof SubType.Accessor;
    }

    /**
     * True if the mixin class is actually class-loadable
     */
    public boolean isLoadable() {
        return this.type.isLoadable();
    }
    
    /**
     * True if the parent mixin config is marked as required
     */
    public boolean isRequired() {
        return this.parent.isRequired();
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
    public MixinClassNode getClassNode(int flags) {
        return this.getState().createClassNode(flags);
    }
    
    /**
     * Get the target class names as declared for this mixin
     */
    List<String> getDeclaredTargetClasses() {
        return Collections.<String>unmodifiableList(Lists.transform(this.declaredTargets, Functions.toStringFunction()));
    }
    
    /**
     * Get the target class names for this mixin
     */
    @Override
    public List<String> getTargetClasses() {
        return Collections.<String>unmodifiableList(this.targetClassNames);
    }
    
    /**
     * Get the soft implementations for this mixin
     */
    List<InterfaceInfo> getSoftImplements() {
        return Collections.<InterfaceInfo>unmodifiableList(this.getState().getSoftImplements());
    }

    /**
     * Get the synthetic inner classes for this mixin
     */
    Set<String> getSyntheticInnerClasses() {
        return Collections.<String>unmodifiableSet(this.getState().getSyntheticInnerClasses());
    }
    
    /**
     * Get the user-defined inner classes for this mixin
     */
    Set<String> getInnerClasses() {
        return Collections.<String>unmodifiableSet(this.getState().getInnerClasses());
    }
    
    /**
     * Get the target class list for this mixin
     */
    List<ClassInfo> getTargets() {
        return Collections.<ClassInfo>unmodifiableList(this.targetClasses);
    }

    /**
     * Get all interfaces for this mixin
     * 
     * @return mixin interfaces
     */
    Set<String> getInterfaces() {
        return this.getState().getInterfaces();
    }
    
    /**
     * Get transformer extensions
     */
    Extensions getExtensions() {
        return this.extensions;
    }

    /**
     * Get a new mixin target context object for the specified target
     * 
     * @param target target class context
     * @return new context
     */
    MixinTargetContext createContextFor(TargetClassContext target) {
        MixinClassNode classNode = this.getClassNode(ClassReader.EXPAND_FRAMES);
        Section preTimer = this.profiler.begin("pre");
        MixinTargetContext context = this.type.createPreProcessor(classNode).prepare(this.extensions).createContextFor(target);
        preTimer.end();
        return context;
    }

    /**
     * Load the mixin class bytes
     * 
     * @param mixinClassName mixin class name
     * @return mixin bytecode
     * @throws ClassNotFoundException if the mixin bytes could not be found
     */
    private ClassNode loadMixinClass(String mixinClassName) throws ClassNotFoundException {
        ClassNode classNode = null;

        try {
            IClassTracker tracker = this.service.getClassTracker();
            if (tracker != null) {
                String restrictions = tracker.getClassRestrictions(mixinClassName);
                if (restrictions.length() > 0) {
                    this.logger.error("Classloader restrictions [{}] encountered loading {}, name: {}", restrictions, this, mixinClassName);
                }
            }
            classNode = this.service.getBytecodeProvider().getClassNode(mixinClassName, true);
        } catch (ClassNotFoundException ex) {
            throw new ClassNotFoundException(String.format("The specified mixin '%s' was not found", mixinClassName));
        } catch (IOException ex) {
            this.logger.warn("Failed to load mixin {}, the specified mixin will not be applied", mixinClassName);
            throw new InvalidMixinException(this, "An error was encountered whilst loading the mixin class", ex);
        }
        
        return classNode;
    }

    /**
     * Updates this mixin with new bytecode
     *
     * @param classNode New bytecode
     */
    void reloadMixin(ClassNode classNode) {
        if (this.pendingState != null) {
            throw new IllegalStateException("Cannot reload mixin while it is initialising");
        }
        this.pendingState = new Reloaded(this.state, classNode);
        this.validate();
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
    public void preApply(String transformedName, ClassNode targetClass) throws Exception {
        if (this.plugin.isAvailable()) {
            Section pluginTimer = this.profiler.begin("plugin");
            try {
                this.plugin.preApply(transformedName, targetClass, this.className, this);
            } finally {
                pluginTimer.end();
            }
        }
    }

    /**
     * Called immediately after the mixin is applied to targetClass
     */
    public void postApply(String transformedName, ClassNode targetClass) throws Exception {
        if (this.plugin.isAvailable()) {
            Section pluginTimer = this.profiler.begin("plugin");
            try {
                this.plugin.postApply(transformedName, targetClass, this.className, this);
            } finally {
                pluginTimer.end();
            }
        }
        
        this.parent.postApply(transformedName, targetClass);
        this.info.addAppliedMixin(this);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("%s:%s", this.parent.getName(), this.name);
    }
    
    static Variant getVariant(ClassNode classNode) {
        return MixinInfo.getVariant(ClassInfo.fromClassNode(classNode));
    }
    
    static Variant getVariant(ClassInfo classInfo) {
//        if (ProxyInfo.isProxy(classInfo)) {
//            return Variant.PROXY;
//        }
        
        if (!classInfo.isInterface()) {
            return Variant.STANDARD;
        }
        
        boolean containsNonAccessorMethod = false;
        for (Method method : classInfo.getMethods()) {
            containsNonAccessorMethod |= (!method.isAccessor() && !method.isSynthetic());
        }
        
        if (containsNonAccessorMethod) {
            // If the mixin contains any other methods, treat it as a regular interface mixin
            return Variant.INTERFACE;
        }
        
        // The mixin contains no non-accessor methods, so we can treat it as an accessor
        return Variant.ACCESSOR;
    }
    
}
