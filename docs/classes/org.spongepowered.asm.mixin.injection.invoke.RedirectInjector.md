[< Back](../README.md)
# public RedirectInjector RedirectInjector #
>#### Class Overview ####
><p>A bytecode injector which allows a method call, field access or
 <tt>new</tt> object creation to be redirected to the annotated handler
 method. For method redirects, the handler method signature must match the
 hooked method precisely <b>but</b> prepended with an arg of the owning
 object's type to accept the object instance the method was going to be
 invoked upon. For example when hooking the following call:</p>
 
 <blockquote><pre>
   int abc = 0;
   int def = 1;
   Foo someObject = new Foo();
   
   // Hooking this method
   boolean xyz = someObject.bar(abc, def);</pre>
 </blockquote>
 
 <p>The signature of the redirected method should be:</p>
 
 <blockquote>
      <pre>public boolean barProxy(Foo someObject, int abc, int def)</pre>
 </blockquote>
 
 <p>For obvious reasons this does not apply for static methods, for static
 methods it is sufficient that the signature simply match the hooked method.
 </p> 
 
 <p>For field redirections, see the details in {@link Redirect} for the
 required signature of the handler method.</p>
 
 <p>For constructor redirections, the signature of the handler method should
 match the constructor itself, return type should be of the type of object
 being created.</p>
## Fields ##
### protected Meta meta ###
>#### Field Overview ####
>No description provided
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Constructors ##
### public RedirectInjector (InjectionInfo) ###
>#### Constructor Overview ####
>No description provided
>
>### Parameters ###
>**info**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Injection info
>
### protected RedirectInjector (InjectionInfo, String) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### protected void checkTarget (Target) ###
>#### Method Overview ####
>No description provided
>
### protected void addTargetNode (Target, List, AbstractInsnNode, Set) ###
>#### Method Overview ####
>No description provided
>
### protected void inject (Target, InjectionNodes.InjectionNode) ###
>#### Method Overview ####
>No description provided
>
### protected boolean preInject (InjectionNodes.InjectionNode) ###
>#### Method Overview ####
>No description provided
>
### protected void postInject (Target, InjectionNodes.InjectionNode) ###
>#### Method Overview ####
>No description provided
>
### protected void injectAtInvoke (Target, InjectionNodes.InjectionNode) ###
>#### Method Overview ####
>Redirect a method invocation
>
### protected void validateParams (RedirectInjector.RedirectedInvoke) ###
>#### Method Overview ####
>Perform validation of an invoke handler parameters, each parameter in the
 handler must match the expected type or be annotated with {@link Coerce}
 and be a supported supertype of the incoming type.
>
>### Parameters ###
>**invoke**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;invocation being redirected
>
### public void injectArrayRedirect (Target, FieldInsnNode, AbstractInsnNode, boolean, String) ###
>#### Method Overview ####
>The code for actually redirecting the array element is the same
 regardless of whether it's a read or write because it just depends on the
 actual handler signature, the correct arguments are already on the stack
 thanks to the nature of xALOAD and xASTORE.
>
>### Parameters ###
>**target**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;target method
>
>**fieldNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;field node
>
>**varNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;array access node
>
>**withArgs**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if the descriptor includes captured arguments from
      the target method signature
>
>**type**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;description of access type for use in error messages
>
### public void injectAtScalarField (Target, FieldInsnNode, int, Type, Type) ###
>#### Method Overview ####
>Redirect a field get or set
>
>### Parameters ###
>**target**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;target method
>
>**fieldNode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;field access node
>
>**opCode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;field access type
>
>**ownerType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type of the field owner
>
>**fieldType**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;field type
>
### protected boolean checkDescriptor (String, Target, String) ###
>#### Method Overview ####
>Check that the handler descriptor matches the calculated descriptor for
 the access being redirected.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if the descriptor was found and includes target method args,
      false if the descriptor was found and does not capture target args
>
>### Parameters ###
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;computed descriptor
>
>**target**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;target method
>
>**type**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;redirector type in human-readable text, for error messages
>
### protected void injectAtConstructor (Target, InjectionNodes.InjectionNode) ###
>#### Method Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)