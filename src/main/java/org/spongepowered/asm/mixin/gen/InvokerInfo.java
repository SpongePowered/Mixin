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

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.gen.throwables.InvalidAccessorException;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorByName;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelector.Result;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.asm.ElementNode;

/**
 * Information about an invoker
 */
class InvokerInfo extends AccessorInfo {
    
    InvokerInfo(MixinTargetContext mixin, MethodNode method) {
        super(mixin, method, Invoker.class);
    }

    @Override
    protected AccessorType initType() {
        if (this.specifiedName != null) {
            String mappedReference = this.mixin.getReferenceMapper().remap(this.mixin.getClassRef(), this.specifiedName);
            return this.initType(mappedReference.replace('.',  '/'), this.mixin.getTargetClassRef());
        }
        
        AccessorName accessorName = AccessorName.of(this.method.name, false);
        if (accessorName != null) {
            for (String prefix : AccessorType.OBJECT_FACTORY.getExpectedPrefixes()) {
                if (prefix.equals(accessorName.prefix)) {
                    return this.initType(accessorName.name, this.mixin.getTargetClassInfo().getSimpleName());
                }
            }
        }
        
        return AccessorType.METHOD_PROXY;
    }
    
    private AccessorType initType(String targetName, String targetClassName) {
        if (Constants.CTOR.equals(targetName) || targetClassName.equals(targetName)) {
            if (!this.returnType.equals(this.mixin.getTargetClassInfo().getType())) {
                throw new InvalidAccessorException(this.mixin,
                        String.format("%s appears to have an invalid return type. %s requires matching return type. Found %s expected %s",
                        this, AccessorType.OBJECT_FACTORY, Bytecode.getSimpleName(this.returnType), this.mixin.getTargetClassInfo().getSimpleName()));
            }
            if (!this.isStatic) {
                throw new InvalidAccessorException(this.mixin, String.format("%s for %s must be static", this, AccessorType.OBJECT_FACTORY,
                        Bytecode.getSimpleName(this.returnType)));
            }
            
            return AccessorType.OBJECT_FACTORY;
        }
        return AccessorType.METHOD_PROXY;
    }

    @Override
    protected Type initTargetFieldType() {
        return null;
    }
    
    @Override
    protected ITargetSelector initTarget() {
        if (this.type == AccessorType.OBJECT_FACTORY) {
            return new MemberInfo(Constants.CTOR, null, Bytecode.changeDescriptorReturnType(this.method.desc, "V"));
        }
        
        return new MemberInfo(this.getTargetName(this.specifiedName), null, this.method.desc);
    }

    @Override
    public void locate() {
        this.targetMethod = this.findTargetMethod();
    }

    private MethodNode findTargetMethod() {
        Result<MethodNode> result = TargetSelector.<MethodNode>run(this.target.configure("orphan"), ElementNode.methodList(this.classNode));

        try {
            return result.getSingleResult(true);
        } catch (IllegalStateException ex) {
            String message = ex.getMessage() + " matching " + this.target + " in " + this.classNode.name + " for " + this;
            if (this.type == AccessorType.METHOD_PROXY && this.specifiedName != null && this.target instanceof ITargetSelectorByName) {
                String name = ((ITargetSelectorByName)this.target).getName();
                if (name != null && (name.contains(".") || name.contains("/"))) {
                    throw new InvalidAccessorException(this, "Invalid factory invoker failed to match the target class. " + message);
                }
            }
            throw new InvalidAccessorException(this, message);
        }
    }

}
