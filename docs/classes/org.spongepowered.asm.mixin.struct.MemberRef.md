[< Back](../README.md)
# public abstract MemberRef MemberRef #
>#### Class Overview ####
>Reference to a field or method that also includes invocation instructions.

 <p>To instances are defined to be equal if they both refer to the same method
 and have the same invocation instructions.</p>
## Constructors ##
### public MemberRef () ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public abstract boolean isField () ###
>#### Method Overview ####
>Whether this member is a field.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;If this member is a field, else it is a method
>
### public abstract int getOpcode () ###
>#### Method Overview ####
>The opcode of the invocation.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The opcode of the invocation
>
### public abstract void setOpcode (int) ###
>#### Method Overview ####
>Set the opcode of the invocation.
>
>### Parameters ###
>**opcode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new opcode
>
### public abstract String getOwner () ###
>#### Method Overview ####
>The internal name for the owner of this member.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The owners name
>
### public abstract void setOwner (String) ###
>#### Method Overview ####
>Changes the owner of this
>
>### Parameters ###
>**owner**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;New owner
>
### public abstract String getName () ###
>#### Method Overview ####
>Name of this member.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Name of this member.
>
### public abstract void setName (String) ###
>#### Method Overview ####
>Rename this member.
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;New name for this member.
>
### public abstract String getDesc () ###
>#### Method Overview ####
>Descriptor of this member.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Descriptor of this member
>
### public abstract void setDesc (String) ###
>#### Method Overview ####
>Changes the descriptor of this member
>
>### Parameters ###
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;New descriptor of this member
>
### public String toString () ###
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

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)