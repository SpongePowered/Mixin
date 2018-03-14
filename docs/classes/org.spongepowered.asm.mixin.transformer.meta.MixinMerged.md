[< Back](../README.md)
# MixinMerged #
>#### Class Overview ####
><p><b>For internal use only!</b> Contains small parts. Keep out of reach of
 children.</p>
 
 <p>Decoration annotation used by the mixin applicator to mark methods in a
 class which have been added or overwritten by a mixin.</p>
## Methods ##
### public String mixin () ###
>#### Method Overview ####
>Mixin which merged this method
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mixin name
>
### public int priority () ###
>#### Method Overview ####
>Prioriy of the mixin which merged this method, used to allow mixins with
 higher priority to overwrite methods already overwritten by those with a
 lower priority.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mixin priority
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)