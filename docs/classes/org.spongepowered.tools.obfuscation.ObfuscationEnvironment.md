[< Back](../README.md)
# ObfuscationEnvironment #
>#### Class Overview ####
>Provides access to information relevant to a particular obfuscation
 environment.
 
 <p>We classify different types of possible obfuscation (eg. "searge",
 "notch") as <em>obfuscation environments</em> and store related information
 such as the input mappings here.</p>
## Fields ##
### protected final ObfuscationType type ###
>#### Field Overview ####
>Type
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final IMappingProvider mappingProvider ###
>#### Field Overview ####
>Mapping provider
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final IMappingWriter mappingWriter ###
>#### Field Overview ####
>No description provided
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final RemapperProxy remapper ###
>#### Field Overview ####
>No description provided
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final IMixinAnnotationProcessor ap ###
>#### Field Overview ####
>Annotation processor
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final String outFileName ###
>#### Field Overview ####
>Name of the resource to write generated mappings to
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final List inFileNames ###
>#### Field Overview ####
>File containing the source mappings
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Constructors ##
### protected ObfuscationEnvironment (ObfuscationType) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public String toString () ###
>#### Method Overview ####
>No description provided
>
### protected abstract IMappingProvider getMappingProvider (Messager, Filer) ###
>#### Method Overview ####
>No description provided
>
### protected abstract IMappingWriter getMappingWriter (Messager, Filer) ###
>#### Method Overview ####
>No description provided
>
### public ObfuscationType getType () ###
>#### Method Overview ####
>Get the type
>
### public MappingMethod getObfMethod (MemberInfo) ###
>#### Method Overview ####
>Get an obfuscation mapping for a method
>
### public MappingMethod getObfMethod (MappingMethod) ###
>#### Method Overview ####
>Get an obfuscation mapping for a method
>
### public MappingMethod getObfMethod (MappingMethod, boolean) ###
>#### Method Overview ####
>Get an obfuscation mapping for a method
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
### public MappingField getObfField (MemberInfo) ###
>#### Method Overview ####
>Get an obfuscation mapping for a field
>
### public MappingField getObfField (MappingField) ###
>#### Method Overview ####
>Get an obfuscation mapping for a field
>
### public MappingField getObfField (MappingField, boolean) ###
>#### Method Overview ####
>Get an obfuscation mapping for a field
>
### public String getObfClass (String) ###
>#### Method Overview ####
>Get an obfuscation mapping for a class
>
### public void writeMappings (Collection) ###
>#### Method Overview ####
>Write out generated mappings
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)