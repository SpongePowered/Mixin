[< Back](../README.md)
# Interface #
>#### Class Overview ####
>I'm probably going to the special hell for this
## Methods ##
### public Class iface () ###
>#### Method Overview ####
>Interface that the parent {@link Implements} indicates the mixin 
 implements. The interface will be hot-patched onto the target class as
 part of the mixin application.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;interface to implement
>
### public String prefix () ###
>#### Method Overview ####
>[Required] prefix for implementing interface methods. Works similarly to
 {@link Shadow} prefixes, but <b>must</b> end with a dollar sign ($)
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;prefix to use
>
### public boolean unique () ###
>#### Method Overview ####
>If set to <tt>true</tt>, all methods implementing this interface are
 treated as if they were individually decorated with {@link Unique}
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true to mark all implementing methods as unique
>
### public Remap remap () ###
>#### Method Overview ####
>By default, the annotation processor will attempt to locate an
 obfuscation mapping for all methods soft-implemented by the interface
 declared in this {@link Interface} annotation, since it is possible that
 the declared interface may be obfuscated and therefore contain obfuscated
 member methods. However since it may be desirable to skip this pass (for
 example if an interface method intrinsically shadows a soft-implemented
 method) this setting is provided to restrict or inhibit processing of
 member methods matching this soft-implements decoration.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Remapping strategy to use, see {@link Remap} for details.
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)