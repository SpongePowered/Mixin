[< Back](../README.md)
# Bytecode #
>#### Class Overview ####
>Utility methods for working with bytecode via ASM
## Fields ##
### public static final int CONSTANTS_INT ###
>#### Field Overview ####
>Integer constant opcodes
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final int CONSTANTS_FLOAT ###
>#### Field Overview ####
>Float constant opcodes
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final int CONSTANTS_DOUBLE ###
>#### Field Overview ####
>Double constant opcodes
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final int CONSTANTS_LONG ###
>#### Field Overview ####
>Long constant opcodes
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final int CONSTANTS_ALL ###
>#### Field Overview ####
>All constant opcodes
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Methods ##
### public static MethodNode findMethod (ClassNode, String, String) ###
>#### Method Overview ####
>Finds a method given the method descriptor
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;discovered method node or null
>
>### Parameters ###
>**classNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the class to scan
>
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the method name
>
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the method descriptor
>
### public static AbstractInsnNode findInsn (MethodNode, int) ###
>#### Method Overview ####
>Find the first insn node with a matching opcode in the specified method
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;found node or null if not found
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method to search
>
>**opcode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;opcode to search for
>
### public static MethodInsnNode findSuperInit (MethodNode, String) ###
>#### Method Overview ####
>Find the call to <tt>super()</tt> in a constructor. This attempts to
 locate the first call to <tt>&lt;init&gt;</tt> which isn't an inline call
 to another object ctor being passed into the super invocation.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Call to <tt>super()</tt> or <tt>null</tt> if not found
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ctor to scan
>
>**superName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;name of superclass
>
### public static void textify (ClassNode, OutputStream) ###
>#### Method Overview ####
>Runs textifier on the specified class node and dumps the output to the
 specified output stream
>
>### Parameters ###
>**classNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;class to textify
>
>**out**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;output stream
>
### public static void textify (MethodNode, OutputStream) ###
>#### Method Overview ####
>Runs textifier on the specified method node and dumps the output to the
 specified output stream
>
>### Parameters ###
>**methodNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method to textify
>
>**out**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;output stream
>
### public static void dumpClass (ClassNode) ###
>#### Method Overview ####
>Dumps the output of CheckClassAdapter.verify to System.out
>
>### Parameters ###
>**classNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the classNode to verify
>
### public static void printMethodWithOpcodeIndices (MethodNode) ###
>#### Method Overview ####
>Prints a representation of a method's instructions to stderr
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method to print
>
### public static void printMethod (MethodNode) ###
>#### Method Overview ####
>Prints a representation of a method's instructions to stderr
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method to print
>
### public static void printNode (AbstractInsnNode) ###
>#### Method Overview ####
>Prints a representation of the specified insn node to stderr
>
>### Parameters ###
>**node**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Node to print
>
### public static String describeNode (AbstractInsnNode) ###
>#### Method Overview ####
>Gets a description of the supplied node for debugging purposes
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;human-readable description of node
>
>### Parameters ###
>**node**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;node to describe
>
### public static String getOpcodeName (AbstractInsnNode) ###
>#### Method Overview ####
>Uses reflection to find an approximate constant name match for the
 supplied node's opcode
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Approximate opcode name (approximate because some constants in
      the {@link Opcodes} class have the same value as opcodes
>
>### Parameters ###
>**node**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Node to query for opcode
>
### public static boolean methodHasLineNumbers (MethodNode) ###
>#### Method Overview ####
>Returns true if the supplied method contains any line number information
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if a line number node is located
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method to scan
>
### public static boolean methodIsStatic (MethodNode) ###
>#### Method Overview ####
>Returns true if the supplied method node is static
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if the method has the {@link Opcodes#ACC_STATIC} flag
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method node
>
### public static boolean fieldIsStatic (FieldNode) ###
>#### Method Overview ####
>Returns true if the supplied field node is static
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if the field has the {@link Opcodes#ACC_STATIC} flag
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;field node
>
### public static int getFirstNonArgLocalIndex (MethodNode) ###
>#### Method Overview ####
>Get the first variable index in the supplied method which is not an
 argument or "this" reference, this corresponds to the size of the
 arguments passed in to the method plus an extra spot for "this" if the
 method is non-static
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;first available local index which is NOT used by a method
      argument or "this"
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;MethodNode to inspect
>
### public static int getFirstNonArgLocalIndex (Type[], boolean) ###
>#### Method Overview ####
>Get the first non-arg variable index based on the supplied arg array and
 whether to include the "this" reference, this corresponds to the size of
 the arguments passed in to the method plus an extra spot for "this" is
 specified
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;first available local index which is NOT used by a method
      argument or "this"
>
>### Parameters ###
>**args**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method arguments
>
>**includeThis**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Whether to include a slot for "this" (generally true
      for all non-static methods)
>
### public static int getArgsSize (Type[]) ###
>#### Method Overview ####
>Get the size of the specified args array in local variable terms (eg.
 doubles and longs take two spaces)
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;size of the specified arguments array in terms of stack slots
>
>### Parameters ###
>**args**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method argument types as array
>
### public static void loadArgs (Type[], InsnList, int) ###
>#### Method Overview ####
>Injects appropriate LOAD opcodes into the supplied InsnList appropriate
 for each entry in the args array starting at pos
>
>### Parameters ###
>**args**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Argument types
>
>**insns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instruction List to inject into
>
>**pos**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Start position
>
### public static void loadArgs (Type[], InsnList, int, int) ###
>#### Method Overview ####
>Injects appropriate LOAD opcodes into the supplied InsnList appropriate
 for each entry in the args array starting at start and ending at end
>
>### Parameters ###
>**args**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Argument types
>
>**insns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instruction List to inject into
>
>**start**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Start position
>
>**end**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;End position
>
### public static void loadArgs (Type[], InsnList, int, int, Type[]) ###
>#### Method Overview ####
>Injects appropriate LOAD opcodes into the supplied InsnList appropriate
 for each entry in the args array starting at start and ending at end
>
>### Parameters ###
>**args**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Argument types
>
>**insns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instruction List to inject into
>
>**start**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Start position
>
>**end**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;End position
>
>**casts**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type casts array
>
### public static Map cloneLabels (InsnList) ###
>#### Method Overview ####
>Clones all of the labels in the source instruction list and returns the
 clones in a map of old label -&gt; new label. This is used to facilitate
 the use of {@link AbstractInsnNode#clone}.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;map of existing labels to their cloned counterparts
>
>### Parameters ###
>**source**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;instruction list
>
### public static String generateDescriptor (Object, Object[]) ###
>#### Method Overview ####
>Generate a bytecode descriptor from the supplied tokens. Each token can
 be a {@link Type}, a {@link Class} or otherwise is converted in-place by
 calling {@link Object#toString toString}.
>
>### Parameters ###
>**returnType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;object representing the method return type, can be
      <tt>null</tt> for <tt>void</tt>
>
>**args**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;objects representing argument types
>
### public static String getDescriptor (Type[]) ###
>#### Method Overview ####
>Generate a method descriptor without return type for the supplied args
 array
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method descriptor without return type
>
>### Parameters ###
>**args**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;argument types
>
### public static String getDescriptor (Type[], Type) ###
>#### Method Overview ####
>Generate a method descriptor with the specified types
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;generated method descriptor
>
>### Parameters ###
>**args**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;argument types
>
>**returnType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;return type
>
### public static String changeDescriptorReturnType (String, String) ###
>#### Method Overview ####
>Changes the return type of a method descriptor to the specified symbol
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;modified descriptor;
>
>### Parameters ###
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;descriptor to modify
>
>**returnType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new return type
>
### public static String getSimpleName (Class) ###
>#### Method Overview ####
>Returns the simple name of an annotation, mainly used for printing
 annotation names in error messages/user-facing strings
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;annotation's simple name
>
>### Parameters ###
>**annotationType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;annotation
>
### public static String getSimpleName (AnnotationNode) ###
>#### Method Overview ####
>Returns the simple name of an annotation, mainly used for printing
 annotation names in error messages/user-facing strings
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;annotation's simple name
>
>### Parameters ###
>**annotation**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;annotation node
>
### public static boolean isConstant (AbstractInsnNode) ###
>#### Method Overview ####
>Gets whether the supplied instruction is a constant instruction (eg. 
 <tt>ICONST_1</tt>)
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if the supplied instruction is a constant
>
>### Parameters ###
>**insn**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;instruction to check
>
### public static Object getConstant (AbstractInsnNode) ###
>#### Method Overview ####
>If the supplied instruction is a constant, returns the constant value
 from the instruction
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the constant value or <tt>null</tt> if the value cannot be parsed
      (or is null)
>
>### Parameters ###
>**insn**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;constant instruction to process
>
### public static Type getConstantType (AbstractInsnNode) ###
>#### Method Overview ####
>Returns the {@link Type} of a particular constant instruction's payload
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type of constant or <tt>null</tt> if it cannot be parsed (or is
      null)
>
>### Parameters ###
>**insn**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;constant instruction
>
### public static boolean hasFlag (ClassNode, int) ###
>#### Method Overview ####
>Check whether the specified flag is set on the specified class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;True if the specified flag is set in this method's access flags
>
>### Parameters ###
>**classNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;class node
>
>**flag**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;flag to check
>
### public static boolean hasFlag (MethodNode, int) ###
>#### Method Overview ####
>Check whether the specified flag is set on the specified method
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;True if the specified flag is set in this method's access flags
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method node
>
>**flag**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;flag to check
>
### public static boolean hasFlag (FieldNode, int) ###
>#### Method Overview ####
>Check whether the specified flag is set on the specified field
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;True if the specified flag is set in this field's access flags
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;field node
>
>**flag**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;flag to check
>
### public static boolean compareFlags (MethodNode, MethodNode, int) ###
>#### Method Overview ####
>Check whether the status of the specified flag matches on both of the
 supplied arguments.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;True if the flag is set to the same value on both members
>
>### Parameters ###
>**m1**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;First method
>
>**m2**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Second method
>
>**flag**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;flag to compare
>
### public static boolean compareFlags (FieldNode, FieldNode, int) ###
>#### Method Overview ####
>Check whether the status of the specified flag matches on both of the
 supplied arguments.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;True if the flag is set to the same value on both members
>
>### Parameters ###
>**f1**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;First field
>
>**f2**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Second field
>
>**flag**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;flag to compare
>
### public static Visibility getVisibility (MethodNode) ###
>#### Method Overview ####
>Returns the <i>ordinal visibility</i> of the supplied argument where a
 higher value equals higher "visibility":
 
 <ol start="0">
   <li>{@link Visibility#PRIVATE}</li>
   <li>{@link Visibility#PROTECTED}</li>
   <li>{@link Visibility#PACKAGE}</li>
   <li>{@link Visibility#PUBLIC}</li>
 </ol>
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;visibility level
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method to get visibility for
>
### public static Visibility getVisibility (FieldNode) ###
>#### Method Overview ####
>Returns the <i>ordinal visibility</i> of the supplied argument where a
 higher value equals higher "visibility":
 
 <ol start="0">
   <li>{@link Visibility#PRIVATE}</li>
   <li>{@link Visibility#PROTECTED}</li>
   <li>{@link Visibility#PACKAGE}</li>
   <li>{@link Visibility#PUBLIC}</li>
 </ol>
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;visibility level
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;field to get visibility for
>
### public static void setVisibility (MethodNode, Bytecode.Visibility) ###
>#### Method Overview ####
>Set the visibility of the specified member, leaving other access flags
 unchanged
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method to change
>
>**visibility**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new visibility
>
### public static void setVisibility (FieldNode, Bytecode.Visibility) ###
>#### Method Overview ####
>Set the visibility of the specified member, leaving other access flags
 unchanged
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;field to change
>
>**visibility**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new visibility
>
### public static void setVisibility (MethodNode, int) ###
>#### Method Overview ####
>Set the visibility of the specified member, leaving other access flags
 unchanged
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method to change
>
>**access**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new visibility
>
### public static void setVisibility (FieldNode, int) ###
>#### Method Overview ####
>Set the visibility of the specified member, leaving other access flags
 unchanged
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;field to change
>
>**access**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new visibility
>
### public static int getMaxLineNumber (ClassNode, int, int) ###
>#### Method Overview ####
>Compute the largest line number found in the specified class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;computed max
>
>### Parameters ###
>**classNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Class to inspect
>
>**min**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;minimum value to return
>
>**pad**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;amount to pad at the end of files
>
### public static String getBoxingType (Type) ###
>#### Method Overview ####
>Get the boxing type name for the specified type, if it is a primitive.
 For non-primitive types, <tt>null</tt> is returned
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;boxing type or null
>
>### Parameters ###
>**type**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type to box
>
### public static String getUnboxingMethod (Type) ###
>#### Method Overview ####
>Get the unboxing method name for the specified primitive type's
 corresponding reference type. For example, if the type passed in is
 <tt>int</tt>, then the return value will be <tt>intValue</tt>. Returns
 <tt>null</tt> for non-primitive types.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;unboxing method name or <tt>null</tt>
>
>### Parameters ###
>**type**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;primitive type to get unboxing method for
>
### public static void mergeAnnotations (ClassNode, ClassNode) ###
>#### Method Overview ####
>Merge annotations from the specified source ClassNode to the destination
 ClassNode, replaces annotations of the equivalent type on the target with
 annotations from the source. If the source node has no annotations then
 no action will take place, if the target node has no annotations then a
 new annotation list will be created. Annotations from the mixin package
 are not merged.
>
>### Parameters ###
>**from**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ClassNode to merge annotations from
>
>**to**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ClassNode to merge annotations to
>
### public static void mergeAnnotations (MethodNode, MethodNode) ###
>#### Method Overview ####
>Merge annotations from the specified source MethodNode to the destination
 MethodNode, replaces annotations of the equivalent type on the target
 with annotations from the source. If the source node has no annotations
 then no action will take place, if the target node has no annotations
 then a new annotation list will be created. Annotations from the mixin
 package are not merged.
>
>### Parameters ###
>**from**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;MethodNode to merge annotations from
>
>**to**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;MethodNode to merge annotations to
>
### public static void mergeAnnotations (FieldNode, FieldNode) ###
>#### Method Overview ####
>Merge annotations from the specified source FieldNode to the destination
 FieldNode, replaces annotations of the equivalent type on the target with
 annotations from the source. If the source node has no annotations then
 no action will take place, if the target node has no annotations then a
 new annotation list will be created. Annotations from the mixin package
 are not merged.
>
>### Parameters ###
>**from**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;FieldNode to merge annotations from
>
>**to**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;FieldNode to merge annotations to
>
### public static void compareBridgeMethods (MethodNode, MethodNode) ###
>#### Method Overview ####
>Compares two synthetic bridge methods and throws an exception if they are
 not compatible.
>
>### Parameters ###
>**a**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Incumbent method
>
>**b**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Incoming method
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)