[< Back](../README.md)
# public InjectorTarget InjectorTarget #
>#### Class Overview ####
>Couples {@link MethodSlice method slices} to a {@link Target} for injection
 purposes.
## Constructors ##
### public InjectorTarget (ISliceContext, Target) ###
>#### Constructor Overview ####
>ctor
>
>### Parameters ###
>**context**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;owner
>
>**target**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;target
>
## Methods ##
### public Target getTarget () ###
>#### Method Overview ####
>Get the target reference
>
### public MethodNode getMethod () ###
>#### Method Overview ####
>Get the target method
>
### public InsnList getSlice (String) ###
>#### Method Overview ####
>Get the slice instructions for the specified slice id
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;insn slice
>
>### Parameters ###
>**id**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;slice id
>
### public InsnList getSlice (InjectionPoint) ###
>#### Method Overview ####
>Get the slice instructions for the specified injection point
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;slice
>
>### Parameters ###
>**injectionPoint**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;injection point to fetch slice for
>
### public void dispose () ###
>#### Method Overview ####
>Dispose all cached instruction lists
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)