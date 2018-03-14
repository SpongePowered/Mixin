[< Back](../README.md)
# MixinPlatformManager #
>#### Class Overview ####
>Handler for platform-specific behaviour required in different mixin
 environments.
## Constructors ##
### public MixinPlatformManager () ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public void init () ###
>#### Method Overview ####
>Initialise this platform manager by scanning the classpath
>
### public Collection getPhaseProviderClasses () ###
>#### Method Overview ####
>Get the phase provider classes from the primary container
>
### public final MixinContainer addContainer (URI) ###
>#### Method Overview ####
>Add a new URI to this platform and return the new container (or an
 existing container if the URI was previously registered)
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;container for specified URI
>
>### Parameters ###
>**uri**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;URI to add
>
### public final void prepare (List) ###
>#### Method Overview ####
>Prepare all containers in this platform
>
>### Parameters ###
>**args**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;command-line arguments from tweaker
>
### public final void inject () ###
>#### Method Overview ####
>Initialise the primary container and dispatch inject to all containers
>
### public String getLaunchTarget () ###
>#### Method Overview ####
>Queries all containers for launch target, returns null if no containers
 specify a launch target
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)