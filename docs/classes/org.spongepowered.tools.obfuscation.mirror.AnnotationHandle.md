[< Back](../README.md)
# AnnotationHandle #
>#### Class Overview ####
>A wrapper for {@link AnnotationMirror} which provides a more convenient way
 to access annotation values.
## Fields ##
### public static final AnnotationHandle MISSING ###
>#### Field Overview ####
>No description provided
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Methods ##
### public AnnotationMirror asMirror () ###
>#### Method Overview ####
>Return the wrapped mirror, only used to pass to Messager methods
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;annotation mirror (can be null)
>
### public boolean exists () ###
>#### Method Overview ####
>Get whether the annotation mirror actually exists, if the mirror is null
 returns false
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if the annotation exists
>
### public String toString () ###
>#### Method Overview ####
>No description provided
>
### public Object getValue (String, T) ###
>#### Method Overview ####
>Get a value with the specified key from this annotation, return the
 specified default value if the key is not set or is not present
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;value or default if not set
>
>### Parameters ###
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;key
>
>**defaultValue**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;value to return if the key is not set or not present
>
### public Object getValue () ###
>#### Method Overview ####
>Get the annotation value or return null if not present or not set
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;value or null if not present or not set
>
### public Object getValue (String) ###
>#### Method Overview ####
>Get the annotation value with the specified key or return null if not
 present or not set
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;value or null if not present or not set
>
>### Parameters ###
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;key to fetch
>
### public boolean getBoolean (String, boolean) ###
>#### Method Overview ####
>Get the primitive boolean value with the specified key or return null if
 not present or not set
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;value or default if not present or not set
>
>### Parameters ###
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;key to fetch
>
>**defaultValue**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;default value to return if value is not present
>
### public AnnotationHandle getAnnotation (String) ###
>#### Method Overview ####
>Get an annotation value as an annotation handle
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;value or <tt>null</tt> if not set
>
>### Parameters ###
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;key to search for in the value map
>
### public List getList () ###
>#### Method Overview ####
>Retrieve the annotation value as a list with values of the specified
 type. Returns an empty list if the value is not present or not set.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;list of values
>
### public List getList (String) ###
>#### Method Overview ####
>Retrieve the annotation value with the specified key as a list with
 values of the specified type. Returns an empty list if the value is not
 present or not set.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;list of values
>
>### Parameters ###
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;key to fetch
>
### public List getAnnotationList (String) ###
>#### Method Overview ####
>Retrieve an annotation key as a list of annotation handles
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;list of annotations
>
>### Parameters ###
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;key to fetch
>
### protected AnnotationValue getAnnotationValue (String) ###
>#### Method Overview ####
>No description provided
>
### protected static List unwrapAnnotationValueList (List) ###
>#### Method Overview ####
>No description provided
>
### protected static AnnotationMirror getAnnotation (Element, Class) ###
>#### Method Overview ####
>No description provided
>
### public static AnnotationHandle of (AnnotationMirror) ###
>#### Method Overview ####
>Returns a new annotation handle for the supplied annotation mirror
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new annotation handle
>
>### Parameters ###
>**annotation**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;annotation mirror to wrap (can be null)
>
### public static AnnotationHandle of (Element, Class) ###
>#### Method Overview ####
>Returns a new annotation handle for the specified annotation on the
 supplied element, consumers should call {@link #exists} in order to check
 whether the requested annotation is present.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new annotation handle
>
>### Parameters ###
>**elem**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;element
>
>**annotationClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;annotation class to search for
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)