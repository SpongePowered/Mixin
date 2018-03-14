[< Back](../README.md)
# AccessorGenerator #
>#### Class Overview ####
>Base class for accessor generators
## Fields ##
### protected final AccessorInfo info ###
>#### Field Overview ####
>Accessor info which describes the accessor
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Constructors ##
### public AccessorGenerator (AccessorInfo) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### protected final MethodNode createMethod (int, int) ###
>#### Method Overview ####
>Create an empty accessor method based on the source method
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new method
>
>### Parameters ###
>**maxLocals**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;max locals size for method
>
>**maxStack**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;max stack size for method
>
### public abstract MethodNode generate () ###
>#### Method Overview ####
>Generate the accessor method
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;generated accessor method
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)