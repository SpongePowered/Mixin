[< Back](../README.md)
# public final VersionNumber VersionNumber #
>#### Class Overview ####
>Represents a software version number in <code>major.minor.revision.build
 </code> format as a sequence of four shorts packed into a long. This is to
 facilitate meaningful comparison between version numbers.
## Fields ##
### public static final VersionNumber NONE ###
>#### Field Overview ####
>Represents no version number or a version number which could not be
 parsed
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Methods ##
### public String toString () ###
>#### Method Overview ####
>No description provided
>
### public int compareTo (VersionNumber) ###
>#### Method Overview ####
>No description provided
>
### public boolean equals (Object) ###
>#### Method Overview ####
>No description provided
>
### public int hashCode () ###
>#### Method Overview ####
>No description provided
>
### public static VersionNumber parse (String) ###
>#### Method Overview ####
>Parse a version number specified as a string
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Version number
>
>### Parameters ###
>**version**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Version number to parse
>
### public static VersionNumber parse (String, String) ###
>#### Method Overview ####
>Parse a version number specified as a string and return default if
 parsing fails
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Version number
>
>### Parameters ###
>**version**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Version number to parse
>
>**defaultVersion**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Version number to return if parse fails
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)