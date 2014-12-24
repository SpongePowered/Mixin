/**
 * This file contributed from LiteLoader. Pending refactor. DO NOT ALTER THIS FILE.
 */

package org.spongepowered.asm.mixin.injection.points;

import java.util.Collection;
import java.util.ListIterator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;

//import com.mumfrey.liteloader.transformers.ClassTransformer;
//import com.mumfrey.liteloader.transformers.event.Event;
//import com.mumfrey.liteloader.transformers.event.InjectionPoint;
//import com.mumfrey.liteloader.transformers.event.MethodInfo;
//import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * An injection point which searches for method invokations matching its arguments and returns a list of insns immediately
 * prior to matching invokations. Only the method name is required, owners and signatures are optional and can be used to disambiguate
 * between methods of the same name but with different args, or belonging to different classes.
 * 
 * @author Adam Mummery-Smith
 */
public class BeforeInvoke extends InjectionPoint
{
    public static final String CODE = "INVOKE";

    protected class InsnInfo
	{
		public final String owner;
		public final String name;
		public final String desc;
		
		public InsnInfo(AbstractInsnNode insn)
		{
			if (insn instanceof MethodInsnNode)
			{
				MethodInsnNode methodNode = (MethodInsnNode)insn;
				this.owner = methodNode.owner;
				this.name = methodNode.name;
				this.desc = methodNode.desc;
			}
			else if (insn instanceof FieldInsnNode)
			{
				FieldInsnNode fieldNode = (FieldInsnNode)insn;
				this.owner = fieldNode.owner;
				this.name = fieldNode.name;
				this.desc = fieldNode.desc;
			}
			else
			{
				throw new IllegalArgumentException("insn must be an instance of MethodInsnNode or FieldInsnNode");
			}
		}
	}
	
	protected final MemberInfo target;
	
	/**
	 * This strategy can be used to identify a particular invokation if the same method is invoked at multiple points, if this value is -1
	 * then the strategy returns ALL invokations of the method. 
	 */
	protected final int ordinal;
	
	/**
	 * True to turn on strategy debugging to the console
	 */
	protected boolean logging = false;
	
	protected final String className;
	
	public BeforeInvoke(InjectionPointData data)
	{
		this.target = data.getTarget();
		this.ordinal = data.getOrdinal();
		this.className = this.getClass().getSimpleName();

//		this.convertClassRefs();
	}
	
//	private void convertClassRefs()
//	{
//		for (int i = 0; i < this.methodOwners.length; i++)
//		{
//			if (this.methodOwners[i] != null) this.methodOwners[i] = this.methodOwners[i].replace('.', '/');
//		}
//		
//		if (this.methodSignatures != null)
//		{
//			for (int i = 0; i < this.methodSignatures.length; i++)
//			{
//				if (this.methodSignatures[i] != null) this.methodSignatures[i] = this.methodSignatures[i].replace('.', '/');
//			}
//		}
//	}
	
	public BeforeInvoke setLogging(boolean logging)
	{
		this.logging = logging;
		return this;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.transformers.event.InjectionStrategy#findInjectionPoint(java.lang.String, org.objectweb.asm.tree.InsnList, com.mumfrey.liteloader.transformers.event.Event, java.util.Collection)
	 */
	@Override
	public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) //, Event event)
	{
		int ordinal = 0;
		boolean found = false;

		if (this.logging)
		{
//			LiteLoaderLogger.debug(ClassTransformer.HORIZONTAL_RULE);
//			LiteLoaderLogger.debug(this.className + " is searching for an injection point in method with descriptor %s", desc);
		}
		
		ListIterator<AbstractInsnNode> iter = insns.iterator();
		while (iter.hasNext())
		{
			AbstractInsnNode insn = iter.next();
			
			if (this.matchesInsn(insn))
			{
				InsnInfo nodeInfo = new InsnInfo(insn);
				
//				if (this.logging) LiteLoaderLogger.debug(this.className + " is considering insn NAME=%s DESC=%s OWNER=%s", nodeInfo.name, nodeInfo.desc, nodeInfo.owner);
				
//				int index = BeforeInvoke.arrayIndexOf(this.methodNames, nodeInfo.name, -1);
//				if (index > -1 && this.logging) LiteLoaderLogger.debug(this.className + "   found a matching insn, checking owner/signature...");
				
//				int ownerIndex = BeforeInvoke.arrayIndexOf(this.methodOwners, nodeInfo.owner, index);
//				int descIndex = BeforeInvoke.arrayIndexOf(this.methodSignatures, nodeInfo.desc, index);
				if (this.target.matches(nodeInfo.owner, nodeInfo.name, nodeInfo.desc))
				{
//					if (this.logging) LiteLoaderLogger.debug(this.className + "     found a matching insn, checking preconditions...");
					if (this.matchesInsn(nodeInfo, ordinal))
					{
//						if (this.logging) LiteLoaderLogger.debug(this.className + "         found a matching insn at ordinal %d", ordinal);
						nodes.add(insn);
						found = true;
						
						if (this.ordinal == ordinal)
							break;
					}
					
					ordinal++;
				}
			}

			this.inspectInsn(desc, insns, insn);
		}
		
//		if (this.logging) LiteLoaderLogger.debug(ClassTransformer.HORIZONTAL_RULE);
		
		return found;
	}

	protected boolean matchesInsn(AbstractInsnNode insn)
	{
		return insn instanceof MethodInsnNode;
	}

	protected void inspectInsn(String desc, InsnList insns, AbstractInsnNode insn)
	{
		// stub for subclasses
	}

	protected boolean matchesInsn(InsnInfo nodeInfo, int ordinal)
	{
//		if (this.logging) LiteLoaderLogger.debug(this.className + "       comparing target ordinal %d with current ordinal %d", this.ordinal, ordinal);
		return this.ordinal == -1 || this.ordinal == ordinal;
	}
//
//	/**
//	 * Special version of contains which returns TRUE if the haystack array is null, which is an odd behaviour we actually
//	 * want here because null indicates that the value is not important
//	 * 
//	 * @param haystack
//	 * @param needle
//	 */
//	private static int arrayIndexOf(String[] haystack, String needle, int pos)
//	{
//		if (haystack == null) return pos;
//		if (pos > -1 && pos < haystack.length && needle.equals(haystack[pos])) return pos;
//		
//		for (int index = 0; index < haystack.length; index++)
//			if (needle.equals(haystack[index])) return index;
//		
//		return -1;
//	}
}
