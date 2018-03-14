[< Back](../README.md)
# public BeforeNew BeforeNew #
>#### Class Overview ####
><p>This injection point searches for NEW opcodes matching its arguments and
 returns a list of insns immediately prior to matching instructions. It
 accepts the following parameters from
 {@link org.spongepowered.asm.mixin.injection.At At}:</p>
 
 <dl>
   <dt><em>named argument</em> class (or specify using <tt>target</tt></dt>
   <dd>The value of the NEW node to look for, the fully-qualified class name
   </dd>
   <dt>ordinal</dt>
   <dd>The ordinal position of the NEW opcode to match. For example if the NEW
   opcode appears 3 times in the method and you want to match the 3rd then you
   can specify an <em>ordinal</em> of <b>2</b> (ordinals are zero-indexed).
   The default value is <b>-1</b> which supresses ordinal matching</dd>
   <dt>target</dt>
   <dd>Target class can also be specified in <tt>target</tt> which also
   supports specifying the exact signature of the constructor to target. In
   this case the <em>target type</em> is specified as the return type of the
   constructor (in place of the usual <tt>V</tt> (void)) and no owner or name
   should be specified (they are ignored).</dd>
 </dl>
 
 <p>Examples:</p>
 <blockquote><pre>
   // Find all NEW opcodes for <tt>String</tt>
   &#064;At(value = "NEW", args = "class=java/lang/String")</pre>
 </blockquote> 
 <blockquote><pre>
   // Find all NEW opcodes for <tt>String</tt>
   &#064;At(value = "NEW", target = "java/lang/String"</pre>
 </blockquote> 
 <blockquote><pre>
   // Find all NEW opcodes for <tt>String</tt> which are constructed using the
   // ctor which takes an array of <tt>char</tt>
   &#064;At(value = "NEW", target = "([C)Ljava/lang/String;"</pre>
 </blockquote> 
 
 <p>Note that like all standard injection points, this class matches the insn
 itself, putting the injection point immediately <em>before</em> the access in
 question. Use {@link org.spongepowered.asm.mixin.injection.At#shift shift}
 specifier to adjust the matched opcode as necessary.</p>
## Constructors ##
### public BeforeNew (InjectionPointData) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public boolean hasDescriptor () ###
>#### Method Overview ####
>Returns whether this injection point has a constructor descriptor defined
>
### public boolean find (String, InsnList, Collection) ###
>#### Method Overview ####
>No description provided
>
### protected boolean findCtor (InsnList, TypeInsnNode) ###
>#### Method Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)