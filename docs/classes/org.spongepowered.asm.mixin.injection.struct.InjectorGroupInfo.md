[< Back](../README.md)
# public InjectorGroupInfo InjectorGroupInfo #
>#### Class Overview ####
>Information store for injector groups
## Constructors ##
### public InjectorGroupInfo (String) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public String toString () ###
>#### Method Overview ####
>No description provided
>
### public boolean isDefault () ###
>#### Method Overview ####
>No description provided
>
### public String getName () ###
>#### Method Overview ####
>No description provided
>
### public int getMinRequired () ###
>#### Method Overview ####
>No description provided
>
### public int getMaxAllowed () ###
>#### Method Overview ####
>No description provided
>
### public Collection getMembers () ###
>#### Method Overview ####
>Get all members of this group as a read-only collection
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;read-only view of group members
>
### public void setMinRequired (int) ###
>#### Method Overview ####
>Set the required minimum value for this group. Since this is normally
 done on the first {@link Group} annotation it is considered a
 warning-level event if a later annotation sets a different value. The
 highest value specified on all annotations is always used.
>
>### Parameters ###
>**min**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new value for min required
>
### public void setMaxAllowed (int) ###
>#### Method Overview ####
>Set the required minimum value for this group. Since this is normally
 done on the first {@link Group} annotation it is considered a
 warning-level event if a later annotation sets a different value. The
 highest value specified on all annotations is always used.
>
>### Parameters ###
>**max**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new value for max allowed
>
### public InjectorGroupInfo add (InjectionInfo) ###
>#### Method Overview ####
>Add a new member to this group
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**member**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;injector to add
>
### public InjectorGroupInfo validate () ###
>#### Method Overview ####
>Validate all members in this group
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Throws ###
>**InjectionValidationException**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;if validation fails
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)