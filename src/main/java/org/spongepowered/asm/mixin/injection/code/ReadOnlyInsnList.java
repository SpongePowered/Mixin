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
package org.spongepowered.asm.mixin.injection.code;

import java.util.ListIterator;

import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;

/**
 * Read-only wrapper for InsnList, defensively passed to InjectionPoint
 * instances so that custom InjectionPoint implementations cannot modify the
 * insn list whilst inspecting it.
 */
class ReadOnlyInsnList extends InsnList {

    private InsnList insnList;

    public ReadOnlyInsnList(InsnList insns) {
        this.insnList = insns;
    }

    void dispose() {
        this.insnList = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.asm.tree.InsnList
     *      #set(org.objectweb.asm.tree.AbstractInsnNode,
     *      org.objectweb.asm.tree.AbstractInsnNode)
     */
    @Override
    public final void set(AbstractInsnNode location, AbstractInsnNode insn) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.asm.tree.InsnList
     *      #add(org.objectweb.asm.tree.AbstractInsnNode)
     */
    @Override
    public final void add(AbstractInsnNode insn) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.asm.tree.InsnList#add(org.objectweb.asm.tree.InsnList)
     */
    @Override
    public final void add(InsnList insns) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.asm.tree.InsnList
     *      #insert(org.objectweb.asm.tree.AbstractInsnNode)
     */
    @Override
    public final void insert(AbstractInsnNode insn) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.asm.tree.InsnList
     *      #insert(org.objectweb.asm.tree.InsnList)
     */
    @Override
    public final void insert(InsnList insns) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.asm.tree.InsnList
     *      #insert(org.objectweb.asm.tree.AbstractInsnNode,
     *      org.objectweb.asm.tree.AbstractInsnNode)
     */
    @Override
    public final void insert(AbstractInsnNode location, AbstractInsnNode insn) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.asm.tree.InsnList
     *      #insert(org.objectweb.asm.tree.AbstractInsnNode,
     *      org.objectweb.asm.tree.InsnList)
     */
    @Override
    public final void insert(AbstractInsnNode location, InsnList insns) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.asm.tree.InsnList
     *      #insertBefore(org.objectweb.asm.tree.AbstractInsnNode,
     *      org.objectweb.asm.tree.AbstractInsnNode)
     */
    @Override
    public final void insertBefore(AbstractInsnNode location, AbstractInsnNode insn) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.asm.tree.InsnList
     *      #insertBefore(org.objectweb.asm.tree.AbstractInsnNode,
     *      org.objectweb.asm.tree.InsnList)
     */
    @Override
    public final void insertBefore(AbstractInsnNode location, InsnList insns) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.asm.tree.InsnList
     *      #remove(org.objectweb.asm.tree.AbstractInsnNode)
     */
    @Override
    public final void remove(AbstractInsnNode insn) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.asm.tree.InsnList#toArray()
     */
    @Override
    public AbstractInsnNode[] toArray() {
        return this.insnList.toArray();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.asm.tree.InsnList#size()
     */
    @Override
    public int size() {
        return this.insnList.size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.asm.tree.InsnList#getFirst()
     */
    @Override
    public AbstractInsnNode getFirst() {
        return this.insnList.getFirst();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.asm.tree.InsnList#getLast()
     */
    @Override
    public AbstractInsnNode getLast() {
        return this.insnList.getLast();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.asm.tree.InsnList#get(int)
     */
    @Override
    public AbstractInsnNode get(int index) {
        return this.insnList.get(index);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.asm.tree.InsnList
     *      #contains(org.objectweb.asm.tree.AbstractInsnNode)
     */
    @Override
    public boolean contains(AbstractInsnNode insn) {
        return this.insnList.contains(insn);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.asm.tree.InsnList
     *      #indexOf(org.objectweb.asm.tree.AbstractInsnNode)
     */
    @Override
    public int indexOf(AbstractInsnNode insn) {
        return this.insnList.indexOf(insn);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.asm.tree.InsnList#iterator()
     */
    @Override
    public ListIterator<AbstractInsnNode> iterator() {
        return this.insnList.iterator();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.asm.tree.InsnList#iterator(int)
     */
    @Override
    public ListIterator<AbstractInsnNode> iterator(int index) {
        return this.insnList.iterator(index);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.asm.tree.InsnList#resetLabels()
     */
    @Override
    public final void resetLabels() {
        this.insnList.resetLabels();
    }
}
