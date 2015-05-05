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
package org.spongepowered.asm.util;

import static org.spongepowered.asm.lib.ClassWriter.*;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.ClassWriter;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.*;
import org.spongepowered.asm.lib.util.CheckClassAdapter;

public class ASMHelper {

    private static final int[] intConstants = new int[]
            {Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5};

    /**
     * Generate a new method "boolean name()", which returns a constant value.
     *
     * @param clazz Class to add method to
     * @param name Name of method
     * @param retval Return value of method
     */
    public static void generateBooleanMethodConst(ClassNode clazz, String name, boolean retval) {
        MethodNode method = new MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, name, "()Z", null, null);
        InsnList code = method.instructions;

        code.add(ASMHelper.pushIntConstant(retval ? 1 : 0));
        code.add(new InsnNode(Opcodes.IRETURN));

        clazz.methods.add(method);
    }

    /**
     * Generate a new method "int name()", which returns a constant value.
     *
     * @param clazz Class to add method to
     * @param name Name of method
     * @param retval Return value of method
     */
    public static void generateIntegerMethodConst(ClassNode clazz, String name, short retval) {
        MethodNode method = new MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, name, "()I", null, null);
        InsnList code = method.instructions;

        code.add(ASMHelper.pushIntConstant(retval));
        code.add(new InsnNode(Opcodes.IRETURN));

        clazz.methods.add(method);
    }

    /**
     * Generate a forwarding method of the form
     * "T name() { return this.forward(); }".
     *
     * @param clazz Class to generate new method on
     * @param name Name of method to generate
     * @param forwardname Name of method to call
     * @param rettype Return type of method
     */
    public static void generateSelfForwardingMethod(ClassNode clazz, String name, String forwardname, Type rettype) {
        MethodNode method =
                new MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, name, "()" + rettype.getDescriptor(), null, null);

        ASMHelper.populateSelfForwardingMethod(method, forwardname, rettype, Type.getObjectType(clazz.name));

        clazz.methods.add(method);
    }

    /**
     * Generate a forwarding method of the form
     * "static T name(S object) { return object.forward(); }".
     *
     * @param clazz Class to generate new method on
     * @param name Name of method to generate
     * @param forwardname Name of method to call
     * @param rettype Return type of method
     * @param argtype Argument type
     */
    public static void generateStaticForwardingMethod(ClassNode clazz, String name, String forwardname, Type rettype, Type argtype) {
        MethodNode method = new MethodNode(Opcodes.ASM5,
                                           Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, name,
                                           "()" + rettype.getDescriptor(), null, null);

        ASMHelper.populateSelfForwardingMethod(method, forwardname, rettype, argtype);

        clazz.methods.add(method);
    }

    /**
     * Generate a forwarding method of the form
     * "T name() { return Class.forward(this); }".
     *
     * @param clazz Class to generate new method on
     * @param name Name of method to generate
     * @param forwardname Name of method to call
     * @param rettype Return type of method
     * @param fowardtype Forward type
     */
    public static void generateForwardingToStaticMethod(ClassNode clazz, String name, String forwardname, Type rettype, Type fowardtype) {
        MethodNode method = new MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, name,
                                           "()" + rettype.getDescriptor(), null, null);

        ASMHelper.populateForwardingToStaticMethod(method, forwardname, rettype, Type.getObjectType(clazz.name), fowardtype);

        clazz.methods.add(method);
    }

    /**
     * Generate a forwarding method of the form
     * "T name() { return Class.forward(this); }".
     *
     * @param clazz Class to generate new method on
     * @param name Name of method to generate
     * @param forwardname Name of method to call
     * @param rettype Return type of method
     * @param fowardtype Forward type
     * @param thistype Type to treat 'this' as for overload searching purposes
     */
    public static void generateForwardingToStaticMethod(ClassNode clazz, String name, String forwardname, Type rettype, Type fowardtype,
                                                        Type thistype) {
        MethodNode
                method =
                new MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, name, "()" + rettype.getDescriptor(), null, null);

        ASMHelper.populateForwardingToStaticMethod(method, forwardname, rettype, thistype, fowardtype);

        clazz.methods.add(method);
    }

    /**
     * Replace a method's code with a forward to another method on itself
     * or the first argument of a static method, as the argument takes the
     * place of this.
     *
     * @param method Method to replace code of
     * @param forwardname Name of method to forward to
     * @param thistype Type of object method is being replaced on
     */
    public static void replaceSelfForwardingMethod(MethodNode method, String forwardname, Type thistype) {
        Type methodType = Type.getMethodType(method.desc);

        method.instructions.clear();

        ASMHelper.populateSelfForwardingMethod(method, forwardname, methodType.getReturnType(), thistype);
    }

    /**
     * Generate a forwarding method of the form
     * "T name(S object) { return object.forward(); }".
     *
     * @param clazz Class to generate new method on
     * @param name Name of method to generate
     * @param forwardname Name of method to call
     * @param rettype Return type of method
     * @param argtype Type of object to call method on
     */
    public static void generateForwardingMethod(ClassNode clazz, String name, String forwardname, Type rettype, Type argtype) {
        MethodNode
                method =
                new MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, name, "()" + rettype.getDescriptor(), null, null);

        ASMHelper.populateForwardingMethod(method, forwardname, rettype, argtype, Type.getObjectType(clazz.name));

        clazz.methods.add(method);
    }

    /**
     * Replace a method's code with a forward to an method on its first
     * argument.
     *
     * @param method Method to replace code of
     * @param forwardname Name of method to forward to
     * @param thistype Type of object method is being replaced on
     */
    public static void replaceForwardingMethod(MethodNode method, String forwardname, Type thistype) {
        Type methodType = Type.getMethodType(method.desc);

        method.instructions.clear();

        ASMHelper.populateForwardingMethod(method, forwardname, methodType.getReturnType(), methodType.getArgumentTypes()[0], thistype);
    }

    /**
     * Populate a forwarding method of the form
     * "T name() { return Class.forward(this); }".
     *
     * @param method Method to generate code for
     * @param forwardname Name of method to call
     * @param rettype Return type of method
     * @param thistype Type of object method is being generated on
     * @param forwardtype Type to forward method to
     */
    public static void populateForwardingToStaticMethod(MethodNode method, String forwardname, Type rettype, Type thistype, Type forwardtype) {
        InsnList code = method.instructions;

        code.add(new VarInsnNode(thistype.getOpcode(Opcodes.ILOAD), 0));
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, forwardtype.getInternalName(), forwardname, Type.getMethodDescriptor(rettype, thistype),
                                    false));
        code.add(new InsnNode(rettype.getOpcode(Opcodes.IRETURN)));
    }

    /**
     * Populate a forwarding method of the form
     * "T name() { return this.forward(); }". This is also valid for methods of
     * the form "static T name(S object) { return object.forward() }".
     *
     * @param method Method to generate code for
     * @param forwardname Name of method to call
     * @param rettype Return type of method
     * @param thistype Type of object method is being generated on
     */
    public static void populateSelfForwardingMethod(MethodNode method, String forwardname, Type rettype, Type thistype) {
        InsnList code = method.instructions;

        code.add(new VarInsnNode(thistype.getOpcode(Opcodes.ILOAD), 0));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, thistype.getInternalName(), forwardname, "()" + rettype.getDescriptor(), false));
        code.add(new InsnNode(rettype.getOpcode(Opcodes.IRETURN)));
    }

    /**
     * Populate a forwarding method of the form
     * "T name(S object) { return object.forward(); }".
     *
     * @param method Method to generate code for
     * @param forwardname Name of method to call
     * @param rettype Return type of method
     * @param argtype Type of object to call method on
     * @param thistype Type of object method is being generated on
     */
    public static void populateForwardingMethod(MethodNode method, String forwardname, Type rettype, Type argtype, Type thistype) {
        InsnList code = method.instructions;

        code.add(new VarInsnNode(argtype.getOpcode(Opcodes.ILOAD), 1));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, argtype.getInternalName(), forwardname, "()" + rettype.getDescriptor(), false));
        code.add(new InsnNode(rettype.getOpcode(Opcodes.IRETURN)));
    }

    /**
     * Gets an instruction that pushes a integer onto the stack.  The
     * instruction uses the smallest push possible (ICONST_*, BIPUSH, SIPUSH or
     * Integer constant).
     *
     * @param c the integer to push onto the stack
     * @return insn node to insert
     */
    public static AbstractInsnNode pushIntConstant(int c) {
        if (c == -1) {
            return new InsnNode(Opcodes.ICONST_M1);
        } else if (c >= 0 && c <= 5) {
            return new InsnNode(intConstants[c]);
        } else if (c >= Byte.MIN_VALUE && c <= Byte.MAX_VALUE) {
            return new IntInsnNode(Opcodes.BIPUSH, c);
        } else if (c >= Short.MIN_VALUE && c <= Short.MAX_VALUE) {
            return new IntInsnNode(Opcodes.SIPUSH, c);
        } else {
            return new LdcInsnNode(c);
        }
    }

    /**
     * Finds a method given the method descriptor
     *
     * @param clazz the class to scan
     * @param name the method name
     * @param desc the method descriptor
     * @return discovered method node or null
     */
    public static MethodNode findMethod(ClassNode clazz, String name, String desc) {
        Iterator<MethodNode> i = clazz.methods.iterator();
        while (i.hasNext()) {
            MethodNode m = i.next();
            if (m.name.equals(name) && m.desc.equals(desc)) {
                return m;
            }
        }
        return null;
    }

    /**
     * Adds a method to a class, overwriting any matching method.
     *
     * @param clazz the class to scan
     * @param method the method to add
     */
    public static void addAndReplaceMethod(ClassNode clazz, MethodNode method) {
        MethodNode m = ASMHelper.findMethod(clazz, method.name, method.desc);
        if (m != null) {
            clazz.methods.remove(m);
        }
        clazz.methods.add(method);
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
        System.err.printf("%s ", node.getClass().getSimpleName());
        if (node instanceof LabelNode) {
            System.err.printf("[%s]", ((LabelNode)node).getLabel());
        } else if (node instanceof JumpInsnNode) {
            System.err.printf("[%s] [%s]", ASMHelper.getOpcodeName(node), ((JumpInsnNode)node).label.getLabel());
        } else if (node instanceof VarInsnNode) {
            System.err.printf("[%s] %d", ASMHelper.getOpcodeName(node), ((VarInsnNode)node).var);
        } else if (node instanceof MethodInsnNode) {
            MethodInsnNode mth = (MethodInsnNode)node;
            System.err.printf("[%s] %s %s %s", ASMHelper.getOpcodeName(node), mth.owner, mth.name, mth.desc);
        } else if (node instanceof FieldInsnNode) {
            FieldInsnNode fld = (FieldInsnNode)node;
            System.err.printf("[%s] %s %s %s", ASMHelper.getOpcodeName(node), fld.owner, fld.name, fld.desc);
        } else if (node instanceof LineNumberNode) {
            LineNumberNode ln = (LineNumberNode)node;
            System.err.printf("LINE=%d LABEL=[%s]", ln.line, ln.start.getLabel());
        } else if (node instanceof LdcInsnNode) {
            System.err.print(((LdcInsnNode)node).cst);
        } else if (node instanceof IntInsnNode) {
            System.err.print(((IntInsnNode)node).operand);
        } else {
            System.err.printf("[%s] ", ASMHelper.getOpcodeName(node));
        }
        System.err.print("\n");
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
        if (opcode > 0) {
            boolean found = false;
            
            try {
                for (java.lang.reflect.Field f : Opcodes.class.getDeclaredFields()) {
                    if (!found && f.getName() != "UNINITIALIZED_THIS") {
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
        
        return opcode >= 0 ? String.valueOf(opcode) : "";
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

    private static AnnotationNode getSingleAnnotation(List<AnnotationNode> annotations, Class<? extends Annotation>... annotationClasses) {
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
     * @param annotationNode Annotation node to query
     * @param key Key to search for
     * @param enumClass Class of enum containing the enum constant to search for
     * @param defaultValue Value to return if the specified key isn't found
     * @return duck-typed annotation value or defaultValue if missing
     */
    public static <T extends Enum<T>> T getAnnotationValue(AnnotationNode annotationNode, String key, Class<T> enumClass, T defaultValue) {
        String[] value = ASMHelper.<String[]>getAnnotationValue(annotationNode, key);
        if (value == null) {
            return defaultValue;
        }
        if (!enumClass.getName().equals(Type.getType(value[0]).getClassName())) {
            throw new IllegalArgumentException("The supplied enum class does not match the stored enum value");
        }
        return Enum.valueOf(enumClass, value[1]);
    }

    public static boolean methodIsStatic(MethodNode method) {
        return (method.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
    }
    
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
}
