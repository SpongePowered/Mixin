[< Back](../README.md)
# Locals #
>#### Class Overview ####
>Utility methods for working with local variables using ASM
## Methods ##
### public static void loadLocals (Type[], InsnList, int, int) ###
>#### Method Overview ####
>Injects appropriate LOAD opcodes into the supplied InsnList for each
 entry in the supplied locals array starting at pos
>
>### Parameters ###
>**locals**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Local types (can contain nulls for uninitialised, TOP, or
      RETURN values in locals)
>
>**insns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instruction List to inject into
>
>**pos**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Start position
>
>**limit**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;maximum number of locals to consume
>
### public static LocalVariableNode getLocalsAt (ClassNode, MethodNode, AbstractInsnNode) ###
>#### Method Overview ####
><p>Attempts to identify available locals at an arbitrary point in the
 bytecode specified by node.</p>
 
 <p>This method builds an approximate view of the locals available at an
 arbitrary point in the bytecode by examining the following features in
 the bytecode:</p> 
 <ul>
   <li>Any available stack map frames</li>
   <li>STORE opcodes</li>
   <li>The local variable table</li>
 </ul>
 
 <p>Inference proceeds by walking the bytecode from the start of the
 method looking for stack frames and STORE opcodes. When either of these
 is encountered, an attempt is made to cross-reference the values in the
 stack map or STORE opcode with the value in the local variable table
 which covers the code range. Stack map frames overwrite the entire
 simulated local variable table with their own value types, STORE opcodes
 overwrite only the local slot to which they pertain. Values in the
 simulated locals array are spaced according to their size (unlike the
 representation in FrameNode) and this TOP, NULL and UNINTITIALIZED_THIS
 opcodes will be represented as null values in the simulated frame.</p>
 
 <p>This code does not currently simulate the prescribed JVM behaviour
 where overwriting the second slot of a DOUBLE or LONG actually
 invalidates the DOUBLE or LONG stored in the previous location, so we
 have to hope (for now) that this behaviour isn't emitted by the compiler
 or any upstream transformers. I may have to re-think this strategy if
 this situation is encountered in the wild.</p>
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;A sparse array containing a view (hopefully) of the locals at the
      specified location
>
>### Parameters ###
>**classNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ClassNode containing the method, used to initialise the
      implicit "this" reference in simple methods with no stack frames
>
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;MethodNode to explore
>
>**node**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Node indicating the position at which to determine the locals
      state. The locals will be enumerated UP TO the specified node, so
      bear in mind that if the specified node is itself a STORE opcode,
      then we will be looking at the state of the locals PRIOR to its
      invocation
>
### public static LocalVariableNode getLocalVariableAt (ClassNode, MethodNode, AbstractInsnNode, int) ###
>#### Method Overview ####
>Attempts to locate the appropriate entry in the local variable table for
 the specified local variable index at the location specified by node.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;a LocalVariableNode containing information about the local
      variable at the specified location in the specified local slot
>
>### Parameters ###
>**classNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Containing class
>
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method
>
>**node**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instruction defining the location to get the local variable
      table at
>
>**var**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Local variable index
>
### public static List getLocalVariableTable (ClassNode, MethodNode) ###
>#### Method Overview ####
>Fetches or generates the local variable table for the specified method.
 Since Mojang strip the local variable table as part of the obfuscation
 process, we need to generate the local variable table when running
 obfuscated. We cache the generated tables so that we only need to do the
 relatively expensive calculation once per method we encounter.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;local variable table
>
>### Parameters ###
>**classNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Containing class
>
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method
>
### public static List getGeneratedLocalVariableTable (ClassNode, MethodNode) ###
>#### Method Overview ####
>Gets the generated the local variable table for the specified method.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;generated local variable table
>
>### Parameters ###
>**classNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Containing class
>
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method
>
### public static List generateLocalVariableTable (ClassNode, MethodNode) ###
>#### Method Overview ####
>Use ASM Analyzer to generate the local variable table for the specified
 method
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;generated local variable table
>
>### Parameters ###
>**classNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Containing class
>
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)