[< Back](../README.md)
# public abstract MemberHandle MemberHandle #
>#### Class Overview ####
>Abstract base class for element handles
## Constructors ##
### protected MemberHandle (String, String, String) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public final String getOwner () ###
>#### Method Overview ####
>No description provided
>
### public final String getName () ###
>#### Method Overview ####
>No description provided
>
### public final String getDesc () ###
>#### Method Overview ####
>No description provided
>
### public abstract Visibility getVisibility () ###
>#### Method Overview ####
>Get the visibility level for this member
>
### public abstract IMapping asMapping (boolean) ###
>#### Method Overview ####
>Return this handle as a mapping
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;this handle as a mapping of appropriate type
>
>### Parameters ###
>**includeOwner**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;include the owner in the generated mapping, false to
      only include name and desc
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)