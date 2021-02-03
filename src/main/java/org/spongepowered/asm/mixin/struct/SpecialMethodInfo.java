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
package org.spongepowered.asm.mixin.struct;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.asm.MethodNodeEx;

/**
 * Information about a special mixin method such as an injector or accessor
 */
public class SpecialMethodInfo extends AnnotatedMethodInfo {
    
    /**
     * Human-readable annotation type 
     */
    protected final String annotationType;
    
    /**
     * Class
     */
    protected final ClassNode classNode;
    
    /**
     * Original name of the method, if available 
     */
    protected final String methodName;

    /**
     * Mixin data
     */
    protected final MixinTargetContext mixin;
    
    public SpecialMethodInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation);
        this.mixin = mixin;
        this.annotationType = this.annotation != null ? "@" + Annotations.getSimpleName(this.annotation) : "Undecorated injector";
        this.classNode = mixin.getTargetClassNode();
        this.methodName = MethodNodeEx.getName(method);
    }

    /**
     * Get the class node for this injection
     * 
     * @return the class containing the injector and the target
     */
    public final ClassNode getClassNode() {
        return this.classNode;
    }
    
    /**
     * Get the class metadata for the mixin
     */
    public final ClassInfo getClassInfo() {
        return this.mixin.getClassInfo();
    }
    
    /**
     * Get the original name of the method, if available
     */
    @Override
    public String getMethodName() {
        return this.methodName;
    }

}
