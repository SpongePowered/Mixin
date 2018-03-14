[< Back](../README.md)
# BeforeConstant #
>#### Class Overview ####
>Special injection point which can be defined by an {@link Constant}
 annotation or using the <em>at</em> code <tt>CONSTANT</tt>.
 
 <p>This injection point searches for <tt>LDC</tt> and other constant opcodes
 matching its arguments and returns a list of injection points matching those
 instructions. When used with {@link At} it accepts the following parameters:
 </p>
 
 <dl>
   <dt>ordinal</dt>
   <dd>The ordinal position of the constant opcode to match. The default value
   is <b>-1</b> which supresses ordinal matching</dd>
   <dt><em>named argument</em> nullValue</dt>
   <dd>To match <tt>null</tt> literals in the method body, set this to
   <tt>true</tt></dd>
   <dt><em>named argument</em> intValue</dt>
   <dd>To match <tt>int</tt> literals in the method body. See also the
   <em>expandZeroConditions</em> argument below for concerns when matching
   conditional zeroes.</dd>
   <dt><em>named argument</em> floatValue</dt>
   <dd>To match <tt>float</tt> literals in the method body.</dd>
   <dt><em>named argument</em> longValue</dt>
   <dd>To match <tt>long</tt> literals in the method body.</dd>
   <dt><em>named argument</em> doubleValue</dt>
   <dd>To match <tt>double</tt> literals in the method body.</dd>
   <dt><em>named argument</em> stringValue</dt>
   <dd>To match {@link String} literals in the method body.</dd>
   <dt><em>named argument</em> classValue</dt>
   <dd>To match {@link Class} literals in the method body.</dd>
   <dt><em>named argument</em> log</dt>
   <dd>Enable debug logging when searching for matching opcodes.</dd>
   <dt><em>named argument</em> expandZeroConditions</dt>
   <dd>See the {@link Constant#expandZeroConditions} option, this argument
   should be a list of {@link Condition} names</dd>
 </dl>
 
 <p>Examples:</p>
 <blockquote><pre>
   // Find all integer constans with value 4
   &#064;At(value = "CONSTANT", args = "intValue=4")</pre>
 </blockquote> 
 <blockquote><pre>
   // Find the String literal "foo"
   &#064;At(value = "CONSTANT", args = "stringValue=foo"</pre>
 </blockquote> 
 <blockquote><pre>
   // Find all integer constants with value 0 and expand conditionals
   &#064;At(
     value = "CONSTANT",
     args = {
       "intValue=0",
       "expandZeroConditions=LESS_THAN_ZERO,GREATER_THAN_ZERO"
     }
   )
   </pre>
 </blockquote> 
 
 <p>Note that like all standard injection points, this class matches the insn
 itself, putting the injection point immediately <em>before</em> the access in
 question. Use {@link org.spongepowered.asm.mixin.injection.At#shift shift}
 specifier to adjust the matched opcode as necessary.</p>
## Constructors ##
### public BeforeConstant (IMixinContext, AnnotationNode, String) ###
>#### Constructor Overview ####
>No description provided
>
### public BeforeConstant (InjectionPointData) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public boolean find (String, InsnList, Collection) ###
>#### Method Overview ####
>No description provided
>
### protected void log (String, Object[]) ###
>#### Method Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)