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
import net.minecraft.launchwrapper.Launch;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.helpers.Booleans;
import org.objectweb.asm.ClassWriter;
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
import org.objectweb.asm.util.CheckClassAdapter;
import org.spongepowered.asm.mixin.InvalidMixinException;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinTransformerError;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.transformers.TreeTransformer;
import org.spongepowered.asm.util.ASMHelper;

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
    
    private static final String INIT = "<init>";
    private static final String CLINIT = "<clinit>";
    
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
    
    public static final boolean DEBUG_ALL = Booleans.parseBoolean(System.getProperty("mixin.debug"), false);
    private static final boolean DEBUG_EXPORT = Booleans.parseBoolean(System.getProperty("mixin.debug.export"), false) | MixinTransformer.DEBUG_ALL;
    private static final boolean DEBUG_VERIFY = Booleans.parseBoolean(System.getProperty("mixin.debug.verify"), false) | MixinTransformer.DEBUG_ALL;

    /**
     * Log all the things
     */
    private final Logger logger = LogManager.getLogger("mixin");
    
    /**
     * Mixin configuration bundle
     */
    private final List<MixinConfig> configs = new ArrayList<MixinConfig>();
    
    /**
     * True once initialisation is done. All mixin configs needs to be
     * initialised as late as possible in startup (so that other transformers
     * have had time to register) but before any game classes are transformed.
     * To do this we initialise the configs on the first call to
     * {@link #transform} and this flag keeps track of when we've done this. 
     */
    private boolean initDone;
    
    /**
     * Sanity check for this transformer. The transformer should never be
     * re-entrant by design so we need to detect and warn when it happens. 
     */
    private int reEntranceCheck = 0;
    
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
        // Go via blackboard to create FORWARD compatibility if Mixins get pulled into FML 
        Object globalMixinTransformer = MixinEnvironment.getCurrentEnvironment().getActiveTransformer();
        if (globalMixinTransformer instanceof IClassTransformer) {
            throw new RuntimeException("Terminating MixinTransformer instance " + this);
        }
        
        // I am a leaf on the wind
        MixinEnvironment.getCurrentEnvironment().setActiveTransformer(this);
        
        List<String> configs = MixinEnvironment.getCurrentEnvironment().getMixinConfigs();
        
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

    /* (non-Javadoc)
     * @see net.minecraft.launchwrapper.IClassTransformer
     *      #transform(java.lang.String, java.lang.String, byte[])
     */
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return basicClass;
        }

        this.reEntranceCheck++;
        
        if (this.reEntranceCheck > 1) {
            this.detectReEntrance();
        }
        
        if (!this.initDone) {
            this.initConfigs();
            this.initDone = true;
        }
        
        try {
            SortedSet<MixinInfo> mixins = null;
            
            for (MixinConfig config : this.configs) {
                if (transformedName != null && transformedName.startsWith(config.getMixinPackage())) {
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
                try {
                    basicClass = this.applyMixins(transformedName, basicClass, mixins);
                } catch (InvalidMixinException th) {
                    this.logger.warn(String.format("Class mixin failed: %s %s", th.getClass().getName(), th.getMessage()), th);
                    th.printStackTrace();
                }
            }

            return basicClass;
        } catch (Exception ex) {
            throw new MixinTransformerError("An unexpected critical error was encountered", ex);
        } finally {
            this.reEntranceCheck--;
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
     * If re-entrance is detected, attempt to find the source and log a warning
     */
    private void detectReEntrance() {
        Set<String> transformerClasses = new HashSet<String>();
        for (IClassTransformer transformer : Launch.classLoader.getTransformers()) {
            transformerClasses.add(transformer.getClass().getName());
        }
        
        transformerClasses.remove(this.getClass().getName());
        
        for (StackTraceElement stackElement : Thread.currentThread().getStackTrace()) {
            if (transformerClasses.contains(stackElement.getClassName())) {
                this.logger.warn("Re-entrance detected from transformer " + stackElement.getClassName() + ", this will cause serious problems.");
                return;
            }
        }

        this.logger.warn("Re-entrance detected from unknown source, this will cause serious problems.", new RuntimeException());
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
        
        for (MixinInfo mixin : mixins) {
            this.logger.log(mixin.getLoggingLevel(), "Mixing {} into {}", mixin.getName(), transformedName);
            this.applyMixin(transformedName, targetClass, mixin.createContextForTarget(targetClass));
        }
        
        // Extension point
        this.postTransform(transformedName, targetClass, mixins);
        
        // Run CheckClassAdapter on the mixin bytecode if debug option is enabled 
        if (MixinTransformer.DEBUG_VERIFY) {
            targetClass.accept(new CheckClassAdapter(new ClassWriter(ClassWriter.COMPUTE_FRAMES)));
        }
        
        // Collapse tree to bytes
        byte[] bytes = this.writeClass(targetClass);
        
        // Export transformed class for debugging purposes
        if (MixinTransformer.DEBUG_EXPORT) {
            try {
                FileUtils.writeByteArrayToFile(new File(".mixin.out/" + transformedName.replace('.', '/') + ".class"), bytes);
            } catch (IOException ex) {
                // don't care
            }
        }
        
        return bytes;
    }

    /**
     * @param transformedName Target class transformed name
     * @param targetClass Target class
     * @param mixins Mixin which were just applied
     */
    protected void postTransform(String transformedName, ClassNode targetClass, SortedSet<MixinInfo> mixins) {
        // Stub for subclasses
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
        } catch (Exception ex) {
            throw new InvalidMixinException("Unexpecteded error whilst applying the mixin class", ex);
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
        for (Iterator<FieldNode> iter = mixin.getClassNode().fields.iterator(); iter.hasNext();) {
            FieldNode field = iter.next();
            AnnotationNode shadow = ASMHelper.getVisibleAnnotation(field, Shadow.class);
            this.validateField(mixin, field, shadow);
            if (!mixin.transformField(field)) {
                iter.remove();
                continue;
            }
            
            FieldNode target = this.findTargetField(targetClass, field);
            if (target == null) {
                // If this field is a shadow field but is NOT found in the target class, that's bad, mmkay
                if (shadow != null) {
                    throw new InvalidMixinException(String.format("Shadow field %s was not located in the target class", field.name));
                }
                
                // This is just a local field, so add it
                targetClass.fields.add(field);
            } else {
                // Check that the shadow field has a matching descriptor
                if (!target.desc.equals(field.desc)) {
                    throw new InvalidMixinException(String.format("The field %s in the target class has a conflicting signature", field.name));
                }
            }
        }
    }

    /**
     * Field sanity checks
     * @param mixin
     * @param field
     * @param shadow
     */
    private void validateField(MixinTargetContext mixin, FieldNode field, AnnotationNode shadow) {
        // Public static fields will fall foul of early static binding in java, including them in a mixin is an error condition
        if (MixinTransformer.hasFlag(field, Opcodes.ACC_STATIC) && !MixinTransformer.hasFlag(field, Opcodes.ACC_PRIVATE)) {
            throw new InvalidMixinException(String.format("Mixin classes cannot contain visible static methods or fields, found %s", field.name));
        }

        // Shadow fields can't have prefixes, it's meaningless for them anyway
        String prefix = ASMHelper.<String>getAnnotationValue(shadow, "prefix", Shadow.class);
        if (field.name.startsWith(prefix)) {
            throw new InvalidMixinException(String.format("Shadow field %s in %s has a shadow prefix. This is not allowed.", field.name, mixin));
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
            boolean isOverwrite = ASMHelper.getVisibleAnnotation(mixinMethod, Overwrite.class) != null;
            boolean isAbstract = MixinTransformer.hasFlag(mixinMethod, Opcodes.ACC_ABSTRACT);
            
            if (isShadow || isAbstract) {
                // For shadow (and abstract, which can be used as a shorthand for Shadow) methods, we just check they're present
                MethodNode target = this.findTargetMethod(targetClass, mixinMethod);
                if (target == null) {
                    throw new InvalidMixinException(String.format("Shadow method %s was not located in the target class", mixinMethod.name));
                }
            } else if (!mixinMethod.name.startsWith("<")) {
                if (MixinTransformer.hasFlag(mixinMethod, Opcodes.ACC_STATIC)
                        && !MixinTransformer.hasFlag(mixinMethod, Opcodes.ACC_PRIVATE)
                        && !MixinTransformer.hasFlag(mixinMethod, Opcodes.ACC_SYNTHETIC)
                        && !isOverwrite) {
                    throw new InvalidMixinException(
                            String.format("Mixin classes cannot contain visible static methods or fields, found %s", mixinMethod.name));
                }
                
                this.mergeMethod(targetClass, mixin, mixinMethod, isOverwrite);
            } else if (MixinTransformer.CLINIT.equals(mixinMethod.name)) {
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
        MethodNode target = this.findTargetMethod(targetClass, method);
        
        if (target != null) {
            AnnotationNode merged = ASMHelper.getVisibleAnnotation(target, MixinMerged.class);
            if (merged != null) {
                String sessionId = ASMHelper.<String>getAnnotationValue(merged, "sessionId");
                
                if (!this.sessionId.equals(sessionId)) {
                    throw new ClassFormatError("Invalid @MixinMerged annotation found in" + mixin + " at " + method.name + " in " + targetClass.name);
                }

                String owner = ASMHelper.<String>getAnnotationValue(merged, "mixin");
                int priority = ASMHelper.<Integer>getAnnotationValue(merged, "priority");
                
                if (priority >= mixin.getPriority() && !owner.equals(mixin.getClassName())) {
                    this.logger.warn("Method overwrite conflict for {}, previously written by {}. Skipping method.", method.name, owner);
                    return;
                }
            }

            targetClass.methods.remove(target);
        } else if (isOverwrite) {
            throw new InvalidMixinException(String.format("Overwrite target %s was not located in the target class", method.name));
        }
        
        targetClass.methods.add(method);
        mixin.getTargetClassInfo().addMethod(method);
        
        ASMHelper.setVisibleAnnotation(method, MixinMerged.class,
                "mixin", mixin.getClassName(),
                "priority", mixin.getPriority(),
                "sessionId", this.sessionId);
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
        InsnList initialiser = this.getInitialiser(ctor);
        if (initialiser == null || initialiser.size() == 0) {
            return;
        }
        
        // Patch the initialiser into the target class ctors
        for (MethodNode method : targetClass.methods) {
            if (MixinTransformer.INIT.equals(method.name)) {
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
            if (MixinTransformer.INIT.equals(mixinMethod.name)) {
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
                if (insn.getOpcode() == Opcodes.INVOKESPECIAL && MixinTransformer.INIT.equals(((MethodInsnNode)insn).name) && superIndex == -1) {
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
    private InsnList getInitialiser(MethodNode ctor) {
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
                            throw new InvalidMixinException("Cannot handle " + ASMHelper.getOpcodeName(opcode) + " opcode (0x"
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
                throw new InvalidMixinException("Could not parse initialiser, expected 0xB5, found 0x" + Integer.toHexString(last.getOpcode()));
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
            if (insn.getOpcode() == Opcodes.INVOKESPECIAL && MixinTransformer.INIT.equals(((MethodInsnNode)insn).name)) {
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
        for (MethodNode method : targetClass.methods) {
            InjectionInfo injectInfo = InjectionInfo.parse(mixin, method);
            if (injectInfo == null) {
                continue;
            }
            
            if (injectInfo.isValid()) {
                injectInfo.inject();
            }
            
            method.visibleAnnotations.remove(injectInfo.getAnnotation());
        }
    }
    
    /**
     * Finds a method in the target class
     * 
     * @param targetClass
     * @param searchFor
     * @return Target method matching searchFor, or null if not found
     */
    private MethodNode findTargetMethod(ClassNode targetClass, MethodNode searchFor) {
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
    private FieldNode findTargetField(ClassNode targetClass, FieldNode searchFor) {
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
    private static boolean hasFlag(MethodNode method, int flag) {
        return (method.access & flag) == flag;
    }
    
    /**
     * Check whether the specified flag is set on the specified field
     * 
     * @param field
     * @param flag 
     * @return True if the specified flag is set in this field's access flags
     */
    private static boolean hasFlag(FieldNode field, int flag) {
        return (field.access & flag) == flag;
    }
}
