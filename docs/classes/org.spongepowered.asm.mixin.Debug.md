[< Back](../README.md)
# public interface Debug Debug #
>#### Class Overview ####
>Anotation used to decorate items you might wish to examine after mixin
 application.
## Methods ##
### public boolean export () ###
>#### Method Overview ####
>Only applicable for classes, use this to decorate mixins that you wish
 to export even if <tt>mixin.debug.export</tt> is not enabled. This is
 useful if you wish to export only the target of the mixin you are working
 on without enabling export globally.
 
 <p>Note that if <tt>mixin.debug.export</tt> is <b>not</b> <tt>true</tt>
 then the decompiler is <em>not</em> initialised (even if present on the
 classpath) and thus you must set <tt>mixin.debug.export.decompile</tt> to
 <tt>true</tt> in order to have force-exported classes decompiled.</p>
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;whether to export the decorated target class
>
### public boolean print () ###
>#### Method Overview ####
>Print the method bytecode to the console after mixin application. This
 setting is only used if the <tt>mixin.debug.verbose</tt> option is
 enabled.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;whether to print the class or method to the console.
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)