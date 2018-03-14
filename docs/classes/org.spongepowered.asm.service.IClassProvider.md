[< Back](../README.md)
# IClassProvider #
>#### Class Overview ####
>Interface for marshal object which can retrieve classes from the environment
## Methods ##
### public URL getClassPath () ###
>#### Method Overview ####
>Get the current classpath from the service classloader
>
### public Class findClass (String) ###
>#### Method Overview ####
>Find a class in the service classloader
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;resultant class
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;class name
>
>### Throws ###
>**ClassNotFoundException**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;if the class was not found
>
### public Class findClass (String, boolean) ###
>#### Method Overview ####
>Marshal a call to <tt>Class.forName</tt> for a regular class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Klass
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;class name
>
>**initialize**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;init flag
>
### public Class findAgentClass (String, boolean) ###
>#### Method Overview ####
>Marshal a call to <tt>Class.forName</tt> for an agent class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Klass
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;agent class name
>
>**initialize**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;init flag
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)