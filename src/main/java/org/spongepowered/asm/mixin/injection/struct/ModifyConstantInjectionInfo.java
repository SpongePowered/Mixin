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
package org.spongepowered.asm.mixin.injection.struct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Constant.Condition;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.invoke.ModifyConstantInjector;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Constants;

import com.google.common.primitives.Ints;

/**
 * Information about a constant modifier injector
 */
public class ModifyConstantInjectionInfo extends InjectionInfo {
    
    /**
     * Special injection point which is defined by an {@link Constant}
     * annotation.
     */
    static class BeforeConstant extends InjectionPoint {
        
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

        public BeforeConstant(InjectionInfo info, AnnotationNode node, String returnType) {
            Boolean empty = Annotations.<Boolean>getValue(node, "nullValue", (Boolean)null);
            this.ordinal = Annotations.<Integer>getValue(node, "ordinal", Integer.valueOf(-1));
            this.nullValue = empty != null ? empty.booleanValue() : false;
            this.intValue = Annotations.<Integer>getValue(node, "intValue", (Integer)null);
            this.floatValue = Annotations.<Float>getValue(node, "floatValue", (Float)null);
            this.longValue = Annotations.<Long>getValue(node, "longValue", (Long)null);
            this.doubleValue = Annotations.<Double>getValue(node, "doubleValue", (Double)null);
            this.stringValue = Annotations.<String>getValue(node, "stringValue", (String)null);
            this.typeValue = Annotations.<Type>getValue(node, "classValue", (Type)null);
            
            this.matchByType = this.validateDiscriminator(info, returnType, empty);
            this.expandOpcodes = this.parseExpandOpcodes(node);
            this.expand = this.expandOpcodes.length > 0;
            
            this.log = Annotations.<Boolean>getValue(node, "log", Boolean.FALSE).booleanValue();
        }

        private String validateDiscriminator(InjectionInfo info, String returnType, Boolean empty) {
            int c = BeforeConstant.count(empty, this.intValue, this.floatValue, this.longValue, this.doubleValue, this.stringValue, this.typeValue);
            if (c == 1) {
                returnType = null;
            } else if (c > 1) {
                throw new InvalidInjectionException(info, "Conflicting constant discriminators specified on @Constant annotation for " + info);
            }
            return returnType;
        }

        private int[] parseExpandOpcodes(AnnotationNode node) {
            Set<Integer> opcodes = new HashSet<Integer>();
            for (Condition condition : Annotations.<Condition>getValue(node, "expandZeroConditions", true, Condition.class)) {
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
            for (int ordinal = 0; iter.hasNext();) {
                AbstractInsnNode insn = iter.next();

                boolean matchesInsn = this.expand ? this.matchesConditionalInsn(insn) : this.matchesConstantInsn(insn);
                if (matchesInsn) {
                    this.log("    BeforeConstant found a matching constant{} at ordinal {}", this.matchByType != null ? " TYPE" : " value", ordinal);
                    if (this.ordinal == -1 || this.ordinal == ordinal) {
                        this.log("      BeforeConstant found {}", Bytecode.describeNode(insn).trim());
                        nodes.add(insn);
                        found = true;
                    }
                    ordinal++;
                }
            }

            return found;
        }

        private boolean matchesConditionalInsn(AbstractInsnNode insn) {
            for (int conditionalOpcode : this.expandOpcodes) {
                if (insn.getOpcode() == conditionalOpcode) {
                    this.log("  BeforeConstant found %s instruction", Bytecode.getOpcodeName(conditionalOpcode));
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

    public ModifyConstantInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation);
    }
    
    @Override
    protected List<AnnotationNode> readInjectionPoints(String type) {
        AnnotationNode constantAnnotation = Annotations.<AnnotationNode>getValue(this.annotation, "constant");
        List<AnnotationNode> ats = new ArrayList<AnnotationNode>();
        ats.add(constantAnnotation);
        return ats;
    }

    @Override
    protected void parseInjectionPoints(List<AnnotationNode> ats) {
        Type returnType = Type.getReturnType(this.method.desc);
        
        for (AnnotationNode at : ats) {
            this.injectionPoints.add(new BeforeConstant(this, at, returnType.getDescriptor()));
        }
    }
    
    @Override
    protected Injector parseInjector(AnnotationNode injectAnnotation) {
        return new ModifyConstantInjector(this);
    }
    
    @Override
    protected String getDescription() {
        return "Constant modifier method";
    }
    
}
