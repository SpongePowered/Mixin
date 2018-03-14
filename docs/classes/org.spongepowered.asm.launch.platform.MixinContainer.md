[< Back](../README.md)
# MixinContainer #
>#### Class Overview ####
>A collection of {@link IMixinPlatformAgent} platform agents)
## Constructors ##
### public MixinContainer (MixinPlatformManager, URI) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public URI getURI () ###
>#### Method Overview ####
>No description provided
>
### public Collection getPhaseProviders () ###
>#### Method Overview ####
>Get phase provider names from all agents in this container
>
### public void prepare () ###
>#### Method Overview ####
>Prepare agents in this container
>
### public void initPrimaryContainer () ###
>#### Method Overview ####
>If this container is the primary container, initialise agents in this
 container as primary
>
### public void inject () ###
>#### Method Overview ####
>Notify all agents to inject into classLoader
>
### public String getLaunchTarget () ###
>#### Method Overview ####
>Analogue of <tt>ITweaker::getLaunchTarget</tt>, queries all agents and
 returns first valid launch target. Returns null if no agents have launch
 target.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;launch target from agent or null
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)