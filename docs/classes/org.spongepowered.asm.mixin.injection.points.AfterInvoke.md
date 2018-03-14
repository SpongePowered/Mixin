[< Back](../README.md)
# public AfterInvoke AfterInvoke #
>#### Class Overview ####
><p>This injection point searches for INVOKEVIRTUAL, INVOKESTATIC and
 INVOKESPECIAL opcodes matching its arguments and returns a list of insns
 after the matching instructions, with special handling for methods
 invocations which return a value and immediately assign it to a local
 variable. It accepts the following parameters from
 {@link org.spongepowered.asm.mixin.injection.At At}:</p>
 
 <dl>
   <dt>target</dt>
   <dd>A
   {@link org.spongepowered.asm.mixin.injection.struct.MemberInfo MemberInfo}
   which identifies the target method</dd>
   <dt>ordinal</dt>
   <dd>The ordinal position of the method invocation to match. For example if
   the method is invoked 3 times and you want to match the 3rd then you can
   specify an <em>ordinal</em> of <b>2</b> (ordinals are zero-indexed). The
   default value is <b>-1</b> which supresses ordinal matching</dd>
 </dl>
 
 <p>Example:</p>
 <blockquote><pre>
   &#064;At(value = "INVOKE_ASSIGN", target="func_1234_a(III)J")</pre>
 </blockquote> 
 
 <p>Note that unlike other standard injection points, this class matches the
 insn after the invocation, and after any local variable assignment. Use the
 {@link org.spongepowered.asm.mixin.injection.At#shift shift} specifier to
 adjust the matched opcode as necessary.</p>
## Constructors ##
### public AfterInvoke (InjectionPointData) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### protected boolean addInsn (InsnList, Collection, AbstractInsnNode) ###
>#### Method Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)