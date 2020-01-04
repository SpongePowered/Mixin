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
package org.spongepowered.asm.mixin.gen;

import java.util.ArrayList;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.util.asm.ASM;

/**
 * Base class for accessor generators
 */
public abstract class AccessorGenerator {

    /**
     * Accessor info which describes the accessor
     */
    protected final AccessorInfo info;

    /**
     * True for static field, false for instance field 
     */
    protected final boolean targetIsStatic;

    public AccessorGenerator(AccessorInfo info, boolean isStatic) {
        this.info = info;
        this.targetIsStatic = isStatic;
    }

    protected void checkModifiers() {
        if (this.info.isStatic() && !this.targetIsStatic) {
            IMixinContext context = this.info.getContext();
            throw new InvalidInjectionException(context, String.format("%s is invalid. Accessor method is%s static but the target is not.",
                    this.info, this.info.isStatic() ? "" : " not"));
        }
    }

    /**
     * Create an empty accessor method based on the source method
     * 
     * @param maxLocals max locals size for method
     * @param maxStack max stack size for method
     * @return new method
     */
    protected final MethodNode createMethod(int maxLocals, int maxStack) {
        MethodNode method = this.info.getMethod();
        MethodNode accessor = new MethodNode(ASM.API_VERSION, (method.access & ~Opcodes.ACC_ABSTRACT) | Opcodes.ACC_SYNTHETIC, method.name,
                method.desc, null, null);
        accessor.visibleAnnotations = new ArrayList<AnnotationNode>();
        accessor.visibleAnnotations.add(this.info.getAnnotation());
        accessor.maxLocals = maxLocals;
        accessor.maxStack = maxStack;
        return accessor;
    }
    
    /**
     * Perform pre-flight checks on the accessor, to ensure it can be generated
     * sanely 
     */
    public void validate() {
        // Stub for subclasses
    }

    /**
     * Generate the accessor method
     * 
     * @return generated accessor method
     */
    public abstract MethodNode generate();
    
}
