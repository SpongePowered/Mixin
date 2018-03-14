[< Back](../README.md)
# IMixinErrorHandler #
>#### Class Overview ####
>Interface for objects which want to perform custom behaviour when fatal mixin
 errors occur. For example displaying a user-friendly error message
## Methods ##
### public ErrorAction onPrepareError (IMixinConfig, Throwable, IMixinInfo, IMixinErrorHandler.ErrorAction) ###
>#### Method Overview ####
>Called when an error occurs whilst initialising a mixin config. This
 allows the plugin to display more user-friendly error messages if
 required.
 
 <p>By default, when a critical error occurs the mixin processor will
 raise a warning if the config is not marked as "required" and will throw
 an {@link Error} if it is. This behaviour can be altered by returning
 different values from this method.</p>
 
 <p>The original throwable which was caught is passed in via the <code>
 th</code> parameter and the default action is passed in to the <code>
 action</code> parameter. A plugin can choose to output a friendly message
 but leave the original behaviour intact (by returning <code>null</code>
 or returning <code>action</code> directly. Alternatively it may throw a
 different exception or error, or can reduce the severity of the error by
 returning a different {@link ErrorAction}.</p>
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null to perform the default action (or return action) or new
      action to take
>
>### Parameters ###
>**config**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Config being prepared when the error occurred
>
>**th**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Throwable which was caught
>
>**mixin**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Mixin which was being applied at the time of the error
>
>**action**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Default action
>
### public ErrorAction onApplyError (String, Throwable, IMixinInfo, IMixinErrorHandler.ErrorAction) ###
>#### Method Overview ####
>Called when an error occurs applying a mixin. This allows
 the plugin to display more user-friendly error messages if required.
 
 <p>By default, when a critical error occurs the mixin processor will
 raise a warning if the config is not marked as "required" and will throw
 an {@link Error} if it is. This behaviour can be altered by returning
 different values from this method.</p>
 
 <p>The original throwable which was caught is passed in via the <code>
 th</code> parameter and the default action is passed in to the <code>
 action</code> parameter. A plugin can choose to output a friendly message
 but leave the original behaviour intact (by returning <code>null</code>
 or returning <code>action</code> directly. Alternatively it may throw a
 different exception or error, or can reduce the severity of the error by
 returning a different {@link ErrorAction}.</p>
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null to perform the default action (or return action) or new
      action to take
>
>### Parameters ###
>**targetClassName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Class being transformed when the error occurred
>
>**th**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Throwable which was caught
>
>**mixin**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Mixin which was being applied at the time of the error
>
>**action**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Default action
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)