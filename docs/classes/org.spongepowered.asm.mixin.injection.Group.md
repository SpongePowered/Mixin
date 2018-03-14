[< Back](../README.md)
# Group #
>#### Class Overview ####
>This annotation can be used on any injector callback to define a value for
 total required injections across multiple callbacks. Much like
 {@link Inject#require()} it can be used to treat an injection failure as a
 critical error, however the <tt>require</tt> value specified applies to all
 injectors in the group. This can be used, for example, for 'surrogate'
 injectors where multiple injectors exist but only one is expected to succeed.
## Methods ##
### public String name () ###
>#### Method Overview ####
>The name of the injector group, any string
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;group name
>
### public int min () ###
>#### Method Overview ####
>Minimum required injections for this injector group
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;required min value
>
### public int max () ###
>#### Method Overview ####
>Maximum allowed injections for this injector group
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;allowed max value
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)