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

import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;
import org.spongepowered.asm.util.asm.ASM;
import org.spongepowered.asm.util.throwables.SyntheticBridgeException;
import org.spongepowered.asm.util.throwables.SyntheticBridgeException.Problem;

import com.google.common.base.Joiner;
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
        PRIVATE(Opcodes.ACC_PRIVATE),
        
        /**
         * Members decorated with {@link Opcodes#ACC_PROTECTED} 
         */
        PROTECTED(Opcodes.ACC_PROTECTED),
        
        /**
         * Members not decorated with any access flags
         */
        PACKAGE(0),
        
        /**
         * Members decorated with {@link Opcodes#ACC_PUBLIC} 
         */
        PUBLIC(Opcodes.ACC_PUBLIC);
        
        static final int MASK = Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED | Opcodes.ACC_PUBLIC;
        
        final int access;

        private Visibility(int access) {
            this.access = access;
        }
        
        /**
         * Get whether this visibility level represents a level which is the
         * same or greater than the supplied level
         * 
         * @param other level to compare to
         * @return true if greater or equal
         */
        public boolean isAtLeast(Visibility other) {
            return other == null || other.ordinal() <= this.ordinal();
        }
        
    }
    
    /**
     * Information bundle returned from {@link Bytecode#findDelegateInit}
     */
    public static class DelegateInitialiser {
        
        /**
         * No delegate initialiser found
         */
        public static final DelegateInitialiser NONE = new DelegateInitialiser(null, false);
        
        /**
         * Constructor invocation
         */
        public final MethodInsnNode insn;
        
        /**
         * True if the invocation is a super call, false if it's a call to
         * another ctor in the same class
         */
        public final boolean isSuper;

        /**
         * True if the invocation is found, false if no delegate constructor
         * was found (false for DelegateInitialiser.NONE)
         */
        public final boolean isPresent;

        DelegateInitialiser(MethodInsnNode insn, boolean isSuper) {
            this.insn = insn;
            this.isSuper = isSuper;
            this.isPresent = insn != null;
        }

        @Override
        public String toString() {
            return this.isSuper ? "super" : "this";
        }
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
    
    /**
     * Mapping of {@link Type#getSort()} return values to boxing types 
     */
    private static final String[] BOXING_TYPES = {
        null,
        "java/lang/Boolean",
        "java/lang/Character",
        "java/lang/Byte",
        "java/lang/Short",
        "java/lang/Integer",
        "java/lang/Float",
        "java/lang/Long",
        "java/lang/Double",
        null,
        null,
        null
    };
    
    /**
     * Mapping of {@link Type#getSort()} return values to boxing types 
     */
    private static final String[] UNBOXING_METHODS = {
        null,
        "booleanValue",
        "charValue",
        "byteValue",
        "shortValue",
        "intValue",
        "floatValue",
        "longValue",
        "doubleValue",
        null,
        null,
        null
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
     * Find the first insn node with a matching opcode in the specified method
     * 
     * @param method method to search
     * @param opcode opcode to search for
     * @return found node or null if not found 
     */
    public static AbstractInsnNode findInsn(MethodNode method, int opcode) {
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
     * Find the call to <tt>super()</tt> or <tt>this()</tt> in a constructor.
     * This attempts to locate the first call to <tt>&lt;init&gt;</tt> which
     * isn't an inline call to another object ctor being passed into the super
     * invocation.
     * 
     * @param ctor ctor to scan
     * @param superName name of superclass
     * @param ownerName name of owning class
     * @return Call to <tt>super()</tt>, <tt>this()</tt> or
     *      <tt>DelegateInitialiser.NONE</tt> if not found
     */
    public static DelegateInitialiser findDelegateInit(MethodNode ctor, String superName, String ownerName) {
        if (!Constants.CTOR.equals(ctor.name)) {
            return DelegateInitialiser.NONE;
        }
        
        int news = 0;
        for (Iterator<AbstractInsnNode> iter = ctor.instructions.iterator(); iter.hasNext();) {
            AbstractInsnNode insn = iter.next();
            if (insn instanceof TypeInsnNode && insn.getOpcode() == Opcodes.NEW) {
                news++;
            } else if (insn instanceof MethodInsnNode && insn.getOpcode() == Opcodes.INVOKESPECIAL) {
                MethodInsnNode methodNode = (MethodInsnNode)insn;
                if (Constants.CTOR.equals(methodNode.name)) {
                    if (news > 0) {
                        news--;
                    } else {
                        boolean isSuper = methodNode.owner.equals(superName);
                        if (isSuper || methodNode.owner.equals(ownerName)) {
                            return new DelegateInitialiser(methodNode, isSuper);
                        }
                    }
                }
            }
        }
        return DelegateInitialiser.NONE;
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
     * @param classNode the ClassNode to verify
     */
    public static void dumpClass(ClassNode classNode) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
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
        System.err.printf("%s%s maxStack=%d maxLocals=%d\n", method.name, method.desc, method.maxStack, method.maxLocals);
        int index = 0;
        for (Iterator<AbstractInsnNode> iter = method.instructions.iterator(); iter.hasNext();) {
            System.err.printf("%-4d  ", index++);
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
        return node != null ? Bytecode.getOpcodeName(node.getOpcode()) : "";
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
     * Returns true if the supplied method contains any line number information
     * 
     * @param method Method to scan
     * @return true if a line number node is located
     */
    public static boolean methodHasLineNumbers(MethodNode method) {
        for (Iterator<AbstractInsnNode> iter = method.instructions.iterator(); iter.hasNext();) {
            if (iter.next() instanceof LineNumberNode) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the supplied method node is static
     * 
     * @param method method node
     * @return true if the method has the {@link Opcodes#ACC_STATIC} flag
     */
    public static boolean isStatic(MethodNode method) {
        return (method.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
    }
    
    /**
     * Returns true if the supplied field node is static
     * 
     * @param field field node
     * @return true if the field has the {@link Opcodes#ACC_STATIC} flag
     */
    public static boolean isStatic(FieldNode field) {
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
        Bytecode.loadArgs(args, insns, start, end, null);
    }

    /**
     * Injects appropriate LOAD opcodes into the supplied InsnList appropriate
     * for each entry in the args array starting at start and ending at end
     * 
     * @param args Argument types
     * @param insns Instruction List to inject into
     * @param start Start position
     * @param end End position
     * @param casts Type casts array
     */
    public static void loadArgs(Type[] args, InsnList insns, int start, int end, Type[] casts) {
        int pos = start, index = 0;

        for (Type type : args) {
            insns.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), pos));
            if (casts != null && index < casts.length && casts[index] != null) {
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, casts[index].getInternalName()));
            }
            pos += type.getSize();
            if (end >= start && pos >= end) {
                return;
            }
            index++;
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
            return Type.getDescriptor((Class<?>)arg);
        }
        return arg == null ? "" : arg.toString();
    }
    
    /**
     * Generate a method descriptor without return type for the supplied args
     * array
     * 
     * @param args argument types
     * @return method descriptor without return type
     */
    public static String getDescriptor(Type[] args) {
        return "(" + Joiner.on("").join(args) + ")";
    }
    
    /**
     * Generate a method descriptor with the specified types
     * 
     * @param args argument types
     * @param returnType return type
     * @return generated method descriptor
     */
    public static String getDescriptor(Type[] args, Type returnType) {
        return Bytecode.getDescriptor(args) + returnType.toString();
    }

    /**
     * Changes the return type of a method descriptor to the specified symbol
     * 
     * @param desc descriptor to modify
     * @param returnType new return type
     * @return modified descriptor;
     */
    public static String changeDescriptorReturnType(String desc, String returnType) {
        if (desc == null || !desc.startsWith("(") || desc.lastIndexOf(')') < 1) {
            return null;
        } else if (returnType == null) {
            return desc;
        }
        return desc.substring(0, desc.lastIndexOf(')') + 1) + returnType;
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
     * Returns the simple name of a type representing a class
     * 
     * @param type type
     * @return annotation's simple name
     */
    public static String getSimpleName(Type type) {
        return type.getSort() < Type.ARRAY ? type.getDescriptor() : Bytecode.getSimpleName(type.getClassName());
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
                return Type.getType(Constants.STRING_DESC);
            } else if (cst instanceof Type) {
                return Type.getType(Constants.CLASS_DESC);
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
     * Checks whether the supplied method is virtual. Specifically this method
     * returns true if the method is non-static and has an access level greater
     * than <tt>private</tt>.
     * 
     * @param method Method to test
     * @return true if virtual
     */
    public static boolean isVirtual(MethodNode method) {
        return method != null && !Bytecode.isStatic(method) && Bytecode.getVisibility(method).isAtLeast(Visibility.PROTECTED);
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
     * Set the visibility of the specified member, leaving other access flags
     * unchanged
     * 
     * @param method method to change
     * @param visibility new visibility
     */
    public static void setVisibility(MethodNode method, Visibility visibility) {
        method.access = Bytecode.setVisibility(method.access, visibility.access);
    }
    
    /**
     * Set the visibility of the specified member, leaving other access flags
     * unchanged
     * 
     * @param field field to change
     * @param visibility new visibility
     */
    public static void setVisibility(FieldNode field, Visibility visibility) {
        field.access = Bytecode.setVisibility(field.access, visibility.access);
    }
    
    /**
     * Set the visibility of the specified member, leaving other access flags
     * unchanged
     * 
     * @param method method to change
     * @param access new visibility
     */
    public static void setVisibility(MethodNode method, int access) {
        method.access = Bytecode.setVisibility(method.access, access);
    }
    
    /**
     * Set the visibility of the specified member, leaving other access flags
     * unchanged
     * 
     * @param field field to change
     * @param access new visibility
     */
    public static void setVisibility(FieldNode field, int access) {
        field.access = Bytecode.setVisibility(field.access, access);
    }
    
    private static int setVisibility(int oldAccess, int newAccess) {
        return oldAccess & ~Visibility.MASK | (newAccess & Visibility.MASK);
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

    /**
     * Get the boxing type name for the specified type, if it is a primitive.
     * For non-primitive types, <tt>null</tt> is returned
     * 
     * @param type type to box
     * @return boxing type or null
     */
    public static String getBoxingType(Type type) {
        return type == null ? null : Bytecode.BOXING_TYPES[type.getSort()];
    }
    
    /**
     * Get the unboxing method name for the specified primitive type's
     * corresponding reference type. For example, if the type passed in is
     * <tt>int</tt>, then the return value will be <tt>intValue</tt>. Returns
     * <tt>null</tt> for non-primitive types.
     * 
     * @param type primitive type to get unboxing method for
     * @return unboxing method name or <tt>null</tt>
     */
    public static String getUnboxingMethod(Type type) {
        return type == null ? null : Bytecode.UNBOXING_METHODS[type.getSort()];
    }

    /**
     * Compares two synthetic bridge methods and throws an exception if they are
     * not compatible.
     * 
     * @param a Incumbent method
     * @param b Incoming method
     */
    public static void compareBridgeMethods(MethodNode a, MethodNode b) {
        ListIterator<AbstractInsnNode> ia = a.instructions.iterator();
        ListIterator<AbstractInsnNode> ib = b.instructions.iterator();
        
        int index = 0;
        for (; ia.hasNext() && ib.hasNext(); index++) {
            AbstractInsnNode na = ia.next();
            AbstractInsnNode nb = ib.next();
            if (na instanceof LabelNode) {
                continue;
            } 
            
            if (na instanceof MethodInsnNode) {
                MethodInsnNode ma = (MethodInsnNode)na;
                MethodInsnNode mb = (MethodInsnNode)nb;
                if (!ma.name.equals(mb.name)) {
                    throw new SyntheticBridgeException(Problem.BAD_INVOKE_NAME, a.name, a.desc, index, na, nb);
                } else if (!ma.desc.equals(mb.desc)) {
                    throw new SyntheticBridgeException(Problem.BAD_INVOKE_DESC, a.name, a.desc, index, na, nb);
                }
            } else if (na.getOpcode() != nb.getOpcode()) {
                throw new SyntheticBridgeException(Problem.BAD_INSN, a.name, a.desc, index, na, nb);
            } else if (na instanceof VarInsnNode) {
                VarInsnNode va = (VarInsnNode)na;
                VarInsnNode vb = (VarInsnNode)nb;
                if (va.var != vb.var) {
                    throw new SyntheticBridgeException(Problem.BAD_LOAD, a.name, a.desc, index, na, nb);
                }
            } else if (na instanceof TypeInsnNode) {
                TypeInsnNode ta = (TypeInsnNode)na;
                TypeInsnNode tb = (TypeInsnNode)nb;
                if (ta.getOpcode() == Opcodes.CHECKCAST && !ta.desc.equals(tb.desc)) {
                    throw new SyntheticBridgeException(Problem.BAD_CAST, a.name, a.desc, index, na, nb);
                }
            }
        }
        
        if (ia.hasNext() || ib.hasNext()) {
            throw new SyntheticBridgeException(Problem.BAD_LENGTH, a.name, a.desc, index, null, null);
        }
    }

    /**
     * Perform a naïve merge of ClassNode members onto a target ClassNode 
     * 
     * @param source Source ClassNode to merge from
     * @param dest Destination ClassNode to merge to
     */
    public static void merge(ClassNode source, ClassNode dest) {
        if (source == null) {
            return;
        }
        
        if (dest == null) {
            throw new NullPointerException("Target ClassNode for merge must not be null");
        }
        
        dest.version = Math.max(source.version, dest.version);
        
        dest.interfaces = Bytecode.<String>merge(source.interfaces, dest.interfaces);
        dest.invisibleAnnotations = Bytecode.<AnnotationNode>merge(source.invisibleAnnotations, dest.invisibleAnnotations);
        dest.visibleAnnotations = Bytecode.<AnnotationNode>merge(source.visibleAnnotations, dest.visibleAnnotations);
        dest.visibleTypeAnnotations = Bytecode.<TypeAnnotationNode>merge(source.visibleTypeAnnotations, dest.visibleTypeAnnotations);
        dest.invisibleTypeAnnotations = Bytecode.<TypeAnnotationNode>merge(source.invisibleTypeAnnotations, dest.invisibleTypeAnnotations);
        dest.attrs = Bytecode.<Attribute>merge(source.attrs, dest.attrs);
        dest.innerClasses = Bytecode.<InnerClassNode>merge(source.innerClasses, dest.innerClasses);
        dest.fields = Bytecode.<FieldNode>merge(source.fields, dest.fields);
        dest.methods = Bytecode.<MethodNode>merge(source.methods, dest.methods);
        
    }
    
    /**
     * Replace all values in a target ClassNode with values from the Source 
     * 
     * @param source Source ClassNode to merge from
     * @param dest Destination ClassNode to merge to
     */
    public static void replace(ClassNode source, ClassNode dest) {
        if (source == null) {
            return;
        }
        
        if (dest == null) {
            throw new NullPointerException("Target ClassNode for replace must not be null");
        }
        
        dest.name = source.name;
        dest.signature = source.signature;
        dest.superName = source.superName;

        dest.version = source.version;
        dest.access = source.access;
        dest.sourceDebug = source.sourceDebug;

        dest.sourceFile = source.sourceFile;
        dest.outerClass = source.outerClass;
        dest.outerMethod = source.outerMethod;
        dest.outerMethodDesc = source.outerMethodDesc;
        
        Bytecode.<String>clear(dest.interfaces);
        Bytecode.<AnnotationNode>clear(dest.visibleAnnotations);
        Bytecode.<AnnotationNode>clear(dest.invisibleAnnotations);
        Bytecode.<TypeAnnotationNode>clear(dest.visibleTypeAnnotations);
        Bytecode.<TypeAnnotationNode>clear(dest.invisibleTypeAnnotations);
        Bytecode.<Attribute>clear(dest.attrs);
        Bytecode.<InnerClassNode>clear(dest.innerClasses);
        Bytecode.<FieldNode>clear(dest.fields);
        Bytecode.<MethodNode>clear(dest.methods);

        if (ASM.API_VERSION >= Opcodes.ASM6) {
            dest.module = source.module;
        }
        
        // TODO Java 10
//        dest.nestHostClassExperimental = source.nestHostClassExperimental;
//        Bytecode.<String>clear(dest.nestMembersExperimental);
//        dest.nestMembersExperimental = Bytecode.<String>merge(source.nestMembersExperimental, dest.nestMembersExperimental);
        
        Bytecode.merge(source, dest);
        
    }
    
    /**
     * Helper function to clear a list if it is not null
     */
    private static <T> void clear(List<T> list) {
        if (list != null) {
            list.clear();
        }
    }
    
    /**
     * Helper function to merge all values of a source list into a destination
     * taking into account that the source or destination are allowed to be null
     * 
     * @param source source list
     * @param destination destination list
     * @return destination list populated with source values, new list if the
     *      destination was null, or null if both are null or empty
     */
    private static <T> List<T> merge(List<T> source, List<T> destination) {
        if (source == null || source.isEmpty()) {
            return destination;
        }
        
        if (destination == null) {
            return new ArrayList<T>(source);
        }
        
        destination.addAll(source);
        return destination;
    }

}
