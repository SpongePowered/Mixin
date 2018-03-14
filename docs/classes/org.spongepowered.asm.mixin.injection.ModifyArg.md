[< Back](../README.md)
# ModifyArg #
>#### Class Overview ####
>Specifies that this mixin method should inject an argument modifier to itself
 in the target method(s) identified by {@link #method}. This type of injection
 provides a lightweight mechanism for changing a single argument of a target
 method invocation. To affect multiple arguments at once, use {@link
 ModifyArgs} instead. 
 
 <p>Consider the following method call:</p>
 
 <blockquote><pre>// x, y and z are of type float
someObject.setLocation(x, y, z, true);</pre></blockquote>
 
 <p>Let us assume that we wish to modify the <tt>y</tt> value in the method
 call. We know that the arguments are <tt>float</tt>s and that the <tt>y</tt>
 value is the <em>second</em> (index = 1) <tt>float</tt> argument. Thus our
 injector requires the following signature:
  
 <blockquote><pre>&#064;ModifyArg(method = "...", at = ..., index = 1)
private float adjustYCoord(float y) {
    return y + 64.0F;
}</pre></blockquote>
 
 <p>The callback consumes the original value of <tt>y</tt> and returns the
 adjusted value.</p>
 
 <p><tt>&#064;ModifyArg</tt> can also consume all of the target method's
 arguments if required, to provide additional context for the callback. In
 this case the arguments of the callback should match the target method:</p> 
  
 <blockquote><pre>&#064;ModifyArg(method = "...", at = ..., index = 1)
private float adjustYCoord(float x, float y, float z, boolean interpolate) {
    return (x == 0 && y == 0) ? 0 : y;
}</pre></blockquote>
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
### public int index () ###
>#### Method Overview ####
><p>Gets the argument index on the target to set. It is not necessary to
 set this value if there is only one argument of the modifier type in the
 hooked method's signature. For example if the target method accepts a
 boolean, an integer and a String, and the modifier method accepts and
 returns an integer, then the integer parameter will be automatically
 selected.</p>
 
 <p>The index is zero-based.</p>
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;argument index to modify or -1 for automatic
>
### public boolean remap () ###
>#### Method Overview ####
>By default, the annotation processor will attempt to locate an
 obfuscation mapping for all {@link ModifyArg} methods since it is
 anticipated that in general the target of a {@link ModifyArg} annotation
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