[< Back](../README.md)
# Profiler #
>#### Class Overview ####
>Performance profiler for Mixin.
## Fields ##
### public static final int ROOT ###
>#### Field Overview ####
>Flag to indicate a root section. Root sections are always recorded at the
 root wherever they occur, but may appear under other sections in order to
 show the time share of the root section relative to the parent.
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1
>
### public static final int FINE ###
>#### Field Overview ####
>Flag to indicate a fine section. Fine sections are always recorded, but
 are only displayed in the printed output if the includeFine flag is set.
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2
>
## Constructors ##
### public Profiler () ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public void setActive (boolean) ###
>#### Method Overview ####
>Set the active state of the profiler. When activating the profiler is
 always reset.
>
>### Parameters ###
>**active**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new active state
>
### public void reset () ###
>#### Method Overview ####
>Reset all profiler state
>
### public Section get (String) ###
>#### Method Overview ####
>Get the specified profiler section
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;profiler section
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;section name
>
### public Section begin (String[]) ###
>#### Method Overview ####
>Begin a new profiler section using the specified path
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new profiler section
>
>### Parameters ###
>**path**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;path parts
>
### public Section begin (int, String[]) ###
>#### Method Overview ####
>Begin a new profiler section using the specified path and flags
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new profiler section
>
>### Parameters ###
>**flags**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;section flags
>
>**path**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;path parts
>
### public Section begin (String) ###
>#### Method Overview ####
>Begin a new profiler section using the specified name
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new profiler section
>
>### Parameters ###
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;section name
>
### public Section begin (int, String) ###
>#### Method Overview ####
>Begin a new profiler section using the specified name and flags
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new profiler section
>
>### Parameters ###
>**flags**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;section flags
>
>**name**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;section name
>
### public void mark (String) ###
>#### Method Overview ####
>Mark a new phase (time slice) for this profiler, all sections record
 their current times and then reset to zero. If no times have been
 recorded in the current phase, the phase is discarded.
>
>### Parameters ###
>**phase**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Name of the phase
>
### public Collection getSections () ###
>#### Method Overview ####
>Get all recorded profiler sections
>
### public PrettyPrinter printer (boolean, boolean) ###
>#### Method Overview ####
>Get the profiler state with all sections in a {@link PrettyPrinter}.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;PrettyPrinter with section data
>
>### Parameters ###
>**includeFine**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Include sections marked as FINE
>
>**group**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Group delegated sections with their root instead of in the
      normal alphabetical order
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)