[< Back](../README.md)
# public final TargetMap TargetMap #
>#### Class Overview ####
>Serialisable map of classes to their associated mixins, used so that we can
 pass target information for supermixins from one compiler session to another
## Methods ##
### public String getSessionId () ###
>#### Method Overview ####
>Get the session ID
>
### public void registerTargets (AnnotatedMixin) ###
>#### Method Overview ####
>Register target classes for the specified mixin
>
>### Parameters ###
>**mixin**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mixin to add targets for
>
### public void registerTargets (List, TypeHandle) ###
>#### Method Overview ####
>Register target classes for the supplied mixin
>
>### Parameters ###
>**targets**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;List of targets
>
>**mixin**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Mixin class
>
### public void addMixin (TypeHandle, TypeHandle) ###
>#### Method Overview ####
>Register the specified mixin against the specified target
>
>### Parameters ###
>**target**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target class
>
>**mixin**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Mixin class
>
### public void addMixin (String, String) ###
>#### Method Overview ####
>Register the specified mixin against the specified target
>
>### Parameters ###
>**target**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target class
>
>**mixin**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Mixin class
>
### public void addMixin (TypeReference, TypeReference) ###
>#### Method Overview ####
>Register the specified mixin against the specified target
>
>### Parameters ###
>**target**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target class
>
>**mixin**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Mixin class
>
### public Collection getMixinsTargeting (TypeElement) ###
>#### Method Overview ####
>Get mixin classes which target the specified class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Collection of mixins registered as targetting the specified class
>
>### Parameters ###
>**target**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target class
>
### public Collection getMixinsTargeting (TypeHandle) ###
>#### Method Overview ####
>Get mixin classes which target the specified class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Collection of mixins registered as targetting the specified class
>
>### Parameters ###
>**target**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target class
>
### public Collection getMixinsTargeting (TypeReference) ###
>#### Method Overview ####
>Get mixin classes which target the specified class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Collection of mixins registered as targetting the specified class
>
>### Parameters ###
>**target**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target class
>
### public void readImports (File) ###
>#### Method Overview ####
>Read upstream library mixins from a file
>
>### Parameters ###
>**file**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;File to read from
>
>### Throws ###
>**IOException**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;if an error occurs whilst reading the file
>
### public void write (boolean) ###
>#### Method Overview ####
>Write this target map to temporary session file
>
>### Parameters ###
>**temp**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Set "delete on exit" for the file
>
### public static TargetMap create (String) ###
>#### Method Overview ####
>Create a TargetMap for the specified session id. Generate new map if
 the session id is invalid or the file cannot be read. Session ID can be
 null
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new TargetMap
>
>### Parameters ###
>**sessionId**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;session to deserialise, can be null to create new
      session
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)