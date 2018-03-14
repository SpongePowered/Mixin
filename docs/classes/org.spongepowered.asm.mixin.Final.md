[< Back](../README.md)
# public interface Final Final #
>#### Class Overview ####
>This annotation has two uses:
 
 <ul>
   <li>
     On an {@link Shadow} field, it can be used to raise an error-level log
     message if any write occurrences appear in the mixin bytecode. This can
     be used in place of declaring the field as actually <tt>final</tt>. This
     is required since it is normally desirable to remove the <tt>final</tt>
     modifier from shadow fields to avoid unwanted field initialisers. If
     {@link Option#DEBUG_VERIFY} is <tt>true</tt>, then an
     {@link InvalidMixinException} is thrown.
   </li>
   <li>
     On an {@link Inject injector} or {@link Overwrite overwritten} method,
     it is equivalent to setting the priority of the containing mixin to
     {@link Integer#MAX_VALUE} but applies only to the annotated method. This
     allows methods to mark themselves as effectively final, preventing their
     replacement by later mixins with higher priority.
   </li>
 </ul>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)