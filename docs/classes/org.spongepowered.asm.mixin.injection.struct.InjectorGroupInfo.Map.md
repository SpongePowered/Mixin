[< Back](../README.md)
# InjectorGroupInfo.Map #
>#### Class Overview ####
>Storage for injector groups
## Constructors ##
### public Map () ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public InjectorGroupInfo get (Object) ###
>#### Method Overview ####
>No description provided
>
### public InjectorGroupInfo forName (String) ###
>#### Method Overview ####
>Get group for the specified name, creates the group in this map if
 it does not already exist
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Existing group or new group if none was previously declared
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Name of group to fetch
>
### public InjectorGroupInfo parseGroup (MethodNode, String) ###
>#### Method Overview ####
>Parse a group from the specified method, use the default group name
 if no group name is specified on the annotation
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Group or NO_GROUP if no group
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(Possibly) annotated method
>
>**defaultGroup**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Default group name to use
>
### public InjectorGroupInfo parseGroup (AnnotationNode, String) ###
>#### Method Overview ####
>Parse a group from the specified annotation, use the default group
 name if no group name is specified on the annotation
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Group or NO_GROUP if no group
>
>### Parameters ###
>**annotation**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Annotation or null
>
>**defaultGroup**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Default group name to use
>
### public void validateAll () ###
>#### Method Overview ####
>Validate all groups in this collection
>
>### Throws ###
>**InjectionValidationException**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;if validation fails
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)