[< Back](../README.md)
# InjectionNodes #
>#### Class Overview ####
>Used to keep track of instruction nodes in a {@link Target} method which are
 targetted by various types of injector. This collection is populated during
 the first injector pass and allows injectors to keep track of their targets
 even when the target method is being manipulated by other injectors.
## Constructors ##
### public InjectionNodes () ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public InjectionNode add (AbstractInsnNode) ###
>#### Method Overview ####
>Add a tracked node to this collection if it does not already exist
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;wrapper for the specified node
>
>### Parameters ###
>**node**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instruction node to add
>
### public InjectionNode get (AbstractInsnNode) ###
>#### Method Overview ####
>Get a tracked node from this collection if it already exists, returns
 null if the node is not tracked
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;wrapper node or null if not tracked
>
>### Parameters ###
>**node**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;instruction node
>
### public boolean contains (AbstractInsnNode) ###
>#### Method Overview ####
>Get whether this collection contains a mapping for the specified insn
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if a wrapper exists for the node
>
>### Parameters ###
>**node**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;instruction node to check
>
### public void replace (AbstractInsnNode, AbstractInsnNode) ###
>#### Method Overview ####
>Replace the specified node with the new node, does not update the wrapper
 if no wrapper exists for <tt>oldNode</tt>
>
>### Parameters ###
>**oldNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;node being replaced
>
>**newNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;node to replace with
>
### public void remove (AbstractInsnNode) ###
>#### Method Overview ####
>Mark the specified node as removed, does not update the wrapper if no
 wrapper exists
>
>### Parameters ###
>**node**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;node being removed
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)