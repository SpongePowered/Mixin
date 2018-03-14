[< Back](../README.md)
# MethodSlices #
>#### Class Overview ####
>Represents a collection of {@link MethodSlice}s, mapped by ID. Stored ids may
 be different to declared slice ids because they are mapped by the underlying
 injector. Some injectors only support a single slice.
## Methods ##
### public MethodSlice get (String) ###
>#### Method Overview ####
>Fetch the slice with the specified id, returns null if no slice with the
 supplied id is available
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;matching slice or null
>
>### Parameters ###
>**id**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;slice id
>
### public String toString () ###
>#### Method Overview ####
>No description provided
>
### public static MethodSlices parse (InjectionInfo) ###
>#### Method Overview ####
>Parse a collection of slices from the supplied injector
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;parsed slice collection
>
>### Parameters ###
>**info**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;owning injector
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)