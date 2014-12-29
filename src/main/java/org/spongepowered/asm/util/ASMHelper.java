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

import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;

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

        code.add(pushIntConstant(retval ? 1 : 0));
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

        code.add(pushIntConstant(retval));
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

        populateSelfForwardingMethod(method, forwardname, rettype, Type.getObjectType(clazz.name));

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
     */
    public static void generateStaticForwardingMethod(ClassNode clazz, String name, String forwardname, Type rettype, Type argtype) {
        MethodNode method = new MethodNode(Opcodes.ASM5,
                                           Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, name,
                                           "()" + rettype.getDescriptor(), null, null);

        populateSelfForwardingMethod(method, forwardname, rettype, argtype);

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
     */
    public static void generateForwardingToStaticMethod(ClassNode clazz, String name, String forwardname, Type rettype, Type fowardtype) {
        MethodNode method = new MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, name,
                                           "()" + rettype.getDescriptor(), null, null);

        populateForwardingToStaticMethod(method, forwardname, rettype, Type.getObjectType(clazz.name), fowardtype);

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
     * @param thistype Type to treat 'this' as for overload searching purposes
     */
    public static void generateForwardingToStaticMethod(ClassNode clazz, String name, String forwardname, Type rettype, Type fowardtype,
                                                        Type thistype) {
        MethodNode
                method =
                new MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, name, "()" + rettype.getDescriptor(), null, null);

        populateForwardingToStaticMethod(method, forwardname, rettype, thistype, fowardtype);

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

        populateSelfForwardingMethod(method, forwardname, methodType.getReturnType(), thistype);
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

        populateForwardingMethod(method, forwardname, rettype, argtype, Type.getObjectType(clazz.name));

        clazz.methods.add(method);
    }

    /**
     * Replace a method's code with a forward to an method on its first argument.
     *
     * @param method Method to replace code of
     * @param forwardname Name of method to forward to
     * @param thistype Type of object method is being replaced on
     */
    public static void replaceForwardingMethod(MethodNode method, String forwardname, Type thistype) {
        Type methodType = Type.getMethodType(method.desc);

        method.instructions.clear();

        populateForwardingMethod(method, forwardname, methodType.getReturnType(), methodType.getArgumentTypes()[0], thistype);
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
     * Gets an instruction that pushes a integer onto the stack.  The instruction uses
     * the smallest push possible (ICONST_*, BIPUSH, SIPUSH or Integer constant).
     *
     * @param c the integer to push onto the stack
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
        MethodNode m = findMethod(clazz, method.name, method.desc);
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
        dumpClass(cw.toByteArray());
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
     * Get a runtime-visible annotation of the specified class from the supplied field node
     *
     * @param field Source field
     * @param annotationClass Type of annotation to search for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getVisibleAnnotation(FieldNode field, Class<? extends Annotation> annotationClass) {
        return ASMHelper.getAnnotation(field.visibleAnnotations, Type.getDescriptor(annotationClass));
    }

    /**
     * Get an invisible annotation of the specified class from the supplied field node
     *
     * @param field Source field
     * @param annotationClass Type of annotation to search for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getInvisibleAnnotation(FieldNode field, Class<? extends Annotation> annotationClass) {
        return ASMHelper.getAnnotation(field.invisibleAnnotations, Type.getDescriptor(annotationClass));
    }

    /**
     * Get a runtime-visible annotation of the specified class from the supplied method node
     *
     * @param method Source method
     * @param annotationClass Type of annotation to search for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getVisibleAnnotation(MethodNode method, Class<? extends Annotation> annotationClass) {
        return ASMHelper.getAnnotation(method.visibleAnnotations, Type.getDescriptor(annotationClass));
    }

    /**
     * Get an invisible annotation of the specified class from the supplied method node
     *
     * @param method Source method
     * @param annotationClass Type of annotation to search for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getInvisibleAnnotation(MethodNode method, Class<? extends Annotation> annotationClass) {
        return ASMHelper.getAnnotation(method.invisibleAnnotations, Type.getDescriptor(annotationClass));
    }

    /**
     * Get a runtime-visible annotation of the specified class from the supplied class node
     *
     * @param classNode Source classNode
     * @param annotationClass Type of annotation to search for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getVisibleAnnotation(ClassNode classNode, Class<? extends Annotation> annotationClass) {
        return ASMHelper.getAnnotation(classNode.visibleAnnotations, Type.getDescriptor(annotationClass));
    }

    /**
     * Get an invisible annotation of the specified class from the supplied class node
     *
     * @param classNode Source classNode
     * @param annotationClass Type of annotation to search for
     * @return the annotation, or null if not present
     */
    public static AnnotationNode getInvisibleAnnotation(ClassNode classNode, Class<? extends Annotation> annotationClass) {
        return ASMHelper.getAnnotation(classNode.invisibleAnnotations, Type.getDescriptor(annotationClass));
    }

    /**
     * Search for and return an annotation node matching the specified type within the supplied
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

    /**
     * Duck type the "value" entry (if any) of the specified annotation node
     *
     * @param annotation Annotation node to query
     * @return duck-typed annotation value, null if missing, or inevitable ClassCastException if your duck is actually a rooster 
     */
    public static <T> T getAnnotationValue(AnnotationNode annotation) {
        return ASMHelper.getAnnotationValue(annotation, "value");
    }

    /**
     * Get the value of an annotation node and do pseudo-duck-typing via Java's crappy generics
     *
     * @param annotation Annotation node to query
     * @param key Key to search for
     * @param defaultValue Value to return if the specified key is not found or is null
     * @return duck-typed annotation value, null if missing, or inevitable ClassCastException if your duck is actually a rooster 
     */
    public static <T> T getAnnotationValue(AnnotationNode annotation, String key, T defaultValue) {
        T returnValue = ASMHelper.getAnnotationValue(annotation, key);
        return returnValue != null ? returnValue : defaultValue;
    }
    
    /**
     * Get the value of an annotation node and do pseudo-duck-typing via Java's crappy generics
     *
     * @param annotation Annotation node to query
     * @param key Key to search for
     * @return duck-typed annotation value, null if missing, or inevitable ClassCastException if your duck is actually a rooster 
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAnnotationValue(AnnotationNode annotation, String key) {
        boolean getNextValue = false;

        if (annotation.values == null) {
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
     * Get the enum constant referenced by an annotation node
     *
     * @param annotationNode Annotation node to query
     * @param key Key to search for
     * @param enumClass Class of enum containing the enum constant to search for
     * @param defaultValue Value to return if the specified key isn't found
     * @return
     * @throws NoSuchFieldException
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAnnotationEnumConstantValue(AnnotationNode annotationNode, String key, Class enumClass, T defaultValue) {
        String[] value = ASMHelper.getAnnotationValue(annotationNode, key);
        try {
            Field enumField = enumClass.getField(value[1]);
            if (enumField.isEnumConstant()) {
                return (T) enumField;
            }
        } catch (NoSuchFieldException e) {
        }
        return defaultValue;
    }

    public static boolean methodIsStatic(MethodNode method) {
        return (method.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
    }
    
    public static boolean fieldIsStatic(FieldNode field) {
        return (field.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
    }
    
    /**
     * Get the first variable index in the supplied method which is not an argument or "this" reference, this corresponds to the size of the arguments
     * passed in to the method plus an extra spot for "this" if the method is non-static
     * 
     * @param method MethodNode to inspect
     * @return first available local index which is NOT used by a method argument or "this"
     */
    public static int getFirstNonArgLocalIndex(MethodNode method) {
        return ASMHelper.getFirstNonArgLocalIndex(Type.getArgumentTypes(method.desc), (method.access & Opcodes.ACC_STATIC) == 0);
    }

    /**
     * Get the first non-arg variable index based on the supplied arg array and whether to include the "this" reference, this corresponds to the size
     * of the arguments passed in to the method plus an extra spot for "this" is specified
     * 
     * @param args Method arguments
     * @param includeThis Whether to include a slot for "this" (generally true for all non-static methods)
     * @return first available local index which is NOT used by a method argument or "this"
     */
    public static int getFirstNonArgLocalIndex(Type[] args, boolean includeThis) {
        return ASMHelper.getArgsSize(args) + (includeThis ? 1 : 0);
    }

    /**
     * Get the size of the specified args array in local variable terms (eg. doubles and longs take two spaces)
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
     * Injects appropriate LOAD opcodes into the supplied InsnList appropriate for each entry in the args array starting at pos
     * 
     * @param args Argument types
     * @param insns Instruction List to inject into
     * @param pos Start position
     */
    public static void loadArgs(Type[] args, InsnList insns, int pos) {
        ASMHelper.loadArgs(args, insns, pos, -1);
    }

    /**
     * Injects appropriate LOAD opcodes into the supplied InsnList appropriate for each entry in the args array starting at start and ending at end
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
