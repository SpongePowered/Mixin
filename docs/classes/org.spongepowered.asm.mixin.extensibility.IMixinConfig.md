[< Back](../README.md)
# public interface IMixinConfig IMixinConfig #
>#### Class Overview ####
>Interface for loaded mixin configurations
## Fields ##
### public static final int DEFAULT_PRIORITY ###
>#### Field Overview ####
>Default priority for mixin configs and mixins
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1000
>
## Methods ##
### public MixinEnvironment getEnvironment () ###
>#### Method Overview ####
>Get the parent environment of this config
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the parent environment
>
### public String getName () ###
>#### Method Overview ####
>Get the name of the file from which this configuration object was
 initialised
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the config filename (resource name)
>
### public String getMixinPackage () ###
>#### Method Overview ####
>Get the package containing all mixin classes
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the base package name for this config
>
### public int getPriority () ###
>#### Method Overview ####
>Get the priority
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the priority
>
### public IMixinConfigPlugin getPlugin () ###
>#### Method Overview ####
>Get the companion plugin, if available
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the companion plugin instance or null if no plugin
>
### public boolean isRequired () ###
>#### Method Overview ####
>True if this mixin is <em>required</em> (failure to apply a defined mixin
 is an <em>error</em> condition).
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if this config is marked as required
>
### public Set getTargets () ###
>#### Method Overview ####
>Get targets for this configuration
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;target classes of mixins in this config
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)