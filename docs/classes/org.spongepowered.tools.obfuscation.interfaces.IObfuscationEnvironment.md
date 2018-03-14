[< Back](../README.md)
# public interface IObfuscationEnvironment IObfuscationEnvironment #
>#### Class Overview ####
>An obfuscation environment provides facilities to fetch obfuscation mappings
 of a particular type and also manage writing generated mappings of that type
 to disk.
## Methods ##
### public MappingMethod getObfMethod (MemberInfo) ###
>#### Method Overview ####
>Get an obfuscation mapping for a method, returns null if no mapping
 exists for the specified method in this environment.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;remapped method or null if no mapping exists
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method to locate a mapping for
>
### public MappingMethod getObfMethod (MappingMethod) ###
>#### Method Overview ####
>Get an obfuscation mapping for a method, returns null if no mapping
 exists for the specified method in this environment.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;remapped method or null if no mapping exists
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method to locate a mapping for
>
### public MappingMethod getObfMethod (MappingMethod, boolean) ###
>#### Method Overview ####
>Get an obfuscation mapping for a method, returns null if no mapping
 exists for the specified method in this environment.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;remapped method or null if no mapping exists
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method to locate a mapping for
>
>**lazyRemap**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;in general, if a mapping is not located, an attempt will
      be made to remap the owner and descriptor of the method to account
      for the fact that classes appearing in the descriptor may need to be
      remapped even if the method name is not. Setting <tt>lazyRemap</tt>
      to <tt>true</tt> disables this behaviour and simply fails fast if no
      mapping is located
>
### public MappingField getObfField (MemberInfo) ###
>#### Method Overview ####
>Get an obfuscation mapping for a field, returns null if no mapping
 exists for the specified field in this environment.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;remapped field or null if no mapping exists
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;field to locate a mapping for
>
### public MappingField getObfField (MappingField) ###
>#### Method Overview ####
>Get an obfuscation mapping for a field, returns null if no mapping
 exists for the specified field in this environment.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;remapped field or null if no mapping exists
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;field to locate a mapping for
>
### public MappingField getObfField (MappingField, boolean) ###
>#### Method Overview ####
>Get an obfuscation mapping for a field, returns null if no mapping
 exists for the specified field in this environment.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;remapped field or null if no mapping exists
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;field to locate a mapping for
>
>**lazyRemap**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;in general, if a mapping is not located, an attempt will
      be made to remap the owner and descriptor of the field to account
      for the fact that classes appearing in the descriptor may need to be
      remapped even if the field name is not. Setting <tt>lazyRemap</tt>
      to <tt>true</tt> disables this behaviour and simply fails fast if no
      mapping is located
>
### public String getObfClass (String) ###
>#### Method Overview ####
>Get an obfuscation mapping for a class, returns null if no mapping exists
 for the specified class in this environment.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;remapped class name
>
>### Parameters ###
>**className**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Name of the class to remap (binary format)
>
### public MemberInfo remapDescriptor (MemberInfo) ###
>#### Method Overview ####
>Remap only the owner and descriptor of the specified method
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;remapped method or null if no remapping occurred
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method to remap
>
### public String remapDescriptor (String) ###
>#### Method Overview ####
>Remap a single descriptor in the context of this environment
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;remapped descriptor, may return the original descriptor if no
      remapping occurred
>
>### Parameters ###
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;descriptor to remap
>
### public void writeMappings (Collection) ###
>#### Method Overview ####
>Write out accumulated mappings
>
>### Parameters ###
>**consumers**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;all consumers accumulated during the AP pass
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)