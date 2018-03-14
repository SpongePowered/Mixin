[< Back](../README.md)
# MixinTargetContext #
>#### Class Overview ####
>This object keeps track of data for applying a mixin to a specific target
 class <em>during</em> a mixin application. This is a single-use object which
 acts as both a handle information we need when applying the mixin (such as
 the actual mixin ClassNode and the target ClassNode) and a gateway to
 context-sensitive operations such as re-targetting method and field accesses
 in the mixin to the appropriate members in the target class hierarchy.
## Methods ##
### public String toString () ###
>#### Method Overview ####
>No description provided
>
### public MixinEnvironment getEnvironment () ###
>#### Method Overview ####
>Get the environment of the owning mixin config
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mixin parent environment
>
### public boolean getOption (MixinEnvironment.Option) ###
>#### Method Overview ####
>No description provided
>
### public ClassNode getClassNode () ###
>#### Method Overview ####
>Get the mixin tree
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mixin tree
>
### public String getClassName () ###
>#### Method Overview ####
>Get the mixin class name
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the mixin class name
>
### public String getClassRef () ###
>#### Method Overview ####
>No description provided
>
### public TargetClassContext getTarget () ###
>#### Method Overview ####
>Get the target class context
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the target class context
>
### public String getTargetClassRef () ###
>#### Method Overview ####
>Get the target class reference
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the reference of the target class (only valid on single-target
      mixins)
>
### public ClassNode getTargetClassNode () ###
>#### Method Overview ####
>Get the target class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the target class
>
### public ClassInfo getTargetClassInfo () ###
>#### Method Overview ####
>Get the target classinfo
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the target class info
>
### protected ClassInfo getClassInfo () ###
>#### Method Overview ####
>Get the class info for this class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the local class info
>
### public ClassSignature getSignature () ###
>#### Method Overview ####
>Get the signature for this mixin class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;signature
>
### public File getStratum () ###
>#### Method Overview ####
>Get the SourceMap stratum for this mixin
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;stratum
>
### public int getMinRequiredClassVersion () ###
>#### Method Overview ####
>Get the minimum required class version for this mixin
>
### public int getDefaultRequiredInjections () ###
>#### Method Overview ####
>Get the defined value for the {@link Inject#require} parameter on
 injectors defined in mixins in this configuration.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;default require value
>
### public String getDefaultInjectorGroup () ###
>#### Method Overview ####
>Get the defined injector group for injectors
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;default group name
>
### public int getMaxShiftByValue () ###
>#### Method Overview ####
>Get the max shift "by" value for the parent config
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;max shift by value
>
### public Map getInjectorGroups () ###
>#### Method Overview ####
>Get the injector groups for this target
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;injector groups
>
### public boolean requireOverwriteAnnotations () ###
>#### Method Overview ####
>Get whether overwrite annotations are required for methods in this mixin
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if overwrite methods must be annoated with {@link Overwrite}
>
### public ClassInfo findRealType (ClassInfo) ###
>#### Method Overview ####
>Find the corresponding class type for the supplied mixin class in this
 mixin target's hierarchy
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Transformed
>
>### Parameters ###
>**mixin**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Mixin class to discover
>
### public void transformMethod (MethodNode) ###
>#### Method Overview ####
>Handles "re-parenting" the method supplied, changes all references to the
 mixin class to refer to the target class (for field accesses and method
 invocations) and also handles fixing up the targets of INVOKESPECIAL
 opcodes for mixins with detached targets.
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method to transform
>
### public void transformDescriptor (FieldNode) ###
>#### Method Overview ####
>Transforms a field descriptor in the context of this mixin target
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Field node to transform
>
### public void transformDescriptor (MethodNode) ###
>#### Method Overview ####
>Transforms a method descriptor in the context of this mixin target
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method node to transform
>
### public void transformDescriptor (TypeInsnNode) ###
>#### Method Overview ####
>Transforms a type insn descriptor in the context of this mixin target
>
>### Parameters ###
>**typeInsn**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type instruction node to transform
>
### public Target getTargetMethod (MethodNode) ###
>#### Method Overview ####
>Get a target method handle from the target class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new or existing target handle for the supplied method
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method to get a target handle for
>
### protected void requireVersion (int) ###
>#### Method Overview ####
>Mark this mixin as requiring the specified class version in the context
 of the current target
>
>### Parameters ###
>**version**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;version to require
>
### public Extensions getExtensions () ###
>#### Method Overview ####
>No description provided
>
### public IMixinInfo getMixin () ###
>#### Method Overview ####
>No description provided
>
### public int getPriority () ###
>#### Method Overview ####
>Get the mixin priority
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the priority (only meaningful in relation to other mixins)
>
### public Set getInterfaces () ###
>#### Method Overview ####
>Get all interfaces for this mixin
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mixin interfaces
>
### public Collection getShadowMethods () ###
>#### Method Overview ####
>Get shadow methods in this mixin
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;shadow methods in the mixin
>
### public List getMethods () ###
>#### Method Overview ####
>Get methods to mixin
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;non-shadow methods in the mixin
>
### public Set getShadowFields () ###
>#### Method Overview ####
>Get shadow fields in this mixin
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;shadow fields in the mixin
>
### public List getFields () ###
>#### Method Overview ####
>Get fields to mixin
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;non-shadow fields in the mixin
>
### public Level getLoggingLevel () ###
>#### Method Overview ####
>Get the logging level for this mixin
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the logging level
>
### public boolean shouldSetSourceFile () ###
>#### Method Overview ####
>Get whether to propogate the source file attribute from a mixin onto the
 target class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if the sourcefile property should be set on the target class
>
### public String getSourceFile () ###
>#### Method Overview ####
>Return the source file name for the mixin
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mixin source file
>
### public IReferenceMapper getReferenceMapper () ###
>#### Method Overview ####
>No description provided
>
### public void preApply (String, ClassNode) ###
>#### Method Overview ####
>Called immediately before the mixin is applied to targetClass
>
>### Parameters ###
>**transformedName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target class's transformed name
>
>**targetClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target class
>
### public void postApply (String, ClassNode) ###
>#### Method Overview ####
>Called immediately after the mixin is applied to targetClass
>
>### Parameters ###
>**transformedName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target class's transformed name
>
>**targetClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target class
>
### public String getUniqueName (MethodNode, boolean) ###
>#### Method Overview ####
>Obtain a unique name for the specified method from the target class
 context
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;unique method name
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method to obtain a name for
>
>**preservePrefix**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true to preserve the method prefix (decorate as 
      postfix) otherwise decorates as infix
>
### public String getUniqueName (FieldNode) ###
>#### Method Overview ####
>Obtain a unique name for the specified field from the target class
 context
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;unique field name
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;field to obtain a name for
>
### public void prepareInjections () ###
>#### Method Overview ####
>Scans the target class for injector methods and prepares discovered
 injectors
>
### public void applyInjections () ###
>#### Method Overview ####
>Apply injectors discovered in the {@link #prepareInjections()} pass
>
### public List generateAccessors () ###
>#### Method Overview ####
>Expand accessor methods mixed into the target class by populating the
 method bodies
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)