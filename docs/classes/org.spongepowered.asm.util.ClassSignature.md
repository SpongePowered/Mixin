[< Back](../README.md)
# ClassSignature #
>#### Class Overview ####
>Represents an object-oriented view of a generic class signature. We use ASM's
 {@link SignatureVisitor} to walk over an incoming signature in order to parse
 out our internal tree. This is done so that incoming signatures from mixins
 can be merged into the target class.
## Fields ##
### protected static final String OBJECT ###
>#### Field Overview ####
>No description provided
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;java/lang/Object
>
## Methods ##
### protected TypeVar getTypeVar (String) ###
>#### Method Overview ####
>Get the type var for the specified var name
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type var for the supplied type var name
>
>### Parameters ###
>**varName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type var to lookup
>
### protected TokenHandle getType (String) ###
>#### Method Overview ####
>Get the token for the specified type var name, creating it if necessary
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type token for the supplied type var name
>
>### Parameters ###
>**varName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type var to lookup
>
### protected String getTypeVar (ClassSignature.TokenHandle) ###
>#### Method Overview ####
>Get the type var matching the supplied type token, or the raw token type
 if no mapping exists for the supplied token handle
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type var (with prefix and suffix) or raw type name
>
>### Parameters ###
>**handle**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type token handle to lookup
>
### protected void addTypeVar (ClassSignature.TypeVar, ClassSignature.TokenHandle) ###
>#### Method Overview ####
>Add a type var to this signature, the type var must not exist
>
>### Parameters ###
>**typeVar**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type var to add
>
>**handle**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type var's type token
>
>### Throws ###
>**IllegalArgumentException**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;if the specified type var already exists
>
### protected void setSuperClass (ClassSignature.Token) ###
>#### Method Overview ####
>Set the superclass for this signature
>
>### Parameters ###
>**superClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;super class to set
>
### public String getSuperClass () ###
>#### Method Overview ####
>Get the raw superclass type of this signature as a string
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;superclass type as a string
>
### protected void addInterface (ClassSignature.Token) ###
>#### Method Overview ####
>Add an interface to this signature
>
>### Parameters ###
>**iface**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;interface to add
>
### public void addInterface (String) ###
>#### Method Overview ####
>Add a raw interface declaration to this signature
>
>### Parameters ###
>**iface**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;interface name to add (bin format)
>
### protected void addRawInterface (String) ###
>#### Method Overview ####
>Add a raw interface which was previously enqueued
>
>### Parameters ###
>**iface**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;interface to add
>
### public void merge (ClassSignature) ###
>#### Method Overview ####
>Merges another class signature into this one. The other signature is
 first conformed so that no formal type parameters overlap with formal
 type parameters defined on this signature. No attempt is made to combine
 formal type parameters, this method merely ensures that parameters do
 not overlap.
>
>### Parameters ###
>**other**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Class signature to merge into this one
>
### public SignatureVisitor getRemapper () ###
>#### Method Overview ####
>Get a remapper for type vars in this signature
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;signature visitor
>
### public String toString () ###
>#### Method Overview ####
>Converts this signature into a string representation compatible with the
 signature attribute of a Java class
>
>### See Also ###
>[Object](java.lang.Object.md) 
>
### public ClassSignature wake () ###
>#### Method Overview ####
>Wake up this signature if it is lazy-loaded
>
### public static ClassSignature of (String) ###
>#### Method Overview ####
>Parse a generic class signature from the supplied string
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;parsed signature object
>
>### Parameters ###
>**signature**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;signature string to parse
>
### public static ClassSignature of (ClassNode) ###
>#### Method Overview ####
>Parse a generic class signature from the supplied class node, uses the
 declared signature if present, else falls back to generating a raw
 signature from the class itself
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;parsed signature
>
>### Parameters ###
>**classNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;class node to parse
>
### public static ClassSignature ofLazy (ClassNode) ###
>#### Method Overview ####
>Returns a lazy-evaluated signature object. For classes with a signature
 present this saves having to do the parse until we actually need it. For
 classes with no signature we just go ahead and generate it from the
 supplied ClassNode while we have it
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;parsed signature or lazy-load handle
>
>### Parameters ###
>**classNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;class node to parse
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)