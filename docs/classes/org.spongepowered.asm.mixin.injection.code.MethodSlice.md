[< Back](../README.md)
# public final MethodSlice MethodSlice #
>#### Class Overview ####
>Stores information about a defined method slice for a particular injector.
## Methods ##
### public String getId () ###
>#### Method Overview ####
>Get the <em>declared</em> id of this slice
>
### public ReadOnlyInsnList getSlice (MethodNode) ###
>#### Method Overview ####
>Get a sliced insn list based on the parameters specified in this slice
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;read only slice
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method to slice
>
### public String toString () ###
>#### Method Overview ####
>No description provided
>
### public static MethodSlice parse (ISliceContext, Slice) ###
>#### Method Overview ####
>Parses the supplied annotation into a MethodSlice
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;parsed MethodSlice
>
>### Parameters ###
>**owner**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Owner injection info
>
>**slice**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Annotation to parse
>
### public static MethodSlice parse (ISliceContext, AnnotationNode) ###
>#### Method Overview ####
>Parses the supplied annotation into a MethodSlice
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;parsed MethodSlice
>
>### Parameters ###
>**info**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Owner injection info
>
>**node**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Annotation to parse
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)