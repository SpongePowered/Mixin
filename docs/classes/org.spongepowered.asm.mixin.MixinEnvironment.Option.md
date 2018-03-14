[< Back](../README.md)
# MixinEnvironment.Option #
>#### Class Overview ####
>Mixin options
## Fields ##
### public static final Option DEBUG_ALL ###
>#### Field Overview ####
>Enable all debugging options
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option DEBUG_EXPORT ###
>#### Field Overview ####
>Enable post-mixin class export. This causes all classes to be written
 to the .mixin.out directory within the runtime directory
 <em>after</em> mixins are applied, for debugging purposes.
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option DEBUG_EXPORT_FILTER ###
>#### Field Overview ####
>Export filter, if omitted allows all transformed classes to be
 exported. If specified, acts as a filter for class names to export
 and only matching classes will be exported. This is useful when using
 Fernflower as exporting can be otherwise very slow. The following
 wildcards are allowed:
 
 <dl>
   <dt>*</dt><dd>Matches one or more characters except dot (.)</dd>
   <dt>**</dt><dd>Matches any number of characters</dd>
   <dt>?</dt><dd>Matches exactly one character</dd>
 </dl>
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option DEBUG_EXPORT_DECOMPILE ###
>#### Field Overview ####
>Allow fernflower to be disabled even if it is found on the classpath
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option DEBUG_EXPORT_DECOMPILE_THREADED ###
>#### Field Overview ####
>Run fernflower in a separate thread. In general this will allow
 export to impact startup time much less (decompiling normally adds
 about 20% to load times) with the trade-off that crashes may lead to
 undecompiled exports.
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option DEBUG_VERIFY ###
>#### Field Overview ####
>Run the CheckClassAdapter on all classes after mixins are applied,
 also enables stricter checks on mixins for use at dev-time, promotes
 some warning-level messages to exceptions
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option DEBUG_VERBOSE ###
>#### Field Overview ####
>Enable verbose mixin logging (elevates all DEBUG level messages to
 INFO level)
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option DEBUG_INJECTORS ###
>#### Field Overview ####
>Elevates failed injections to an error condition, see
 {@link Inject#expect} for details
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option DEBUG_STRICT ###
>#### Field Overview ####
>Enable strict checks
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option DEBUG_UNIQUE ###
>#### Field Overview ####
>If false (default), {@link Unique} public methods merely raise a
 warning when encountered and are not merged into the target. If true,
 an exception is thrown instead
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option DEBUG_TARGETS ###
>#### Field Overview ####
>Enable strict checking for mixin targets
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option DEBUG_PROFILER ###
>#### Field Overview ####
>Enable the performance profiler for all mixin operations (normally it
 is only enabled during mixin prepare operations)
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option DUMP_TARGET_ON_FAILURE ###
>#### Field Overview ####
>Dumps the bytecode for the target class to disk when mixin
 application fails
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option CHECK_ALL ###
>#### Field Overview ####
>Enable all checks
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option CHECK_IMPLEMENTS ###
>#### Field Overview ####
>Checks that all declared interface methods are implemented on a class
 after mixin application.
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option CHECK_IMPLEMENTS_STRICT ###
>#### Field Overview ####
>If interface check is enabled, "strict mode" (default) applies the
 implementation check even to abstract target classes. Setting this
 option to <tt>false</tt> causes abstract targets to be skipped when
 generating the implementation report.
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option IGNORE_CONSTRAINTS ###
>#### Field Overview ####
>Ignore all constraints on mixin annotations, output warnings instead
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option HOT_SWAP ###
>#### Field Overview ####
>Enables the hot-swap agent
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option ENVIRONMENT ###
>#### Field Overview ####
>Parent for environment settings
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option OBFUSCATION_TYPE ###
>#### Field Overview ####
>Force refmap obf type when required
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option DISABLE_REFMAP ###
>#### Field Overview ####
>Disable refmap when required
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option REFMAP_REMAP ###
>#### Field Overview ####
>Rather than disabling the refMap, you may wish to remap existing
 refMaps at runtime. This can be achieved by setting this property and
 supplying values for <tt>mixin.env.refMapRemappingFile</tt> and
 <tt>mixin.env.refMapRemappingEnv</tt>. Though those properties can be
 ignored if starting via <tt>GradleStart</tt> (this property is also
 automatically enabled if loading via GradleStart).
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option REFMAP_REMAP_RESOURCE ###
>#### Field Overview ####
>If <tt>mixin.env.remapRefMap</tt> is enabled, this setting can be
 used to override the name of the SRG file to read mappings from. The
 mappings must have a source type of <tt>searge</tt> and a target type
 matching the current development environment. If the source type is
 not <tt>searge</tt> then the <tt>mixin.env.refMapRemappingEnv</tt>
 should be set to the correct source environment type.
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option REFMAP_REMAP_SOURCE_ENV ###
>#### Field Overview ####
>When using <tt>mixin.env.refMapRemappingFile</tt>, this setting
 overrides the default source environment (searge). However note that
 the specified environment type must exist in the orignal refmap.
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option IGNORE_REQUIRED ###
>#### Field Overview ####
>Globally ignore the "required" attribute of all configurations
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option DEFAULT_COMPATIBILITY_LEVEL ###
>#### Field Overview ####
>Default compatibility level to operate at
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option SHIFT_BY_VIOLATION_BEHAVIOUR ###
>#### Field Overview ####
>Behaviour when the maximum defined {@link At#by} value is exceeded in
 a mixin. Currently the behaviour is to <tt>warn</tt>. In later
 versions of Mixin this may be promoted to <tt>error</tt>.
 
 <p>Available values for this option are:</p>
 
 <dl>
   <dt>ignore</dt>
   <dd>Pre-0.7 behaviour, no action is taken when a violation is
     encountered</dd>
   <dt>warn</dt>
   <dd>Current behaviour, a <tt>WARN</tt>-level message is raised for
     violations</dd>
   <dt>error</dt>
   <dd>Violations throw an exception</dd>
 </dl>
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### public static final Option INITIALISER_INJECTION_MODE ###
>#### Field Overview ####
>Behaviour for initialiser injections, current supported options are
 "default" and "safe"
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Methods ##
### public static Option values () ###
>#### Method Overview ####
>No description provided
>
### public static Option valueOf (String) ###
>#### Method Overview ####
>No description provided
>
### public String toString () ###
>#### Method Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)