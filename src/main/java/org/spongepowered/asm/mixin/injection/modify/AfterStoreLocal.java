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
package org.spongepowered.asm.mixin.injection.modify;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.injection.InjectionPoint.AtCode;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;

/**
 * <p>This injection point is a companion for the {@link ModifyVariable}
 * injector which searches for STORE operations which match the local variables
 * described by the injector's defined discriminators.</p>
 * 
 * <p>This allows you consumers to specify an injection immediately after a
 * local variable is written in a method. Specify an <tt>ordinal</tt> of <tt>n
 * </tt> to match the <em>n + 1<sup>th</sup></em> access of the variable in
 * question.</p>
 * 
 * <dl>
 *   <dt>ordinal</dt>
 *   <dd>The ordinal position of the STORE opcode for the matching local
 *   variable to search for, if not specified then the injection point returns
 *   <em>all </em> opcodes for which the parent annotation's discriminators
 *   match. The default value is <b>-1</b> which supresses ordinal checking.
 *   </dd>
 * </dl>
 * 
 * <p>Example:</p>
 * <blockquote><pre>
 *   &#064;ModifyVariable(
 *       method = "md",
 *       ordinal = 1,
 *       at = &#064;At(
 *           value = "STORE",
 *           ordinal = 0
 *       )
 *   )</pre>
 * </blockquote>
 * <p>Note that if <em>value</em> is the only parameter specified, it can be
 * omitted:</p> 
 * <blockquote><pre>
 *   &#064;At("STORE")</pre>
 * </blockquote>
 * 
 * <p><b>Important Note:</b> Unlike other standard injection points, this class
 * matches the insn immediately <b>after</b> the matching point.</p>
 */
@AtCode("STORE")
public class AfterStoreLocal extends BeforeLoadLocal {

    public AfterStoreLocal(InjectionPointData data) {
        super(data, Opcodes.ISTORE, true);
    }
    
}
