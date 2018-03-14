[< Back](../README.md)
# public CallbackInfo CallbackInfo #
>#### Class Overview ####
>CallbackInfo instances are passed to callbacks in order to provide
 information and handling opportunities to the callback to interact with the
 callback itself. For example by allowing the callback to be "cancelled" and
 return from a method prematurely.
## Constructors ##
### public CallbackInfo (String, boolean) ###
>#### Constructor Overview ####
>This ctor is always called by injected code
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;calling method name
>
>**cancellable**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if the callback can be cancelled
>
## Methods ##
### public String getId () ###
>#### Method Overview ####
>Get the ID of the injector which defined this callback. This defaults to
 the method name but can be overridden by specifying the {@link Inject#id}
 parameter on the injector
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the injector ID
>
### public String toString () ###
>#### Method Overview ####
>No description provided
>
### public final boolean isCancellable () ###
>#### Method Overview ####
>No description provided
>
### public final boolean isCancelled () ###
>#### Method Overview ####
>No description provided
>
### public void cancel () ###
>#### Method Overview ####
>No description provided
>
### public static String getCallInfoClassName (Type) ###
>#### Method Overview ####
>Gets the {@link CallbackInfo} class name to use for the specified return
 type. Currently returns {@link CallbackInfo} for void types and
 {@link CallbackInfoReturnable} for non-void types.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;CallbackInfo class name to use
>
>### Parameters ###
>**returnType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;return type of the target method
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)