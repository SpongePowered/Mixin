[< Back](../README.md)
# BeforeLoadLocal #
>#### Class Overview ####
><p>This injection point is a companion for the {@link ModifyVariable}
 injector which searches for LOAD operations which match the local variables
 described by the injector's defined discriminators.</p>
 
 <p>This allows you consumers to specify an injection immediately before a
 local variable is accessed in a method. Specify an <tt>ordinal</tt> of <tt>n
 </tt> to match the <em>n + 1<sup>th</sup></em> access of the variable in
 question.</p>
 
 <dl>
   <dt>ordinal</dt>
   <dd>The ordinal position of the LOAD opcode for the matching local variable
   to search for, if not specified then the injection point returns <em>all
   </em> opcodes for which the parent annotation's discriminators match. The
   default value is <b>-1</b> which supresses ordinal checking.</dd>
 </dl>
 
 <p>Example:</p>
 <blockquote><pre>
   &#064;ModifyVariable(
       method = "md",
       ordinal = 1,
       at = &#064;At(
           value = "LOAD",
           ordinal = 0
       )
   )</pre>
 </blockquote>
 <p>Note that if <em>value</em> is the only parameter specified, it can be
 omitted:</p> 
 <blockquote><pre>
   &#064;At("LOAD")</pre>
 </blockquote>
## Constructors ##
### protected BeforeLoadLocal (InjectionPointData) ###
>#### Constructor Overview ####
>No description provided
>
### protected BeforeLoadLocal (InjectionPointData, int, boolean) ###
>#### Constructor Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)