[< Back](../README.md)
# MixinEnvironment.Phase #
>#### Class Overview ####
>Environment phase, deliberately not implemented as an enum
## Fields ##
### public static final Phase PREINIT ###
>#### Field Overview ####
>"Pre initialisation" phase, everything before the tweak system begins
 to load the game
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Phase INIT ###
>#### Field Overview ####
>"Initialisation" phase, after FML's deobf transformer has loaded
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Phase DEFAULT ###
>#### Field Overview ####
>"Default" phase, during runtime
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Methods ##
### public String toString () ###
>#### Method Overview ####
>No description provided
>
### public static Phase forName (String) ###
>#### Method Overview ####
>Get a phase by name, returns <tt>null</tt> if no phases exist with
 the specified name
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;phase object or <tt>null</tt> if non existent
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;phase name to lookup
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)