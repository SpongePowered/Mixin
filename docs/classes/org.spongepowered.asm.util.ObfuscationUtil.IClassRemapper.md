[< Back](../README.md)
# ObfuscationUtil.IClassRemapper #
>#### Class Overview ####
>Interface for remapper proxies
## Methods ##
### public String map (String) ###
>#### Method Overview ####
>Map type name to the new name. Subclasses can override.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new name for the class
>
>### Parameters ###
>**typeName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Class name to convert
>
### public String unmap (String) ###
>#### Method Overview ####
>Convert a mapped type name back to the original obfuscated name
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;old name for the class
>
>### Parameters ###
>**typeName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Class name to convert
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)