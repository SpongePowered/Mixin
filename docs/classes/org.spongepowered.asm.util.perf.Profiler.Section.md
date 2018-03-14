[< Back](../README.md)
# public Section Profiler.Section #
>#### Class Overview ####
>Profiler section. Normal sections do nothing so that the profiler itself
 consumes minimal resources when disabled.
## Fields ##
### protected boolean invalidated ###
>#### Field Overview ####
>True if this section has been invalidated by a call to Profiler#clear
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Methods ##
### public boolean isRoot () ###
>#### Method Overview ####
>Get whether this is a root section
>
### public boolean isFine () ###
>#### Method Overview ####
>Get whether this section is FINE
>
### public String getName () ###
>#### Method Overview ####
>Get the section name
>
### public String getBaseName () ###
>#### Method Overview ####
>Get the base name for this section, for delegated sections this is
 the name of the parent section, minus the root
>
### public void setInfo (String) ###
>#### Method Overview ####
>Set the auxilliary info for this section
>
>### Parameters ###
>**info**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;aux info
>
### public String getInfo () ###
>#### Method Overview ####
>Get the auxilliary info for this section
>
### protected Section stop () ###
>#### Method Overview ####
>Stop timing of this section
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent
>
### public Section end () ###
>#### Method Overview ####
>Stop timing of this section and end it (pop from profiler stack)
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent
>
### public Section next (String) ###
>#### Method Overview ####
>Stop timing of this section and start a new section at the same level
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new section
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;name of the next section
>
### public long getTime () ###
>#### Method Overview ####
>Get the current time in milliseconds in the current phase
>
### public long getTotalTime () ###
>#### Method Overview ####
>Get the current time in milliseconds in all phases
>
### public double getSeconds () ###
>#### Method Overview ####
>Get the current time in seconds in the current phase
>
### public double getTotalSeconds () ###
>#### Method Overview ####
>Get the current time in seconds in all phases
>
### public long getTimes () ###
>#### Method Overview ####
>Get all available time slices including the current one in
 milliseconds
>
### public int getCount () ###
>#### Method Overview ####
>Get the number of total time periods recorded in the current slice
>
### public int getTotalCount () ###
>#### Method Overview ####
>Get the number of total time periods recorded in the all slices
>
### public double getAverageTime () ###
>#### Method Overview ####
>Get the average time in milliseconds of each time period recorded in
 the current slice
>
### public double getTotalAverageTime () ###
>#### Method Overview ####
>Get the average time in milliseconds of each time period recorded in
 the all slices
>
### public final String toString () ###
>#### Method Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)