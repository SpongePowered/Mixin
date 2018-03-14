[< Back](../README.md)
# public ReEntranceLock ReEntranceLock #
>#### Class Overview ####
>Re-entrance semaphore used to share re-entrance data with the TreeInfo
## Constructors ##
### public ReEntranceLock (int) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public int getMaxDepth () ###
>#### Method Overview ####
>Get max depth
>
### public int getDepth () ###
>#### Method Overview ####
>Get current depth
>
### public ReEntranceLock push () ###
>#### Method Overview ####
>Increase the re-entrance depth counter and set the semaphore if depth
 exceeds max depth
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
### public ReEntranceLock pop () ###
>#### Method Overview ####
>Decrease the re-entrance depth
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
### public boolean check () ###
>#### Method Overview ####
>Run the depth check but do not set the semaphore
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if depth has exceeded max
>
### public boolean checkAndSet () ###
>#### Method Overview ####
>Run the depth check and set the semaphore if depth is exceeded
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if semaphore is set
>
### public ReEntranceLock set () ###
>#### Method Overview ####
>Set the semaphore
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
### public boolean isSet () ###
>#### Method Overview ####
>Get whether the semaphore is set
>
### public ReEntranceLock clear () ###
>#### Method Overview ####
>Clear the semaphore
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)