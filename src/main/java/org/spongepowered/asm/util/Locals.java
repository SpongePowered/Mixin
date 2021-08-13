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
package org.spongepowered.asm.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo.FrameData;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Method;
import org.spongepowered.asm.util.asm.ASM;
import org.spongepowered.asm.util.asm.MixinVerifier;
import org.spongepowered.asm.util.throwables.LVTGeneratorError;

/**
 * Utility methods for working with local variables using ASM
 */
public final class Locals {

    /**
     * A local variable entry added by mixin itself, eg. by an injector
     */
    public static class SyntheticLocalVariableNode extends LocalVariableNode {

        public SyntheticLocalVariableNode(String name, String descriptor, String signature, LabelNode start, LabelNode end, int index) {
            super(name, descriptor, signature, start, end, index);
        }
    
    }

    /**
     * A local variable entry which is "dead" (has been removed from the known
     * frame) but is being retained in the computed frame as a ghost so that we
     * can decide whether we want to include it in the final frame or not.
     * 
     * <p>Zombie nodes are currently only persisted for 1 instruction, so in the
     * cases where there is a frame node followed directly by the candidate insn
     * the zombies will be returned as valid results, but otherwise culled.</p> 
     */
    static class ZombieLocalVariableNode extends LocalVariableNode {
        
        static final char CHOP = 'C';
        static final char TRIM = 'X';
        
        /**
         * Progenitor of this zombie, to allow "resurrection" (am I stretching
         * this metaphor too far?)
         */
        final LocalVariableNode ancestor;
        
        final char type;

        /**
         * Number of instructions the zombie has "lived" for, incremented by the
         * state machine loop 
         */
        int lifetime;
        
        /**
         * Number of frames which have elapsed for this zombie, incremented by
         * the state machine loop when a frame node is encountered
         */
        int frames;
        
        ZombieLocalVariableNode(LocalVariableNode ancestor, char type) {
            super(ancestor.name, ancestor.desc, ancestor.signature, ancestor.start, ancestor.end, ancestor.index);
            this.ancestor = ancestor;
            this.type = type;
        }
        
        boolean checkResurrect(Settings settings) {
            int insnThreshold = this.type == ZombieLocalVariableNode.CHOP ? settings.choppedInsnThreshold : settings.trimmedInsnThreshold;
            if (insnThreshold > -1 && this.lifetime > insnThreshold) {
                return false;
            }
            int frameThreshold = this.type == ZombieLocalVariableNode.CHOP ? settings.choppedFrameThreshold : settings.trimmedFrameThreshold;
            return frameThreshold == -1 || this.frames <= frameThreshold;
        }
        
        static ZombieLocalVariableNode of(LocalVariableNode ancestor, char type) {
            if (ancestor instanceof ZombieLocalVariableNode) {
                return (ZombieLocalVariableNode)ancestor;
            }
            return ancestor != null ? new ZombieLocalVariableNode(ancestor, type) : null;
        }
        
        @Override
        public String toString() {
            return String.format("Z(%s,%-2d)", this.type, this.lifetime);
        }
        
    }
    
    /**
     * Settings for <tt>getLocalsAt</tt> containing the tunable options for the
     * algorithm. This exists for two purposes: Firstly, wrapping tunables up in
     * a single object for convenience, but secondly providing some level of
     * forward compatibility for platforms that which to provide <em>backward
     * </em> compatibility to their own consumers. The {@link #flagsCustom}
     * field is* provided as a way of encoding arbitrary options that downstream
     * projects may wish to use to tune their own implementations. The {@link
     * #flags} field is reserved for mixin internal flags to be added at a later
     * date.
     */
    public static class Settings {
        
        /**
         * When an incoming frame contains TOP entries, these are nearly always
         * bogus. If we previously knew the local in that slot, resurrect it.
         * Only resurrects TRIM zombies.
         */
        public static int RESURRECT_FOR_BOGUS_TOP = 0x01;
        
        /**
         * When a LOAD grows the frame, resurrect any zombies in the exposed
         * portion of the frame, based on the thresholds configured.
         */
        public static int RESURRECT_EXPOSED_ON_LOAD = 0x02;
        
        /**
         * When a STORE grows the frame, resurrect any zombies in the exposed
         * portion of the frame, based on the thresholds configured.
         */
        public static int RESURRECT_EXPOSED_ON_STORE = 0x04;

        /**
         * Default flags
         */
        public static int DEFAULT_FLAGS = Settings.RESURRECT_FOR_BOGUS_TOP | Settings.RESURRECT_EXPOSED_ON_LOAD | Settings.RESURRECT_EXPOSED_ON_STORE;
        
        /**
         * Default settings. CHOP zombies can be resurrected for 1 frame, TRIM
         * zombies can be resurrected forever
         */
        public static Settings DEFAULT = new Settings(Settings.DEFAULT_FLAGS, 0, -1, 1, -1, -1);
        
        /**
         * Reserved flags for Mixin 
         */
        final int flags;
        
        /**
         * Platform-specific flags
         */
        final int flagsCustom;
        
        /**
         * Number of instructions that a CHOPped local is eligible for
         * resurrection, -1 to ignore, 0 for none
         */
        final int choppedInsnThreshold;
        
        /**
         * Number of frames that a CHOPped local is eligible for resurrection,
         * -1 to ignore, 0 for none
         */
        final int choppedFrameThreshold;
        
        /**
         * Number of instructions that a TRIMmed local is eligible for
         * resurrection, -1 to ignore, 0 for none
         */
        final int trimmedInsnThreshold;

        /**
         * Number of frames that a TRIMmed local is eligible for resurrection,
         * -1 to ignore, 0 for none
         */
        final int trimmedFrameThreshold;

        /**
         * @param flags Mixin flags
         * @param flagsCustom Platform-specific flags
         * @param insnThreshold Number of instructions that a local (regardless
         *      of death reason) is eligible for resurrection, -1 to ignore,
     *          0 for none
         * @param frameThreshold Number of frames that a local (regardless of
         *      death reason) is eligible for resurrection,-1 to ignore, 0 for
         *      none
         */
        public Settings(int flags, int flagsCustom, int insnThreshold, int frameThreshold) {
            this(flags, flagsCustom, insnThreshold, frameThreshold, insnThreshold, frameThreshold);
        }
        
        /**
         * @param flags Mixin flags
         * @param flagsCustom Platform-specific flags
         * @param choppedInsnThreshold Number of instructions that a CHOPped
         *      local is eligible for resurrection, -1 to ignore, 0 for none
         * @param choppedFrameThreshold Number of frames that a CHOPped local is
         *      eligible for resurrection,-1 to ignore, 0 for none
         * @param trimmedInsnThreshold Number of instructions that a TRIMmed#
         *      local is eligible for resurrection, -1 to ignore, 0 for none
         * @param trimmedFrameThreshold Number of frames that a TRIMmed local is
         *      eligible for resurrection, -1 to ignore, 0 for none
         */
        public Settings(int flags, int flagsCustom, int choppedInsnThreshold, int choppedFrameThreshold, int trimmedInsnThreshold,
                int trimmedFrameThreshold) {
            this.flags = flags;
            this.flagsCustom = flagsCustom;
            this.choppedInsnThreshold = choppedInsnThreshold;
            this.choppedFrameThreshold = choppedFrameThreshold;
            this.trimmedInsnThreshold = trimmedInsnThreshold;
            this.trimmedFrameThreshold = trimmedFrameThreshold;
        }
        
        boolean hasFlags(int flags) {
            return (this.flags & flags) == flags;
        }
        
        boolean hasCustomFlags(int flagsCustom) {
            return (this.flagsCustom & flagsCustom) == flagsCustom;
        }
        
    }

    /**
     * Frame type names just for the purposes of debug printing
     */
    private static final String[] FRAME_TYPES = { "TOP", "INTEGER", "FLOAT", "DOUBLE", "LONG", "NULL", "UNINITIALIZED_THIS" };
    
    /**
     * Cached local variable lists, to avoid having to recalculate them
     * (expensive) if multiple injectors are working with the same method
     */
    private static final Map<String, List<LocalVariableNode>> calculatedLocalVariables = new HashMap<String, List<LocalVariableNode>>();
    
    private Locals() {
        // utility class
    }

    /**
     * Injects appropriate LOAD opcodes into the supplied InsnList for each
     * entry in the supplied locals array starting at pos
     * 
     * @param locals Local types (can contain nulls for uninitialised, TOP, or
     *      RETURN values in locals)
     * @param insns Instruction List to inject into
     * @param pos Start position
     * @param limit maximum number of locals to consume
     */
    public static void loadLocals(Type[] locals, InsnList insns, int pos, int limit) {
        for (; pos < locals.length && limit > 0; pos++) {
            if (locals[pos] != null) {
                insns.add(new VarInsnNode(locals[pos].getOpcode(Opcodes.ILOAD), pos));
                limit--;
            }
        }
    }

    /**
     * <p>Attempts to identify available locals at an arbitrary point in the
     * bytecode specified by node.</p>
     * 
     * <p>This method builds an approximate view of the locals available at an
     * arbitrary point in the bytecode by examining the following features in
     * the bytecode:</p> 
     * <ul>
     *   <li>Any available stack map frames</li>
     *   <li>STORE opcodes</li>
     *   <li>The local variable table</li>
     * </ul>
     * 
     * <p>Inference proceeds by walking the bytecode from the start of the
     * method looking for stack frames and STORE opcodes. When either of these
     * is encountered, an attempt is made to cross-reference the values in the
     * stack map or STORE opcode with the value in the local variable table
     * which covers the code range. Stack map frames overwrite the entire
     * simulated local variable table with their own value types, STORE opcodes
     * overwrite only the local slot to which they pertain. Values in the
     * simulated locals array are spaced according to their size (unlike the
     * representation in FrameNode) and this TOP, NULL and UNINTITIALIZED_THIS
     * opcodes will be represented as null values in the simulated frame.</p>
     * 
     * <p>This code does not currently simulate the prescribed JVM behaviour
     * where overwriting the second slot of a DOUBLE or LONG actually
     * invalidates the DOUBLE or LONG stored in the previous location, so we
     * have to hope (for now) that this behaviour isn't emitted by the compiler
     * or any upstream transformers. I may have to re-think this strategy if
     * this situation is encountered in the wild.</p>
     * 
     * @param classNode ClassNode containing the method, used to initialise the
     *      implicit "this" reference in simple methods with no stack frames
     * @param method MethodNode to explore
     * @param node Node indicating the position at which to determine the locals
     *      state. The locals will be enumerated UP TO the specified node, so
     *      bear in mind that if the specified node is itself a STORE opcode,
     *      then we will be looking at the state of the locals PRIOR to its
     *      invocation
     * @return A sparse array containing a view (hopefully) of the locals at the
     *      specified location
     */
    public static LocalVariableNode[] getLocalsAt(ClassNode classNode, MethodNode method, AbstractInsnNode node) {
        return Locals.getLocalsAt(classNode, method, node, Settings.DEFAULT);
    }
    
    /**
     * <p>Attempts to identify available locals at an arbitrary point in the
     * bytecode specified by node.</p>
     * 
     * <p>This method builds an approximate view of the locals available at an
     * arbitrary point in the bytecode by examining the following features in
     * the bytecode:</p> 
     * <ul>
     *   <li>Any available stack map frames</li>
     *   <li>STORE opcodes</li>
     *   <li>The local variable table</li>
     * </ul>
     * 
     * <p>Inference proceeds by walking the bytecode from the start of the
     * method looking for stack frames and STORE opcodes. When either of these
     * is encountered, an attempt is made to cross-reference the values in the
     * stack map or STORE opcode with the value in the local variable table
     * which covers the code range. Stack map frames overwrite the entire
     * simulated local variable table with their own value types, STORE opcodes
     * overwrite only the local slot to which they pertain. Values in the
     * simulated locals array are spaced according to their size (unlike the
     * representation in FrameNode) and this TOP, NULL and UNINTITIALIZED_THIS
     * opcodes will be represented as null values in the simulated frame.</p>
     * 
     * <p>This code does not currently simulate the prescribed JVM behaviour
     * where overwriting the second slot of a DOUBLE or LONG actually
     * invalidates the DOUBLE or LONG stored in the previous location, so we
     * have to hope (for now) that this behaviour isn't emitted by the compiler
     * or any upstream transformers. I may have to re-think this strategy if
     * this situation is encountered in the wild.</p>
     * 
     * @param classNode ClassNode containing the method, used to initialise the
     *      implicit "this" reference in simple methods with no stack frames
     * @param method MethodNode to explore
     * @param node Node indicating the position at which to determine the locals
     *      state. The locals will be enumerated UP TO the specified node, so
     *      bear in mind that if the specified node is itself a STORE opcode,
     *      then we will be looking at the state of the locals PRIOR to its
     *      invocation
     * @param settings Tunable settings for the state machine
     * @return A sparse array containing a view (hopefully) of the locals at the
     *      specified location
     */
    public static LocalVariableNode[] getLocalsAt(ClassNode classNode, MethodNode method, AbstractInsnNode node, Settings settings) {
        for (int i = 0; i < 3 && (node instanceof LabelNode || node instanceof LineNumberNode); i++) {
            AbstractInsnNode nextNode = Locals.nextNode(method.instructions, node);
            if (nextNode instanceof FrameNode) { // Do not ffwd over frames
                break;
            }
            node = nextNode;
        }
        
        ClassInfo classInfo = ClassInfo.forName(classNode.name);
        if (classInfo == null) {
            throw new LVTGeneratorError("Could not load class metadata for " + classNode.name + " generating LVT for " + method.name);
        }
        Method methodInfo = classInfo.findMethod(method, method.access | ClassInfo.INCLUDE_INITIALISERS);
        if (methodInfo == null) {
            throw new LVTGeneratorError("Could not locate method metadata for " + method.name + " generating LVT in " + classNode.name);
        }
        List<FrameData> frames = methodInfo.getFrames();

        LocalVariableNode[] frame = new LocalVariableNode[method.maxLocals];
        int local = 0, index = 0;

        // Initialise implicit "this" reference in non-static methods
        if ((method.access & Opcodes.ACC_STATIC) == 0) {
            frame[local++] = new LocalVariableNode("this", Type.getObjectType(classNode.name).toString(), null, null, null, 0);
        }
        
        // Initialise method arguments
        for (Type argType : Type.getArgumentTypes(method.desc)) {
            frame[local] = new LocalVariableNode("arg" + index++, argType.toString(), null, null, null, local);
            local += argType.getSize();
        }
        
        final int initialFrameSize = local;
        int frameSize = local;
        int frameIndex = -1;
        int lastFrameSize = local;
        int knownFrameSize = local;
        VarInsnNode storeInsn = null;

        for (Iterator<AbstractInsnNode> iter = method.instructions.iterator(); iter.hasNext();) {
            AbstractInsnNode insn = iter.next();
            
            // Tick the zombies
            for (int l = 0; l < frame.length; l++) {
                if (frame[l] instanceof ZombieLocalVariableNode) {
                    ZombieLocalVariableNode zombie = (ZombieLocalVariableNode)frame[l];
                    zombie.lifetime++;
                    if (insn instanceof FrameNode) {
                        zombie.frames++;
                    }
                }
            }

            if (storeInsn != null) {
                LocalVariableNode storedLocal = Locals.getLocalVariableAt(classNode, method, insn, storeInsn.var);
                frame[storeInsn.var] = storedLocal;
                knownFrameSize = Math.max(knownFrameSize, storeInsn.var + 1);
                if (storedLocal != null && storeInsn.var < method.maxLocals - 1 && storedLocal.desc != null
                        && Type.getType(storedLocal.desc).getSize() == 2) {
                    frame[storeInsn.var + 1] = null; // TOP
                    knownFrameSize = Math.max(knownFrameSize, storeInsn.var + 2);
                    if (settings.hasFlags(Settings.RESURRECT_EXPOSED_ON_STORE)) {
                        Locals.resurrect(frame, knownFrameSize, settings);
                    }
                }
                storeInsn = null;
            }
            
            handleFrame: if (insn instanceof FrameNode) {
                frameIndex++;
                FrameNode frameNode = (FrameNode)insn;
                if (frameNode.type == Opcodes.F_SAME || frameNode.type == Opcodes.F_SAME1) {
                    break handleFrame;
                }
                
                int frameNodeSize = Locals.computeFrameSize(frameNode, initialFrameSize);
                FrameData frameData = frameIndex < frames.size() ? frames.get(frameIndex) : null;

                if (frameData != null) {
                    if (frameData.type == Opcodes.F_FULL) {
                        knownFrameSize = lastFrameSize = frameSize = Math.max(initialFrameSize, Math.min(frameNodeSize, frameData.size));
                    } else {
                        frameSize = Locals.getAdjustedFrameSize(frameSize, frameData, initialFrameSize);
                    }
                } else {
                    frameSize = Locals.getAdjustedFrameSize(frameSize, frameNode, initialFrameSize);
                }
                
                // Sanity check
                if (frameSize < initialFrameSize) {
                    throw new IllegalStateException(String.format("Locals entered an invalid state evaluating %s::%s%s at instruction %d (%s). "
                            + "Initial frame size is %d, calculated a frame size of %d with %s", classNode.name, method.name, method.desc,
                            method.instructions.indexOf(insn), Bytecode.describeNode(insn, false), initialFrameSize, frameSize, frameData));
                }
                
                if ((frameData == null && (frameNode.type == Opcodes.F_CHOP || frameNode.type == Opcodes.F_NEW))
                        || (frameData != null && frameData.type == Opcodes.F_CHOP)) {
                    for (int framePos = frameSize; framePos < frame.length; framePos++) {
                        frame[framePos] = ZombieLocalVariableNode.of(frame[framePos], ZombieLocalVariableNode.CHOP);
                    }
                    knownFrameSize = lastFrameSize = frameSize;
                    break handleFrame;
                }

                int framePos = frameNode.type == Opcodes.F_APPEND ? lastFrameSize : 0;
                lastFrameSize = frameSize;
                
                // localPos tracks the location in the frame node's locals list, which doesn't leave space for TOP entries
                for (int localPos = 0; framePos < frame.length; framePos++, localPos++) {
                    // Get the local at the current position in the FrameNode's locals list
                    final Object localType = (localPos < frameNode.local.size()) ? frameNode.local.get(localPos) : null;

                    if (localType instanceof String) { // String refers to a reference type
                        frame[framePos] = Locals.getLocalVariableAt(classNode, method, insn, framePos);
                    } else if (localType instanceof Integer) { // Integer refers to a primitive type or other marker
                        boolean isMarkerType = localType == Opcodes.UNINITIALIZED_THIS || localType == Opcodes.NULL;
                        boolean is32bitValue = localType == Opcodes.INTEGER || localType == Opcodes.FLOAT;
                        boolean is64bitValue = localType == Opcodes.DOUBLE || localType == Opcodes.LONG;
                        if (localType == Opcodes.TOP) {
                            // Explicit TOP entries are pretty much always bogus, but depending on our resurrection
                            // strategy we may want to resurrect eligible zombies here. Real TOP entries are handled below
                            if (frame[framePos] instanceof ZombieLocalVariableNode && settings.hasFlags(Settings.RESURRECT_FOR_BOGUS_TOP)) {
                                ZombieLocalVariableNode zombie = (ZombieLocalVariableNode)frame[framePos];
                                if (zombie.type == ZombieLocalVariableNode.TRIM) {
                                    frame[framePos] = zombie.ancestor;
                                }
                            }
                        } else if (isMarkerType) {
                            frame[framePos] = null;
                        } else if (is32bitValue || is64bitValue) {
                            frame[framePos] = Locals.getLocalVariableAt(classNode, method, insn, framePos);

                            if (is64bitValue) {
                                framePos++;
                                frame[framePos] = null; // TOP
                            }
                        } else {
                            throw new LVTGeneratorError("Unrecognised locals opcode " + localType + " in locals array at position " + localPos
                                    + " in " + classNode.name + "." + method.name + method.desc);
                        }
                    } else if (localType == null) {
                        if (framePos >= initialFrameSize && framePos >= frameSize && frameSize > 0) {
                            if (framePos < knownFrameSize) {
                                frame[framePos] = Locals.getLocalVariableAt(classNode, method, insn, framePos);
                            } else {
                                frame[framePos] = ZombieLocalVariableNode.of(frame[framePos], ZombieLocalVariableNode.TRIM);
                            }
                        }
                    } else if (localType instanceof LabelNode) {
                        // Uninitialised
                    } else {
                        throw new LVTGeneratorError("Invalid value " + localType + " in locals array at position " + localPos
                                + " in " + classNode.name + "." + method.name + method.desc);
                    }
                }
            } else if (insn instanceof VarInsnNode) {
                VarInsnNode varInsn = (VarInsnNode)insn;
                boolean isLoad = insn.getOpcode() >= Opcodes.ILOAD && insn.getOpcode() <= Opcodes.SALOAD;
                if (isLoad) {
                    frame[varInsn.var] = Locals.getLocalVariableAt(classNode, method, insn, varInsn.var);
                    int varSize = frame[varInsn.var].desc != null ? Type.getType(frame[varInsn.var].desc).getSize() : 1;
                    knownFrameSize = Math.max(knownFrameSize, varInsn.var + varSize);
                    if (settings.hasFlags(Settings.RESURRECT_EXPOSED_ON_LOAD)) {
                        Locals.resurrect(frame, knownFrameSize, settings);
                    }
                } else {
                    // Update the LVT for the opcode AFTER this one, since we always want to know
                    // the frame state BEFORE the *current* instruction to match the contract of
                    // injection points
                    storeInsn = varInsn;
                }
            }
            
            if (insn == node) {
                break;
            }
        }

        // Null out any "unknown" or mixin-provided locals
        for (int l = 0; l < frame.length; l++) {
            if (frame[l] instanceof ZombieLocalVariableNode) {
                ZombieLocalVariableNode zombie = (ZombieLocalVariableNode)frame[l];
                // preserve zombies where the frame node which culled them was immediately prior to
                // the matched instruction, or *was itself* the matched instruction, the returned
                // frame will contain the original node (the zombie ancestor)
                frame[l] = (zombie.lifetime > 1) ? null : zombie.ancestor;
            }
            
            if ((frame[l] != null && frame[l].desc == null) || frame[l] instanceof SyntheticLocalVariableNode) {
                frame[l] = null;
            }
        }

        return frame;
    }

    /**
     * Walks the supplied <tt>frame</tt> up to the specified <tt>knownFrameSize
     * </tt> and resurrects any zombies that meet the required criteria
     * 
     * @param frame Frame to walk
     * @param knownFrameSize Known frame size in which to resurrect
     * @param settings Resurrection settings
     */
    private static void resurrect(LocalVariableNode[] frame, int knownFrameSize, Settings settings) {
        for (int l = 0; l < knownFrameSize && l < frame.length; l++) {
            if (frame[l] instanceof ZombieLocalVariableNode) {
                ZombieLocalVariableNode zombie = (ZombieLocalVariableNode)frame[l];
                if (zombie.checkResurrect(settings)) {
                    frame[l] = zombie.ancestor;
                }
            }
        }
    }

   /**
     * Attempts to locate the appropriate entry in the local variable table for
     * the specified local variable index at the location specified by node.
     * 
     * @param classNode Containing class
     * @param method Method
     * @param node Instruction defining the location to get the local variable
     *      table at
     * @param var Local variable index
     * @return a LocalVariableNode containing information about the local
     *      variable at the specified location in the specified local slot
     */
    public static LocalVariableNode getLocalVariableAt(ClassNode classNode, MethodNode method, AbstractInsnNode node, int var) {
        return Locals.getLocalVariableAt(classNode, method, method.instructions.indexOf(node), var);
    }

    /**
     * Attempts to locate the appropriate entry in the local variable table for
     * the specified local variable index at the location specified by pos.
     * 
     * @param classNode Containing class
     * @param method Method
     * @param var Local variable index
     * @param pos The opcode index to get the local variable table at
     * @return a LocalVariableNode containing information about the local
     *      variable at the specified location in the specified local slot
     */
    private static LocalVariableNode getLocalVariableAt(ClassNode classNode, MethodNode method, int pos, int var) {
        LocalVariableNode localVariableNode = null;
        LocalVariableNode fallbackNode = null;

        for (LocalVariableNode local : Locals.getLocalVariableTable(classNode, method)) {
            if (local.index != var) {
                continue;
            }
            if (Locals.isOpcodeInRange(method.instructions, local, pos)) {
                localVariableNode = local;
            } else if (localVariableNode == null) {
                fallbackNode = local;
            }
        }
        
        if (localVariableNode == null && !method.localVariables.isEmpty()) {
            for (LocalVariableNode local : Locals.getGeneratedLocalVariableTable(classNode, method)) {
                if (local.index == var && Locals.isOpcodeInRange(method.instructions, local, pos)) {
                    localVariableNode = local;
                }
            }
        }
        
        return localVariableNode != null ? localVariableNode : fallbackNode;
    }

    private static boolean isOpcodeInRange(InsnList insns, LocalVariableNode local, int pos) {
        return insns.indexOf(local.start) <= pos && insns.indexOf(local.end) > pos;
    }

    /**
     * Fetches or generates the local variable table for the specified method.
     * Since Mojang strip the local variable table as part of the obfuscation
     * process, we need to generate the local variable table when running
     * obfuscated. We cache the generated tables so that we only need to do the
     * relatively expensive calculation once per method we encounter.
     * 
     * @param classNode Containing class
     * @param method Method
     * @return local variable table 
     */
    public static List<LocalVariableNode> getLocalVariableTable(ClassNode classNode, MethodNode method) {
        if (method.localVariables.isEmpty()) {
            return Locals.getGeneratedLocalVariableTable(classNode, method);
        }
        return Collections.<LocalVariableNode>unmodifiableList(method.localVariables);
    }
    
    /**
     * Gets the generated the local variable table for the specified method.
     * 
     * @param classNode Containing class
     * @param method Method
     * @return generated local variable table 
     */
    public static List<LocalVariableNode> getGeneratedLocalVariableTable(ClassNode classNode, MethodNode method) {
        String methodId = String.format("%s.%s%s", classNode.name, method.name, method.desc);
        List<LocalVariableNode> localVars = Locals.calculatedLocalVariables.get(methodId);
        if (localVars != null) {
            return localVars;
        }

        localVars = Locals.generateLocalVariableTable(classNode, method);
        Locals.calculatedLocalVariables.put(methodId, localVars);
        return Collections.<LocalVariableNode>unmodifiableList(localVars);
    }

    /**
     * Use ASM Analyzer to generate the local variable table for the specified
     * method
     * 
     * @param classNode Containing class
     * @param method Method
     * @return generated local variable table
     */
    public static List<LocalVariableNode> generateLocalVariableTable(ClassNode classNode, MethodNode method) {
        List<Type> interfaces = null;
        if (classNode.interfaces != null) {
            interfaces = new ArrayList<Type>();
            for (String iface : classNode.interfaces) {
                interfaces.add(Type.getObjectType(iface));
            }
        }

        Type objectType = null;
        if (classNode.superName != null) {
            objectType = Type.getObjectType(classNode.superName);
        }

        // Use Analyzer to generate the bytecode frames
        Analyzer<BasicValue> analyzer = new Analyzer<BasicValue>(
                new MixinVerifier(ASM.API_VERSION, Type.getObjectType(classNode.name), objectType, interfaces, false));
        try {
            analyzer.analyze(classNode.name, method);
        } catch (AnalyzerException ex) {
            ex.printStackTrace();
        }

        // Get frames from the Analyzer
        Frame<BasicValue>[] frames = analyzer.getFrames();

        // Record the original size of hte method
        int methodSize = method.instructions.size();

        // List of LocalVariableNodes to return
        List<LocalVariableNode> localVariables = new ArrayList<LocalVariableNode>();

        LocalVariableNode[] localNodes = new LocalVariableNode[method.maxLocals]; // LocalVariableNodes for current frame
        BasicValue[] locals = new BasicValue[method.maxLocals]; // locals in previous frame, used to work out what changes between frames
        LabelNode[] labels = new LabelNode[methodSize]; // Labels to add to the method, for the markers
        String[] lastKnownType = new String[method.maxLocals];

        // Traverse the frames and work out when locals begin and end
        for (int i = 0; i < methodSize; i++) {
            Frame<BasicValue> f = frames[i];
            if (f == null) {
                continue;
            }
            LabelNode label = null;

            for (int j = 0; j < f.getLocals(); j++) {
                BasicValue local = f.getLocal(j);
                if (local == null && locals[j] == null) {
                    continue;
                }
                if (local != null && local.equals(locals[j])) {
                    continue;
                }

                if (label == null) {
                    AbstractInsnNode existingLabel = method.instructions.get(i);
                    if (existingLabel instanceof LabelNode) {
                        label = (LabelNode) existingLabel;
                    } else {
                        labels[i] = label = new LabelNode();
                    }
                }
                
                if (local == null && locals[j] != null) {
                    localVariables.add(localNodes[j]);
                    localNodes[j].end = label;
                    localNodes[j] = null;
                } else if (local != null) {
                    if (locals[j] != null) {
                        localVariables.add(localNodes[j]);
                        localNodes[j].end = label;
                        localNodes[j] = null;
                    }

                    String desc = lastKnownType[j];
                    Type localType = local.getType();
                    if (localType != null) {
                        desc = localType.getSort() >= Type.ARRAY && "null".equals(localType.getInternalName())
                                ? Constants.OBJECT_DESC : localType.getDescriptor();
                    }
                    
                    localNodes[j] = new LocalVariableNode("var" + j, desc, null, label, null, j);
                    if (desc != null) {
                        lastKnownType[j] = desc;
                    }
                }

                locals[j] = local;
            }
        }

        // Reached the end of the method so flush all current locals and mark the end
        LabelNode label = null;
        for (int k = 0; k < localNodes.length; k++) {
            if (localNodes[k] != null) {
                if (label == null) {
                    label = new LabelNode();
                    method.instructions.add(label);
                }

                localNodes[k].end = label;
                localVariables.add(localNodes[k]);
            }
        }

        // Insert generated labels into the method body
        for (int n = methodSize - 1; n >= 0; n--) {
            if (labels[n] != null) {
                method.instructions.insert(method.instructions.get(n), labels[n]);
            }
        }

        return localVariables;
    }
    
    /**
     * Get the insn immediately following the specified insn, or return the same
     * insn if the insn is the last insn in the list
     * 
     * @param insns Insn list to fetch from
     * @param insn Insn node
     * @return Next insn or the same insn if last in the list
     */
    private static AbstractInsnNode nextNode(InsnList insns, AbstractInsnNode insn) {
        int index = insns.indexOf(insn) + 1;
        if (index > 0 && index < insns.size()) {
            return insns.get(index);
        }
        return insn;
    }
    
    /**
     * Compute a new frame size based on the supplied frame type and the size of
     * locals contained in the frame (this may differ from the number of actual
     * frame slots if the frame contains doubles or longs)
     * 
     * @param currentSize current frame size
     * @param frameNode frame entry
     * @param initialFrameSize Method initial frame size
     * @return new frame size
     */
    private static int getAdjustedFrameSize(int currentSize, FrameNode frameNode, int initialFrameSize) {
        return Locals.getAdjustedFrameSize(currentSize, frameNode.type, Locals.computeFrameSize(frameNode, initialFrameSize), initialFrameSize);
    }

    /**
     * Compute a new frame size based on the supplied frame type and the size of
     * locals contained in the frame (this may differ from the number of actual
     * frame slots if the frame contains doubles or longs)
     * 
     * @param currentSize current frame size
     * @param frameData frame entry
     * @param initialFrameSize Method initial frame size
     * @return new frame size
     */
    private static int getAdjustedFrameSize(int currentSize, FrameData frameData, int initialFrameSize) {
        return Locals.getAdjustedFrameSize(currentSize, frameData.type, frameData.size, initialFrameSize);
    }
    
    /**
     * Compute a new frame size based on the supplied frame type and the size of
     * locals contained in the frame (this may differ from the number of actual
     * frame slots if the frame contains doubles or longs)
     * 
     * @param currentSize current frame size
     * @param type frame entry type
     * @param size frame entry size
     * @param initialFrameSize Method initial frame size
     * @return new frame size
     */
    private static int getAdjustedFrameSize(int currentSize, int type, int size, int initialFrameSize) {
        switch (type) {
            case Opcodes.F_NEW:
            case Opcodes.F_FULL:
                return Math.max(initialFrameSize, size);
            case Opcodes.F_APPEND:
                return currentSize + size;
            case Opcodes.F_CHOP:
                return Math.max(initialFrameSize, currentSize - size);
            case Opcodes.F_SAME:
            case Opcodes.F_SAME1:
                return currentSize;
            default:
                return currentSize;
        }
     }
    
    /**
     * Compute the size required to accomodate the entries described by the
     * supplied frame node
     * 
     * @param frameNode frame node with locals to compute
     * @param initialFrameSize Method initial frame size
     * @return size of frame node locals
     */
    public static int computeFrameSize(FrameNode frameNode, int initialFrameSize) {
        if (frameNode.local == null) {
            return initialFrameSize;
        }
        int size = 0;
        for (Object local : frameNode.local) {
            if (local instanceof Integer) {
                size += (local == Opcodes.DOUBLE || local == Opcodes.LONG) ? 2 : 1;
            } else {
                size++;
            }
        }
        return Math.max(initialFrameSize, size);
    }
    
    /**
     * Debug function to return printable name of a frame entry
     * 
     * @param frameEntry Frame entry
     * @return string representation of the supplied frame entry
     */
    public static String getFrameTypeName(Object frameEntry) {
        if (frameEntry == null) {
            return "NULL";
        }
        
        if (frameEntry instanceof String) {
            return Bytecode.getSimpleName(frameEntry.toString());
        }
        
        if (frameEntry instanceof Integer) {
            int type = ((Integer)frameEntry).intValue();
            
            if (type >= Locals.FRAME_TYPES.length) {
                return "INVALID";
            }
            
            return Locals.FRAME_TYPES[type];
        }
        
        return "?";
    }
    
}
