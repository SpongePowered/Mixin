[< Back](../README.md)
# public BeforeReturn BeforeReturn #
>#### Class Overview ####
><p>This injection point searches for RETURN opcodes in the target method and
 returns a list of insns immediately prior to matching instructions. Note that
 <em>every</em> RETURN opcode will be returned and thus every natural exit
 from the method except for exception throws will be implicitly specified. To
 specify a particular RETURN use the <em>ordinal</em> parameter. The injection
 point accepts the following parameters from
 {@link org.spongepowered.asm.mixin.injection.At At}:</p>
 
 <dl>
   <dt>ordinal</dt>
   <dd>The ordinal position of the RETURN opcode to match, if not specified
   then the injection point returns <em>all</em> RETURN opcodes. For example
   if the RETURN opcode appears 3 times in the method and you want to match
   the 3rd then you can specify an <em>ordinal</em> of <b>2</b> (ordinals are
   zero-indexed). The default value is <b>-1</b> which supresses ordinal
   matching</dd>
 </dl>
 
 <p>Example:</p>
 <blockquote><pre>
   &#064;At(value = "RETURN")</pre>
 </blockquote>
 <p>Note that if <em>value</em> is the only parameter specified, it can be
 omitted:</p> 
 <blockquote><pre>
   &#064;At("RETURN")</pre>
 </blockquote>
## Constructors ##
### public BeforeReturn (InjectionPointData) ###
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