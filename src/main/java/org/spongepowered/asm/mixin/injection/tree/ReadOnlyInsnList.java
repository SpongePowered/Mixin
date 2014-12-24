/**
 * This file contributed from LiteLoader. Pending refactor. DO NOT ALTER THIS FILE.
 */

package org.spongepowered.asm.mixin.injection.tree;

import java.util.ListIterator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

/**
 * Read-only wrapper for InsnList
 * 
 * @author Adam Mummery-Smith
 */
public class ReadOnlyInsnList extends InsnList
{
    private InsnList insnList;

    public ReadOnlyInsnList(InsnList insns)
    {
        this.insnList = insns;
    }
    
    void dispose()
    {
        this.insnList = null;
    }

    @Override
    public void set(AbstractInsnNode location, AbstractInsnNode insn)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(AbstractInsnNode insn)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(InsnList insns)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insert(AbstractInsnNode insn)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insert(InsnList insns)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insert(AbstractInsnNode location, AbstractInsnNode insn)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insert(AbstractInsnNode location, InsnList insns)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insertBefore(AbstractInsnNode location, AbstractInsnNode insn)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insertBefore(AbstractInsnNode location, InsnList insns)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(AbstractInsnNode insn)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public AbstractInsnNode[] toArray()
    {
//      throw new UnsupportedOperationException();
        return this.insnList.toArray();
    }

    @Override
    public int size()
    {
        return this.insnList.size();
    }

    @Override
    public AbstractInsnNode getFirst()
    {
        return this.insnList.getFirst();
    }

    @Override
    public AbstractInsnNode getLast()
    {
        return this.insnList.getLast();
    }

    @Override
    public AbstractInsnNode get(int index)
    {
        return this.insnList.get(index);
    }

    @Override
    public boolean contains(AbstractInsnNode insn)
    {
        return this.insnList.contains(insn);
    }

    @Override
    public int indexOf(AbstractInsnNode insn)
    {
        return this.insnList.indexOf(insn);
    }

    @Override
    public ListIterator<AbstractInsnNode> iterator()
    {
        return this.insnList.iterator();
    }

    @Override
    public ListIterator<AbstractInsnNode> iterator(int index)
    {
        return this.insnList.iterator(index);
    }

    @Override
    public void resetLabels()
    {
        this.insnList.resetLabels();
    }
}
