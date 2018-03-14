[< Back](../README.md)
# Dynamic #
>#### Class Overview ####
>Decorator annotation for mixin elements whose targets are not available in
 the original class and are either fabricated or transformed at runtime. This
 annotation is purely for decoration purposes and has no semantic implications
 of any kind.
 
 <p>Its use on mixin elements is encouraged because it yields the following
 benefits:</p>
 
 <ol>
   <li>Aids mixin maintainers by providing context for seemingly-pointless
     injections. An injector or overwrite whose target does not appear to
     exist can be given context by decorating it with this annotation.</li>
   <li>Allowing mixin tooling such as IDE plugins to gain awareness that a
     particular target can be allowed to fail validation from static analysis.
     In the case where tooling might flag the target as invalid, this
     annotation can provide context to suppress or enhance generated warnings.
     </li>
   <li>Provide additional context when an injection fails. For example if an
     injector decorated with <tt>&#064;Dynamic</tt> fails to locate a valid
     target, mixin will include the contents of the <tt>&#064;Dynamic</tt>
     annotation with the error message, providing additional debugging
     information to end users. This might, for example, be used to identify
     when an upstream transformation is disabled or changed.</li>
 </ol>
 
 <p>Because of the possible inclusion in error messages, it is suggested that
 the description provided via <tt>&#064;Dynamic</tt> provides as much
 information in as terse a manner possible. Good examples of text descriptions
 to include might be:
 
 <ul>
   <li><tt>"Method Foo.bar is added at runtime by mod X"</tt></li>
   <li><tt>"All calls to Foo.bar are replaced at runtime by mod Y with a calls
     to Baz.flop"</tt></li>
   <li><tt>"Added by SomeOtherUpstreamMixin"</tt></li>
 </ul>
 
 <p>In other words, the contents of the <tt>&#064;Dynamic</tt> should try to
 provide as much useful context for the decorated method without being overly
 verbose.</p>
 
 <p>In the case where the target method or field is added by an upstream
 mixin, and the mixin in question is on the classpath of the project, it is
 also possible to use the {@link #mixin} value to specify the mixin which
 contributes the target member. This provides both a useful navigable link in
 IDEs, and useful context for mixin tooling such as IDE plugins.</p>
 
 <p>If the target member is contributed by an upstream mixin but the mixin is
 <em>not</em> on the classpath of the current project, using the fully-
 qualified name of the upstream mixin as the {@link #value} is a useful
 fallback, since developers reading the source code can still use the string
 value as a search term in their IDE or on Github.</p>
## Methods ##
### public String value () ###
>#### Method Overview ####
>Description of this <tt>&#064;Dynamic</tt> member. See the notes above.
 Can be omitted if {@link #mixin} is specified instead.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;description of the member
>
### public Class mixin () ###
>#### Method Overview ####
>If the target member is added by an upstream mixin, and the mixin in
 question is on the classpath, specify the mixin class here in order to
 provide useful context for the annotated member.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;upstream mixin reference
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)