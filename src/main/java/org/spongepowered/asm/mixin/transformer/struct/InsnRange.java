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
package org.spongepowered.asm.mixin.transformer.struct;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;

/**
 * Struct for representing a range of instructions
 */
public class InsnRange {

    /**
     * Start of the range (line number)
     */
    public final int start;
    
    /**
     * End of the range (line number)
     */
    public final int end;
    
    /**
     * Range marker (index of insn)
     */
    public final int marker;

    /**
     * Create a range with the specified values.
     * 
     * @param start Start of the range
     * @param end End of the range
     * @param marker Arbitrary marker value
     */
    public InsnRange(int start, int end, int marker) {
        this.start = start;
        this.end = end;
        this.marker = marker;
    }
    
    /**
     * Range is valid if both start and end are nonzero and end is after or
     * at start
     * 
     * @return true if valid
     */
    public boolean isValid() {
        return (this.start != 0 && this.end != 0 && this.end >= this.start);
    }
    
    /**
     * Returns true if the supplied value is between or equal to start and
     * end
     * 
     * @param value true if the range contains value
     */
    public boolean contains(int value) {
        return value >= this.start && value <= this.end;
    }
    
    /**
     * Returns true if the supplied value is outside the range
     * 
     * @param value true if the range does not contain value
     */
    public boolean excludes(int value) {
        return value < this.start || value > this.end;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("Range[%d-%d,%d,valid=%s)", this.start, this.end, this.marker, this.isValid());
    }

    /**
     * Apply this range to the specified insn list
     * 
     * @param insns insn list to filter
     * @param inclusive whether to include or exclude instructions
     * @return filtered list
     */
    public Deque<AbstractInsnNode> apply(InsnList insns, boolean inclusive) {
        Deque<AbstractInsnNode> filtered = new ArrayDeque<AbstractInsnNode>();
        int line = 0;

        boolean gatherNodes = false;
        int trimAtOpcode = -1;
        LabelNode optionalInsn = null;
        for (Iterator<AbstractInsnNode> iter = insns.iterator(this.marker); iter.hasNext();) {
            AbstractInsnNode insn = iter.next();
            if (insn instanceof LineNumberNode) {
                line = ((LineNumberNode)insn).line;
                AbstractInsnNode next = insns.get(insns.indexOf(insn) + 1);
                if (line == this.end && next.getOpcode() != Opcodes.RETURN) {
                    gatherNodes = !inclusive;
                    trimAtOpcode = Opcodes.RETURN;
                } else {
                    gatherNodes = inclusive ? this.contains(line) : this.excludes(line);
                    trimAtOpcode = -1;
                }
            } else if (gatherNodes) {
                if (optionalInsn != null) {
                    filtered.add(optionalInsn);
                    optionalInsn = null;
                }
                
                if (insn instanceof LabelNode) {
                    optionalInsn = (LabelNode)insn;
                } else {
                    int opcode = insn.getOpcode();
                    if (opcode == trimAtOpcode) {
                        trimAtOpcode = -1;
                        continue;
                    }
                    
                    filtered.add(insn);
                }
            }
        }
        
        return filtered;
    }

}
