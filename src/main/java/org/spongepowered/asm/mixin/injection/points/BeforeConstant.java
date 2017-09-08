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
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.FrameNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.LabelNode;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Constant.Condition;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InjectionPoint.AtCode;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Constants;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

/**
 * Special injection point which can be defined by an {@link Constant}
 * annotation or using the <em>at</em> code <tt>CONSTANT</tt>.
 * 
 * <p>This injection point searches for <tt>LDC</tt> and other constant opcodes
 * matching its arguments and returns a list of injection points matching those
 * instructions. When used with {@link At} it accepts the following parameters:
 * </p>
 * 
 * <dl>
 *   <dt>ordinal</dt>
 *   <dd>The ordinal position of the constant opcode to match. The default value
 *   is <b>-1</b> which supresses ordinal matching</dd>
 *   <dt><em>named argument</em> nullValue</dt>
 *   <dd>To match <tt>null</tt> literals in the method body, set this to
 *   <tt>true</tt></dd>
 *   <dt><em>named argument</em> intValue</dt>
 *   <dd>To match <tt>int</tt> literals in the method body. See also the
 *   <em>expandZeroConditions</em> argument below for concerns when matching
 *   conditional zeroes.</dd>
 *   <dt><em>named argument</em> floatValue</dt>
 *   <dd>To match <tt>float</tt> literals in the method body.</dd>
 *   <dt><em>named argument</em> longValue</dt>
 *   <dd>To match <tt>long</tt> literals in the method body.</dd>
 *   <dt><em>named argument</em> doubleValue</dt>
 *   <dd>To match <tt>double</tt> literals in the method body.</dd>
 *   <dt><em>named argument</em> stringValue</dt>
 *   <dd>To match {@link String} literals in the method body.</dd>
 *   <dt><em>named argument</em> classValue</dt>
 *   <dd>To match {@link Class} literals in the method body.</dd>
 *   <dt><em>named argument</em> log</dt>
 *   <dd>Enable debug logging when searching for matching opcodes.</dd>
 *   <dt><em>named argument</em> expandZeroConditions</dt>
 *   <dd>See the {@link Constant#expandZeroConditions} option, this argument
 *   should be a list of {@link Condition} names</dd>
 * </dl>
 * 
 * <p>Examples:</p>
 * <blockquote><pre>
 *   // Find all integer constans with value 4
 *   &#064;At(value = "CONSTANT", args = "intValue=4")</pre>
 * </blockquote> 
 * <blockquote><pre>
 *   // Find the String literal "foo"
 *   &#064;At(value = "CONSTANT", args = "stringValue=foo"</pre>
 * </blockquote> 
 * <blockquote><pre>
 *   // Find all integer constants with value 0 and expand conditionals
 *   &#064;At(
 *     value = "CONSTANT",
 *     args = {
 *       "intValue=0",
 *       "expandZeroConditions=LESS_THAN_ZERO,GREATER_THAN_ZERO"
 *     }
 *   )
 *   </pre>
 * </blockquote> 
 * 
 * <p>Note that like all standard injection points, this class matches the insn
 * itself, putting the injection point immediately <em>before</em> the access in
 * question. Use {@link org.spongepowered.asm.mixin.injection.At#shift shift}
 * specifier to adjust the matched opcode as necessary.</p>
 */
@AtCode("CONSTANT")
public class BeforeConstant extends InjectionPoint {
    
    private static final Logger logger = LogManager.getLogger("mixin");

    /**
     * Ordinal of the target insn
     */
    private final int ordinal;
    
    private final boolean nullValue;
    private final Integer intValue;
    private final Float floatValue;
    private final Long longValue;
    private final Double doubleValue;
    private final String stringValue;
    private final Type typeValue;
    
    private final int[] expandOpcodes;
    private final boolean expand;
    
    private final String matchByType;
    
    private final boolean log;

    public BeforeConstant(IMixinContext context, AnnotationNode node, String returnType) {
        super(Annotations.<String>getValue(node, "slice", ""), Selector.DEFAULT, null);
        
        Boolean empty = Annotations.<Boolean>getValue(node, "nullValue", (Boolean)null);
        this.ordinal = Annotations.<Integer>getValue(node, "ordinal", Integer.valueOf(-1));
        this.nullValue = empty != null && empty.booleanValue();
        this.intValue = Annotations.<Integer>getValue(node, "intValue", (Integer)null);
        this.floatValue = Annotations.<Float>getValue(node, "floatValue", (Float)null);
        this.longValue = Annotations.<Long>getValue(node, "longValue", (Long)null);
        this.doubleValue = Annotations.<Double>getValue(node, "doubleValue", (Double)null);
        this.stringValue = Annotations.<String>getValue(node, "stringValue", (String)null);
        this.typeValue = Annotations.<Type>getValue(node, "classValue", (Type)null);
        
        this.matchByType = this.validateDiscriminator(context, returnType, empty, "on @Constant annotation");
        this.expandOpcodes = this.parseExpandOpcodes(Annotations.<Condition>getValue(node, "expandZeroConditions", true, Condition.class));
        this.expand = this.expandOpcodes.length > 0;
        
        this.log = Annotations.<Boolean>getValue(node, "log", Boolean.FALSE).booleanValue();
    }
    
    public BeforeConstant(InjectionPointData data) {
        super(data);
        
        String strNullValue = data.get("nullValue", null);
        Boolean empty = strNullValue != null ? Boolean.parseBoolean(strNullValue) : null;
        
        this.ordinal = data.getOrdinal();
        this.nullValue = empty != null && empty.booleanValue();
        this.intValue = Ints.tryParse(data.get("intValue", ""));
        this.floatValue = Floats.tryParse(data.get("floatValue", ""));
        this.longValue = Longs.tryParse(data.get("longValue", ""));
        this.doubleValue = Doubles.tryParse(data.get("doubleValue", ""));
        this.stringValue = data.get("stringValue", null);
        String strClassValue = data.get("classValue", null);
        this.typeValue = strClassValue != null ? Type.getObjectType(strClassValue.replace('.', '/')) : null;
        
        this.matchByType = this.validateDiscriminator(data.getContext(), "V", empty, "in @At(\"CONSTANT\") args");
        if ("V".equals(this.matchByType)) {
            throw new InvalidInjectionException(data.getContext(), "No constant discriminator could be parsed in @At(\"CONSTANT\") args");
        }
        
        List<Condition> conditions = new ArrayList<Condition>();
        String strConditions = data.get("expandZeroConditions", "").toLowerCase();
        for (Condition condition : Condition.values()) {
            if (strConditions.contains(condition.name().toLowerCase())) {
                conditions.add(condition);
            }
        }
        
        this.expandOpcodes = this.parseExpandOpcodes(conditions);
        this.expand = this.expandOpcodes.length > 0;
        
        this.log = data.get("log", false);
    }

    private String validateDiscriminator(IMixinContext context, String returnType, Boolean empty, String type) {
        int c = BeforeConstant.count(empty, this.intValue, this.floatValue, this.longValue, this.doubleValue, this.stringValue, this.typeValue);
        if (c == 1) {
            returnType = null;
        } else if (c > 1) {
            throw new InvalidInjectionException(context, "Conflicting constant discriminators specified " + type + " for " + context);
        }
        return returnType;
    }

    private int[] parseExpandOpcodes(List<Condition> conditions) {
        Set<Integer> opcodes = new HashSet<Integer>();
        for (Condition condition : conditions) {
            Condition actual = condition.getEquivalentCondition();
            for (int opcode : actual.getOpcodes()) {
                opcodes.add(Integer.valueOf(opcode));
            }
        }
        return Ints.toArray(opcodes);
    }

    @Override
    public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
        boolean found = false;

        this.log("BeforeConstant is searching for constants in method with descriptor {}", desc);
        
        ListIterator<AbstractInsnNode> iter = insns.iterator();
        for (int ordinal = 0, last = 0; iter.hasNext();) {
            AbstractInsnNode insn = iter.next();

            boolean matchesInsn = this.expand ? this.matchesConditionalInsn(last, insn) : this.matchesConstantInsn(insn);
            if (matchesInsn) {
                this.log("    BeforeConstant found a matching constant{} at ordinal {}", this.matchByType != null ? " TYPE" : " value", ordinal);
                if (this.ordinal == -1 || this.ordinal == ordinal) {
                    this.log("      BeforeConstant found {}", Bytecode.describeNode(insn).trim());
                    nodes.add(insn);
                    found = true;
                }
                ordinal++;
            }
            
            if (!(insn instanceof LabelNode) && !(insn instanceof FrameNode)) {
                last = insn.getOpcode();
            }
        }

        return found;
    }

    private boolean matchesConditionalInsn(int last, AbstractInsnNode insn) {
        for (int conditionalOpcode : this.expandOpcodes) {
            int opcode = insn.getOpcode();
            if (opcode == conditionalOpcode) {
                if (last == Opcodes.LCMP || last == Opcodes.FCMPL || last == Opcodes.FCMPG || last == Opcodes.DCMPL || last == Opcodes.DCMPG) {
                    this.log("  BeforeConstant is ignoring {} following {}", Bytecode.getOpcodeName(opcode), Bytecode.getOpcodeName(last));
                    return false;
                }
                
                this.log("  BeforeConstant found {} instruction", Bytecode.getOpcodeName(opcode));
                return true;
            }
        }
        
        if (this.intValue != null && this.intValue.intValue() == 0 && Bytecode.isConstant(insn)) {
            Object value = Bytecode.getConstant(insn);
            this.log("  BeforeConstant found INTEGER constant: value = {}", value);
            return value instanceof Integer && ((Integer)value).intValue() == 0;
        }
        
        return false;
    }

    private boolean matchesConstantInsn(AbstractInsnNode insn) {
        if (!Bytecode.isConstant(insn)) {
            return false;
        }
        
        Object value = Bytecode.getConstant(insn);
        if (value == null) {
            this.log("  BeforeConstant found NULL constant: nullValue = {}", this.nullValue);
            return this.nullValue || Constants.OBJECT.equals(this.matchByType);
        } else if (value instanceof Integer) {
            this.log("  BeforeConstant found INTEGER constant: value = {}, intValue = {}", value, this.intValue);
            return value.equals(this.intValue) || "I".equals(this.matchByType);
        } else if (value instanceof Float) {
            this.log("  BeforeConstant found FLOAT constant: value = {}, floatValue = {}", value, this.floatValue);
            return value.equals(this.floatValue) || "F".equals(this.matchByType);
        } else if (value instanceof Long) {
            this.log("  BeforeConstant found LONG constant: value = {}, longValue = {}", value, this.longValue);
            return value.equals(this.longValue) || "J".equals(this.matchByType);
        } else if (value instanceof Double) {
            this.log("  BeforeConstant found DOUBLE constant: value = {}, doubleValue = {}", value, this.doubleValue);
            return value.equals(this.doubleValue) || "D".equals(this.matchByType);
        } else if (value instanceof String) {
            this.log("  BeforeConstant found STRING constant: value = {}, stringValue = {}", value, this.stringValue);
            return value.equals(this.stringValue) || Constants.STRING.equals(this.matchByType);
        } else if (value instanceof Type) {
            this.log("  BeforeConstant found CLASS constant: value = {}, typeValue = {}", value, this.typeValue);
            return value.equals(this.typeValue) || Constants.CLASS.equals(this.matchByType);
        }
        
        return false;
    }

    protected void log(String message, Object... params) {
        if (this.log) {
            BeforeConstant.logger.info(message, params);
        }
    }
    
    private static int count(Object... values) {
        int counter = 0;
        for (Object value : values) {
            if (value != null) {
                counter++;
            }
        }
        return counter;
    }

}
