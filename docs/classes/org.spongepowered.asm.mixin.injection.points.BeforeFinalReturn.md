[< Back](../README.md)
# public BeforeFinalReturn BeforeFinalReturn #
>#### Class Overview ####
><p>This injection point searches for the last RETURN opcode in the target
 method and returns it. Note that the last RETURN opcode may not correspond to
 the notional "bottom" of a method in the original Java source, since
 conditional expressions can cause the bytecode emitted to differ
 significantly in order from the original Java.</p>
 
 <p>Example:</p>
 <blockquote><pre>
   &#064;At(value = "TAIL")</pre>
 </blockquote>
 <p>Note that if <em>value</em> is the only parameter specified, it can be
 omitted:</p> 
 <blockquote><pre>
   &#064;At("TAIL")</pre>
 </blockquote>
## Constructors ##
### public BeforeFinalReturn (InjectionPointData) ###
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