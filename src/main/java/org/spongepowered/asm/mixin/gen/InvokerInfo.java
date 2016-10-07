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

import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

/**
 * Information about an invoker
 */
public class InvokerInfo extends AccessorInfo {

    public InvokerInfo(MixinTargetContext mixin, MethodNode method) {
        super(mixin, method, Invoker.class);
    }

    @Override
    protected AccessorType initType() {
        return AccessorType.METHOD_PROXY;
    }
    
    @Override
    protected Type initTargetFieldType() {
        return null;
    }
    
    @Override
    protected MemberInfo initTarget() {
        return new MemberInfo(this.getTargetName(), null, this.method.desc);
    }
    
    @Override
    public void locate() {
        this.targetMethod = this.findTargetMethod();
    }

    private MethodNode findTargetMethod() {
        return this.<MethodNode>findTarget(this.classNode.methods);
    }

}
