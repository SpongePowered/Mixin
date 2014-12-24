/**
 * This file contributed from LiteLoader. Pending refactor. DO NOT ALTER THIS FILE.
 */

package org.spongepowered.asm.mixin.injection.points;

import java.util.Collection;
import java.util.ListIterator;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;

//import com.mumfrey.liteloader.transformers.event.Event;
//import com.mumfrey.liteloader.transformers.event.InjectionPoint;

/**
 * An injection point which searches for RETURN opcodes in the supplied method and either finds all insns or
 * the insn at the specified ordinal 
 * 
 * @author Adam Mummery-Smith
 */
public class BeforeReturn extends InjectionPoint
{
    public static final String CODE = "RETURN";

    private final int ordinal;
	
	public BeforeReturn(InjectionPointData data)
	{
		this.ordinal = data.getOrdinal();
	}

	@Override
	public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) //, Event event)
	{
		boolean found = false;
		int returnOpcode = Type.getReturnType(desc).getOpcode(Opcodes.IRETURN);
		int ordinal = 0;
		
		ListIterator<AbstractInsnNode> iter = insns.iterator();
		while (iter.hasNext())
		{
			AbstractInsnNode insn = iter.next();
			
			if (insn instanceof InsnNode && insn.getOpcode() == returnOpcode)
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
