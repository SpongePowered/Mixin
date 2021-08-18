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
package org.spongepowered.asm.mixin.injection.invoke.arg;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.logging.ILogger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.transformer.SyntheticClassInfo;
import org.spongepowered.asm.mixin.transformer.ext.IClassGenerator;
import org.spongepowered.asm.service.ISyntheticClassInfo;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.IConsumer;
import org.spongepowered.asm.util.SignaturePrinter;
import org.spongepowered.asm.util.asm.MethodVisitorEx;

/**
 * Class generator which creates subclasses of {@link Args} to be used by the
 * {@link ModifyArgs} injector. The subclasses contain getter and setter logic
 * to provide access to a particular configuration of arguments and classes are
 * only generated for each unique argument combination.
 */
public final class ArgsClassGenerator implements IClassGenerator {
    
    public static final String ARGS_NAME = Args.class.getName();
    public static final String ARGS_REF = ArgsClassGenerator.ARGS_NAME.replace('.', '/');
    
    public static final String GETTER_PREFIX = "$";

    private static final String CLASS_NAME_BASE = Constants.SYNTHETIC_PACKAGE + ".args.Args$";

    private static final String OBJECT = "java/lang/Object";
    private static final String OBJECT_ARRAY = "[L" + ArgsClassGenerator.OBJECT + ";";
    
    private static final String VALUES_FIELD = "values";
    
    private static final String CTOR_DESC = "(" + ArgsClassGenerator.OBJECT_ARRAY + ")V";
    
    private static final String SET = "set";
    private static final String SET_DESC = "(ILjava/lang/Object;)V";
    
    private static final String SETALL = "setAll";
    private static final String SETALL_DESC = "([Ljava/lang/Object;)V";

    private static final String NPE = "java/lang/NullPointerException";
    private static final String NPE_CTOR_DESC = "(Ljava/lang/String;)V";
    
    private static final String AIOOBE = "org/spongepowered/asm/mixin/injection/invoke/arg/ArgumentIndexOutOfBoundsException";
    private static final String AIOOBE_CTOR_DESC = "(I)V";
    
    private static final String ACE = "org/spongepowered/asm/mixin/injection/invoke/arg/ArgumentCountException";
    private static final String ACE_CTOR_DESC = "(IILjava/lang/String;)V";
    
    /**
     * Logger
     */
    private static final ILogger logger = MixinService.getService().getLogger("mixin");

    /**
     * Synthetic class info for args class
     */
    class ArgsClassInfo extends SyntheticClassInfo {
        
        final String desc;

        final Type[] args;
        
        int loaded = 0;

        ArgsClassInfo(IMixinInfo mixin, String name, String desc) {
            super(mixin, name);
            this.desc = desc;
            this.args = Type.getArgumentTypes(desc);
        }

        @Override
        public boolean isLoaded() {
            return this.loaded > 0;
        }
        
        String getSignature() {
            return new SignaturePrinter("", null, this.args).setFullyQualified(true).getFormattedArgs();
        }
    }
    
    /**
     * Synthetic class registry 
     */
    private final IConsumer<ISyntheticClassInfo> registry;

    /**
     * The next subclass number, classes generated in sequence eg.
     * <tt>Args$1</tt>, <tt>Args$2</tt>, etc. 
     */
    private int nextIndex = 1;
    
    /**
     * Map of descriptors to generated class infos
     */
    private final Map<String, ArgsClassInfo> descToClass = new HashMap<String, ArgsClassInfo>();
    
    /**
     * Map of class names to generated class infos
     */
    private final Map<String, ArgsClassInfo> nameToClass = new HashMap<String, ArgsClassInfo>();
    
    /**
     * Ctor
     * 
     * @param registry sythetic class registry
     */
    public ArgsClassGenerator(IConsumer<ISyntheticClassInfo> registry) {
        this.registry = registry;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.ext.IClassGenerator
     *      #getName()
     */
    @Override
    public String getName() {
        return "args";
    }

    /**
     * Get (or generate) the class name for the specified descriptor. The class
     * will not be generated until it is used. Calling this method simply
     * allocates a name for the specified descriptor.
     * 
     * @param desc Descriptor of the <em>target</em> method, the return type is
     *      ignored for the purposes of generating Args subclasses
     * @param mixin Mixin which requires the class. Only the first mixin to
     *      request a class will be registered against it but this is for
     *      debugging only anyway
     * @return name of the Args subclass to use
     */
    public ISyntheticClassInfo getArgsClass(String desc, IMixinInfo mixin) {
        String voidDesc = Bytecode.changeDescriptorReturnType(desc, "V");
        ArgsClassInfo info = this.descToClass.get(voidDesc);
        if (info == null) {
            String name = String.format("%s%d", ArgsClassGenerator.CLASS_NAME_BASE, this.nextIndex++);
            ArgsClassGenerator.logger.debug("ArgsClassGenerator assigning {} for descriptor {}", name, voidDesc);
            info = new ArgsClassInfo(mixin, name, voidDesc);
            this.descToClass.put(voidDesc, info);
            this.nameToClass.put(name, info);
            this.registry.accept(info);
        }
        return info;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.ext.IClassGenerator
     *      #generate(java.lang.String, org.objectweb.asm.tree.ClassNode)
     */
    @Override
    public boolean generate(String name, ClassNode classNode) {
        ArgsClassInfo info = this.nameToClass.get(name);
        if (info == null) {
            return false;
        }
        
        if (info.loaded > 0) {
            ArgsClassGenerator.logger.debug("ArgsClassGenerator is re-generating {}, already did this {} times!", name, info.loaded);
        }
        
        ClassVisitor visitor = classNode;
        if (MixinEnvironment.getCurrentEnvironment().getOption(Option.DEBUG_VERIFY)) {
            visitor = new CheckClassAdapter(classNode);
        }
        
        visitor.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER | Opcodes.ACC_SYNTHETIC, info.getName(), null,
                ArgsClassGenerator.ARGS_REF, null);
        visitor.visitSource(name.substring(name.lastIndexOf('.') + 1) + ".java", null);
        
        this.generateCtor(info, visitor);
        this.generateToString(info, visitor);
        this.generateFactory(info, visitor);
        this.generateSetters(info, visitor);
        this.generateGetters(info, visitor);
        
        visitor.visitEnd();
        info.loaded++;
        
        return true;
    }

    /**
     * Generate the constructor for the subclass, the ctor simply calls the
     * superclass ctor and does nothing else besides
     * 
     * @param ref Class ref being generated
     * @param desc Argument descriptor
     * @param args Parsed argument list from descriptor
     * @param writer Class writer
     */
    private void generateCtor(ArgsClassInfo info, ClassVisitor writer) {
        MethodVisitor ctor = writer.visitMethod(Opcodes.ACC_PRIVATE, Constants.CTOR, ArgsClassGenerator.CTOR_DESC, null, null);
        ctor.visitCode();
        ctor.visitVarInsn(Opcodes.ALOAD, 0);
        ctor.visitVarInsn(Opcodes.ALOAD, 1);
        ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, ArgsClassGenerator.ARGS_REF, Constants.CTOR, ArgsClassGenerator.CTOR_DESC, false);
        ctor.visitInsn(Opcodes.RETURN);
        ctor.visitMaxs(2, 2);
        ctor.visitEnd();
    }

    /**
     * Generate a toString method for this Args class.
     * 
     * @param ref Class ref being generated
     * @param desc Argument descriptor
     * @param args Parsed argument list from descriptor
     * @param writer Class writer
     */
    private void generateToString(ArgsClassInfo info, ClassVisitor writer) {
        MethodVisitor toString = writer.visitMethod(Opcodes.ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
        toString.visitCode();
        toString.visitLdcInsn("Args" + info.getSignature());
        toString.visitInsn(Opcodes.ARETURN);
        toString.visitMaxs(1, 1);
        toString.visitEnd();
    }
    
    /**
     * Generate the factory method (<tt>of</tt>) for the subclass, the factory
     * method takes the arguments which would have been passed to the target
     * method, marshals them into an <tt>Object[]</tt> array, and then calls the
     * constructor.
     * 
     * @param ref Class ref being generated
     * @param desc Argument descriptor
     * @param args Parsed argument list from descriptor
     * @param writer Class writer
     */
    private void generateFactory(ArgsClassInfo info, ClassVisitor writer) {
        String ref = info.getName();
        String factoryDesc = Bytecode.changeDescriptorReturnType(info.desc, "L" + ref + ";");
        MethodVisitorEx of = new MethodVisitorEx(writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "of", factoryDesc, null, null));
        of.visitCode();
        
        // Create args subclass
        of.visitTypeInsn(Opcodes.NEW, ref);
        of.visitInsn(Opcodes.DUP);
        
        // Create Object[] array to contain args
        of.visitConstant((byte)info.args.length);
        of.visitTypeInsn(Opcodes.ANEWARRAY, ArgsClassGenerator.OBJECT);

        // Iterate over args and stuff them into the array
        for (byte index = 0, argIndex = 0; index < info.args.length; index++) {
            Type arg = info.args[index];
            of.visitInsn(Opcodes.DUP);
            of.visitConstant(index);
            of.visitVarInsn(arg.getOpcode(Opcodes.ILOAD), argIndex);
            ArgsClassGenerator.box(of, arg);
            of.visitInsn(Opcodes.AASTORE);
            argIndex += arg.getSize();
        }

        // Call the constructor passing in the generated array
        of.visitMethodInsn(Opcodes.INVOKESPECIAL, ref, Constants.CTOR, ArgsClassGenerator.CTOR_DESC, false);
        
        // Return the new object
        of.visitInsn(Opcodes.ARETURN);

        of.visitMaxs(6, Bytecode.getArgsSize(info.args));
        of.visitEnd();
    }

    /**
     * Generate the getter method for each arguments. These getters are not
     * available from consumer code, but instead are called by the injector to
     * retrieve each argument in turn for passing to the method invocation being
     * modified.
     * 
     * @param ref Class ref being generated
     * @param desc Argument descriptor
     * @param args Parsed argument list from descriptor
     * @param writer Class writer
     */
    private void generateGetters(ArgsClassInfo info, ClassVisitor writer) {
        byte argIndex = 0;
        for (Type arg : info.args) {
            String name = ArgsClassGenerator.GETTER_PREFIX + argIndex;
            String sig = "()" + arg.getDescriptor();
            MethodVisitorEx get = new MethodVisitorEx(writer.visitMethod(Opcodes.ACC_PUBLIC, name, sig, null, null));
            get.visitCode();
            
            // Read the value from the values field
            get.visitVarInsn(Opcodes.ALOAD, 0);
            get.visitFieldInsn(Opcodes.GETFIELD, info.getName(), ArgsClassGenerator.VALUES_FIELD, ArgsClassGenerator.OBJECT_ARRAY);
            get.visitConstant(argIndex);
            get.visitInsn(Opcodes.AALOAD);
            
            // Unbox (if primitive) or cast down the value
            ArgsClassGenerator.unbox(get, arg);
            
            // Return the value
            get.visitInsn(arg.getOpcode(Opcodes.IRETURN));
            
            get.visitMaxs(2, 1);
            get.visitEnd();
            argIndex++;
        }
    }
    
    /**
     * Generate the setter methods. These methods implement the abstract
     * {@link Args#set} and {@link Args#setAll} methods. 
     * 
     * @param ref Class ref being generated
     * @param desc Argument descriptor
     * @param args Parsed argument list from descriptor
     * @param writer Class writer
     */
    private void generateSetters(ArgsClassInfo info, ClassVisitor writer) {
        this.generateIndexedSetter(info, writer);
        this.generateMultiSetter(info, writer);
    }
    
    /**
     * Generate the <tt>set</tt> method body. The <tt>set</tt> method performs a
     * <tt>CHECKCAST</tt> on all incoming arguments, checks that the argument
     * index is not out of bounds, and also ensures that primitive types are not
     * assigned <tt>null</tt> by the consumer code. 
     * 
     * @param ref Class ref being generated
     * @param desc Argument descriptor
     * @param args Parsed argument list from descriptor
     * @param writer Class writer
     */
    private void generateIndexedSetter(ArgsClassInfo info, ClassVisitor writer) {
        MethodVisitorEx set = new MethodVisitorEx(writer.visitMethod(Opcodes.ACC_PUBLIC,
                ArgsClassGenerator.SET, ArgsClassGenerator.SET_DESC, null, null));
        set.visitCode();
        
        Label store = new Label(), checkNull = new Label();
        Label[] labels = new Label[info.args.length];
        for (int label = 0; label < labels.length; label++) {
            labels[label] = new Label();
        }
        
        // Put the values array on the stack to begin with
        set.visitVarInsn(Opcodes.ALOAD, 0);
        set.visitFieldInsn(Opcodes.GETFIELD, info.getName(), ArgsClassGenerator.VALUES_FIELD, ArgsClassGenerator.OBJECT_ARRAY);

        // Each argument index will jump to its own label
        for (byte index = 0; index < info.args.length; index++) {
            set.visitVarInsn(Opcodes.ILOAD, 1);
            set.visitConstant(index);
            set.visitJumpInsn(Opcodes.IF_ICMPEQ, labels[index]);
        }
        
        // No argument was matched, so we throw an out of bounds exception
        ArgsClassGenerator.throwAIOOBE(set, 1);
        
        // For each arg we do a CHECKCAST to ensure the supplied type is
        // assignable to the arg type, we leave the index and value on the stack
        // and jump to the next stage
        for (int index = 0; index < info.args.length; index++) {
            String boxingType = Bytecode.getBoxingType(info.args[index]);
            set.visitLabel(labels[index]);
            set.visitVarInsn(Opcodes.ILOAD, 1);
            set.visitVarInsn(Opcodes.ALOAD, 2);
            set.visitTypeInsn(Opcodes.CHECKCAST, boxingType != null ? boxingType : info.args[index].getInternalName());
            set.visitJumpInsn(Opcodes.GOTO, boxingType != null ? checkNull : store);
        }
        
        // For primitive types, we check that the supplied value is not null
        set.visitLabel(checkNull);
        set.visitInsn(Opcodes.DUP);
        set.visitJumpInsn(Opcodes.IFNONNULL, store);
        
        // If the arg type is primitive but the user supplied NULL, throw an exception
        ArgsClassGenerator.throwNPE(set, "Argument with primitive type cannot be set to NULL");
        
        // Everything above succeeded, so we just assign the value into the array
        set.visitLabel(store);
        set.visitInsn(Opcodes.AASTORE);
        set.visitInsn(Opcodes.RETURN);
        set.visitMaxs(6, 3);
        set.visitEnd();
    }
    
    /**
     * Generate the varargs <tt>set</tt> method body. The <tt>set</tt> method
     * performs a <tt>CHECKCAST</tt> on all incoming arguments, and also ensures
     * that primitive types are not assigned <tt>null</tt> by the consumer code.
     * 
     * @param ref Class ref being generated
     * @param desc Argument descriptor
     * @param args Parsed argument list from descriptor
     * @param writer Class writer
     */
    private void generateMultiSetter(ArgsClassInfo info, ClassVisitor writer) {
        MethodVisitorEx set = new MethodVisitorEx(writer.visitMethod(Opcodes.ACC_PUBLIC,
                ArgsClassGenerator.SETALL, ArgsClassGenerator.SETALL_DESC, null, null));
        set.visitCode();
        
        Label lengthOk = new Label(), nullPrimitive = new Label();
        int maxStack = 6;
        
        // Compare the length of the varargs array to the expected argument count
        set.visitVarInsn(Opcodes.ALOAD, 1);
        set.visitInsn(Opcodes.ARRAYLENGTH);
        set.visitInsn(Opcodes.DUP);
        set.visitConstant((byte)info.args.length);
        
        // If the lengths are the same, proceed with assignment
        set.visitJumpInsn(Opcodes.IF_ICMPEQ, lengthOk);
        
        // Otherwise prepare and throw an ArgumentCountException
        set.visitTypeInsn(Opcodes.NEW, ArgsClassGenerator.ACE);
        set.visitInsn(Opcodes.DUP);
        set.visitInsn(Opcodes.DUP2_X1);
        set.visitInsn(Opcodes.POP2);
        set.visitConstant((byte)info.args.length);
        set.visitLdcInsn(info.getSignature());

        set.visitMethodInsn(Opcodes.INVOKESPECIAL, ArgsClassGenerator.ACE, Constants.CTOR, ArgsClassGenerator.ACE_CTOR_DESC, false);
        set.visitInsn(Opcodes.ATHROW);
        
        set.visitLabel(lengthOk);
        set.visitInsn(Opcodes.POP); // Pop the remaining length value
        
        // Put the values array on the stack to begin with
        set.visitVarInsn(Opcodes.ALOAD, 0);
        set.visitFieldInsn(Opcodes.GETFIELD, info.getName(), ArgsClassGenerator.VALUES_FIELD, ArgsClassGenerator.OBJECT_ARRAY);

        for (byte index = 0; index < info.args.length; index++) {
            // Dup the member array reference and target index
            set.visitInsn(Opcodes.DUP);
            set.visitConstant(index);
            
            // Read the value from the varargs array
            set.visitVarInsn(Opcodes.ALOAD, 1);
            set.visitConstant(index);
            set.visitInsn(Opcodes.AALOAD);
            
            // Check the argument type
            String boxingType = Bytecode.getBoxingType(info.args[index]);
            set.visitTypeInsn(Opcodes.CHECKCAST, boxingType != null ? boxingType : info.args[index].getInternalName());
            
            // For primitives, check the value is not null
            if (boxingType != null) {
                set.visitInsn(Opcodes.DUP);
                set.visitJumpInsn(Opcodes.IFNULL, nullPrimitive);
                maxStack = 7;
            }
     
            // Everything succeeded, assign the value
            set.visitInsn(Opcodes.AASTORE);
        }

        set.visitInsn(Opcodes.RETURN);
        
        set.visitLabel(nullPrimitive);
        ArgsClassGenerator.throwNPE(set, "Argument with primitive type cannot be set to NULL");
        set.visitInsn(Opcodes.RETURN);
        
        set.visitMaxs(maxStack, 2);
        set.visitEnd();
    }

    /**
     * Add insns to throw a null pointer exception with the specified message
     */
    private static void throwNPE(MethodVisitorEx method, String message) {
        method.visitTypeInsn(Opcodes.NEW, ArgsClassGenerator.NPE);
        method.visitInsn(Opcodes.DUP);
        method.visitLdcInsn(message);
        method.visitMethodInsn(Opcodes.INVOKESPECIAL, ArgsClassGenerator.NPE, Constants.CTOR, ArgsClassGenerator.NPE_CTOR_DESC, false);
        method.visitInsn(Opcodes.ATHROW);
    }

    /**
     * Add insns to throw an {@link ArgumentIndexOutOfBoundsException}, reads
     * the arg index from the local var specified by <tt>arg</tt>
     */
    private static void throwAIOOBE(MethodVisitorEx method, int arg) {
        method.visitTypeInsn(Opcodes.NEW, ArgsClassGenerator.AIOOBE);
        method.visitInsn(Opcodes.DUP);
        method.visitVarInsn(Opcodes.ILOAD, arg);
        method.visitMethodInsn(Opcodes.INVOKESPECIAL, ArgsClassGenerator.AIOOBE, Constants.CTOR, ArgsClassGenerator.AIOOBE_CTOR_DESC, false);
        method.visitInsn(Opcodes.ATHROW);
    }

    /**
     * Box (if necessary) the supplied primitive type. Does not affect
     * reference types.
     * 
     * @param method method visitor
     * @param var type to box
     */
    private static void box(MethodVisitor method, Type var) {
        String boxingType = Bytecode.getBoxingType(var);
        if (boxingType != null) {
            String desc = String.format("(%s)L%s;", var.getDescriptor(), boxingType);
            method.visitMethodInsn(Opcodes.INVOKESTATIC, boxingType, "valueOf", desc, false);
        }
    }
    
    /**
     * Unbox (if necessary, otherwise just <tt>CHECKCAST</tt>) the supplied type
     * 
     * @param method method visitor
     * @param var type to unbox
     */
    private static void unbox(MethodVisitor method, Type var) {
        String boxingType = Bytecode.getBoxingType(var);
        if (boxingType != null) {
            String unboxingMethod = Bytecode.getUnboxingMethod(var);
            String desc = "()" + var.getDescriptor();
            method.visitTypeInsn(Opcodes.CHECKCAST, boxingType);
            method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, boxingType, unboxingMethod, desc, false);
        } else {
            method.visitTypeInsn(Opcodes.CHECKCAST, var.getInternalName());
        }
    }
    
}
