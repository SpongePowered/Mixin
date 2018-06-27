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
import java.util.ListIterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InjectionPoint.AtCode;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.refmap.IMixinContext;

/**
 * <p>This injection point searches for INVOKEVIRTUAL, INVOKESTATIC and
 * INVOKESPECIAL opcodes matching its arguments and returns a list of insns
 * immediately prior to matching instructions. It accepts the following
 * parameters from {@link At}:</p>
 * 
 * <dl>
 *   <dt>target</dt>
 *   <dd>A {@link MemberInfo MemberInfo} which identifies the target method</dd>
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
 * question. Use {@link At#shift} specifier to adjust the matched opcode as
 * necessary.</p>
 */
@AtCode("INVOKE")
public class BeforeInvoke extends InjectionPoint {
    
    /**
     * Member search type, the <tt>PERMISSIVE</tt> search is only used when
     * refmap remapping is enabled.
     */
    public enum SearchType {
        
        STRICT,
        PERMISSIVE
        
    }

    protected final MemberInfo target;
    
    /**
     * This option enables a fallback "permissive" search to occur if initial
     * search fails <b>if and only if the {@link Option#REFMAP_REMAP} option is
     * enabled and the context mixin's parent config has a valid refmap</b>.
     */
    protected final boolean allowPermissive;

    /**
     * This strategy can be used to identify a particular invocation if the same
     * method is invoked at multiple points, if this value is -1 then the
     * strategy returns <em>all</em> invocations of the method.
     */
    protected final int ordinal;

    /**
     * Class name (description) for debug logging
     */
    protected final String className;
    
    /**
     * 
     */
    protected final IMixinContext context;

    /**
     * Logger reference 
     */
    protected final Logger logger = LogManager.getLogger("mixin");

    /**
     * True to turn on strategy debugging to the console
     */
    private boolean log = false;

    public BeforeInvoke(InjectionPointData data) {
        super(data);
        
        this.target = data.getTarget();
        this.ordinal = data.getOrdinal();
        this.log = data.get("log", false);
        this.className = this.getClassName();
        this.context = data.getContext();
        this.allowPermissive = this.context.getOption(Option.REFMAP_REMAP) && this.context.getOption(Option.REFMAP_REMAP_ALLOW_PERMISSIVE)
                && !this.context.getReferenceMapper().isDefault();
    }

    private String getClassName() {
        AtCode atCode = this.getClass().<AtCode>getAnnotation(AtCode.class);
        return String.format("@At(%s)", atCode != null ? atCode.value() : this.getClass().getSimpleName().toUpperCase());
    }

    /**
     * Set the logging state for this injector
     * 
     * @param logging logging state
     * @return fluent interface
     */
    public BeforeInvoke setLogging(boolean logging) {
        this.log = logging;
        return this;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.InjectionPoint
     *      #find(java.lang.String, org.spongepowered.asm.lib.tree.InsnList,
     *      java.util.Collection)
     */
    @Override
    public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
        this.log("{} is searching for an injection point in method with descriptor {}", this.className, desc);
        
        if (!this.find(desc, insns, nodes, this.target, SearchType.STRICT) && this.target.desc != null && this.allowPermissive) {
            this.logger.warn("STRICT match for {} using \"{}\" in {} returned 0 results, attempting permissive search. "
                    + "To inhibit permissive search set mixin.env.allowPermissiveMatch=false", this.className, this.target, this.context);
            return this.find(desc, insns, nodes, this.target, SearchType.PERMISSIVE);
        }
        return true;
    }

    protected boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes, MemberInfo member, SearchType searchType) {
        if (member == null) {
            return false;
        }
        
        MemberInfo target = searchType == SearchType.PERMISSIVE ? member.transform(null) : member;
        
        int ordinal = 0;
        int found = 0;
        
        ListIterator<AbstractInsnNode> iter = insns.iterator();
        while (iter.hasNext()) {
            AbstractInsnNode insn = iter.next();

            if (this.matchesInsn(insn)) {
                MemberInfo nodeInfo = new MemberInfo(insn);
                this.log("{} is considering insn {}", this.className, nodeInfo);

                if (target.matches(nodeInfo.owner, nodeInfo.name, nodeInfo.desc)) {
                    this.log("{} > found a matching insn, checking preconditions...", this.className);
                    
                    if (this.matchesInsn(nodeInfo, ordinal)) {
                        this.log("{} > > > found a matching insn at ordinal {}", this.className, ordinal);
                        
                        if (this.addInsn(insns, nodes, insn)) {
                            found++;
                        }

                        if (this.ordinal == ordinal) {
                            break;
                        }
                    }

                    ordinal++;
                }
            }

            this.inspectInsn(desc, insns, insn);
        }
        
        if (searchType == SearchType.PERMISSIVE && found > 1) {
            this.logger.warn("A permissive match for {} using \"{}\" in {} matched {} instructions, this may cause unexpected behaviour. "
                    + "To inhibit permissive search set mixin.env.allowPermissiveMatch=false", this.className, member, this.context, found);
        }

        return found > 0;
    }

    protected boolean addInsn(InsnList insns, Collection<AbstractInsnNode> nodes, AbstractInsnNode insn) {
        nodes.add(insn);
        return true;
    }

    protected boolean matchesInsn(AbstractInsnNode insn) {
        return insn instanceof MethodInsnNode;
    }

    protected void inspectInsn(String desc, InsnList insns, AbstractInsnNode insn) {
        // stub for subclasses
    }

    protected boolean matchesInsn(MemberInfo nodeInfo, int ordinal) {
        this.log("{} > > comparing target ordinal {} with current ordinal {}", this.className, this.ordinal, ordinal);
        return this.ordinal == -1 || this.ordinal == ordinal;
    }
    
    protected void log(String message, Object... params) {
        if (this.log) {
            this.logger.info(message, params); 
        }
    }
    
}
