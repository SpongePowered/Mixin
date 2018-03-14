[< Back](../README.md)
# AccessorInfo #
>#### Class Overview ####
>Information about an accessor
## Fields ##
### protected static final Pattern PATTERN_ACCESSOR ###
>#### Field Overview ####
>Pattern for matching accessor names (for inflector)
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final Type argTypes ###
>#### Field Overview ####
>Accessor method argument types (raw, from method)
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final Type returnType ###
>#### Field Overview ####
>Accessor method return type (raw, from method)
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final AccessorType type ###
>#### Field Overview ####
>Type of accessor to generate, computed based on the signature of the
 target method.
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final MemberInfo target ###
>#### Field Overview ####
>Computed information about the target field or method, name and
 descriptor
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected FieldNode targetField ###
>#### Field Overview ####
>For accessors, stores the discovered target field
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected MethodNode targetMethod ###
>#### Field Overview ####
>For invokers, stores the discovered target method
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Constructors ##
### public AccessorInfo (MixinTargetContext, MethodNode) ###
>#### Constructor Overview ####
>No description provided
>
### protected AccessorInfo (MixinTargetContext, MethodNode, Class) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### protected AccessorType initType () ###
>#### Method Overview ####
>No description provided
>
### protected Type initTargetFieldType () ###
>#### Method Overview ####
>No description provided
>
### protected MemberInfo initTarget () ###
>#### Method Overview ####
>No description provided
>
### protected String getTargetName () ###
>#### Method Overview ####
>No description provided
>
### protected String inflectTarget () ###
>#### Method Overview ####
>Uses the name of this accessor method and the calculated accessor type to
 try and inflect the name of the target field or method. This allows a
 method named <tt>getFoo</tt> to be inflected to a target named
 <tt>foo</tt> for example.
>
### public static String inflectTarget (String, AccessorInfo.AccessorType, String, IMixinContext, boolean) ###
>#### Method Overview ####
>Uses the name of an accessor method and the accessor type to try and
 inflect the name of the target field or method. This allows a method
 named <tt>getFoo</tt> to be inflected to a target named <tt>foo</tt> for
 example.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;inflected target member name or <tt>null</tt> if name cannot be
      inflected
>
>### Parameters ###
>**accessorName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Name of the accessor method
>
>**accessorType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type of accessor being processed, this is calculated
      from the method signature (<tt>void</tt> methods being setters,
      methods with return types being getters)
>
>**accessorDescription**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;description of the accessor to include in
      error messages
>
>**context**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Mixin context
>
>**verbose**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Emit warnings when accessor prefix doesn't match type
>
### public final MemberInfo getTarget () ###
>#### Method Overview ####
>Get the inflected/specified target member for this accessor
>
### public final Type getTargetFieldType () ###
>#### Method Overview ####
>For field accessors, returns the field type, returns null for invokers
>
### public final FieldNode getTargetField () ###
>#### Method Overview ####
>For field accessors, returns the target field, returns null for invokers
>
### public final MethodNode getTargetMethod () ###
>#### Method Overview ####
>For invokers, returns the target method, returns null for field accessors
>
### public final Type getReturnType () ###
>#### Method Overview ####
>Get the return type of the annotated method
>
### public final Type getArgTypes () ###
>#### Method Overview ####
>Get the argument types of the annotated method
>
### public String toString () ###
>#### Method Overview ####
>No description provided
>
### public void locate () ###
>#### Method Overview ####
>First pass, locate the target field in the class. This is done after all
 other mixins are applied so that mixin-added fields and methods can be
 targetted.
>
### public MethodNode generate () ###
>#### Method Overview ####
>Second pass, generate the actual accessor method for this accessor. The
 method still respects intrinsic/mixinmerged rules so is not guaranteed to
 be added to the target class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;generated accessor method
>
### protected Object findTarget (List) ###
>#### Method Overview ####
>Generified candidate search, since the search logic is the same for both
 fields and methods.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;best match
>
>### Parameters ###
>**nodes**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Node list to search (method/field list)
>
### public static AccessorInfo of (MixinTargetContext, MethodNode, Class) ###
>#### Method Overview ####
>Return a wrapper AccessorInfo of the correct type based on the method
 passed in.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;parsed AccessorInfo
>
>### Parameters ###
>**mixin**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mixin context which owns this accessor
>
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;annotated method
>
>**type**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;annotation type to process
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)