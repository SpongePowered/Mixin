[< Back](../README.md)
# public abstract Injector Injector #
>#### Class Overview ####
>Base class for bytecode injectors
## Fields ##
### protected static final Logger logger ###
>#### Field Overview ####
>Log more things
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected InjectionInfo info ###
>#### Field Overview ####
>Injection info
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final ClassNode classNode ###
>#### Field Overview ####
>Class node
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final MethodNode methodNode ###
>#### Field Overview ####
>Callback method
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final Type methodArgs ###
>#### Field Overview ####
>Arguments of the handler method
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final Type returnType ###
>#### Field Overview ####
>Return type of the handler method
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final boolean isStatic ###
>#### Field Overview ####
>True if the callback method is static
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Constructors ##
### public Injector (InjectionInfo) ###
>#### Constructor Overview ####
>Make a new CallbackInjector for the supplied InjectionInfo
>
>### Parameters ###
>**info**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Information about this injection
>
## Methods ##
### public String toString () ###
>#### Method Overview ####
>No description provided
>
### public final List find (InjectorTarget, List) ###
>#### Method Overview ####
>...
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;discovered injection points
>
>### Parameters ###
>**injectorTarget**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target method to inject into
>
>**injectionPoints**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;InjectionPoint instances which will identify
      target insns in the target method
>
### protected void addTargetNode (Target, List, AbstractInsnNode, Set) ###
>#### Method Overview ####
>No description provided
>
### public final void inject (Target, List) ###
>#### Method Overview ####
>Performs the injection on the specified target
>
>### Parameters ###
>**target**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;target to inject into
>
>**nodes**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;selected nodes
>
### protected boolean findTargetNodes (MethodNode, InjectionPoint, InsnList, Collection) ###
>#### Method Overview ####
>No description provided
>
### protected void sanityCheck (Target, List) ###
>#### Method Overview ####
>No description provided
>
### protected abstract void inject (Target, InjectionNodes.InjectionNode) ###
>#### Method Overview ####
>No description provided
>
### protected void postInject (Target, InjectionNodes.InjectionNode) ###
>#### Method Overview ####
>No description provided
>
### protected AbstractInsnNode invokeHandler (InsnList) ###
>#### Method Overview ####
>Invoke the handler method
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;injected insn node
>
>### Parameters ###
>**insns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instruction list to inject into
>
### protected AbstractInsnNode invokeHandler (InsnList, MethodNode) ###
>#### Method Overview ####
>Invoke a handler method
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;injected insn node
>
>### Parameters ###
>**insns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Instruction list to inject into
>
>**handler**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Actual method to invoke (may be different if using a
      surrogate)
>
### protected void throwException (InsnList, String, String) ###
>#### Method Overview ####
>Throw an exception. The exception class must have a string which takes a
 string argument
>
>### Parameters ###
>**insns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Insn list to inject into
>
>**exceptionType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type of exception to throw (binary name)
>
>**message**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Message to pass to the exception constructor
>
### public static boolean canCoerce (Type, Type) ###
>#### Method Overview ####
>Returns whether the <tt>from</tt> type can be coerced to the <tt>to</tt>
 type.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if <tt>from</tt> can be coerced to <tt>to</tt>
>
>### Parameters ###
>**from**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type to coerce from
>
>**to**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type to coerce to
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)