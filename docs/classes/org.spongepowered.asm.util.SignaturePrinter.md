[< Back](../README.md)
# SignaturePrinter #
>#### Class Overview ####
>Generates callback signature for callback pretty-print
## Constructors ##
### public SignaturePrinter (MethodNode) ###
>#### Constructor Overview ####
>No description provided
>
### public SignaturePrinter (MethodNode, String[]) ###
>#### Constructor Overview ####
>No description provided
>
### public SignaturePrinter (String, String) ###
>#### Constructor Overview ####
>No description provided
>
### public SignaturePrinter (String, Type, Type[]) ###
>#### Constructor Overview ####
>No description provided
>
### public SignaturePrinter (String, Type, LocalVariableNode[]) ###
>#### Constructor Overview ####
>No description provided
>
### public SignaturePrinter (String, Type, Type[], String[]) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public String getFormattedArgs () ###
>#### Method Overview ####
>Return only the arguments portion of this signature as a Java-style block
>
### public String getReturnType () ###
>#### Method Overview ####
>Get string representation of this signature's return type
>
### public void setModifiers (MethodNode) ###
>#### Method Overview ####
>Set modifiers on this signature using the supplied method node
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method node to read modifiers from
>
### public SignaturePrinter setFullyQualified (boolean) ###
>#### Method Overview ####
>Set whether this signature generates fully-qualified class output, mainly
 used when generating signatures for Mirror
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**fullyQualified**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new value for fully-qualified
>
### public boolean isFullyQualified () ###
>#### Method Overview ####
>Get whether this printer will fully-qualify class names in generated
 signatures
>
### public String toString () ###
>#### Method Overview ####
>No description provided
>
### public String toDescriptor () ###
>#### Method Overview ####
>Return this signature in descriptor format (return type after args)
>
### public static String getTypeName (Type, boolean) ###
>#### Method Overview ####
>Get the source code name for the specified type
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;String representation of the specified type, eg "int" for an
         integer primitive or "String" for java.lang.String
>
>### Parameters ###
>**type**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type to generate a friendly name for
>
>**box**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;True to return the equivalent boxing type for primitives
>
### public static String getTypeName (Type, boolean, boolean) ###
>#### Method Overview ####
>Get the source code name for the specified type
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;String representation of the specified type, eg "int" for an
         integer primitive or "String" for java.lang.String
>
>### Parameters ###
>**type**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type to generate a friendly name for
>
>**box**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;True to return the equivalent boxing type for primitives
>
>**fullyQualified**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fully-qualify class names
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)