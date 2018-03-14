[< Back](../README.md)
# Extensions #
>#### Class Overview ####
>Mixin transformer extensions and common modules such as class generators
## Constructors ##
### public Extensions (MixinTransformer) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public MixinTransformer getTransformer () ###
>#### Method Overview ####
>No description provided
>
### public void add (IExtension) ###
>#### Method Overview ####
>Add a new transformer extension
>
>### Parameters ###
>**extension**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;extension to add
>
### public List getExtensions () ###
>#### Method Overview ####
>Get all extensions
>
### public List getActiveExtensions () ###
>#### Method Overview ####
>Get all active extensions
>
### public IExtension getExtension (Class) ###
>#### Method Overview ####
>Get a specific extension
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;extension instance or null
>
>### Parameters ###
>**extensionClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;extension class to look up
>
### public void select (MixinEnvironment) ###
>#### Method Overview ####
>Selectively activate extensions based on the current environment
>
>### Parameters ###
>**environment**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;current environment
>
### public void preApply (ITargetClassContext) ###
>#### Method Overview ####
>Process tasks before mixin application
>
>### Parameters ###
>**context**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target class context
>
### public void postApply (ITargetClassContext) ###
>#### Method Overview ####
>Process tasks after mixin application
>
>### Parameters ###
>**context**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target class context
>
### public void export (MixinEnvironment, String, boolean, byte[]) ###
>#### Method Overview ####
>Export class bytecode to disk
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
### public void add (IClassGenerator) ###
>#### Method Overview ####
>Add a new generator to the mixin extensions
>
>### Parameters ###
>**generator**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;generator to add
>
### public List getGenerators () ###
>#### Method Overview ####
>Get all active generators
>
### public IClassGenerator getGenerator (Class) ###
>#### Method Overview ####
>No description provided
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;generator
>
>### Parameters ###
>**generatorClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;generator class or interface to look up
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)