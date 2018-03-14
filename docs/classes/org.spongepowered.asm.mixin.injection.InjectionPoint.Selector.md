[< Back](../README.md)
# InjectionPoint.Selector #
>#### Class Overview ####
>Selector type for slice delmiters, ignored for normal injection points.
 <tt>Selectors</tt> can be supplied in {@link At} annotations by including
 a colon (<tt>:</tt>) character followed by the selector type
 (case-sensitive), eg:
 
 <blockquote><pre>&#064;At(value = "INVOKE:LAST", ... )</pre></blockquote>
## Fields ##
### public static final Selector FIRST ###
>#### Field Overview ####
>Use the <em>first</em> instruction from the query result.
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Selector LAST ###
>#### Field Overview ####
>Use the <em>last</em> instruction from the query result.
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Selector ONE ###
>#### Field Overview ####
>The query <b>must return exactly one</b> instruction, if it returns
 more than one instruction this should be considered a fail-fast error
 state and a runtime exception will be thrown.
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Selector DEFAULT ###
>#### Field Overview ####
>Default selector type used if no selector is explicitly specified.
 <em>For internal use only. Currently {@link #FIRST}</em>
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Methods ##
### public static Selector values () ###
>#### Method Overview ####
>No description provided
>
### public static Selector valueOf (String) ###
>#### Method Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)