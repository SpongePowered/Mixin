[< Back](../README.md)
# InterfaceInfo #
>#### Class Overview ####
>Information about an interface being runtime-patched onto a mixin target
 class, see {@link org.spongepowered.asm.mixin.Implements Implements}
## Methods ##
### public String getPrefix () ###
>#### Method Overview ####
>Get the prefix string (non null)
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the prefix
>
### public Type getIface () ###
>#### Method Overview ####
>Get the interface type
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;interface type
>
### public String getName () ###
>#### Method Overview ####
>Get the internal name of the interface
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the internal name for the interface
>
### public String getInternalName () ###
>#### Method Overview ####
>Get the internal name of the interface
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the internal name for the interface
>
### public boolean isUnique () ###
>#### Method Overview ####
>Get whether all methods for this interface should be treated as unique
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true to treat all member methods as unique
>
### public boolean renameMethod (MethodNode) ###
>#### Method Overview ####
>Processes a method node in the mixin and renames it if necessary. If the
 prefix is found then we verify that the method exists in the target
 interface and throw our teddies out of the pram if that's not the case
 (replacement behaviour for {@link Override} essentially.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if the method was remapped
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method to rename
>
### public boolean equals (Object) ###
>#### Method Overview ####
>No description provided
>
### public int hashCode () ###
>#### Method Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)