[< Back](../README.md)
# public BeforeFieldAccess BeforeFieldAccess #
>#### Class Overview ####
><p>This injection point searches for GETFIELD and PUTFIELD (and static
 equivalent) opcodes matching its arguments and returns a list of insns
 immediately prior to matching instructions. It accepts the following
 parameters from {@link At}:
 </p>
 
 <dl>
   <dt>target</dt>
   <dd>A {@link MemberInfo MemberInfo} which identifies the target field.</dd>
   <dt>opcode</dt>
   <dd>The {@link Opcodes opcode} of the field access, must be one of
   GETSTATIC, PUTSTATIC, GETFIELD or PUTFIELD.</dd>
   <dt>ordinal</dt>
   <dd>The ordinal position of the field access to match. For example if the
   field is referenced 3 times and you want to match the 3rd then you can
   specify an <em>ordinal</em> of <b>2</b> (ordinals are zero-indexed). The
   default value is <b>-1</b> which supresses ordinal matching</dd>
 </dl>
 
 <p>Example:</p>
 <blockquote><pre>
   &#064;At(value = "FIELD", target="field_59_z:I", opcode = Opcodes.GETFIELD)
 </pre>
 </blockquote>
 
 <p>Matching array access:</p>
 <p>For array fields, it is possible to match field accesses followed by a
 corresponding array element <em>get</em>, <em>set</em> or <em>length</em>
 operation. To enable this behaviour specify the <tt>array</tt> named-argument
 with the desired operation:</p> 
 
 <blockquote><pre>
   &#064;At(value = "FIELD", target="myIntArray:[I", args = "array=get")
 </pre>
 </blockquote>
 
 <p>See {@link Redirect} for information on array element redirection.</p>
 
 <p>Note that like all standard injection points, this class matches the insn
 itself, putting the injection point immediately <em>before</em> the access in
 question. Use {@link At#shift} specifier to adjust the matched opcode as
 necessary.</p>
## Fields ##
### public static final int ARRAY_SEARCH_FUZZ_DEFAULT ###
>#### Field Overview ####
>Default fuzz factor for searching for array access opcodes
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;8
>
## Constructors ##
### public BeforeFieldAccess (InjectionPointData) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public int getFuzzFactor () ###
>#### Method Overview ####
>No description provided
>
### public int getArrayOpcode () ###
>#### Method Overview ####
>No description provided
>
### protected boolean matchesInsn (AbstractInsnNode) ###
>#### Method Overview ####
>No description provided
>
### protected boolean addInsn (InsnList, Collection, AbstractInsnNode) ###
>#### Method Overview ####
>No description provided
>
### public static AbstractInsnNode findArrayNode (InsnList, FieldInsnNode, int, int) ###
>#### Method Overview ####
>Searches for an array access instruction in the supplied instruction list
 which is within <tt>searchRange</tt> instructions of the supplied field
 instruction. Searching halts if the search range is exhausted, if an
 {@link Opcodes#ARRAYLENGTH} opcode is encountered immediately after the
 specified access, if a matching field access is found, or if the end of 
 the method is reached.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;matching opcode or <tt>null</tt> if not matched
>
>### Parameters ###
>**insns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instruction list to search
>
>**fieldNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Field instruction to search from
>
>**opcode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;array access opcode to search for
>
>**searchRange**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;search range
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)