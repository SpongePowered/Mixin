
### [![Build Status](http://ci.liteloader.com/view/Other/job/Mixin/badge/icon)](http://ci.liteloader.com/view/Other/job/Mixin/lastSuccessfulBuild/)

![Mixin Logo](docs/javadoc/resources/logo.png?raw=true)

**Mixin** is a trait/mixin framework for Java using [ASM](https://asm.ow2.io/)
and hooking into the runtime classloading process via a set of pluggable
built-in or user-provided services. Built-in services currently support Mojang's
[LegacyLauncher](https://github.com/Mojang/LegacyLauncher) system, though this
is deprecated in favour of [ModLauncher](https://github.com/cpw/modlauncher) by
cpw, which has greater extensibility and has support for Java 8 and later.

### Documentation

The main documentation for **Mixin** can be found in the
[Wiki](https://github.com/SpongePowered/Mixin/wiki).

Additional documentation for individual features and annotations can be found in
the extensive [Javadoc](http://jenkins.liteloader.com/job/Mixin/javadoc/). For
additional help use the channel [`#mixin` on the Sponge Discord Server](https://discord.gg/tBcwxz2).

### Binaries

Mixin binaries are available via [Jenkins](https://jenkins.liteloader.com/view/Other/job/Mixin/)
and are published to the following maven repositories:

* https://repo.spongepowered.org/repository/maven-public/ - SNAPHOTs and RELEASE
  builds
* https://files.minecraftforge.net/maven/ - RELEASE builds only

### Tooling

For handling obfuscation tasks, Mixin provides an [Annotation Processor](https://github.com/SpongePowered/Mixin/wiki/Using-the-Mixin-Annotation-Processor)
which works at compile time to generate obfuscation mappings for your toolchain
to apply. If using Gradle 5 or later, annotation processors are no longer
automatically loaded from `compile` configurations and must be specified
explicitly via `annotationProcessor` configurations. For this purpose, Mixin
provides "fat jar" artefacts containing all required dependencies via the
`:processor` classifier. For example if your build uses the dependency
`org.spongepowered:mixin:1.2.3` then your annotationProcessor configuration
should specify dependency `org.spongepowered:mixin:1.2.3:processor`.

If you are using Mixin in a [Minecraft Forge](https://minecraftforge.net/)
project then the [MixinGradle](https://github.com/SpongePowered/MixinGradle)
plugin can be used to simplify the configuration of the Mixin Annotation
Processor. It provides a simple syntax for configuring the Mixin AP for your
project, see the [MixinGradle README](https://github.com/SpongePowered/MixinGradle/blob/master/README.md)
for how to configure MixinGradle.  

### Integration with Eclipse

When developing using **Mixin**, you can use the **Mixin Annotation Processor**
within Eclipse to provide context-sensitive errors and warnings to help you more
easily troubleshoot your mixins. To do so:

1. Run the `gradle build` command to generate the mixin jar
2. Open the properties of your eclipse project and navigate to `Java Compiler`
  -> `Annotation Processing` -> `Factory Path`  
3. Check the `Enable project specific settings` checkbox
4. Click the `Add External JARs` button and select the generated mixin jar with
 the suffix **-processor** (hint: it should be in `Mixin/build/libs`)
5. Navigate up one level to `Java Compiler` -> `Annotation Processing`
6. Check the `Enable project specific settings` checkbox
7. Check the `Enable annotation processing` checkbox
8. Click the `New...` button next to the `Processor options` box
 * Set `Key` to **reobfSrgFile**
 * Set `Value` to the fully-qualified path to the `mcp-srg.srg` file (the
   location of the mapping file varies by platform, if you are unsure where to
   find it please follow the discord link below). 
9. Click `OK` to apply the changes

### Integration with IntelliJ IDEA

Enhanced functionality for working with **Mixin** in IntelliJ IDEA is available
via the [Minecraft Development for IntelliJ IDEA](https://plugins.jetbrains.com/idea/plugin/8327)
plugin developed by [DemonWav](https://github.com/demonwav).  

### Version History

[Specifying the `minVersion` property in your configurations](https://github.com/SpongePowered/Mixin/wiki/PSA-Forward-Compatibility-Features-in-Mixin)
is extrememly important. The following version history can be used to determine
when features were introduced (and sometimes when major bugs are squashed) in
order to help you determine which `minVersion` you should specify.

<table width="100%">
  <thead>
    <tr>
      <th width="15%">Version</th>
      <th width="20%">Date</th>
      <th width="65%">Features / Changes</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td valign="top"><b>0.8.3</b></td>
      <td valign="top">February 2021</td>
      <td valign="top">
        <ul>
          <li>Added dynamic target selector support and <tt>&#64;Desc</tt>
            target selector</li>
          <li>Added pattern target selector</li>
          <li>Added more expressive quantifier support to explicit target
            selectors</li>
          <li>Facelift and overall improvements to javadoc</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b><s>0.8.1</s> (0.8.2)</b></td>
      <td valign="top">September 2020</td>
      <td valign="top">
        <ul>
          <li><b>Hotfix for supporting ModLauncher 7.0</b></li>
          <li>Fix critical issue with resolving obfuscated members in inherited
            interfaces</li>
          <li><b>Updated to ASM 7.2</b></li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.8</b></td>
      <td valign="top">January 2020</td>
      <td valign="top">
        <ul>
          <li><b>Updated to ASM 6.2</b></li>
          <li><b>Support for <a href="https://github.com/cpw/modlauncher/">
            ModLauncher</a></b></li>
          <li>Added recognition for Java 9 and 10</li>
          <li>Support for <em>ForgeGradle 3+</em> tsrg obfuscation tables
          <li>Configs can now inherit from other configs</li>
          <li><tt>&#064;Invoke</tt> can now be used to expose constructors</li>
          <li>Dramatically improved context reporting of unexpected applicator
            and preprocessor exceptions, making it easier to diagnose when an
            agent chokes on a specific opcode</li>
          <li>Bug fixes for
            <ul>
              <li>Calling members of accessor mixins from inside mixin code</li>
              <li>Incorrect handling of spaces in explicit target declarations</li>
              <li>Unexpected behaviour when attempting to redirect a ctor</li>
              <li>Properly detect incompatible accessor overlap and ignore valid
                ones (don't warn)</li>
              <li>Interface static accessors now correctly conform target if
                interface is classloaded before target class</li>
              <li>Staticness mismatch for accessor correctly detected and
                reported instead of causing crash</li>
              <li>Fixed generator and injector errors relating to double-word
                operands on the stack needing DUP2</li>
              <li>Fixed issue in LVT generator folded in from FabricMC</li>
              <li>Fail-fast when a <tt>final</tt> method is accidentally hidden
                by a mixin</li>  
              <li>Fix the appearance of stray <tt>CallbackInfo</tt> instances in
                local capture injector LVTs</li>
            </ul>
          </li>
          <li>Apache Commons-IO Dependency removed</li>
          <li>Renamed shaded ASM removed</li>
          <li>Improved resolution of local variables for local variable capture
            injections</li>
          <li><tt>&#064;Coerce</tt> on callback injectors and redirects can now
            resolve super interfaces including mixed-in interfaces</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.7.11</b></td>
      <td valign="top">July 2018</td>
      <td valign="top">
        <ul>
          <li>Fixes for 3 minor bugs: handling of maxShiftBy fixed, improved
            BeforeInvoke permissive search, disable generic signature merging
            unless decompiler is active.</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.7.10</b></td>
      <td valign="top">June 2018</td>
      <td valign="top">
        <ul>
          <li>Log an error when a mixin class is subject to classloader restrictions</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.7.9</b></td>
      <td valign="top">April 2018</td>
      <td valign="top">
        <ul>
          <li>Allow certain injectors to target mixin methods.</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.7.8</b></td>
      <td valign="top">April 2018</td>
      <td valign="top">
        <ul>
          <li>Bug fixes for member declaration validation, non-wild ctor redirects,
            and internal errors in <tt>Args</tt> subclass generator</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.7.7</b></td>
      <td valign="top">March 2018</td>
      <td valign="top">
        <ul>
          <li>Fixes for handling of log message triggers for INIT phase and error
            when running with unexpected logger configurations</li>
          <li>Add warnings for invalid slice points and narrowing conversion in
            ModifyConstant handlers</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.7.6</b></td>
      <td valign="top">November 2017</td>
      <td valign="top">
        <ul>
          <li>Fix inheritance for string system properties</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.7.5</b></td>
      <td valign="top">October 2017</td>
      <td valign="top">
        <ul>
          <li>Add support for <tt>&#064;Coerce</tt> on redirect injectors.</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.7.4</b></td>
      <td valign="top">September 2017</td>
      <td valign="top">
        <ul>
          <li>Added <tt>&#064;Dynamic</tt> annotation for decorating mixin
          elements with dynamically-injected targets.</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.7.3</b></td>
      <td valign="top">August 2017</td>
      <td valign="top">
        <ul>
          <li>Internal changes to provide for support modlauncher and java 9</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.7.2</b></td>
      <td valign="top">August 2017</td>
      <td valign="top">
        <ul>
          <li>Add profiler for inspecting mixin performance.</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.7.1</b></td>
      <td valign="top">August 2017</td>
      <td valign="top">
        <ul>
          <li>Fixes and improvements to the Mixin AP, fixing handling of multi-
          dimensional arrays and resolving methods in superclasses of derived
          types of obfuscated classes</li>
          <li>Add runtime refmap remapping to support using deobfCompile
          dependencies with different mapping versions.</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.7</b></td>
      <td valign="top">July 2017</td>
      <td valign="top">
        <ul>
          <li>All official binaries are now signed</li>
          <li>Upgrade to ASM 5.2</li>
          <li>Add support for inner classes in Mixins</li>
          <li>Injectors can now have multiple explicit targets</li>
          <li><tt>&#064;At</tt> annotations can now have their own <tt>id</tt></li>
          <li>Add support for using <tt>&#064;Overwrite</tt> on non-obfuscated
          methods as a way of verifying that an overwrite target exists</li>
          <li>Improve support for synthetic bridges, detect conflicting bridge
          methods</li>
          <li>Detect and warn of excessive At.Shift.BY values</li>
          <li><tt>ModifyConstant</tt> can now support multiple slices</li>
          <li>Add <tt>allow</tt> to injectors to detect over-injection</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.6.15</b></td>
      <td valign="top">July 2017</td>
      <td valign="top">
        <ul>
          <li>Add support for multiple constants in <tt>ModifyConstant</tt></li>
          <li>Add <tt>CONSTANT</tt> as general-purpose injection point</li>
          <li>Add support for redirecting array length access in field
          redirectors</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.6.14</b></td>
      <td valign="top">July 2017</td>
      <td valign="top">
        <ul>
          <li>Add support for using <tt>&#064;Coerce</tt> on reference types in
          Callback Injectors to support derived types.</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.6.13</b></td>
      <td valign="top">July 2017</td>
      <td valign="top">
        <ul>
          <li>Add support for conforming visibility of overwrite methods to
          match target class. Fixes issues where a target class method has been
          modified by an <em>Access Transformer</em> to have higher visibility
          </li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.6.12</b></td>
      <td valign="top">June 2017</td>
      <td valign="top">
        <ul>
          <li>Add <tt>slice</tt> argument to <tt>&#064;ModifyConstant</tt></li>
          <li>Add <tt>&#064;ModifyArgs</tt> injector which can change multiple
          method call arguments with a single handler.</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.6.11</b></td>
      <td valign="top">June 2017</td>
      <td valign="top">
        <ul>
          <li>Fix handling of <tt>&#064;Unique</tt> when the same unique method
          exists in more than one mixin targetting the same class</li>
          <li>Fix handling of merged lambdas so that lambdas from mixins are
          applied correctly when lambdas already exist in the target class (both
          in the original class and when applied by earlier mixins)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.6.10</b></td>
      <td valign="top">May 2017</td>
      <td valign="top">
        <ul>
          <li>(0.6.9) Minor fix to remove dependence on deprecated helper</li>
          <li>Respect <tt>remap</tt> on Mixin for contained <tt>&#064;At</tt></li>
          <li>Require redirectors which occur before call to superctor to be static</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.6.8</b></td>
      <td valign="top">February 2017</td>
      <td valign="top">
        <ul>
          <li>Allow &#064;ModifyConstant to hook implicit zero in comparisons</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.6.7</b></td>
      <td valign="top">January 2017</td>
      <td valign="top">
        <ul>
          <li>Add support for &#064;Redirect on array access</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.6.6</b></td>
      <td valign="top">January 2017</td>
      <td valign="top">
        <ul>
          <li>Allow static methods in accessor mixins in Java 8 and above</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.6.5</b></td>
      <td valign="top">January 2017</td>
      <td valign="top">
        <ul>
          <li>Add support for injector slices</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.6.4</b></td>
      <td valign="top">January 2017</td>
      <td valign="top">
        <ul>
          <li>Allow descriptors on NEW injection points</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.6.3</b></td>
      <td valign="top">December 2016</td>
      <td valign="top">
        <ul>
          <li>SourceDebugExtension support</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.6.2</b></td>
      <td valign="top">December 2016</td>
      <td valign="top">
        <ul>
          <li>Add support for &#064;Pseudo (virtual target) mixins</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.6.1</b></td>
      <td valign="top">November 2016</td>
      <td valign="top">
        <ul>
          <li>Process soft-implements annotations in the AP</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.6</b></td>
      <td valign="top">October 2016</td>
      <td valign="top">
        <ul>
          <li><em>Accessor Mixin</em> support</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.17</b></td>
      <td valign="top">October 2016</td>
      <td valign="top">
        <ul>
          <li>Allow &#064;Redirect injectors to target <tt>NEW</tt> opcodes for
          constructor redirection</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.16</b></td>
      <td valign="top">October 2016</td>
      <td valign="top">
        <ul>
          <li>Annotation Processor improvements. Support shadows and overrides
          in multi-target mixins</li>
          <li>Support pluggable obfuscation environments in AP</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.14</b></td>
      <td valign="top">September 2016</td>
      <td valign="top">
        <ul>
          <li>Add async decompilation support</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.13</b></td>
      <td valign="top">September 2016</td>
      <td valign="top">
        <ul>
          <li>Add alternative strategy for injecting field initialisers</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.10</b></td>
      <td valign="top">June 2016</td>
      <td valign="top">
        <ul>
          <li>Support &#064;Unique on fields</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.9</b></td>
      <td valign="top">June 2016</td>
      <td valign="top">
        <ul>
          <li>Hard fail if a required mixin target was already transformed</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.8</b></td>
      <td valign="top">June 2016</td>
      <td valign="top">
        <ul>
          <li>Support constraints on injectors</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.7</b></td>
      <td valign="top">June 2016</td>
      <td valign="top">
        <ul>
          <li>Add &#064;Unique annotation</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.6</b></td>
      <td valign="top">May 2016</td>
      <td valign="top">
        <ul>
          <li>Environment changes, support environment via agents</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.5</b></td>
      <td valign="top">April 2016</td>
      <td valign="top">
        <ul>
          <li>Add &#064;ModifyConstant injector</li>
          <li>Add &#064;Debug annotation</li>
          <li>Allow static &#064;ModifyArg handlers in instance methods</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.4</b></td>
      <td valign="top">April 2016</td>
      <td valign="top">
        <ul>
          <li>Error handlers also receive mixin prepare errors</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.3</b></td>
      <td valign="top">February 2016</td>
      <td valign="top">
        <ul>
          <li>Conform injectors</li>
          <li>Enable hotswapper automatically if agent is active</li>
          <li>Fix multiple issues with generics in Annotation Processors</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.2</b></td>
      <td valign="top">February 2016</td>
      <td valign="top">
        <ul>
          <li>Support ID on injectors</li>
          <li>Support priority for injectors</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.1</b></td>
      <td valign="top">February 2016</td>
      <td valign="top">
        <ul>
          <li>Overhaul injectors, injectors from all mixins now scan before any
          injectors are actually processed. Makes injectors more deterministic.
          </li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.4.19</b></td>
      <td valign="top">February 2016</td>
      <td valign="top">
        <ul>
          <li>Add support for &#064;Redirect on fields as well as methods</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.4.18</b></td>
      <td valign="top">February 2016</td>
      <td valign="top">
        <ul>
          <li>Add &#064;ModifyLocal injector</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.4.17</b></td>
      <td valign="top">January 2016</td>
      <td valign="top">
        <ul>
          <li>Support ExtraSRGs in Annotation Processor</li>
          <li>Include constructors in reference map</li>
          <li>Add &#064;Mutable annotation to suppress &#064;Final warnings</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.4.15</b></td>
      <td valign="top">January 2016</td>
      <td valign="top">
        <ul>
          <li>Include soft targets in refmap</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.4.14</b></td>
      <td valign="top">January 2016</td>
      <td valign="top">
        <ul>
          <li>Add support for interface mixins</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.4.13</b></td>
      <td valign="top">January 2016</td>
      <td valign="top">
        <ul>
          <li>Add &#064;Final annotation</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.4.11</b></td>
      <td valign="top">January 2016</td>
      <td valign="top">
        <ul>
          <li>Add support for injector grouping and config-wide require value</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.4.10</b></td>
      <td valign="top">December 2015</td>
      <td valign="top">
        <ul>
          <li>Runtime remapping support using RemapperChain</li>
          <li>Ignore class transformers decorated with &#064;Resource</li>
          <li>Support &#064;reason and &#064;author validation on overwrites</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.4.8</b></td>
      <td valign="top">December 2015</td>
      <td valign="top">
        <ul>
          <li>Annotation Processor improved to support
            <a href="https://github.com/SpongePowered/MixinGradle">
            MixinGradle</a>
          </li>
          <li>Support multiple target obfuscation environments in refmaps</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.4.6</b></td>
      <td valign="top">September 2015</td>
      <td valign="top">
        <ul>
          <li>Add INIT phase for handling early FML startup</li>
          <li>Add support for lambdas in mixins</li>
          <li>Add support for hot code replacement in mixins</li>
          <li>Improve Java 8 feature support</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.4.4</b></td>
      <td valign="top">July 2015</td>
      <td valign="top">
        <ul>
          <li>Add constraints for overwrites</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.4.3</b></td>
      <td valign="top">May 2015</td>
      <td valign="top">
        <ul>
          <li>Add <tt>INVOKE_ASSIGN</tt> injection point</li>
          <li>Support injector callbacks without args</li>
          <li>Support coercion of covariant parameter types in callbacks</li>
          <li>Support truncating local-capturing injector handlers</li>
          <li>Runtime decompilation of exported classes using fernflower</li>
          <li>Add export filter</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.4</b></td>
      <td valign="top">May 2015</td>
      <td valign="top">
        <ul>
          <li>Shade relocated ASM package and use throughout</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.3.2</b></td>
      <td valign="top">April 2015</td>
      <td valign="top">
        <ul>
          <li>Error handler support</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.3.1</b></td>
      <td valign="top">April 2015</td>
      <td valign="top">
        <ul>
          <li>Annotation Merging</li>
          <li>Allow Overwrite methods to be aliased</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.3</b></td>
      <td valign="top">March 2015</td>
      <td valign="top">
        <ul>
          <li>Implemented Environments</li>
          <li>Intrinsic method support</li>
          <li>Enabled local variable capture</li>
          <li>Alias support</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.2</b></td>
      <td valign="top">March 2015</td>
      <td valign="top">
        <ul>
          <li>Added supermixin support (mixins inheriting from other mixins)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><b>0.1</b></td>
      <td valign="top">January 2015</td>
      <td valign="top">
        <ul>
          <li>Basic Mixin Support</li>
          <li>Basic Injector Support</li>
          <li>Annotation Processor</li>
        </ul>
      </td>
    </tr>
  </tbody>
</table>
