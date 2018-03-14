[< Back](../README.md)
# public static final Visibility Bytecode.Visibility #
>#### Class Overview ####
>Ordinal member visibility level. This is used to represent visibility of
 a member in a formal way from lowest to highest. The
 {@link Bytecode#getVisibility} methods can be used to convert access
 flags to this enum. The value returned from {@link #ordinal} can then be
 used to determine whether a visibility level is <i>higher</i> or <i>lower
 </i> than any other given visibility level.
## Fields ##
### public static final Visibility PRIVATE ###
>#### Field Overview ####
>Members decorated with {@link Opcodes#ACC_PRIVATE}
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Visibility PROTECTED ###
>#### Field Overview ####
>Members decorated with {@link Opcodes#ACC_PROTECTED}
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Visibility PACKAGE ###
>#### Field Overview ####
>Members not decorated with any access flags
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Visibility PUBLIC ###
>#### Field Overview ####
>Members decorated with {@link Opcodes#ACC_PUBLIC}
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Methods ##
### public static Visibility values () ###
>#### Method Overview ####
>No description provided
>
### public static Visibility valueOf (String) ###
>#### Method Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)