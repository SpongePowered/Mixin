[< Back](../README.md)
# ModifyVariableInjector #
>#### Class Overview ####
>A bytecode injector which allows a single local variable in the target method
 to be captured and altered. See also {@link LocalVariableDiscriminator} and
 {@link ModifyVariable}.
## Constructors ##
### public ModifyVariableInjector (InjectionInfo, LocalVariableDiscriminator) ###
>#### Constructor Overview ####
>No description provided
>
>### Parameters ###
>**info**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Injection info
>
>**discriminator**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;discriminator
>
## Methods ##
### protected boolean findTargetNodes (MethodNode, InjectionPoint, InsnList, Collection) ###
>#### Method Overview ####
>No description provided
>
### protected void sanityCheck (Target, List) ###
>#### Method Overview ####
>No description provided
>
### protected void inject (Target, InjectionNodes.InjectionNode) ###
>#### Method Overview ####
>Do the injection
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)