[< Back](../README.md)
# AnnotatedMixinElementHandlerSoftImplements #
>#### Class Overview ####
>A module for {@link AnnotatedMixin} whic handles soft-implements clauses
## Methods ##
### public void process (AnnotationHandle) ###
>#### Method Overview ####
>Process a soft-implements annotation on a mixin. This causes the
 interface declared in the annotation and all of its super-interfaces to
 be enumerated for member methods. Any member methods which are discovered
 in the mixin are then tested for remapability based on the strategy
 defined in the soft-implements decoration
>
>### Parameters ###
>**implementsAnnotation**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the &#064;Implements annotation on the
      element
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)