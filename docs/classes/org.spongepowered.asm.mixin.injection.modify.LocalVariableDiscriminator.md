[< Back](../README.md)
# public LocalVariableDiscriminator LocalVariableDiscriminator #
>#### Class Overview ####
>Encapsulates logic for identifying a local variable in a target method using
 3 criteria: <em>ordinal</em>, <em>index</em> and <em>name</em>. This is used
 by the {@link ModifyVariableInjector} and its associated injection points.
## Constructors ##
### public LocalVariableDiscriminator (boolean, int, int, Set, boolean) ###
>#### Constructor Overview ####
>No description provided
>
>### Parameters ###
>**argsOnly**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true to only search within the method arguments
>
>**ordinal**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;target variable ordinal
>
>**index**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;target variable index
>
>**names**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;target variable names
>
>**print**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true to print lvt
>
## Methods ##
### public boolean isArgsOnly () ###
>#### Method Overview ####
>True if this discriminator will examine only the target method args and
 won't consider the rest of the LVT at the target location
>
### public int getOrdinal () ###
>#### Method Overview ####
>Get the local variable ordinal (nth variable of type)
>
### public int getIndex () ###
>#### Method Overview ####
>Get the local variable absolute index
>
### public Set getNames () ###
>#### Method Overview ####
>Get valid names for consideration
>
### public boolean hasNames () ###
>#### Method Overview ####
>Returns true if names is not empty
>
### public boolean printLVT () ###
>#### Method Overview ####
>True if the injector should print the LVT
>
### protected boolean isImplicit (LocalVariableDiscriminator.Context) ###
>#### Method Overview ####
>If the user specifies no values for <tt>ordinal</tt>, <tt>index</tt> or 
 <tt>names</tt> then we are considered to be operating in "implicit mode"
 where only a single local variable of the specified type is expected to
 exist.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if operating in implicit mode
>
>### Parameters ###
>**context**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target context
>
### public int findLocal (Type, boolean, Target, AbstractInsnNode) ###
>#### Method Overview ####
>Find a matching local variable in the specified target
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;index of local or -1 if not matched
>
>### Parameters ###
>**returnType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;variable tyoe
>
>**argsOnly**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;only match in the method args
>
>**target**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;target method
>
>**node**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;current instruction
>
### public int findLocal (LocalVariableDiscriminator.Context) ###
>#### Method Overview ####
>Find a local variable for the specified context
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;index of local or -1 if not found
>
>### Parameters ###
>**context**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;search context
>
### public static LocalVariableDiscriminator parse (AnnotationNode) ###
>#### Method Overview ####
>Parse a local variable discriminator from the supplied annotation
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;discriminator configured using values from the annoation
>
>### Parameters ###
>**annotation**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;annotation to parse
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)