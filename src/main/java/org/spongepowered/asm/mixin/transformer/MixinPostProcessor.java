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
package org.spongepowered.asm.mixin.transformer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.ClassVisitor;
import org.spongepowered.asm.lib.ClassWriter;
import org.spongepowered.asm.lib.FieldVisitor;
import org.spongepowered.asm.lib.MethodVisitor;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.InsnNode;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.CompatibilityLevel;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Method;
import org.spongepowered.asm.mixin.transformer.MixinInfo.MixinClassNode;
import org.spongepowered.asm.mixin.transformer.MixinInfo.MixinMethodNode;
import org.spongepowered.asm.mixin.transformer.throwables.MixinTransformerError;
import org.spongepowered.asm.transformers.MixinClassWriter;
import org.spongepowered.asm.transformers.TreeTransformer;
import org.spongepowered.asm.util.Bytecode;

/**
 * Performs post-processing tasks required for certain classes which pass
 * through the mixin transformer, such as transforming synthetic inner classes
 * and populating accessor methods in accessor mixins.
 */
class MixinPostProcessor extends TreeTransformer implements MixinConfig.IListener {
    
    /**
     * Synthetic inner classes in mixins
     */
    private final Set<String> syntheticInnerClasses = new HashSet<String>();
    
    /**
     * Accessor mixins
     */
    private final Map<String, MixinInfo> accessorMixins = new HashMap<String, MixinInfo>();
    
    /**
     * Loadable classes within mixin packages
     */
    private final Set<String> loadable = new HashSet<String>();
    
    @Override
    public void onInit(MixinInfo mixin) {
        for (String innerClass : mixin.getSyntheticInnerClasses()) {
            this.registerSyntheticInner(innerClass.replace('/', '.'));
        }
    }

    @Override
    public void onPrepare(MixinInfo mixin) {
        String className = mixin.getClassName();
        
        if (mixin.isLoadable()) {
            this.registerLoadable(className);
        }
        
        if (mixin.isAccessor()) {
            this.registerAccessor(mixin);
        }
    }

    void registerSyntheticInner(String className) {
        this.syntheticInnerClasses.add(className);
    }

    void registerLoadable(String className) {
        this.loadable.add(className);
    }

    void registerAccessor(MixinInfo mixin) {
        this.registerLoadable(mixin.getClassName());
        this.accessorMixins.put(mixin.getClassName(), mixin);
    }

    /**
     * Get whether the specified class can be piped through the postprocessor
     * rather than being classloaded normally
     * 
     * @param className Class name to check
     * @return True if the specified class name has been registered for post-
     *      processing
     */
    boolean canTransform(String className) {
        return this.syntheticInnerClasses.contains(className) || this.loadable.contains(className);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.ILegacyClassTransformer#getName()
     */
    @Override
    public String getName() {
        return this.getClass().getName();
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.ILegacyClassTransformer
     *      #isDelegationExcluded()
     */
    @Override
    public boolean isDelegationExcluded() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.ILegacyClassTransformer
     *      #transformClassBytes(java.lang.String, java.lang.String, byte[])
     */
    @Override
    public byte[] transformClassBytes(String name, String transformedName, byte[] bytes) {
        if (this.syntheticInnerClasses.contains(transformedName)) {
            return this.processSyntheticInner(bytes);
        }
        
        if (this.accessorMixins.containsKey(transformedName)) {
            MixinInfo mixin = this.accessorMixins.get(transformedName);
            return this.processAccessor(bytes, mixin);
        }
        
        return bytes;
    }

    /**
     * "Pass through" a synthetic inner class. Transforms package-private
     * members in the class into public so that they are accessible from their
     * new home in the target class
     */
    private byte[] processSyntheticInner(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = new MixinClassWriter(cr, 0);

        ClassVisitor visibilityVisitor = new ClassVisitor(Opcodes.ASM5, cw) {
            
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access | Opcodes.ACC_PUBLIC, name, signature, superName, interfaces);
            }
            
            
            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                if ((access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) == 0) {
                    access |= Opcodes.ACC_PUBLIC;
                }
                return super.visitField(access, name, desc, signature, value);
            }
            
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                if ((access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) == 0) {
                    access |= Opcodes.ACC_PUBLIC;
                }
                return super.visitMethod(access, name, desc, signature, exceptions);
            }
            
        };
        
        cr.accept(visibilityVisitor, ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }

    private byte[] processAccessor(byte[] bytes, MixinInfo mixin) {
        if (!MixinEnvironment.getCompatibilityLevel().isAtLeast(CompatibilityLevel.JAVA_8)) {
            return bytes;
        }
        
        boolean transformed = false;
        MixinClassNode classNode = mixin.getClassNode(0);
        ClassInfo targetClass = mixin.getTargets().get(0);
        
        for (Iterator<MixinMethodNode> iter = classNode.mixinMethods.iterator(); iter.hasNext();) {
            MixinMethodNode methodNode = iter.next();
            if (!Bytecode.hasFlag(methodNode, Opcodes.ACC_STATIC)) {
                continue;
            }
            
            AnnotationNode accessor = methodNode.getVisibleAnnotation(Accessor.class);
            AnnotationNode invoker = methodNode.getVisibleAnnotation(Invoker.class);
            if (accessor != null || invoker != null) {
                Method method = MixinPostProcessor.getAccessorMethod(mixin, methodNode, targetClass);
                MixinPostProcessor.createProxy(methodNode, targetClass, method);
                transformed = true;
            }
        }
        
        if (transformed) {
            return this.writeClass(classNode);
        }
        
        return bytes;
    }

    private static Method getAccessorMethod(MixinInfo mixin, MethodNode methodNode, ClassInfo targetClass) throws MixinTransformerError {
        Method method = mixin.getClassInfo().findMethod(methodNode, ClassInfo.INCLUDE_ALL);
        
        // Normally the target will be renamed when the mixin is conformed to the target, if we get here
        // without this happening then we will end up invoking an undecorated method, which is bad!
        if (!method.isRenamed()) {
            throw new MixinTransformerError("Unexpected state: " + mixin + " loaded before " + targetClass + " was conformed");
        }
        
        return method;
    }

    private static void createProxy(MethodNode methodNode, ClassInfo targetClass, Method method) {
        methodNode.instructions.clear();
        Type[] args = Type.getArgumentTypes(methodNode.desc);
        Type returnType = Type.getReturnType(methodNode.desc);
        Bytecode.loadArgs(args, methodNode.instructions, 0);
        methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, targetClass.getName(), method.getName(), methodNode.desc, false));
        methodNode.instructions.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));
        methodNode.maxStack = Bytecode.getFirstNonArgLocalIndex(args, false);
        methodNode.maxLocals = 0;
    }

}
