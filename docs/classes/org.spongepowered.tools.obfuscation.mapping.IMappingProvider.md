[< Back](../README.md)
# IMappingProvider #
>#### Class Overview ####
>A mapping provider stores raw mapping information for use by the AP, access
 is delegated via the environment which provides enhanced lookup functionality
 such as resolving members recursively in super classes. The mapping provider
 is not required to provide such functionality and merely facilitates raw
 mapping lookups from a particular source.
## Methods ##
### public void clear () ###
>#### Method Overview ####
>Clear this mapping provider, used to ensure the internal data is cleared
 before beginning a {@link #read}
>
### public boolean isEmpty () ###
>#### Method Overview ####
>Returns true if this mapping provider contains no mappings
>
### public void read (File) ###
>#### Method Overview ####
>Called multiple times by the environment. This method will be called for
 each input file specified by the user.
>
>### Parameters ###
>**input**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;input file to read
>
>### Throws ###
>**IOException**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;if an error occurs reading the input file or the file
      does not exist or cannot be opened
>
### public MappingMethod getMethodMapping (MappingMethod) ###
>#### Method Overview ####
>Retrieve a method mapping from this provider. This method should return
 <tt>null</tt> if no mapping is found
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mapped method or <tt>null</tt> if not found
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method to find a mapping for
>
### public MappingField getFieldMapping (MappingField) ###
>#### Method Overview ####
>Retrieve a field mapping from this provider. This method should return
 <tt>null</tt> if no mapping is found
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mapped field or <tt>null</tt> if not found
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;field to find a mapping for
>
### public String getClassMapping (String) ###
>#### Method Overview ####
>Retrieve a class mapping from this provider. This method should return
 <tt>null</tt> if no mapping is found
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mapped class name or <tt>null</tt> if not found
>
>### Parameters ###
>**className**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;name of the class to find a mapping for
>
### public String getPackageMapping (String) ###
>#### Method Overview ####
>Retrieve a package mapping from this provider. This method should return
 <tt>null</tt> if no mapping is found
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mapped package name or <tt>null</tt> if not found
>
>### Parameters ###
>**packageName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;name of the package to find a mapping for
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)