[< Back](../README.md)
# public final ObfuscationType ObfuscationType #
>#### Class Overview ####
>Obfuscation types supported by the annotation processor
## Methods ##
### public final ObfuscationEnvironment createEnvironment () ###
>#### Method Overview ####
>Create obfuscation environment instance for this obfuscation type
>
### public String toString () ###
>#### Method Overview ####
>No description provided
>
### public String getKey () ###
>#### Method Overview ####
>No description provided
>
### public ObfuscationTypeDescriptor getConfig () ###
>#### Method Overview ####
>No description provided
>
### public IMixinAnnotationProcessor getAnnotationProcessor () ###
>#### Method Overview ####
>No description provided
>
### public boolean isDefault () ###
>#### Method Overview ####
>Get whether this is ithe default obfuscation environment
>
### public boolean isSupported () ###
>#### Method Overview ####
>Get whether this obfuscation type has data available
>
### public List getInputFileNames () ###
>#### Method Overview ####
>Get the input file names specified for this obfuscation type
>
### public String getOutputFileName () ###
>#### Method Overview ####
>Get the output filenames specified for this obfuscation type
>
### public static Iterable types () ###
>#### Method Overview ####
>All available obfuscation types
>
### public static ObfuscationType create (ObfuscationTypeDescriptor, IMixinAnnotationProcessor) ###
>#### Method Overview ####
>Create a new obfuscation type from the supplied descriptor
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new obfuscation type
>
>### Parameters ###
>**descriptor**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;obfuscation type metadata
>
>**ap**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;annotation processor
>
### public static ObfuscationType get (String) ###
>#### Method Overview ####
>Retrieve an obfuscation type by key
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;obfuscation type or <tt>null</tt> if no matching type is
      available
>
>### Parameters ###
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;obfuscation type key to retrieve
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)