[< Back](../README.md)
# public interface Pseudo Pseudo #
>#### Class Overview ####
>A Mixin marked as <b>Pseudo</b> is allowed to target classes which are not
 available at compile time and may not be available at runtime. This means
 that certain restrictions apply:
 
 <p>In particular, the superclass requirement for pseudo mixins is extremely
 important if the target has an obfuscated class in its hierarchy. For example
 let's assume that we're mixing into a class <tt>SomeCustomScreen</tt> from
 another party which extends <tt>GuiScreen</tt> which is obfuscated.
 Attempting to inject into <tt>initGui</tt> will succeed at dev time and fail
 at production time, because the reference is obfuscated. We can overcome this
 by ensuring the mixin inherits from the same superclass, thus allowing
 <tt>initGui</tt> to be resolved in the superclass hierarchy (this is not the
 case for normal mixins).</p>
 
 <p>{@link Overwrite} methods which are <b>not</b> inherited from a superclass
 (if the target is obfuscated) <b>must</b> be decorated manually with aliases.
 </p>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)