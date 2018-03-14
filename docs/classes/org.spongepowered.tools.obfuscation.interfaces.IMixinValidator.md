[< Back](../README.md)
# IMixinValidator #
>#### Class Overview ####
>A mixin validator module, basically just a way of making the various sanity
 checks modular
## Methods ##
### public boolean validate (IMixinValidator.ValidationPass, TypeElement, AnnotationHandle, Collection) ###
>#### Method Overview ####
>Validate all the things, return false to halt processing of further
 validators. Raise compiler errors/warnings directly.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;False to halt processing of further validators
>
>### Parameters ###
>**pass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;current validation pass
>
>**mixin**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Mixin being validated
>
>**annotation**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Mixin annotation
>
>**targets**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Mixin targets
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)