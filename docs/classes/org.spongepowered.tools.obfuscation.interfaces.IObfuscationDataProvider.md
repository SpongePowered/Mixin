[< Back](../README.md)
# IObfuscationDataProvider #
>#### Class Overview ####
>Provides obfuscation data to the annotation processor
## Methods ##
### public ObfuscationData getObfEntryRecursive (MemberInfo) ###
>#### Method Overview ####
>Attempts to resolve an obfuscation entry by recursively enumerating
 superclasses of the target member until a match is found. If a match is
 found in a superclass then the reference is remapped to the original
 owner class.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ObfuscationData with remapped owner class corresponding to the
      original owner class
      or String for fields)
>
>### Parameters ###
>**targetMember**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Member to search for
>
### public ObfuscationData getObfEntry (MemberInfo) ###
>#### Method Overview ####
>Resolves a field or method reference to an ObfuscationData set
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;obfuscation data (by type) for the supplied member
      or String for fields)
>
>### Parameters ###
>**targetMember**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;member to search for
>
### public ObfuscationData getObfEntry (IMapping) ###
>#### Method Overview ####
>Resolves a field or method reference to an ObfuscationData set
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;obfuscation data (by type) for the supplied member
      or String for fields)
>
>### Parameters ###
>**mapping**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;member to search for
>
### public ObfuscationData getObfMethodRecursive (MemberInfo) ###
>#### Method Overview ####
>Attempts to resolve an obfuscated method by recursively enumerating
 superclasses of the target method until a match is found. If a match is
 found in a superclass then the method owner is remapped to the obfuscated
 name of the original owner class.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ObfuscationData with remapped owner class corresponding to the
      original owner class
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method to search for
>
### public ObfuscationData getObfMethod (MemberInfo) ###
>#### Method Overview ####
>Get an obfuscation mapping for a method
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method to fetch obfuscation mapping for
>
### public ObfuscationData getRemappedMethod (MemberInfo) ###
>#### Method Overview ####
>Get an obfuscation mapping for a method if an explicit mapping exists.
 Where no direct mapping exists, remap the descriptor of the method only.
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method to fetch obfuscation mapping for
>
### public ObfuscationData getObfMethod (MappingMethod) ###
>#### Method Overview ####
>Get an obfuscation mapping for a method
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method to fetch obfuscation mapping for
>
### public ObfuscationData getRemappedMethod (MappingMethod) ###
>#### Method Overview ####
>Get an obfuscation mapping for a method if an explicit mapping exists.
 Where no direct mapping exists, remap the descriptor of the method only.
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method to fetch obfuscation mapping for
>
### public ObfuscationData getObfFieldRecursive (MemberInfo) ###
>#### Method Overview ####
>Attempts to resolve an obfuscated field by recursively enumerating
 superclasses of the target field until a match is found. If a match is
 found in a superclass then the field owner is remapped to the obfuscated
 name of the original owner class.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ObfuscationData with remapped owner class corresponding to the
      original owner class
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Field to search for
>
### public ObfuscationData getObfField (MemberInfo) ###
>#### Method Overview ####
>Get an obfuscation mapping for a field
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;field to fetch obfuscation mapping for
>
### public ObfuscationData getObfField (MappingField) ###
>#### Method Overview ####
>Get an obfuscation mapping for a field
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;field to fetch obfuscation mapping for
>
### public ObfuscationData getObfClass (TypeHandle) ###
>#### Method Overview ####
>Get an obfuscation mapping for a class
>
>### Parameters ###
>**type**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;class type to fetch obfuscation mapping for
>
### public ObfuscationData getObfClass (String) ###
>#### Method Overview ####
>Get an obfuscation mapping for a class
>
>### Parameters ###
>**className**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;class name to fetch obfuscation mapping for
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)