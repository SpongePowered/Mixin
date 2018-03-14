[< Back](../README.md)
# public MixinPlatformAgentFML MixinPlatformAgentFML #
>#### Class Overview ####
>Platform agent for use under FML.
 
 <p>When FML is present we scan containers for the manifest entries which are
 inhibited by the tweaker, in particular the <tt>FMLCorePlugin</tt> and
 <tt>FMLCorePluginContainsFMLMod</tt> entries. This is required because FML
 performs no further processing of containers if they contain a tweaker!</p>
## Constructors ##
### public MixinPlatformAgentFML (MixinPlatformManager, URI) ###
>#### Constructor Overview ####
>No description provided
>
>### Parameters ###
>**manager**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;platform manager
>
>**uri**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;URI of the resource for this agent
>
## Methods ##
### public String getPhaseProvider () ###
>#### Method Overview ####
>No description provided
>
### public void prepare () ###
>#### Method Overview ####
>No description provided
>
### public void initPrimaryContainer () ###
>#### Method Overview ####
>No description provided
>
### public void inject () ###
>#### Method Overview ####
>No description provided
>
### public String getLaunchTarget () ###
>#### Method Overview ####
>No description provided
>
### protected final boolean checkForCoInitialisation () ###
>#### Method Overview ####
>Performs a naive check which attempts to discover whether we are pre or
 post FML's main injection. If we are <i>pre</i>, then we must <b>not</b>
 manually call <tt>injectIntoClassLoader</tt> on the wrapper because FML
 will add the wrapper to the tweaker list itself. This occurs when mixin
 tweaker is loaded explicitly.
 
 <p>In the event that we are <i>post</i> FML's injection, then we must
 instead call <tt>injectIntoClassLoader</tt> on the wrapper manually.</p>
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if FML was already injected
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)