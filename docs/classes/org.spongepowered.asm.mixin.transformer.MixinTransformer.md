[< Back](../README.md)
# public MixinTransformer MixinTransformer #
>#### Class Overview ####
>Transformer which manages the mixin configuration and application process
## Methods ##
### public void audit (MixinEnvironment) ###
>#### Method Overview ####
>Force-load all classes targetted by mixins but not yet applied
>
>### Parameters ###
>**environment**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;current environment
>
### public String getName () ###
>#### Method Overview ####
>No description provided
>
### public boolean isDelegationExcluded () ###
>#### Method Overview ####
>No description provided
>
### public synchronized byte transformClassBytes (String, String, byte[]) ###
>#### Method Overview ####
>No description provided
>
### public List reload (String, byte[]) ###
>#### Method Overview ####
>Update a mixin class with new bytecode.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;List of classes that need to be updated
>
>### Parameters ###
>**mixinClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Name of the mixin
>
>**bytes**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;New bytecode
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)