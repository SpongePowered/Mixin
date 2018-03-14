[< Back](../README.md)
# InjectionNodes.InjectionNode #
>#### Class Overview ####
>A node targetted by one or more injectors. Using this wrapper allows
 injectors to be aware of when their target node is removed or replace by
 another injector. It also allows injectors to decorate certain nodes with
 custom metadata to allow arbitration between injectors to take place.
## Constructors ##
### public InjectionNode (AbstractInsnNode) ###
>#### Constructor Overview ####
>Create a new node wrapper for the specified target node
>
>### Parameters ###
>**node**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;target node
>
## Methods ##
### public int getId () ###
>#### Method Overview ####
>Get the unique id for this injector
>
### public AbstractInsnNode getOriginalTarget () ###
>#### Method Overview ####
>Get the original target of this node
>
### public AbstractInsnNode getCurrentTarget () ###
>#### Method Overview ####
>Get the current target of this node, can be null if the node was
 replaced
>
### public InjectionNode replace (AbstractInsnNode) ###
>#### Method Overview ####
>Replace this node with the specified target
>
>### Parameters ###
>**target**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new node
>
### public InjectionNode remove () ###
>#### Method Overview ####
>Remove the node
>
### public boolean matches (AbstractInsnNode) ###
>#### Method Overview ####
>Checks whether the original or current target of this node match the
 specified node
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if the supplied node matches either of this node's
      internal identities
>
>### Parameters ###
>**node**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;node to check
>
### public boolean isReplaced () ###
>#### Method Overview ####
>Get whether this node has been replaced
>
### public boolean isRemoved () ###
>#### Method Overview ####
>Get whether this node has been removed
>
### public InjectionNode decorate (String, V) ###
>#### Method Overview ####
>Decorate this node with arbitrary metadata for injector arbitration
>
>### Parameters ###
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;meta key
>
>**value**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;meta value
>
### public boolean hasDecoration (String) ###
>#### Method Overview ####
>Get whether this node is decorated with the specified key
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if the specified decoration exists
>
>### Parameters ###
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;meta key
>
### public Object getDecoration (String) ###
>#### Method Overview ####
>Get the specified decoration
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;decoration value or null if absent
>
>### Parameters ###
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;meta key
>
### public int compareTo (InjectionNodes.InjectionNode) ###
>#### Method Overview ####
>No description provided
>
### public String toString () ###
>#### Method Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)