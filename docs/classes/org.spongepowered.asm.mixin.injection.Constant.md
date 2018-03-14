[< Back](../README.md)
# public interface Constant Constant #
>#### Class Overview ####
>Annotation for specifying the injection point for an {@link ModifyConstant}
 injector. Leaving all values unset causes the injection point to match all
 constants with the same type as the {@link ModifyConstant} handler's return
 type.
 
 <p>To match a specific constant, specify the appropriate value for the
 appropriate argument. Specifying values of different types will cause an
 error to be raised by the injector.</p>
## Methods ##
### public boolean nullValue () ###
>#### Method Overview ####
>Causes this injector to match <tt>ACONST_NULL</tt> (null object) literals
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true to match <tt>null</tt>
>
### public int intValue () ###
>#### Method Overview ####
>Specify an integer constant to match, includes byte and short values.
 
 <p><b>Special note for referencing <tt>0</tt> (zero) which forms part of
 a comparison expression:</b> See the {@link #expandZeroConditions} option
 below.</p>
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;integer value to match
>
### public float floatValue () ###
>#### Method Overview ####
>Specify a float constant to match
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;float value to match
>
### public long longValue () ###
>#### Method Overview ####
>Specify a long constant to match
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;long value to match
>
### public double doubleValue () ###
>#### Method Overview ####
>Specify a double constant to match
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;double value to match
>
### public String stringValue () ###
>#### Method Overview ####
>Specify a String constant to match
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;string value to match
>
### public Class classValue () ###
>#### Method Overview ####
>Specify a type literal to match
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type literal to match
>
### public int ordinal () ###
>#### Method Overview ####
>Ordinal offset. Many InjectionPoints will return every opcode matching
 their criteria, specifying <em>ordinal</em> allows a particular opcode to
 be identified from the returned list. The default value of -1 does not
 alter the behaviour and returns all matching opcodes. Specifying a value
 of 0 or higher returns <em>only</em> the requested opcode (if one exists:
 for example specifying an ordinal of 4 when only 2 opcodes are matched by
 the InjectionPoint is not going to work particularly well!)
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ordinal value for supported InjectionPoint types
>
### public String slice () ###
>#### Method Overview ####
>This specifies the ID of the slice to use for this query.
 
 <p>For more details see the {@link Slice#id}</p>
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the slice identifier, or empty string to use the default slice
>
### public Condition expandZeroConditions () ###
>#### Method Overview ####
>Whilst most constants can be located in the compiled method with relative
 ease, there exists a special case when a <tt>zero</tt> is used in a
 conditional expression. For example:
 
 <blockquote><code>if (x &gt;= 0)</code></blockquote>
 
 <p>This special case occurs because java includes explicit instructions
 for this type of comparison, and thus the compiled code might look more
 like this:</p>
 
 <blockquote><code>if (x.isGreaterThanOrEqualToZero())</code></blockquote>
 
 <p>Of course if we know that the constant we are searching for is part of
 a comparison, then we can explicitly search for the
 <tt>isGreaterThanOrEqualToZero</tt> and convert it back to the original
 form in order to redirect it just like any other constant access.</p>
 
 <p>To enable this behaviour, you may specify one or more values for this
 argument based on the type of expression you wish to expand. Since the
 Java compiler is wont to compile certain expressions as the <i>inverse
 </i> of their source-level counterparts (eg. compiling a <em>do this if
 greater than</em> structure to a <em>ignore this if less than or equal
 </em> structure); specifying a particular expression type implicitly
 includes the inverse expression as well.</p>
 
 <p>It is worth noting that the effect on ordinals may be hard to predict,
 and thus care should be taken to ensure that the selected injection
 points match the expected locations.</p>
 
 <p>Specifying this option has the following effects:</p>
 
 <ul>
   <li>Matching conditional opcodes in the target method are identified
     for injection candidacy.</li>
   <li>An <tt>intValue</tt> of <tt>0</tt> is implied and does not need to
     be explicitly defined.</li>
   <li>However, explicitly specifying an <tt>intValue</tt> of <tt>0</tt>
     will cause this selector to also match explicit <tt>0</tt> constants
     in the method body as well.</li>
 </ul>
>
### public boolean log () ###
>#### Method Overview ####
>No description provided
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true to enable verbose debug logging for this injection point
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)