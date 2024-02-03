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
package org.spongepowered.asm.mixin.injection.code;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.spongepowered.asm.mixin.injection.struct.Constructor;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.transformer.struct.Initialiser;
import org.spongepowered.asm.mixin.transformer.struct.Initialiser.InjectionMode;
import org.spongepowered.asm.util.Bytecode.DelegateInitialiser;
import org.spongepowered.asm.util.Constants;

/**
 * InsnList with extensions, see {@link IInsnListEx}
 */
public class InsnListEx extends InsnListReadOnly implements IInsnListEx {
    
    /**
     * The target method
     */
    private final Target target;

    public InsnListEx(Target target) {
        super(target.insns);
        this.target = target;
    }
    
    @Override
    public String toString() {
        return this.target.toString();
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.code.IInsnListEx
     *      #getTargetName()
     */
    @Override
    public String getTargetName() {
        return this.target.getName();
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.code.IInsnListEx
     *      #getTargetDesc()
     */
    @Override
    public String getTargetDesc() {
        return this.target.getDesc();
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.code.IInsnListEx
     *      #getTargetSignature()
     */
    @Override
    public String getTargetSignature() {
        return this.target.getSignature();
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.code.IInsnListEx
     *      #getTargetAccess()
     */
    @Override
    public int getTargetAccess() {
        return this.target.method.access;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.code.IInsnListEx
     *      #isTargetStatic()
     */
    @Override
    public boolean isTargetStatic() {
        return this.target.isStatic;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.code.IInsnListEx
     *      #isTargetConstructor()
     */
    @Override
    public boolean isTargetConstructor() {
        return this.target instanceof Constructor;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.code.IInsnListEx
     *      #isTargetStaticInitialiser()
     */
    @Override
    public boolean isTargetStaticInitialiser() {
        return Constants.CLINIT.equals(this.target.getName());
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.code.IInsnListEx
     *      #getSpecialNode
     */
    @Override
    public AbstractInsnNode getSpecialNode(SpecialNodeType type) {
        switch (type) {
            case DELEGATE_CTOR:
                if (this.target instanceof Constructor) {
                    DelegateInitialiser superCall = ((Constructor)this.target).findDelegateInitNode();
                    if (superCall.isPresent && this.contains(superCall.insn)) {
                        return superCall.insn;
                    }
                }
                return null;

            case INITIALISER_INJECTION_POINT:
                if (this.target instanceof Constructor) {
                    // mode is always DEFAULT because we want to locate initialisers if possible
                    InjectionMode mode = Initialiser.InjectionMode.DEFAULT;
                    AbstractInsnNode initialiserInjectionPoint = ((Constructor)this.target).findInitialiserInjectionPoint(mode);
                    if (this.contains(initialiserInjectionPoint)) {
                        return initialiserInjectionPoint;
                    }
                }
                return null;

            case CTOR_BODY:
                if (this.target instanceof Constructor) {
                    AbstractInsnNode beforeBody = ((Constructor)this.target).findFirstBodyInsn();
                    if (this.contains(beforeBody)) {
                        return beforeBody;
                    }
                }
                return null;
            default:
                return null;
        }
    }

}
