[< Back](../README.md)
# public final ConstraintParser ConstraintParser #
>#### Class Overview ####
>Parser for constraints
## Methods ##
### public static Constraint parse (String) ###
>#### Method Overview ####
>Parse the supplied expression as a constraint and returns a new
 Constraint. Returns {@link Constraint#NONE} if the constraint could not
 be parsed or is empty.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;parsed constraint
>
>### Parameters ###
>**expr**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;constraint expression to parse
>
### public static Constraint parse (AnnotationNode) ###
>#### Method Overview ####
>Parse a constraint expression on the supplied annotation as a constraint
 and returns a new Constraint. Returns {@link Constraint#NONE} if the
 constraint could not be parsed or is empty.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;parsed constraint
>
>### Parameters ###
>**annotation**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;annotation containing the constraint expression to
      parse
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)