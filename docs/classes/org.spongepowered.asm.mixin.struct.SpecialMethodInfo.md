[< Back](../README.md)
# public abstract SpecialMethodInfo SpecialMethodInfo #
>#### Class Overview ####
>Information about a special mixin method such as an injector or accessor
## Fields ##
### protected final AnnotationNode annotation ###
>#### Field Overview ####
>Annotation on the method
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final ClassNode classNode ###
>#### Field Overview ####
>Class
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final MethodNode method ###
>#### Field Overview ####
>Annotated method
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final MixinTargetContext mixin ###
>#### Field Overview ####
>Mixin data
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Constructors ##
### public SpecialMethodInfo (MixinTargetContext, MethodNode, AnnotationNode) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public final IMixinContext getContext () ###
>#### Method Overview ####
>Get the mixin target context for this injection
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the target context
>
### public final AnnotationNode getAnnotation () ###
>#### Method Overview ####
>Get the annotation which this InjectionInfo was created from
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The annotation which this InjectionInfo was created from
>
### public final ClassNode getClassNode () ###
>#### Method Overview ####
>Get the class node for this injection
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the class containing the injector and the target
>
### public final MethodNode getMethod () ###
>#### Method Overview ####
>Get method being called
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;injector method
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)