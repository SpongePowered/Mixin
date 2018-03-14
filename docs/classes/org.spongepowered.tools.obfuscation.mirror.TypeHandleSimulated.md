[< Back](../README.md)
# public TypeHandleSimulated TypeHandleSimulated #
>#### Class Overview ####
>A simulated type handle, used with virtual (pseudo) mixins. For obfuscation
 purposes, we have to use some kind of context to resolve target members so
 that appropriate refmaps can be generated. For this purpose we use the mixin
 itself as the context in order to allow us to look up members in superclasses
 and superinterfaces of the mixin (in the hope that we can locate targets
 there. If we cannot achieve this, then remapping will have to be done by hand
## Constructors ##
### public TypeHandleSimulated (String, TypeMirror) ###
>#### Constructor Overview ####
>No description provided
>
### public TypeHandleSimulated (PackageElement, String, TypeMirror) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### protected TypeElement getTargetElement () ###
>#### Method Overview ####
>No description provided
>
### public boolean isPublic () ###
>#### Method Overview ####
>No description provided
>
### public boolean isImaginary () ###
>#### Method Overview ####
>No description provided
>
### public boolean isSimulated () ###
>#### Method Overview ####
>No description provided
>
### public AnnotationHandle getAnnotation (Class) ###
>#### Method Overview ####
>No description provided
>
### public TypeHandle getSuperclass () ###
>#### Method Overview ####
>No description provided
>
### public String findDescriptor (MemberInfo) ###
>#### Method Overview ####
>No description provided
>
### public FieldHandle findField (String, String, boolean) ###
>#### Method Overview ####
>No description provided
>
### public MethodHandle findMethod (String, String, boolean) ###
>#### Method Overview ####
>No description provided
>
### public MappingMethod getMappingMethod (String, String) ###
>#### Method Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)