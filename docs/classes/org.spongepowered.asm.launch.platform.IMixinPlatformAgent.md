[< Back](../README.md)
# IMixinPlatformAgent #
>#### Class Overview ####
>Base interface for platform agents. Platform agents are environment-specific
 handlers which are used by the Mixin subsystem to perform platform-specific
 tasks required by different environments without having to litter the Mixin
 codebase with a bunch of environment-specific cruft.
 
 <p>Platform Agents handle mixin environment tasks on a per-container basis,
 with each container in the environment being assigned one of each available
 type of agent to handle those tasks on behalf of the container.</p>
## Methods ##
### public String getPhaseProvider () ###
>#### Method Overview ####
>Get the phase provider for this agent
>
### public void prepare () ###
>#### Method Overview ####
>Called during pre-initialisation, after all tweakers and tweak containers
 have been added to the environment.
>
### public void initPrimaryContainer () ###
>#### Method Overview ####
>Called from <tt>inject</tt> in the parent tweaker but <b>only called on
 the primary tweak container</b>. This is useful if the agent needs to
 perform some environment-specific setup just once.
>
### public void inject () ###
>#### Method Overview ####
>Called from <tt>inject</tt> in the parent tweaker
>
### public String getLaunchTarget () ###
>#### Method Overview ####
>Get the launch target from this container, should return null if no
 custom target is available.
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)