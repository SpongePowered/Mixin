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

import static org.spongepowered.asm.lib.ClassWriter.*;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.ClassWriter;
import org.spongepowered.asm.lib.MethodVisitor;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.*;
import org.spongepowered.asm.lib.util.CheckClassAdapter;
import org.spongepowered.asm.lib.util.TraceClassVisitor;

import com.google.common.primitives.Ints;

/**
 * Utility methods for working with bytecode via ASM
 */
public final class Bytecode {
    
    /**
     * Ordinal member visibility level. This is used to represent visibility of
     * a member in a formal way from lowest to highest. The
     * {@link Bytecode#getVisibility} methods can be used to convert access
     * flags to this enum. The value returned from {@link #ordinal} can then be
     * used to determine whether a visibility level is <i>higher</i> or <i>lower
     * </i> than any other given visibility level.  
     */
    public enum Visibility {
        
        /**
         * Members decorated with {@link Opcodes#ACC_PRIVATE} 
         */
        PRIVATE,
        
        /**
         * Members decorated with {@link Opcodes#ACC_PROTECTED} 
         */
        PROTECTED,
        
        /**
         * Members not decorated with any access flags
         */
        PACKAGE,
        
        /**
         * Members decorated with {@link Opcodes#ACC_PUBLIC} 
         */
        PUBLIC
        
    }
    
    /**
     * Integer constant opcodes
     */
    public static final int[] CONSTANTS_INT = {
        Opcodes.ICONST_M1, Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5
    };

    /**
     * Float constant opcodes 
     */
    public static final int[] CONSTANTS_FLOAT = {
        Opcodes.FCONST_0, Opcodes.FCONST_1, Opcodes.FCONST_2
    };
    
    /**
     * Double constant opcodes
     */
    public static final int[] CONSTANTS_DOUBLE = {
        Opcodes.DCONST_0, Opcodes.DCONST_1
    };
    
    /**
     * Long constant opcodes
     */
    public static final int[] CONSTANTS_LONG = {
        Opcodes.LCONST_0, Opcodes.LCONST_1
    };
    
    /**
     * All constant opcodes 
     */
    public static final int[] CONSTANTS_ALL = {
        Opcodes.ACONST_NULL,
        Opcodes.ICONST_M1,
        Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5,
        Opcodes.LCONST_0, Opcodes.LCONST_1,
        Opcodes.FCONST_0, Opcodes.FCONST_1, Opcodes.FCONST_2, 
        Opcodes.DCONST_0, Opcodes.DCONST_1,
        Opcodes.BIPUSH, // 15
        Opcodes.SIPUSH, // 16
        Opcodes.LDC,    // 17
    };
    
    private static final Object[] CONSTANTS_VALUES = {
        null,
        Integer.valueOf(-1),
        Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3), Integer.valueOf(4), Integer.valueOf(5),
        Long.valueOf(0L), Long.valueOf(1L),
        Float.valueOf(0.0F), Float.valueOf(1.0F), Float.valueOf(2.0F), 
        Double.valueOf(0.0), Double.valueOf(1.0)
    };
    
    private static final String[] CONSTANTS_TYPES = {
        null,
        "I",
        "I", "I", "I", "I", "I", "I",
        "J", "J",
        "F", "F", "F", 
        "D", "D",
        "I", //"B",
        "I", //"S"
    };
    
    private Bytecode() {
        // utility class
    }

    /**
     * Finds a method given the method descriptor
     *
     * @param classNode the class to scan
     * @param name the method name
     * @param desc the method descriptor
     * @return discovered method node or null
     */
    public static MethodNode findMethod(ClassNode classNode, String name, String desc) {
        for (MethodNode method : classNode.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Runs textifier on the specified class node and dumps the output to the
     * specified output stream
     * 
     * @param classNode class to textify
     * @param out output stream
     */
    public static void textify(ClassNode classNode, OutputStream out) {
        classNode.accept(new TraceClassVisitor(new PrintWriter(out)));
    }

    /**
     * Runs textifier on the specified method node and dumps the output to the
     * specified output stream
     * 
     * @param methodNode method to textify
     * @param out output stream
     */
    public static void textify(MethodNode methodNode, OutputStream out) {
        TraceClassVisitor trace = new TraceClassVisitor(new PrintWriter(out));
        MethodVisitor mv = trace.visitMethod(methodNode.access, methodNode.name, methodNode.desc, methodNode.signature,
                methodNode.exceptions.toArray(new String[0]));
        methodNode.accept(mv);
        trace.visitEnd();
    }

    /**
     * Dumps the output of CheckClassAdapter.verify to System.out
     *
     * @param classNode the classNode to verify
     */
    public static void dumpClass(ClassNode classNode) {
        ClassWriter cw = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
        classNode.accept(cw);
        Bytecode.dumpClass(cw.toByteArray());
    }

    /**
     * Dumps the output of CheckClassAdapter.verify to System.out
     *
     * @param bytes the bytecode of the class to check
     */
    public static void dumpClass(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        CheckClassAdapter.verify(cr, true, new PrintWriter(System.out));
    }
    
    /**
     * Prints a representation of a method's instructions to stderr
     * 
     * @param method Method to print
     */
    public static void printMethodWithOpcodeIndices(MethodNode method) {
        System.err.printf("%s%s\n", method.name, method.desc);
        int i = 0;
        for (Iterator<AbstractInsnNode> iter = method.instructions.iterator(); iter.hasNext();) {
            System.err.printf("[%4d] %s\n", i++, Bytecode.describeNode(iter.next()));
        }
    }

    /**
     * Prints a representation of a method's instructions to stderr
     * 
     * @param method Method to print
     */
    public static void printMethod(MethodNode method) {
        System.err.printf("%s%s\n", method.name, method.desc);
        for (Iterator<AbstractInsnNode> iter = method.instructions.iterator(); iter.hasNext();) {
            System.err.print("  ");
            Bytecode.printNode(iter.next());
        }
    }

    /**
     * Prints a representation of the specified insn node to stderr
     * 
     * @param node Node to print
     */
    public static void printNode(AbstractInsnNode node) {
        System.err.printf("%s\n", Bytecode.describeNode(node));
    }

    /**
     * Gets a description of the supplied node for debugging purposes
     * 
     * @param node node to describe
     * @return human-readable description of node
     */
    public static String describeNode(AbstractInsnNode node) {
        if (node == null) {
            return String.format("   %-14s ", "null");
        }
        
        if (node instanceof LabelNode) {
            return String.format("[%s]", ((LabelNode)node).getLabel());
        }
        
        String out = String.format("   %-14s ", node.getClass().getSimpleName().replace("Node", ""));
        if (node instanceof JumpInsnNode) {
            out += String.format("[%s] [%s]", Bytecode.getOpcodeName(node), ((JumpInsnNode)node).label.getLabel());
        } else if (node instanceof VarInsnNode) {
            out += String.format("[%s] %d", Bytecode.getOpcodeName(node), ((VarInsnNode)node).var);
        } else if (node instanceof MethodInsnNode) {
            MethodInsnNode mth = (MethodInsnNode)node;
            out += String.format("[%s] %s %s %s", Bytecode.getOpcodeName(node), mth.owner, mth.name, mth.desc);
        } else if (node instanceof FieldInsnNode) {
            FieldInsnNode fld = (FieldInsnNode)node;
            out += String.format("[%s] %s %s %s", Bytecode.getOpcodeName(node), fld.owner, fld.name, fld.desc);
        } else if (node instanceof LineNumberNode) {
            LineNumberNode ln = (LineNumberNode)node;
            out += String.format("LINE=[%d] LABEL=[%s]", ln.line, ln.start.getLabel());
        } else if (node instanceof LdcInsnNode) {
            out += (((LdcInsnNode)node).cst);
        } else if (node instanceof IntInsnNode) {
            out += (((IntInsnNode)node).operand);
        } else if (node instanceof FrameNode) {
            out += String.format("[%s] ", Bytecode.getOpcodeName(((FrameNode)node).type, "H_INVOKEINTERFACE", -1));
        } else {
            out += String.format("[%s] ", Bytecode.getOpcodeName(node));
        }
        return out;
    }
    
    /**
     * Uses reflection to find an approximate constant name match for the
     * supplied node's opcode
     * 
     * @param node Node to query for opcode
     * @return Approximate opcode name (approximate because some constants in
     *      the {@link Opcodes} class have the same value as opcodes
     */
    public static String getOpcodeName(AbstractInsnNode node) {
        return Bytecode.getOpcodeName(node.getOpcode());
    }

    /**
     * Uses reflection to find an approximate constant name match for the
     * supplied opcode
     * 
     * @param opcode Opcode to look up
     * @return Approximate opcode name (approximate because some constants in
     *      the {@link Opcodes} class have the same value as opcodes
     */
    public static String getOpcodeName(int opcode) {
        return Bytecode.getOpcodeName(opcode, "UNINITIALIZED_THIS", 1);
    }

    private static String getOpcodeName(int opcode, String start, int min) {
        if (opcode >= min) {
            boolean found = false;
            
            try {
                for (java.lang.reflect.Field f : Opcodes.class.getDeclaredFields()) {
                    if (!found && !f.getName().equals(start)) {
                        continue;
                    }
                    found = true;
                    if (f.getType() == Integer.TYPE && f.getInt(null) == opcode) {
                        return f.getName();
                    }
                }
            } catch (Exception ex) {
                // derp
            }
        }        
        
        return opcode >= 0 ? String.valueOf(opcode) : "UNKNOWN";
    }
    
    /**
     * Returns true if the supplied method node is static
     * 
     * @param method method node
     * @return true if the method has the {@link Opcodes#ACC_STATIC} flag
     */
    public static boolean methodIsStatic(MethodNode method) {
        return (method.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
    }
    
    /**
     * Returns true if the supplied field node is static
     * 
     * @param field field node
     * @return true if the field has the {@link Opcodes#ACC_STATIC} flag
     */
    public static boolean fieldIsStatic(FieldNode field) {
        return (field.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
    }
    
    /**
     * Get the first variable index in the supplied method which is not an
     * argument or "this" reference, this corresponds to the size of the
     * arguments passed in to the method plus an extra spot for "this" if the
     * method is non-static
     * 
     * @param method MethodNode to inspect
     * @return first available local index which is NOT used by a method
     *      argument or "this"
     */
    public static int getFirstNonArgLocalIndex(MethodNode method) {
        return Bytecode.getFirstNonArgLocalIndex(Type.getArgumentTypes(method.desc), (method.access & Opcodes.ACC_STATIC) == 0);
    }

    /**
     * Get the first non-arg variable index based on the supplied arg array and
     * whether to include the "this" reference, this corresponds to the size of
     * the arguments passed in to the method plus an extra spot for "this" is
     * specified
     * 
     * @param args Method arguments
     * @param includeThis Whether to include a slot for "this" (generally true
     *      for all non-static methods)
     * @return first available local index which is NOT used by a method
     *      argument or "this"
     */
    public static int getFirstNonArgLocalIndex(Type[] args, boolean includeThis) {
        return Bytecode.getArgsSize(args) + (includeThis ? 1 : 0);
    }

    /**
     * Get the size of the specified args array in local variable terms (eg.
     * doubles and longs take two spaces)
     * 
     * @param args Method argument types as array
     * @return size of the specified arguments array in terms of stack slots
     */
    public static int getArgsSize(Type[] args) {
        int size = 0;

        for (Type type : args) {
            size += type.getSize();
        }

        return size;
    }

    /**
     * Injects appropriate LOAD opcodes into the supplied InsnList appropriate
     * for each entry in the args array starting at pos
     * 
     * @param args Argument types
     * @param insns Instruction List to inject into
     * @param pos Start position
     */
    public static void loadArgs(Type[] args, InsnList insns, int pos) {
        Bytecode.loadArgs(args, insns, pos, -1);
    }

    /**
     * Injects appropriate LOAD opcodes into the supplied InsnList appropriate
     * for each entry in the args array starting at start and ending at end
     * 
     * @param args Argument types
     * @param insns Instruction List to inject into
     * @param start Start position
     * @param end End position
     */
    public static void loadArgs(Type[] args, InsnList insns, int start, int end) {
        int pos = start;

        for (Type type : args) {
            insns.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), pos));
            pos += type.getSize();
            if (end >= start && pos >= end) {
                return;
            }
        }
    }
    
    /**
     * Clones all of the labels in the source instruction list and returns the
     * clones in a map of old label -&gt; new label. This is used to facilitate
     * the use of {@link AbstractInsnNode#clone}.
     * 
     * @param source instruction list
     * @return map of existing labels to their cloned counterparts
     */
    public static Map<LabelNode, LabelNode> cloneLabels(InsnList source) {
        Map<LabelNode, LabelNode> labels = new HashMap<LabelNode, LabelNode>();
        
        for (Iterator<AbstractInsnNode> iter = source.iterator(); iter.hasNext();) {
            AbstractInsnNode insn = iter.next();
            if (insn instanceof LabelNode) {
                labels.put((LabelNode)insn, new LabelNode(((LabelNode)insn).getLabel()));
            }
        }
        
        return labels;
    }
    
    /**
     * Generate a bytecode descriptor from the supplied tokens. Each token can
     * be a {@link Type}, a {@link Class} or otherwise is converted in-place by
     * calling {@link Object#toString toString}.
     * 
     * @param returnType object representing the method return type, can be
     *      <tt>null</tt> for <tt>void</tt>
     * @param args objects representing argument types
     */
    public static String generateDescriptor(Object returnType, Object... args) {
        StringBuilder sb = new StringBuilder().append('(');

        for (Object arg : args) {
            sb.append(Bytecode.toDescriptor(arg));
        }

        return sb.append(')').append(returnType != null ? Bytecode.toDescriptor(returnType) : "V").toString();
    }

    /**
     * Converts the supplied object to a descriptor component, used by
     * {@link #generateDescriptor}.
     * 
     * @param arg object to convert
     */
    private static String toDescriptor(Object arg) {
        if (arg instanceof String) {
            return (String)arg;
        } else if (arg instanceof Type) {
            return arg.toString();
        } else if (arg instanceof Class) {
            return Type.getDescriptor((Class<?>)arg).toString();
        }
        return arg == null ? "" : arg.toString();
    }

    /**
     * Returns the simple name of an annotation, mainly used for printing
     * annotation names in error messages/user-facing strings
     * 
     * @param annotationType annotation
     * @return annotation's simple name
     */
    public static String getSimpleName(Class<? extends Annotation> annotationType) {
        return annotationType.getSimpleName();
    }
    
    /**
     * Returns the simple name of an annotation, mainly used for printing
     * annotation names in error messages/user-facing strings
     * 
     * @param annotation annotation node
     * @return annotation's simple name
     */
    public static String getSimpleName(AnnotationNode annotation) {
        return Bytecode.getSimpleName(annotation.desc);
    }

    /**
     * Returns the simple name from an object type descriptor (in L...; format)
     * 
     * @param desc type descriptor
     * @return "simple" name
     */
    public static String getSimpleName(String desc) {
        int pos = Math.max(desc.lastIndexOf('/'), 0);
        return desc.substring(pos + 1).replace(";", "");
    }

    /**
     * Gets whether the supplied instruction is a constant instruction (eg. 
     * <tt>ICONST_1</tt>)
     * 
     * @param insn instruction to check
     * @return true if the supplied instruction is a constant
     */
    public static boolean isConstant(AbstractInsnNode insn) {
        if (insn == null) {
            return false;
        }
        return Ints.contains(Bytecode.CONSTANTS_ALL, insn.getOpcode());
    }

    /**
     * If the supplied instruction is a constant, returns the constant value
     * from the instruction
     * 
     * @param insn constant instruction to process
     * @return the constant value or <tt>null</tt> if the value cannot be parsed
     *      (or is null)
     */
    public static Object getConstant(AbstractInsnNode insn) {
        if (insn == null) {
            return null;
        } else if (insn instanceof LdcInsnNode) {
            return ((LdcInsnNode)insn).cst;
        } else if (insn instanceof IntInsnNode) {
            int value = ((IntInsnNode)insn).operand;
            if (insn.getOpcode() == Opcodes.BIPUSH || insn.getOpcode() == Opcodes.SIPUSH) {
                return Integer.valueOf(value);
            }
            throw new IllegalArgumentException("IntInsnNode with invalid opcode " + insn.getOpcode() + " in getConstant");
        }
        
        int index = Ints.indexOf(Bytecode.CONSTANTS_ALL, insn.getOpcode());
        return index < 0 ? null : Bytecode.CONSTANTS_VALUES[index];
    }

    /**
     * Returns the {@link Type} of a particular constant instruction's payload 
     * 
     * @param insn constant instruction
     * @return type of constant or <tt>null</tt> if it cannot be parsed (or is
     *      null)
     */
    public static Type getConstantType(AbstractInsnNode insn) {
        if (insn == null) {
            return null;
        } else if (insn instanceof LdcInsnNode) {
            Object cst = ((LdcInsnNode)insn).cst;
            if (cst instanceof Integer) {
                return Type.getType("I");
            } else if (cst instanceof Float) {
                return Type.getType("F");
            } else if (cst instanceof Long) {
                return Type.getType("J");
            } else if (cst instanceof Double) {
                return Type.getType("D");
            } else if (cst instanceof String) {
                return Type.getType(Constants.STRING);
            } else if (cst instanceof Type) {
                return Type.getType(Constants.CLASS);
            }
            throw new IllegalArgumentException("LdcInsnNode with invalid payload type " + cst.getClass() + " in getConstant");
        }
        
        int index = Ints.indexOf(Bytecode.CONSTANTS_ALL, insn.getOpcode());
        return index < 0 ? null : Type.getType(Bytecode.CONSTANTS_TYPES[index]);
    }
    
    /**
     * Check whether the specified flag is set on the specified class
     * 
     * @param classNode class node
     * @param flag flag to check
     * @return True if the specified flag is set in this method's access flags
     */
    public static boolean hasFlag(ClassNode classNode, int flag) {
        return (classNode.access & flag) == flag;
    }
    
    /**
     * Check whether the specified flag is set on the specified method
     * 
     * @param method method node
     * @param flag flag to check
     * @return True if the specified flag is set in this method's access flags
     */
    public static boolean hasFlag(MethodNode method, int flag) {
        return (method.access & flag) == flag;
    }
    
    /**
     * Check whether the specified flag is set on the specified field
     * 
     * @param field field node
     * @param flag flag to check
     * @return True if the specified flag is set in this field's access flags
     */
    public static boolean hasFlag(FieldNode field, int flag) {
        return (field.access & flag) == flag;
    }

    /**
     * Check whether the status of the specified flag matches on both of the
     * supplied arguments.
     * 
     * @param m1 First method
     * @param m2 Second method
     * @param flag flag to compare
     * @return True if the flag is set to the same value on both members
     */
    public static boolean compareFlags(MethodNode m1, MethodNode m2, int flag) {
        return Bytecode.hasFlag(m1, flag) == Bytecode.hasFlag(m2, flag);
    }
    
    /**
     * Check whether the status of the specified flag matches on both of the
     * supplied arguments.
     * 
     * @param f1 First field
     * @param f2 Second field
     * @param flag flag to compare
     * @return True if the flag is set to the same value on both members
     */
    public static boolean compareFlags(FieldNode f1, FieldNode f2, int flag) {
        return Bytecode.hasFlag(f1, flag) == Bytecode.hasFlag(f2, flag);
    }
    
    /**
     * Returns the <i>ordinal visibility</i> of the supplied argument where a
     * higher value equals higher "visibility":
     * 
     * <ol start="0">
     *   <li>{@link #Visibility.PRIVATE}</li>
     *   <li>{@link #Visibility.PROTECTED}</li>
     *   <li>{@link #Visibility.PACKAGE}</li>
     *   <li>{@link #Visibility.PUBLIC}</li>
     * </ol>
     * 
     * @param method method to get visibility for
     * @return visibility level
     */
    public static Visibility getVisibility(MethodNode method) {
        return Bytecode.getVisibility(method.access & 0x7);
    }
    
    /**
     * Returns the <i>ordinal visibility</i> of the supplied argument where a
     * higher value equals higher "visibility":
     * 
     * <ol start="0">
     *   <li>{@link Visibility#PRIVATE}</li>
     *   <li>{@link Visibility#PROTECTED}</li>
     *   <li>{@link Visibility#PACKAGE}</li>
     *   <li>{@link Visibility#PUBLIC}</li>
     * </ol>
     * 
     * @param field field to get visibility for
     * @return visibility level
     */
    public static Visibility getVisibility(FieldNode field) {
        return Bytecode.getVisibility(field.access & 0x7);
    }

    /**
     * Returns the <i>ordinal visibility</i> of the supplied argument where a
     * higher value equals higher "visibility":
     * 
     * <ol start="0">
     *   <li>{@link Visibility#PRIVATE}</li>
     *   <li>{@link Visibility#PROTECTED}</li>
     *   <li>{@link Visibility#PACKAGE}</li>
     *   <li>{@link Visibility#PUBLIC}</li>
     * </ol>
     * 
     * @param flags access flags of member
     * @return visibility level
     */
    private static Visibility getVisibility(int flags) {
        if ((flags & Opcodes.ACC_PROTECTED) != 0) {
            return Bytecode.Visibility.PROTECTED;
        } else if ((flags & Opcodes.ACC_PRIVATE) != 0) {
            return Bytecode.Visibility.PRIVATE;
        } else if ((flags & Opcodes.ACC_PUBLIC) != 0) {
            return Bytecode.Visibility.PUBLIC;
        }
        return Bytecode.Visibility.PACKAGE;
    }
    
    /**
     * Compute the largest line number found in the specified class
     * 
     * @param classNode Class to inspect
     * @param min minimum value to return
     * @param pad amount to pad at the end of files
     * @return computed max
     */
    public static int getMaxLineNumber(ClassNode classNode, int min, int pad) {
        int max = 0;
        for (MethodNode method : classNode.methods) {
            for (Iterator<AbstractInsnNode> iter = method.instructions.iterator(); iter.hasNext();) {
                AbstractInsnNode insn = iter.next();
                if (insn instanceof LineNumberNode) {
                    max = Math.max(max, ((LineNumberNode)insn).line);
                }
            }
        }
        return Math.max(min, max + pad);
    }

}
