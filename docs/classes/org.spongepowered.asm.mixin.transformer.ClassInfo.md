[< Back](../README.md)
# public final ClassInfo ClassInfo #
>#### Class Overview ####
>Information about a class, used as a way of keeping track of class hierarchy
 information needed to support more complex mixin behaviour such as detached
 superclass and mixin inheritance.
## Fields ##
### public static final int INCLUDE_PRIVATE ###
>#### Field Overview ####
>No description provided
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final int INCLUDE_STATIC ###
>#### Field Overview ####
>No description provided
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final int INCLUDE_ALL ###
>#### Field Overview ####
>No description provided
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Methods ##
### public Set getMixins () ###
>#### Method Overview ####
>Get all mixins which target this class
>
### public boolean isMixin () ###
>#### Method Overview ####
>Get whether this class is a mixin
>
### public boolean isPublic () ###
>#### Method Overview ####
>Get whether this class has ACC_PUBLIC
>
### public boolean isAbstract () ###
>#### Method Overview ####
>Get whether this class has ACC_ABSTRACT
>
### public boolean isSynthetic () ###
>#### Method Overview ####
>Get whether this class has ACC_SYNTHETIC
>
### public boolean isProbablyStatic () ###
>#### Method Overview ####
>Get whether this class is probably static (or is not an inner class)
>
### public boolean isInner () ###
>#### Method Overview ####
>Get whether this class is an inner class
>
### public boolean isInterface () ###
>#### Method Overview ####
>Get whether this is an interface or not
>
### public Set getInterfaces () ###
>#### Method Overview ####
>Returns the answer to life, the universe and everything
>
### public String toString () ###
>#### Method Overview ####
>No description provided
>
### public MethodMapper getMethodMapper () ###
>#### Method Overview ####
>No description provided
>
### public int getAccess () ###
>#### Method Overview ####
>No description provided
>
### public String getName () ###
>#### Method Overview ####
>Get the class name (binary name)
>
### public String getClassName () ###
>#### Method Overview ####
>Get the class name (java format)
>
### public String getSuperName () ###
>#### Method Overview ####
>Get the superclass name (binary name)
>
### public ClassInfo getSuperClass () ###
>#### Method Overview ####
>Get the superclass info, can return null if the superclass cannot be
 resolved
>
### public String getOuterName () ###
>#### Method Overview ####
>Get the name of the outer class, or null if this is not an inner class
>
### public ClassInfo getOuterClass () ###
>#### Method Overview ####
>Get the outer class info, can return null if the outer class cannot be
 resolved or if this is not an inner class
>
### public ClassSignature getSignature () ###
>#### Method Overview ####
>Return the class signature
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;signature as a {@link ClassSignature} instance
>
### public Set getMethods () ###
>#### Method Overview ####
>Get class/interface methods
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;read-only view of class methods
>
### public Set getInterfaceMethods (boolean) ###
>#### Method Overview ####
>If this is an interface, returns a set containing all methods in this
 interface and all super interfaces. If this is a class, returns a set
 containing all methods for all interfaces implemented by this class and
 all super interfaces of those interfaces.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;read-only view of class methods
>
>### Parameters ###
>**includeMixins**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Whether to include methods from mixins targeting
      this class info
>
### public boolean hasSuperClass (String) ###
>#### Method Overview ####
>Test whether this class has the specified superclass in its hierarchy
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if the specified class appears in the class's hierarchy
      anywhere
>
>### Parameters ###
>**superClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Name of the superclass to search for in the hierarchy
>
### public boolean hasSuperClass (String, ClassInfo.Traversal) ###
>#### Method Overview ####
>Test whether this class has the specified superclass in its hierarchy
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if the specified class appears in the class's hierarchy
      anywhere
>
>### Parameters ###
>**superClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Name of the superclass to search for in the hierarchy
>
>**traversal**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Traversal type to allow during this lookup
>
### public boolean hasSuperClass (ClassInfo) ###
>#### Method Overview ####
>Test whether this class has the specified superclass in its hierarchy
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if the specified class appears in the class's hierarchy
      anywhere
>
>### Parameters ###
>**superClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Superclass to search for in the hierarchy
>
### public boolean hasSuperClass (ClassInfo, ClassInfo.Traversal) ###
>#### Method Overview ####
>Test whether this class has the specified superclass in its hierarchy
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if the specified class appears in the class's hierarchy
      anywhere
>
>### Parameters ###
>**superClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Superclass to search for in the hierarchy
>
>**traversal**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Traversal type to allow during this lookup
>
### public boolean hasSuperClass (ClassInfo, ClassInfo.Traversal, boolean) ###
>#### Method Overview ####
>Test whether this class has the specified superclass in its hierarchy
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if the specified class appears in the class's hierarchy
      anywhere
>
>### Parameters ###
>**superClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Superclass to search for in the hierarchy
>
>**traversal**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Traversal type to allow during this lookup
>
>**includeInterfaces**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;True to include interfaces in the lookup
>
### public ClassInfo findSuperClass (String) ###
>#### Method Overview ####
>Search for the specified superclass in this class's hierarchy. If found
 returns the ClassInfo, otherwise returns null
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Matched superclass or null if not found
>
>### Parameters ###
>**superClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Superclass name to search for
>
### public ClassInfo findSuperClass (String, ClassInfo.Traversal) ###
>#### Method Overview ####
>Search for the specified superclass in this class's hierarchy. If found
 returns the ClassInfo, otherwise returns null
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Matched superclass or null if not found
>
>### Parameters ###
>**superClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Superclass name to search for
>
>**traversal**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Traversal type to allow during this lookup
>
### public ClassInfo findSuperClass (String, ClassInfo.Traversal, boolean) ###
>#### Method Overview ####
>Search for the specified superclass in this class's hierarchy. If found
 returns the ClassInfo, otherwise returns null
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Matched superclass or null if not found
>
>### Parameters ###
>**superClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Superclass name to search for
>
>**traversal**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Traversal type to allow during this lookup
>
>**includeInterfaces**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;True to include interfaces in the lookup
>
### public boolean hasMixinInHierarchy () ###
>#### Method Overview ####
>Find out whether this (mixin) class has another mixin in its superclass
 hierarchy. This method always returns false for non-mixin classes.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if and only if one or more mixins are found in the hierarchy
      of this mixin
>
### public boolean hasMixinTargetInHierarchy () ###
>#### Method Overview ####
>Find out whether this (non-mixin) class has a mixin targetting
 <em>any</em> of its superclasses. This method always returns false for
 mixin classes.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if and only if one or more classes in this class's hierarchy
      are targetted by a mixin
>
### public Method findMethodInHierarchy (MethodNode, ClassInfo.SearchType) ###
>#### Method Overview ####
>Finds the specified private or protected method in this class's hierarchy
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the method object or null if the method could not be resolved
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method to search for
>
>**searchType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Search strategy to use
>
### public Method findMethodInHierarchy (MethodNode, ClassInfo.SearchType, int) ###
>#### Method Overview ####
>Finds the specified private or protected method in this class's hierarchy
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the method object or null if the method could not be resolved
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method to search for
>
>**searchType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Search strategy to use
>
>**flags**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;search flags
>
### public Method findMethodInHierarchy (MethodInsnNode, ClassInfo.SearchType) ###
>#### Method Overview ####
>Finds the specified public or protected method in this class's hierarchy
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the method object or null if the method could not be resolved
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method to search for
>
>**searchType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Search strategy to use
>
### public Method findMethodInHierarchy (MethodInsnNode, ClassInfo.SearchType, int) ###
>#### Method Overview ####
>Finds the specified public or protected method in this class's hierarchy
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the method object or null if the method could not be resolved
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method to search for
>
>**searchType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Search strategy to use
>
>**flags**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;search flags
>
### public Method findMethodInHierarchy (String, String, ClassInfo.SearchType) ###
>#### Method Overview ####
>Finds the specified public or protected method in this class's hierarchy
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the method object or null if the method could not be resolved
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method name to search for
>
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method descriptor
>
>**searchType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Search strategy to use
>
### public Method findMethodInHierarchy (String, String, ClassInfo.SearchType, ClassInfo.Traversal) ###
>#### Method Overview ####
>Finds the specified public or protected method in this class's hierarchy
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the method object or null if the method could not be resolved
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method name to search for
>
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method descriptor
>
>**searchType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Search strategy to use
>
>**traversal**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Traversal type to allow during this lookup
>
### public Method findMethodInHierarchy (String, String, ClassInfo.SearchType, ClassInfo.Traversal, int) ###
>#### Method Overview ####
>Finds the specified public or protected method in this class's hierarchy
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the method object or null if the method could not be resolved
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method name to search for
>
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method descriptor
>
>**searchType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Search strategy to use
>
>**traversal**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Traversal type to allow during this lookup
>
>**flags**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;search flags
>
### public Field findFieldInHierarchy (FieldNode, ClassInfo.SearchType) ###
>#### Method Overview ####
>Finds the specified private or protected field in this class's hierarchy
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the field object or null if the field could not be resolved
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Field to search for
>
>**searchType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Search strategy to use
>
### public Field findFieldInHierarchy (FieldNode, ClassInfo.SearchType, int) ###
>#### Method Overview ####
>Finds the specified private or protected field in this class's hierarchy
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the field object or null if the field could not be resolved
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Field to search for
>
>**searchType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Search strategy to use
>
>**flags**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;search flags
>
### public Field findFieldInHierarchy (FieldInsnNode, ClassInfo.SearchType) ###
>#### Method Overview ####
>Finds the specified public or protected field in this class's hierarchy
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the field object or null if the field could not be resolved
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Field to search for
>
>**searchType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Search strategy to use
>
### public Field findFieldInHierarchy (FieldInsnNode, ClassInfo.SearchType, int) ###
>#### Method Overview ####
>Finds the specified public or protected field in this class's hierarchy
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the field object or null if the field could not be resolved
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Field to search for
>
>**searchType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Search strategy to use
>
>**flags**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;search flags
>
### public Field findFieldInHierarchy (String, String, ClassInfo.SearchType) ###
>#### Method Overview ####
>Finds the specified public or protected field in this class's hierarchy
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the field object or null if the field could not be resolved
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Field name to search for
>
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Field descriptor
>
>**searchType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Search strategy to use
>
### public Field findFieldInHierarchy (String, String, ClassInfo.SearchType, ClassInfo.Traversal) ###
>#### Method Overview ####
>Finds the specified public or protected field in this class's hierarchy
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the field object or null if the field could not be resolved
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Field name to search for
>
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Field descriptor
>
>**searchType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Search strategy to use
>
>**traversal**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Traversal type to allow during this lookup
>
### public Field findFieldInHierarchy (String, String, ClassInfo.SearchType, ClassInfo.Traversal, int) ###
>#### Method Overview ####
>Finds the specified public or protected field in this class's hierarchy
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the field object or null if the field could not be resolved
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Field name to search for
>
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Field descriptor
>
>**searchType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Search strategy to use
>
>**traversal**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Traversal type to allow during this lookup
>
>**flags**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;search flags
>
### public Method findMethod (MethodNode) ###
>#### Method Overview ####
>Finds the specified public or protected method in this class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the method object or null if the method could not be resolved
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method to search for
>
### public Method findMethod (MethodNode, int) ###
>#### Method Overview ####
>Finds the specified public or protected method in this class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the method object or null if the method could not be resolved
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method to search for
>
>**flags**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;search flags
>
### public Method findMethod (MethodInsnNode) ###
>#### Method Overview ####
>Finds the specified public or protected method in this class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the method object or null if the method could not be resolved
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method to search for
>
### public Method findMethod (MethodInsnNode, int) ###
>#### Method Overview ####
>Finds the specified public or protected method in this class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the method object or null if the method could not be resolved
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method to search for
>
>**flags**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;search flags
>
### public Method findMethod (String, String, int) ###
>#### Method Overview ####
>Finds the specified public or protected method in this class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the method object or null if the method could not be resolved
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method name to search for
>
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method signature to search for
>
>**flags**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;search flags
>
### public Field findField (FieldNode) ###
>#### Method Overview ####
>Finds the specified field in this class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the field object or null if the field could not be resolved
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Field to search for
>
### public Field findField (FieldInsnNode, int) ###
>#### Method Overview ####
>Finds the specified public or protected method in this class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the field object or null if the field could not be resolved
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Field to search for
>
>**flags**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;search flags
>
### public Field findField (String, String, int) ###
>#### Method Overview ####
>Finds the specified field in this class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the field object or null if the field could not be resolved
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Field name to search for
>
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Field signature to search for
>
>**flags**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;search flags
>
### public boolean equals (Object) ###
>#### Method Overview ####
>No description provided
>
### public int hashCode () ###
>#### Method Overview ####
>No description provided
>
### public static ClassInfo forName (String) ###
>#### Method Overview ####
>Return a ClassInfo for the specified class name, fetches the ClassInfo
 from the cache where possible
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ClassInfo for the specified class name or null if the specified
      name cannot be resolved for some reason
>
>### Parameters ###
>**className**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Binary name of the class to look up
>
### public static ClassInfo forType (org.spongepowered.asm.lib.Type) ###
>#### Method Overview ####
>Return a ClassInfo for the specified class type, fetches the ClassInfo
 from the cache where possible and generates the class meta if not.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ClassInfo for the supplied type or null if the supplied type
      cannot be found or is a primitive type
>
>### Parameters ###
>**type**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type to look up
>
### public static ClassInfo getCommonSuperClass (String, String) ###
>#### Method Overview ####
>ASM logic applied via ClassInfo, returns first common superclass of
 classes specified by <tt>type1</tt> and <tt>type2</tt>.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;common superclass info
>
>### Parameters ###
>**type1**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;First type
>
>**type2**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Second type
>
### public static ClassInfo getCommonSuperClass (org.spongepowered.asm.lib.Type, org.spongepowered.asm.lib.Type) ###
>#### Method Overview ####
>ASM logic applied via ClassInfo, returns first common superclass of
 classes specified by <tt>type1</tt> and <tt>type2</tt>.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;common superclass info
>
>### Parameters ###
>**type1**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;First type
>
>**type2**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Second type
>
### public static ClassInfo getCommonSuperClassOrInterface (String, String) ###
>#### Method Overview ####
>ASM logic applied via ClassInfo, returns first common superclass of
 classes specified by <tt>type1</tt> and <tt>type2</tt>.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;common superclass info
>
>### Parameters ###
>**type1**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;First type
>
>**type2**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Second type
>
### public static ClassInfo getCommonSuperClassOrInterface (org.spongepowered.asm.lib.Type, org.spongepowered.asm.lib.Type) ###
>#### Method Overview ####
>ASM logic applied via ClassInfo, returns first common superclass of
 classes specified by <tt>type1</tt> and <tt>type2</tt>.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;common superclass info
>
>### Parameters ###
>**type1**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;First type
>
>**type2**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Second type
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)