/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered.org <http://www.spongepowered.org>
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
package org.spongepowered.asm.mixin.injection.invoke;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.injection.InvalidInjectionException;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.ASMHelper;

import com.google.common.collect.ObjectArrays;

/**
 * <p>A bytecode injector which allows a method call to be redirected to the annotated handler method. The handler method signature must match the
 * hooked method precisely <b>but</b> prepended with an arg of the owning object's type to accept the object instance the method was going to be
 * invoked on. For example when hooking the following call:</p>
 * 
 * <blockquote><pre>
 *   int abc = 0;
 *   int def = 1;
 *   Foo someObject = new Foo();
 *   
 *   // Hooking this method
 *   boolean xyz = someObject.bar(abc, def);</pre>
 * </blockquote>
 * 
 * <p>The signature of the redirected method should be:</p>
 * 
 * <blockquote><pre>public boolean barProxy(Foo someObject, int abc, int def)</pre></blockquote>
 * 
 * <p>For obvious reasons this does not apply for static methods, for static methods it is sufficient that the signature simply match the hooked
 * method.</p> 
 */
public class RedirectInjector extends InvokeInjector {

    /**
     * @param info Injection info
     */
    public RedirectInjector(InjectionInfo info) {
        super(info, "@Redirect");
    }
    
    /**
     * Do the injection
     */
    @Override
    protected void inject(Target target, MethodInsnNode node) {
        boolean targetIsStatic = node.getOpcode() == Opcodes.INVOKESTATIC;
        Type ownerType = Type.getType("L" + node.owner + ";");
        Type returnType = Type.getReturnType(node.desc);
        Type[] args = Type.getArgumentTypes(node.desc);
        Type[] stackVars = targetIsStatic ? args : ObjectArrays.concat(ownerType, args);
        
        String desc = Injector.printArgs(stackVars) + returnType;
        if (!desc.equals(this.methodNode.desc)) {
            throw new InvalidInjectionException("@Redirect handler method has an invalid signature "
                    + ", expected " + desc + " found " + this.methodNode.desc);
        }
        
        InsnList insns = new InsnList();
        int extraLocals = ASMHelper.getArgsSize(stackVars) + 1;
        int[] argMap = this.storeArgs(target, stackVars, insns, 0);
        this.invokeHandlerWithArgs(stackVars, insns, argMap, 0, stackVars.length);
        
        target.method.instructions.insertBefore(node, insns);
        target.method.instructions.remove(node);
        target.method.maxLocals = Math.max(target.method.maxLocals, target.maxLocals + extraLocals);
        target.method.maxStack = Math.max(target.method.maxStack, target.maxStack + 1);
    }
}
