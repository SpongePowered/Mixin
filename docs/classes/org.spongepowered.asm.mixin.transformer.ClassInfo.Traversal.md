[< Back](../README.md)
# ClassInfo.Traversal #
>#### Class Overview ####
><p>To all intents and purposes, the "real" class hierarchy and the mixin
 class hierarchy exist in parallel, this means that for some hierarchy
 validation operations we need to walk <em>across</em> to the other
 hierarchy in order to allow meaningful validation to occur.</p>

 <p>This enum defines the type of traversal operations which are allowed
 for a particular lookup.</p>

 <p>Each traversal type has a <code>next</code> property which defines
 the traversal type to use on the <em>next</em> step of the hierarchy
 validation. For example, the type {@link #IMMEDIATE} which requires an
 immediate match falls through to {@link #NONE} on the next step, which
 prevents further traversals from occurring in the lookup.</p>
## Fields ##
### public static final Traversal NONE ###
>#### Field Overview ####
>No traversals are allowed.
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Traversal ALL ###
>#### Field Overview ####
>Traversal is allowed at all stages.
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Traversal IMMEDIATE ###
>#### Field Overview ####
>Traversal is allowed at the bottom of the hierarchy but no further.
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Traversal SUPER ###
>#### Field Overview ####
>Traversal is allowed only on superclasses and not at the bottom of
 the hierarchy.
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Methods ##
### public static Traversal values () ###
>#### Method Overview ####
>No description provided
>
### public static Traversal valueOf (String) ###
>#### Method Overview ####
>No description provided
>
### public Traversal next () ###
>#### Method Overview ####
>Return the next traversal type for this traversal type
>
### public boolean canTraverse () ###
>#### Method Overview ####
>Return whether this traversal type allows traversal
>
### public SearchType getSearchType () ###
>#### Method Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)