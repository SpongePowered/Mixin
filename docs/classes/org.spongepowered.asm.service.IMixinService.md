[< Back](../README.md)
# public interface IMixinService IMixinService #
>#### Class Overview ####
>Mixin Service interface. Mixin services connect the mixin subsytem to the
 underlying environment. It is something of a god interface at present because
 it contains all of the current functionality accessors for calling into
 launchwrapper. In the future once support for modlauncher is added, it is
 anticipated that the interface can be split down into sub-services which
 handle different aspects of interacting with the environment.
## Methods ##
### public String getName () ###
>#### Method Overview ####
>Get the friendly name for this service
>
### public boolean isValid () ###
>#### Method Overview ####
>True if this service type is valid in the current environment
>
### public void prepare () ###
>#### Method Overview ####
>Called at subsystem boot
>
### public Phase getInitialPhase () ###
>#### Method Overview ####
>Get the initial subsystem phase
>
### public void init () ###
>#### Method Overview ####
>Called at the end of subsystem boot
>
### public void beginPhase () ###
>#### Method Overview ####
>Called whenever a new phase is started
>
### public void checkEnv (Object) ###
>#### Method Overview ####
>Check whether the supplied object is a valid boot source for mixin
 environment
>
>### Parameters ###
>**bootSource**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;boot source
>
### public ReEntranceLock getReEntranceLock () ###
>#### Method Overview ####
>Get the transformer re-entrance lock for this service, the transformer
 uses this lock to track transformer re-entrance when co-operative load
 and transform is performed by the service.
>
### public IClassProvider getClassProvider () ###
>#### Method Overview ####
>Return the class provider for this service
>
### public IClassBytecodeProvider getBytecodeProvider () ###
>#### Method Overview ####
>Return the class bytecode provider for this service
>
### public Collection getPlatformAgents () ###
>#### Method Overview ####
>Get additional platform agents for this service
>
### public InputStream getResourceAsStream (String) ###
>#### Method Overview ####
>Get a resource as a stream from the appropriate classloader, this is
 delegated via the service so that the service can choose the correct
 classloader from which to obtain the resource.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;input stream or null if resource not found
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;resource path
>
### public void registerInvalidClass (String) ###
>#### Method Overview ####
>Register an invalid class with the service classloader
>
>### Parameters ###
>**className**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;invalid class name
>
### public boolean isClassLoaded (String) ###
>#### Method Overview ####
>Check whether the specified class was already loaded by the service
 classloader
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if the class was already loaded
>
>### Parameters ###
>**className**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;class name to check
>
### public Collection getTransformers () ###
>#### Method Overview ####
>Get currently available transformers in the environment
>
### public String getSideName () ###
>#### Method Overview ####
>Get the detected side name for this environment
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)