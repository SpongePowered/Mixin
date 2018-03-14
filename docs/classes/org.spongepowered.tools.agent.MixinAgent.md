[< Back](../README.md)
# MixinAgent #
>#### Class Overview ####
>An agent that re-transforms a mixin's target classes if the mixin has been
 redefined. Basically this agent enables hot-swapping of mixins.
## Fields ##
### public static final byte ERROR_BYTECODE ###
>#### Field Overview ####
>Bytecode that signals an error. When returned from the class file
 transformer this causes a class file format exception and indicates in
 the ide that somethings went wrong.
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Constructors ##
### public MixinAgent (MixinTransformer) ###
>#### Constructor Overview ####
>Constructs an agent from a class transformer in which it will use to
 transform mixin's target class.
>
>### Parameters ###
>**classTransformer**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Class transformer that will transform a mixin's
                         target class
>
## Methods ##
### public void registerMixinClass (String) ###
>#### Method Overview ####
>No description provided
>
### public void registerTargetClass (String, byte[]) ###
>#### Method Overview ####
>No description provided
>
### public static void init (Instrumentation) ###
>#### Method Overview ####
>Sets the instrumentation instance so that the mixin agents can redefine
 mixins.
>
>### Parameters ###
>**instrumentation**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instance to use to redefine the mixins
>
### public static void premain (String, Instrumentation) ###
>#### Method Overview ####
>Initialize the java agent

 <p>This will be called automatically if the jar is in a -javaagent java
 command line argument</p>
>
>### Parameters ###
>**arg**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Ignored
>
>**instrumentation**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instance to use to transform the mixins
>
### public static void agentmain (String, Instrumentation) ###
>#### Method Overview ####
>Initialize the java agent

 <p>This will be called automatically if the java agent is loaded after
 JVVM startup</p>
>
>### Parameters ###
>**arg**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Ignored
>
>**instrumentation**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instance to use to re-define the mixins
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)