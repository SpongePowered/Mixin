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

import org.spongepowered.asm.lib.Label;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.FieldNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.LabelNode;
import org.spongepowered.asm.lib.tree.LineNumberNode;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.mixin.transformer.meta.MixinRenamed;
import org.spongepowered.asm.util.ASMHelper;
import org.spongepowered.asm.util.Constants;


/**
 * Applies mixins to a target class
 */
public class MixinApplicator {
    
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
    private static final int[] INITIALISER_OPCODE_BLACKLIST = {
        Opcodes.RETURN, Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.IALOAD, Opcodes.LALOAD, Opcodes.FALOAD, Opcodes.DALOAD,
        Opcodes.AALOAD, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD, Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE,
        Opcodes.ASTORE, Opcodes.IASTORE, Opcodes.LASTORE, Opcodes.FASTORE, Opcodes.DASTORE, Opcodes.AASTORE, Opcodes.BASTORE, Opcodes.CASTORE,
        Opcodes.SASTORE
    };

    /**
     * Log more things
     */
    private final Logger logger = LogManager.getLogger("mixin");
    
    /**
     * Session ID, used as a check when parsing {@link MixinMerged} annotations
     * to prevent them being applied at compile time by people trying to
     * circumvent mixin application
     */
    private final String sessionId;
    
    /**
     * Target class name
     */
    private final String targetName;
    
    /**
     * Target class tree 
     */
    private final ClassNode targetClass;
    
    MixinApplicator(String sessionId, String transformedName, ClassNode targetClass) {
        this.sessionId = sessionId;
        this.targetName = transformedName;
        this.targetClass = targetClass;
    }
    
    /**
     * Apply supplied mixins to the target class
     */
    void apply(SortedSet<MixinInfo> mixins) {
        for (MixinInfo mixin : mixins) {
            this.logger.log(mixin.getLoggingLevel(), "Mixing {} into {}", mixin.getName(), this.targetName);
            this.applyMixin(mixin.createContextFor(this.targetClass));
        }
    }

    /**
     * Apply the mixin described by mixin to the supplied classNode
     * 
     * @param mixin Mixin to apply
     */
    private void applyMixin(MixinTargetContext mixin) {
        try {
            mixin.preApply(this.targetName, this.targetClass);
            
            this.applyInterfaces(mixin);
            this.applyAttributes(mixin);
            this.applyAnnotations(mixin);
            this.applyFields(mixin);
            this.applyMethods(mixin);
            this.applyInitialisers(mixin);
            this.applyInjections(mixin);
            
            mixin.postApply(this.targetName, this.targetClass);
        } catch (InvalidMixinException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidMixinException(mixin, "Unexpecteded error whilst applying the mixin class", ex);
        }
    }

    /**
     * Mixin interfaces implemented by the mixin class onto the target class
     * 
     * @param mixin
     */
    private void applyInterfaces(MixinTargetContext mixin) {
        for (String interfaceName : mixin.getInterfaces()) {
            if (!this.targetClass.interfaces.contains(interfaceName)) {
                this.targetClass.interfaces.add(interfaceName);
            }
        }
    }

    /**
     * Mixin misc attributes from mixin class onto the target class
     * 
     * @param mixin
     */
    private void applyAttributes(MixinTargetContext mixin) {
        if (mixin.shouldSetSourceFile()) {
            this.targetClass.sourceFile = mixin.getSourceFile();
        }
    }

    /**
     * Mixin class-level annotations on the mixin into the target class
     * 
     * @param mixin
     */
    private void applyAnnotations(MixinTargetContext mixin) {
        ClassNode sourceClass = mixin.getClassNode();
        this.mergeAnnotations(sourceClass, this.targetClass);
    }
    
    /**
     * Mixin fields from mixin class into the target class. It is vital that
     * this is done before mixinMethods because we need to compute renamed
     * fields so that transformMethod can rename field references in the method
     * body.
     * 
     * @param mixin
     */
    private void applyFields(MixinTargetContext mixin) {
        for (FieldNode shadow : mixin.getShadowFields()) {
            FieldNode target = this.findTargetField(shadow);
            if (target != null) {
                this.mergeAnnotations(shadow, target);
            }
        }
        
        for (FieldNode field : mixin.getFields()) {
            FieldNode target = this.findTargetField(field);
            if (target == null) {
                // This is just a local field, so add it
                this.targetClass.fields.add(field);
            }
        }
    }

    /**
     * Mixin methods from the mixin class into the target class
     * 
     * @param mixin
     */
    private void applyMethods(MixinTargetContext mixin) {
        for (MethodNode shadow : mixin.getShadowMethods()) {
            MethodNode target = this.findTargetMethod(shadow);
            if (target != null) {
                this.mergeAnnotations(shadow, target);
            }
        }
        
        for (MethodNode mixinMethod : mixin.getMethods()) {
            // Reparent all mixin methods into the target class
            mixin.transformMethod(mixinMethod);

            if (!mixinMethod.name.startsWith("<")) {
                boolean isOverwrite = ASMHelper.getVisibleAnnotation(mixinMethod, Overwrite.class) != null;
                
                if (MixinApplicator.hasFlag(mixinMethod, Opcodes.ACC_STATIC)
                        && !MixinApplicator.hasFlag(mixinMethod, Opcodes.ACC_PRIVATE)
                        && !MixinApplicator.hasFlag(mixinMethod, Opcodes.ACC_SYNTHETIC)
                        && !isOverwrite) {
                    throw new InvalidMixinException(mixin, 
                            String.format("Mixin classes cannot contain visible static methods or fields, found %s", mixinMethod.name));
                }
                
                this.mergeMethod(mixin, mixinMethod, isOverwrite);
            } else if (Constants.CLINIT.equals(mixinMethod.name)) {
                // Class initialiser insns get appended
                this.appendInsns(mixinMethod);
            }
        }
    }

    /**
     * Attempts to merge the supplied method into the target class
     * 
     * @param mixin Mixin being applied
     * @param method Method to merge
     * @param isOverwrite true if the method is annotated with an
     *      {@link Overwrite} annotation
     */
    private void mergeMethod(MixinTargetContext mixin, MethodNode method, boolean isOverwrite) {
        MethodNode target = this.findTargetMethod(method);
        
        if (target != null) {
            if (this.alreadyMerged(mixin, method, isOverwrite, target)) {
                return;
            }
            
            AnnotationNode intrinsic = ASMHelper.getInvisibleAnnotation(method, Intrinsic.class);
            if (intrinsic != null) {
                if (this.mergeIntrinsic(mixin, method, isOverwrite, target, intrinsic)) {
                    return;
                }
            } else {
                this.targetClass.methods.remove(target);
            }
        } else if (isOverwrite) {
            throw new InvalidMixinException(mixin, String.format("Overwrite target %s was not located in the target class", method.name));
        }
        
        this.targetClass.methods.add(method);
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
     * @param mixin Mixin context
     * @param method Method being merged
     * @param isOverwrite True if the incoming method is tagged with Override
     * @param target target method being checked
     * @return true if the target was already merged and should be skipped
     */
    private boolean alreadyMerged(MixinTargetContext mixin, MethodNode method, boolean isOverwrite, MethodNode target) {
        AnnotationNode merged = ASMHelper.getVisibleAnnotation(target, MixinMerged.class);
        if (merged == null) {
            return false;
        }
    
        String sessionId = ASMHelper.<String>getAnnotationValue(merged, "sessionId");
        
        if (!this.sessionId.equals(sessionId)) {
            throw new ClassFormatError("Invalid @MixinMerged annotation found in" + mixin + " at " + method.name + " in " + this.targetClass.name);
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
     * @param mixin Mixin context
     * @param method Method being merged
     * @param isOverwrite True if the incoming method is tagged with Override
     * @param target target method being checked
     * @param intrinsic {@link Intrinsic} annotation
     * @return true if the intrinsic method was skipped (short-circuit further
     *      merge operations)
     */
    private boolean mergeIntrinsic(MixinTargetContext mixin, MethodNode method, boolean isOverwrite,
            MethodNode target, AnnotationNode intrinsic) {
        
        if (isOverwrite) {
            throw new InvalidMixinException(mixin, "@Intrinsic is not compatible with @Overwrite, remove one of these annotations on "
                    + method.name);
        }
        
        if (MixinApplicator.hasFlag(method, Opcodes.ACC_STATIC)) {
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
    private void displaceIntrinsic(MixinTargetContext mixin, MethodNode method, MethodNode target) {
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
     * @param method
     */
    private void appendInsns(MethodNode method) {
        if (Type.getReturnType(method.desc) != Type.VOID_TYPE) {
            throw new IllegalArgumentException("Attempted to merge insns from a method which does not return void");
        }
        
        MethodNode target = this.findTargetMethod(method);

        if (target != null) {
            AbstractInsnNode returnNode = MixinApplicator.findInsn(target, Opcodes.RETURN);
            
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
     * @param mixin
     */
    private void applyInitialisers(MixinTargetContext mixin) {
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
        for (MethodNode method : this.targetClass.methods) {
            if (Constants.INIT.equals(method.name)) {
                method.maxStack = Math.max(method.maxStack, ctor.maxStack);
                this.injectInitialiser(method, initialiser);
            }
        }
    }

    /**
     * Finds a suitable ctor for reading the instance initialiser bytecode
     * 
     * @param mixin mixin to search
     * @return
     */
    private MethodNode getConstructor(MixinTargetContext mixin) {
        MethodNode ctor = null;
        
        for (MethodNode mixinMethod : mixin.getMethods()) {
            if (Constants.INIT.equals(mixinMethod.name)) {
                if (MixinApplicator.hasLineNumbers(mixinMethod)) {
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
        boolean lineNumberIsValid = false;
        AbstractInsnNode endReturn = null;
        
        int line = 0, start = 0, end = 0, superIndex = -1;
        for (Iterator<AbstractInsnNode> iter = ctor.instructions.iterator(); iter.hasNext();) {
            AbstractInsnNode insn = iter.next();
            if (insn instanceof LineNumberNode) {
                line = ((LineNumberNode)insn).line;
                lineNumberIsValid = true;
            } else if (insn instanceof MethodInsnNode) {
                if (insn.getOpcode() == Opcodes.INVOKESPECIAL && Constants.INIT.equals(((MethodInsnNode)insn).name) && superIndex == -1) {
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
                    for (int ivalidOp : MixinApplicator.INITIALISER_OPCODE_BLACKLIST) {
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
                return;
            }
        }
        
        this.logger.warn("Failed to locate super-invoke whilst injecting initialiser, initialiser was not mixed in!");
    }

    /**
     * Process {@link Inject} annotations and inject callbacks to annotated
     * methods
     * @param mixin
     */
    private void applyInjections(MixinTargetContext mixin) {
        List<InjectionInfo> injected = new ArrayList<InjectionInfo>();
        
        for (MethodNode method : this.targetClass.methods) {
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
     * Merge annotations from the specified source ClassNode to the destination
     * ClassNode, replaces annotations of the equivalent type on the target with
     * annotations from the source. If the source node has no annotations then
     * no action will take place, if the target node has no annotations then a
     * new annotation list will be created. Annotations from the mixin package
     * are not merged. 
     * 
     * @param from ClassNode to merge annotations from
     * @param to ClassNode to merge annotations to
     */
    private void mergeAnnotations(ClassNode from, ClassNode to) {
        to.visibleAnnotations = this.mergeAnnotations(from.visibleAnnotations, to.visibleAnnotations, from.name);
        to.invisibleAnnotations = this.mergeAnnotations(from.invisibleAnnotations, to.invisibleAnnotations, from.name);
    }
        
    /**
     * Merge annotations from the specified source MethodNode to the destination
     * MethodNode, replaces annotations of the equivalent type on the target
     * with annotations from the source. If the source node has no annotations
     * then no action will take place, if the target node has no annotations
     * then a new annotation list will be created. Annotations from the mixin
     * package are not merged. 
     * 
     * @param from MethodNode to merge annotations from
     * @param to MethodNode to merge annotations to
     */
    private void mergeAnnotations(MethodNode from, MethodNode to) {
        to.visibleAnnotations = this.mergeAnnotations(from.visibleAnnotations, to.visibleAnnotations, from.name);
        to.invisibleAnnotations = this.mergeAnnotations(from.invisibleAnnotations, to.invisibleAnnotations, from.name);
    }
    
    /**
     * Merge annotations from the specified source FieldNode to the destination
     * FieldNode, replaces annotations of the equivalent type on the target with
     * annotations from the source. If the source node has no annotations then
     * no action will take place, if the target node has no annotations then a
     * new annotation list will be created. Annotations from the mixin package
     * are not merged. 
     * 
     * @param from FieldNode to merge annotations from
     * @param to FieldNode to merge annotations to
     */
    private void mergeAnnotations(FieldNode from, FieldNode to) {
        to.visibleAnnotations = this.mergeAnnotations(from.visibleAnnotations, to.visibleAnnotations, from.name);
        to.invisibleAnnotations = this.mergeAnnotations(from.invisibleAnnotations, to.invisibleAnnotations, from.name);
    }
    
    /**
     * Merge annotations from the source list to the target list. Returns the
     * target list or a new list if the target list was null.
     * 
     * @param from Annotations to merge
     * @param to Annotation list to merge into
     * @param name Name of the item being merged, for debugging purposes
     * @return The merged list (or a new list if the target list was null)
     */
    private List<AnnotationNode> mergeAnnotations(List<AnnotationNode> from, List<AnnotationNode> to, String name) {
        try {
            if (from == null) {
                return to;
            }
            
            if (to == null) {
                to = new ArrayList<AnnotationNode>();
            }
            
            for (AnnotationNode annotation : from) {
                if (annotation.desc.startsWith("L" + Constants.MIXIN_PACKAGE_REF)) {
                    continue;
                }
                
                for (Iterator<AnnotationNode> iter = to.iterator(); iter.hasNext();) {
                    if (iter.next().desc.equals(annotation.desc)) {
                        iter.remove();
                        break;
                    }
                }
                
                to.add(annotation);
            }
        } catch (Exception ex) {
            this.logger.warn("Exception encountered whilst merging annotations for {}", name);
        }
        
        return to;
    }

    /**
     * Find the first insn node with a matching opcode in the specified method
     * 
     * @param method method to search
     * @param opcode opcode to search for
     * @return found node or null if not found 
     */
    private static AbstractInsnNode findInsn(MethodNode method, int opcode) {
        Iterator<AbstractInsnNode> findReturnIter = method.instructions.iterator();
        while (findReturnIter.hasNext()) {
            AbstractInsnNode insn = findReturnIter.next();
            if (insn.getOpcode() == opcode) {
                return insn;
            }
        }
        return null;
    }

    /**
     * Returns true if the supplied method contains any line number information
     * 
     * @param method Method to scan
     * @return true if a line number node is located
     */
    private static boolean hasLineNumbers(MethodNode method) {
        for (Iterator<AbstractInsnNode> iter = method.instructions.iterator(); iter.hasNext();) {
            if (iter.next() instanceof LineNumberNode) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds a method in the target class
     * @param searchFor
     * 
     * @return Target method matching searchFor, or null if not found
     */
    private MethodNode findTargetMethod(MethodNode searchFor) {
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
    private FieldNode findTargetField(FieldNode searchFor) {
        for (FieldNode target : this.targetClass.fields) {
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
