[< Back](../README.md)
# public abstract InjectionPoint InjectionPoint #
>#### Class Overview ####
><p>Base class for injection point discovery classes. Each subclass describes
 a strategy for locating code injection points within a method, with the
 {@link #find} method populating a collection with insn nodes from the method
 which satisfy its strategy.</p>
 
 <p>This base class also contains composite strategy factory methods such as
 {@link #and} and {@link #or} which allow strategies to be combined using
 intersection (and) or union (or) relationships to allow multiple strategies
 to be easily combined.</p>
 
 <p>You are free to create your own injection point subclasses, but take note
 that it <b>is allowed</b> for a single InjectionPoint instance to be used for
 multiple injections and thus implementing classes MUST NOT cache the insn
 list, event, or nodes instance passed to the {@link #find} method, as each
 call to {@link #find} must be considered a separate functional contract and
 the InjectionPoint's lifespan is not linked to the discovery lifespan,
 therefore it is important that the InjectionPoint implementation is fully
 <b>stateless</b>.</p>
## Fields ##
### public static final int DEFAULT_ALLOWED_SHIFT_BY ###
>#### Field Overview ####
>Initial limit on the value of {@link At#by} which triggers warning/error
 (based on environment)
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;0
>
### public static final int MAX_ALLOWED_SHIFT_BY ###
>#### Field Overview ####
>Hard limit on the value of {@link At#by} which triggers warning/error
 (based on environment)
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;0
>
## Constructors ##
### protected InjectionPoint () ###
>#### Constructor Overview ####
>No description provided
>
### protected InjectionPoint (InjectionPointData) ###
>#### Constructor Overview ####
>No description provided
>
### public InjectionPoint (String, InjectionPoint.Selector, String) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public String getSlice () ###
>#### Method Overview ####
>No description provided
>
### public Selector getSelector () ###
>#### Method Overview ####
>No description provided
>
### public String getId () ###
>#### Method Overview ####
>No description provided
>
### public abstract boolean find (String, InsnList, Collection) ###
>#### Method Overview ####
>Find injection points in the supplied insn list
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if one or more injection points were found
>
>### Parameters ###
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method descriptor, supplied to allow return types and
      arguments etc. to be determined
>
>**insns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Insn list to search in, the strategy MUST ONLY add nodes
      from this list to the {@code nodes} collection
>
>**nodes**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Collection of nodes to populate. Injectors should NOT make
      any assumptions about the state of this collection and should only
      call the <b>add()</b> method
>
### public String toString () ###
>#### Method Overview ####
>No description provided
>
### protected static AbstractInsnNode nextNode (InsnList, AbstractInsnNode) ###
>#### Method Overview ####
>Get the insn immediately following the specified insn, or return the same
 insn if the insn is the last insn in the list
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Next insn or the same insn if last in the list
>
>### Parameters ###
>**insns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Insn list to fetch from
>
>**insn**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Insn node
>
### public static InjectionPoint and (InjectionPoint[]) ###
>#### Method Overview ####
>Returns a composite injection point which returns the intersection of
 nodes from all component injection points
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;adjusted InjectionPoint
>
>### Parameters ###
>**operands**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;injection points to perform intersection
>
### public static InjectionPoint or (InjectionPoint[]) ###
>#### Method Overview ####
>Returns a composite injection point which returns the union of nodes from
 all component injection points
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;adjusted InjectionPoint
>
>### Parameters ###
>**operands**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;injection points to perform union
>
### public static InjectionPoint after (InjectionPoint) ###
>#### Method Overview ####
>Returns an injection point which returns all insns immediately following
 insns from the supplied injection point
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;adjusted InjectionPoint
>
>### Parameters ###
>**point**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;injection points to perform shift
>
### public static InjectionPoint before (InjectionPoint) ###
>#### Method Overview ####
>Returns an injection point which returns all insns immediately prior to
 insns from the supplied injection point
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;adjusted InjectionPoint
>
>### Parameters ###
>**point**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;injection points to perform shift
>
### public static InjectionPoint shift (InjectionPoint, int) ###
>#### Method Overview ####
>Returns an injection point which returns all insns offset by the
 specified "count" from insns from the supplied injection point
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;adjusted InjectionPoint
>
>### Parameters ###
>**point**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;injection points to perform shift
>
>**count**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;amount to shift by
>
### public static List parse (IInjectionPointContext, List) ###
>#### Method Overview ####
>Parse a collection of InjectionPoints from the supplied {@link At}
 annotations
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;InjectionPoint parsed from the supplied data or null if parsing
      failed
>
>### Parameters ###
>**owner**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Data for the mixin containing the annotation, used to obtain
      the refmap, amongst other things
>
>**ats**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;{@link At} annotations to parse information from
>
### public static List parse (IMixinContext, MethodNode, AnnotationNode, List) ###
>#### Method Overview ####
>Parse a collection of InjectionPoints from the supplied {@link At}
 annotations
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;InjectionPoint parsed from the supplied data or null if parsing
      failed
>
>### Parameters ###
>**context**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Data for the mixin containing the annotation, used to
      obtain the refmap, amongst other things
>
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The annotated handler method
>
>**parent**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The parent annotation which owns this {@link At} annotation
>
>**ats**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;{@link At} annotations to parse information from
>
### public static InjectionPoint parse (IInjectionPointContext, At) ###
>#### Method Overview ####
>Parse an InjectionPoint from the supplied {@link At} annotation
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;InjectionPoint parsed from the supplied data or null if parsing
      failed
>
>### Parameters ###
>**owner**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Data for the mixin containing the annotation, used to obtain
      the refmap, amongst other things
>
>**at**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;{@link At} annotation to parse information from
>
### public static InjectionPoint parse (IMixinContext, MethodNode, AnnotationNode, At) ###
>#### Method Overview ####
>Parse an InjectionPoint from the supplied {@link At} annotation
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;InjectionPoint parsed from the supplied data or null if parsing
      failed
>
>### Parameters ###
>**context**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Data for the mixin containing the annotation, used to
      obtain the refmap, amongst other things
>
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The annotated handler method
>
>**parent**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The parent annotation which owns this {@link At} annotation
>
>**at**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;{@link At} annotation to parse information from
>
### public static InjectionPoint parse (IInjectionPointContext, AnnotationNode) ###
>#### Method Overview ####
>Parse an InjectionPoint from the supplied {@link At} annotation supplied
 as an AnnotationNode instance
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;InjectionPoint parsed from the supplied data or null if parsing
      failed
>
>### Parameters ###
>**owner**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Data for the mixin containing the annotation, used to obtain
      the refmap, amongst other things
>
>**node**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;{@link At} annotation to parse information from
>
### public static InjectionPoint parse (IMixinContext, MethodNode, AnnotationNode, AnnotationNode) ###
>#### Method Overview ####
>Parse an InjectionPoint from the supplied {@link At} annotation supplied
 as an AnnotationNode instance
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;InjectionPoint parsed from the supplied data or null if parsing
      failed
>
>### Parameters ###
>**context**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Data for the mixin containing the annotation, used to
      obtain the refmap, amongst other things
>
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The annotated handler method
>
>**parent**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The parent annotation which owns this {@link At} annotation
>
>**node**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;{@link At} annotation to parse information from
>
### public static InjectionPoint parse (IMixinContext, MethodNode, AnnotationNode, String, At.Shift, int, List, String, String, int, int, String) ###
>#### Method Overview ####
>Parse and instantiate an InjectionPoint from the supplied information.
 Returns null if an InjectionPoint could not be created.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;InjectionPoint parsed from the supplied data or null if parsing
      failed
>
>### Parameters ###
>**context**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Data for the mixin containing the annotation, used to
      obtain the refmap, amongst other things
>
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The annotated handler method
>
>**parent**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The parent annotation which owns this {@link At} annotation
>
>**at**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Injection point specifier
>
>**shift**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Shift type to apply
>
>**by**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Amount of shift to apply for the BY shift type
>
>**args**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Named parameters
>
>**target**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target for supported injection points
>
>**slice**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Slice id for injectors which support multiple slices
>
>**ordinal**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Ordinal offset for supported injection points
>
>**opcode**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Bytecode opcode for supported injection points
>
>**id**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Injection point id from annotation
>
### protected String getAtCode () ###
>#### Method Overview ####
>No description provided
>
### public static void register (Class) ###
>#### Method Overview ####
>Register an injection point class. The supplied class must be decorated
 with an {@link AtCode} annotation for registration purposes.
>
>### Parameters ###
>**type**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;injection point type to register
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)