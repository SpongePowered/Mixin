/**
 * This file contributed from LiteLoader. Pending merge into ASMHelper. DO NOT ALTER THIS FILE.
 */

package org.spongepowered.asm.util;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SimpleVerifier;

//import com.mumfrey.liteloader.core.runtime.Obf;

/**
 * Utility methods for working with bytecode using ASM
 * 
 * @author Adam Mummery-Smith
 */
public abstract class ByteCodeUtilities
{
    private static Map<String, List<LocalVariableNode>> calculatedLocalVariables = new HashMap<String, List<LocalVariableNode>>();
    
    private ByteCodeUtilities() {}

//    /**
//     * Replace all constructor invokations for the target class in the supplied classNode with invokations of the replacement class
//     * 
//     * @param classNode Class to search in
//     * @param target Target type
//     * @param replacement Replacement type
//     */
//    public static void replaceConstructors(ClassNode classNode, Obf target, Obf replacement)
//    {
//        for (MethodNode method : classNode.methods)
//        {
//            ByteCodeUtilities.replaceConstructors(method, target, replacement);
//        }
//    }

//    /**
//     * Replace all constructor invokations for the target class in the supplied method with invokations of the replacement class
//     * 
//     * @param method Method to look in
//     * @param target Target type
//     * @param replacement Replacement type
//     */
//    public static void replaceConstructors(MethodNode method, Obf target, Obf replacement)
//    {
//        Iterator<AbstractInsnNode> iter = method.instructions.iterator();
//        while (iter.hasNext())
//        {
//            AbstractInsnNode insn = iter.next();
//            if (insn.getOpcode() == Opcodes.NEW)
//            {
//                TypeInsnNode typeInsn = (TypeInsnNode)insn;
//                if (target.obf.equals(typeInsn.desc) || target.ref.equals(typeInsn.desc))
//                {
//                    typeInsn.desc = replacement.ref;
//                }
//            }
//            else if (insn instanceof MethodInsnNode && insn.getOpcode() == Opcodes.INVOKESPECIAL)
//            {
//                MethodInsnNode methodInsn = (MethodInsnNode)insn;
//                if ((target.obf.equals(methodInsn.owner) || target.ref.equals(methodInsn.owner)) && "<init>".equals(methodInsn.name))
//                {
//                    methodInsn.owner = replacement.ref;
//                }
//            }
//        }
//    }

    /**
     * Injects appropriate LOAD opcodes into the supplied InsnList appropriate for each entry in the args array starting at pos
     * 
     * @param args Argument types
     * @param insns Instruction List to inject into
     * @param pos Start position
     */
    public static void loadArgs(Type[] args, InsnList insns, int pos)
    {
        ByteCodeUtilities.loadArgs(args, insns, pos, -1);
    }
        
    /**
     * Injects appropriate LOAD opcodes into the supplied InsnList appropriate for each entry in the args array starting at start and ending at end
     * 
     * @param args Argument types
     * @param insns Instruction List to inject into
     * @param start Start position
     * @param end End position
     */
    public static void loadArgs(Type[] args, InsnList insns, int start, int end)
    {
        int pos = start;
        
        for (Type type : args)
        {
            insns.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), pos));
            pos += type.getSize();
            if (end >= start && pos >= end) return;
        }
    }
    
    /**
     * Injects appropriate LOAD opcodes into the supplied InsnList for each entry in the supplied locals array starting at pos
     * 
     * @param locals Local types (can contain nulls for uninitialised, TOP, or RETURN values in locals)
     * @param insns Instruction List to inject into
     * @param pos Start position
     */
    public static void loadLocals(Type[] locals, InsnList insns, int pos)
    {
        for (; pos < locals.length; pos++)
        {
            if (locals[pos] != null)
            {
                insns.add(new VarInsnNode(locals[pos].getOpcode(Opcodes.ILOAD), pos));
            }
        }
    }
    
    /**
     * Get the first variable index in the supplied method which is not an argument or "this" reference, this corresponds
     * to the size of the arguments passed in to the method plus an extra spot for "this" if the method is non-static 
     * 
     * @param method MethodNode to inspect
     * @return first available local index which is NOT used by a method argument or "this"
     */
    public static int getFirstNonArgLocalIndex(MethodNode method)
    {
        return ByteCodeUtilities.getFirstNonArgLocalIndex(Type.getArgumentTypes(method.desc), (method.access & Opcodes.ACC_STATIC) == 0);
    }
    
    /**
     * Get the first non-arg variable index based on the supplied arg array and whether to include the "this" reference,
     * this corresponds to the size of the arguments passed in to the method plus an extra spot for "this" is specified 

     * @param args Method arguments
     * @param includeThis Whether to include a slot for "this" (generally true for all non-static methods)
     * @return first available local index which is NOT used by a method argument or "this"
     */
    public static int getFirstNonArgLocalIndex(Type[] args, boolean includeThis)
    {
        return ByteCodeUtilities.getArgsSize(args) + (includeThis ? 1 : 0);
    }
    
    /**
     * Get the size of the specified args array in local variable terms (eg. doubles and longs take two spaces)
     * 
     * @param args Method argument types as array
     * @return size of the specified arguments array in terms of stack slots
     */
    public static int getArgsSize(Type[] args)
    {
        int size = 0;
        
        for (Type type : args)
        {
            size += type.getSize();
        }
        
        return size;
    }

    /**
     * Attempts to identify available locals at an arbitrary point in the bytecode specified by node.
     * 
     * This method builds an approximate view of the locals available at an arbitrary point in the bytecode by examining the following
     * features in the bytecode:
     * 
     *  * Any available stack map frames
     *  * STORE opcodes
     *  * The local variable table
     *  
     * Inference proceeds by walking the bytecode from the start of the method looking for stack frames and STORE opcodes. When either
     * of these is encountered, an attempt is made to cross-reference the values in the stack map or STORE opcode with the value in the
     * local variable table which covers the code range. Stack map frames overwrite the entire simulated local variable table with their
     * own value types, STORE opcodes overwrite only the local slot to which they pertain. Values in the simulated locals array are spaced
     * according to their size (unlike the representation in FrameNode) and this TOP, NULL and UNINTITIALIZED_THIS opcodes will be
     * represented as null values in the simulated frame.
     * 
     * This code does not currently simulate the prescribed JVM behaviour where overwriting the second slot of a DOUBLE or LONG actually
     * invalidates the DOUBLE or LONG stored in the previous location, so we have to hope (for now) that this behaviour isn't emitted by
     * the compiler or any upstream transformers. I may have to re-think this strategy if this situation is encountered in the wild. 
     * 
     * @param classNode ClassNode containing the method, used to initialise the implicit "this" reference in simple methods with no stack frames
     * @param method MethodNode to explore
     * @param node Node indicating the position at which to determine the locals state. The locals will be enumerated UP TO the specified
     *     node, so bear in mind that if the specified node is itself a STORE opcode, then we will be looking at the state of the locals
     *     PRIOR to its invokation
     * @return A sparse array containing a view (hopefully) of the locals at the specified location
     */
    public static LocalVariableNode[] getLocalsAt(ClassNode classNode, MethodNode method, AbstractInsnNode node)
    {
        LocalVariableNode[] frame = new LocalVariableNode[method.maxLocals];
        
        // Initialise implicit "this" reference in non-static methods
        if ((method.access & Opcodes.ACC_STATIC) == 0)
        {
            frame[0] = new LocalVariableNode("this", classNode.name, null, null, null, 0);
        }
        
        for (Iterator<AbstractInsnNode> iter = method.instructions.iterator(); iter.hasNext();)
        {
            AbstractInsnNode insn = iter.next();
            if (insn instanceof FrameNode)
            {
                FrameNode frameNode = (FrameNode)insn;
                
                // localPos tracks the location in the frame node's locals list, which doesn't leave space for TOP entries
                for (int localPos = 0, framePos = 0; framePos < frame.length; framePos++, localPos++)
                {
                    // Get the local at the current position in the FrameNode's locals list
                    final Object localType = (localPos < frameNode.local.size()) ? frameNode.local.get(localPos) : null;
                    
                    if (localType instanceof String) // String refers to a reference type
                    {
                        frame[framePos] = ByteCodeUtilities.getLocalVariableAt(classNode, method, node, framePos);
                    }
                    else if (localType instanceof Integer) // Integer refers to a primitive type or other marker
                    {
                        boolean isMarkerType = localType == Opcodes.UNINITIALIZED_THIS || localType == Opcodes.TOP || localType == Opcodes.NULL;
                        boolean is32bitValue = localType == Opcodes.INTEGER || localType == Opcodes.FLOAT;
                        boolean is64bitValue = localType == Opcodes.DOUBLE || localType == Opcodes.LONG;
                        if (isMarkerType)
                        {
                            frame[framePos] = null;
                        }
                        else if (is32bitValue || is64bitValue)
                        {
                            frame[framePos] = ByteCodeUtilities.getLocalVariableAt(classNode, method, node, framePos);

                            if (is64bitValue)
                            {
                                framePos++;
                                frame[framePos] = null; // TOP
                            }
                        }
                        else
                        {
                            throw new RuntimeException("Unrecognised locals opcode " + localType + " in locals array at position " + localPos + " in " + classNode.name + "." + method.name + method.desc);
                        }
                    }
                    else if (localType == null)
                    {
                        frame[framePos] = null;
                    }
                    else
                    {
                        throw new RuntimeException("Invalid value " + localType + " in locals array at position " + localPos + " in " + classNode.name + "." + method.name + method.desc);
                    }
                }
            }
            else if (insn instanceof VarInsnNode)
            {
                VarInsnNode varNode = (VarInsnNode)insn;
                frame[varNode.var] = ByteCodeUtilities.getLocalVariableAt(classNode, method, node, varNode.var);
            }
            else if (insn == node)
            {
                break;
            }
        }
        
        return frame;
    }

    /**
     * Attempts to locate the appropriate entry in the local variable table for the specified local variable index at the location
     * specified by node.
     * 
     * @param classNode Containing class
     * @param method Method
     * @param node Instruction defining the location to get the local variable table at
     * @param var Local variable index
     * @return a LocalVariableNode containing information about the local variable at the specified location in the specified local slot
     */
    public static LocalVariableNode getLocalVariableAt(ClassNode classNode, MethodNode method, AbstractInsnNode node, int var)
    {
        LocalVariableNode localVariableNode = null;
        
        int pos = method.instructions.indexOf(node);

        List<LocalVariableNode> localVariables = ByteCodeUtilities.getLocalVariableTable(classNode, method);
        for (LocalVariableNode local : localVariables)
        {
            if (local.index != var) continue;
            int start = method.instructions.indexOf(local.start);
            int end = method.instructions.indexOf(local.end);
            if (localVariableNode == null || start < pos && end > pos)
            {
                localVariableNode = local;
            }
        }
        
        return localVariableNode;
    }

    /**
     * Fetches or generates the local variable table for the specified method. Since Mojang strip the local variable table
     * as part of the obfuscation process, we need to generate the local variable table when running obfuscated. We cache
     * the generated tables so that we only need to do the relatively expensive calculation once per method we encounter.
     * 
     * @param classNode Containing class
     * @param method Method
     */
    public static List<LocalVariableNode> getLocalVariableTable(ClassNode classNode, MethodNode method)
    {
        if (method.localVariables.isEmpty())
        {
            String signature = String.format("%s.%s%s", classNode.name, method.name, method.desc);
            
            List<LocalVariableNode> localVars = ByteCodeUtilities.calculatedLocalVariables.get(signature);
            if (localVars != null)
            {
                return localVars;
            }
            
            localVars = ByteCodeUtilities.generateLocalVariableTable(classNode, method);
            ByteCodeUtilities.calculatedLocalVariables.put(signature, localVars);
            return localVars;
        }
        
        return method.localVariables;
    }
    
    /**
     * Use ASM Analyzer to generate the local variable table for the specified method
     * 
     * @param classNode Containing class
     * @param method Method
     */
    public static List<LocalVariableNode> generateLocalVariableTable(ClassNode classNode, MethodNode method)
    {
        List<Type> interfaces = null;
        if (classNode.interfaces != null)
        {
            interfaces = new ArrayList<Type>();
            for (String iface : classNode.interfaces)
            {
                interfaces.add(Type.getObjectType(iface));
            }
        }
        
        Type objectType = null;
        if (classNode.superName != null)
        {
            objectType = Type.getObjectType(classNode.superName);
        }
        
        // Use Analyzer to generate the bytecode frames
        Analyzer<BasicValue> analyzer = new Analyzer<BasicValue>(new SimpleVerifier(Type.getObjectType(classNode.name), objectType, interfaces, false));
        try
        {
            analyzer.analyze(classNode.name, method);
        }
        catch (AnalyzerException ex)
        {
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
        
        // Traverse the frames and work out when locals begin and end
        for (int i = 0; i < methodSize; i++)
        {
            Frame<BasicValue> f = frames[i];
            if (f == null) continue;
            LabelNode label = null;
            
            for (int j = 0; j < f.getLocals(); j++)
            {
                BasicValue local = f.getLocal(j);
                if (local == null && locals[j] == null) continue;
                if (local != null && local.equals(locals[j])) continue;

                if (label == null)
                {
                    label = new LabelNode();
                    labels[i] = label;
                }
                
                if (local == null && locals[j] != null)
                {
                    localVariables.add(localNodes[j]);
                    localNodes[j].end = label;
                    localNodes[j] = null;
                }
                else if (local != null)
                {
                    if (locals[j] != null)
                    {
                        localVariables.add(localNodes[j]);
                        localNodes[j].end = label;
                        localNodes[j] = null;
                    }
                    
                    String desc = (local.getType() != null) ? local.getType().getDescriptor() : null;
                    localNodes[j] = new LocalVariableNode("var" + j, desc, null, label, null, j);
                }
                
                locals[j] = local;
            }
        }
        
        // Reached the end of the method so flush all current locals and mark the end
        LabelNode label = null;
        for (int k = 0; k < localNodes.length; k++)
        {
            if (localNodes[k] != null)
            {
                if (label == null)
                {
                    label = new LabelNode();
                    method.instructions.add(label);
                }
                
                localNodes[k].end = label;
                localVariables.add(localNodes[k]);
            }
        }

        // Insert generated labels into the method body
        for (int n = methodSize - 1; n >= 0; n--)
        {
            if (labels[n] != null)
            {
                method.instructions.insert(method.instructions.get(n), labels[n]);
            }
        }
        
        return localVariables;
    }

    /**
     * Get the source code name for the specified type
     * 
     * @param type Type to generate a friendly name for
     * @return String representation of the specified type, eg "int" for an integer primitive or "String" for java.lang.String
     */
    public static String getTypeName(Type type)
    {
        switch (type.getSort())
        {
            case Type.BOOLEAN: return "boolean";
            case Type.CHAR:    return "char";
            case Type.BYTE:    return "byte";
            case Type.SHORT:   return "short";
            case Type.INT:     return "int";
            case Type.FLOAT:   return "float";
            case Type.LONG:    return "long";
            case Type.DOUBLE:  return "double";
            case Type.ARRAY:   return ByteCodeUtilities.getTypeName(type.getElementType()) + "[]";
            case Type.OBJECT:
                String typeName = type.getClassName();
                typeName = typeName.substring(typeName.lastIndexOf('.') + 1);
                return typeName;
        }
        
        return "Object";
    }

//    /**
//     * Finds a method in the target class, uses names specified in the {@link Obfuscated} annotation if present
//     * 
//     * @param targetClass Class to search in
//     * @param searchFor Method to search for
//     */
//    public static MethodNode findTargetMethod(ClassNode targetClass, MethodNode searchFor)
//    {
//        for (MethodNode target : targetClass.methods)
//        {
//            if (target.name.equals(searchFor.name) && target.desc.equals(searchFor.desc))
//                return target;
//        }
//        
//        AnnotationNode obfuscatedAnnotation = ByteCodeUtilities.getVisibleAnnotation(searchFor, Obfuscated.class);
//        if (obfuscatedAnnotation != null)
//        {
//            for (String obfuscatedName : ByteCodeUtilities.<List<String>>getAnnotationValue(obfuscatedAnnotation))
//            {
//                for (MethodNode target : targetClass.methods)
//                {
//                    if (target.name.equals(obfuscatedName) && target.desc.equals(searchFor.desc))
//                        return target;
//                }
//            }
//        }
//        
//        return null;
//    }

//    /**
//     * Finds a field in the target class, uses names specified in the {@link Obfuscated} annotation if present
//     * 
//     * @param targetClass Class to search in
//     * @param searchFor Field to search for
//     */
//    public static FieldNode findTargetField(ClassNode targetClass, FieldNode searchFor)
//    {
//        for (FieldNode target : targetClass.fields)
//        {
//            if (target.name.equals(searchFor.name))
//                return target;
//        }
//        
//        AnnotationNode obfuscatedAnnotation = ByteCodeUtilities.getVisibleAnnotation(searchFor, Obfuscated.class);
//        if (obfuscatedAnnotation != null)
//        {
//            for (String obfuscatedName : ByteCodeUtilities.<List<String>>getAnnotationValue(obfuscatedAnnotation))
//            {
//                for (FieldNode target : targetClass.fields)
//                {
//                    if (target.name.equals(obfuscatedName))
//                        return target;
//                }
//            }
//        }
//            
//        return null;
//    }

//    /**
//     * Find a method in the target class which matches the specified method name and descriptor
//     * 
//     * @param classNode
//     * @param searchFor
//     * @param desc
//     */
//    public static MethodNode findMethod(ClassNode classNode, Obf searchFor, String desc)
//    {
//        int ordinal = 0;
//        
//        for (MethodNode method : classNode.methods)
//        {
//            if (searchFor.matches(method.name, ordinal++) && method.desc.equals(desc))
//                return method;
//        }
//        
//        return null;
//    }

//    /**
//     * Find a field in the target class which matches the specified field name
//     * 
//     * @param classNode
//     * @param searchFor
//     */
//    public static FieldNode findField(ClassNode classNode, Obf searchFor)
//    {
//        int ordinal = 0;
//        
//        for (FieldNode field : classNode.fields)
//        {
//            if (searchFor.matches(field.name, ordinal++))
//                return field;
//        }
//        
//        return null;
//    }

    public static ClassNode loadClass(String className) throws IOException
    {
        return ByteCodeUtilities.loadClass(className, true, null);
    }
    
    public static ClassNode loadClass(String className, boolean runTransformers) throws IOException
    {
        return ByteCodeUtilities.loadClass(className, runTransformers, null);
    }
    
    public static ClassNode loadClass(String className, IClassTransformer source) throws IOException
    {
        return ByteCodeUtilities.loadClass(className, source != null, source);
    }
    
    public static ClassNode loadClass(String className, boolean runTransformers, IClassTransformer source) throws IOException
    {
        byte[] bytes = Launch.classLoader.getClassBytes(className);

        if (runTransformers)
        {
            bytes = ByteCodeUtilities.applyTransformers(className, bytes, source);
        }
        
        return ByteCodeUtilities.readClass(bytes);
    }
    
    public static ClassNode readClass(byte[] basicClass)
    {
        ClassReader classReader = new ClassReader(basicClass);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
        return classNode;
    }

    public static byte[] applyTransformers(String className, byte[] basicClass)
    {
        return ByteCodeUtilities.applyTransformers(className, basicClass, null);
    }
    
    public static byte[] applyTransformers(String className, byte[] basicClass, IClassTransformer source)
    {
        final List<IClassTransformer> transformers = Launch.classLoader.getTransformers();
        
        for (final IClassTransformer transformer : transformers)
        {
            if (transformer != source)
            {
                basicClass = transformer.transform(className, className, basicClass);
            }
        }
        
        return basicClass;
    }

    /**
     * Get an annotation of the specified class from the supplied field node
     */
    public static AnnotationNode getVisibleAnnotation(FieldNode field, Class<? extends Annotation> annotationClass)
    {
        return ByteCodeUtilities.getAnnotation(field.visibleAnnotations, Type.getDescriptor(annotationClass));
    }
    
    /**
     * Get an annotation of the specified class from the supplied field node
     */
    public static AnnotationNode getInvisibleAnnotation(FieldNode field, Class<? extends Annotation> annotationClass)
    {
        return ByteCodeUtilities.getAnnotation(field.invisibleAnnotations, Type.getDescriptor(annotationClass));
    }

    /**
     * Get an annotation of the specified class from the supplied method node
     */
    public static AnnotationNode getVisibleAnnotation(MethodNode method, Class<? extends Annotation> annotationClass)
    {
        return ByteCodeUtilities.getAnnotation(method.visibleAnnotations, Type.getDescriptor(annotationClass));
    }
    
    /**
     * Get an annotation of the specified class from the supplied method node
     */
    public static AnnotationNode getInvisibleAnnotation(MethodNode method, Class<? extends Annotation> annotationClass)
    {
        return ByteCodeUtilities.getAnnotation(method.invisibleAnnotations, Type.getDescriptor(annotationClass));
    }

    /**
     * Get an annotation of the specified class from the supplied class node
     */
    public static AnnotationNode getVisibleAnnotation(ClassNode classNode, Class<? extends Annotation> annotationClass)
    {
        return ByteCodeUtilities.getAnnotation(classNode.visibleAnnotations, Type.getDescriptor(annotationClass));
    }
    
    /**
     * Get an annotation of the specified class from the supplied class node
     */
    public static AnnotationNode getInvisibleAnnotation(ClassNode classNode, Class<? extends Annotation> annotationClass)
    {
        return ByteCodeUtilities.getAnnotation(classNode.invisibleAnnotations, Type.getDescriptor(annotationClass));
    }
    
    /**
     * Get an annotation of the specified class from the supplied list of annotations, returns null if no matching annotation was found
     */
    public static AnnotationNode getAnnotation(List<AnnotationNode> annotations, String annotationType)
    {
        if (annotations != null)
        {
            for (AnnotationNode annotation : annotations)
            {
                if (annotationType.equals(annotation.desc))
                    return annotation;
            }
        }
        
        return null;
    }

    /**
     * Get the value of an annotation node (the value at key "value")
     * 
     * @param annotation Annotation node to inspect
     */
    public static <T> T getAnnotationValue(AnnotationNode annotation)
    {
        return ByteCodeUtilities.getAnnotationValue(annotation, "value");
    }

    /**
     * Get the value of an annotation node
     * 
     * @param annotation
     * @param key
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAnnotationValue(AnnotationNode annotation, String key)
    {
        if (annotation == null || annotation.values == null)
            return null;
        
        boolean getNextValue = false;
        for (Object value : annotation.values)
        {
            if (getNextValue) return (T)value;
            if (value.equals(key)) getNextValue = true;
        }
        return null;
    }
    
//    /**
//     * @param returnType
//     * @param args
//     */
//    public static String generateDescriptor(Type returnType, Object... args)
//    {
//        return ByteCodeUtilities.generateDescriptor(Obf.MCP, returnType, args);
//    }
//    
//    /**
//     * @param returnType
//     * @param args
//     */
//    public static String generateDescriptor(Obf returnType, Object... args)
//    {
//        return ByteCodeUtilities.generateDescriptor(Obf.MCP, returnType, args);
//    }
//    
//    /**
//     * @param returnType
//     * @param args
//     */
//    public static String generateDescriptor(String returnType, Object... args)
//    {
//        return ByteCodeUtilities.generateDescriptor(Obf.MCP, returnType, args);
//    }
    
//    /**
//     * @param obfType
//     * @param returnType
//     * @param args
//     */
//    public static String generateDescriptor(int obfType, Object returnType, Object... args)
//    {
//        StringBuilder sb = new StringBuilder().append('(');;
//
//        for (Object arg : args)
//        {
//            sb.append(ByteCodeUtilities.toDescriptor(obfType, arg));
//        }
//        
//        return sb.append(')').append(returnType != null ? ByteCodeUtilities.toDescriptor(obfType, returnType) : "V").toString();
//    }

//    /**
//     * @param obfType
//     * @param arg
//     */
//    private static String toDescriptor(int obfType, Object arg)
//    {
//        if (arg instanceof Obf)
//        {
//            return ((Obf)arg).getDescriptor(obfType);
//        }
//        else if (arg instanceof String)
//        {
//            return (String)arg;
//        }
//        else if (arg instanceof Type)
//        {
//            return arg.toString();
//        }
//        else if (arg instanceof Class)
//        {
//            return Type.getDescriptor((Class<?>)arg).toString();
//        }
//        
//        return arg == null ? "" : arg.toString();
//    }
}
