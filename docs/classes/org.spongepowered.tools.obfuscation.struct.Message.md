[< Back](../README.md)
# public Message Message #
>#### Class Overview ####
>Wrapper for Annotation Processor messages, used to enable messages to be
 easily queued and manipulated
## Constructors ##
### public Message (Diagnostic.Kind, CharSequence) ###
>#### Constructor Overview ####
>No description provided
>
### public Message (Diagnostic.Kind, CharSequence, Element) ###
>#### Constructor Overview ####
>No description provided
>
### public Message (Diagnostic.Kind, CharSequence, Element, AnnotationHandle) ###
>#### Constructor Overview ####
>No description provided
>
### public Message (Diagnostic.Kind, CharSequence, Element, AnnotationMirror) ###
>#### Constructor Overview ####
>No description provided
>
### public Message (Diagnostic.Kind, CharSequence, Element, AnnotationHandle, AnnotationValue) ###
>#### Constructor Overview ####
>No description provided
>
### public Message (Diagnostic.Kind, CharSequence, Element, AnnotationMirror, AnnotationValue) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public Message sendTo (Messager) ###
>#### Method Overview ####
>Send this message to the supplied message
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**messager**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;messager to send to
>
### public Kind getKind () ###
>#### Method Overview ####
>Get the message kind
>
### public Message setKind (Diagnostic.Kind) ###
>#### Method Overview ####
>Set the message kind
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**kind**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;message kind
>
### public CharSequence getMsg () ###
>#### Method Overview ####
>Get the message text
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
### public Message setMsg (CharSequence) ###
>#### Method Overview ####
>Set the message text
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**msg**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;message text
>
### public Element getElement () ###
>#### Method Overview ####
>Get the target element
>
### public AnnotationMirror getAnnotation () ###
>#### Method Overview ####
>Get the target annotation
>
### public AnnotationValue getValue () ###
>#### Method Overview ####
>Get the target annotation value
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)