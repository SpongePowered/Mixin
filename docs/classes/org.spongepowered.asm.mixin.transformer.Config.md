[< Back](../README.md)
# public Config Config #
>#### Class Overview ####
>Handle for marshalling mixin configs outside of the transformer package
## Constructors ##
### public Config (MixinConfig) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public String getName () ###
>#### Method Overview ####
>No description provided
>
### public boolean isVisited () ###
>#### Method Overview ####
>Get whether config has been visited
>
### public IMixinConfig getConfig () ###
>#### Method Overview ####
>Get API-level config view
>
### public MixinEnvironment getEnvironment () ###
>#### Method Overview ####
>Get environment for the config
>
### public String toString () ###
>#### Method Overview ####
>No description provided
>
### public boolean equals (Object) ###
>#### Method Overview ####
>No description provided
>
### public int hashCode () ###
>#### Method Overview ####
>No description provided
>
### public static Config create (String, MixinEnvironment) ###
>#### Method Overview ####
>Factory method, create a config from the specified config file and fail
 over to the specified environment if no selector is present in the config
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new config or null if invalid config version
>
>### Parameters ###
>**configFile**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;config resource
>
>**outer**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;failover environment
>
### public static Config create (String) ###
>#### Method Overview ####
>Factory method, create a config from the specified config resource
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new config or null if invalid config version
>
>### Parameters ###
>**configFile**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;config resource
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)