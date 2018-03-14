[< Back](../README.md)
# public interface IMixinInfo IMixinInfo #
>#### Class Overview ####
>Interface for MixinInfo, used in extensibility API
## Methods ##
### public IMixinConfig getConfig () ###
>#### Method Overview ####
>Get the config to which this mixin belongs
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the mixin config
>
### public String getName () ###
>#### Method Overview ####
>Get the simple name of the mixin
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the simple name (mixin tail minus the package)
>
### public String getClassName () ###
>#### Method Overview ####
>Get the name of the mixin class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mixin class name
>
### public String getClassRef () ###
>#### Method Overview ####
>Get the ref (internal name) of the mixin class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mixin class ref (internal name)
>
### public byte getClassBytes () ###
>#### Method Overview ####
>Get the class bytecode
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mixin bytecode (raw bytecode after transformers)
>
### public boolean isDetachedSuper () ###
>#### Method Overview ####
>True if the superclass of the mixin is <b>not</b> the direct superclass
 of one or more targets.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if the mixin has a detached superclass
>
### public ClassNode getClassNode (int) ###
>#### Method Overview ####
>Get a new tree for the class bytecode
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;get a new ClassNode representing the mixin's bytecode
>
>### Parameters ###
>**flags**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Flags to pass to the ClassReader
>
### public List getTargetClasses () ###
>#### Method Overview ####
>Get the target classes for this mixin
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;list of target classes
>
### public int getPriority () ###
>#### Method Overview ####
>Get the mixin priority
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the priority
>
### public Phase getPhase () ###
>#### Method Overview ####
>Get the mixin phase
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the phase
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)