[< Back](../README.md)
# public TypeHandle TypeHandle #
>#### Class Overview ####
>A wrapper for TypeElement which gives us a soft-failover mechanism when
 dealing with classes that are inaccessible via mirror (such as anonymous
 inner classes).
## Constructors ##
### public TypeHandle (PackageElement, String) ###
>#### Constructor Overview ####
>Ctor for imaginary elements, require the enclosing package and the FQ
 name
>
>### Parameters ###
>**pkg**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Package
>
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;FQ class name
>
### public TypeHandle (TypeElement) ###
>#### Constructor Overview ####
>Ctor for real elements
>
>### Parameters ###
>**element**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ze element
>
### public TypeHandle (DeclaredType) ###
>#### Constructor Overview ####
>Ctor for real elements, instanced via a type mirror
>
>### Parameters ###
>**type**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type
>
## Methods ##
### public final String toString () ###
>#### Method Overview ####
>No description provided
>
### public final String getName () ###
>#### Method Overview ####
>Returns the fully qualified class name
>
### public final PackageElement getPackage () ###
>#### Method Overview ####
>Returns the enclosing package element
>
### public final TypeElement getElement () ###
>#### Method Overview ####
>Returns the actual element (returns null for imaginary elements)
>
### protected TypeElement getTargetElement () ###
>#### Method Overview ####
>Returns the actual element (returns simulated value for imaginary
 elements)
>
### public AnnotationHandle getAnnotation (Class) ###
>#### Method Overview ####
>Get an annotation handle for the specified annotation on this type
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new annotation handle, call <tt>exists</tt> on the returned
      handle to determine whether the annotation is present
>
>### Parameters ###
>**annotationClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type of annotation to search for
>
### public final List getEnclosedElements () ###
>#### Method Overview ####
>Returns enclosed elements (methods, fields, etc.)
>
### public List getEnclosedElements (ElementKind[]) ###
>#### Method Overview ####
>Returns enclosed elements (methods, fields, etc.) of a particular type
>
>### Parameters ###
>**kind**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;types of element to return
>
### public TypeMirror getType () ###
>#### Method Overview ####
>Returns the enclosed element as a type mirror, or null if this is an
 imaginary type
>
### public TypeHandle getSuperclass () ###
>#### Method Overview ####
>Returns the enclosed element's superclass if available, or null if this
 class does not have a superclass
>
### public List getInterfaces () ###
>#### Method Overview ####
>Get interfaces directly implemented by this type
>
### public boolean isPublic () ###
>#### Method Overview ####
>Get whether the element is probably public
>
### public boolean isImaginary () ###
>#### Method Overview ####
>Get whether the element is imaginary (inaccessible via mirror)
>
### public boolean isSimulated () ###
>#### Method Overview ####
>Get whether this handle is simulated
>
### public final TypeReference getReference () ###
>#### Method Overview ####
>Get the TypeReference for this type, used for serialisation
>
### public MappingMethod getMappingMethod (String, String) ###
>#### Method Overview ####
>Return a method as a remapping candidate, usually returns a method owned
 by this class but for simulated handles we resolve the reference in the
 class hierarchy of the simulated target (eg. self)
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;this handle as a mapping method
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method name
>
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method descriptor
>
### public String findDescriptor (MemberInfo) ###
>#### Method Overview ####
>Find a descriptor for the supplied MemberInfo
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;descriptor or null if no matching member could be located
>
>### Parameters ###
>**memberInfo**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;MemberInfo to use as search term
>
### public final FieldHandle findField (VariableElement) ###
>#### Method Overview ####
>Find a member field in this type which matches the name and declared type
 of the supplied element
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;handle to the discovered field if matched or null if no match
>
>### Parameters ###
>**element**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Element to match
>
### public final FieldHandle findField (VariableElement, boolean) ###
>#### Method Overview ####
>Find a member field in this type which matches the name and declared type
 of the supplied element
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;handle to the discovered field if matched or null if no match
>
>### Parameters ###
>**element**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Element to match
>
>**caseSensitive**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;True if case-sensitive comparison should be used
>
### public final FieldHandle findField (String, String) ###
>#### Method Overview ####
>Find a member field in this type which matches the name and declared type
 specified
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;handle to the discovered field if matched or null if no match
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Field name to search for
>
>**type**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Field descriptor (java-style)
>
### public FieldHandle findField (String, String, boolean) ###
>#### Method Overview ####
>Find a member field in this type which matches the name and declared type
 specified
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;handle to the discovered field if matched or null if no match
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Field name to search for
>
>**type**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Field descriptor (java-style)
>
>**caseSensitive**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;True if case-sensitive comparison should be used
>
### public final MethodHandle findMethod (ExecutableElement) ###
>#### Method Overview ####
>Find a member method in this type which matches the name and declared
 type of the supplied element
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;handle to the discovered method if matched or null if no match
>
>### Parameters ###
>**element**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Element to match
>
### public final MethodHandle findMethod (ExecutableElement, boolean) ###
>#### Method Overview ####
>Find a member method in this type which matches the name and declared
 type of the supplied element
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;handle to the discovered method if matched or null if no match
>
>### Parameters ###
>**element**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Element to match
>
>**caseSensitive**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;True if case-sensitive comparison should be used
>
### public final MethodHandle findMethod (String, String) ###
>#### Method Overview ####
>Find a member method in this type which matches the name and signature
 specified
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;handle to the discovered method if matched or null if no match
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method name to search for
>
>**signature**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method signature
>
### public MethodHandle findMethod (String, String, boolean) ###
>#### Method Overview ####
>Find a member method in this type which matches the name and signature
 specified
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;handle to the discovered method if matched or null if no match
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method name to search for
>
>**signature**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method signature
>
>**matchCase**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;True if case-sensitive comparison should be used
>
### protected static MethodHandle findMethod (TypeHandle, String, String, String, boolean) ###
>#### Method Overview ####
>No description provided
>
### protected static boolean compareElement (Element, String, String, boolean) ###
>#### Method Overview ####
>No description provided
>
### protected static List getEnclosedElements (TypeElement, ElementKind[]) ###
>#### Method Overview ####
>No description provided
>
### protected static List getEnclosedElements (TypeElement) ###
>#### Method Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)