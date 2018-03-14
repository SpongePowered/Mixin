[< Back](../README.md)
# Interface.Remap #
>#### Class Overview ####
>Describes the remapping strategy applied to methods matching this
 interface.
## Fields ##
### public static final Remap ALL ###
>#### Field Overview ####
>Attempt to remap all members of this interface which are declared in
 the annotated mixin, including non-prefixed methods which match.
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Remap FORCE ###
>#### Field Overview ####
>Attempt to remap all members of this interface which are declared in
 the annotated mixin, including non-prefixed methods which match. <b>
 If mappings are not located for a member method, raise a compile-time
 error.</b>
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Remap ONLY_PREFIXED ###
>#### Field Overview ####
>Remap only methods in the annotated mixin which are prefixed with the
 declared prefix. Note that if no prefix is defined, this has the same
 effect as {@link #NONE}
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Remap NONE ###
>#### Field Overview ####
>Do not remap members matching this interface. (Equivalent to <tt>
 remap=false</tt> on other remappable annotations)
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Methods ##
### public static Remap values () ###
>#### Method Overview ####
>No description provided
>
### public static Remap valueOf (String) ###
>#### Method Overview ####
>No description provided
>
### public boolean forceRemap () ###
>#### Method Overview ####
>Returns whether this remap type should force remapping
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)