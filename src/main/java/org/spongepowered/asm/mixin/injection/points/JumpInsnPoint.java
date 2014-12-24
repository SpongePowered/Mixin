/**
 * This file contributed from LiteLoader. Pending refactor. DO NOT ALTER THIS FILE.
 */

package org.spongepowered.asm.mixin.injection.points;

import java.util.Collection;
import java.util.ListIterator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;

//import com.mumfrey.liteloader.transformers.event.Event;
//import com.mumfrey.liteloader.transformers.event.InjectionPoint;

/**
 * An injection point which searches for JUMP opcodes (if, try/catch, continue, break, conditional assignment, etc.)
 * with either a particular opcode or at a particular ordinal in the method body (eg. "the Nth JUMP insn" where N is the
 * ordinal of the instruction). By default it returns all JUMP instructions in a method body.
 * 
 * @author Adam Mummery-Smith
 */
public class JumpInsnPoint extends InjectionPoint
{
    public static final String CODE = "JUMP";

    private final int opCode;
	
	private final int ordinal;
	
	public JumpInsnPoint(InjectionPointData data)
	{
		this.opCode = data.getOpcode();
		this.ordinal = data.getOrdinal();
	}
	
	@Override
	public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) //, Event event)
	{
		boolean found = false;
		int ordinal = 0;
		
		ListIterator<AbstractInsnNode> iter = insns.iterator();
		while (iter.hasNext())
		{
			AbstractInsnNode insn = iter.next();
			
			if (insn instanceof JumpInsnNode && (this.opCode == -1 || insn.getOpcode() == this.opCode))
			{
				if (this.ordinal == -1 || this.ordinal == ordinal)
				{
					nodes.add(insn);
					found = true;
				}
				
				ordinal++;
			}
		}
		
		return found;
	}
}
