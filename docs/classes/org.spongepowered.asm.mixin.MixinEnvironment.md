[< Back](../README.md)
# public final MixinEnvironment MixinEnvironment #
>#### Class Overview ####
>The mixin environment manages global state information for the mixin
 subsystem.
## Methods ##
### public Phase getPhase () ###
>#### Method Overview ####
>Get the phase for this environment
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the phase
>
### public List getMixinConfigs () ###
>#### Method Overview ####
>Get mixin configurations from the blackboard
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;list of registered mixin configs
>
### public MixinEnvironment addConfiguration (String) ###
>#### Method Overview ####
>Add a mixin configuration to the blackboard
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**config**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Name of configuration resource to add
>
### public MixinEnvironment registerErrorHandlerClass (String) ###
>#### Method Overview ####
>Add a new error handler class to this environment
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**handlerName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Handler class to add
>
### public MixinEnvironment registerTokenProviderClass (String) ###
>#### Method Overview ####
>Add a new token provider class to this environment
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**providerName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Class name of the token provider to add
>
### public MixinEnvironment registerTokenProvider (IEnvironmentTokenProvider) ###
>#### Method Overview ####
>Add a new token provider to this environment
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**provider**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Token provider to add
>
### public Integer getToken (String) ###
>#### Method Overview ####
>Get a token value from this environment
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;token value or null if the token is not present in the
      environment
>
>### Parameters ###
>**token**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Token to fetch
>
### public Set getErrorHandlerClasses () ###
>#### Method Overview ####
>Get all registered error handlers for this environment
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;set of error handler class names
>
### public Object getActiveTransformer () ###
>#### Method Overview ####
>Get the active mixin transformer instance (if any)
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;active mixin transformer instance
>
### public void setActiveTransformer (ITransformer) ###
>#### Method Overview ####
>Set the mixin transformer instance
>
>### Parameters ###
>**transformer**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Mixin Transformer
>
### public MixinEnvironment setSide (MixinEnvironment.Side) ###
>#### Method Overview ####
>Allows a third party to set the side if the side is currently UNKNOWN
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**side**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Side to set to
>
### public Side getSide () ###
>#### Method Overview ####
>Get (and detect if necessary) the current side
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;current side (or UNKNOWN if could not be determined)
>
### public String getVersion () ###
>#### Method Overview ####
>Get the current mixin subsystem version
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;current version
>
### public boolean getOption (MixinEnvironment.Option) ###
>#### Method Overview ####
>Get the specified option from the current environment
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Option value
>
>### Parameters ###
>**option**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Option to get
>
### public void setOption (MixinEnvironment.Option, boolean) ###
>#### Method Overview ####
>Set the specified option for this environment
>
>### Parameters ###
>**option**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Option to set
>
>**value**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;New option value
>
### public String getOptionValue (MixinEnvironment.Option) ###
>#### Method Overview ####
>Get the specified option from the current environment
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Option value
>
>### Parameters ###
>**option**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Option to get
>
### public Enum getOption (MixinEnvironment.Option, E) ###
>#### Method Overview ####
>Get the specified option from the current environment
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Option value
>
>### Parameters ###
>**option**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Option to get
>
>**defaultValue**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;value to use if the user-defined value is invalid
>
### public void setObfuscationContext (String) ###
>#### Method Overview ####
>Set the obfuscation context
>
>### Parameters ###
>**context**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new context
>
### public String getObfuscationContext () ###
>#### Method Overview ####
>Get the current obfuscation context
>
### public String getRefmapObfuscationContext () ###
>#### Method Overview ####
>Get the current obfuscation context
>
### public RemapperChain getRemappers () ###
>#### Method Overview ####
>Get the remapper chain for this environment
>
### public void audit () ###
>#### Method Overview ####
>Invoke a mixin environment audit process
>
### public List getTransformers () ###
>#### Method Overview ####
>Returns (and generates if necessary) the transformer delegation list for
 this environment.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;current transformer delegation list (read-only)
>
### public void addTransformerExclusion (String) ###
>#### Method Overview ####
>Adds a transformer to the transformer exclusions list
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Class transformer exclusion to add
>
### public String toString () ###
>#### Method Overview ####
>No description provided
>
### public static void init (MixinEnvironment.Phase) ###
>#### Method Overview ####
>Initialise the mixin environment in the specified phase
>
>### Parameters ###
>**phase**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;initial phase
>
### public static MixinEnvironment getEnvironment (MixinEnvironment.Phase) ###
>#### Method Overview ####
>Get the mixin environment for the specified phase
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the environment
>
>### Parameters ###
>**phase**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;phase to fetch environment for
>
### public static MixinEnvironment getDefaultEnvironment () ###
>#### Method Overview ####
>Gets the default environment
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the {@link Phase#DEFAULT DEFAULT} environment
>
### public static MixinEnvironment getCurrentEnvironment () ###
>#### Method Overview ####
>Gets the current environment
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the currently active environment
>
### public static CompatibilityLevel getCompatibilityLevel () ###
>#### Method Overview ####
>Get the current compatibility level
>
### public static void setCompatibilityLevel (MixinEnvironment.CompatibilityLevel) ###
>#### Method Overview ####
>Set desired compatibility level for the entire environment
>
>### Parameters ###
>**level**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Level to set, ignored if less than the current level
>
>### Throws ###
>**IllegalArgumentException**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;if the specified level is not supported
>
### public static Profiler getProfiler () ###
>#### Method Overview ####
>Get the performance profiler
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;profiler
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)