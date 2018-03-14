[< Back](../README.md)
# public interface Cancellable Cancellable #
>#### Class Overview ####
>Interface for things which can be cancelled
## Methods ##
### public boolean isCancellable () ###
>#### Method Overview ####
>Get whether this is actually cancellable
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;whether this is actually cancellable
>
### public boolean isCancelled () ###
>#### Method Overview ####
>Get whether this is cancelled
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;whether this is cancelled
>
### public void cancel () ###
>#### Method Overview ####
>If the object is cancellable, cancels the object, implementors may throw
 an EventCancellationException if the object is not actually cancellable.
>
>### Throws ###
>**CancellationException**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(optional) may be thrown if the object is
      not actually cancellable. Contractually, this object may not throw
      the exception if isCancellable() returns true.
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)