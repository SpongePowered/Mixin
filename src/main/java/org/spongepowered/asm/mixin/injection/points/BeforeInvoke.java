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
package org.spongepowered.asm.mixin.injection.points;

import java.util.Collection;
import java.util.ListIterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;

/**
 * <p>This injection point searches for INVOKEVIRTUAL, INVOKESTATIC and
 * INVOKESPECIAL opcodes matching its arguments and returns a list of insns
 * immediately prior to matching instructions. It accepts the following
 * parameters from {@link org.spongepowered.asm.mixin.injection.At At}:</p>
 * 
 * <dl>
 *   <dt>target</dt>
 *   <dd>A
 *   {@link org.spongepowered.asm.mixin.injection.struct.MemberInfo MemberInfo}
 *   which identifies the target method</dd>
 *   <dt>ordinal</dt>
 *   <dd>The ordinal position of the method invocation to match. For example if
 *   the method is invoked 3 times and you want to match the 3rd then you can
 *   specify an <em>ordinal</em> of <b>2</b> (ordinals are zero-indexed). The
 *   default value is <b>-1</b> which supresses ordinal matching</dd>
 * </dl>
 * 
 * <p>Example:</p>
 * <blockquote><pre>
 *   &#064;At(value = "INVOKE", target="func_1234_a(III)V")</pre>
 * </blockquote> 
 * 
 * <p>Note that like all standard injection points, this class matches the insn
 * itself, putting the injection point immediately <em>before</em> the access in
 * question. Use {@link org.spongepowered.asm.mixin.injection.At#shift shift}
 * specifier to adjust the matched opcode as necessary.</p>
 */
public class BeforeInvoke extends InjectionPoint {

    public static final String CODE = "INVOKE";

    protected final MemberInfo target;

    /**
     * This strategy can be used to identify a particular invokation if the same
     * method is invoked at multiple points, if this value is -1 then the
     * strategy returns <em>all</em> invokations of the method.
     */
    protected final int ordinal;

    /**
     * True to turn on strategy debugging to the console
     */
    protected boolean logging = false;

    protected final Logger logger = LogManager.getLogger("mixin");

    protected final String className;

    public BeforeInvoke(InjectionPointData data) {
        this.target = data.getTarget();
        this.ordinal = data.getOrdinal();
        this.logging = data.get("log", false);
        this.className = this.getClass().getSimpleName();
    }

    public BeforeInvoke setLogging(boolean logging) {
        this.logging = logging;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mumfrey.liteloader.transformers.event.InjectionStrategy
     *      #findInjectionPoint(java.lang.String,
     *      org.objectweb.asm.tree.InsnList,
     *      com.mumfrey.liteloader.transformers.event.Event,
     *      java.util.Collection)
     */
    @Override
    public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
        int ordinal = 0;
        boolean found = false;

        if (this.logging) {
             this.logger.info("{} is searching for an injection point in method with descriptor {}", this.className, desc);
        }

        ListIterator<AbstractInsnNode> iter = insns.iterator();
        while (iter.hasNext()) {
            AbstractInsnNode insn = iter.next();

            if (this.matchesInsn(insn)) {
                MemberInfo nodeInfo = new MemberInfo(insn);

                if (this.logging) {
                    this.logger.info("{} is considering insn {}", this.className, nodeInfo);
                }

                if (this.target.matches(nodeInfo.owner, nodeInfo.name, nodeInfo.desc)) {
                    if (this.logging) {
                        this.logger.info("{} > found a matching insn, checking preconditions...", this.className);
                    }
                    
                    if (this.matchesInsn(nodeInfo, ordinal)) {
                        if (this.logging) {
                            this.logger.info("{} > > > found a matching insn at ordinal {}", this.className, ordinal);
                        }
                        
                        nodes.add(insn);
                        found = true;

                        if (this.ordinal == ordinal) {
                            break;
                        }
                    }

                    ordinal++;
                }
            }

            this.inspectInsn(desc, insns, insn);
        }

        return found;
    }

    protected boolean matchesInsn(AbstractInsnNode insn) {
        return insn instanceof MethodInsnNode;
    }

    protected void inspectInsn(String desc, InsnList insns, AbstractInsnNode insn) {
        // stub for subclasses
    }

    protected boolean matchesInsn(MemberInfo nodeInfo, int ordinal) {
        if (this.logging) {
            this.logger.info("{} > > comparing target ordinal {} with current ordinal {}", this.className, this.ordinal, ordinal);
        }
        return this.ordinal == -1 || this.ordinal == ordinal;
    }
}
