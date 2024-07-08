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
import java.util.*;
import java.util.Map.Entry;

import org.spongepowered.asm.logging.ILogger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.extensibility.IActivityContext.IActivity;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.struct.Constructor;
import org.spongepowered.asm.mixin.throwables.MixinError;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Field;
import org.spongepowered.asm.mixin.transformer.ext.extensions.ExtensionClassExporter;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.mixin.transformer.meta.MixinRenamed;
import org.spongepowered.asm.mixin.transformer.struct.Initialiser;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.mixin.transformer.throwables.MixinApplicatorException;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.MixinService;
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
import com.google.common.collect.ImmutableSet;

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
        INJECT_PREPARE,
        
        /**
         * Apply accessors and invokers 
         */
        ACCESSOR,
        
        /**
         * Apply preinjection steps on injectors from previous pass 
         */
        INJECT_PREINJECT,
        
        /**
         * Apply injectors from previous pass 
         */
        INJECT_APPLY

    }
    
    /**
     * Order collection to use for all passes except ApplicatorPass.INJECT
     */
    protected static final Set<Integer> ORDERS_NONE = ImmutableSet.<Integer>of(Integer.valueOf(0)); 

    /**
     * Log more things
     */
    protected final ILogger logger = MixinService.getService().getLogger("mixin");
    
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
     * Target class info 
     */
    protected final ClassInfo targetClassInfo;
    
    /**
     * Profiler 
     */
    protected final Profiler profiler = Profiler.getProfiler("mixin");
    
    /**
     * Audit trail (if available); 
     */
    protected final IMixinAuditTrail auditTrail;

    /**
     * Activity tracker
     */
    protected final ActivityStack activities = new ActivityStack();

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
        this.targetClassInfo = context.getClassInfo();
        
        ExtensionClassExporter exporter = context.getExtensions().<ExtensionClassExporter>getExtension(ExtensionClassExporter.class);
        this.mergeSignatures = exporter.isDecompilerActive()
                && MixinEnvironment.getCurrentEnvironment().getOption(Option.DEBUG_EXPORT_DECOMPILE_MERGESIGNATURES);
        
        this.auditTrail = MixinService.getService().getAuditTrail();
    }
    
    /**
     * Apply supplied mixins to the target class
     */
    final void apply(SortedSet<MixinInfo> mixins) {
        List<MixinTargetContext> mixinContexts = new ArrayList<MixinTargetContext>();
        
        for (Iterator<MixinInfo> iter = mixins.iterator(); iter.hasNext();) {
            MixinInfo mixin = iter.next();
            try {
                this.logger.log(mixin.getLoggingLevel(), "Mixing {} from {} into {}", mixin.getName(), mixin.getParent(), this.targetName);
                mixinContexts.add(mixin.createContextFor(this.context));
                if (this.auditTrail != null) {
                    this.auditTrail.onApply(this.targetName, mixin.toString());
                }
            } catch (InvalidMixinException ex) {
                if (mixin.isRequired()) {
                    throw ex;
                }
                this.context.addSuppressed(ex);
                iter.remove(); // Do not process this mixin further
            }
        }
        
        MixinTargetContext current = null;
        
        this.activities.clear();
        try {
            IActivity activity = this.activities.begin("PreApply Phase");
            IActivity preApplyActivity = this.activities.begin("Mixin");
            for (MixinTargetContext context : mixinContexts) {
                preApplyActivity.next(context.toString());
                (current = context).preApply(this.targetName, this.targetClass);
            }
            preApplyActivity.end();
            
            for (ApplicatorPass pass : ApplicatorPass.values()) {
                activity.next("%s Applicator Phase", pass);
                Section timer = this.profiler.begin("pass", pass.name().toLowerCase(Locale.ROOT));
                IActivity applyActivity = this.activities.begin("Mixin");
                
                Set<Integer> orders = MixinApplicatorStandard.ORDERS_NONE;
                if (pass == ApplicatorPass.INJECT_APPLY) {
                    orders = new TreeSet<Integer>();
                    for (MixinTargetContext context : mixinContexts) {
                        context.getInjectorOrders(orders);
                    }
                }
            
                for (Integer injectorOrder : orders) {
                    for (Iterator<MixinTargetContext> iter = mixinContexts.iterator(); iter.hasNext();) {
                        current = iter.next();
                        applyActivity.next(current.toString());
                        try {
                            this.applyMixin(current, pass, injectorOrder.intValue());
                        } catch (InvalidMixinException ex) {
                            if (current.isRequired()) {
                                throw ex;
                            }
                            this.context.addSuppressed(ex);
                            iter.remove(); // Do not process this mixin further
                        }
                    }
                }
                applyActivity.end();
                timer.end();
            }
            
            activity.next("PostApply Phase");
            IActivity postApplyActivity = this.activities.begin("Mixin");
            for (Iterator<MixinTargetContext> iter = mixinContexts.iterator(); iter.hasNext();) {
                current = iter.next();
                postApplyActivity.next(current.toString());
                try {
                    current.postApply(this.targetName, this.targetClass);
                } catch (InvalidMixinException ex) {
                    if (current.isRequired()) {
                        throw ex;
                    }
                    this.context.addSuppressed(ex);
                    iter.remove();
                }
            }
            activity.end();
        } catch (InvalidMixinException ex) {
            ex.prepend(this.activities);
            throw ex;
        } catch (Exception ex) {
            throw new MixinApplicatorException(current, "Unexpecteded " + ex.getClass().getSimpleName() + " whilst applying the mixin class:", ex,
                    this.activities);
        }

        this.applySourceMap(this.context);
        this.context.processDebugTasks();
    }

    /**
     * Apply the mixin described by mixin to the supplied ClassNode
     * 
     * @param mixin Mixin to apply
     */
    protected final void applyMixin(MixinTargetContext mixin, ApplicatorPass pass, int injectorOrder) {
        IActivity activity = this.activities.begin("Apply");
        switch (pass) {
            case MAIN:
                activity.next("Apply Signature");
                this.applySignature(mixin);
                activity.next("Apply Interfaces");
                this.applyInterfaces(mixin);
                activity.next("Apply Attributess");
                this.applyAttributes(mixin);
                activity.next("Apply Annotations");
                this.applyAnnotations(mixin);
                activity.next("Apply Fields");
                this.applyFields(mixin);
                activity.next("Apply Methods");
                this.applyMethods(mixin);
                activity.next("Apply Initialisers");
                this.applyInitialisers(mixin);
                break;
                
            case INJECT_PREPARE:
                activity.next("Prepare Injections");
                this.prepareInjections(mixin);
                break;
                
            case ACCESSOR:
                activity.next("Apply Accessors");
                this.applyAccessors(mixin);
                break;
                
            case INJECT_PREINJECT:
                activity.next("Apply Injections");
                this.applyPreInjections(mixin);
                break;
                
            case INJECT_APPLY:
                activity.next("Apply Injections");
                this.applyInjections(mixin, injectorOrder);
                break;
                
            default:
                // wat?
                throw new IllegalStateException("Invalid pass specified " + pass);
        }
        activity.end();
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
                this.targetClassInfo.addInterface(interfaceName);
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
        
        int requiredVersion = mixin.getMinRequiredClassVersion();
        if ((requiredVersion & 0xFFFF) > (this.targetClass.version & 0xFFFF)) {
            this.targetClass.version = requiredVersion;
        }
    }

    /**
     * Mixin class-level annotations on the mixin into the target class
     * 
     * @param mixin mixin target context
     */
    protected void applyAnnotations(MixinTargetContext mixin) {
        ClassNode sourceClass = mixin.getClassNode();
        Annotations.merge(sourceClass, this.targetClass);
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
                Annotations.merge(shadow, target);
                
                // Strip the FINAL flag from @Mutable fields
                if (entry.getValue().isDecoratedMutable()) {
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
        IActivity activity = this.activities.begin("?");
        for (MethodNode shadow : mixin.getShadowMethods()) {
            activity.next("@Shadow %s:%s", shadow.desc, shadow.name);
            this.applyShadowMethod(mixin, shadow);
        }
        
        for (MethodNode mixinMethod : mixin.getMethods()) {
            activity.next("%s:%s", mixinMethod.desc, mixinMethod.name);
            this.applyNormalMethod(mixin, mixinMethod);
        }
        activity.end();
    }

    protected void applyShadowMethod(MixinTargetContext mixin, MethodNode shadow) {
        MethodNode target = this.findTargetMethod(shadow);
        if (target != null) {
            Annotations.merge(shadow, target);
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
            IActivity activity = this.activities.begin("Merge CLINIT insns");
            this.appendInsns(mixin, mixinMethod);
            activity.end();
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
    @SuppressWarnings("unchecked")
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
        
        AnnotationNode accMethod = Annotations.getSingleVisible(method, Accessor.class, Invoker.class);
        if (accMethod != null) {
            AnnotationNode accTarget = Annotations.getSingleVisible(target, Accessor.class, Invoker.class);
            if (accTarget != null) {
                String myTarget = Annotations.<String>getValue(accMethod, "target");
                String trTarget = Annotations.<String>getValue(accTarget, "target");
                if (myTarget == null) {
                    throw new MixinError("Encountered undecorated Accessor method in " + mixin + " applying to " + this.targetName);
                }
                if (myTarget.equals(trTarget)) {
                    // This is fine, the accessors overlap
                    return true;
                }
                throw new InvalidMixinException(mixin, String.format("Incompatible @%s %s (for %s) in %s previously written by %s (for %s)",
                        Annotations.getSimpleName(accMethod), method.name, myTarget, mixin, owner, trTarget));
            }
        }

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
            List<AbstractInsnNode> returnNodes = Bytecode.findAllInsns(target, Opcodes.RETURN);
            if (!returnNodes.isEmpty()) {
                // Replace all existing return instructions with a GOTO to the start of the newly appended code
                LabelNode appendedCodeStartLabel = new LabelNode();
                for (AbstractInsnNode returnNode : returnNodes) {
                    target.instructions.set(returnNode, new JumpInsnNode(Opcodes.GOTO, appendedCodeStartLabel));
                }
                target.instructions.add(appendedCodeStartLabel);

                // Append all the new code to the end of the target method, excluding line numbers
                Iterator<AbstractInsnNode> injectIter = method.instructions.iterator();
                while (injectIter.hasNext()) {
                    AbstractInsnNode insn = injectIter.next();
                    if (!(insn instanceof LineNumberNode)) {
                        injectIter.remove();
                        target.instructions.add(insn);
                    }
                }
                
                target.maxLocals = Math.max(target.maxLocals, method.maxLocals);
                target.maxStack = Math.max(target.maxStack, method.maxStack);

                // Merge incoming try-catch blocks into the target method
                target.tryCatchBlocks.addAll(method.tryCatchBlocks);
                // We could probably copy over local variable information as well?
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
        // Find the initialiser in the candidate ctor
        Initialiser initialiser = mixin.getInitialiser();
        if (initialiser == null || initialiser.size() == 0) {
            return;
        }
        
        // Patch the initialiser into the target class ctors
        for (Constructor ctor : this.context.getConstructors()) {
            if (ctor.isInjectable()) {
                int extraStack = initialiser.getMaxStack() - ctor.getMaxStack();
                if (extraStack > 0) {
                    ctor.extendStack().add(extraStack);
                }
                initialiser.injectInto(ctor);
            }
        }
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
     * Run preinject application on all injectors discovered in the previous
     * pass
     * 
     * @param mixin Mixin being applied
     * @param injectorOrder injector order for this pass
     */
    protected void applyPreInjections(MixinTargetContext mixin) {
        mixin.applyPreInjections();
    }

    /**
     * Apply all injectors discovered in the previous pass
     * 
     * @param mixin Mixin being applied
     * @param injectorOrder injector order for this pass
     */
    protected void applyInjections(MixinTargetContext mixin, int injectorOrder) {
        mixin.applyInjections(injectorOrder);
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
            if (target.name.equals(searchFor.name) && target.desc.equals(searchFor.desc)) {
                return target;
            }
        }

        return null;
    }
    
}
