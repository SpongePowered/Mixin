[< Back](../README.md)
# IRemapper #
>#### Class Overview ####
>Interface for remap chain participants
## Methods ##
### public String mapMethodName (String, String, String) ###
>#### Method Overview ####
>Map method name to the new name. Subclasses can override.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new name of the method
>
>### Parameters ###
>**owner**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;owner of the method.
>
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;name of the method.
>
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;descriptor of the method.
>
### public String mapFieldName (String, String, String) ###
>#### Method Overview ####
>Map field name to the new name. Subclasses can override.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new name of the field.
>
>### Parameters ###
>**owner**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;owner of the field.
>
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;name of the field
>
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;descriptor of the field
>
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
### public String mapDesc (String) ###
>#### Method Overview ####
>Convert a descriptor to remapped form
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new descriptor
>
>### Parameters ###
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;descriptor to convert
>
### public String unmapDesc (String) ###
>#### Method Overview ####
>Convert a descriptor back to the original obfuscated form
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;old descriptor
>
>### Parameters ###
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;descriptor to convert
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)