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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.MethodVisitor;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.FieldNode;
import org.spongepowered.asm.lib.tree.InnerClassNode;
import org.spongepowered.asm.lib.tree.MethodNode;
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
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.Surrogate;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Method;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.mixin.transformer.throwables.MixinReloadException;
import org.spongepowered.asm.mixin.transformer.throwables.MixinTargetAlreadyLoadedException;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.perf.Profiler;
import org.spongepowered.asm.util.perf.Profiler.Section;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;

/**
 * Runtime information bundle about a mixin
 */
class MixinInfo implements Comparable<MixinInfo>, IMixinInfo {
    
    /**
     * A MethodNode in a mixin
     */
    class MixinMethodNode extends MethodNode {
        
        private final String originalName;
        
        public MixinMethodNode(int access, String name, String desc, String signature, String[] exceptions) {
            super(Opcodes.ASM5, access, name, desc, signature, exceptions);
            this.originalName = name;
        }
        
        @Override
        public String toString() {
            return String.format("%s%s", this.originalName, this.desc);
        }
        
        public String getOriginalName() {
            return this.originalName;
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
        
        public IMixinInfo getOwner() {
            return MixinInfo.this;
        }

    }
    
    /**
     * ClassNode for a MixinInfo
     */
    class MixinClassNode extends ClassNode {
        
        public final List<MixinMethodNode> mixinMethods;
        
        public MixinClassNode(MixinInfo mixin) {
            this(Opcodes.ASM5);
        }

        @SuppressWarnings("unchecked")
        public MixinClassNode(int api) {
            super(api);
            this.mixinMethods = (List<MixinMethodNode>)(Object)this.methods;
        }

        public MixinInfo getMixin() {
            return MixinInfo.this;
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
         * Mixin bytes (read once, generate tree on demand)
         */
        private byte[] mixinBytes;

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
        protected MixinClassNode classNode;

        State(byte[] mixinBytes) {
            this(mixinBytes, null);
        }

        State(byte[] mixinBytes, ClassInfo classInfo) {
            this.mixinBytes = mixinBytes;
            this.connect();
            this.classInfo = classInfo != null ? classInfo : ClassInfo.fromClassNode(this.getClassNode());
        }

        private void connect() {
            this.classNode = this.createClassNode(0);
        }

        private void complete() {
            this.classNode = null;
        }

        ClassInfo getClassInfo() {
            return this.classInfo;
        }

        byte[] getClassBytes() {
            return this.mixinBytes;
        }
        
        MixinClassNode getClassNode() {
            return this.classNode;
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
            MixinClassNode classNode = new MixinClassNode(MixinInfo.this);
            ClassReader classReader = new ClassReader(this.mixinBytes);
            classReader.accept(classNode, flags);
            return classNode;
        }

        /**
         * Performs pre-flight checks on the mixin
         * 
         * @param type Mixin Type
         * @param targetClasses Mixin's target classes
         */
        void validate(SubType type, List<ClassInfo> targetClasses) {
            MixinPreProcessorStandard preProcessor = type.createPreProcessor(this.getClassNode()).prepare();
            for (ClassInfo target : targetClasses) {
                preProcessor.conform(target);
            }
            
            type.validate(this, targetClasses);

            this.detachedSuper = type.isDetachedSuper();
            this.unique = Annotations.getVisible(this.getClassNode(), Unique.class) != null;

            // Pre-flight checks
            this.validateInner();
            this.validateClassVersion();
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

        private void validateClassVersion() {
            if (this.classNode.version > MixinEnvironment.getCompatibilityLevel().classVersion()) {
                String helpText = ".";
                for (CompatibilityLevel level : CompatibilityLevel.values()) {
                    if (level.classVersion() >= this.classNode.version) {
                        helpText = String.format(". Mixin requires compatibility level %s or above.", level.name()); 
                    }
                }
                
                throw new InvalidMixinException(MixinInfo.this, "Unsupported mixin class version " + this.classNode.version + helpText);
            }
        }

        private void validateRemappables(List<ClassInfo> targetClasses) {
            // Can't have remappable fields or methods on a multi-target mixin, because after obfuscation the fields will remap to conflicting names
            if (targetClasses.size() > 1) {
                for (FieldNode field : this.classNode.fields) {
                    this.validateRemappable(Shadow.class, field.name, Annotations.getVisible(field, Shadow.class));
                }
                
                for (MethodNode method : this.classNode.methods) {
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
            this.interfaces.addAll(this.classNode.interfaces);
            this.interfaces.addAll(type.getInterfaces());
            
            AnnotationNode implementsAnnotation = Annotations.getInvisible(this.classNode, Implements.class);
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
            for (InnerClassNode inner : this.classNode.innerClasses) {
                ClassInfo innerClass = ClassInfo.forName(inner.name);
                if ((inner.outerName != null && inner.outerName.equals(this.classInfo.getName()))
                        || inner.name.startsWith(this.classNode.name + "$")) {
                    if (innerClass.isProbablyStatic() && innerClass.isSynthetic()) {
                        this.syntheticInnerClasses.add(inner.name);
                    } else {
                        this.innerClasses.add(inner.name);
                    }
                }
            }
        }
        
        protected void validateChanges(SubType type, List<ClassInfo> targetClasses) {
            type.createPreProcessor(this.classNode).prepare();
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

        Reloaded(State previous, byte[] mixinBytes) {
            super(mixinBytes, previous.getClassInfo());
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
            List<ClassInfo> targets = MixinInfo.this.readTargetClasses(this.classNode, true);
            if (!new HashSet<ClassInfo>(targets).equals(new HashSet<ClassInfo>(targetClasses))) {
                throw new MixinReloadException(MixinInfo.this, "Cannot change target classes");
            }
            int priority = MixinInfo.this.readPriority(this.classNode);
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
                ClassNode classNode = state.getClassNode();
                
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
                if (!MixinEnvironment.getCompatibilityLevel().supportsMethodsInInterfaces()) {
                    throw new InvalidMixinException(this.mixin, "Interface mixin not supported in current enviromnment");
                }
                
                ClassNode classNode = state.getClassNode();
                
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
                if (targetIsInterface && !MixinEnvironment.getCompatibilityLevel().supportsMethodsInInterfaces()) {
                    throw new InvalidMixinException(this.mixin, "Accessor mixin targetting an interface is not supported in current enviromnment");
                }
            }
            
            @Override
            void validate(State state, List<ClassInfo> targetClasses) {
                ClassNode classNode = state.getClassNode();
                
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
            if (!mixin.getClassInfo().isInterface()) {
                return new SubType.Standard(mixin);
            }
            
            boolean containsNonAccessorMethod = false;
            for (Method method : mixin.getClassInfo().getMethods()) {
                containsNonAccessorMethod |= !method.isAccessor();
            }
            
            if (containsNonAccessorMethod) {
                // If the mixin contains any other methods, treat it as a regular interface mixin
                return new SubType.Interface(mixin);
            }
            
            // The mixin contains no non-accessor methods, so we can treat it as an accessor
            return new SubType.Accessor(mixin);
        }

    }
    
    /**
     * Mixin service
     */
    private static final IMixinService classLoaderUtil = MixinService.getService();

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
     * Profiler 
     */
    private final transient Profiler profiler = MixinEnvironment.getProfiler();
    
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
     * Service 
     */
    private final transient IMixinService service;

    /**
     * Configuration plugin
     */
    private final transient IMixinConfigPlugin plugin;

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
     * @param runTransformers true if this mixin should run transformers on its
     *      bytecode when loading
     * @param plugin mixin config companion plugin, may be null
     * @param suppressPlugin true to suppress the plugin from filtering targets
     *      of this mixin
     */
    MixinInfo(IMixinService service, MixinConfig parent, String name, boolean runTransformers, IMixinConfigPlugin plugin, boolean suppressPlugin) {
        this.service = service;
        this.parent = parent;
        this.name = name;
        this.className = parent.getMixinPackage() + name;
        this.plugin = plugin;
        this.phase = parent.getEnvironment().getPhase();
        this.strict = parent.getEnvironment().getOption(Option.DEBUG_TARGETS);
        
        // Read the class bytes and transform
        try {
            byte[] mixinBytes = this.loadMixinClass(this.className, runTransformers);
            this.pendingState = new State(mixinBytes);
            this.info = this.pendingState.getClassInfo();
            this.type = SubType.getTypeFor(this);
        } catch (InvalidMixinException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidMixinException(this, ex);
        }
        
        if (!this.type.isLoadable()) {
            // Inject the mixin class name into the LaunchClassLoader's invalid
            // classes set so that any classes referencing the mixin directly will
            // cause the game to crash
            MixinInfo.classLoaderUtil.registerInvalidClass(this.className);
        }
        
        // Read the class bytes and transform
        try {
            this.priority = this.readPriority(this.pendingState.getClassNode());
            this.virtual = this.readPseudo(this.pendingState.getClassNode());
            this.targetClasses = this.readTargetClasses(this.pendingState.getClassNode(), suppressPlugin);
            this.targetClassNames = Collections.<String>unmodifiableList(Lists.transform(this.targetClasses, Functions.toStringFunction()));
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
     * Read the target class names from the {@link Mixin} annotation
     * 
     * @param classNode mixin classnode
     * @param suppressPlugin true to suppress plugin filtering targets
     * @return target class list read from classNode
     */
    protected List<ClassInfo> readTargetClasses(MixinClassNode classNode, boolean suppressPlugin) {
        if (classNode == null) {
            return Collections.<ClassInfo>emptyList();
        }
        
        AnnotationNode mixin = Annotations.getInvisible(classNode, Mixin.class);
        if (mixin == null) {
            throw new InvalidMixinException(this, String.format("The mixin '%s' is missing an @Mixin annotation", this.className));
        }
        
        List<ClassInfo> targets = new ArrayList<ClassInfo>();
        List<org.spongepowered.asm.lib.Type> publicTargets = Annotations.getValue(mixin, "value");
        List<String> privateTargets = Annotations.getValue(mixin, "targets");

        if (publicTargets != null) {
            this.readTargets(targets, Lists.transform(publicTargets, new Function<org.spongepowered.asm.lib.Type, String>() {
                @Override
                public String apply(org.spongepowered.asm.lib.Type input) {
                    return input.getClassName();
                }
            }), suppressPlugin, false);
        }
        
        if (privateTargets != null) {
            this.readTargets(targets, Lists.transform(privateTargets, new Function<String, String>() {
                @Override
                public String apply(String input) {
                    return MixinInfo.this.getParent().remapClassName(MixinInfo.this.getClassRef(), input);
                }
            }), suppressPlugin, true);
        }
        
        return targets;
    }

    /**
     * Reads a target list into the outTargets list
     */
    private void readTargets(Collection<ClassInfo> outTargets, Collection<String> inTargets, boolean suppressPlugin, boolean checkPublic) {
        for (String targetRef : inTargets) {
            String targetName = targetRef.replace('/', '.');
            if (MixinInfo.classLoaderUtil.isClassLoaded(targetName) && !this.isReloading()) {
                String message = String.format("Critical problem: %s target %s was already transformed.", this, targetName);
                if (this.parent.isRequired()) {
                    throw new MixinTargetAlreadyLoadedException(this, message, targetName);
                }
                this.logger.error(message);
            }
            
            if (this.shouldApplyMixin(suppressPlugin, targetName)) {
                ClassInfo targetInfo = this.getTarget(targetName, checkPublic);
                if (targetInfo != null && !outTargets.contains(targetInfo)) {
                    outTargets.add(targetInfo);
                    targetInfo.addMixin(this);
                }
            }
        }
    }

    private boolean shouldApplyMixin(boolean suppressPlugin, String targetName) {
        Section pluginTimer = this.profiler.begin("plugin");
        boolean result = this.plugin == null || suppressPlugin || this.plugin.shouldApplyMixin(targetName, this.className);
        pluginTimer.end();
        return result;
    }

    private ClassInfo getTarget(String targetName, boolean checkPublic) throws InvalidMixinException {
        ClassInfo targetInfo = ClassInfo.forName(targetName);
        if (targetInfo == null) {
            if (this.isVirtual()) {
                this.logger.debug("Skipping virtual target {} for {}", targetName, this);
            } else {
                this.handleTargetError(String.format("@Mixin target %s was not found %s", targetName, this));
            }
            return null;
        }
        this.type.validateTarget(targetName, targetInfo);
        if (checkPublic && targetInfo.isPublic() && !this.isVirtual()) {
            this.handleTargetError(String.format("@Mixin target %s is public in %s and should be specified in value", targetName, this));
        }
        return targetInfo;
    }

    private void handleTargetError(String message) {
        if (this.strict) {
            this.logger.error(message);
            throw new InvalidMixinException(this, message);
        }
        this.logger.warn(message);
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
        return this.getState().getClassBytes();
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
     * Get a new mixin target context object for the specified target
     * 
     * @param target target class context
     * @return new context
     */
    MixinTargetContext createContextFor(TargetClassContext target) {
        MixinClassNode classNode = this.getClassNode(ClassReader.EXPAND_FRAMES);
        Section preTimer = this.profiler.begin("pre");
        MixinTargetContext preProcessor = this.type.createPreProcessor(classNode).prepare().createContextFor(target);
        preTimer.end();
        return preProcessor;
    }

    /**
     * Load the mixin class bytes
     * 
     * @param mixinClassName mixin class name
     * @param runTransformers true to run transformers on the loaded bytecode
     * @return mixin bytecode
     * @throws ClassNotFoundException if the mixin bytes could not be found
     */
    private byte[] loadMixinClass(String mixinClassName, boolean runTransformers) throws ClassNotFoundException {
        byte[] mixinBytes = null;

        try {
            if (runTransformers) {
                String restrictions = this.service.getClassRestrictions(mixinClassName);
                if (restrictions.length() > 0) {
                    this.logger.error("Classloader restrictions [{}] encountered loading {}, name: {}", restrictions, this, mixinClassName);
                }
            }
            mixinBytes = this.service.getBytecodeProvider().getClassBytes(mixinClassName, runTransformers);
            
        } catch (ClassNotFoundException ex) {
            throw new ClassNotFoundException(String.format("The specified mixin '%s' was not found", mixinClassName));
        } catch (IOException ex) {
            this.logger.warn("Failed to load mixin {}, the specified mixin will not be applied", mixinClassName);
            throw new InvalidMixinException(this, "An error was encountered whilst loading the mixin class", ex);
        }
        
        return mixinBytes;
    }

    /**
     * Updates this mixin with new bytecode
     *
     * @param mixinBytes New bytecode
     */
    void reloadMixin(byte[] mixinBytes) {
        if (this.pendingState != null) {
            throw new IllegalStateException("Cannot reload mixin while it is initialising");
        }
        this.pendingState = new Reloaded(this.state, mixinBytes);
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
    public void preApply(String transformedName, ClassNode targetClass) {
        if (this.plugin != null) {
            Section pluginTimer = this.profiler.begin("plugin");
            this.plugin.preApply(transformedName, targetClass, this.className, this);
            pluginTimer.end();
        }
    }

    /**
     * Called immediately after the mixin is applied to targetClass
     */
    public void postApply(String transformedName, ClassNode targetClass) {
        if (this.plugin != null) {
            Section pluginTimer = this.profiler.begin("plugin");
            this.plugin.postApply(transformedName, targetClass, this.className, this);
            pluginTimer.end();
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
    
}
