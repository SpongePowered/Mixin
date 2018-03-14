[< Back](../README.md)
# public final ObfuscationServices ObfuscationServices #
>#### Class Overview ####
>Obfuscation service manager
## Methods ##
### public static ObfuscationServices getInstance () ###
>#### Method Overview ####
>Singleton pattern, get or create the instance
>
### public void initProviders (IMixinAnnotationProcessor) ###
>#### Method Overview ####
>Initialise services
>
>### Parameters ###
>**ap**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;annotation processor
>
### public Set getSupportedOptions () ###
>#### Method Overview ####
>Get the options supported by all available providers
>
### public IObfuscationService getService (Class) ###
>#### Method Overview ####
>Get the service instance for the specified class from the service loader
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;service instance or null if no matching services were loaded
>
>### Parameters ###
>**serviceClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;service class
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)