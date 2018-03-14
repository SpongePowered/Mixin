[< Back](../README.md)
# public final Annotations Annotations #
>#### Class Overview ####
>Utility class for working with ASM annotations
## Methods ##
### public static void setVisible (FieldNode, Class, Object[]) ###
>#### Method Overview ####
>Set a runtime-visible annotation of the specified class on the supplied
 field node
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target field
>
>**annotationClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type of annotation to search for
>
>**value**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Values (interleaved key/value pairs) to set
>
### public static void setInvisible (FieldNode, Class, Object[]) ###
>#### Method Overview ####
>Set an invisible annotation of the specified class on the supplied field
 node
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target field
>
>**annotationClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type of annotation to search for
>
>**value**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Values (interleaved key/value pairs) to set
>
### public static void setVisible (MethodNode, Class, Object[]) ###
>#### Method Overview ####
>Set a runtime-visible annotation of the specified class on the supplied
 method node
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target method
>
>**annotationClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type of annotation to search for
>
>**value**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Values (interleaved key/value pairs) to set
>
### public static void setInvisible (MethodNode, Class, Object[]) ###
>#### Method Overview ####
>Set a invisible annotation of the specified class on the supplied method
 node
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target method
>
>**annotationClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type of annotation to search for
>
>**value**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Values (interleaved key/value pairs) to set
>
### public static AnnotationNode getVisible (FieldNode, Class) ###
>#### Method Overview ####
>Get a runtime-visible annotation of the specified class from the supplied
 field node
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the annotation, or null if not present
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Source field
>
>**annotationClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type of annotation to search for
>
### public static AnnotationNode getInvisible (FieldNode, Class) ###
>#### Method Overview ####
>Get an invisible annotation of the specified class from the supplied
 field node
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the annotation, or null if not present
>
>### Parameters ###
>**field**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Source field
>
>**annotationClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type of annotation to search for
>
### public static AnnotationNode getVisible (MethodNode, Class) ###
>#### Method Overview ####
>Get a runtime-visible annotation of the specified class from the supplied
 method node
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the annotation, or null if not present
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Source method
>
>**annotationClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type of annotation to search for
>
### public static AnnotationNode getInvisible (MethodNode, Class) ###
>#### Method Overview ####
>Get an invisible annotation of the specified class from the supplied
 method node
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the annotation, or null if not present
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Source method
>
>**annotationClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type of annotation to search for
>
### public static AnnotationNode getSingleVisible (MethodNode, Class[]) ###
>#### Method Overview ####
>Get a runtime-visible annotation of the specified class from the supplied
 method node
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the annotation, or null if not present
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Source method
>
>**annotationClasses**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Types of annotation to search for
>
### public static AnnotationNode getSingleInvisible (MethodNode, Class[]) ###
>#### Method Overview ####
>Get an invisible annotation of the specified class from the supplied
 method node
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the annotation, or null if not present
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Source method
>
>**annotationClasses**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Types of annotation to search for
>
### public static AnnotationNode getVisible (ClassNode, Class) ###
>#### Method Overview ####
>Get a runtime-visible annotation of the specified class from the supplied
 class node
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the annotation, or null if not present
>
>### Parameters ###
>**classNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Source classNode
>
>**annotationClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type of annotation to search for
>
### public static AnnotationNode getInvisible (ClassNode, Class) ###
>#### Method Overview ####
>Get an invisible annotation of the specified class from the supplied
 class node
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the annotation, or null if not present
>
>### Parameters ###
>**classNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Source classNode
>
>**annotationClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type of annotation to search for
>
### public static AnnotationNode getVisibleParameter (MethodNode, Class, int) ###
>#### Method Overview ####
>Get a runtime-visible parameter annotation of the specified class from
 the supplied method node
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the annotation, or null if not present
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Source method
>
>**annotationClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type of annotation to search for
>
>**paramIndex**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Index of the parameter to fetch annotation for
>
### public static AnnotationNode getInvisibleParameter (MethodNode, Class, int) ###
>#### Method Overview ####
>Get an invisible parameter annotation of the specified class from the
 supplied method node
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the annotation, or null if not present
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Source method
>
>**annotationClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type of annotation to search for
>
>**paramIndex**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Index of the parameter to fetch annotation for
>
### public static AnnotationNode getParameter (List[], String, int) ###
>#### Method Overview ####
>Get a parameter annotation of the specified class from the supplied
 method node
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the annotation, or null if not present
>
>### Parameters ###
>**parameterAnnotations**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Annotations for the parameter
>
>**annotationType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type of annotation to search for
>
>**paramIndex**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Index of the parameter to fetch annotation for
>
### public static AnnotationNode get (List, String) ###
>#### Method Overview ####
>Search for and return an annotation node matching the specified type
 within the supplied
 collection of annotation nodes
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;matching annotation node or null if the annotation doesn't exist
>
>### Parameters ###
>**annotations**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Haystack
>
>**annotationType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Needle
>
### public static Object getValue (AnnotationNode) ###
>#### Method Overview ####
>Duck type the "value" entry (if any) of the specified annotation node
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;duck-typed annotation value, null if missing, or inevitable
      {@link ClassCastException} if your duck is actually a rooster
>
>### Parameters ###
>**annotation**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Annotation node to query
>
### public static Object getValue (AnnotationNode, String, T) ###
>#### Method Overview ####
>Get the value of an annotation node and do pseudo-duck-typing via Java's
 crappy generics
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;duck-typed annotation value, null if missing, or inevitable
      {@link ClassCastException} if your duck is actually a rooster
>
>### Parameters ###
>**annotation**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Annotation node to query
>
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Key to search for
>
>**defaultValue**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Value to return if the specified key is not found or
      is null
>
### public static Object getValue (AnnotationNode, String, Class) ###
>#### Method Overview ####
>Gets an annotation value or returns the default value of the annotation
 if the annotation value is not present
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Value of the specified annotation node, default value if not
      specified, or null if no value or default
>
>### Parameters ###
>**annotation**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Annotation node to query
>
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Key to search for
>
>**annotationClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Annotation class to query reflectively for the
      default value
>
### public static Object getValue (AnnotationNode, String) ###
>#### Method Overview ####
>Get the value of an annotation node and do pseudo-duck-typing via Java's
 crappy generics
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;duck-typed annotation value, null if missing, or inevitable
      {@link ClassCastException} if your duck is actually a rooster
>
>### Parameters ###
>**annotation**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Annotation node to query
>
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Key to search for
>
### public static Enum getValue (AnnotationNode, String, Class, T) ###
>#### Method Overview ####
>Get the value of an annotation node as the specified enum, returns
 defaultValue if the annotation value is not set
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;duck-typed annotation value or defaultValue if missing
>
>### Parameters ###
>**annotation**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Annotation node to query
>
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Key to search for
>
>**enumClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Class of enum containing the enum constant to search for
>
>**defaultValue**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Value to return if the specified key isn't found
>
### public static List getValue (AnnotationNode, String, boolean) ###
>#### Method Overview ####
>Return the specified annotation node value as a list of nodes
>
>### Parameters ###
>**annotation**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Annotation node to query
>
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Key to search for
>
>**notNull**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;if true, return an empty list instead of null if the
      annotation value is absent
>
### public static List getValue (AnnotationNode, String, boolean, Class) ###
>#### Method Overview ####
>Return the specified annotation node value as a list of enums
>
>### Parameters ###
>**annotation**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Annotation node to query
>
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Key to search for
>
>**notNull**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;if true, return an empty list instead of null if the
      annotation value is absent
>
>**enumClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Class of enum containing the enum constant to use
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)