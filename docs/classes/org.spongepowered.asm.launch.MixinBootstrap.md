[< Back](../README.md)
# MixinBootstrap #
>#### Class Overview ####
>Bootstaps the mixin subsystem. This class acts as a bridge between the mixin
 subsystem and the tweaker or coremod which is boostrapping it. Without this
 class, a coremod may cause classload of MixinEnvironment in the
 LaunchClassLoader before we have a chance to exclude it. By placing the main
 bootstap logic here we avoid the need for consumers to add the classloader
 exclusion themselves.
 
 <p>In development, where (because of the classloader environment at dev time)
 it is safe to let a coremod initialise the mixin subsystem, we can perform
 initialisation all in one go using the {@link #init} method and everything is
 fine. However in production the tweaker must be used and the situation is a
 little more delicate.</p>
 
 <p>In an ideal world, the mixin tweaker would initialise the environment in
 its constructor and that would be the end of the story. However we also need
 to register the additional tweaker for environment to detect the transition
 from pre-init to default and we cannot do this within the tweaker constructor
 witout triggering a ConcurrentModificationException in the tweaker list. To
 work around this we register the secondary tweaker from within the mixin 
 tweaker's acceptOptions method instead.</p>
## Fields ##
### public static final String VERSION ###
>#### Field Overview ####
>Subsystem version
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;0.7.6
>
## Methods ##
### public static void addProxy () ###
>#### Method Overview ####
>No description provided
>
### public static MixinPlatformManager getPlatform () ###
>#### Method Overview ####
>Get the platform manager
>
### public static void init () ###
>#### Method Overview ####
>Initialise the mixin subsystem
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)