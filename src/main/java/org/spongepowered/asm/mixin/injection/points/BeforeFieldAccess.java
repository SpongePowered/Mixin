/**
 * This file contributed from LiteLoader. Pending refactor. DO NOT ALTER THIS FILE.
 */

package org.spongepowered.asm.mixin.injection.points;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;

/**
 * An injection point which searches for GETFIELD and SETFIELD opcodes matching its arguments and returns a list of insns
 * immediately prior to matching instructions. Only the field name is required, owners and signatures are optional and can
 * be used to disambiguate between fields of the same name but with different types, or belonging to different classes.
 * 
 * @author Adam Mummery-Smith
 */
public class BeforeFieldAccess extends BeforeInvoke
{
    @SuppressWarnings("hiding")
    public static final String CODE = "FIELD";
    
	private final int opcode;
	
	public BeforeFieldAccess(InjectionPointData data)
	{
		super(data);
		this.opcode = data.getOpcode();
	}
	
	@Override
	protected boolean matchesInsn(AbstractInsnNode insn)
	{
		return insn instanceof FieldInsnNode && ((FieldInsnNode)insn).getOpcode() == this.opcode;
	}
}
