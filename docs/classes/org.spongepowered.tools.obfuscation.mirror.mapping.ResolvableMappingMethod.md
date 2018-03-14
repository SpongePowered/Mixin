[< Back](../README.md)
# public final ResolvableMappingMethod ResolvableMappingMethod #
>#### Class Overview ####
>A mapping method obtained from a {@link TypeHandle}. The context for this
 mapping allows references in superclasses to be resolved when necessary since
 the hierarchy information is available.
## Constructors ##
### public ResolvableMappingMethod (TypeHandle, String, String) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public MappingMethod getSuper () ###
>#### Method Overview ####
>No description provided
>
### public MappingMethod move (TypeHandle) ###
>#### Method Overview ####
>Specialised version of <tt>move</tt> which allows this resolvable method
 to be reparented to a new type handle
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;remapped method
>
>### Parameters ###
>**newOwner**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new owner
>
### public MappingMethod remap (String) ###
>#### Method Overview ####
>No description provided
>
### public MappingMethod transform (String) ###
>#### Method Overview ####
>No description provided
>
### public MappingMethod copy () ###
>#### Method Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)