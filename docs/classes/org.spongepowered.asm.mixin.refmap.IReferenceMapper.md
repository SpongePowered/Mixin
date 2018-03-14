[< Back](../README.md)
# public interface IReferenceMapper IReferenceMapper #
>#### Class Overview ####
>Interface for reference mapper objects
## Methods ##
### public boolean isDefault () ###
>#### Method Overview ####
>Get whether this mapper is defaulted. Use this flag rather than reference
 comparison to {@link ReferenceMapper#DEFAULT_MAPPER} because of
 classloader shenanigans
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if this mapper is a defaulted mapper
>
### public String getResourceName () ###
>#### Method Overview ####
>Get the resource name this refmap was loaded from (if available).
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;name of the resource
>
### public String getStatus () ###
>#### Method Overview ####
>Get a user-readable "status" string for this refmap for use in error 
 messages
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;status message
>
### public String getContext () ###
>#### Method Overview ####
>Get the current context
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;current context key, can be null
>
### public void setContext (String) ###
>#### Method Overview ####
>Set the current remap context, can be null
>
>### Parameters ###
>**context**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;remap context
>
### public String remap (String, String) ###
>#### Method Overview ####
>Remap a reference for the specified owning class in the current context
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;remapped reference, returns original reference if not remapped
>
>### Parameters ###
>**className**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Owner class
>
>**reference**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Reference to remap
>
### public String remapWithContext (String, String, String) ###
>#### Method Overview ####
>Remap a reference for the specified owning class in the specified context
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;remapped reference, returns original reference if not remapped
>
>### Parameters ###
>**context**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Remap context to use
>
>**className**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Owner class
>
>**reference**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Reference to remap
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)