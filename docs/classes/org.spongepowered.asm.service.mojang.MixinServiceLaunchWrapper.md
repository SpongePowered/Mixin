[< Back](../README.md)
# public MixinServiceLaunchWrapper MixinServiceLaunchWrapper #
>#### Class Overview ####
>Mixin service for launchwrapper
## Fields ##
### public static final String BLACKBOARD_KEY_TWEAKCLASSES ###
>#### Field Overview ####
>No description provided
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;TweakClasses
>
### public static final String BLACKBOARD_KEY_TWEAKS ###
>#### Field Overview ####
>No description provided
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Tweaks
>
## Constructors ##
### public MixinServiceLaunchWrapper () ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public String getName () ###
>#### Method Overview ####
>No description provided
>
### public boolean isValid () ###
>#### Method Overview ####
>No description provided
>
### public void prepare () ###
>#### Method Overview ####
>No description provided
>
### public Phase getInitialPhase () ###
>#### Method Overview ####
>No description provided
>
### public void init () ###
>#### Method Overview ####
>No description provided
>
### public ReEntranceLock getReEntranceLock () ###
>#### Method Overview ####
>No description provided
>
### public Collection getPlatformAgents () ###
>#### Method Overview ####
>No description provided
>
### public IClassProvider getClassProvider () ###
>#### Method Overview ####
>No description provided
>
### public IClassBytecodeProvider getBytecodeProvider () ###
>#### Method Overview ####
>No description provided
>
### public Class findClass (String) ###
>#### Method Overview ####
>No description provided
>
### public Class findClass (String, boolean) ###
>#### Method Overview ####
>No description provided
>
### public Class findAgentClass (String, boolean) ###
>#### Method Overview ####
>No description provided
>
### public void beginPhase () ###
>#### Method Overview ####
>No description provided
>
### public void checkEnv (Object) ###
>#### Method Overview ####
>No description provided
>
### public InputStream getResourceAsStream (String) ###
>#### Method Overview ####
>No description provided
>
### public void registerInvalidClass (String) ###
>#### Method Overview ####
>No description provided
>
### public boolean isClassLoaded (String) ###
>#### Method Overview ####
>No description provided
>
### public URL getClassPath () ###
>#### Method Overview ####
>No description provided
>
### public Collection getTransformers () ###
>#### Method Overview ####
>No description provided
>
### public byte getClassBytes (String, String) ###
>#### Method Overview ####
>No description provided
>
### public byte getClassBytes (String, boolean) ###
>#### Method Overview ####
>Loads class bytecode from the classpath
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Transformed class bytecode for the specified class
>
>### Parameters ###
>**className**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Name of the class to load
>
>**runTransformers**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;True to run the loaded bytecode through the
      delegate transformer chain
>
>### Throws ###
>**ClassNotFoundException**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;if the specified class could not be loaded
>
>**IOException**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;if an error occurs whilst reading the specified class
>
### public ClassNode getClassNode (String) ###
>#### Method Overview ####
>No description provided
>
### public final String getSideName () ###
>#### Method Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)