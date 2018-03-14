[< Back](../README.md)
# TypeReference #
>#### Class Overview ####
>Soft wrapper for a {@link TypeHandle} so that we can serialise it
## Constructors ##
### public TypeReference (TypeHandle) ###
>#### Constructor Overview ####
>Create a new soft wrapper for the specified type handle
>
>### Parameters ###
>**handle**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;handle to wrap
>
### public TypeReference (String) ###
>#### Constructor Overview ####
>Create a type reference with no handle
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Name of the type
>
## Methods ##
### public String getName () ###
>#### Method Overview ####
>Get the class name (internal format)
>
### public String getClassName () ###
>#### Method Overview ####
>Get the FQ class name (dotted format)
>
### public TypeHandle getHandle (ProcessingEnvironment) ###
>#### Method Overview ####
>Fetch or attempt to generate the type handle
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type handle
>
>### Parameters ###
>**processingEnv**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;environment to create handle if it needs to be
      regenerated
>
### public String toString () ###
>#### Method Overview ####
>No description provided
>
### public int compareTo (TypeReference) ###
>#### Method Overview ####
>No description provided
>
### public boolean equals (Object) ###
>#### Method Overview ####
>No description provided
>
### public int hashCode () ###
>#### Method Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)