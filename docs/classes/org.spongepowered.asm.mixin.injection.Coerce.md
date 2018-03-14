[< Back](../README.md)
# public interface Coerce Coerce #
>#### Class Overview ####
><h3>Usage with callback injectors (<tt>&#064;Inject</tt>)</h3>
 
 <p>This annotation has two usages applicable to Callback Injectors (defined
 using {@link Inject &#064;Inject}. For local capture injectors, it indicates
 that the injector should coerce top-level primitive types (int) to covariant
 types defined on the handler. For other injectors, it can be used on a
 reference type to indicate that the intended type is covariant over the
 argument type (or that the argument type is contravariant on the target class
 type). This can be used for multi-target injectors with a bounded type
 argument on the class or target method.</p>
 
 <p>During LVT generation it is not always possible to inflect the exact local
 type for types represented internally as integers, for example booleans and
 shorts. However adding a surrogate for these cases is overkill when the type
 is known for certain by the injector. Since the bytecode for all types stored
 as integer interally will be valid, we can force the local type to any
 covariant type as long as we know this in advance.</p>
 
 <p>This annotation allows a covariant type parameter to be marked, and thus
 coerced to the correct type when the LVT generation would otherwise mark the
 type as invalid.</p>
 
 <h3>Usage with redirect injectors (<tt>&#064;Redirect</tt>)</h3>
 
 <p>Similarly to callback injectors, <tt>&#064;Coerce</tt> can be used to
 indicate that an incoming parameter can be consumed as a valid supertype, up
 to and including {@link Object}. This is particularly useful when an argument
 on the target method invocation is inaccessible or unknown.</p>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)