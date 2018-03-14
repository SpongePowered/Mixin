[< Back](../README.md)
# IClassBytecodeProvider #
>#### Class Overview ####
>Interface for object which can provide class bytecode
## Methods ##
### public byte getClassBytes (String, String) ###
>#### Method Overview ####
>Retrieve class bytes using available classloaders, does not transform the
 class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;class bytes or null if not found
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;class name
>
>**transformedName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;transformed class name
>
>### Throws ###
>**IOException**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;propagated
>
### public byte getClassBytes (String, boolean) ###
>#### Method Overview ####
>Retrieve transformed class bytes by using available classloaders and
 running transformer delegation chain on the result if the runTransformers
 option is enabled
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;transformed bytes
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;full class name
>
>**runTransformers**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true to run transformers on the loaded bytecode
>
>### Throws ###
>**ClassNotFoundException**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;if class not found
>
>**IOException**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;propagated
>
### public ClassNode getClassNode (String) ###
>#### Method Overview ####
>Retrieve transformed class as an ASM tree
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;tree
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;full class name
>
>### Throws ###
>**ClassNotFoundException**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;if class not found
>
>**IOException**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;propagated
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)