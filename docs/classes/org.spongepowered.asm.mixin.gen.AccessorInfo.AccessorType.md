[< Back](../README.md)
# public static AccessorType AccessorInfo.AccessorType #
>#### Class Overview ####
>Accessor types
## Fields ##
### public static final AccessorType FIELD_GETTER ###
>#### Field Overview ####
>A field getter, accessor must accept no args and return field type
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final AccessorType FIELD_SETTER ###
>#### Field Overview ####
>A field setter, accessor must accept single arg of the field type and
 return void
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final AccessorType METHOD_PROXY ###
>#### Field Overview ####
>An invoker (proxy) method
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Methods ##
### public static AccessorType values () ###
>#### Method Overview ####
>No description provided
>
### public static AccessorType valueOf (String) ###
>#### Method Overview ####
>No description provided
>
### public boolean isExpectedPrefix (String) ###
>#### Method Overview ####
>Returns true if the supplied prefix string is an allowed prefix for
 this accessor type
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if the expected prefix set contains the supplied value
>
>### Parameters ###
>**prefix**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;prefix to check
>
### public String getExpectedPrefixes () ###
>#### Method Overview ####
>Returns all the expected prefixes for this accessor type as a string
 for debugging/error message purposes
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;string representation of expected prefixes for this accessor
      type
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)