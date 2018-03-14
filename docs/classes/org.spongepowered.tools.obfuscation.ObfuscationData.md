[< Back](../README.md)
# ObfuscationData #
>#### Class Overview ####
>Return value struct for various obfuscation queries performed by the mixin
 annotation processor.
 
 <p>When obfuscation queries are performed by the AP, the returned data are
 encapsulated in an <tt>ObfuscationData</tt> object which contains a mapping
 of the different obfuscation types to the respective remapped value of the
 original entry for that environment.</p>
 
 <p>The returned data are iterable over the keys, consumers should call
 {@link #get} to retrieve the remapped entries for each key.</p>
## Constructors ##
### public ObfuscationData () ###
>#### Constructor Overview ####
>No description provided
>
### public ObfuscationData (T) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public void add (ObfuscationType, T) ###
>#### Method Overview ####
>Add an entry to the map, overwrites any previous entries. Since this
 method name poorly communicates its purpose, it is deprecated in favour
 of {@link #put}.
>
>### Parameters ###
>**type**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;obfuscation type
>
>**value**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new entry
>
### public void put (ObfuscationType, T) ###
>#### Method Overview ####
>Put an entry into this map, replaces any existing entries.
>
>### Parameters ###
>**type**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;obfuscation type
>
>**value**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;entry
>
### public boolean isEmpty () ###
>#### Method Overview ####
>Returns true if this store contains no entries
>
### public Object get (ObfuscationType) ###
>#### Method Overview ####
>Get the obfuscation entry for the specified obfuscation type, returns the
 default (if present) if no entry is found for the specified type
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;obfuscation entry or default value if absent
>
>### Parameters ###
>**type**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;obfuscation type
>
### public Iterator iterator () ###
>#### Method Overview ####
>No description provided
>
### public String toString () ###
>#### Method Overview ####
>No description provided
>
### public String values () ###
>#### Method Overview ####
>Get a string representation of the values in this data set
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;string
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)