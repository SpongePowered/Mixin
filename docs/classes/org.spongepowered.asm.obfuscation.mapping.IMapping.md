[< Back](../README.md)
# public interface IMapping IMapping #
>#### Class Overview ####
>Base class for member mapping entries
## Methods ##
### public Type getType () ###
>#### Method Overview ####
>Get the mapping type (field, method, class, package)
>
### public Object move (String) ###
>#### Method Overview ####
>Create a clone of this mapping with a new owner
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;cloned mapping
>
>### Parameters ###
>**newOwner**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new owner
>
### public Object remap (String) ###
>#### Method Overview ####
>Create a clone of this mapping with a new name
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;cloned mapping
>
>### Parameters ###
>**newName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new name
>
### public Object transform (String) ###
>#### Method Overview ####
>Create a clone of this mapping with a new descriptor
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;cloned mapping
>
>### Parameters ###
>**newDesc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new descriptor
>
### public Object copy () ###
>#### Method Overview ####
>Create a clone of this mapping
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;cloned mapping
>
### public String getName () ###
>#### Method Overview ####
>Get the mapping name, for method mappings this includes the owner
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the mapping name, includes the owner for method mappings
>
### public String getSimpleName () ###
>#### Method Overview ####
>Get the base name of this member, for example the bare field, method or
 class name
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the base name of this mapping
>
### public String getOwner () ###
>#### Method Overview ####
>Get the owner of this member, for fields and methods this is the class
 name, for classes it is the package name, for packages it is undefined.
 Can return null.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the parent of this mapping
>
### public String getDesc () ###
>#### Method Overview ####
>Get the descriptor of this member, for example the method descriptor or
 field type. For classes and packages this is undefined. Can return null
 since not all mapping types support descriptors.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the mapping descriptor
>
### public Object getSuper () ###
>#### Method Overview ####
>Get the next most immediate super-implementation of this mapping. For
 example if the mapping is a method and the method overrides a method in
 the immediate superclass, return that method. Can return null if no
 superclass is available or if no superclass definition exists.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the method immediately overridden by this method, or null if not
      present or not resolvable
>
### public String serialise () ###
>#### Method Overview ####
>Get a representation of this mapping for serialisation. Individual
 writers are free to use their own mappings, this method is for
 convenience only.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;string representation of this mapping
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)