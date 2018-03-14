[< Back](../README.md)
# MixinEnvironment.CompatibilityLevel #
>#### Class Overview ####
>Operational compatibility level for the mixin subsystem
## Fields ##
### public static final CompatibilityLevel JAVA_6 ###
>#### Field Overview ####
>Java 6 and above
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final CompatibilityLevel JAVA_7 ###
>#### Field Overview ####
>Java 7 and above
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final CompatibilityLevel JAVA_8 ###
>#### Field Overview ####
>Java 8 and above
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final CompatibilityLevel JAVA_9 ###
>#### Field Overview ####
>Java 9 and above
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Methods ##
### public static CompatibilityLevel values () ###
>#### Method Overview ####
>No description provided
>
### public static CompatibilityLevel valueOf (String) ###
>#### Method Overview ####
>No description provided
>
### public int classVersion () ###
>#### Method Overview ####
>Class version expected at this compatibility level
>
### public boolean supportsMethodsInInterfaces () ###
>#### Method Overview ####
>Get whether this environment supports non-abstract methods in
 interfaces, true in Java 1.8 and above
>
### public boolean isAtLeast (MixinEnvironment.CompatibilityLevel) ###
>#### Method Overview ####
>Get whether this level is the same or greater than the specified
 level
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if this level is equal or higher the supplied level
>
>### Parameters ###
>**level**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;level to compare to
>
### public boolean canElevateTo (MixinEnvironment.CompatibilityLevel) ###
>#### Method Overview ####
>Get whether this level can be elevated to the specified level
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if this level supports elevation
>
>### Parameters ###
>**level**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;desired level
>
### public boolean canSupport (MixinEnvironment.CompatibilityLevel) ###
>#### Method Overview ####
>True if this level can support the specified level
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if the other level can be elevated to this level
>
>### Parameters ###
>**level**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;desired level
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)