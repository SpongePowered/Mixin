[< Back](../README.md)
# InjectionInfo #
>#### Class Overview ####
>Contructs information about an injection from an {@link Inject} annotation
 and allows the injection to be processed.
## Fields ##
### protected final boolean isStatic ###
>#### Field Overview ####
>Annotated method is static
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final Deque targets ###
>#### Field Overview ####
>Target method(s)
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final MethodSlices slices ###
>#### Field Overview ####
>Method slice descriptors parsed from the annotation
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final String atKey ###
>#### Field Overview ####
>The key into the annotation which contains the injection points
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final List injectionPoints ###
>#### Field Overview ####
>Injection points parsed from
 {@link org.spongepowered.asm.mixin.injection.At} annotations
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final Map targetNodes ###
>#### Field Overview ####
>Map of lists of nodes enumerated by calling {@link #prepare}
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected Injector injector ###
>#### Field Overview ####
>Bytecode injector
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected InjectorGroupInfo group ###
>#### Field Overview ####
>Injection group
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Constructors ##
### protected InjectionInfo (MixinTargetContext, MethodNode, AnnotationNode) ###
>#### Constructor Overview ####
>ctor
>
>### Parameters ###
>**mixin**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Mixin data
>
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Injector method
>
>**annotation**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Annotation to parse
>
### protected InjectionInfo (MixinTargetContext, MethodNode, AnnotationNode, String) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### protected void readAnnotation () ###
>#### Method Overview ####
>Parse the info from the supplied annotation
>
### protected Set parseTargets (String) ###
>#### Method Overview ####
>No description provided
>
### protected List readInjectionPoints (String) ###
>#### Method Overview ####
>No description provided
>
### protected void parseInjectionPoints (List) ###
>#### Method Overview ####
>No description provided
>
### protected void parseRequirements () ###
>#### Method Overview ####
>No description provided
>
### protected abstract Injector parseInjector (AnnotationNode) ###
>#### Method Overview ####
>No description provided
>
### public boolean isValid () ###
>#### Method Overview ####
>Get whether there is enough valid information in this info to actually
 perform an injection.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true if this InjectionInfo was successfully parsed
>
### public void prepare () ###
>#### Method Overview ####
>Discover injection points
>
### public void inject () ###
>#### Method Overview ####
>Perform injections
>
### public void postInject () ###
>#### Method Overview ####
>Perform cleanup and post-injection tasks
>
### public void notifyInjected (Target) ###
>#### Method Overview ####
>Callback from injector which notifies us that a callback was injected. No
 longer used.
>
>### Parameters ###
>**target**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;target into which the injector injected
>
### protected String getDescription () ###
>#### Method Overview ####
>No description provided
>
### public String toString () ###
>#### Method Overview ####
>No description provided
>
### public Collection getTargets () ###
>#### Method Overview ####
>Get methods being injected into
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;methods being injected into
>
### public MethodSlice getSlice (String) ###
>#### Method Overview ####
>Get the slice descriptors
>
### public String getSliceId (String) ###
>#### Method Overview ####
>Return the mapped slice id for the specified ID. Injectors which only
 support use of a single slice will always return the default id (an empty
 string)
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mapped id
>
>### Parameters ###
>**id**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;slice id
>
### public int getInjectedCallbackCount () ###
>#### Method Overview ####
>Get the injected callback count
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the injected callback count
>
### public MethodNode addMethod (int, String, String) ###
>#### Method Overview ####
>Inject a method into the target class
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new method
>
>### Parameters ###
>**access**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method access flags, synthetic will be automatically added
>
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method name
>
>**desc**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Method descriptor
>
### public void addCallbackInvocation (MethodNode) ###
>#### Method Overview ####
>Notify method, called by injector when adding a callback into a target
>
>### Parameters ###
>**handler**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;callback handler being invoked
>
### protected String getDynamicInfo () ###
>#### Method Overview ####
>Get info from a decorating {@link Dynamic} annotation. If the annotation
 is present, a descriptive string suitable for inclusion in an error
 message is returned. If the annotation is not present then an empty
 string is returned.
>
### public static InjectionInfo parse (MixinTargetContext, MethodNode) ###
>#### Method Overview ####
>Parse an injector from the specified method (if an injector annotation is
 present). If no injector annotation is present then <tt>null</tt> is
 returned.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;parsed InjectionInfo or null
>
>### Parameters ###
>**mixin**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;context
>
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mixin method
>
### public static AnnotationNode getInjectorAnnotation (IMixinInfo, MethodNode) ###
>#### Method Overview ####
>Returns any injector annotation found on the specified method. If
 multiple matching annotations are found then an exception is thrown. If
 no annotations are present then <tt>null</tt> is returned.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;annotation or null
>
>### Parameters ###
>**mixin**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;context
>
>**method**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mixin method
>
### public static String getInjectorPrefix (AnnotationNode) ###
>#### Method Overview ####
>Get the conform prefix for an injector handler by type
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;conform prefix
>
>### Parameters ###
>**annotation**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Annotation to inspect
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)