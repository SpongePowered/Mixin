[< Back](../README.md)
# IReferenceManager #
>#### Class Overview ####
>Consumer for generated references, builds the refmap during an AP pass and
 supports writing the generated refmap to file once the AP run is complete
## Methods ##
### public void setAllowConflicts (boolean) ###
>#### Method Overview ####
>Set whether this reference manager should allow conflicts to be inserted
 without raising an exception. Set to allow overrides to be written into
 the refmap when necessary
>
>### Parameters ###
>**allowConflicts**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;allow conflicts without raising an exception
>
### public boolean getAllowConflicts () ###
>#### Method Overview ####
>Get whether replacement mappings are allowed. Normally a mapping conflict
 will raise a {@link ReferenceConflictException}.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if conflicts are allowed
>
### public void write () ###
>#### Method Overview ####
>Write the generated refmap to file
>
### public ReferenceMapper getMapper () ###
>#### Method Overview ####
>Get the underlying reference mapper
>
### public void addMethodMapping (String, String, ObfuscationData) ###
>#### Method Overview ####
>Adds a method mapping to the internal refmap
>
>### Parameters ###
>**className**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Mixin class name which owns the refmap entry
>
>**reference**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Original reference, as it appears in the annotation
>
>**obfMethodData**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method data to add for this mapping
>
### public void addMethodMapping (String, String, MemberInfo, ObfuscationData) ###
>#### Method Overview ####
>Adds a method mapping to the internal refmap, generates refmap entries
 using the supplied parsed memberinfo as context
>
>### Parameters ###
>**className**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Mixin class name which owns the refmap entry
>
>**reference**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Original reference, as it appears in the annotation
>
>**context**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The context for this mapping entry, remapped using the
      supplied obfuscation data
>
>**obfMethodData**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method data to add for this mapping
>
### public void addFieldMapping (String, String, MemberInfo, ObfuscationData) ###
>#### Method Overview ####
>Adds a field mapping to the internal refmap, generates refmap entries
 using the supplied parsed memberinfo as context
>
>### Parameters ###
>**className**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Mixin class name which owns the refmap entry
>
>**reference**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Original reference, as it appears in the annotation
>
>**context**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The context for this mapping entry, remapped using the
      supplied obfuscation data
>
>**obfFieldData**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Field data to add for this mapping
>
### public void addClassMapping (String, String, ObfuscationData) ###
>#### Method Overview ####
>Adds a class mapping to the internal refmap
>
>### Parameters ###
>**className**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Mixin class name which owns the refmap entry
>
>**reference**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Original reference, as it appears in the annotation
>
>**obfClassData**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Class obf names
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)