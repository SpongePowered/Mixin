[< Back](../README.md)
# TypeUtils #
>#### Class Overview ####
>Convenience functions for mirror types
## Methods ##
### public static PackageElement getPackage (TypeMirror) ###
>#### Method Overview ####
>If the supplied type is a {@link DeclaredType}, return the package in
 which it is declared
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;package for supplied type or null
>
>### Parameters ###
>**type**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type to find package for
>
### public static PackageElement getPackage (TypeElement) ###
>#### Method Overview ####
>Return the package in which the specified type element is declared
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;package for supplied type or null
>
>### Parameters ###
>**type**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type to find package for
>
### public static String getElementType (Element) ###
>#### Method Overview ####
>Convenience method to convert element to string representation for error
 messages
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;string representation of element name
>
>### Parameters ###
>**element**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Element to inspect
>
### public static String stripGenerics (String) ###
>#### Method Overview ####
>Strip generic arguments from the supplied type descriptor
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type descriptor with generic args removed
>
>### Parameters ###
>**type**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type descriptor
>
### public static String getName (VariableElement) ###
>#### Method Overview ####
>Get the name of the specified field
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;field name
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;field element
>
### public static String getName (ExecutableElement) ###
>#### Method Overview ####
>Get the name of the specified method
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method name
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method element
>
### public static String getJavaSignature (Element) ###
>#### Method Overview ####
>Get a java-style signature for the specified element (return type follows
 args) eg:
 
 <pre>(int,int)boolean</pre>
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;java signature
>
>### Parameters ###
>**element**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;element to generate java signature for
>
### public static String getJavaSignature (String) ###
>#### Method Overview ####
>Get a java-style signature from the specified bytecode descriptor
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;java signature
>
>### Parameters ###
>**descriptor**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;descriptor to convert to java signature
>
### public static String getTypeName (TypeMirror) ###
>#### Method Overview ####
>Get the type name for the specified type
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type name
>
>### Parameters ###
>**type**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type mirror
>
### public static String getTypeName (DeclaredType) ###
>#### Method Overview ####
>Get the type name for the specified type
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type name
>
>### Parameters ###
>**type**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type mirror
>
### public static String getDescriptor (Element) ###
>#### Method Overview ####
>Get a bytecode-style descriptor for the specified element
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;descriptor
>
>### Parameters ###
>**element**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;element to generate descriptor for
>
### public static String getDescriptor (ExecutableElement) ###
>#### Method Overview ####
>Get a bytecode-style descriptor for the specified method
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;descriptor
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method to generate descriptor for
>
### public static String getInternalName (VariableElement) ###
>#### Method Overview ####
>Get a bytecode-style descriptor for the specified field
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;descriptor
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;field to generate descriptor for
>
### public static String getInternalName (TypeMirror) ###
>#### Method Overview ####
>Get a bytecode-style descriptor for the specified type
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;descriptor
>
>### Parameters ###
>**type**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type to generate descriptor for
>
### public static String getInternalName (DeclaredType) ###
>#### Method Overview ####
>Get a bytecode-style name for the specified type
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;bytecode-style name
>
>### Parameters ###
>**type**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type to get name for
>
### public static String getInternalName (TypeElement) ###
>#### Method Overview ####
>Get a bytecode-style name for the specified type element
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;bytecode-style name
>
>### Parameters ###
>**element**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type element to get name for
>
### public static boolean isAssignable (ProcessingEnvironment, TypeMirror, TypeMirror) ###
>#### Method Overview ####
>Get whether the target type is assignable to the specified superclass
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if targetType is assignable to superClass
>
>### Parameters ###
>**processingEnv**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;processing environment
>
>**targetType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;target type to check
>
>**superClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;superclass type to check
>
### public static Visibility getVisibility (Element) ###
>#### Method Overview ####
>Get the ordinal visibility for the specified element
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;visibility level or null if element is null
>
>### Parameters ###
>**element**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;element to inspect
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)