[< Back](../README.md)
# InjectorRemap #
>#### Class Overview ####
>Remap tracking object for injectors. When remapping an injector we will
 generally want to raise an error if <tt>remap=true</tt> but we don't find
 a mapping for the injector. However it may be the case that remap is true
 because some of the &#064;At's need remapping. This state struct is used to
 log the original error, but supress it if any &#064;At annotations are
 remapped in the process. When {@link #dispatchPendingMessages} is called at
 the end, if no &#064;At's have been remapped then we dispatch the error as
 planned.
## Constructors ##
### public InjectorRemap (boolean) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public boolean shouldRemap () ###
>#### Method Overview ####
>Get whether <tt>remap=true</tt> on the injector
>
### public void notifyRemapped () ###
>#### Method Overview ####
>Callback from the parser to notify this injector that it has been
 remapped
>
### public void addMessage (Diagnostic.Kind, CharSequence, Element, AnnotationHandle) ###
>#### Method Overview ####
>Add an error message on ths injector, the message will be suppressed if
 a child {@link At} is remapped
>
>### Parameters ###
>**kind**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;message kind
>
>**msg**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;message
>
>**element**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;annotated element
>
>**annotation**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;annotation
>
### public void clearMessage () ###
>#### Method Overview ####
>Clear the current message (if any)
>
### public void dispatchPendingMessages (Messager) ###
>#### Method Overview ####
>Called after processing completes. Dispatches the queued message (if any)
 if no child At annotations were remapped.
>
>### Parameters ###
>**messager**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;messager to push message into
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)