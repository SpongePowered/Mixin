[< Back](../README.md)
# IMixinContext #
>#### Class Overview ####
>Context for performing reference mapping
## Methods ##
### public IMixinInfo getMixin () ###
>#### Method Overview ####
>Get the mixin info
>
### public Extensions getExtensions () ###
>#### Method Overview ####
>Get the mixin transformer extension manager
>
### public String getClassRef () ###
>#### Method Overview ####
>Get the internal mixin class name
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;internal class name
>
### public String getTargetClassRef () ###
>#### Method Overview ####
>Get the internal name of the target class for this context
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;internal target class name
>
### public IReferenceMapper getReferenceMapper () ###
>#### Method Overview ####
>Get the reference mapper for this mixin
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ReferenceMapper instance (can be null)
>
### public boolean getOption (MixinEnvironment.Option) ###
>#### Method Overview ####
>Retrieve the value of the specified <tt>option</tt> from the environment
 this mixin belongs to.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;option value
>
>### Parameters ###
>**option**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;option to check
>
### public int getPriority () ###
>#### Method Overview ####
>Get the priority of the mixin
>
### public Target getTargetMethod (MethodNode) ###
>#### Method Overview ####
>Obtain a {@link Target} method handle for a method in the target, this is
 used by consumers to manipulate the bytecode in a target method in a
 controlled manner.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;target method
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method node to wrap
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)