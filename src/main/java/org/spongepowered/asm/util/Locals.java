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
        for (int i = 0; i < 3 && (node instanceof LabelNode || node instanceof LineNumberNode); i++) {
            node = Locals.nextNode(method.instructions, node);
        }
            
        ClassInfo classInfo = ClassInfo.forName(classNode.name);
        if (classInfo == null) {
            throw new LVTGeneratorError("Could not load class metadata for " + classNode.name + " generating LVT for " + method.name);
        }
        Method methodInfo = classInfo.findMethod(method);
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
        
        int initialFrameSize = local;
        int frameSize = local;
        int frameIndex = -1;
        int lastFrameSize = local;
        VarInsnNode storeInsn = null;

        for (Iterator<AbstractInsnNode> iter = method.instructions.iterator(); iter.hasNext();) {
            AbstractInsnNode insn = iter.next();
            if (storeInsn != null) {
                frame[storeInsn.var] = Locals.getLocalVariableAt(classNode, method, insn, storeInsn.var);
                storeInsn = null;
            }
            
            handleFrame: if (insn instanceof FrameNode) {
                frameIndex++;
                FrameNode frameNode = (FrameNode)insn;
                if (frameNode.type == Opcodes.F_SAME || frameNode.type == Opcodes.F_SAME1) {
                    break handleFrame;
                }
                
                FrameData frameData = frameIndex < frames.size() ? frames.get(frameIndex) : null;

                if (frameData != null) {
                    if (frameData.type == Opcodes.F_FULL) {
                        frameSize = Math.min(frameSize, frameData.locals);
                        lastFrameSize = frameSize;
                    } else {
                        frameSize = Locals.getAdjustedFrameSize(frameSize, frameData);
                    }
                } else {
                    frameSize = Locals.getAdjustedFrameSize(frameSize, frameNode);
                }
                
                if (frameNode.type == Opcodes.F_CHOP) {
                    for (int framePos = frameSize; framePos < frame.length; framePos++) {
                        frame[framePos] = null; 
                    }
                    lastFrameSize = frameSize;
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
                            // Do nothing, explicit TOP entries are pretty much always bogus, and real ones are handled below
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
                            frame[framePos] = null;
                        }
                    } else if (localType instanceof LabelNode) {
                        // Uninitialised
                    } else {
                        throw new LVTGeneratorError("Invalid value " + localType + " in locals array at position " + localPos
                                + " in " + classNode.name + "." + method.name + method.desc);
                    }
                }
            } else if (insn instanceof VarInsnNode) {
                VarInsnNode varNode = (VarInsnNode) insn;
                boolean isLoad = insn.getOpcode() >= Opcodes.ILOAD && insn.getOpcode() <= Opcodes.SALOAD;
                if (isLoad) {
                    frame[varNode.var] = Locals.getLocalVariableAt(classNode, method, insn, varNode.var);
                } else {
                    // Update the LVT for the opcode AFTER this one, since we always want to know
                    // the frame state BEFORE the *current* instruction to match the contract of
                    // injection points
                    storeInsn = varNode;
                }
            }
            
            if (insn == node) {
                break;
            }
        }
        
        // Null out any "unknown" locals
        for (int l = 0; l < frame.length; l++) {
            if (frame[l] != null && frame[l].desc == null) {
                frame[l] = null;
            }
        }

        return frame;
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
        return method.localVariables;
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
        return localVars;
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

                    String desc = (local.getType() != null) ? local.getType().getDescriptor() : lastKnownType[j];
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
     * @return new frame size
     */
    private static int getAdjustedFrameSize(int currentSize, FrameNode frameNode) {
        return Locals.getAdjustedFrameSize(currentSize, frameNode.type, Locals.computeFrameSize(frameNode));
    }

    /**
     * Compute a new frame size based on the supplied frame type and the size of
     * locals contained in the frame (this may differ from the number of actual
     * frame slots if the frame contains doubles or longs)
     * 
     * @param currentSize current frame size
     * @param frameData frame entry
     * @return new frame size
     */
    private static int getAdjustedFrameSize(int currentSize, FrameData frameData) {
        return Locals.getAdjustedFrameSize(currentSize, frameData.type, frameData.size);
    }
    
    /**
     * Compute a new frame size based on the supplied frame type and the size of
     * locals contained in the frame (this may differ from the number of actual
     * frame slots if the frame contains doubles or longs)
     * 
     * @param currentSize current frame size
     * @param type frame entry type
     * @param size frame entry size
     * @return new frame size
     */
    private static int getAdjustedFrameSize(int currentSize, int type, int size) {
        switch (type) {
            case Opcodes.F_NEW:
            case Opcodes.F_FULL:
                return size;
            case Opcodes.F_APPEND:
                return currentSize + size;
            case Opcodes.F_CHOP:
                return currentSize - size;
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
     * @return size of frame node locals
     */
    public static int computeFrameSize(FrameNode frameNode) {
        if (frameNode.local == null) {
            return 0;
        }
        int size = 0;
        for (Object local : frameNode.local) {
            if (local instanceof Integer) {
                size += (local == Opcodes.DOUBLE || local == Opcodes.LONG) ? 2 : 1;
            } else {
                size++;
            }
        }
        return size;
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
