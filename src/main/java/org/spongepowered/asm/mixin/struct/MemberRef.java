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
package org.spongepowered.asm.mixin.struct;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.FieldInsnNode;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.transformer.throwables.MixinTransformerError;
import org.spongepowered.asm.util.Bytecode;

/**
 * Reference to a field or method that also includes invocation instructions.
 *
 * <p>To instances are defined to be equal if they both refer to the same method
 * and have the same invocation instructions.</p>
 */
public abstract class MemberRef {

    /**
     * A static reference to a method backed by an invoke instruction
     */
    public static final class Method extends MemberRef {
        
        private static final int OPCODES = Opcodes.INVOKEVIRTUAL | Opcodes.INVOKESPECIAL | Opcodes.INVOKESTATIC | Opcodes.INVOKEINTERFACE;

        /**
         * Method invocation instruction
         */
        public final MethodInsnNode insn;

        /**
         * ctor
         *
         * @param insn Method instruction of this member reference
         */
        public Method(MethodInsnNode insn) {
            this.insn = insn;
        }

        @Override
        public boolean isField() {
            return false;
        }

        @Override
        public int getOpcode() {
            return this.insn.getOpcode();
        }
        
        @Override
        public void setOpcode(int opcode) {
            if ((opcode & Method.OPCODES) == 0) {
                throw new IllegalArgumentException("Invalid opcode for method instruction: 0x" + Integer.toHexString(opcode));
            }

            this.insn.setOpcode(opcode);
        }

        @Override
        public String getOwner() {
            return this.insn.owner;
        }

        @Override
        public void setOwner(String owner) {
            this.insn.owner = owner;
        }

        @Override
        public String getName() {
            return this.insn.name;
        }
        
        @Override
        public void setName(String name) {
            this.insn.name = name;
        }

        @Override
        public String getDesc() {
            return this.insn.desc;
        }

        @Override
        public void setDesc(String desc) {
            this.insn.desc = desc;
        }
    }

    /**
     * A static reference to a field backed by field get/put instruction
     */
    public static final class Field extends MemberRef {
        
        private static final int OPCODES = Opcodes.GETSTATIC | Opcodes.PUTSTATIC | Opcodes.GETFIELD | Opcodes.PUTFIELD;

        /**
         * Field accessor instruction
         */
        public final FieldInsnNode insn;

        /**
         * ctor
         *
         * @param insn Field instruction this member reference
         */
        public Field(FieldInsnNode insn) {
            this.insn = insn;
        }

        @Override
        public boolean isField() {
            return true;
        }

        @Override
        public int getOpcode() {
            return this.insn.getOpcode();
        }
        
        @Override
        public void setOpcode(int opcode) {
            if ((opcode & Field.OPCODES) == 0) {
                throw new IllegalArgumentException("Invalid opcode for field instruction: 0x" + Integer.toHexString(opcode));
            }
            
            this.insn.setOpcode(opcode);
        }

        @Override
        public String getOwner() {
            return this.insn.owner;
        }

        @Override
        public void setOwner(String owner) {
            this.insn.owner = owner;
        }

        @Override
        public String getName() {
            return this.insn.name;
        }
        
        @Override
        public void setName(String name) {
            this.insn.name = name;
        }
        
        @Override
        public String getDesc() {
            return this.insn.desc;
        }

        @Override
        public void setDesc(String desc) {
            this.insn.desc = desc;
        }
    }

    /**
     * A reference to a field or method backed by a method handle
     */
    public static final class Handle extends MemberRef {

        private org.spongepowered.asm.lib.Handle handle;

        /**
         * Creates a member reference initially referring to the member referred
         * to by the method handle and the invocation instruction of the method
         * handle.
         *
         * @param handle Initial method handle.
         */
        public Handle(org.spongepowered.asm.lib.Handle handle) {
            this.handle = handle;
        }

        /**
         * Gets a method handle for the member this is object is referring to.
         *
         * @return Method handle representing this object
         */
        public org.spongepowered.asm.lib.Handle getMethodHandle() {
            return this.handle;
        }

        @Override
        public boolean isField() {
            switch (this.handle.getTag()) {
                case Opcodes.H_INVOKEVIRTUAL:
                case Opcodes.H_INVOKESTATIC:
                case Opcodes.H_INVOKEINTERFACE:
                case Opcodes.H_INVOKESPECIAL:
                case Opcodes.H_NEWINVOKESPECIAL:
                    return false;
                case Opcodes.H_GETFIELD:
                case Opcodes.H_GETSTATIC:
                case Opcodes.H_PUTFIELD:
                case Opcodes.H_PUTSTATIC:
                    return true;
                default:
                    throw new MixinTransformerError("Invalid tag " + this.handle.getTag() + " for method handle " + this.handle + ".");
            }
        }

        @Override
        public int getOpcode() {
            int opcode = MemberRef.opcodeFromTag(this.handle.getTag());
            if (opcode == 0) {
                throw new MixinTransformerError("Invalid tag " + this.handle.getTag() + " for method handle " + this.handle + ".");
            }
            return opcode;
        }
        
        @Override
        public void setOpcode(int opcode) {
            int tag = MemberRef.tagFromOpcode(opcode);
            if (tag == 0) {
                throw new MixinTransformerError("Invalid opcode " + Bytecode.getOpcodeName(opcode) + " for method handle " + this.handle + ".");
            }
            boolean itf = tag == Opcodes.H_INVOKEINTERFACE;
            this.handle = new org.spongepowered.asm.lib.Handle(tag, this.handle.getOwner(), this.handle.getName(), this.handle.getDesc(), itf);
        }

        @Override
        public String getOwner() {
            return this.handle.getOwner();
        }

        @Override
        public void setOwner(String owner) {
            boolean itf = this.handle.getTag() == Opcodes.H_INVOKEINTERFACE;
            this.handle = new org.spongepowered.asm.lib.Handle(this.handle.getTag(), owner, this.handle.getName(), this.handle.getDesc(), itf);
        }

        @Override
        public String getName() {
            return this.handle.getName();
        }

        @Override
        public void setName(String name) {
            boolean itf = this.handle.getTag() == Opcodes.H_INVOKEINTERFACE;
            this.handle = new org.spongepowered.asm.lib.Handle(this.handle.getTag(), this.handle.getOwner(), name, this.handle.getDesc(), itf);
        }

        @Override
        public String getDesc() {
            return this.handle.getDesc();
        }

        @Override
        public void setDesc(String desc) {
            boolean itf = this.handle.getTag() == Opcodes.H_INVOKEINTERFACE;
            this.handle = new org.spongepowered.asm.lib.Handle(this.handle.getTag(), this.handle.getOwner(), this.handle.getName(), desc, itf);
        }
    }
    
    private static final int[] H_OPCODES = {
        0,                          // invalid
        Opcodes.GETFIELD,           // H_GETFIELD
        Opcodes.GETSTATIC,          // H_GETSTATIC
        Opcodes.PUTFIELD,           // H_PUTFIELD
        Opcodes.PUTSTATIC,          // H_PUTSTATIC
        Opcodes.INVOKEVIRTUAL,      // H_INVOKEVIRTUAL
        Opcodes.INVOKESTATIC,       // H_INVOKESTATIC
        Opcodes.INVOKESPECIAL,      // H_INVOKESPECIAL
        Opcodes.INVOKESPECIAL,      // H_NEWINVOKESPECIAL
        Opcodes.INVOKEINTERFACE     // H_INVOKEINTERFACE
    };

    /**
     * Whether this member is a field.
     *
     * @return If this member is a field, else it is a method
     */
    public abstract boolean isField();

    /**
     * The opcode of the invocation.
     *
     * @return The opcode of the invocation
     */
    public abstract int getOpcode();

    /**
     * Set the opcode of the invocation.
     * 
     * @param opcode new opcode
     */
    public abstract void setOpcode(int opcode);

    /**
     * The internal name for the owner of this member.
     *
     * @return The owners name
     */
    public abstract String getOwner();

    /**
     * Changes the owner of this
     *
     * @param owner New owner
     */
    public abstract void setOwner(String owner);

    /**
     * Name of this member.
     *
     * @return Name of this member.
     */
    public abstract String getName();
    
    /**
     * Rename this member.
     *
     * @param name New name for this member.
     */
    public abstract void setName(String name);

    /**
     * Descriptor of this member.
     *
     * @return Descriptor of this member
     */
    public abstract String getDesc();

    /**
     * Changes the descriptor of this member
     *
     * @param desc New descriptor of this member
     */
    public abstract void setDesc(String desc);

    @Override
    public String toString() {
        String name = Bytecode.getOpcodeName(this.getOpcode());
        return String.format("%s for %s.%s%s%s", name, this.getOwner(), this.getName(), this.isField() ? ":" : "", this.getDesc());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MemberRef)) {
            return false;
        }

        MemberRef other = (MemberRef)obj;
        return this.getOpcode() == other.getOpcode()
                && this.getOwner().equals(other.getOwner())
                && this.getName().equals(other.getName())
                && this.getDesc().equals(other.getDesc());
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
    
    static int opcodeFromTag(int tag) {
        return (tag >= 0 && tag < MemberRef.H_OPCODES.length) ? MemberRef.H_OPCODES[tag] : 0;
    }
    
    static int tagFromOpcode(int opcode) {
        for (int tag = 1; tag < MemberRef.H_OPCODES.length; tag++) {
            if (MemberRef.H_OPCODES[tag] == opcode) {
                return tag;
            }
        }
        return 0;
    }
    
}
