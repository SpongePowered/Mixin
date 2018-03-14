[< Back](../README.md)
# BeforeStringInvoke #
>#### Class Overview ####
><p>Like {@link BeforeInvoke}, this injection point searches for
 INVOKEVIRTUAL, INVOKESTATIC and INVOKESPECIAL opcodes matching its arguments
 and returns a list of insns immediately prior to matching instructions. This
 specialised version however only matches methods which accept a single string
 and return void, but allows the string itself to be matched as part of the
 search process. This is primarily used for matching particular invocations of
 <em>Profiler::startSection</em> with a specific argument. Note that because a
 string literal is required, this injection point can not be used to match
 invocations where the value being passed in is a variable.</p>
 
 <p>To be precise, this injection point matches invocations of the specified
 method which are preceded by an LDC instruction. The LDC instruction's
 payload can be specified with the <b>ldc</b> named argument (see below)</p>
 
 <p>The following parameters from
 {@link org.spongepowered.asm.mixin.injection.At At} are accepted</p>
 
 <dl>
   <dt>target</dt>
   <dd>A
   {@link org.spongepowered.asm.mixin.injection.struct.MemberInfo MemberInfo}
   which identifies the target method, the method <b>must</b> be specified
   with a signature which accepts a single string and returns void,
   eg. <code>(Ljava/lang/String;)V</code></dd>
   <dt>ordinal</dt>
   <dd>The ordinal position of the method invocation to match. For example if
   the method is invoked 3 times and you want to match the 3rd then you can
   specify an <em>ordinal</em> of <b>2</b> (ordinals are zero-indexed). The
   default value is <b>-1</b> which supresses ordinal matching</dd>
   <dt><em>named argument</em> ldc</dt>
   <dd>The value of the LDC node to look for prior to the method invocation
   </dd>
 </dl>
 
 <p>Example:</p>
 <blockquote><pre>
   &#064;At(value = "INVOKE_STRING",
      target="startSection(Ljava/lang/String;)V", args = { "ldc=root" })</pre>
 </blockquote>
 <p>Notice the use of the <em>named argument</em> "ldc" which specifies the
 value of the target LDC node.</p> 
 
 <p>Note that like all standard injection points, this class matches the insn
 itself, putting the injection point immediately <em>before</em> the access in
 question. Use {@link org.spongepowered.asm.mixin.injection.At#shift shift}
 specifier to adjust the matched opcode as necessary.</p>
## Constructors ##
### public BeforeStringInvoke (InjectionPointData) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public boolean find (String, InsnList, Collection) ###
>#### Method Overview ####
>No description provided
>
### protected void inspectInsn (String, InsnList, AbstractInsnNode) ###
>#### Method Overview ####
>No description provided
>
### protected boolean matchesInsn (MemberInfo, int) ###
>#### Method Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)