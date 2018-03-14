[< Back](../README.md)
# public static File SourceMap.File #
>#### Class Overview ####
>Defines a source code file within a source map stratum
## Fields ##
### public final int id ###
>#### Field Overview ####
>File index in stratum
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public final int lineOffset ###
>#### Field Overview ####
>The base line offset for this stratum, line numbers in the output
 will be offset by this amount from their originals
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public final int size ###
>#### Field Overview ####
>The size of this stratum (number of lines)
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public final String sourceFileName ###
>#### Field Overview ####
>Actual source file name to include in the smap
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public final String sourceFilePath ###
>#### Field Overview ####
>Full path to the source file
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Constructors ##
### public File (int, int, int, String) ###
>#### Constructor Overview ####
>Create a new SMAP Stratum
>
>### Parameters ###
>**lineOffset**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;line offset
>
>**size**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;total lines
>
>**sourceFileName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;source file name
>
### public File (int, int, int, String, String) ###
>#### Constructor Overview ####
>Create a new SMAP Stratum
>
>### Parameters ###
>**lineOffset**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;line offset
>
>**size**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;total lines
>
>**sourceFileName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;source file name
>
>**sourceFilePath**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;source path
>
## Methods ##
### public void applyOffset (ClassNode) ###
>#### Method Overview ####
>Offset the line numbers in the target class node by the base
 lineoffset for this stratum
>
>### Parameters ###
>**classNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;class to operate upon
>
### public void applyOffset (MethodNode) ###
>#### Method Overview ####
>Offset the line numbers in the target method node by the base
 lineoffset for this stratum
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method to operate upon
>
### public void appendLines (StringBuilder) ###
>#### Method Overview ####
>Append lines representing this File to the supplied StringBuilder
>
>### Parameters ###
>**sb**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;StringBuilder to append to
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)