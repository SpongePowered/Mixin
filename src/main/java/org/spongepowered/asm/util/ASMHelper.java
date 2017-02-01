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

import static com.google.common.base.Preconditions.*;
import static org.spongepowered.asm.lib.ClassWriter.*;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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
 * Utility methods for working with ASM
 */
public final class ASMHelper {

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
    
    private ASMHelper() {
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
        ASMHelper.dumpClass(cw.toByteArray());
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
            System.err.printf("[%4d] %s\n", i++, ASMHelper.describeNode(iter.next()));
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
            ASMHelper.printNode(iter.next());
        }
    }

    /**
     * Prints a representation of the specified insn node to stderr
     * 
     * @param node Node to print
     */
    public static void printNode(AbstractInsnNode node) {
        System.err.printf("%s\n", ASMHelper.describeNode(node));
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
            out += String.format("[%s] [%s]", ASMHelper.getOpcodeName(node), ((JumpInsnNode)node).label.getLabel());
        } else if (node instanceof VarInsnNode) {
            out += String.format("[%s] %d", ASMHelper.getOpcodeName(node), ((VarInsnNode)node).var);
        } else if (node instanceof MethodInsnNode) {
            MethodInsnNode mth = (MethodInsnNode)node;
            out += String.format("[%s] %s %s %s", ASMHelper.getOpcodeName(node), mth.owner, mth.name, mth.desc);
        } else if (node instanceof FieldInsnNode) {
            FieldInsnNode fld = (FieldInsnNode)node;
            out += String.format("[%s] %s %s %s", ASMHelper.getOpcodeName(node), fld.owner, fld.name, fld.desc);
        } else if (node instanceof LineNumberNode) {
            LineNumberNode ln = (LineNumberNode)node;
            out += String.format("LINE=[%d] LABEL=[%s]", ln.line, ln.start.getLabel());
        } else if (node instanceof LdcInsnNode) {
            out += (((LdcInsnNode)node).cst);
        } else if (node instanceof IntInsnNode) {
            out += (((IntInsnNode)node).operand);
        } else if (node instanceof FrameNode) {
            out += String.format("[%s] ", ASMHelper.getOpcodeName(((FrameNode)node).type, "H_INVOKEINTERFACE", -1));
        } else {
            out += String.format("[%s] ", ASMHelper.getOpcodeName(node));
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
        return ASMHelper.getOpcodeName(node.getOpcode());
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
        return ASMHelper.getOpcodeName(opcode, "UNINITIALIZED_THIS", 1);
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
     * Set a runtime-visible annotation of the specified class on the supplied
     * field node
     *
     * @param field Target field
     * @param annotationClass Type of annotation to search for
     * @param value Values (interleaved key/value pairs) to set
     */
    public static void setVisibleAnnotation(FieldNode field, Class<? extends Annotation> annotationClass, Object... value) {
        AnnotationNode node = ASMHelper.makeAnnotationNode(Type.getDescriptor(annotationClass), value);
        field.visibleAnnotations = ASMHelper.addAnnotation(field.visibleAnnotations, node);
    }
    
    /**
     * Set an invisible annotation of the specified class on the supplied field
     * node
     *
     * @param field Target field
     * @param annotationClass Type of annotation to search for
     * @param value Values (interleaved key/value pairs) to set
     */
    public static void setInvisibleAnnotation(FieldNode field, Class<? extends Annotation> annotationClass, Object... value) {
        AnnotationNode node = ASMHelper.makeAnnotationNode(Type.getDescriptor(annotationClass), value);
        field.invisibleAnnotations = ASMHelper.addAnnotation(field.invisibleAnnotations, node);
    }
    
    /**
     * Set a runtime-visible annotation of the specified class on the supplied
     * method node
     *
     * @param method Target method
     * @param annotationClass Type of annotation to search for
     * @param value Values (interleaved key/value pairs) to set
     */
    public static void setVisibleAnnotation(MethodNode method, Class<? extends Annotation> annotationClass, Object... value) {
        AnnotationNode node = ASMHelper.makeAnnotationNode(Type.getDescriptor(annotationClass), value);
        method.visibleAnnotations = ASMHelper.addAnnotation(method.visibleAnnotations, node);
    }
    
    /**
     * Set a invisible annotation of the specified class on the supplied method
     * node
     *
     * @param method Target method
     * @param annotationClass Type of annotation to search for
     * @param value Values (interleaved key/value pairs) to set
     */
    public static void setInvisibleAnnotation(MethodNode method, Class<? extends Annotation> annotationClass, Object... value) {
        AnnotationNode node = ASMHelper.makeAnnotationNode(Type.getDescriptor(annotationClass), value);
        method.invisibleAnnotations = ASMHelper.addAnnotation(method.invisibleAnnotations, node);
    }

    /**
     * Create a new annotation node with the supplied values
     * 
     * @param annotationType Name (internal name) of the annotation interface to
     *      create
     * @param value Interleaved key/value pairs. Keys must be strings
     * @return new annotation node
     */
    private static AnnotationNode makeAnnotationNode(String annotationType, Object... value) {
        AnnotationNode node = new AnnotationNode(annotationType);
        for (int pos = 0; pos < value.length - 1; pos += 2) {
            if (!(value[pos] instanceof String)) {
                throw new IllegalArgumentException("Annotation keys must be strings, found " + value[pos].getClass().getSimpleName()
                        + " with " + value[pos].toString() + " at index " + pos + " creating " + annotationType);
            }
            node.visit((String)value[pos], value[pos + 1]);
        }
        return node;
    }
    
    private static List<AnnotationNode> addAnnotation(List<AnnotationNode> annotations, AnnotationNode node) {
        if (annotations == null) {
            annotations = new ArrayList<AnnotationNode>(1);
        } else {
            annotations.remove(ASMHelper.getAnnotation(annotations, node.desc));
        }
        annotations.add(node);
        return annotations;
    }

    /**
     * Get a runtime-visible annotation of the specified class from the supplied
     * field node
     *
     * @param field Source field
     * @param annotationClass Type of annotation to search for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getVisibleAnnotation(FieldNode field, Class<? extends Annotation> annotationClass) {
        return ASMHelper.getAnnotation(field.visibleAnnotations, Type.getDescriptor(annotationClass));
    }

    /**
     * Get an invisible annotation of the specified class from the supplied
     * field node
     *
     * @param field Source field
     * @param annotationClass Type of annotation to search for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getInvisibleAnnotation(FieldNode field, Class<? extends Annotation> annotationClass) {
        return ASMHelper.getAnnotation(field.invisibleAnnotations, Type.getDescriptor(annotationClass));
    }

    /**
     * Get a runtime-visible annotation of the specified class from the supplied
     * method node
     *
     * @param method Source method
     * @param annotationClass Type of annotation to search for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getVisibleAnnotation(MethodNode method, Class<? extends Annotation> annotationClass) {
        return ASMHelper.getAnnotation(method.visibleAnnotations, Type.getDescriptor(annotationClass));
    }

    /**
     * Get an invisible annotation of the specified class from the supplied
     * method node
     *
     * @param method Source method
     * @param annotationClass Type of annotation to search for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getInvisibleAnnotation(MethodNode method, Class<? extends Annotation> annotationClass) {
        return ASMHelper.getAnnotation(method.invisibleAnnotations, Type.getDescriptor(annotationClass));
    }

    /**
     * Get a runtime-visible annotation of the specified class from the supplied
     * method node
     *
     * @param method Source method
     * @param annotationClasses Types of annotation to search for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getSingleVisibleAnnotation(MethodNode method, Class<? extends Annotation>... annotationClasses) {
        return ASMHelper.getSingleAnnotation(method.visibleAnnotations, annotationClasses);
    }

    /**
     * Get an invisible annotation of the specified class from the supplied
     * method node
     *
     * @param method Source method
     * @param annotationClasses Types of annotation to search for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getSingleInvisibleAnnotation(MethodNode method, Class<? extends Annotation>... annotationClasses) {
        return ASMHelper.getSingleAnnotation(method.invisibleAnnotations, annotationClasses);
    }

    /**
     * Get a runtime-visible annotation of the specified class from the supplied
     * class node
     *
     * @param classNode Source classNode
     * @param annotationClass Type of annotation to search for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getVisibleAnnotation(ClassNode classNode, Class<? extends Annotation> annotationClass) {
        return ASMHelper.getAnnotation(classNode.visibleAnnotations, Type.getDescriptor(annotationClass));
    }

    /**
     * Get an invisible annotation of the specified class from the supplied
     * class node
     *
     * @param classNode Source classNode
     * @param annotationClass Type of annotation to search for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getInvisibleAnnotation(ClassNode classNode, Class<? extends Annotation> annotationClass) {
        return ASMHelper.getAnnotation(classNode.invisibleAnnotations, Type.getDescriptor(annotationClass));
    }

    /**
     * Get a runtime-visible parameter annotation of the specified class from
     * the supplied method node
     *
     * @param method Source method
     * @param annotationClass Type of annotation to search for
     * @param paramIndex Index of the parameter to fetch annotation for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getVisibleParameterAnnotation(MethodNode method, Class<? extends Annotation> annotationClass, int paramIndex) {
        return ASMHelper.getParameterAnnotation(method.visibleParameterAnnotations, Type.getDescriptor(annotationClass), paramIndex);
    }

    /**
     * Get an invisible parameter annotation of the specified class from the
     * supplied method node
     *
     * @param method Source method
     * @param annotationClass Type of annotation to search for
     * @param paramIndex Index of the parameter to fetch annotation for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getInvisibleParameterAnnotation(MethodNode method, Class<? extends Annotation> annotationClass, int paramIndex) {
        return ASMHelper.getParameterAnnotation(method.invisibleParameterAnnotations, Type.getDescriptor(annotationClass), paramIndex);
    }

    /**
     * Get a parameter annotation of the specified class from the supplied
     * method node
     *
     * @param parameterAnnotations Annotations for the parameter
     * @param annotationType Type of annotation to search for
     * @param paramIndex Index of the parameter to fetch annotation for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getParameterAnnotation(List<AnnotationNode>[] parameterAnnotations, String annotationType, int paramIndex) {
        if (parameterAnnotations == null || paramIndex < 0 || paramIndex >= parameterAnnotations.length) {
            return null;
        }
        
        return ASMHelper.getAnnotation(parameterAnnotations[paramIndex], annotationType);
    }

    /**
     * Search for and return an annotation node matching the specified type
     * within the supplied
     * collection of annotation nodes
     *
     * @param annotations Haystack
     * @param annotationType Needle
     * @return matching annotation node or null if the annotation doesn't exist
     */
    public static AnnotationNode getAnnotation(List<AnnotationNode> annotations, String annotationType) {
        if (annotations == null) {
            return null;
        }

        for (AnnotationNode annotation : annotations) {
            if (annotationType.equals(annotation.desc)) {
                return annotation;
            }
        }

        return null;
    }

    private static AnnotationNode getSingleAnnotation(List<AnnotationNode> annotations, Class<? extends Annotation>[] annotationClasses) {
        List<AnnotationNode> nodes = new ArrayList<AnnotationNode>();
        for (Class<? extends Annotation> annotationClass : annotationClasses) {
            AnnotationNode annotation = ASMHelper.getAnnotation(annotations, Type.getDescriptor(annotationClass));
            if (annotation != null) {
                nodes.add(annotation);
            }
        }
        
        int foundNodes = nodes.size();
        if (foundNodes > 1) {
            throw new IllegalArgumentException("Conflicting annotations found: " + annotationClasses);
        }
    
        return foundNodes == 0 ? null : nodes.get(0);
    }

    /**
     * Duck type the "value" entry (if any) of the specified annotation node
     *
     * @param <T> duck type
     * @param annotation Annotation node to query
     * @return duck-typed annotation value, null if missing, or inevitable
     *      {@link ClassCastException} if your duck is actually a rooster 
     */
    public static <T> T getAnnotationValue(AnnotationNode annotation) {
        return ASMHelper.getAnnotationValue(annotation, "value");
    }

    /**
     * Get the value of an annotation node and do pseudo-duck-typing via Java's
     * crappy generics
     *
     * @param <T> duck type
     * @param annotation Annotation node to query
     * @param key Key to search for
     * @param defaultValue Value to return if the specified key is not found or
     *      is null
     * @return duck-typed annotation value, null if missing, or inevitable
     *      {@link ClassCastException} if your duck is actually a rooster 
     */
    public static <T> T getAnnotationValue(AnnotationNode annotation, String key, T defaultValue) {
        T returnValue = ASMHelper.getAnnotationValue(annotation, key);
        return returnValue != null ? returnValue : defaultValue;
    }

    /**
     * Gets an annotation value or returns the default value of the annotation
     * if the annotation value is not present
     * 
     * @param <T> duck type
     * @param annotation Annotation node to query
     * @param key Key to search for
     * @param annotationClass Annotation class to query reflectively for the
     *      default value
     * @return Value of the specified annotation node, default value if not
     *      specified, or null if no value or default
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAnnotationValue(AnnotationNode annotation, String key, Class<?> annotationClass) {
        checkNotNull(annotationClass, "annotationClass cannot be null");
        T value = ASMHelper.getAnnotationValue(annotation, key);
        if (value == null) {
            try {
                value = (T)annotationClass.getDeclaredMethod(key).getDefaultValue();
            } catch (NoSuchMethodException ex) {
                // Don't care
            }
        }
        return value;
    }

    /**
     * Get the value of an annotation node and do pseudo-duck-typing via Java's
     * crappy generics
     *
     * @param <T> duck type
     * @param annotation Annotation node to query
     * @param key Key to search for
     * @return duck-typed annotation value, null if missing, or inevitable
     *      {@link ClassCastException} if your duck is actually a rooster 
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAnnotationValue(AnnotationNode annotation, String key) {
        boolean getNextValue = false;

        if (annotation == null || annotation.values == null) {
            return null;
        }

        // Keys and value are stored in successive pairs, search for the key and if found return the following entry
        for (Object value : annotation.values) {
            if (getNextValue) {
                return (T) value;
            }
            if (value.equals(key)) {
                getNextValue = true;
            }
        }

        return null;
    }

    /**
     * Get the value of an annotation node as the specified enum, returns
     * defaultValue if the annotation value is not set
     *
     * @param <T> duck type
     * @param annotation Annotation node to query
     * @param key Key to search for
     * @param enumClass Class of enum containing the enum constant to search for
     * @param defaultValue Value to return if the specified key isn't found
     * @return duck-typed annotation value or defaultValue if missing
     */
    public static <T extends Enum<T>> T getAnnotationValue(AnnotationNode annotation, String key, Class<T> enumClass, T defaultValue) {
        String[] value = ASMHelper.<String[]>getAnnotationValue(annotation, key);
        if (value == null) {
            return defaultValue;
        }
        return ASMHelper.toEnumValue(enumClass, value);
    }

    /**
     * Return the specified annotation node value as a list of nodes
     * 
     * @param <T> list element type
     * @param annotation Annotation node to query
     * @param key Key to search for
     * @param notNull if true, return an empty list instead of null if the
     *      annotation value is absent
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> getAnnotationValue(AnnotationNode annotation, String key, boolean notNull) {
        Object value = ASMHelper.<Object>getAnnotationValue(annotation, key);
        if (value instanceof List) {
            return (List<T>)value;
        } else if (value != null) {
            List<T> list = new ArrayList<T>();
            list.add((T)value);
            return list;
        }
        return Collections.<T>emptyList();
    }
    
    /**
     * Return the specified annotation node value as a list of enums
     * 
     * @param <T> list element type
     * @param annotation Annotation node to query
     * @param key Key to search for
     * @param notNull if true, return an empty list instead of null if the
     *      annotation value is absent
     * @param enumClass Class of enum containing the enum constant to use
     */
    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> List<T> getAnnotationValue(AnnotationNode annotation, String key, boolean notNull, Class<T> enumClass) {
        Object value = ASMHelper.<Object>getAnnotationValue(annotation, key);
        if (value instanceof List) {
            for (ListIterator<Object> iter = ((List<Object>)value).listIterator(); iter.hasNext();) {
                iter.set(ASMHelper.toEnumValue(enumClass, (String[])iter.next()));
            }
            return (List<T>)value;
        } else if (value instanceof String[]) {
            List<T> list = new ArrayList<T>();
            list.add(ASMHelper.toEnumValue(enumClass, (String[])value));
            return list;
        }
        return Collections.<T>emptyList();
    }

    private static <T extends Enum<T>> T toEnumValue(Class<T> enumClass, String[] value) {
        if (!enumClass.getName().equals(Type.getType(value[0]).getClassName())) {
            throw new IllegalArgumentException("The supplied enum class does not match the stored enum value");
        }
        return Enum.valueOf(enumClass, value[1]);
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
        return ASMHelper.getFirstNonArgLocalIndex(Type.getArgumentTypes(method.desc), (method.access & Opcodes.ACC_STATIC) == 0);
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
        return ASMHelper.getArgsSize(args) + (includeThis ? 1 : 0);
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
        ASMHelper.loadArgs(args, insns, pos, -1);
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
            sb.append(ASMHelper.toDescriptor(arg));
        }

        return sb.append(')').append(returnType != null ? ASMHelper.toDescriptor(returnType) : "V").toString();
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
        return ASMHelper.getSimpleName(annotation.desc);
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
        return Ints.contains(ASMHelper.CONSTANTS_ALL, insn.getOpcode());
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
        
        int index = Ints.indexOf(ASMHelper.CONSTANTS_ALL, insn.getOpcode());
        return index < 0 ? null : ASMHelper.CONSTANTS_VALUES[index];
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
        
        int index = Ints.indexOf(ASMHelper.CONSTANTS_ALL, insn.getOpcode());
        return index < 0 ? null : Type.getType(ASMHelper.CONSTANTS_TYPES[index]);
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
