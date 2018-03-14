[< Back](../README.md)
# public interface IGlobalPropertyService IGlobalPropertyService #
>#### Class Overview ####
>Global property service
## Methods ##
### public Object getProperty (String) ###
>#### Method Overview ####
>Get a value from the global property store (blackboard) and duck-type it
 to the specified type
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;value
>
>### Parameters ###
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;blackboard key
>
### public void setProperty (String, Object) ###
>#### Method Overview ####
>Set the specified value in the global property store (blackboard)
>
>### Parameters ###
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;blackboard key
>
>**value**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new value
>
### public Object getProperty (String, T) ###
>#### Method Overview ####
>Get the value from the global property store (blackboard) but return
 <tt>defaultValue</tt> if the specified key is not set.
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
### public String getPropertyString (String, String) ###
>#### Method Overview ####
>Get a string from the global property store (blackboard), returns default
 value if not set or null.
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