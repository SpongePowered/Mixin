[< Back](../README.md)
# IHotSwap #
>#### Class Overview ####
>Interface to allow the hot-swap agent to be loaded on-demand
## Methods ##
### public void registerMixinClass (String) ###
>#### Method Overview ####
>Registers a mixin class with the agent.

 <p>This is needed as the mixin needs to be loaded to be redefined.</p>
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Fully qualified name of the mixin class
>
### public void registerTargetClass (String, byte[]) ###
>#### Method Overview ####
>Registers a class targeted by at least one mixin.

 <p>This is used to rollback the target class to a state before the
 mixin's were applied.</p>
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Name of the class
>
>**bytecode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Bytecode of the class before mixin's have been applied
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)