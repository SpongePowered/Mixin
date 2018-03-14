[< Back](../README.md)
# public interface ModifyArgs ModifyArgs #
>#### Class Overview ####
>Specifies that this mixin method should inject an multi-argument modifier
 callback to itself in the target method(s) identified by {@link #method}.
 This type of injector provides a powerful but inefficient method for
 modifying multiple arguments of a method at once without making use of a
 {@link Redirect} injector. In general it is better to use redirectors where
 possible, however this type of injector can also function where {@link
 Redirect} cannot, such as modifying arguments of a super-constructor call. To
 modify a single method argument, use {@link ModifyArg} instead.
 
 <p>This injector works by creating an <em>argument bundle</em> in the form of
 {@link Args} which is passed to your handler method. You can manipulate the
 method arguments via the bundle in your handler method. The bundle is then
 unpacked and the original method is called with the modified arguments.</p>
 
 <p>Since the argument bundle is created for every invocation of the target
 method, and primitive types must undergo boxing and unboxing, this injector
 is intrinsically less efficient than other methods. However for certain uses
 this injector is more powerful:</p>
 
 <ul>
   <li>For modifying arguments of a superconstructor call, it would normally
     be necessary to employ multiple {@link ModifyArg} callbacks (one for each
     argument you wish to modify). However access to the enclosing scope is
     not provided by {@link ModifyArg}, which can be problematic.</li>
   <li>This injector can be used to target multiple methods with differing
     argument types and counts.</li>
 </ul>
 
 <p>Methods decorated with this injector should return <tt>void</tt> and
 return either:</p>
 
 <ul>
   <li>A single argument of type {@link Args}</li>
   <li>A single {@link Args} argument followed by the arguments of the
     enclosing target method.</li>
 </ul>
## Methods ##
### public String method () ###
>#### Method Overview ####
>String representation of one or more
 {@link org.spongepowered.asm.mixin.injection.struct.MemberInfo 
 MemberInfo} which identify the target methods.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;target method(s) for this injector
>
### public Slice slice () ###
>#### Method Overview ####
>A {@link Slice} annotation which describes the method bisection used in
 the {@link #at} query for this injector.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;slice
>
### public At at () ###
>#### Method Overview ####
>An {@link At} annotation which describes the {@link InjectionPoint} in
 the target method. The specified {@link InjectionPoint} <i>must only</i>
 return {@link org.spongepowered.asm.lib.tree.MethodInsnNode} instances
 and an exception will be thrown if this is not the case.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;{@link At} which identifies the target method invocation
>
### public boolean remap () ###
>#### Method Overview ####
>By default, the annotation processor will attempt to locate an
 obfuscation mapping for all {@link ModifyArgs} methods since it is
 anticipated that in general the target of a {@link ModifyArgs} annotation
 will be an obfuscated method in the target class. However since it is
 possible to also apply mixins to non-obfuscated targets (or non-
 obfuscated methods in obfuscated targets, such as methods added by Forge)
 it may be necessary to suppress the compiler error which would otherwise
 be generated. Setting this value to <em>false</em> will cause the
 annotation processor to skip this annotation when attempting to build the
 obfuscation table for the mixin.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;True to instruct the annotation processor to search for
      obfuscation mappings for this annotation
>
### public int require () ###
>#### Method Overview ####
>In general, injectors are intended to "fail soft" in that a failure to
 locate the injection point in the target method is not considered an
 error condition. Another transformer may have changed the method
 structure or any number of reasons may cause an injection to fail. This
 also makes it possible to define several injections to achieve the same
 task given <em>expected</em> mutation of the target class and the
 injectors which fail are simply ignored.
 
 <p>However, this behaviour is not always desirable. For example, if your
 application depends on a particular injection succeeding you may wish to
 detect the injection failure as an error condition. This argument is thus
 provided to allow you to stipulate a <b>minimum</b> number of successful
 injections for this callback handler. If the number of injections
 specified is not achieved then an {@link InjectionError} is thrown at
 application time. Use this option with care.</p>
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Minimum required number of injected callbacks, default specified
      by the containing config
>
### public int expect () ###
>#### Method Overview ####
>Like {@link #require()} but only enabled if the
 {@link Option#DEBUG_INJECTORS mixin.debug.countInjections} option is set
 to <tt>true</tt> and defaults to 1. Use this option during debugging to
 perform simple checking of your injectors. Causes the injector to throw
 a {@link InvalidInjectionException} if the expected number of injections
 is not realised.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Minimum number of <em>expected</em> callbacks, default 1
>
### public int allow () ###
>#### Method Overview ####
>Injection points are in general expected to match every candidate
 instruction in the target method or slice, except in cases where options
 such as {@link At#ordinal} are specified which naturally limit the number
 of results.
 
 <p>This option allows for sanity-checking to be performed on the results
 of an injection point by specifying a maximum allowed number of matches,
 similar to that afforded by {@link Group#max}. For example if your
 injection is expected to match 4 invocations of a target method, but
 instead matches 5, this can become a detectable tamper condition by
 setting this value to <tt>4</tt>.
 
 <p>Setting any value 1 or greater is allowed. Values less than 1 or less
 than {@link #require} are ignored. {@link #require} supercedes this
 argument such that if <tt>allow</tt> is less than <tt>require</tt> the
 value of <tt>require</tt> is always used.</p>
 
 <p>Note that this option is not a <i>limit</i> on the query behaviour of
 this injection point. It is only a sanity check used to ensure that the
 number of matches is not too high
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Maximum allowed number of injections for this
>
### public String constraints () ###
>#### Method Overview ####
>Returns constraints which must be validated for this injector to
 succeed. See {@link Constraint} for details of constraint formats.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Constraints for this annotation
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)