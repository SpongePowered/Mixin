[< Back](../README.md)
# MixinValidator #
>#### Class Overview ####
>Base class for mixin validators
## Fields ##
### protected final ProcessingEnvironment processingEnv ###
>#### Field Overview ####
>Processing environment for this validator, used for raising compiler
 errors and warnings
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final Messager messager ###
>#### Field Overview ####
>Messager to use to output errors and warnings
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final IOptionProvider options ###
>#### Field Overview ####
>Option provider
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected final ValidationPass pass ###
>#### Field Overview ####
>Pass to run this validator in
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Constructors ##
### public MixinValidator (IMixinAnnotationProcessor, IMixinValidator.ValidationPass) ###
>#### Constructor Overview ####
>ctor
>
>### Parameters ###
>**ap**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Processing environment
>
>**pass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Validation pass being performed
>
## Methods ##
### public final boolean validate (IMixinValidator.ValidationPass, TypeElement, AnnotationHandle, Collection) ###
>#### Method Overview ####
>No description provided
>
### protected abstract boolean validate (TypeElement, AnnotationHandle, Collection) ###
>#### Method Overview ####
>No description provided
>
### protected final void note (String, Element) ###
>#### Method Overview ####
>Output a compiler note
>
>### Parameters ###
>**note**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Message
>
>**element**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Element to attach the note to
>
### protected final void warning (String, Element) ###
>#### Method Overview ####
>Output a compiler note
>
>### Parameters ###
>**warning**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Message
>
>**element**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Element to attach the warning to
>
### protected final void error (String, Element) ###
>#### Method Overview ####
>Output a compiler note
>
>### Parameters ###
>**error**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Message
>
>**element**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Element to attach the error to
>
### protected final Collection getMixinsTargeting (TypeMirror) ###
>#### Method Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)