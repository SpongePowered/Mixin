/**
 * This file contributed from LiteLoader. Pending refactor. DO NOT ALTER THIS FILE.
 */

package org.spongepowered.asm.mixin.injection.points;

import java.util.Collection;
import java.util.ListIterator;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.TypeInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;

//import com.mumfrey.liteloader.core.runtime.Obf;
//import com.mumfrey.liteloader.transformers.event.Event;
//import com.mumfrey.liteloader.transformers.event.InjectionPoint;

public class BeforeNew extends InjectionPoint
{
    public static final String CODE = "NEW";

    private final String className;
	
	private final int ordinal;
	
	public BeforeNew(InjectionPointData data)
	{
		this.ordinal = data.getOrdinal();
		this.className = data.get("class", "").replace('.', '/');
		
//		for (int i = 0; i < this.classNames.length; i++)
//		{
//			this.classNames[i] = this.classNames[i].replace('.', '/');
//		}
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
			
			if (insn instanceof TypeInsnNode && insn.getOpcode() == Opcodes.NEW && this.matchesOwner((TypeInsnNode)insn))
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

	private boolean matchesOwner(TypeInsnNode insn)
	{
//		for (String className : this.classNames)
//		{
//			if (className.equals(insn.desc)) return true;
//		}
		
		return this.className.equals(insn.desc);
	}

}
