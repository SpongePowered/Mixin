[< Back](../README.md)
# public interface IClassGenerator IClassGenerator #
>#### Class Overview ####
>Base interface for class generators
## Methods ##
### public byte generate (String) ###
>#### Method Overview ####
>Generate (if possible) the specified class name. The generator should
 return <tt>null</tt> if it cannot generate the specified class, in order
 that the next generator in the chain can process the request. The first
 generator to return a value will halt further processing of the request.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Class bytecode or null
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Class name to generate
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)