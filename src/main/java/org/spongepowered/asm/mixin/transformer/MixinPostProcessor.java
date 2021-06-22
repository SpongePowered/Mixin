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
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Method;
import org.spongepowered.asm.mixin.transformer.MixinInfo.MixinClassNode;
import org.spongepowered.asm.mixin.transformer.MixinInfo.MixinMethodNode;
import org.spongepowered.asm.mixin.transformer.meta.MixinProxy;
import org.spongepowered.asm.mixin.transformer.throwables.MixinTransformerError;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Bytecode.Visibility;
import org.spongepowered.asm.util.LanguageFeatures;

/**
 * Performs post-processing tasks required for certain classes which pass
 * through the mixin transformer, such as transforming synthetic inner classes
 * and populating accessor methods in accessor mixins.
 */
class MixinPostProcessor implements MixinConfig.IListener {

    /**
     * Transformer session ID
     */
    private final String sessionId;
    
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
    
    MixinPostProcessor(String sessionId) {
        this.sessionId = sessionId;
    }

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
    boolean canProcess(String className) {
        return this.syntheticInnerClasses.contains(className) || this.loadable.contains(className);
    }
    
    public boolean processClass(String name, ClassNode classNode) {

        if (this.syntheticInnerClasses.contains(name)) {
            this.processSyntheticInner(classNode);
            return true;
        }
        
        if (this.accessorMixins.containsKey(name)) {
            MixinInfo mixin = this.accessorMixins.get(name);
            return this.processAccessor(classNode, mixin);
        }
        
        return false;
    }

    /**
     * "Pass through" a synthetic inner class. Transforms package-private
     * members in the class into public so that they are accessible from their
     * new home in the target class
     */
    private void processSyntheticInner(ClassNode classNode) {
        classNode.access |= Opcodes.ACC_PUBLIC;
        
        for (FieldNode field : classNode.fields) {
            if ((field.access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) == 0) {
                field.access |= Opcodes.ACC_PUBLIC;
            }
        }

        for (MethodNode method : classNode.methods) {
            if ((method.access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) == 0) {
                method.access |= Opcodes.ACC_PUBLIC;
            }
        }
    }

    private boolean processAccessor(ClassNode classNode, MixinInfo mixin) {
        if (!MixinEnvironment.getCompatibilityLevel().supports(LanguageFeatures.METHODS_IN_INTERFACES)) {
            return false;
        }
        
        boolean transformed = false;
        MixinClassNode mixinClassNode = mixin.getClassNode(0);
        ClassInfo targetClass = mixin.getTargets().get(0);
        
        if (!Bytecode.hasFlag(mixinClassNode, Opcodes.ACC_PUBLIC)) {
            Bytecode.setVisibility(mixinClassNode, Visibility.PUBLIC);
            transformed = true;
        }
        
        for (MixinMethodNode methodNode : mixinClassNode.mixinMethods) {
            if (!Bytecode.hasFlag(methodNode, Opcodes.ACC_STATIC)) {
                continue;
            }
            
            AnnotationNode accessor = methodNode.getVisibleAnnotation(Accessor.class);
            AnnotationNode invoker = methodNode.getVisibleAnnotation(Invoker.class);
            if (accessor != null || invoker != null) {
                Method method = this.getAccessorMethod(mixin, methodNode, targetClass);
                MixinPostProcessor.createProxy(methodNode, targetClass, method);
                Annotations.setVisible(methodNode, MixinProxy.class, "sessionId", this.sessionId);
                classNode.methods.add(methodNode);
                transformed = true;
            }
        }
        
        if (!transformed) {
            return false;
        }
        
        Bytecode.replace(mixinClassNode, classNode);
        return true;
    }

    private Method getAccessorMethod(MixinInfo mixin, MethodNode methodNode, ClassInfo targetClass) throws MixinTransformerError {
        Method method = mixin.getClassInfo().findMethod(methodNode, ClassInfo.INCLUDE_ALL);
        
        // Normally the target will be renamed when the mixin is conformed to the target, if we get here
        // without this happening then we will end up invoking an undecorated method, which is bad!
        if (!method.isConformed()) {
            String uniqueName = targetClass.getMethodMapper().getUniqueName(methodNode, this.sessionId, true);
            method.conform(uniqueName);
        }
        
        return method;
    }

    private static void createProxy(MethodNode methodNode, ClassInfo targetClass, Method method) {
        methodNode.access |= Opcodes.ACC_SYNTHETIC;
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
