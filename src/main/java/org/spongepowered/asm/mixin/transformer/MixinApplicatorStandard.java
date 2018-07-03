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

import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.Label;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.signature.SignatureReader;
import org.spongepowered.asm.lib.signature.SignatureVisitor;
import org.spongepowered.asm.lib.tree.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Field;
import org.spongepowered.asm.mixin.transformer.ext.extensions.ExtensionClassExporter;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.mixin.transformer.meta.MixinRenamed;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.ConstraintParser;
import org.spongepowered.asm.util.ConstraintParser.Constraint;
import org.spongepowered.asm.util.perf.Profiler;
import org.spongepowered.asm.util.perf.Profiler.Section;
import org.spongepowered.asm.util.throwables.ConstraintViolationException;
import org.spongepowered.asm.util.throwables.InvalidConstraintException;

import com.google.common.collect.ImmutableList;

/**
 * Applies mixins to a target class
 */
class MixinApplicatorStandard {
    
    /**
     * Annotations which can have constraints
     */
    protected static final List<Class<? extends Annotation>> CONSTRAINED_ANNOTATIONS = ImmutableList.<Class<? extends Annotation>>of(
        Overwrite.class,
        Inject.class,
        ModifyArg.class,
        ModifyArgs.class,
        Redirect.class,
        ModifyVariable.class,
        ModifyConstant.class
    );
    
    /**
     * Passes the mixin applicator applies to each mixin
     */
    enum ApplicatorPass {
        /**
         * Main pass, mix in methods, fields, interfaces etc
         */
        MAIN,
        
        /**
         * Enumerate injectors and scan for injection points 
         */
        PREINJECT,
        
        /**
         * Apply injectors from previous pass 
         */
        INJECT
    }
    
    /**
     * Strategy for injecting initialiser insns
     */
    enum InitialiserInjectionMode {
        /**
         * Default mode, attempts to place initialisers after all other
         * competing initialisers in the target ctor
         */
        DEFAULT,
        
        /**
         * Safe mode, only injects initialiser directly after the super-ctor
         * invocation 
         */
        SAFE
    }
    
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
     * List of opcodes which must not appear in a class initialiser, mainly a
     * sanity check so that if any of the specified opcodes are found, we can
     * log it as an error condition and then people can bitch at me to fix it.
     * Essentially if it turns out that field initialisers can somehow make use
     * of local variables, then I need to write some code to ensure that said
     * locals are shifted so that they don't interfere with locals in the
     * receiving constructor. 
     */
    protected static final int[] INITIALISER_OPCODE_BLACKLIST = {
        Opcodes.RETURN, Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.IALOAD, Opcodes.LALOAD, Opcodes.FALOAD, Opcodes.DALOAD,
        Opcodes.AALOAD, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD, Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE,
        Opcodes.ASTORE, Opcodes.IASTORE, Opcodes.LASTORE, Opcodes.FASTORE, Opcodes.DASTORE, Opcodes.AASTORE, Opcodes.BASTORE, Opcodes.CASTORE,
        Opcodes.SASTORE
    };

    /**
     * Log more things
     */
    protected final Logger logger = LogManager.getLogger("mixin");
    
    /**
     * Target class context 
     */
    protected final TargetClassContext context;
    
    /**
     * Target class name
     */
    protected final String targetName;
    
    /**
     * Target class tree 
     */
    protected final ClassNode targetClass;
    
    /**
     * Profiler 
     */
    protected final Profiler profiler = MixinEnvironment.getProfiler();
    
    /**
     * Flag to track whether signatures from applied mixins should be merged
     * into target classes. This is only true when the runtime decompiler is
     * active. If this is disabled, signatures on merged mixin methods are
     * stripped instead of remapped.
     */
    protected final boolean mergeSignatures;
    
    MixinApplicatorStandard(TargetClassContext context) {
        this.context = context;
        this.targetName = context.getClassName();
        this.targetClass = context.getClassNode();
        
        ExtensionClassExporter exporter = context.getExtensions().<ExtensionClassExporter>getExtension(ExtensionClassExporter.class);
        this.mergeSignatures = exporter.isDecompilerActive()
                && MixinEnvironment.getCurrentEnvironment().getOption(Option.DEBUG_EXPORT_DECOMPILE_MERGESIGNATURES);
    }
    
    /**
     * Apply supplied mixins to the target class
     */
    void apply(SortedSet<MixinInfo> mixins) {
        List<MixinTargetContext> mixinContexts = new ArrayList<MixinTargetContext>();
        
        for (MixinInfo mixin : mixins) {
            this.logger.log(mixin.getLoggingLevel(), "Mixing {} from {} into {}", mixin.getName(), mixin.getParent(), this.targetName);
            mixinContexts.add(mixin.createContextFor(this.context));
        }
        
        MixinTargetContext current = null;
        
        try {
            for (MixinTargetContext context : mixinContexts) {
                (current = context).preApply(this.targetName, this.targetClass);
            }
            
            for (ApplicatorPass pass : ApplicatorPass.values()) {
                Section timer = this.profiler.begin("pass", pass.name().toLowerCase());
                for (MixinTargetContext context : mixinContexts) {
                    this.applyMixin(current = context, pass);
                }
                timer.end();
            }
            
            for (MixinTargetContext context : mixinContexts) {
                (current = context).postApply(this.targetName, this.targetClass);
            }
        } catch (InvalidMixinException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidMixinException(current, "Unexpecteded " + ex.getClass().getSimpleName() + " whilst applying the mixin class: "
                    + ex.getMessage(), ex);
        }

        this.applySourceMap(this.context);
        this.context.processDebugTasks();
    }

    /**
     * Apply the mixin described by mixin to the supplied classNode
     * 
     * @param mixin Mixin to apply
     */
    protected final void applyMixin(MixinTargetContext mixin, ApplicatorPass pass) {
        switch (pass) {
            case MAIN:
                this.applySignature(mixin);
                this.applyInterfaces(mixin);
                this.applyAttributes(mixin);
                this.applyAnnotations(mixin);
                this.applyFields(mixin);
                this.applyMethods(mixin);
                this.applyInitialisers(mixin);
                break;
                
            case PREINJECT:
                this.prepareInjections(mixin);
                break;
                
            case INJECT:
                this.applyAccessors(mixin);
                this.applyInjections(mixin);
                break;
                
            default:
                // wat?
                throw new IllegalStateException("Invalid pass specified " + pass);
        }
    }

    protected void applySignature(MixinTargetContext mixin) {
        if (this.mergeSignatures) {
            this.context.mergeSignature(mixin.getSignature());
        }
    }

    /**
     * Mixin interfaces implemented by the mixin class onto the target class
     * 
     * @param mixin mixin target context
     */
    protected void applyInterfaces(MixinTargetContext mixin) {
        for (String interfaceName : mixin.getInterfaces()) {
            if (!this.targetClass.interfaces.contains(interfaceName)) {
                this.targetClass.interfaces.add(interfaceName);
                mixin.getTargetClassInfo().addInterface(interfaceName);
            }
        }
    }

    /**
     * Mixin misc attributes from mixin class onto the target class
     * 
     * @param mixin mixin target context
     */
    protected void applyAttributes(MixinTargetContext mixin) {
        if (mixin.shouldSetSourceFile()) {
            this.targetClass.sourceFile = mixin.getSourceFile();
        }
        this.targetClass.version = Math.max(this.targetClass.version, mixin.getMinRequiredClassVersion());
    }

    /**
     * Mixin class-level annotations on the mixin into the target class
     * 
     * @param mixin mixin target context
     */
    protected void applyAnnotations(MixinTargetContext mixin) {
        ClassNode sourceClass = mixin.getClassNode();
        Bytecode.mergeAnnotations(sourceClass, this.targetClass);
    }
    
    /**
     * Mixin fields from mixin class into the target class. It is vital that
     * this is done before mixinMethods because we need to compute renamed
     * fields so that transformMethod can rename field references in the method
     * body.
     * 
     * @param mixin mixin target context
     */
    protected void applyFields(MixinTargetContext mixin) {
        this.mergeShadowFields(mixin);
        this.mergeNewFields(mixin);
    }

    protected void mergeShadowFields(MixinTargetContext mixin) {
        for (Entry<FieldNode, Field> entry : mixin.getShadowFields()) {
            FieldNode shadow = entry.getKey();
            FieldNode target = this.findTargetField(shadow);
            if (target != null) {
                Bytecode.mergeAnnotations(shadow, target);
                
                // Strip the FINAL flag from @Mutable non-private fields
                if (entry.getValue().isDecoratedMutable() && !Bytecode.hasFlag(target, Opcodes.ACC_PRIVATE)) {
                    target.access &= ~Opcodes.ACC_FINAL;
                }
            }
        }
    }

    protected void mergeNewFields(MixinTargetContext mixin) {
        for (FieldNode field : mixin.getFields()) {
            FieldNode target = this.findTargetField(field);
            if (target == null) {
                // This is just a local field, so add it
                this.targetClass.fields.add(field);
                
                if (field.signature != null) {
                    if (this.mergeSignatures) {
                        SignatureVisitor sv = mixin.getSignature().getRemapper();
                        new SignatureReader(field.signature).accept(sv);
                        field.signature = sv.toString();
                    } else {
                        field.signature = null;
                    }
                }
            }
        }
    }

    /**
     * Mixin methods from the mixin class into the target class
     * 
     * @param mixin mixin target context
     */
    protected void applyMethods(MixinTargetContext mixin) {
        for (MethodNode shadow : mixin.getShadowMethods()) {
            this.applyShadowMethod(mixin, shadow);
        }
        
        for (MethodNode mixinMethod : mixin.getMethods()) {
            this.applyNormalMethod(mixin, mixinMethod);
        }
    }

    protected void applyShadowMethod(MixinTargetContext mixin, MethodNode shadow) {
        MethodNode target = this.findTargetMethod(shadow);
        if (target != null) {
            Bytecode.mergeAnnotations(shadow, target);
        }
    }

    protected void applyNormalMethod(MixinTargetContext mixin, MethodNode mixinMethod) {
        // Reparent all mixin methods into the target class
        mixin.transformMethod(mixinMethod);

        if (!mixinMethod.name.startsWith("<")) {
            this.checkMethodVisibility(mixin, mixinMethod);
            this.checkMethodConstraints(mixin, mixinMethod);
            this.mergeMethod(mixin, mixinMethod);
        } else if (Constants.CLINIT.equals(mixinMethod.name)) {
            // Class initialiser insns get appended
            this.appendInsns(mixin, mixinMethod);
        }
    }

    /**
     * Attempts to merge the supplied method into the target class
     * 
     * @param mixin Mixin being applied
     * @param method Method to merge
     */
    protected void mergeMethod(MixinTargetContext mixin, MethodNode method) {
        boolean isOverwrite = Annotations.getVisible(method, Overwrite.class) != null;
        MethodNode target = this.findTargetMethod(method);
        
        if (target != null) {
            if (this.isAlreadyMerged(mixin, method, isOverwrite, target)) {
                return;
            }
            
            AnnotationNode intrinsic = Annotations.getInvisible(method, Intrinsic.class);
            if (intrinsic != null) {
                if (this.mergeIntrinsic(mixin, method, isOverwrite, target, intrinsic)) {
                    mixin.getTarget().methodMerged(method);
                    return;
                }
            } else {
                if (mixin.requireOverwriteAnnotations() && !isOverwrite) {
                    throw new InvalidMixinException(mixin,
                            String.format("%s%s in %s cannot overwrite method in %s because @Overwrite is required by the parent configuration",
                            method.name, method.desc, mixin, mixin.getTarget().getClassName()));
                }
                
                this.targetClass.methods.remove(target);
            }
        } else if (isOverwrite) {
            throw new InvalidMixinException(mixin, String.format("Overwrite target \"%s\" was not located in target class %s",
                    method.name, mixin.getTargetClassRef()));
        }
        
        this.targetClass.methods.add(method);
        mixin.methodMerged(method);
        
        if (method.signature != null) {
            if (this.mergeSignatures) {
                SignatureVisitor sv = mixin.getSignature().getRemapper();
                new SignatureReader(method.signature).accept(sv);
                method.signature = sv.toString();
            } else {
                method.signature = null;
            }
        }
    }

    /**
     * Check whether this method was already merged into the target, returns
     * false if the method was <b>not</b> already merged or if the incoming
     * method has a higher priority than the already merged method.
     * 
     * @param mixin Mixin context
     * @param method Method being merged
     * @param isOverwrite True if the incoming method is tagged with Override
     * @param target target method being checked
     * @return true if the target was already merged and should be skipped
     */
    protected boolean isAlreadyMerged(MixinTargetContext mixin, MethodNode method, boolean isOverwrite, MethodNode target) {
        AnnotationNode merged = Annotations.getVisible(target, MixinMerged.class);
        if (merged == null) {
            if (Annotations.getVisible(target, Final.class) != null) {
                this.logger.warn("Overwrite prohibited for @Final method {} in {}. Skipping method.", method.name, mixin);
                return true;
            }
            return false;
        }
    
        String sessionId = Annotations.<String>getValue(merged, "sessionId");
        
        if (!this.context.getSessionId().equals(sessionId)) {
            throw new ClassFormatError("Invalid @MixinMerged annotation found in" + mixin + " at " + method.name + " in " + this.targetClass.name);
        }
        
        if (Bytecode.hasFlag(target, Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE)
                && Bytecode.hasFlag(method, Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE)) {
            if (mixin.getEnvironment().getOption(Option.DEBUG_VERBOSE)) {
                this.logger.warn("Synthetic bridge method clash for {} in {}", method.name, mixin);
            }
            return true;
        }
        
        String owner = Annotations.<String>getValue(merged, "mixin");
        int priority = Annotations.<Integer>getValue(merged, "priority");
        
        if (priority >= mixin.getPriority() && !owner.equals(mixin.getClassName())) {
            this.logger.warn("Method overwrite conflict for {} in {}, previously written by {}. Skipping method.", method.name, mixin, owner);
            return true;
        }
        
        if (Annotations.getVisible(target, Final.class) != null) {
            this.logger.warn("Method overwrite conflict for @Final method {} in {} declared by {}. Skipping method.", method.name, mixin, owner);
            return true;
        }

        return false;
    }

    /**
     * Validates and prepares an intrinsic merge, returns true if the intrinsic
     * check results in a "skip" action, indicating that no further merge action
     * should be undertaken
     * 
     * @param mixin Mixin context
     * @param method Method being merged
     * @param isOverwrite True if the incoming method is tagged with Override
     * @param target target method being checked
     * @param intrinsic {@link Intrinsic} annotation
     * @return true if the intrinsic method was skipped (short-circuit further
     *      merge operations)
     */
    protected boolean mergeIntrinsic(MixinTargetContext mixin, MethodNode method, boolean isOverwrite,
            MethodNode target, AnnotationNode intrinsic) {
        
        if (isOverwrite) {
            throw new InvalidMixinException(mixin, "@Intrinsic is not compatible with @Overwrite, remove one of these annotations on "
                    + method.name + " in " + mixin);
        }
        
        String methodName = method.name + method.desc;
        if (Bytecode.hasFlag(method, Opcodes.ACC_STATIC)) {
            throw new InvalidMixinException(mixin, "@Intrinsic method cannot be static, found " + methodName + " in " + mixin);
        }
        
        if (!Bytecode.hasFlag(method, Opcodes.ACC_SYNTHETIC)) {
            AnnotationNode renamed = Annotations.getVisible(method, MixinRenamed.class);
            if (renamed == null || !Annotations.getValue(renamed, "isInterfaceMember", Boolean.FALSE)) {
                throw new InvalidMixinException(mixin, "@Intrinsic method must be prefixed interface method, no rename encountered on "
                        + methodName + " in " + mixin);
            }
        }
        
        if (!Annotations.getValue(intrinsic, "displace", Boolean.FALSE)) {
            this.logger.log(mixin.getLoggingLevel(), "Skipping Intrinsic mixin method {} for {}", methodName, mixin.getTargetClassRef());
            return true;
        }
        
        this.displaceIntrinsic(mixin, method, target);
        return false;
    }

    /**
     * Handles intrinsic displacement
     * 
     * @param mixin Mixin context
     * @param method Method being merged
     * @param target target method being checked
     */
    protected void displaceIntrinsic(MixinTargetContext mixin, MethodNode method, MethodNode target) {
        // Deliberately include invalid character in the method name so that
        // we guarantee no hackiness
        String proxyName = "proxy+" + target.name;
        
        for (Iterator<AbstractInsnNode> iter = method.instructions.iterator(); iter.hasNext();) {
            AbstractInsnNode insn = iter.next();
            if (insn instanceof MethodInsnNode && insn.getOpcode() != Opcodes.INVOKESTATIC) {
                MethodInsnNode methodNode = (MethodInsnNode)insn;
                if (methodNode.owner.equals(this.targetClass.name) && methodNode.name.equals(target.name) && methodNode.desc.equals(target.desc)) {
                    methodNode.name = proxyName;
                }
            }
        }
        
        target.name = proxyName;
    }

    /**
     * Handles appending instructions from the source method to the target
     * method. Both methods must return void
     * 
     * @param mixin mixin target context 
     * @param method source method
     */
    protected final void appendInsns(MixinTargetContext mixin, MethodNode method) {
        if (Type.getReturnType(method.desc) != Type.VOID_TYPE) {
            throw new IllegalArgumentException("Attempted to merge insns from a method which does not return void");
        }
        
        MethodNode target = this.findTargetMethod(method);

        if (target != null) {
            AbstractInsnNode returnNode = Bytecode.findInsn(target, Opcodes.RETURN);
            
            if (returnNode != null) {
                Iterator<AbstractInsnNode> injectIter = method.instructions.iterator();
                while (injectIter.hasNext()) {
                    AbstractInsnNode insn = injectIter.next();
                    if (!(insn instanceof LineNumberNode) && insn.getOpcode() != Opcodes.RETURN) {
                        target.instructions.insertBefore(returnNode, insn);
                    }
                }
                
                target.maxLocals = Math.max(target.maxLocals, method.maxLocals);
                target.maxStack = Math.max(target.maxStack, method.maxStack);
            }
            
            return;
        }
        
        this.targetClass.methods.add(method);
    }
    
    /**
     * (Attempts to) find and patch field initialisers from the mixin into the
     * target class
     * 
     * @param mixin mixin target context
     */
    protected void applyInitialisers(MixinTargetContext mixin) {
        // Try to find a suitable constructor, we need a constructor with line numbers in order to extract the initialiser 
        MethodNode ctor = this.getConstructor(mixin);
        if (ctor == null) {
            return;
        }
        
        // Find the initialiser instructions in the candidate ctor
        Deque<AbstractInsnNode> initialiser = this.getInitialiser(mixin, ctor);
        if (initialiser == null || initialiser.size() == 0) {
            return;
        }
        
        // Patch the initialiser into the target class ctors
        for (MethodNode method : this.targetClass.methods) {
            if (Constants.CTOR.equals(method.name)) {
                method.maxStack = Math.max(method.maxStack, ctor.maxStack);
                this.injectInitialiser(mixin, method, initialiser);
            }
        }
    }
    
    /**
     * Finds a suitable ctor for reading the instance initialiser bytecode
     * 
     * @param mixin mixin to search
     * @return appropriate ctor or null if none found
     */
    protected MethodNode getConstructor(MixinTargetContext mixin) {
        MethodNode ctor = null;
        
        for (MethodNode mixinMethod : mixin.getMethods()) {
            if (Constants.CTOR.equals(mixinMethod.name) && Bytecode.methodHasLineNumbers(mixinMethod)) {
                if (ctor == null) {
                    ctor = mixinMethod;
                } else {
                    // Not an error condition, just weird
                    this.logger.warn(String.format("Mixin %s has multiple constructors, %s was selected\n", mixin, ctor.desc));
                }
            }
        }
        
        return ctor;
    }

    /**
     * Identifies line numbers in the supplied ctor which correspond to the
     * start and end of the method body.
     * 
     * @param ctor constructor to scan
     * @return range indicating the line numbers of the specified constructor
     *      and the position of the superclass ctor invocation
     */
    private Range getConstructorRange(MethodNode ctor) {
        boolean lineNumberIsValid = false;
        AbstractInsnNode endReturn = null;
        
        int line = 0, start = 0, end = 0, superIndex = -1;
        for (Iterator<AbstractInsnNode> iter = ctor.instructions.iterator(); iter.hasNext();) {
            AbstractInsnNode insn = iter.next();
            if (insn instanceof LineNumberNode) {
                line = ((LineNumberNode)insn).line;
                lineNumberIsValid = true;
            } else if (insn instanceof MethodInsnNode) {
                if (insn.getOpcode() == Opcodes.INVOKESPECIAL && Constants.CTOR.equals(((MethodInsnNode)insn).name) && superIndex == -1) {
                    superIndex = ctor.instructions.indexOf(insn);
                    start = line;
                }
            } else if (insn.getOpcode() == Opcodes.PUTFIELD) {
                lineNumberIsValid = false;
            } else if (insn.getOpcode() == Opcodes.RETURN) {
                if (lineNumberIsValid) {
                    end = line;
                } else {
                    end = start;
                    endReturn = insn;
                }
            }
        }
        
        if (endReturn != null) {
            LabelNode label = new LabelNode(new Label());
            ctor.instructions.insertBefore(endReturn, label);
            ctor.instructions.insertBefore(endReturn, new LineNumberNode(start, label));
        }
        
        return new Range(start, end, superIndex);
    }

    /**
     * Get insns corresponding to the instance initialiser (hopefully) from the
     * supplied constructor.
     * 
     * @param mixin mixin target context
     * @param ctor constructor to inspect
     * @return initialiser bytecode extracted from the supplied constructor, or
     *      null if the constructor range could not be parsed
     */
    protected final Deque<AbstractInsnNode> getInitialiser(MixinTargetContext mixin, MethodNode ctor) {
        //
        // TODO Potentially rewrite this to be less horrible.
        //
        
        // Find the range of line numbers which corresponds to the constructor body
        Range init = this.getConstructorRange(ctor);
        if (!init.isValid()) {
            return null;
        }
        
        // Now we know where the constructor is, look for insns which lie OUTSIDE the method body
        int line = 0;
        Deque<AbstractInsnNode> initialiser = new ArrayDeque<AbstractInsnNode>();
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
                    for (int ivalidOp : MixinApplicatorStandard.INITIALISER_OPCODE_BLACKLIST) {
                        if (opcode == ivalidOp) {
                            // At the moment I don't handle any transient locals because I haven't seen any in the wild, but let's avoid writing
                            // code which will likely break things and fix it if a real test case ever appears
                            throw new InvalidMixinException(mixin, "Cannot handle " + Bytecode.getOpcodeName(opcode) + " opcode (0x"
                                    + Integer.toHexString(opcode).toUpperCase() + ") in class initialiser");
                        }
                    }
                    
                    initialiser.add(insn);
                }
            }
        }
        
        // Check that the last insn is a PUTFIELD, if it's not then 
        AbstractInsnNode last = initialiser.peekLast();
        if (last != null) {
            if (last.getOpcode() != Opcodes.PUTFIELD) {
                throw new InvalidMixinException(mixin, "Could not parse initialiser, expected 0xB5, found 0x"
                        + Integer.toHexString(last.getOpcode()) + " in " + mixin);
            }
        }
        
        return initialiser;
    }

    /**
     * Inject initialiser code into the target constructor
     * 
     * @param mixin mixin target context
     * @param ctor target constructor
     * @param initialiser initialiser instructions
     */
    protected final void injectInitialiser(MixinTargetContext mixin, MethodNode ctor, Deque<AbstractInsnNode> initialiser) {
        Map<LabelNode, LabelNode> labels = Bytecode.cloneLabels(ctor.instructions);
        
        AbstractInsnNode insn = this.findInitialiserInjectionPoint(mixin, ctor, initialiser);
        if (insn == null) {
            this.logger.warn("Failed to locate initialiser injection point in <init>{}, initialiser was not mixed in.", ctor.desc);
            return;
        }

        for (AbstractInsnNode node : initialiser) {
            if (node instanceof LabelNode) {
                continue;
            }
            if (node instanceof JumpInsnNode) {
                throw new InvalidMixinException(mixin, "Unsupported JUMP opcode in initialiser in " + mixin);
            }
            AbstractInsnNode imACloneNow = node.clone(labels);
            ctor.instructions.insert(insn, imACloneNow);
            insn = imACloneNow;
        }
    }

    /**
     * Find the injection point for injected initialiser insns in the target
     * ctor
     * 
     * @param mixin target context for mixin being applied
     * @param ctor target ctor
     * @param initialiser source initialiser insns
     * @return target node
     */
    protected AbstractInsnNode findInitialiserInjectionPoint(MixinTargetContext mixin, MethodNode ctor, Deque<AbstractInsnNode> initialiser) {
        Set<String> initialisedFields = new HashSet<String>();
        for (AbstractInsnNode initialiserInsn : initialiser) {
            if (initialiserInsn.getOpcode() == Opcodes.PUTFIELD) {
                initialisedFields.add(MixinApplicatorStandard.fieldKey((FieldInsnNode)initialiserInsn)); 
            }
        }

        InitialiserInjectionMode mode = this.getInitialiserInjectionMode(mixin.getEnvironment());
        String targetName = mixin.getTargetClassInfo().getName(); 
        String targetSuperName = mixin.getTargetClassInfo().getSuperName();
        AbstractInsnNode targetInsn = null;

        for (Iterator<AbstractInsnNode> iter = ctor.instructions.iterator(); iter.hasNext();) {
            AbstractInsnNode insn = iter.next();
            if (insn.getOpcode() == Opcodes.INVOKESPECIAL && Constants.CTOR.equals(((MethodInsnNode)insn).name)) {
                String owner = ((MethodInsnNode)insn).owner;
                if (owner.equals(targetName) || owner.equals(targetSuperName)) {
                    targetInsn = insn;
                    if (mode == InitialiserInjectionMode.SAFE) {
                        break;
                    }
                }
            } else if (insn.getOpcode() == Opcodes.PUTFIELD && mode == InitialiserInjectionMode.DEFAULT) {
                String key = MixinApplicatorStandard.fieldKey((FieldInsnNode)insn);
                if (initialisedFields.contains(key)) {
                    targetInsn = insn;
                }
            }            
        }
        
        return targetInsn;
    }

    private InitialiserInjectionMode getInitialiserInjectionMode(MixinEnvironment environment) {
        String strMode = environment.getOptionValue(Option.INITIALISER_INJECTION_MODE);
        if (strMode == null) {
            return InitialiserInjectionMode.DEFAULT;
        }
        try {
            return InitialiserInjectionMode.valueOf(strMode.toUpperCase());
        } catch (Exception ex) {
            this.logger.warn("Could not parse unexpected value \"{}\" for mixin.initialiserInjectionMode, reverting to DEFAULT", strMode);
            return InitialiserInjectionMode.DEFAULT;
        }
    }

    private static String fieldKey(FieldInsnNode fieldNode) {
        return String.format("%s:%s", fieldNode.desc, fieldNode.name);
    }

    /**
     * Scan for injector methods and injection points
     * 
     * @param mixin Mixin being scanned
     */
    protected void prepareInjections(MixinTargetContext mixin) {
        mixin.prepareInjections();
    }
    
    /**
     * Apply all injectors discovered in the previous pass
     * 
     * @param mixin Mixin being applied
     */
    protected void applyInjections(MixinTargetContext mixin) {
        mixin.applyInjections();
    }
    
    /**
     * Apply all accessors discovered during preprocessing
     * 
     * @param mixin Mixin being applied
     */
    protected void applyAccessors(MixinTargetContext mixin) {
        List<MethodNode> accessorMethods = mixin.generateAccessors();
        for (MethodNode method : accessorMethods) {
            if (!method.name.startsWith("<")) {
                this.mergeMethod(mixin, method);
            }
        }
    }

    /**
     * Check visibility before merging a mixin method
     * 
     * @param mixin mixin target context
     * @param mixinMethod method to check
     */
    protected void checkMethodVisibility(MixinTargetContext mixin, MethodNode mixinMethod) {
        if (Bytecode.hasFlag(mixinMethod, Opcodes.ACC_STATIC)
                && !Bytecode.hasFlag(mixinMethod, Opcodes.ACC_PRIVATE)
                && !Bytecode.hasFlag(mixinMethod, Opcodes.ACC_SYNTHETIC)
                && Annotations.getVisible(mixinMethod, Overwrite.class) == null) {
            throw new InvalidMixinException(mixin, 
                    String.format("Mixin %s contains non-private static method %s", mixin, mixinMethod));
        }
    }

    protected void applySourceMap(TargetClassContext context) {
        this.targetClass.sourceDebug = context.getSourceMap().toString();
    }

    /**
     * Check constraints in annotations on the specified mixin method
     * 
     * @param mixin Target context
     * @param method Mixin method
     */
    protected void checkMethodConstraints(MixinTargetContext mixin, MethodNode method) {
        for (Class<? extends Annotation> annotationType : MixinApplicatorStandard.CONSTRAINED_ANNOTATIONS) {
            AnnotationNode annotation = Annotations.getVisible(method, annotationType);
            if (annotation != null) {
                this.checkConstraints(mixin, method, annotation);
            }
        }
    }
    
    /**
     * Check constraints for the specified annotation based on token values in
     * the current environment
     * 
     * @param mixin Mixin being applied
     * @param method annotated method
     * @param annotation Annotation node to check constraints
     */
    protected final void checkConstraints(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        try {
            Constraint constraint = ConstraintParser.parse(annotation);
            try {
                constraint.check(mixin.getEnvironment());
            } catch (ConstraintViolationException ex) {
                String message = String.format("Constraint violation: %s on %s in %s", ex.getMessage(), method, mixin);
                this.logger.warn(message);
                if (!mixin.getEnvironment().getOption(Option.IGNORE_CONSTRAINTS)) {
                    throw new InvalidMixinException(mixin, message, ex);
                }
            }
        } catch (InvalidConstraintException ex) {
            throw new InvalidMixinException(mixin, ex.getMessage());
        }
    }

    /**
     * Finds a method in the target class
     * @param searchFor
     * 
     * @return Target method matching searchFor, or null if not found
     */
    protected final MethodNode findTargetMethod(MethodNode searchFor) {
        for (MethodNode target : this.targetClass.methods) {
            if (target.name.equals(searchFor.name) && target.desc.equals(searchFor.desc)) {
                return target;
            }
        }
        
        return null;
    }

    /**
     * Finds a field in the target class
     * @param searchFor
     * 
     * @return Target field matching searchFor, or null if not found
     */
    protected final FieldNode findTargetField(FieldNode searchFor) {
        for (FieldNode target : this.targetClass.fields) {
            if (target.name.equals(searchFor.name)) {
                return target;
            }
        }

        return null;
    }
    
}
