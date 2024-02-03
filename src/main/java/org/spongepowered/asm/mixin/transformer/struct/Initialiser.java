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

import java.util.Deque;
import java.util.Locale;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.injection.struct.Constructor;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Bytecode;

public class Initialiser {

    /**
     * Strategy for injecting initialiser insns
     */
    public enum InjectionMode {

        /**
         * Default mode, attempts to place initialisers after all other
         * competing initialisers in the target ctor
         */
        DEFAULT,
        
        /**
         * Safe mode, only injects initialiser directly after the super-ctor
         * invocation 
         */
        SAFE;
        
        /**
         * Get the injection mode based on the the environment
         * 
         * @param env Environment to query for the injection mode option
         */
        public static InjectionMode ofEnvironment(MixinEnvironment env) {
            String strMode = env.getOptionValue(Option.INITIALISER_INJECTION_MODE);
            if (strMode == null) {
                return Initialiser.InjectionMode.DEFAULT;
            }
            try {
                return Initialiser.InjectionMode.valueOf(strMode.toUpperCase(Locale.ROOT));
            } catch (Exception ex) {
                Initialiser.logger.warn("Could not parse unexpected value \"{}\" for mixin.initialiserInjectionMode, reverting to DEFAULT",
                        strMode);
                return Initialiser.InjectionMode.DEFAULT;
            }
        }

    }

    /**
     * Logger
     */
    static final ILogger logger = MixinService.getService().getLogger("mixin");

    /**
     * List of opcodes which must not appear in a class initialiser, mainly a
     * sanity check so that if any of the specified opcodes are found, we can
     * log it as an error condition and then people can bitch at me to fix it.
     * Essentially if it turns out that field initialisers can somehow make use
     * of local variables, then I need to write some code to ensure that said
     * locals are shifted so that they don't interfere with locals in the
     * receiving constructor. 
     */
    protected static final int[] OPCODE_BLACKLIST = {
        Opcodes.RETURN, Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.IALOAD, Opcodes.LALOAD, Opcodes.FALOAD, Opcodes.DALOAD,
        Opcodes.AALOAD, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD, Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE,
        Opcodes.ASTORE, Opcodes.IASTORE, Opcodes.LASTORE, Opcodes.FASTORE, Opcodes.DASTORE, Opcodes.AASTORE, Opcodes.BASTORE, Opcodes.CASTORE,
        Opcodes.SASTORE
    };


    /**
     * Mixin context which contains the source constructor 
     */
    private final MixinTargetContext mixin;
    
    /**
     * Source constructor 
     */
    private final MethodNode ctor;
    
    /**
     * Filtered instructions
     */
    private Deque<AbstractInsnNode> insns;
    
    public Initialiser(MixinTargetContext mixin, MethodNode ctor, InsnRange range) {
        this.mixin = mixin;
        this.ctor = ctor;
        this.initInstructions(range);
    }
    
    private void initInstructions(InsnRange range) {
        // Now we know where the constructor is, look for insns which lie OUTSIDE the method body
        this.insns = range.apply(this.ctor.instructions, false);

        for (AbstractInsnNode insn : this.insns) {
            int opcode = insn.getOpcode();
            for (int ivalidOp : Initialiser.OPCODE_BLACKLIST) {
                if (opcode == ivalidOp) {
                    // At the moment I don't handle any transient locals because I haven't seen any in the wild, but let's avoid writing
                    // code which will likely break things and fix it if a real test case ever appears
                    throw new InvalidMixinException(this.mixin, "Cannot handle " + Bytecode.getOpcodeName(opcode) + " opcode (0x"
                            + Integer.toHexString(opcode).toUpperCase(Locale.ROOT) + ") in class initialiser");
                }
            }
        }
        
        // Check that the last insn is a PUTFIELD, if it's not then 
        AbstractInsnNode last = this.insns.peekLast();
        if (last != null) {
            if (last.getOpcode() != Opcodes.PUTFIELD) {
                throw new InvalidMixinException(this.mixin, "Could not parse initialiser, expected 0xB5, found 0x"
                        + Integer.toHexString(last.getOpcode()) + " in " + this);
            }
        }
    }
    
    /**
     * Get the number of instructions in the extracted initialiser
     */
    public int size() {
        return this.insns.size();
    }

    /**
     * Get the MAXS for the original (source) constructor
     */
    public int getMaxStack() {
        return this.ctor.maxStack;
    }

    /**
     * Get the source constructor
     */
    public MethodNode getCtor() {
        return this.ctor;
    }

    /**
     * Get the extracted instructions
     */
    public Deque<AbstractInsnNode> getInsns() {
        return this.insns;
    }

    /**
     * Inject initialiser code into the target constructor
     * 
     * @param ctor Constructor to inject into
     */
    public void injectInto(Constructor ctor) {
        AbstractInsnNode marker = ctor.findInitialiserInjectionPoint(Initialiser.InjectionMode.ofEnvironment(this.mixin.getEnvironment()));
        if (marker == null) {
            Initialiser.logger.warn("Failed to locate initialiser injection point in <init>{}, initialiser was not mixed in.", ctor.getDesc());
            return;
        }

        Map<LabelNode, LabelNode> labels = Bytecode.cloneLabels(ctor.insns);
        for (AbstractInsnNode node : this.insns) {
            if (node instanceof LabelNode) {
                continue;
            }
            if (node instanceof JumpInsnNode) {
                throw new InvalidMixinException(this.mixin, "Unsupported JUMP opcode in initialiser in " + this.mixin);
            }
            
            ctor.insertBefore(marker, node.clone(labels));
        }
    }

}
