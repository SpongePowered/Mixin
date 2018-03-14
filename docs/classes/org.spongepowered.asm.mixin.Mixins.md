[< Back](../README.md)
# public final Mixins Mixins #
>#### Class Overview ####
>Entry point for registering global mixin resources. Compatibility with
 pre-0.6 versions is maintained via the methods on {@link MixinEnvironment}
 delegating to the methods here.
## Methods ##
### public static void addConfigurations (String[]) ###
>#### Method Overview ####
>Add multiple configurations
>
>### Parameters ###
>**configFiles**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;config resources to add
>
### public static void addConfiguration (String) ###
>#### Method Overview ####
>Add a mixin configuration resource
>
>### Parameters ###
>**configFile**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;path to configuration resource
>
### public static int getUnvisitedCount () ###
>#### Method Overview ####
>Get the number of "unvisited" configurations available. This is the
 number of configurations which have been added since the last selection
 attempt.
 
 <p>If the transformer has already entered a phase but no mixins have yet
 been applied, it is safe to visit any additional configs which were
 registered in the mean time and may wish to apply to the current phase.
 This is particularly true during the PREINIT phase, which by necessity
 must start as soon as the first class is transformed after bootstrapping,
 but may not have any valid mixins until later in the actual preinit
 process due to the order in which things are discovered.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;unvisited config count
>
### public static Set getConfigs () ###
>#### Method Overview ####
>Get current pending configs set, only configs which have yet to be
 consumed are present in this set
>
### public static void registerErrorHandlerClass (String) ###
>#### Method Overview ####
>Register a gloabl error handler class
>
>### Parameters ###
>**handlerName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Fully qualified class name
>
### public static Set getErrorHandlerClasses () ###
>#### Method Overview ####
>Get current error handlers
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)