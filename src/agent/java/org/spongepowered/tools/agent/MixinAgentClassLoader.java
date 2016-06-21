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
package org.spongepowered.tools.agent;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.ClassWriter;
import org.spongepowered.asm.lib.MethodVisitor;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.util.Constants;

/**
 * Class loader that is used to load fake mixin classes so that they can be
 * re-defined.
 */
class MixinAgentClassLoader extends ClassLoader {

    private static final Logger logger = LogManager.getLogger("mixin.agent");

    /**
     * Mapping of mixin mixin classes to their fake classes
     */
    private Map<Class<?>, byte[]> mixins = new HashMap<Class<?>, byte[]>();

    /**
     * Mapping that keep track of bytecode for classes that are targeted by
     * mixins
     */
    private Map<String, byte[]> targets = new HashMap<String, byte[]>();

    /**
     * Add a fake mixin class
     *
     * @param name Name of the fake class
     */
    void addMixinClass(String name) {
        MixinAgentClassLoader.logger.debug("Mixin class {} added to class loader", name);
        try {
            byte[] bytes = this.materialise(name);
            Class<?> clazz = this.defineClass(name, bytes, 0, bytes.length);
            // apparently the class needs to be instantiated at least once
            // to be including in list returned by allClasses() method in jdi api
            clazz.newInstance();
            this.mixins.put(clazz, bytes);
        } catch (Throwable e) {
            MixinAgentClassLoader.logger.catching(e);
        }
    }

    /**
     * Registers the bytecode for a class targeted by a mixin
     *
     * @param name Name of the target clas
     * @param bytecode Bytecode of the target class
     */
    void addTargetClass(String name, byte[] bytecode) {
        this.targets.put(name, bytecode);
    }

    /**
     * Gets the bytecode for a fake mixin class
     *
     * @param clazz Mixin class
     * @return Bytecode of the fake mixin class
     */
    byte[] getFakeMixinBytecode(Class<?> clazz) {
        return this.mixins.get(clazz);
    }

    /**
     * Gets the original bytecode for a target class
     *
     * @param name Name of the target class
     * @return Original bytecode
     */
    byte[] getOriginalTargetBytecode(String name) {
        return this.targets.get(name);
    }

    /**
     * Generates the simplest possible class that is instantiable
     *
     * @param name Name of the generated class
     * @return Bytecode of the generated class
     */
    private byte[] materialise(String name) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(MixinEnvironment.getCompatibilityLevel().classVersion(), Opcodes.ACC_PUBLIC, name.replace('.', '/'), null,
                Type.getInternalName(Object.class), null);

        // create init method
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, Constants.CTOR, "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(Object.class), Constants.CTOR, "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
