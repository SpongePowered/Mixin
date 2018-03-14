[< Back](../README.md)
# MemberInfo #
>#### Class Overview ####
><p>Information bundle about a member (method or field) parsed from a String
 token in another annotation, this is used where target members need to be
 specified as Strings in order to parse the String representation to something
 useful.</p>
 
 <p>Some examples:</p>
 <blockquote><pre>
   // references a method or field called func_1234_a, if there are multiple
   // members with the same signature, matches the first occurrence
   func_1234_a
   
   // references a method or field called func_1234_a, if there are multiple
   // members with the same signature, matches all occurrences
   func_1234_a*
   
   // references a method called func_1234_a which takes 3 ints and returns
   // a bool
   func_1234_a(III)Z
   
   // references a field called field_5678_z which is a String
   field_5678_z:Ljava/lang/String;
   
   // references a ctor which takes a single String argument 
   &lt;init&gt;(Ljava/lang/String;)V
   
   // references a method called func_1234_a in class foo.bar.Baz
   Lfoo/bar/Baz;func_1234_a
  
   // references a field called field_5678_z in class com.example.Dave
   Lcom/example/Dave;field_5678_z
  
   // references a method called func_1234_a in class foo.bar.Baz which takes
   // three doubles and returns void
   Lfoo/bar/Baz;func_1234_a(DDD)V
   
   // alternate syntax for the same
   foo.bar.Baz.func_1234_a(DDD)V</pre>
 </blockquote>
## Fields ##
### public final String owner ###
>#### Field Overview ####
>Member owner in internal form but without L;, can be null
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public final String name ###
>#### Field Overview ####
>Member name, can be null to match any member
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public final String desc ###
>#### Field Overview ####
>Member descriptor, can be null
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public final boolean matchAll ###
>#### Field Overview ####
>True to match all matching members, not just the first
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Constructors ##
### public MemberInfo (String, boolean) ###
>#### Constructor Overview ####
>ctor
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Member name, must not be null
>
>**matchAll**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if this info should match all matching references,
      or only the first
>
### public MemberInfo (String, String, boolean) ###
>#### Constructor Overview ####
>ctor
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Member name, must not be null
>
>**owner**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Member owner, can be null otherwise must be in internal form
      without L;
>
>**matchAll**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if this info should match all matching references,
      or only the first
>
### public MemberInfo (String, String, String) ###
>#### Constructor Overview ####
>ctor
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Member name, must not be null
>
>**owner**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Member owner, can be null otherwise must be in internal form
      without L;
>
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Member descriptor, can be null
>
### public MemberInfo (String, String, String, boolean) ###
>#### Constructor Overview ####
>ctor
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Member name, must not be null
>
>**owner**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Member owner, can be null otherwise must be in internal form
      without L;
>
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Member descriptor, can be null
>
>**matchAll**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;True to match all matching members, not just the first
>
### public MemberInfo (String, String, String, boolean, String) ###
>#### Constructor Overview ####
>ctor
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Member name, must not be null
>
>**owner**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Member owner, can be null otherwise must be in internal form
      without L;
>
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Member descriptor, can be null
>
>**matchAll**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;True to match all matching members, not just the first
>
### public MemberInfo (AbstractInsnNode) ###
>#### Constructor Overview ####
>Initialise a MemberInfo using the supplied insn which must be an instance
 of MethodInsnNode or FieldInsnNode.
>
>### Parameters ###
>**insn**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;instruction node to copy values from
>
## Methods ##
### public String toString () ###
>#### Method Overview ####
>No description provided
>
### public String toSrg () ###
>#### Method Overview ####
>Return this MemberInfo as an SRG mapping
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;SRG representation of this MemberInfo
>
### public String toDescriptor () ###
>#### Method Overview ####
>Returns this MemberInfo as a java-style descriptor
>
### public String toCtorType () ###
>#### Method Overview ####
>Returns the <em>constructor type</em> represented by this MemberInfo
>
### public String toCtorDesc () ###
>#### Method Overview ####
>Returns the <em>constructor descriptor</em> represented by this
 MemberInfo, returns null if no descriptor is present.
>
### public String getReturnType () ###
>#### Method Overview ####
>Get the return type for this MemberInfo, if the decriptor is present,
 returns null if the descriptor is absent or if this MemberInfo represents
 a field
>
### public IMapping asMapping () ###
>#### Method Overview ####
>Returns this MemberInfo as a {@link MappingField} or
 {@link MappingMethod}
>
### public MappingMethod asMethodMapping () ###
>#### Method Overview ####
>Returns this MemberInfo as a mapping method
>
### public MappingField asFieldMapping () ###
>#### Method Overview ####
>Returns this MemberInfo as a mapping field
>
### public boolean isFullyQualified () ###
>#### Method Overview ####
>Get whether this reference is fully qualified
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if all components of this reference are non-null
>
### public boolean isField () ###
>#### Method Overview ####
>Get whether this MemberInfo is definitely a field, the output of this
 method is undefined if {@link #isFullyQualified} returns false.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if this is definitely a field
>
### public boolean isConstructor () ###
>#### Method Overview ####
>Get whether this member represents a constructor
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if member name is <tt>&lt;init&gt;</tt>
>
### public boolean isClassInitialiser () ###
>#### Method Overview ####
>Get whether this member represents a class initialiser
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if member name is <tt>&lt;clinit&gt;</tt>
>
### public boolean isInitialiser () ###
>#### Method Overview ####
>Get whether this member represents a constructor or class initialiser
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if member name is <tt>&lt;init&gt;</tt> or
      <tt>&lt;clinit&gt;</tt>
>
### public MemberInfo validate () ###
>#### Method Overview ####
>Perform ultra-simple validation of the descriptor, checks that the parts
 of the descriptor are basically sane.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent
>
>### Throws ###
>**InvalidMemberDescriptorException**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;if any validation check fails
>
### public boolean matches (String, String, String) ###
>#### Method Overview ####
>Test whether this MemberInfo matches the supplied values. Null values are
 ignored.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if all non-null values in this reference match non-null
      arguments supplied to this method
>
>### Parameters ###
>**owner**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Owner to compare with, null to skip
>
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Name to compare with, null to skip
>
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Signature to compare with, null to skip
>
### public boolean matches (String, String, String, int) ###
>#### Method Overview ####
>Test whether this MemberInfo matches the supplied values at the specified
 ordinal. Null values are ignored.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if all non-null values in this reference match non-null
      arguments supplied to this method
>
>### Parameters ###
>**owner**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Owner to compare with, null to skip
>
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Name to compare with, null to skip
>
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Signature to compare with, null to skip
>
>**ordinal**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ordinal position within the class, used to honour the
      matchAll semantics
>
### public boolean matches (String, String) ###
>#### Method Overview ####
>Test whether this MemberInfo matches the supplied values. Null values are
 ignored.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if all non-null values in this reference match non-null
      arguments supplied to this method
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Name to compare with, null to skip
>
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Signature to compare with, null to skip
>
### public boolean matches (String, String, int) ###
>#### Method Overview ####
>Test whether this MemberInfo matches the supplied values at the specified
 ordinal. Null values are ignored.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if all non-null values in this reference match non-null
      arguments supplied to this method
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Name to compare with, null to skip
>
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Signature to compare with, null to skip
>
>**ordinal**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ordinal position within the class, used to honour the
      matchAll semantics
>
### public boolean equals (Object) ###
>#### Method Overview ####
>No description provided
>
### public int hashCode () ###
>#### Method Overview ####
>No description provided
>
### public MemberInfo move (String) ###
>#### Method Overview ####
>Create a new version of this member with a different owner
>
>### Parameters ###
>**newOwner**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;New owner for this member
>
### public MemberInfo transform (String) ###
>#### Method Overview ####
>Create a new version of this member with a different descriptor
>
>### Parameters ###
>**newDesc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;New descriptor for this member
>
### public MemberInfo remapUsing (MappingMethod, boolean) ###
>#### Method Overview ####
>Create a remapped version of this member using the supplied method data
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;New MethodInfo with remapped values
>
>### Parameters ###
>**srgMethod**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;SRG method data to use
>
>**setOwner**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;True to set the owner as well as the name
>
### public static MemberInfo parseAndValidate (String) ###
>#### Method Overview ####
>Parse a MemberInfo from a string and perform validation
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;parsed MemberInfo
>
>### Parameters ###
>**string**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;String to parse MemberInfo from
>
### public static MemberInfo parseAndValidate (String, IMixinContext) ###
>#### Method Overview ####
>Parse a MemberInfo from a string and perform validation
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;parsed MemberInfo
>
>### Parameters ###
>**string**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;String to parse MemberInfo from
>
>**context**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Context to use for reference mapping
>
### public static MemberInfo parse (String) ###
>#### Method Overview ####
>Parse a MemberInfo from a string
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;parsed MemberInfo
>
>### Parameters ###
>**string**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;String to parse MemberInfo from
>
### public static MemberInfo parse (String, IMixinContext) ###
>#### Method Overview ####
>Parse a MemberInfo from a string
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;parsed MemberInfo
>
>### Parameters ###
>**string**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;String to parse MemberInfo from
>
>**context**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Context to use for reference mapping
>
### public static MemberInfo fromMapping (IMapping) ###
>#### Method Overview ####
>Return the supplied mapping parsed as a MemberInfo
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new MemberInfo
>
>### Parameters ###
>**mapping**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mapping to parse
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)