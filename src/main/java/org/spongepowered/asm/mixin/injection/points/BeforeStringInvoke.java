/**
 * This file contributed from LiteLoader. Pending refactor. DO NOT ALTER THIS FILE.
 */

package org.spongepowered.asm.mixin.injection.points;

import java.util.Collection;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;

//import com.mumfrey.liteloader.transformers.event.Event;
//import com.mumfrey.liteloader.transformers.event.MethodInfo;
//import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * An injection point which searches for a matching String LDC insn immediately prior to a qualifying invoke
 *  
 * @author Adam Mummery-Smith
 */
public class BeforeStringInvoke extends BeforeInvoke
{
    @SuppressWarnings("hiding")
    public static final String CODE = "INVOKE_STRING";

//	private static final String STRING_VOID_SIG = "(Ljava/lang/String;)V";
	
	private final String ldcValue;
	
	private boolean foundLdc;
	
//	public BeforeStringInvoke(String ldcValue, String[] methodNames, String[] methodOwners)
//	{
//		this(ldcValue, methodNames, methodOwners, -1);
//	}
	
	public BeforeStringInvoke(InjectionPointData data)
	{
		super(data);
		this.ldcValue = data.get("ldc", "");

//		for (int i = 0; i < this.methodSignatures.length; i++)
//			if (!STRING_VOID_SIG.equals(this.methodSignatures[i]))
//				throw new IllegalArgumentException("BeforeStringInvoke requires method with with signature " + STRING_VOID_SIG);
	}
	
	@Override
	public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) //, Event event)
	{
		this.foundLdc = false;
		
		return super.find(desc, insns, nodes); //, event);
	}
	
	@Override
	protected void inspectInsn(String desc, InsnList insns, AbstractInsnNode insn)
	{
		if (insn instanceof LdcInsnNode)
		{
			LdcInsnNode node = (LdcInsnNode)insn;
			if (node.cst instanceof String && this.ldcValue.equals(node.cst))
			{
//				if (this.logging) LiteLoaderLogger.info("BeforeInvoke found a matching LDC with value %s", node.cst);
				this.foundLdc = true;
				return;
			}
		}
		
		this.foundLdc = false;
	}
	
	@Override
	protected boolean matchesInsn(InsnInfo nodeInfo, int ordinal)
	{
//		if (this.logging) LiteLoaderLogger.debug("BeforeInvoke       foundLdc \"%s\" = %s", this.ldcValue, this.foundLdc);
		return this.foundLdc && super.matchesInsn(nodeInfo, ordinal);
	}
}
