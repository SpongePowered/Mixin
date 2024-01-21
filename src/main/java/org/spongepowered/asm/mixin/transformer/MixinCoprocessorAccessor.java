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
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
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
import org.spongepowered.asm.util.LanguageFeatures;
import org.spongepowered.asm.util.Bytecode.Visibility;

/**
 * This coprocessor handles transformations to accessor mixins themselves as
 * they are classloaded. Besides ensuring that the interface is public, its
 * primary responsibility is to transform the decorated accessor methods into
 * proxies for the target members.
 */
class MixinCoprocessorAccessor extends MixinCoprocessor {

    /**
     * Transformer session ID
     */
    protected final String sessionId;

    /**
     * Accessor mixins
     */
    private final Map<String, MixinInfo> accessorMixins = new HashMap<String, MixinInfo>();
    
    MixinCoprocessorAccessor(String sessionId) {
        this.sessionId = sessionId;
    }
    
    @Override
    String getName() {
        return "accessor";
    }

    @Override
    public void onPrepare(MixinInfo mixin) {
        if (mixin.isAccessor()) {
            this.registerAccessor(mixin);
        }
    }

    void registerAccessor(MixinInfo mixin) {
        this.accessorMixins.put(mixin.getClassName(), mixin);
    }

    @Override
    ProcessResult process(String className, ClassNode classNode) {
        if (!MixinEnvironment.getCompatibilityLevel().supports(LanguageFeatures.METHODS_IN_INTERFACES)
                    || !this.accessorMixins.containsKey(className)) {
            return ProcessResult.NONE;
        }
        
        MixinInfo mixin = this.accessorMixins.get(className);
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
                MixinCoprocessorAccessor.createProxy(methodNode, targetClass, method);
                Annotations.setVisible(methodNode, MixinProxy.class, "sessionId", this.sessionId);
                classNode.methods.add(methodNode);
                transformed = true;
            }
        }
        
        if (!transformed) {
            return ProcessResult.NONE;
        }
        
        Bytecode.replace(mixinClassNode, classNode);
        return ProcessResult.PASSTHROUGH_TRANSFORMED;
    }

    private Method getAccessorMethod(MixinInfo mixin, MethodNode methodNode, ClassInfo targetClass) throws MixinTransformerError {
        Method method = mixin.getClassInfo().findMethod(methodNode, ClassInfo.INCLUDE_ALL);
        
        // Normally the target will be renamed when the mixin is conformed to the target, if we get here
        // without this happening then we will end up invoking an undecorated method, which is bad!
        if (!method.isConformed()) {
            String uniqueName = targetClass.getMethodMapper().getUniqueName(mixin, methodNode, this.sessionId, true);
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
        methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, targetClass.getName(), method.getName(), methodNode.desc,
                targetClass.isInterface()));
        methodNode.instructions.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));
        methodNode.maxStack = Bytecode.getFirstNonArgLocalIndex(args, false);
        methodNode.maxLocals = 0;
    }

}
