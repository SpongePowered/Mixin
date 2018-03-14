[< Back](../README.md)
# LocalCapture #
>#### Class Overview ####
>Specifies the behaviour for capturing local variables at an injection point.
 
 <p>Since local capture relies on calculating the local variable table for the
 target method it is disabled by default for performance reasons. When
 capturing is enabled, local variables are passed to the handler method after
 the {@link CallbackInfo} argument. Since it is entirely possible for another
 transformer to make an incompatible change to the the local variable table at
 run time, the purpose of this enum is to specify the behaviour for local
 capture and the type of recovery to be performed when an incompatible change
 is detected.</p>
## Fields ##
### public static final LocalCapture NO_CAPTURE ###
>#### Field Overview ####
>Do not capture locals, this is the default behaviour
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final LocalCapture PRINT ###
>#### Field Overview ####
>Do not capture locals. Print the expected method signature to stderr
 instead.
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final LocalCapture CAPTURE_FAILSOFT ###
>#### Field Overview ####
>Capture locals. If the calculated locals are different from the expected
 values, log a warning and skip this injection.
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final LocalCapture CAPTURE_FAILHARD ###
>#### Field Overview ####
>Capture locals. If the calculated locals are different from the expected
 values, throw an {@link Error}.
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final LocalCapture CAPTURE_FAILEXCEPTION ###
>#### Field Overview ####
>Capture locals. If the calculated locals are different from the expected
 values, generate a method stub containing an exception. This will allow
 normal execution to continue unless the callback is encountered.
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Methods ##
### public static LocalCapture values () ###
>#### Method Overview ####
>No description provided
>
### public static LocalCapture valueOf (String) ###
>#### Method Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)