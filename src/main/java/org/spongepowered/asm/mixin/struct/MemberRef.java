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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.transformer.throwables.MixinTransformerError;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Handles;

/**
 * Reference to a field or method that also includes invocation instructions.
 *
 * <p>Two instances are defined to be equal if they both refer to the same
 * member and have the same invocation instructions.</p>
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

        private org.objectweb.asm.Handle handle;

        /**
         * Creates a member reference initially referring to the member referred
         * to by the method handle and the invocation instruction of the method
         * handle.
         *
         * @param handle Initial method handle.
         */
        public Handle(org.objectweb.asm.Handle handle) {
            this.handle = handle;
        }

        /**
         * Gets a method handle for the member this is object is referring to.
         *
         * @return Method handle representing this object
         */
        public org.objectweb.asm.Handle getMethodHandle() {
            return this.handle;
        }

        @Override
        public boolean isField() {
            return Handles.isField(this.handle);
        }

        @Override
        public int getOpcode() {
            int opcode = Handles.opcodeFromTag(this.handle.getTag());
            if (opcode == 0) {
                throw new MixinTransformerError("Invalid tag " + this.handle.getTag() + " for method handle " + this.handle + ".");
            }
            return opcode;
        }
        
        @Override
        public void setOpcode(int opcode) {
            int tag = Handles.tagFromOpcode(opcode);
            if (tag == 0) {
                throw new MixinTransformerError("Invalid opcode " + Bytecode.getOpcodeName(opcode) + " for method handle " + this.handle + ".");
            }
            this.setHandle(tag, this.handle.getOwner(), this.handle.getName(), this.handle.getDesc(), this.handle.isInterface());
        }

        @Override
        public String getOwner() {
            return this.handle.getOwner();
        }

        @Override
        public void setOwner(String owner) {
            this.setHandle(this.handle.getTag(), owner, this.handle.getName(), this.handle.getDesc(), this.handle.isInterface());
        }

        @Override
        public String getName() {
            return this.handle.getName();
        }

        @Override
        public void setName(String name) {
            this.setHandle(this.handle.getTag(), this.handle.getOwner(), name, this.handle.getDesc(), this.handle.isInterface());
        }

        @Override
        public String getDesc() {
            return this.handle.getDesc();
        }

        @Override
        public void setDesc(String desc) {
            this.setHandle(this.handle.getTag(), this.handle.getOwner(), this.handle.getName(), desc, this.handle.isInterface());
        }

        public void setHandle(int tag, String owner, String name, String desc, boolean isInterface) {
            this.handle = new org.objectweb.asm.Handle(tag, owner, name, desc, isInterface);
        }

    }

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
        return String.format("%s for %s.%s%s%s", Bytecode.getOpcodeName(this.getOpcode()), this.getOwner(), this.getName(), this.isField() ? ":" : "",
                this.getDesc());
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
    
}
