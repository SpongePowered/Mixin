[< Back](../README.md)
# public interface ITypeHandleProvider ITypeHandleProvider #
>#### Class Overview ####
>Manager object which cann supply {@link TypeHandle} instances
## Methods ##
### public TypeHandle getTypeHandle (String) ###
>#### Method Overview ####
>Generate a type handle for the specified type
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;A new type handle or null if the type could not be found
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type name (class name)
>
### public TypeHandle getSimulatedHandle (String, TypeMirror) ###
>#### Method Overview ####
>Generate a type handle for the specified type, simulate the target using
 the supplied type
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;A new type handle
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type name (class name)
>
>**simulatedTarget**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Simulation target
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)