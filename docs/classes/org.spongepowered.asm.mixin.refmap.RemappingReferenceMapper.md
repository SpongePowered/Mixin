[< Back](../README.md)
# public final RemappingReferenceMapper RemappingReferenceMapper #
>#### Class Overview ####
>This adapter is designed to address a problem with mixins when "deobfCompile"
 dependencies are used in a project which is using newer MCP mappings than the
 ones the imported dependency was compiled with.
 
 <p>Before now, refMaps in deobfCompile dependencies had to be disabled
 because the obfuscated mappings were no use in a development environment.
 However there existed no "mcp-to-different-version-of-mcp" mappings to use
 instead.</p>
 
 <p>This class leverages the fact that mappings are provided into the
 environment by GradleStart and consumes SRG mappings in the imported refMaps
 and converts them on-the-fly using srg-to-mcp mappings. This allows refMaps
 to be normalised to the current MCP environment.</p>
 
 <p>Note that this class takes a naïve approach to remapping on the basis that
 searge names are unique and can thus be remapped with a straightforward dumb
 string replacement. Whilst the input environment and mappings are
 customisable via the appropriate environment vars, this fact should be taken
 into account if a different mapping environment is to be used.</p>
 
 <p>All lookups are straightforward string replacements using <em>all</em>
 values in the map, this basically means this is probably pretty slow, but I
 don't care because the number of mappings processed is usually pretty small
 (few hundred at most) and this is only used in dev where we don't actually
 care about speed. Some performance is gained (approx 10ms per lookup) by
 caching the transformed descriptors.</p>
## Methods ##
### public boolean isDefault () ###
>#### Method Overview ####
>No description provided
>
### public String getResourceName () ###
>#### Method Overview ####
>No description provided
>
### public String getStatus () ###
>#### Method Overview ####
>No description provided
>
### public String getContext () ###
>#### Method Overview ####
>No description provided
>
### public void setContext (String) ###
>#### Method Overview ####
>No description provided
>
### public String remap (String, String) ###
>#### Method Overview ####
>No description provided
>
### public String remapWithContext (String, String, String) ###
>#### Method Overview ####
>No description provided
>
### public static IReferenceMapper of (MixinEnvironment, IReferenceMapper) ###
>#### Method Overview ####
>Wrap the specified refmap in a remapping adapter using settings in the
 supplied environment
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;wrapped refmap or original refmap is srg data is not available
>
>### Parameters ###
>**env**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;environment to read configuration from
>
>**refMap**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;refmap to wrap
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)