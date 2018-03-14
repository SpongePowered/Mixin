[< Back](../README.md)
# public BeforeInvoke BeforeInvoke #
>#### Class Overview ####
><p>This injection point searches for INVOKEVIRTUAL, INVOKESTATIC and
 INVOKESPECIAL opcodes matching its arguments and returns a list of insns
 immediately prior to matching instructions. It accepts the following
 parameters from {@link At}:</p>
 
 <dl>
   <dt>target</dt>
   <dd>A {@link MemberInfo MemberInfo} which identifies the target method</dd>
   <dt>ordinal</dt>
   <dd>The ordinal position of the method invocation to match. For example if
   the method is invoked 3 times and you want to match the 3rd then you can
   specify an <em>ordinal</em> of <b>2</b> (ordinals are zero-indexed). The
   default value is <b>-1</b> which supresses ordinal matching</dd>
 </dl>
 
 <p>Example:</p>
 <blockquote><pre>
   &#064;At(value = "INVOKE", target="func_1234_a(III)V")</pre>
 </blockquote> 
 
 <p>Note that like all standard injection points, this class matches the insn
 itself, putting the injection point immediately <em>before</em> the access in
 question. Use {@link At#shift} specifier to adjust the matched opcode as
 necessary.</p>
## Fields ##
### protected final MemberInfo target ###
>#### Field Overview ####
>No description provided
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final MemberInfo permissiveTarget ###
>#### Field Overview ####
>No description provided
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final int ordinal ###
>#### Field Overview ####
>This strategy can be used to identify a particular invocation if the same
 method is invoked at multiple points, if this value is -1 then the
 strategy returns <em>all</em> invocations of the method.
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final String className ###
>#### Field Overview ####
>Class name (description) for debug logging
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Constructors ##
### public BeforeInvoke (InjectionPointData) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public BeforeInvoke setLogging (boolean) ###
>#### Method Overview ####
>Set the logging state for this injector
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**logging**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;logging state
>
### public boolean find (String, InsnList, Collection) ###
>#### Method Overview ####
>No description provided
>
### protected boolean find (String, InsnList, Collection, MemberInfo) ###
>#### Method Overview ####
>No description provided
>
### protected boolean addInsn (InsnList, Collection, AbstractInsnNode) ###
>#### Method Overview ####
>No description provided
>
### protected boolean matchesInsn (AbstractInsnNode) ###
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
### protected void log (String, Object[]) ###
>#### Method Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)