[< Back](../README.md)
# GlobalProperties #
>#### Class Overview ####
>Access to underlying global property service provided by the current
 environment
## Methods ##
### public static Object get (String) ###
>#### Method Overview ####
>Get a value from the blackboard and duck-type it to the specified type
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;value
>
>### Parameters ###
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;blackboard key
>
### public static void put (String, Object) ###
>#### Method Overview ####
>Put the specified value onto the blackboard
>
>### Parameters ###
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;blackboard key
>
>**value**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new value
>
### public static Object get (String, T) ###
>#### Method Overview ####
>Get the value from the blackboard but return <tt>defaultValue</tt> if the
 specified key is not set.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;value from blackboard or default value
>
>### Parameters ###
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;blackboard key
>
>**defaultValue**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;value to return if the key is not set or is null
>
### public static String getString (String, String) ###
>#### Method Overview ####
>Get a string from the blackboard, returns default value if not set or
 null.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;value from blackboard or default
>
>### Parameters ###
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;blackboard key
>
>**defaultValue**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;default value to return if the specified key is not
      set or is null
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)