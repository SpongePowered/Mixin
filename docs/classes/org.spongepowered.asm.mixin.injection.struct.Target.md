[< Back](../README.md)
# public Target Target #
>#### Class Overview ####
>Information about the current injection target, mainly just convenience
 rather than passing a bunch of values around.
## Fields ##
### public final ClassNode classNode ###
>#### Field Overview ####
>Target class node
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public final MethodNode method ###
>#### Field Overview ####
>Target method
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public final InsnList insns ###
>#### Field Overview ####
>Method instructions
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public final boolean isStatic ###
>#### Field Overview ####
>True if the method is static
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public final boolean isCtor ###
>#### Field Overview ####
>True if the method is a constructor
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public final Type arguments ###
>#### Field Overview ####
>Method arguments
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public final Type returnType ###
>#### Field Overview ####
>Return type computed from the method descriptor
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Constructors ##
### public Target (ClassNode, MethodNode) ###
>#### Constructor Overview ####
>Make a new Target for the supplied method
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;target method
>
## Methods ##
### public InjectionNode addInjectionNode (AbstractInsnNode) ###
>#### Method Overview ####
>Add an injection node to this target if it does not already exist,
 returns the existing node if it exists
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;wrapper for the specified node
>
>### Parameters ###
>**node**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instruction node to add
>
### public InjectionNode getInjectionNode (AbstractInsnNode) ###
>#### Method Overview ####
>Get an injection node from this collection if it already exists, returns
 null if the node is not tracked
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;wrapper node or null if not tracked
>
>### Parameters ###
>**node**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;instruction node
>
### public int getMaxLocals () ###
>#### Method Overview ####
>Get the original max locals of the method
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the original max locals value
>
### public int getMaxStack () ###
>#### Method Overview ####
>Get the original max stack of the method
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the original max stack value
>
### public int getCurrentMaxLocals () ###
>#### Method Overview ####
>Get the current max locals of the method
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the current max local value
>
### public int getCurrentMaxStack () ###
>#### Method Overview ####
>Get the current max stack of the method
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the current max stack value
>
### public int allocateLocal () ###
>#### Method Overview ####
>Allocate a new local variable for the method
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the allocated local index
>
### public int allocateLocals (int) ###
>#### Method Overview ####
>Allocate a number of new local variables for this method, returns the
 first local variable index of the allocated range
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the first local variable index of the allocated range
>
>### Parameters ###
>**locals**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;number of locals to allocate
>
### public void addToLocals (int) ###
>#### Method Overview ####
>Allocate a number of new local variables for this method
>
>### Parameters ###
>**locals**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;number of locals to allocate
>
### public void setMaxLocals (int) ###
>#### Method Overview ####
>Set the maxlocals for this target to the specified value, the specfied
 value must be higher than the original max locals
>
>### Parameters ###
>**maxLocals**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;max locals value to set
>
### public void addToStack (int) ###
>#### Method Overview ####
>Allocate a number of new stack variables for this method
>
>### Parameters ###
>**stack**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;number of stack entries to allocate
>
### public void setMaxStack (int) ###
>#### Method Overview ####
>Set the max stack size for this target to the specified value, the
 specfied value must be higher than the original max stack
>
>### Parameters ###
>**maxStack**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;max stack value to set
>
### public int generateArgMap (Type[], int) ###
>#### Method Overview ####
>Generate an array containing local indexes for the specified args,
 returns an array of identical size to the supplied array with an
 allocated local index in each corresponding position
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;array containing a corresponding local arg index for each member
      of the supplied args array
>
>### Parameters ###
>**args**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Argument types
>
>**start**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;starting index
>
### public int getArgIndices () ###
>#### Method Overview ####
>Get the argument indices for this target, calculated on first use
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;argument indices for this target
>
### public String getCallbackInfoClass () ###
>#### Method Overview ####
>Get the CallbackInfo class used for this target, based on the target
 return type
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;CallbackInfo class name
>
### public String getSimpleCallbackDescriptor () ###
>#### Method Overview ####
>Get "simple" callback descriptor (descriptor with only CallbackInfo)
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;generated descriptor
>
### public String getCallbackDescriptor (Type[], Type[]) ###
>#### Method Overview ####
>Get the callback descriptor
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;generated descriptor
>
>### Parameters ###
>**locals**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Local variable types
>
>**argumentTypes**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Argument types
>
### public String getCallbackDescriptor (boolean, Type[], Type[], int, int) ###
>#### Method Overview ####
>Get the callback descriptor
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;generated descriptor
>
>### Parameters ###
>**captureLocals**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;True if the callback is capturing locals
>
>**locals**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Local variable types
>
>**argumentTypes**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Argument types
>
>**startIndex**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;local index to start at
>
>**extra**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;extra locals to include
>
### public String toString () ###
>#### Method Overview ####
>No description provided
>
### public int compareTo (Target) ###
>#### Method Overview ####
>No description provided
>
### public int indexOf (InjectionNodes.InjectionNode) ###
>#### Method Overview ####
>Return the index of the specified instruction in this instruction list
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;opcode index
>
>### Parameters ###
>**node**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;instruction to locate, must exist in the target
>
### public int indexOf (AbstractInsnNode) ###
>#### Method Overview ####
>Return the index of the specified instruction in this instruction list
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;opcode index
>
>### Parameters ###
>**insn**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;instruction to locate, must exist in the target
>
### public AbstractInsnNode get (int) ###
>#### Method Overview ####
>Return the instruction at the specified index
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;requested instruction
>
>### Parameters ###
>**index**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;opcode index
>
### public Iterator iterator () ###
>#### Method Overview ####
>No description provided
>
### public MethodInsnNode findInitNodeFor (TypeInsnNode) ###
>#### Method Overview ####
>Find the first <tt>&lt;init&gt;</tt> invocation after the specified
 <tt>NEW</tt> insn
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;INVOKESPECIAL opcode of ctor, or null if not found
>
>### Parameters ###
>**newNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;NEW insn
>
### public MethodInsnNode findSuperInitNode () ###
>#### Method Overview ####
>Find the call to <tt>super()</tt> in a constructor. This attempts to
 locate the first call to <tt>&lt;init&gt;</tt> which isn't an inline call
 to another object ctor being passed into the super invocation.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Call to <tt>super()</tt> or <tt>null</tt> if not found
>
### public void insertBefore (InjectionNodes.InjectionNode, InsnList) ###
>#### Method Overview ####
>Insert the supplied instructions before the specified instruction
>
>### Parameters ###
>**location**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instruction to insert before
>
>**insns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instructions to insert
>
### public void insertBefore (AbstractInsnNode, InsnList) ###
>#### Method Overview ####
>Insert the supplied instructions before the specified instruction
>
>### Parameters ###
>**location**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instruction to insert before
>
>**insns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instructions to insert
>
### public void replaceNode (AbstractInsnNode, AbstractInsnNode) ###
>#### Method Overview ####
>Replace an instruction in this target with the specified instruction and
 mark the node as replaced for other injectors
>
>### Parameters ###
>**location**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instruction to replace
>
>**insn**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instruction to replace with
>
### public void replaceNode (AbstractInsnNode, AbstractInsnNode, InsnList) ###
>#### Method Overview ####
>Replace an instruction in this target with the specified instructions and
 mark the node as replaced with the specified champion node from the list.
>
>### Parameters ###
>**location**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instruction to replace
>
>**champion**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instruction which notionally replaces the original insn
>
>**insns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instructions to actually insert (must contain champion)
>
### public void wrapNode (AbstractInsnNode, AbstractInsnNode, InsnList, InsnList) ###
>#### Method Overview ####
>Wrap instruction in this target with the specified instructions and mark
 the node as replaced with the specified champion node from the list.
>
>### Parameters ###
>**location**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instruction to replace
>
>**champion**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instruction which notionally replaces the original insn
>
>**before**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instructions to actually insert (must contain champion)
>
>**after**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instructions to insert after the specified location
>
### public void replaceNode (AbstractInsnNode, InsnList) ###
>#### Method Overview ####
>Replace an instruction in this target with the specified instructions and
 mark the original node as removed
>
>### Parameters ###
>**location**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instruction to replace
>
>**insns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instructions to replace with
>
### public void removeNode (AbstractInsnNode) ###
>#### Method Overview ####
>Remove the specified instruction from the target and mark it as removed
 for injections
>
>### Parameters ###
>**insn**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;instruction to remove
>
### public void addLocalVariable (int, String, String) ###
>#### Method Overview ####
>Add an entry to the target LVT
>
>### Parameters ###
>**index**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;local variable index
>
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;local variable name
>
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;local variable type
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)