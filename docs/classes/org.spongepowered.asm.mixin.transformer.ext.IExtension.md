[< Back](../README.md)
# IExtension #
>#### Class Overview ####
>Mixin Transformer extension interface for pre- and post-processors
## Methods ##
### public boolean checkActive (MixinEnvironment) ###
>#### Method Overview ####
>Check whether this extension is active for the specified environment
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if the module should be active in the specified environment
>
>### Parameters ###
>**environment**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;current environment
>
### public void preApply (ITargetClassContext) ###
>#### Method Overview ####
>Called before the mixins are applied
>
>### Parameters ###
>**context**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target class context
>
### public void postApply (ITargetClassContext) ###
>#### Method Overview ####
>Called after the mixins are applied
>
>### Parameters ###
>**context**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target class context
>
### public void export (MixinEnvironment, String, boolean, byte[]) ###
>#### Method Overview ####
>Called when a class needs to be exported
>
>### Parameters ###
>**env**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Environment
>
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Class name
>
>**force**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;True to export even if the current environment settings
      would normally disable it
>
>**bytes**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Bytes to export
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)