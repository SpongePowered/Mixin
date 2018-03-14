[< Back](../README.md)
# InjectionPointData #
>#### Class Overview ####
>Data read from an {@link org.spongepowered.asm.mixin.injection.At} annotation
 and passed into an InjectionPoint ctor
## Constructors ##
### public InjectionPointData (IMixinContext, MethodNode, AnnotationNode, String, List, String, String, int, int, String) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public String getAt () ###
>#### Method Overview ####
>Get the <tt>at</tt> value on the injector
>
### public String getType () ###
>#### Method Overview ####
>Get the parsed constructor <tt>type</tt> for this injector
>
### public Selector getSelector () ###
>#### Method Overview ####
>Get the selector value parsed from the injector
>
### public IMixinContext getContext () ###
>#### Method Overview ####
>Get the context
>
### public MethodNode getMethod () ###
>#### Method Overview ####
>Get the annotated method
>
### public Type getMethodReturnType () ###
>#### Method Overview ####
>Get the return type of the annotated method
>
### public AnnotationNode getParent () ###
>#### Method Overview ####
>Get the root annotation (eg. {@link Inject})
>
### public String getSlice () ###
>#### Method Overview ####
>Get the slice id specified on the injector
>
### public LocalVariableDiscriminator getLocalVariableDiscriminator () ###
>#### Method Overview ####
>No description provided
>
### public String get (String, String) ###
>#### Method Overview ####
>Get the supplied value from the named args, return defaultValue if the
 arg is not set
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;argument value or default if not set
>
>### Parameters ###
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;argument name
>
>**defaultValue**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;value to return if the arg is not set
>
### public int get (String, int) ###
>#### Method Overview ####
>Get the supplied value from the named args, return defaultValue if the
 arg is not set
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;argument value or default if not set
>
>### Parameters ###
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;argument name
>
>**defaultValue**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;value to return if the arg is not set
>
### public boolean get (String, boolean) ###
>#### Method Overview ####
>Get the supplied value from the named args, return defaultValue if the
 arg is not set
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;argument value or default if not set
>
>### Parameters ###
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;argument name
>
>**defaultValue**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;value to return if the arg is not set
>
### public MemberInfo get (String) ###
>#### Method Overview ####
>Get the supplied value from the named args as a {@link MemberInfo},
 throws an exception if the argument cannot be parsed as a MemberInfo.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;argument value as a MemberInfo
>
>### Parameters ###
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;argument name
>
### public MemberInfo getTarget () ###
>#### Method Overview ####
>Get the target value specified on the injector
>
### public int getOrdinal () ###
>#### Method Overview ####
>Get the ordinal specified on the injection point
>
### public int getOpcode () ###
>#### Method Overview ####
>Get the opcode specified on the injection point
>
### public int getOpcode (int) ###
>#### Method Overview ####
>Get the opcode specified on the injection point or return the default if
 no opcode was specified
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;opcode or default
>
>### Parameters ###
>**defaultOpcode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;opcode to return if none specified
>
### public int getOpcode (int, int[]) ###
>#### Method Overview ####
>Get the opcode specified on the injection point or return the default if
 no opcode was specified or if the specified opcode does not appear in the
 supplied list of valid opcodes
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;opcode or default
>
>### Parameters ###
>**defaultOpcode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;opcode to return if none specified
>
>**validOpcodes**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;valid opcodes
>
### public String getId () ###
>#### Method Overview ####
>Get the id specified on the injection point (or null if not specified)
>
### public String toString () ###
>#### Method Overview ####
>No description provided
>
### public static String parseType (String) ###
>#### Method Overview ####
>Parse a constructor type from the supplied <tt>at</tt> string
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;parsed constructor type
>
>### Parameters ###
>**at**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;at to parse
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)