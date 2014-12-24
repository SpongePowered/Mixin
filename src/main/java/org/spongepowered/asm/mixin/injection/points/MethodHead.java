/**
 * This file contributed from LiteLoader. Pending refactor. DO NOT ALTER THIS FILE.
 */

package org.spongepowered.asm.mixin.injection.points;

import java.util.Collection;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;

//import com.mumfrey.liteloader.transformers.event.Event;
//import com.mumfrey.liteloader.transformers.event.InjectionPoint;

/**
 * An injection point which locates the first instruction in a method body
 *  
 * @author Adam Mummery-Smith
 */
public class MethodHead extends InjectionPoint
{
    public static final String CODE = "HEAD";

    public MethodHead(InjectionPointData data)
	{
	}
	
	@Override
	public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) //, Event event)
	{
		nodes.add(insns.getFirst());
		return true;
	}
}
