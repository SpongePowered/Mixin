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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.spongepowered.asm.mixin.MixinEnvironment.CompatibilityLevel;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.MixinException;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.util.ASMHelper;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * Runtime information bundle about a mixin
 */
class MixinInfo extends TreeInfo implements Comparable<MixinInfo>, IMixinInfo {

    /**
     * State that can be reloaded.
     */
    private class ValidationState {
        
        /**
         * Mixin bytes (read once, generate tree on demand)
         */
        byte[] mixinBytes;

        /**
         * All interfaces implemented by this mixin, including soft
         * implementations
         */
        final Set<String> interfaces = new HashSet<String>();

        /**
         * Interfaces soft-implemented using {@link Implements}
         */
        final List<InterfaceInfo> softImplements = new ArrayList<InterfaceInfo>();

        /**
         * Synthetic inner classes
         */
        final Set<String> syntheticInnerClasses = new HashSet<String>();

        /**
         * Mixin ClassInfo
         */
        final ClassInfo classInfo;

        /**
         * True if the superclass of the mixin is <b>not</b> the direct
         * superclass of one or more targets
         */
        boolean detachedSuper;

        /**
         * Initial ClassNode created for mixin validation, not used for actual
         * application
         */
        ClassNode classNode;

        ValidationState(byte[] mixinBytes, ClassInfo classInfo) {
            this.mixinBytes = mixinBytes;
            this.classNode = this.getClassNode(0);
            this.classInfo = classInfo;
        }

        ValidationState(byte[] mixinBytes) {
            this.mixinBytes = mixinBytes;
            this.classNode = this.getClassNode(0);
            this.classInfo = ClassInfo.fromClassNode(this.classNode);
        }

        /**
         * Gets a new tree from the bytecode
         *
         * @param flags Flags passed into classReader
         * @return Tree representing the bytecode
         */
        ClassNode getClassNode(int flags) {
            MixinClassNode classNode = new MixinClassNode(MixinInfo.this);
            ClassReader classReader = new ClassReader(this.mixinBytes);
            classReader.accept(classNode, flags);
            return classNode;
        }
    }

    private class ReloadedState extends ValidationState {
        
        /**
         * The previous validation state to compare the changes to
         */
        private final ValidationState previous;

        ReloadedState(ValidationState previous, byte[] mixinBytes) {
            super(mixinBytes, previous.classInfo);
            this.previous = previous;
        }

        /**
         * Validates that the changes are allowed to be made, these restrictions
         * only exits while reloading mixins.
         */
        void validateChanges() {
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
            if (!new HashSet<ClassInfo>(targets).equals(new HashSet<ClassInfo>(MixinInfo.this.getTargets0()))) {
                throw new MixinReloadException(MixinInfo.this, "Cannot change target classes");
            }
            int priority = MixinInfo.this.readPriority(this.classNode);
            if (priority != MixinInfo.this.getPriority()) {
                throw new MixinReloadException(MixinInfo.this, "Cannot change mixin priority");
            }
        }
    }

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
     * True if this is an interface mixin rather than a regular mixin
     */
    private final transient boolean isInterfaceMixin;

    /**
     * Holds state that currently is not fully initialised or validated
     */
    private transient ValidationState uninitialisedState;

    /**
     * Holds the current validated state
     */
    private transient ValidationState validationState;
    
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
        byte[] mixinBytes = this.loadMixinClass(this.className, runTransformers);
        this.uninitialisedState = new ValidationState(mixinBytes);
        this.info = this.uninitialisedState.classInfo;
        this.isInterfaceMixin = this.info.isInterface();

        ClassNode classNode = this.uninitialisedState.getClassNode(0);
        this.priority = this.readPriority(classNode);
        this.targetClasses = this.readTargetClasses(classNode, suppressPlugin);
        this.targetClassNames = Collections.unmodifiableList(Lists.transform(this.targetClasses, Functions.toStringFunction()));
    }
    
    void validate() {
        try {
            ClassNode classNode = this.uninitialisedState.classNode;
            this.uninitialisedState.detachedSuper = this.validateTargetClasses(classNode);
            this.validateMixin(this.uninitialisedState);
            this.readImplementations(this.uninitialisedState);
            this.readInnerClasses(this.uninitialisedState);
            if (this.uninitialisedState instanceof ReloadedState) {
                ((ReloadedState) this.uninitialisedState).validateChanges();
            } else {
                this.createPreProcessor(classNode).prepare();
            }
            this.uninitialisedState.classNode = null;
            // the state is now fully initialised and validated
            this.validationState = this.uninitialisedState;
        } finally {
            this.uninitialisedState = null;
        }
    }

    /**
     * Read the target class names from the {@link Mixin} annotation
     * 
     * @param classNode
     * @param suppressPlugin 
     * @return
     */
    protected List<ClassInfo> readTargetClasses(ClassNode classNode, boolean suppressPlugin) {
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
    private void readTargets(List<ClassInfo> outTargets, List<String> inTargets, boolean suppressPlugin, boolean checkPublic) {
        for (String targetClassName : inTargets) {
            targetClassName = targetClassName.replace('/', '.');
            if (this.plugin == null || suppressPlugin || this.plugin.shouldApplyMixin(targetClassName, this.className)) {
                ClassInfo targetInfo = ClassInfo.forName(targetClassName);
                this.checkTarget(targetClassName, targetInfo, checkPublic);
                if (!outTargets.contains(targetInfo)) {
                    outTargets.add(targetInfo);
                    targetInfo.addMixin(this);
                }
            }
        }
    }

    private void checkTarget(String targetClassName, ClassInfo targetInfo, boolean checkPublic) throws InvalidMixinException {
        if (targetInfo == null) {
            throw new MixinException("@Mixin target " + targetClassName + " was not found " + this);
        }
        boolean targetIsInterface = targetInfo.isInterface();
        if (targetIsInterface != this.isInterfaceMixin) {
            String not = targetIsInterface ? "" : "not ";
            throw new InvalidMixinException(this, "@Mixin target type mismatch: " + targetClassName + " is " + not + "an interface in " + this);
        }
        if (checkPublic && targetInfo.isPublic()) {
            throw new InvalidMixinException(this, "@Mixin target " + targetClassName + " is public in " + this
                    + " and must be specified in value");
        }
    }

    /**
     * Read the priority from the {@link Mixin} annotation
     * 
     * @param classNode
     * @return
     */
    protected int readPriority(ClassNode classNode) {
        AnnotationNode mixin = ASMHelper.getInvisibleAnnotation(classNode, Mixin.class);
        if (mixin == null) {
            throw new InvalidMixinException(this, String.format("The mixin '%s' is missing an @Mixin annotation", this.className));
        }
        
        Integer priority = ASMHelper.getAnnotationValue(mixin, "priority");
        return priority == null ? this.parent.getDefaultMixinPriority() : priority.intValue();
    }

    private boolean validateTargetClasses(ClassNode classNode) {
        if (this.isInterfaceMixin) {
            return this.validateInterfaceMixinTargets(classNode);
        }
        return this.validateClassMixinTargets(classNode);
    }

    private boolean validateInterfaceMixinTargets(ClassNode classNode) {
        if (!"java/lang/Object".equals(classNode.superName)) {
            throw new InvalidMixinException(this, "Super class of " + this + " is invalid, found " + classNode.superName.replace('/', '.'));
        }
        
        return false;
    }

    private boolean validateClassMixinTargets(ClassNode classNode) {
        boolean detached = false;
        
        for (ClassInfo targetClass : this.targetClasses) {
            if (classNode.superName.equals(targetClass.getSuperName())) {
                continue;
            }
            
            if (!targetClass.hasSuperClass(classNode.superName, ClassInfo.Traversal.SUPER)) {
                ClassInfo superClass = ClassInfo.forName(classNode.superName);
                if (superClass.isMixin()) {
                    // If superclass is a mixin, check for hierarchy derp
                    for (ClassInfo superTarget : superClass.getTargets()) {
                        if (this.targetClasses.contains(superTarget)) {
                            throw new InvalidMixinException(this, "Illegal hierarchy detected. Derived mixin " + this + " targets the same class "
                                    + superTarget.getClassName() + " as its superclass " + superClass.getClassName());
                        }
                    }
                }
                
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
     * @param state State to validate
     */
    private void validateMixin(ValidationState state) {
        this.validateType();
        this.validateInner(state);
        this.validateClassVersion(state);
        this.validateRemappables(state);
    }

    private void validateType() {
        if (this.isInterfaceMixin && !MixinEnvironment.getCompatibilityLevel().supportsMethodsInInterfaces()) {
            throw new InvalidMixinException(this, "Interface mixin not supported in current enviromnment");
        }
    }

    private void validateInner(ValidationState state) {
        // isInner (shouldn't) return true for static inner classes
        if (!state.classInfo.isProbablyStatic()) {
            throw new InvalidMixinException(this, "Inner class mixin must be declared static");
        }
    }

    private void validateClassVersion(ValidationState state) {
        if (state.classNode.version > MixinEnvironment.getCompatibilityLevel().classVersion()) {
            String helpText = ".";
            for (CompatibilityLevel level : CompatibilityLevel.values()) {
                if (level.classVersion() >= state.classNode.version) {
                    helpText = String.format(". Mixin requires compatibility level %s or above.", level.name()); 
                }
            }
            
            throw new InvalidMixinException(this, "Unsupported mixin class version " + state.classNode.version + helpText);
        }
    }

    private void validateRemappables(ValidationState state) {
        // Can't have remappable fields or methods on a multi-target mixin, because after obfuscation the fields will remap to conflicting names
        if (this.targetClasses.size() > 1) {
            for (FieldNode field : state.classNode.fields) {
                this.checkRemappable(Shadow.class, field.name, ASMHelper.getVisibleAnnotation(field, Shadow.class));
            }
            
            for (MethodNode method : state.classNode.methods) {
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
    private void readImplementations(ValidationState state) {
        state.interfaces.addAll(state.classNode.interfaces);
        
        AnnotationNode implementsAnnotation = ASMHelper.getInvisibleAnnotation(state.classNode, Implements.class);
        if (implementsAnnotation == null) {
            return;
        }
        
        List<AnnotationNode> interfaces = ASMHelper.getAnnotationValue(implementsAnnotation);
        if (interfaces == null) {
            return;
        }
        
        for (AnnotationNode interfaceNode : interfaces) {
            InterfaceInfo interfaceInfo = InterfaceInfo.fromAnnotation(this, interfaceNode);
            state.softImplements.add(interfaceInfo);
            state.interfaces.add(interfaceInfo.getInternalName());
            // only add interface if its initial initialisation
            if (!(state instanceof ReloadedState)) {
                state.classInfo.addInterface(interfaceInfo.getInternalName());
            }
        }
    }

    /**
     * Read inner class definitions for the class and locate any synthetic inner
     * classes so that we can add them to the passthrough set in our parent
     * config.
     */
    private void readInnerClasses(ValidationState state) {
        for (InnerClassNode inner : state.classNode.innerClasses) {
            ClassInfo innerClass = ClassInfo.forName(inner.name);
            if (innerClass.isSynthetic() && innerClass.isProbablyStatic()) {
                if ((inner.outerName != null && inner.outerName.equals(state.classInfo.getName()))
                        || inner.name.startsWith(state.classNode.name + "$")) {
                    state.syntheticInnerClasses.add(inner.name);
                } else {
                    throw new InvalidMixinException(this, "Unhandled synthetic inner class found: " + inner.name);
                }
            }
        }
    }

    /**
     * Current state, either the validated state or the uninitialised state if
     * the mixin is initialising for the first time. Should never return null.
     */
    private ValidationState getCurrentState() {
        if (this.validationState == null) {
            return this.uninitialisedState;
        }
        return this.validationState;
    }

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
        return this.getClassInfo().getName();
    }

    /**
     * Get the class bytecode
     */
    @Override
    public byte[] getClassBytes() {
        return this.getCurrentState().mixinBytes;
    }
    
    /**
     * True if the superclass of the mixin is <b>not</b> the direct superclass
     * of one or more targets
     */
    @Override
    public boolean isDetachedSuper() {
        return this.getCurrentState().detachedSuper;
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
     * Get whether this is an interface mixin or not
     */
    @Override
    public boolean isInterfaceMixin() {
        return this.isInterfaceMixin;
    }

    /**
     * Get a new tree for the class bytecode
     */
    @Override
    public ClassNode getClassNode(int flags) {
        return this.getCurrentState().getClassNode(flags);
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
        return Collections.unmodifiableList(this.getCurrentState().softImplements);
    }

    /**
     * Get the synthetic inner classes for this mixin
     */
    public Set<String> getSyntheticInnerClasses() {
        return Collections.unmodifiableSet(this.getCurrentState().syntheticInnerClasses);
    }
    
    /**
     * Get the target class list for this mixin
     */
    public List<ClassInfo> getTargets() {
        return Collections.unmodifiableList(this.getTargets0());
    }

    /**
     * Internal method to avoid synthetic bridge generation
     */
    protected List<ClassInfo> getTargets0() {
        return this.targetClasses;
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
        return this.getCurrentState().interfaces;
    }

    /**
     * Get a new mixin target context object for the specified target
     * 
     * @param target
     * @return new context
     */
    public MixinTargetContext createContextFor(TargetClassContext target) {
        ClassNode classNode = this.getClassNode(ClassReader.EXPAND_FRAMES);
        return this.createPreProcessor(classNode).prepare().createContextFor(target);
    }

    /**
     * Create a preprocessor based on this mixin's type
     * 
     * @return new preprocessor
     */
    private MixinPreProcessor createPreProcessor(ClassNode classNode) {
        if (this.isInterfaceMixin) {
            return new InterfaceMixinPreProcessor(this, classNode);
        }
        return new MixinPreProcessor(this, classNode);
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

    /**
     * Updates this mixin with new bytecode
     *
     * @param mixinBytes New bytecode
     */
    void reloadMixin(byte[] mixinBytes) {
        if (this.uninitialisedState != null) {
            throw new IllegalStateException("Cannot reload mixin while it is initialising");
        }
        this.uninitialisedState = new ReloadedState(this.validationState, mixinBytes);
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
