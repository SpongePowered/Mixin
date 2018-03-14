[< Back](../README.md)
# Unique #
>#### Class Overview ####
>This annotation, when applied to a member method or field in a mixin,
 indicates that the member <b>should never</b> overwrite a matching member in
 the target class. This indicates that the member differs from the normal
 "overlay-like" behaviour of mixins in general, and should only ever be
 <em>added</em> to the target. For public fields, the annotation has no
 effect.
 
 <p>Typical usage of this annotation would be to decorate a utility method in
 a mixin, or mark an interface-implementing method which must not overwrite a
 target if it exists (consider appropriate use of {@link Intrinsic} in these
 situations).</p>

 <p>Because of the mixed usage, this annotation has different implications for
 methods with differing visibility:</p>
 
 <dl>
   <dt>public methods</dt>
   <dd>public methods marked with this annotation are <b>discarded</b> if a
   matching target exists. Unless {@link #silent} is set to <tt>true</tt>, a
   <tt>warning</tt>-level message is generated.</dd>
   <dt>private and protected methods</dt>
   <dd>non-public methods are <b>renamed</b> if a matching target method is
   found, this allows utility methods to be safely assigned meaningful names
   in code, but renamed if a conflict occurs when a mixin is applied.</dd>
 </dl>
 
 <p><strong>Notes</strong></p>
 
 <ul>
   <li>To mark all methods in a mixin as unique, apply the annotation to the
     mixin itself</li>
   <li>Uniqueness can be defined on a per-interface basis by using an
     {@link Implements} annotation with <tt>unique</tt> set to <tt>true</tt>
     </li>
 </ul>
## Methods ##
### public boolean silent () ###
>#### Method Overview ####
>If this annotation is applied to a public method in a mixin and a
 conflicting method is found in a target class, then a
 <tt>warning</tt>-level message is generated in the log. To suppress this
 message, set this value to <tt>true</tt>.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true to suppress warning message when a public method is
      discarded
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)