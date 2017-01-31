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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.TypeInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InjectionPoint.AtCode;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.util.Constants;

import com.google.common.base.Strings;

/**
 * <p>This injection point searches for NEW opcodes matching its arguments and
 * returns a list of insns immediately prior to matching instructions. It
 * accepts the following parameters from
 * {@link org.spongepowered.asm.mixin.injection.At At}:</p>
 * 
 * <dl>
 *   <dt><em>named argument</em> class (or specify using <tt>target</tt></dt>
 *   <dd>The value of the NEW node to look for, the fully-qualified class name
 *   </dd>
 *   <dt>ordinal</dt>
 *   <dd>The ordinal position of the NEW opcode to match. For example if the NEW
 *   opcode appears 3 times in the method and you want to match the 3rd then you
 *   can specify an <em>ordinal</em> of <b>2</b> (ordinals are zero-indexed).
 *   The default value is <b>-1</b> which supresses ordinal matching</dd>
 *   <dt>target</dt>
 *   <dd>Target class can also be specified in <tt>target</tt> which also
 *   supports specifying the exact signature of the constructor to target. In
 *   this case the <em>target type</em> is specified as the return type of the
 *   constructor (in place of the usual <tt>V</tt> (void)) and no owner or name
 *   should be specified (they are ignored).</dd>
 * </dl>
 * 
 * <p>Examples:</p>
 * <blockquote><pre>
 *   // Find all NEW opcodes for <tt>String</tt>
 *   &#064;At(value = "NEW", args = "class=java/lang/String")</pre>
 * </blockquote> 
 * <blockquote><pre>
 *   // Find all NEW opcodes for <tt>String</tt>
 *   &#064;At(value = "NEW", target = "java/lang/String"</pre>
 * </blockquote> 
 * <blockquote><pre>
 *   // Find all NEW opcodes for <tt>String</tt> which are constructed using the
 *   // ctor which takes an array of <tt>char</tt>
 *   &#064;At(value = "NEW", target = "([C)Ljava/lang/String;"</pre>
 * </blockquote> 
 * 
 * <p>Note that like all standard injection points, this class matches the insn
 * itself, putting the injection point immediately <em>before</em> the access in
 * question. Use {@link org.spongepowered.asm.mixin.injection.At#shift shift}
 * specifier to adjust the matched opcode as necessary.</p>
 */
@AtCode("NEW")
public class BeforeNew extends InjectionPoint {

    /**
     * Class name we're looking for
     */
    private final String target;
    
    /**
     * Ctor descriptor we're looking for 
     */
    private final String desc;

    /**
     * Ordinal value
     */
    private final int ordinal;

    public BeforeNew(InjectionPointData data) {
        super(data);
        
        this.ordinal = data.getOrdinal();
        String target = Strings.emptyToNull(data.get("class", data.get("target", "")).replace('.', '/'));
        MemberInfo member = MemberInfo.parseAndValidate(target, data.getContext());
        this.target = member.toCtorType();
        this.desc = member.toCtorDesc();
    }
    
    /**
     * Returns whether this injection point has a constructor descriptor defined
     */
    public boolean hasDescriptor() {
        return this.desc != null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
        boolean found = false;
        int ordinal = 0;

        Collection<TypeInsnNode> newNodes = new ArrayList<TypeInsnNode>();
        Collection<AbstractInsnNode> candidates = (Collection<AbstractInsnNode>) (this.desc != null ? newNodes : nodes);
        ListIterator<AbstractInsnNode> iter = insns.iterator();
        while (iter.hasNext()) {
            AbstractInsnNode insn = iter.next();

            if (insn instanceof TypeInsnNode && insn.getOpcode() == Opcodes.NEW && this.matchesOwner((TypeInsnNode) insn)) {
                if (this.ordinal == -1 || this.ordinal == ordinal) {
                    candidates.add(insn);
                    found = this.desc == null;
                }

                ordinal++;
            }
        }
        
        if (this.desc != null) {
            for (TypeInsnNode newNode : newNodes) {
                if (this.findCtor(insns, newNode)) {
                    nodes.add(newNode);
                    found = true;
                }
            }
        }

        return found;
    }

    protected boolean findCtor(InsnList insns, TypeInsnNode newNode) {
        int indexOf = insns.indexOf(newNode);
        for (Iterator<AbstractInsnNode> iter = insns.iterator(indexOf); iter.hasNext();) {
            AbstractInsnNode insn = iter.next();
            if (insn instanceof MethodInsnNode && insn.getOpcode() == Opcodes.INVOKESPECIAL) {
                MethodInsnNode methodNode = (MethodInsnNode)insn;
                if (Constants.CTOR.equals(methodNode.name) && methodNode.owner.equals(newNode.desc) && methodNode.desc.equals(this.desc)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesOwner(TypeInsnNode insn) {
        return this.target == null || this.target.equals(insn.desc);
    }
}
