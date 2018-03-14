[< Back](../README.md)
# public CallbackInjector CallbackInjector #
>#### Class Overview ####
>This class is responsible for generating the bytecode for injected callbacks
## Constructors ##
### public CallbackInjector (InjectionInfo, boolean, LocalCapture, String) ###
>#### Constructor Overview ####
>Make a new CallbackInjector with the supplied args
>
>### Parameters ###
>**info**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;information about this injector
>
>**cancellable**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;True if injections performed by this injector should
      be cancellable
>
>**localCapture**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Local variable capture behaviour
>
## Methods ##
### protected void sanityCheck (Target, List) ###
>#### Method Overview ####
>No description provided
>
### protected void addTargetNode (Target, List, AbstractInsnNode, Set) ###
>#### Method Overview ####
>No description provided
>
### protected void inject (Target, InjectionNodes.InjectionNode) ###
>#### Method Overview ####
>No description provided
>
### protected void instanceCallbackInfo (CallbackInjector.Callback, String, String, boolean) ###
>#### Method Overview ####
>No description provided
>
>### Parameters ###
>**callback**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;callback handle
>
>**id**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;callback id
>
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;constructor descriptor
>
>**store**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if storing in a local, false if this is happening at an
      invoke
>
### protected void injectCancellationCode (CallbackInjector.Callback) ###
>#### Method Overview ####
>if (e.isCancelled()) return e.getReturnValue();
>
>### Parameters ###
>**callback**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;callback handle
>
### protected void injectReturnCode (CallbackInjector.Callback) ###
>#### Method Overview ####
>Inject the appropriate return code for the method type
>
>### Parameters ###
>**callback**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;callback handle
>
### protected boolean isStatic () ###
>#### Method Overview ####
>Explicit to avoid creation of synthetic accessor
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if the target method is static
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)