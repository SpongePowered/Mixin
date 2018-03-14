[< Back](../README.md)
# public interface ILegacyClassTransformer ILegacyClassTransformer #
>#### Class Overview ####
>Adapter interface for legacy class transformers. Legacy class transformers
 operate on raw byte arrays.
## Methods ##
### public String getName () ###
>#### Method Overview ####
>Get the identifier for this transformer, usually the class name but for
 wrapped transformers this is the class name of the wrapped transformer
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;transformer's identifying name
>
### public boolean isDelegationExcluded () ###
>#### Method Overview ####
>Get whether this transformer is excluded from delegation. Some
 transformers (such as the mixin transformer itself) should not be
 included in the delegation list because they are re-entrant or do not
 need to run on incoming bytecode.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if this transformer should be <em>excluded</em> from the
      transformer delegation list
>
### public byte transformClassBytes (String, String, byte[]) ###
>#### Method Overview ####
>Transform a class in byte array form.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;transformed bytes
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Class original name
>
>**transformedName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Class name after being processed by the class name
      transformer
>
>**basicClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;class byte array
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)