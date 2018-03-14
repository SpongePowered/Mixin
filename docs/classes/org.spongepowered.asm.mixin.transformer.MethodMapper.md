[< Back](../README.md)
# public MethodMapper MethodMapper #
>#### Class Overview ####
>Maintains method remaps for a target class
## Constructors ##
### public MethodMapper (MixinEnvironment, ClassInfo) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public ClassInfo getClassInfo () ###
>#### Method Overview ####
>No description provided
>
### public void remapHandlerMethod (MixinInfo, MethodNode, ClassInfo.Method) ###
>#### Method Overview ####
>Conforms an injector handler method
>
>### Parameters ###
>**mixin**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;owner mixin
>
>**handler**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;annotated injector handler method
>
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method in target
>
### public String getHandlerName (MixinInfo.MixinMethodNode) ###
>#### Method Overview ####
>Get the name for a handler method provided a source mixin method
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;conformed handler name
>
>### Parameters ###
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mixin method
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)