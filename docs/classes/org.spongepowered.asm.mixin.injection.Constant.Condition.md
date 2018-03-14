[< Back](../README.md)
# public static final Condition Constant.Condition #
>#### Class Overview ####
>Available options for the {@link Constant#expandZeroConditions} setting.
 Each option matches the inverse instructions as well because in the
 compiled code it is not unusual for <tt>if (x &gt; 0)</tt> to be compiled
 as <tt>if (!(x &lt;= 0))</tt>
 
 <p>Note that all of these options assume that <tt>x</tt> is on the <b>
 left-hand side</b> of the expression in question. For expressions where
 zero is on the <b>right-hand side</b> you should choose the inverse.</p>
## Fields ##
### public static final Condition LESS_THAN_ZERO ###
>#### Field Overview ####
>Match &lt; operators and &gt;= instructions:
 
 <code>x &lt; 0</code>
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Condition LESS_THAN_OR_EQUAL_TO_ZERO ###
>#### Field Overview ####
>Match &lt;= operators and &gt; instructions
 
 <code>x &lt;= 0</code>
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Condition GREATER_THAN_OR_EQUAL_TO_ZERO ###
>#### Field Overview ####
>Match &gt;= operators and &lt; instructions, equivalent to
 {@link #LESS_THAN_ZERO}
 
 <code>x &gt;= 0</code>
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Condition GREATER_THAN_ZERO ###
>#### Field Overview ####
>Match &gt; operators and &lt;= instructions, equivalent to
 {@link #LESS_THAN_OR_EQUAL_TO_ZERO}
 
 <code>x &gt; 0</code>
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Methods ##
### public static Condition values () ###
>#### Method Overview ####
>No description provided
>
### public static Condition valueOf (String) ###
>#### Method Overview ####
>No description provided
>
### public Condition getEquivalentCondition () ###
>#### Method Overview ####
>Get the condition which is equivalent to this condition
>
### public int getOpcodes () ###
>#### Method Overview ####
>Get the opcodes for this condition
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)