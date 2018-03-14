[< Back](../README.md)
# ArgsClassGenerator #
>#### Class Overview ####
>Class generator which creates subclasses of {@link Args} to be used by the
 {@link ModifyArgs} injector. The subclasses contain getter and setter logic
 to provide access to a particular configuration of arguments and classes are
 only generated for each unique argument combination.
## Fields ##
### public static final String ARGS_NAME ###
>#### Field Overview ####
>No description provided
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final String ARGS_REF ###
>#### Field Overview ####
>No description provided
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final String GETTER_PREFIX ###
>#### Field Overview ####
>No description provided
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;$
>
## Constructors ##
### public ArgsClassGenerator () ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public String getClassName (String) ###
>#### Method Overview ####
>Get (or generate) the class name for the specified descriptor. The class
 will not be generated until it is used. Calling this method simply
 allocates a name for the specified descriptor.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;name of the Args subclass to use
>
>### Parameters ###
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Descriptor of the <em>target</em> method, with the return
      type changed to void (V)
>
### public String getClassRef (String) ###
>#### Method Overview ####
>Get (or generate) the class name for the specified descriptor in internal
 format (reference). The class will not be generated until it is used.
 Calling this method simply allocates a name for the specified descriptor.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;reference of the Args subclass to use
>
>### Parameters ###
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Descriptor of the <em>target</em> method, with the return
      type changed to void (V)
>
### public byte generate (String) ###
>#### Method Overview ####
>No description provided
>
### public byte getBytes (String) ###
>#### Method Overview ####
>Fetch or generate class bytes for the specified class name. Returns null
 if this generator does not have bytes for the specified class.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;class bytes or null
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Class name
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)