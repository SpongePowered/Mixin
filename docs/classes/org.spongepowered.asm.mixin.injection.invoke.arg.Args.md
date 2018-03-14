[< Back](../README.md)
# Args #
>#### Class Overview ####
>Argument bundle class used in {@link ModifyArgs} callbacks. See the
 documentation for {@link ModifyArgs} for details. Synthetic subclasses are
 generated at runtime for specific injectors.
## Fields ##
### protected final Object values ###
>#### Field Overview ####
>Argument values
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Constructors ##
### protected Args (Object[]) ###
>#### Constructor Overview ####
>Ctor.
>
>### Parameters ###
>**values**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;argument values
>
## Methods ##
### public int size () ###
>#### Method Overview ####
>Return the argument list size.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;number of arguments available
>
### public Object get (int) ###
>#### Method Overview ####
>Retrieve the argument value at the specified index
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the argument value
>
>### Parameters ###
>**index**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;argument index to retrieve
>
>### Throws ###
>**ArrayIndexOutOfBoundsException**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;if a value outside the range of
      available arguments is accessed
>
### public abstract void set (int, T) ###
>#### Method Overview ####
>Set (modify) the specified argument value. Internal verification is
 performed upon supplied values and the following requirements are
 enforced:
 
 <ul>
   <li>Reference types must be assignable to the object type, or can be
      <tt>null</tt>.
   <li>Primitive types must match the target types exactly and <b>cannot
      </b> be <tt>null</tt>.
 </ul>
>
>### Parameters ###
>**index**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Argument index to set
>
>**value**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Argument value
>
>### Throws ###
>**ArgumentIndexOutOfBoundsException**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;if the specified argument index
      is outside the range of available arguments
>
### public abstract void setAll (Object[]) ###
>#### Method Overview ####
>Set (modify) all argument values. The number and type of arguments
 supplied to this method must precisely match the argument types in the
 bundle. See {@link #set(int, Object)} for details.
>
>### Parameters ###
>**values**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Argument values to set
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)