[< Back](../README.md)
# public interface Inject Inject #
>#### Class Overview ####
>Specifies that this mixin method should inject a callback (or
 callback<b>s</b>) to itself in the target method(s) identified by
 {@link #method}.
## Methods ##
### public String id () ###
>#### Method Overview ####
>The identifier for this injector, can be retrieved via the
 {@link CallbackInfo#getId} accessor. If not specified, the ID defaults to
 the target method name.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the injector id to use
>
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
>Array of {@link Slice} annotations which describe the method bisections
 used in the {@link #at} queries for this injector.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;slices
>
### public At at () ###
>#### Method Overview ####
>Array of {@link At} annotations which describe the
 {@link InjectionPoint}s in the target method. Allows one or more
 callbacks to be injected in the target method.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;injection point specifiers for this injector
>
### public boolean cancellable () ###
>#### Method Overview ####
>Setting an injected callback to <em>cancellable</em> allows the injected
 callback to inject optional RETURN opcodes into the target method, the
 return behaviour can then be controlled from within the callback by
 interacting with the supplied {@link CallbackInfo} object.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if this injector should inject appropriate RETURN opcodes
      which allow it to be cancelled
>
### public LocalCapture locals () ###
>#### Method Overview ####
>Specifies the local variable capture behaviour for this injector.
 
 <p>When capturing local variables in scope, the variables are appended to
 the callback invocation after the {@link CallbackInfo} argument.</p>
 
 <p>Capturing local variables from the target scope requires careful
 planning because unlike other aspects of an injection (such as the target
 method name and signature), the local variable table is <b>not</b> safe
 from modification by other transformers which may be in use in the
 production environment. Even other injectors which target the same target
 method have the ability to modify the local variable table and thus it is
 in no way safe to assume that local variables in scope at development
 time will be so in production.</p>
 
 <p>To provide some level of flexibility, especially where changes can be
 anticipated (for example a well-known mod makes changes which result in a
 particular structure for the local variable table) it is possible to
 provide <em>overloads</em> for the handler method which will become
 surrogate targets for the orphaned injector by annotating them with an
 {@link Surrogate} annotation.</p>
 
 <p>It is also important to nominate the failure behaviour to follow when
 local capture fails and so all {@link LocalCapture} behaviours which
 specify a capture action imply a particular behaviour for handling
 failure. See the javadoc on the {@link LocalCapture} members for more
 details.</p>
 
 <p>Determining what local variables are available to you and in what
 order can be somewhat tricky, and so a simple mechanism for enumerating
 available locals is provided. By setting <code>locals</code> to
 {@link LocalCapture#PRINT}, the injector writes the local capture state
 to STDERR instead of injecting the callback. Using the output thus
 obtained it is then a straightforward matter of altering the callback
 method signature to match the signature proposed by the Callback
 Injector.</p>
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the desired local capture behaviour for this injector
>
### public boolean remap () ###
>#### Method Overview ####
>By default, the annotation processor will attempt to locate an
 obfuscation mapping for all {@link Inject} methods since it is
 anticipated that in general the target of a {@link Inject} annotation
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