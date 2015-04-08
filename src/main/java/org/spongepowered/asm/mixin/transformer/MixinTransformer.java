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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import net.minecraft.launchwrapper.IClassTransformer;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.MixinApplyError;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.mixin.transformer.meta.MixinRenamed;
import org.spongepowered.asm.transformers.TreeTransformer;
import org.spongepowered.asm.util.ASMHelper;
import org.spongepowered.asm.util.Constants;

/**
 * Transformer which applies Mixin classes to their declared target classes
 */
public class MixinTransformer extends TreeTransformer {
    
    /**
     * Internal struct for representing a range
     */
    class Range {
        /**
         * Start of the range
         */
        final int start;
        
        /**
         * End of the range 
         */
        final int end;
        
        /**
         * Range marker
         */
        final int marker;

        /**
         * Create a range with the specified values.
         * 
         * @param start Start of the range
         * @param end End of the range
         * @param marker Arbitrary marker value
         */
        Range(int start, int end, int marker) {
            this.start = start;
            this.end = end;
            this.marker = marker;
        }
        
        /**
         * Range is valid if both start and end are nonzero and end is after or
         * at start
         * 
         * @return true if valid
         */
        boolean isValid() {
            return (this.start != 0 && this.end != 0 && this.end >= this.start);
        }
        
        /**
         * Returns true if the supplied value is between or equal to start and
         * end
         * 
         * @param value true if the range contains value
         */
        boolean contains(int value) {
            return value >= this.start && value <= this.end;
        }
        
        /**
         * Returns true if the supplied value is outside the range
         * 
         * @param value true if the range does not contain value
         */
        boolean excludes(int value) {
            return value < this.start || value > this.end;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return String.format("Range[%d-%d,%d,valid=%s)", this.start, this.end, this.marker, this.isValid());
        }
    }
    
    /**
     * Re-entrance semaphore used to share re-entrance data with the TreeInfo
     */
    class ReEntranceState {
        
        /**
         * Max valid depth
         */
        private final int maxDepth;
        
        /**
         * Re-entrance depth
         */
        private int depth = 0;
        
        /**
         * Semaphore set when check exceeds a depth of 1
         */
        private boolean semaphore = false;
        
        public ReEntranceState(int maxDepth) {
            this.maxDepth = maxDepth;
        }
        
        /**
         * Get max depth
         */
        public int getMaxDepth() {
            return this.maxDepth;
        }
        
        /**
         * Get current depth
         */
        public int getDepth() {
            return this.depth;
        }
        
        /**
         * Increase the re-entrance depth counter and set the semaphore if depth
         * exceeds max depth
         * 
         * @return fluent interface
         */
        ReEntranceState push() {
            this.depth++;
            this.checkAndSet();
            return this;
        }
        
        /**
         * Decrease the re-entrance depth
         * 
         * @return fluent interface
         */
        ReEntranceState pop() {
            if (this.depth == 0) {
                throw new IllegalStateException("ReEntranceState pop() with zero depth");
            }
            
            this.depth--;
            return this;
        }
        
        /**
         * Run the depth check but do not set the semaphore
         * 
         * @return true if depth has exceeded max
         */
        boolean check() {
            return this.depth > this.maxDepth;
        }
        
        /**
         * Run the depth check and set the semaphore if depth is exceeded
         * 
         * @return true if semaphore is set
         */
        boolean checkAndSet() {
            return this.semaphore |= this.check();
        }
        
        /**
         * Set the semaphore
         * 
         * @return fluent interface
         */
        ReEntranceState set() {
            this.semaphore = true;
            return this;
        }
        
        /**
         * Get whether the semaphore is set
         */
        boolean isSet() {
            return this.semaphore;
        }
        
        /**
         * Clear the semaphore
         * 
         * @return fluent interface
         */
        ReEntranceState clear() {
            this.semaphore = false;
            return this;
        }
    }
    
    /**
     * List of opcodes which must not appear in a class initialiser, mainly a
     * sanity check so that if any of the specified opcodes are found, we can
     * log it as an error condition and then people can bitch at me to fix it.
     * Essentially if it turns out that field initialisers can somehow make use
     * of local variables, then I need to write some code to ensure that said
     * locals are shifted so that they don't interfere with locals in the
     * receiving constructor. 
     */
    private static final int[] INITIALISER_OPCODE_BLACKLIST = {
        Opcodes.RETURN, Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.IALOAD, Opcodes.LALOAD, Opcodes.FALOAD, Opcodes.DALOAD,
        Opcodes.AALOAD, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD, Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE,
        Opcodes.ASTORE, Opcodes.IASTORE, Opcodes.LASTORE, Opcodes.FASTORE, Opcodes.DASTORE, Opcodes.AASTORE, Opcodes.BASTORE, Opcodes.CASTORE,
        Opcodes.SASTORE
    };
    
    /**
     * Log all the things
     */
    private final Logger logger = LogManager.getLogger("mixin");
    
    /**
     * Mixin configuration bundle
     */
    private final List<MixinConfig> configs = new ArrayList<MixinConfig>();
    
    /**
     * Transformer modules
     */
    private final List<IMixinTransformerModule> modules = new ArrayList<IMixinTransformerModule>();

    /**
     * True once initialisation is done. All mixin configs needs to be
     * initialised as late as possible in startup (so that other transformers
     * have had time to register) but before any game classes are transformed.
     * To do this we initialise the configs on the first call to
     * {@link #transform} and this flag keeps track of when we've done this. 
     */
    private boolean initDone;
    
    /**
     * Re-entrance detector
     */
    private final ReEntranceState lock = new ReEntranceState(1);
    
    /**
     * Session ID, used as a check when parsing {@link MixinMerged} annotations
     * to prevent them being applied at compile time by people trying to
     * circumvent mixin application
     */
    private final String sessionId = UUID.randomUUID().toString();
    
    /**
     * ctor 
     */
    public MixinTransformer() {
        MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();
        
        Object globalMixinTransformer = environment.getActiveTransformer();
        if (globalMixinTransformer instanceof IClassTransformer) {
            throw new RuntimeException("Terminating MixinTransformer instance " + this);
        }
        
        // I am a leaf on the wind
        environment.setActiveTransformer(this);
        
        this.addConfigs(environment);
        this.addModules(environment);
        
        TreeInfo.setLock(this.lock);
    }

    private void addConfigs(MixinEnvironment environment) {
        List<String> configs = environment.getMixinConfigs();
        
        if (configs != null) {
            for (String configFile : configs) {
                try {
                    MixinConfig config = MixinConfig.create(configFile);
                    if (config != null) {
                        this.configs.add(config);
                    }
                } catch (Exception ex) {
                    this.logger.warn(String.format("Failed to load mixin config: %s", configFile), ex);
                }
            }
        }
        
        Collections.sort(this.configs);
    }

    private void addModules(MixinEnvironment environment) {
        // Run CheckClassAdapter on the mixin bytecode if debug option is enabled 
        if (environment.getOption(Option.DEBUG_VERIFY)) {
            this.modules.add(new MixinTransformerModuleCheckClass());
        }
        
        // Run implementation checker if option is enabled
        if (environment.getOption(Option.CHECK_IMPLEMENTS)) {
            this.modules.add(new MixinTransformerModuleInterfaceChecker());
        }
    }

    /* (non-Javadoc)
     * @see net.minecraft.launchwrapper.IClassTransformer
     *      #transform(java.lang.String, java.lang.String, byte[])
     */
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || transformedName == null) {
            return basicClass;
        }

        boolean locked = this.lock.push().isSet();
        
        if (!this.initDone && !locked) {
            this.initDone = true;
            try {
                this.initConfigs();
            } finally {
                this.lock.pop();
            }
        }
        
        try {
            SortedSet<MixinInfo> mixins = null;
            
            for (MixinConfig config : this.configs) {
                if (config.packageMatch(transformedName)) {
                    if (config.canPassThrough(transformedName)) {
                        return this.passThrough(name, transformedName, basicClass);
                    }
                    
                    throw new NoClassDefFoundError(String.format("%s is a mixin class and cannot be referenced directly", transformedName));
                }
                
                if (config.hasMixinsFor(transformedName)) {
                    if (mixins == null) {
                        mixins = new TreeSet<MixinInfo>();
                    }
                    
                    // Get and sort mixins for the class
                    mixins.addAll(config.getMixinsFor(transformedName));
                }
            }
            
            if (mixins != null) {
                // Re-entrance is "safe" as long as we don't need to apply any mixins, if there are mixins then we need to panic now
                if (locked) {
                    this.logger.warn("Re-entrance detected, this will cause serious problems.", new RuntimeException());
                    throw new Error("Re-entrance error.");
                }
                
                try {
                    basicClass = this.applyMixins(transformedName, basicClass, mixins);
                } catch (InvalidMixinException th) {
                    if (MixinEnvironment.getCurrentEnvironment().getOption(Option.DUMP_TARGET_ON_FAILURE)) {
                        this.dumpClass(transformedName.replace('.', '/') + ".target", basicClass);
                    }
                    
                    MixinConfig config = th.getMixin().getParent();
                    this.logger.log(config.isRequired() ? Level.FATAL : Level.WARN, String.format("Mixin failed applying %s -> %s: %s %s",
                            th.getMixin(), transformedName, th.getClass().getName(), th.getMessage()), th);

                    if (config.isRequired()) {
                        throw new MixinApplyError("Mixin [" + th.getMixin() + "] FAILED for REQUIRED config [" + config + "]", th);
                    }
                    
                    th.printStackTrace();
                }
            }

            return basicClass;
        } catch (Exception ex) {
            throw new MixinTransformerError("An unexpected critical error was encountered", ex);
        } finally {
            this.lock.pop();
        }
    }

    /**
     * Initialise mixin configs
     */
    private void initConfigs() {
        for (MixinConfig config : this.configs) {
            try {
                config.initialise();
            } catch (Exception ex) {
                this.logger.error("Error encountered whilst initialising mixin config '" + config.getName() + "': " + ex.getMessage(), ex);
            }
        }
        
        for (MixinConfig config : this.configs) {
            IMixinConfigPlugin plugin = config.getPlugin();
            if (plugin == null) {
                continue;
            }
            
            Set<String> otherTargets = new HashSet<String>();
            for (MixinConfig otherConfig : this.configs) {
                if (!otherConfig.equals(config)) {
                    otherTargets.addAll(otherConfig.getTargets());
                }
            }
            
            plugin.acceptTargets(config.getTargets(), Collections.unmodifiableSet(otherTargets));
        }

        for (MixinConfig config : this.configs) {
            try {
                config.postInitialise();
            } catch (Exception ex) {
                this.logger.error("Error encountered during mixin config postInit setp'" + config.getName() + "': " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * "Pass through" a synthetic inner class. Transforms package-private
     * members in the class into public so that they are accessible from their
     * new home in the target class
     * 
     * @param name original class name
     * @param transformedName deobfuscated class name
     * @param basicClass class bytecode
     * @return public-ified class bytecode
     */
    private byte[] passThrough(String name, String transformedName, byte[] basicClass) {
        ClassNode passThroughClass = this.readClass(basicClass, true);
        
        // Make the class public
        passThroughClass.access |= Opcodes.ACC_PUBLIC;
        
        // Make package-private fields public
        for (FieldNode field : passThroughClass.fields) {
            if ((field.access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) == 0) {
                field.access |= Opcodes.ACC_PUBLIC;
            }
        }
        
        // Make package-private methods public
        for (MethodNode method : passThroughClass.methods) {
            if ((method.access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) == 0) {
                method.access |= Opcodes.ACC_PUBLIC;
            }
        }
        
        return this.writeClass(transformedName, passThroughClass);
    }

    /**
     * Apply mixins for specified target class to the class described by the
     * supplied byte array.
     * 
     * @param transformedName 
     * @param basicClass
     * @param mixins
     * @return class bytecode after application of mixins
     */
    private byte[] applyMixins(String transformedName, byte[] basicClass, SortedSet<MixinInfo> mixins) {
        // Tree for target class
        ClassNode targetClass = this.readClass(basicClass, true);
        
        this.preTransform(transformedName, targetClass, mixins);
        
        for (MixinInfo mixin : mixins) {
            this.logger.log(mixin.getLoggingLevel(), "Mixing {} into {}", mixin.getName(), transformedName);
            this.applyMixin(transformedName, targetClass, mixin.createContextFor(targetClass));
        }
        
        this.postTransform(transformedName, targetClass, mixins);
        
        return this.writeClass(transformedName, targetClass);
    }

    private byte[] writeClass(String transformedName, ClassNode targetClass) {
        // Collapse tree to bytes
        byte[] bytes = this.writeClass(targetClass);
        
        // Export transformed class for debugging purposes
        if (MixinEnvironment.getCurrentEnvironment().getOption(Option.DEBUG_EXPORT)) {
            this.dumpClass(transformedName.replace('.', '/'), bytes);
        }
        
        return bytes;
    }

    private void dumpClass(String fileName, byte[] bytes) {
        try {
            FileUtils.writeByteArrayToFile(new File(Constants.DEBUG_OUTPUT_PATH + "/" + fileName + ".class"), bytes);
        } catch (IOException ex) {
            // don't care
        }
    }

    /**
     * Process tasks before transform
     * 
     * @param transformedName Target class transformed name
     * @param targetClass Target class
     * @param mixins Mixin which were just applied
     */
    private void preTransform(String transformedName, ClassNode targetClass, SortedSet<MixinInfo> mixins) {
        for (IMixinTransformerModule module : this.modules) {
            module.preTransform(transformedName, targetClass, mixins);
        }
    }

    /**
     * Apply the mixin described by mixin to the supplied classNode
     * 
     * @param transformedName Target class transformed name
     * @param targetClass Target class
     * @param mixin Mixin to apply
     */
    protected void applyMixin(String transformedName, ClassNode targetClass, MixinTargetContext mixin) {
        try {
            mixin.preApply(transformedName, targetClass);
            this.applyMixinInterfaces(targetClass, mixin);
            this.applyMixinAttributes(targetClass, mixin);
            this.applyMixinFields(targetClass, mixin);
            this.applyMixinMethods(targetClass, mixin);
            this.applyInitialisers(targetClass, mixin);
            this.applyInjections(targetClass, mixin);
            mixin.postApply(transformedName, targetClass);
        } catch (InvalidMixinException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidMixinException(mixin, "Unexpecteded error whilst applying the mixin class", ex);
        }
    }

    /**
     * Process tasks after transform
     * 
     * @param transformedName Target class transformed name
     * @param targetClass Target class
     * @param mixins Mixin which were just applied
     */
    private void postTransform(String transformedName, ClassNode targetClass, SortedSet<MixinInfo> mixins) {
        for (IMixinTransformerModule module : this.modules) {
            module.postTransform(transformedName, targetClass, mixins);
        }
    }

    /**
     * Mixin interfaces implemented by the mixin class onto the target class
     * 
     * @param targetClass
     * @param mixin
     */
    private void applyMixinInterfaces(ClassNode targetClass, MixinTargetContext mixin) {
        for (String interfaceName : mixin.getInterfaces()) {
            if (!targetClass.interfaces.contains(interfaceName)) {
                targetClass.interfaces.add(interfaceName);
            }
        }
    }

    /**
     * Mixin misc attributes from mixin class onto the target class
     * 
     * @param targetClass
     * @param mixin
     */
    private void applyMixinAttributes(ClassNode targetClass, MixinTargetContext mixin) {
        if (mixin.shouldSetSourceFile()) {
            targetClass.sourceFile = mixin.getClassNode().sourceFile;
        }
    }

    /**
     * Mixin fields from mixin class into the target class. It is vital that
     * this is done before mixinMethods because we need to compute renamed
     * fields so that transformMethod can rename field references in the method
     * body.
     * 
     * @param targetClass
     * @param mixin
     */
    private void applyMixinFields(ClassNode targetClass, MixinTargetContext mixin) {
        for (FieldNode field : mixin.getClassNode().fields) {
            FieldNode target = MixinTransformer.findTargetField(targetClass, field);
            if (target == null) {
                // This is just a local field, so add it
                targetClass.fields.add(field);
            }
        }
    }

    /**
     * Mixin methods from the mixin class into the target class
     * 
     * @param targetClass
     * @param mixin
     */
    private void applyMixinMethods(ClassNode targetClass, MixinTargetContext mixin) {
        for (MethodNode mixinMethod : mixin.getClassNode().methods) {
            // Reparent all mixin methods into the target class
            mixin.transformMethod(mixinMethod);

            boolean isShadow = ASMHelper.getVisibleAnnotation(mixinMethod, Shadow.class) != null;
            
            if (isShadow) {
                continue;
            } else if (!mixinMethod.name.startsWith("<")) {
                boolean isOverwrite = ASMHelper.getVisibleAnnotation(mixinMethod, Overwrite.class) != null;
                
                if (MixinTransformer.hasFlag(mixinMethod, Opcodes.ACC_STATIC)
                        && !MixinTransformer.hasFlag(mixinMethod, Opcodes.ACC_PRIVATE)
                        && !MixinTransformer.hasFlag(mixinMethod, Opcodes.ACC_SYNTHETIC)
                        && !isOverwrite) {
                    throw new InvalidMixinException(mixin, 
                            String.format("Mixin classes cannot contain visible static methods or fields, found %s", mixinMethod.name));
                }
                
                this.mergeMethod(targetClass, mixin, mixinMethod, isOverwrite);
            } else if (Constants.CLINIT.equals(mixinMethod.name)) {
                // Class initialiser insns get appended
                this.appendInsns(targetClass, mixinMethod.name, mixinMethod);
            } 
        }
    }

    /**
     * Attempts to merge the supplied method into the target class
     * 
     * @param targetClass Target class to merge into
     * @param mixin Mixin being applied
     * @param method Method to merge
     * @param isOverwrite true if the method is annotated with an
     *      {@link Overwrite} annotation
     */
    private void mergeMethod(ClassNode targetClass, MixinTargetContext mixin, MethodNode method, boolean isOverwrite) {
        MethodNode target = MixinTransformer.findTargetMethod(targetClass, method);
        
        if (target != null) {
            if (this.alreadyMerged(targetClass, mixin, method, isOverwrite, target)) {
                return;
            }
            
            AnnotationNode intrinsic = ASMHelper.getInvisibleAnnotation(method, Intrinsic.class);
            if (intrinsic != null) {
                if (this.mergeIntrinsic(targetClass, mixin, method, isOverwrite, target, intrinsic)) {
                    return;
                }
            } else {
                targetClass.methods.remove(target);
            }
        } else if (isOverwrite) {
            throw new InvalidMixinException(mixin, String.format("Overwrite target %s was not located in the target class", method.name));
        }
        
        targetClass.methods.add(method);
        mixin.getTargetClassInfo().addMethod(method);
        
        ASMHelper.setVisibleAnnotation(method, MixinMerged.class,
                "mixin", mixin.getClassName(),
                "priority", mixin.getPriority(),
                "sessionId", this.sessionId);
    }

    /**
     * Check whether this method was already merged into the target, returns
     * false if the method was <b>not</b> already merged or if the incoming
     * method has a higher priority than the already merged method.
     * 
     * @param targetClass Target classnode
     * @param mixin Mixin context
     * @param method Method being merged
     * @param isOverwrite True if the incoming method is tagged with Override
     * @param target target method being checked
     * @return true if the target was already merged and should be skipped
     */
    private boolean alreadyMerged(ClassNode targetClass, MixinTargetContext mixin, MethodNode method, boolean isOverwrite, MethodNode target) {
        AnnotationNode merged = ASMHelper.getVisibleAnnotation(target, MixinMerged.class);
        if (merged == null) {
            return false;
        }
    
        String sessionId = ASMHelper.<String>getAnnotationValue(merged, "sessionId");
        
        if (!this.sessionId.equals(sessionId)) {
            throw new ClassFormatError("Invalid @MixinMerged annotation found in" + mixin + " at " + method.name + " in " + targetClass.name);
        }

        String owner = ASMHelper.<String>getAnnotationValue(merged, "mixin");
        int priority = ASMHelper.<Integer>getAnnotationValue(merged, "priority");
        
        if (priority >= mixin.getPriority() && !owner.equals(mixin.getClassName())) {
            this.logger.warn("Method overwrite conflict for {}, previously written by {}. Skipping method.", method.name, owner);
            return true;
        }

        return false;
    }

    /**
     * Validates and prepares an intrinsic merge, returns true if the intrinsic
     * check results in a "skip" action, indicating that no further merge action
     * should be undertaken
     * 
     * @param targetClass Target classnode
     * @param mixin Mixin context
     * @param method Method being merged
     * @param isOverwrite True if the incoming method is tagged with Override
     * @param target target method being checked
     * @param intrinsic {@link Intrinsic} annotation
     * @return true if the intrinsic method was skipped (short-circuit further
     *      merge operations)
     */
    private boolean mergeIntrinsic(ClassNode targetClass, MixinTargetContext mixin, MethodNode method,
            boolean isOverwrite, MethodNode target, AnnotationNode intrinsic) {
        
        if (isOverwrite) {
            throw new InvalidMixinException(mixin, "@Intrinsic is not compatible with @Overwrite, remove one of these annotations on "
                    + method.name);
        }
        
        if (MixinTransformer.hasFlag(method, Opcodes.ACC_STATIC)) {
            throw new InvalidMixinException(mixin, "@Intrinsic method cannot be static, found " + method.name);
        }
        
        AnnotationNode renamed = ASMHelper.getVisibleAnnotation(method, MixinRenamed.class);
        if (renamed == null || !ASMHelper.getAnnotationValue(renamed, "isInterfaceMember", false)) {
            throw new InvalidMixinException(mixin, "@Intrinsic method must be prefixed interface method, no rename encountered on " + method.name);
        }
        
        if (!ASMHelper.getAnnotationValue(intrinsic, "displace", false)) {
            this.logger.log(mixin.getLoggingLevel(), "Skipping Intrinsic mixin method {}", method.name);
            return true;
        }
        
        this.displaceIntrinsic(targetClass, mixin, method, target);
        return false;
    }

    /**
     * Handles intrinsic displacement
     * 
     * @param targetClass Target classnode
     * @param mixin Mixin context
     * @param method Method being merged
     * @param target target method being checked
     */
    private void displaceIntrinsic(ClassNode targetClass, MixinTargetContext mixin, MethodNode method, MethodNode target) {
        // Deliberately include invalid character in the method name so that
        // we guarantee no hackiness
        String proxyName = "proxy+" + target.name;
        
        for (Iterator<AbstractInsnNode> iter = method.instructions.iterator(); iter.hasNext();) {
            AbstractInsnNode insn = iter.next();
            if (insn instanceof MethodInsnNode && insn.getOpcode() != Opcodes.INVOKESTATIC) {
                MethodInsnNode methodNode = (MethodInsnNode)insn;
                if (methodNode.owner.equals(targetClass.name) && methodNode.name.equals(target.name) && methodNode.desc.equals(target.desc)) {
                    methodNode.name = proxyName;
                }
            }
        }
        
        target.name = proxyName;
    }

    /**
     * Handles appending instructions from the source method to the target
     * method
     * 
     * @param targetClass
     * @param targetMethodName
     * @param sourceMethod
     */
    private void appendInsns(ClassNode targetClass, String targetMethodName, MethodNode sourceMethod) {
        if (Type.getReturnType(sourceMethod.desc) != Type.VOID_TYPE) {
            throw new IllegalArgumentException("Attempted to merge insns into a method which does not return void");
        }

        if (targetMethodName == null || targetMethodName.length() == 0) {
            targetMethodName = sourceMethod.name;
        }

        boolean found = false;

        for (MethodNode method : targetClass.methods) {
            if ((targetMethodName.equals(method.name)) && sourceMethod.desc.equals(method.desc)) {
                found = true;
                AbstractInsnNode returnNode = null;
                Iterator<AbstractInsnNode> findReturnIter = method.instructions.iterator();
                while (findReturnIter.hasNext()) {
                    AbstractInsnNode insn = findReturnIter.next();
                    if (insn.getOpcode() == Opcodes.RETURN) {
                        returnNode = insn;
                        break;
                    }
                }

                Iterator<AbstractInsnNode> injectIter = sourceMethod.instructions.iterator();
                while (injectIter.hasNext()) {
                    AbstractInsnNode insn = injectIter.next();
                    if (!(insn instanceof LineNumberNode) && insn.getOpcode() != Opcodes.RETURN) {
                        method.instructions.insertBefore(returnNode, insn);
                    }
                }
            }
        }
        
        if (!found) {
            sourceMethod.name = targetMethodName;
            targetClass.methods.add(sourceMethod);
        }
    }
    
    /**
     * (Attempts to) find and patch field initialisers from the mixin into the
     * target class
     * 
     * @param targetClass
     * @param mixin
     */
    private void applyInitialisers(ClassNode targetClass, MixinTargetContext mixin) {
        // Try to find a suitable constructor, we need a constructor with line numbers in order to extract the initialiser 
        MethodNode ctor = this.getConstructor(mixin);
        if (ctor == null) {
            return;
        }
        
        // Find the initialiser instructions in the candidate ctor
        InsnList initialiser = this.getInitialiser(mixin, ctor);
        if (initialiser == null || initialiser.size() == 0) {
            return;
        }
        
        // Patch the initialiser into the target class ctors
        for (MethodNode method : targetClass.methods) {
            if (Constants.INIT.equals(method.name)) {
                method.maxStack = Math.max(method.maxStack, ctor.maxStack);
                this.injectInitialiser(method, initialiser);
            }
        }
    }

    /**
     * Finds a suitable ctor for reading the instance initialiser bytecode
     */
    private MethodNode getConstructor(MixinTargetContext mixin) {
        MethodNode ctor = null;
        
        for (MethodNode mixinMethod : mixin.getClassNode().methods) {
            if (Constants.INIT.equals(mixinMethod.name)) {
                boolean hasLineNumbers = false;
                for (Iterator<AbstractInsnNode> iter = mixinMethod.instructions.iterator(); iter.hasNext();) {
                    if (iter.next() instanceof LineNumberNode) {
                        hasLineNumbers = true;
                        break;
                    }
                }
                if (hasLineNumbers) {
                    if (ctor == null) {
                        ctor = mixinMethod;
                    } else {
                        // Not an error condition, just weird
                        this.logger.warn(String.format("Mixin %s has multiple constructors, %s was selected\n", mixin, ctor.desc));
                    }
                }
            }
        }
        
        return ctor;
    }

    /**
     * Identifies line numbers in the supplied ctor which correspond to the
     * start and end of the method body.
     * 
     * @param ctor
     * @return range indicating the line numbers of the specified constructor
     *      and the position of the superclass ctor invocation
     */
    private Range getConstructorRange(MethodNode ctor) {
        int line = 0, start = 0, end = 0, superIndex = -1;
        for (Iterator<AbstractInsnNode> iter = ctor.instructions.iterator(); iter.hasNext();) {
            AbstractInsnNode insn = iter.next();
            if (insn instanceof LineNumberNode) {
                line = ((LineNumberNode)insn).line;
            } else if (insn instanceof MethodInsnNode) {
                if (insn.getOpcode() == Opcodes.INVOKESPECIAL && Constants.INIT.equals(((MethodInsnNode)insn).name) && superIndex == -1) {
                    superIndex = ctor.instructions.indexOf(insn);
                    start = line;
                }
            } else if (insn.getOpcode() == Opcodes.RETURN) {
                end = line;
            }
        }
        
        return new Range(start, end, superIndex);
    }

    /**
     * Get insns corresponding to the instance initialiser (hopefully) from the
     * supplied constructor.
     * 
     * TODO Potentially rewrite this to be less horrible.
     * 
     * @param mixin
     * @param ctor
     * @return initialiser bytecode extracted from the supplied constructor, or
     *      null if the constructor range could not be parsed
     */
    private InsnList getInitialiser(MixinTargetContext mixin, MethodNode ctor) {
        // Find the range of line numbers which corresponds to the constructor body
        Range init = this.getConstructorRange(ctor);
        if (!init.isValid()) {
            return null;
        }
        
        // Now we know where the constructor is, look for insns which lie OUTSIDE the method body
        int line = 0;
        InsnList initialiser = new InsnList();
        boolean gatherNodes = false;
        int trimAtOpcode = -1;
        LabelNode optionalInsn = null;
        for (Iterator<AbstractInsnNode> iter = ctor.instructions.iterator(init.marker); iter.hasNext();) {
            AbstractInsnNode insn = iter.next();
            if (insn instanceof LineNumberNode) {
                line = ((LineNumberNode)insn).line;
                AbstractInsnNode next = ctor.instructions.get(ctor.instructions.indexOf(insn) + 1);
                if (line == init.end && next.getOpcode() != Opcodes.RETURN) {
                    gatherNodes = true;
                    trimAtOpcode = Opcodes.RETURN;
                } else {
                    gatherNodes = init.excludes(line);
                    trimAtOpcode = -1;
                }
            } else if (gatherNodes) {
                if (optionalInsn != null) {
                    initialiser.add(optionalInsn);
                    optionalInsn = null;
                }
                
                if (insn instanceof LabelNode) {
                    optionalInsn = (LabelNode)insn;
                } else {
                    int opcode = insn.getOpcode();
                    if (opcode == trimAtOpcode) {
                        trimAtOpcode = -1;
                        continue;
                    }
                    for (int ivalidOp : MixinTransformer.INITIALISER_OPCODE_BLACKLIST) {
                        if (opcode == ivalidOp) {
                            // At the moment I don't handle any transient locals because I haven't seen any in the wild, but let's avoid writing
                            // code which will likely break things and fix it if a real test case ever appears
                            throw new InvalidMixinException(mixin, "Cannot handle " + ASMHelper.getOpcodeName(opcode) + " opcode (0x"
                                    + Integer.toHexString(opcode).toUpperCase() + ") in class initialiser");
                        }
                    }
                    
                    initialiser.add(insn);
                }
            }
        }
        
        // Check that the last insn is a PUTFIELD, if it's not then 
        AbstractInsnNode last = initialiser.getLast();
        if (last != null) {
            if (last.getOpcode() != Opcodes.PUTFIELD) {
                throw new InvalidMixinException(mixin, "Could not parse initialiser, expected 0xB5, found 0x"
                        + Integer.toHexString(last.getOpcode()));
            }
        }
        
        return initialiser;
    }

    /**
     * Inject initialiser code into the target constructor
     * 
     * @param ctor
     * @param initialiser
     */
    private void injectInitialiser(MethodNode ctor, InsnList initialiser) {
        for (Iterator<AbstractInsnNode> iter = ctor.instructions.iterator(0); iter.hasNext();) {
            AbstractInsnNode insn = iter.next();
            if (insn.getOpcode() == Opcodes.INVOKESPECIAL && Constants.INIT.equals(((MethodInsnNode)insn).name)) {
                ctor.instructions.insert(insn, initialiser);
            }
        }
    }

    /**
     * Process {@link Inject} annotations and inject callbacks to annotated
     * methods
     * 
     * @param targetClass
     * @param mixin
     */
    private void applyInjections(ClassNode targetClass, MixinTargetContext mixin) {
        List<InjectionInfo> injected = new ArrayList<InjectionInfo>();
        
        for (MethodNode method : targetClass.methods) {
            InjectionInfo injectInfo = InjectionInfo.parse(mixin, method);
            if (injectInfo == null) {
                continue;
            }
            
            if (injectInfo.isValid()) {
                injectInfo.inject();
                injected.add(injectInfo);
            }
            
            method.visibleAnnotations.remove(injectInfo.getAnnotation());
        }
        
        for (InjectionInfo injectInfo : injected) {
            injectInfo.postInject();
        }
    }
    
    /**
     * Finds a method in the target class
     * 
     * @param targetClass
     * @param searchFor
     * @return Target method matching searchFor, or null if not found
     */
    private static MethodNode findTargetMethod(ClassNode targetClass, MethodNode searchFor) {
        for (MethodNode target : targetClass.methods) {
            if (target.name.equals(searchFor.name) && target.desc.equals(searchFor.desc)) {
                return target;
            }
        }
        
        return null;
    }

    /**
     * Finds a field in the target class
     * 
     * @param targetClass
     * @param searchFor
     * @return Target field matching searchFor, or null if not found
     */
    private static FieldNode findTargetField(ClassNode targetClass, FieldNode searchFor) {
        for (FieldNode target : targetClass.fields) {
            if (target.name.equals(searchFor.name)) {
                return target;
            }
        }

        return null;
    }
    
    /**
     * Check whether the specified flag is set on the specified method
     * 
     * @param method
     * @param flag 
     * @return True if the specified flag is set in this method's access flags
     */
    static boolean hasFlag(MethodNode method, int flag) {
        return (method.access & flag) == flag;
    }
    
    /**
     * Check whether the specified flag is set on the specified field
     * 
     * @param field
     * @param flag 
     * @return True if the specified flag is set in this field's access flags
     */
    static boolean hasFlag(FieldNode field, int flag) {
        return (field.access & flag) == flag;
    }
}
