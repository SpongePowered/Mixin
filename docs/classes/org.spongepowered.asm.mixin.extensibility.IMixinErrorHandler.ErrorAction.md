[< Back](../README.md)
# public static final ErrorAction IMixinErrorHandler.ErrorAction #
>#### Class Overview ####
>Action to take when handling an error. By default, if a config is marked
 as "required" then the default action will be {@link #ERROR}, and will be
 {@link #WARN} otherwise.
## Fields ##
### public static final ErrorAction NONE ###
>#### Field Overview ####
>Take no action, this should be treated as a non-critical error and
 processing should continue
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final ErrorAction WARN ###
>#### Field Overview ####
>Generate a warning but continue processing
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final ErrorAction ERROR ###
>#### Field Overview ####
>Throw a
 {@link org.spongepowered.asm.mixin.throwables.MixinApplyError} to
 halt further processing if possible
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public final Level logLevel ###
>#### Field Overview ####
>Logging level for the specified error action
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Methods ##
### public static ErrorAction values () ###
>#### Method Overview ####
>No description provided
>
### public static ErrorAction valueOf (String) ###
>#### Method Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)