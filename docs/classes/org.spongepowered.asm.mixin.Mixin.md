[< Back](../README.md)
# public interface Mixin Mixin #
>#### Class Overview ####
>Decorator for mixin classes
## Methods ##
### public Class value () ###
>#### Method Overview ####
>Target class(es) for this mixin
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;classes this mixin targets
>
### public String targets () ###
>#### Method Overview ####
>Since specifying targets in {@link #value} requires that the classes be
 publicly visible, this property is provided to allow package-private,
 anonymous innner, and private inner classes to be referenced. Referencing
 an otherwise public class using this property is an error condition and
 will throw an exception at runtime. It is completely fine to specify both
 public and private targets for the same mixin however.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;protected or package-private classes this mixin targets
>
### public int priority () ###
>#### Method Overview ####
>Priority for the mixin, relative to other mixins targetting the same
 classes
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the mixin priority (relative to other mixins targetting the same
      class)
>
### public boolean remap () ###
>#### Method Overview ####
>By default, the annotation processor will attempt to locate an
 obfuscation mapping for all {@link Shadow} and
 {@link org.spongepowered.asm.mixin.injection.Inject} annotated members
 since it is anticipated that in general the target of a {@link Mixin}
 will be an obfuscated class and all annotated members will need to be
 added to the obfuscation table. However since it is possible to also
 apply mixins to non-obfuscated targets it may be desirable to suppress
 the compiler warnings which would otherwise be generated. This can be
 done on an individual member basis by setting <code>remap</code> to
 <em>false</em> on the individual annotations, or disabled for the entire
 mixin by setting the value here to <em>false</em>. Doing so will cause
 the annotation processor to skip all annotations in this mixin when
 building the obfuscation table unless the individual annotation is
 explicitly decorated with <tt>remap = true</tt>.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;True to instruct the annotation processor to search for
      obfuscation mappings for this annotation (default true).
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)