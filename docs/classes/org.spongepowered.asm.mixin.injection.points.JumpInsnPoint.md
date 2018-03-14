[< Back](../README.md)
# JumpInsnPoint #
>#### Class Overview ####
><p>This injection point searches for JUMP opcodes (if, try/catch, continue,
 break, conditional assignment, etc.) with either a particular opcode or at a
 particular ordinal in the method body (eg. "the Nth JUMP insn" where N is the
 ordinal of the instruction). By default it returns all JUMP instructions in a
 method body. It accepts the following parameters from
 {@link org.spongepowered.asm.mixin.injection.At At}:</p>
 
 <dl>
   <dt>opcode</dt>
   <dd>The {@link Opcodes opcode} of the jump instruction, must be one of
   IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT,
   IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, GOTO, JSR, IFNULL or
   IFNONNULL. Defaults to <b>-1</b> which matches any JUMP opcode.
   </dd>
   <dt>ordinal</dt>
   <dd>The ordinal position of the jump insn to match. For example if there
   are 3 jumps of the specified type and you want to match the 2nd then you
   can specify an <em>ordinal</em> of <b>1</b> (ordinals are zero-indexed).
   The default value is <b>-1</b> which supresses ordinal matching</dd>
 </dl>
 
 <p>Example:</p>
 <blockquote><pre>
   &#064;At(value = "JUMP", opcode = Opcodes.IFLE, ordinal = 2)</pre>
 </blockquote>
 
 <p>Note that like all standard injection points, this class matches the insn
 itself, putting the injection point immediately <em>before</em> the access in
 question. Use {@link org.spongepowered.asm.mixin.injection.At#shift shift}
 specifier to adjust the matched opcode as necessary.</p>
## Constructors ##
### public JumpInsnPoint (InjectionPointData) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public boolean find (String, InsnList, Collection) ###
>#### Method Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)