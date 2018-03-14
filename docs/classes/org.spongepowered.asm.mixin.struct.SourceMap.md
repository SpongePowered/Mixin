[< Back](../README.md)
# SourceMap #
>#### Class Overview ####
>Structure which contains information about a SourceDebugExtension SMAP
## Constructors ##
### public SourceMap (String) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public String getSourceFile () ###
>#### Method Overview ####
>Get the original source file
>
### public String getPseudoGeneratedSourceFile () ###
>#### Method Overview ####
>Get the generated source file
>
### public File addFile (ClassNode) ###
>#### Method Overview ####
>Add a file to this SourceMap in the default stratum
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new File
>
>### Parameters ###
>**classNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;class node to read details from
>
### public File addFile (String, ClassNode) ###
>#### Method Overview ####
>Add a file to this SourceMap in the specified stratum
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new File
>
>### Parameters ###
>**stratumName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;name of the stratum to add to
>
>**classNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;class node to read file details from
>
### public File addFile (String, String, int) ###
>#### Method Overview ####
>Add a file to this SourceMap in the default stratum
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new File
>
>### Parameters ###
>**sourceFileName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;source filename
>
>**sourceFilePath**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;path to source file
>
>**size**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;number of lines to allocate
>
### public File addFile (String, String, String, int) ###
>#### Method Overview ####
>Add a file to this SourceMap in the specified stratum
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new File
>
>### Parameters ###
>**stratumName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;name of the stratum to add to
>
>**sourceFileName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;source filename
>
>**sourceFilePath**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;path to source file
>
>**size**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;number of lines to allocate
>
### public String toString () ###
>#### Method Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)