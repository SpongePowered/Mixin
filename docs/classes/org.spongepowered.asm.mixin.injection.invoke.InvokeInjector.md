[< Back](../README.md)
# public abstract InvokeInjector InvokeInjector #
>#### Class Overview ####
>Base class for injectors which inject at method invokes
## Fields ##
### protected final String annotationType ###
>#### Field Overview ####
>No description provided
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Constructors ##
### public InvokeInjector (InjectionInfo, String) ###
>#### Constructor Overview ####
>No description provided
>
>### Parameters ###
>**info**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Information about this injection
>
>**annotationType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Annotation type, used for error messages
>
## Methods ##
### protected void sanityCheck (Target, List) ###
>#### Method Overview ####
>No description provided
>
### protected void checkTarget (Target) ###
>#### Method Overview ####
>Sanity checks on target
>
>### Parameters ###
>**target**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;target
>
### protected final void checkTargetModifiers (Target, boolean) ###
>#### Method Overview ####
>Check that the <tt>static</tt> modifier of the target method matches the
 handler
>
>### Parameters ###
>**target**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target to check
>
>**exactMatch**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;True if static must match, false to only check if an
      instance handler is targetting a static method
>
### protected void checkTargetForNode (Target, InjectionNodes.InjectionNode) ###
>#### Method Overview ####
>The normal staticness check is not location-aware, in that it merely
 enforces static modifiers of handlers to match their targets. For
 injecting into constructors however (which are ostensibly instance
 methods) calls which are injected <em>before</em> the call to <tt>
 super()</tt> cannot access <tt>this</tt> and must therefore be declared
 as static.
>
>### Parameters ###
>**target**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target method
>
>**node**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Injection location
>
### protected void inject (Target, InjectionNodes.InjectionNode) ###
>#### Method Overview ####
>No description provided
>
### protected abstract void injectAtInvoke (Target, InjectionNodes.InjectionNode) ###
>#### Method Overview ####
>Perform a single injection
>
>### Parameters ###
>**target**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target to inject into
>
>**node**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Discovered instruction node
>
### protected AbstractInsnNode invokeHandlerWithArgs (Type[], InsnList, int[]) ###
>#### Method Overview ####
>No description provided
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;injected insn node
>
>### Parameters ###
>**args**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;handler arguments
>
>**insns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;InsnList to inject insns into
>
>**argMap**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Mapping of args to local variables
>
### protected AbstractInsnNode invokeHandlerWithArgs (Type[], InsnList, int[], int, int) ###
>#### Method Overview ####
>No description provided
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;injected insn node
>
>### Parameters ###
>**args**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;handler arguments
>
>**insns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;InsnList to inject insns into
>
>**argMap**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Mapping of args to local variables
>
>**startArg**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Starting arg to consume
>
>**endArg**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Ending arg to consume
>
### protected int storeArgs (Target, Type[], InsnList, int) ###
>#### Method Overview ####
>Store args on the stack starting at the end and working back to position
 specified by start, return the generated argMap
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the generated argmap
>
>### Parameters ###
>**target**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;target method
>
>**args**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;argument types
>
>**insns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;instruction list to generate insns into
>
>**start**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Starting index
>
### protected void storeArgs (Type[], InsnList, int[], int, int) ###
>#### Method Overview ####
>Store args on the stack to their positions allocated based on argMap
>
>### Parameters ###
>**args**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;argument types
>
>**insns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;instruction list to generate insns into
>
>**argMap**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;generated argmap containing local indices for all args
>
>**start**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Starting index
>
>**end**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Ending index
>
### protected void pushArgs (Type[], InsnList, int[], int, int) ###
>#### Method Overview ####
>Load args onto the stack from their positions allocated in argMap
>
>### Parameters ###
>**args**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;argument types
>
>**insns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;instruction list to generate insns into
>
>**argMap**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;generated argmap containing local indices for all args
>
>**start**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Starting index
>
>**end**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Ending index
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)