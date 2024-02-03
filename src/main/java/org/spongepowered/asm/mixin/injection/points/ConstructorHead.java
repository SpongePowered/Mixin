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
package org.spongepowered.asm.mixin.injection.points;

import java.util.Collection;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.injection.InjectionPoint.AtCode;
import org.spongepowered.asm.mixin.injection.code.IInsnListEx;
import org.spongepowered.asm.mixin.injection.code.IInsnListEx.SpecialNodeType;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionPointException;
import org.spongepowered.asm.service.MixinService;

/**
 * <p>Like {@link MethodHead HEAD}, this injection point can be used to specify
 * the first instruction in a method, but provides special handling for
 * constructors. For regular method, the behaviour is identical to <tt>HEAD</tt>
 * .</p>
 * 
 * <p>By default, this injection point attempts to select the first instruction
 * after any initialisers (including initialisers merged by mixins) but will
 * fall back to selecting the first instruction after the delegate call (eg. a
 * call to <tt>super()</tt> or <tt>this()</tt>) in the case where heuristic
 * detection of the initialisers fails. This behaviour can be overridden by
 * providing the <tt>enforce</tt> parameter to enforce selection of a specific
 * location.</p>
 * 
 * <dl>
 *   <dt>enforce=POST_DELEGATE</dt>
 *   <dd>Select the instruction immediately after the delegate constructor</dd>
 *   <dt>enforce=POST_MIXIN</dt>
 *   <dd>Select the instruction immediately after all mixin-initialised field
 *     initialisers, this is similar to POST_DELEGATE if no applied mixins have
 *     initialisers for target class fields, except that the injection point
 *     will be after any mixin-supplied initialisers.</dd>
 *   <dt>enforce=PRE_BODY</dt>
 *   <dd>Selects the first instruction in the target method body, as determined
 *     by the line numbers. If the target method does not have line numbers
 *     available, the result is equivalent to POST_DELEGATE.</dd>
 * </dl>
 * 
 * <p>Example default behaviour:</p>
 * <blockquote><pre>
 *   &#064;At(value = "CTOR_HEAD", unsafe = true)</pre>
 * </blockquote> 
 * 
 * <p>Example behaviour enforcing post-delegate injection point:</p>
 * <blockquote><pre>
 *   &#064;At(value = "CTOR_HEAD", unsafe = true, args="enforce=POST_DELEGATE")
 *   </pre>
 * </blockquote> 
 */
@AtCode("CTOR_HEAD")
public class ConstructorHead extends MethodHead {
    
    /**
     * Location enforcement
     */
    static enum Enforce {
        
        /**
         * Use default behaviour (POST_INIT)
         */
        DEFAULT,
        
        /**
         * Enforce selection of post-delegate insn 
         */
        POST_DELEGATE,
        
        /**
         * Enforce selection of post-initialiser insn 
         */
        POST_INIT,
        
        /**
         * Enforce selection of the first body insn
         */
        PRE_BODY;
        
    }

    /**
     * Logger 
     */
    protected final ILogger logger = MixinService.getService().getLogger("mixin");

    /**
     * Enforce behaviour parsed from At args
     */
    private final Enforce enforce;
    
    /**
     * True to warn when enfored selection fails 
     */
    private final boolean verbose;

    private final MethodNode method;

    public ConstructorHead(InjectionPointData data) {
        super(data);
        if (!data.isUnsafe()) {
            throw new InvalidInjectionPointException(data.getMixin(), "@At(\"CTOR_HEAD\") requires unsafe=true");
        }
        this.enforce = data.get("enforce", Enforce.DEFAULT);
        this.verbose = data.getMixin().getOption(Option.DEBUG_VERBOSE);
        this.method = data.getMethod();
    }

    @Override
    public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
        if (!(insns instanceof IInsnListEx)) {
            return false;
        }
        
        IInsnListEx xinsns = (IInsnListEx)insns;
        if (!xinsns.isTargetConstructor()) {
            return super.find(desc, insns, nodes);
        }
        
        AbstractInsnNode delegateCtor = xinsns.getSpecialNode(SpecialNodeType.DELEGATE_CTOR);
        AbstractInsnNode postDelegate = delegateCtor != null ? delegateCtor.getNext() : null;
        if (this.enforce == Enforce.POST_DELEGATE) {
            if (postDelegate == null) {
                if (this.verbose) {
                    this.logger.warn("@At(\"{}\") on {}{} targetting {} failed for enforce=POST_DELEGATE because no delegate was found",
                            this.getAtCode(), this.method.name, this.method.desc, xinsns);
                }
                return false;
            }
            nodes.add(postDelegate);
            return true;
        }
        
        SpecialNodeType type = this.enforce == Enforce.PRE_BODY ? SpecialNodeType.CTOR_BODY : SpecialNodeType.INITIALISER_INJECTION_POINT;
        AbstractInsnNode postInit = xinsns.getSpecialNode(type);

        if (postInit != null) {
            nodes.add(postInit);
            return true;
        }
        
        if (postDelegate != null) {
            nodes.add(postDelegate);
            return true;
        }
        
        return super.find(desc, insns, nodes);
    }
}
