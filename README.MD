### [![Build Status](http://ci.liteloader.com/view/Other/job/Mixin/badge/icon)](http://ci.liteloader.com/view/Other/job/Mixin/lastSuccessfulBuild/)

![Mixin Logo](docs/logo.png?raw=true)

**Mixin** is a trait/mixin framework for Java using [ASM](http://asm.ow2.org/)
and hooking into the runtime class-loading process via Mojang's
[LegacyLauncher](https://github.com/Mojang/LegacyLauncher) system. The main
documentation for **Mixin** can be found in the [Wiki](../../wiki). Additional
documentation for individual features and annotations can be found in the
extensive [Javadoc](http://jenkins.liteloader.com/job/Mixin/javadoc/). For
additional help use the channel `#spongedev` on the Espernet IRC network.

### Building Mixin
**Mixin** uses the [Gradle](http://gradle.org/) build automation system. To
perform a build, use gradle version 2.11 and execute

    gradle

from within the project root directory. *(If you do not have Gradle installed on
your system you can instead run the supplied Gradle wrapper `gradlew`)*

### Integration with Eclipse IDE

The **Mixin** project can be integrated easily with Eclipse using the [Gradle
Plugin](http://marketplace.eclipse.org/content/gradle-integration-eclipse-44).
Simply choose `Import` -> `Gradle Project` and follow the prompts.

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
 * Set `Value` to the fully-qualified path to the `mcp-srg.srg` file (for Sponge
 this can be found in `Sponge/build/srgs/mcp-srg.srg`)
9. Click `OK` to apply the changes

### Integration with IntelliJ IDEA IDE

Enhanced functionality for working with **Mixin** in IntelliJ IDEA is available
via the [Minecraft Development for IntelliJ IDEA](https://plugins.jetbrains.com/idea/plugin/8327)
plugin developed by [DemonWav](https://github.com/demonwav).  

### Version History

<table width="100%">
  <thead>
    <tr>
      <th width="15%">Version</th>
      <th width="65%">Features / Changes</th>
      <th width="20%">Date</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td valign="top"><b>0.1</b></td>
      <td valign="top">
        <ul>
          <li>Basic Mixin Support</li>
          <li>Basic Injector Support</li>
          <li>Annotation Processor</li>
        </ul>
      </td>
      <td valign="top">January 2015</td>
    </tr>
    <tr>
      <td valign="top"><b>0.2</b></td>
      <td valign="top">
        <ul>
          <li>Added supermixin support (mixins inheriting from other mixins)</li>
        </ul>
      </td>
      <td valign="top">March 2015</td>
    </tr>
    <tr>
      <td valign="top"><b>0.3</b></td>
      <td valign="top">
        <ul>
          <li>Implemented Environments</li>
          <li>Intrinsic method support</li>
          <li>Enabled local variable capture</li>
          <li>Alias support</li>
        </ul>
      </td>
      <td valign="top">March 2015</td>
    </tr>
    <tr>
      <td valign="top"><b>0.3.1</b></td>
      <td valign="top">
        <ul>
          <li>Annotation Merging</li>
          <li>Allow Overwrite methods to be aliased</li>
        </ul>
      </td>
      <td valign="top">April 2015</td>
    </tr>
    <tr>
      <td valign="top"><b>0.3.2</b></td>
      <td valign="top">
        <ul>
          <li>Error handler support</li>
        </ul>
      </td>
      <td valign="top">April 2015</td>
    </tr>
    <tr>
      <td valign="top"><b>0.4</b></td>
      <td valign="top">
        <ul>
          <li>Shade relocated ASM package and use throughout</li>
        </ul>
      </td>
      <td valign="top">May 2015</td>
    </tr>
    <tr>
      <td valign="top"><b>0.4.3</b></td>
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
      <td valign="top">May 2015</td>
    </tr>
    <tr>
      <td valign="top"><b>0.4.4</b></td>
      <td valign="top">
        <ul>
          <li>Add constraints for overwrites</li>
        </ul>
      </td>
      <td valign="top">July 2015</td>
    </tr>
    <tr>
      <td valign="top"><b>0.4.6</b></td>
      <td valign="top">
        <ul>
          <li>Add INIT phase for handling early FML startup</li>
          <li>Add support for lambdas in mixins</li>
          <li>Add support for hot code replacement in mixins</li>
          <li>Improve Java 8 feature support</li>
        </ul>
      </td>
      <td valign="top">September 2015</td>
    </tr>
    <tr>
      <td valign="top"><b>0.4.8</b></td>
      <td valign="top">
        <ul>
          <li>Annotation Processor improved to support
            <a href="https://github.com/SpongePowered/MixinGradle">
            MixinGradle</a>
          </li>
          <li>Support multiple target obfuscation environments in refmaps</li>
        </ul>
      </td>
      <td valign="top">December 2015</td>
    </tr>
    <tr>
      <td valign="top"><b>0.4.10</b></td>
      <td valign="top">
        <ul>
          <li>Runtime remapping support using RemapperChain</li>
          <li>Ignore class transformers decorated with &#064;Resource</li>
          <li>Support &#064;reason and &#064;author validation on overwrites</li>
        </ul>
      </td>
      <td valign="top">December 2015</td>
    </tr>
    <tr>
      <td valign="top"><b>0.4.11</b></td>
      <td valign="top">
        <ul>
          <li>Add support for injector grouping and config-wide require value</li>
        </ul>
      </td>
      <td valign="top">January 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.4.13</b></td>
      <td valign="top">
        <ul>
          <li>Add &#064;Final annotation</li>
        </ul>
      </td>
      <td valign="top">January 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.4.14</b></td>
      <td valign="top">
        <ul>
          <li>Add support for interface mixins</li>
        </ul>
      </td>
      <td valign="top">January 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.4.15</b></td>
      <td valign="top">
        <ul>
          <li>Include soft targets in refmap</li>
        </ul>
      </td>
      <td valign="top">January 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.4.17</b></td>
      <td valign="top">
        <ul>
          <li>Support ExtraSRGs in Annotation Processor</li>
          <li>Include constructors in reference map</li>
          <li>Add &#064;Mutable annotation to suppress &#064;Final warnings</li>
        </ul>
      </td>
      <td valign="top">January 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.4.18</b></td>
      <td valign="top">
        <ul>
          <li>Add &#064;ModifyLocal injector</li>
        </ul>
      </td>
      <td valign="top">February 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.4.19</b></td>
      <td valign="top">
        <ul>
          <li>Add support for &#064;Redirect on fields as well as methods</li>
        </ul>
      </td>
      <td valign="top">February 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.1</b></td>
      <td valign="top">
        <ul>
          <li>Overhaul injectors, injectors from all mixins now scan before any
          injectors are actually processed. Makes injectors more deterministic.
          </li>
        </ul>
      </td>
      <td valign="top">February 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.2</b></td>
      <td valign="top">
        <ul>
          <li>Support ID on injectors</li>
          <li>Support priority for injectors</li>
        </ul>
      </td>
      <td valign="top">February 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.3</b></td>
      <td valign="top">
        <ul>
          <li>Conform injectors</li>
          <li>Enable hotswapper automatically if agent is active</li>
          <li>Fix multiple issues with generics in Annotation Processors</li>
        </ul>
      </td>
      <td valign="top">February 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.4</b></td>
      <td valign="top">
        <ul>
          <li>Error handlers also receive mixin prepare errors</li>
        </ul>
      </td>
      <td valign="top">April 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.5</b></td>
      <td valign="top">
        <ul>
          <li>Add &#064;ModifyConstant injector</li>
          <li>Add &#064;Debug annotation</li>
          <li>Allow static &#064;ModifyArg handlers in instance methods</li>
        </ul>
      </td>
      <td valign="top">April 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.6</b></td>
      <td valign="top">
        <ul>
          <li>Environment changes, support environment via agents</li>
        </ul>
      </td>
      <td valign="top">May 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.7</b></td>
      <td valign="top">
        <ul>
          <li>Add &#064;Unique annotation</li>
        </ul>
      </td>
      <td valign="top">June 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.8</b></td>
      <td valign="top">
        <ul>
          <li>Support constraints on injectors</li>
        </ul>
      </td>
      <td valign="top">June 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.9</b></td>
      <td valign="top">
        <ul>
          <li>Hard fail if a required mixin target was already transformed</li>
        </ul>
      </td>
      <td valign="top">June 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.10</b></td>
      <td valign="top">
        <ul>
          <li>Support &#064;Unique on fields</li>
        </ul>
      </td>
      <td valign="top">June 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.13</b></td>
      <td valign="top">
        <ul>
          <li>Add alternative strategy for injecting field initialisers</li>
        </ul>
      </td>
      <td valign="top">September 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.14</b></td>
      <td valign="top">
        <ul>
          <li>Add async decompilation support</li>
        </ul>
      </td>
      <td valign="top">September 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.16</b></td>
      <td valign="top">
        <ul>
          <li>Annotation Processor improvements. Support shadows and overrides
          in multi-target mixins</li>
          <li>Support pluggable obfuscation environments in AP</li>
        </ul>
      </td>
      <td valign="top">October 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.5.17</b></td>
      <td valign="top">
        <ul>
          <li>Allow &#064;Redirect injectors to target <tt>NEW</tt> opcodes for
          constructor redirection</li>
        </ul>
      </td>
      <td valign="top">October 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.6</b></td>
      <td valign="top">
        <ul>
          <li><em>Accessor Mixin</em> support</li>
        </ul>
      </td>
      <td valign="top">October 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.6.1</b></td>
      <td valign="top">
        <ul>
          <li>Process soft-implements annotations in the AP</li>
        </ul>
      </td>
      <td valign="top">November 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.6.2</b></td>
      <td valign="top">
        <ul>
          <li>Add support for &#064;Pseudo (virtual target) mixins</li>
        </ul>
      </td>
      <td valign="top">December 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.6.3</b></td>
      <td valign="top">
        <ul>
          <li>SourceDebugExtension support</li>
        </ul>
      </td>
      <td valign="top">December 2016</td>
    </tr>
    <tr>
      <td valign="top"><b>0.6.4</b></td>
      <td valign="top">
        <ul>
          <li>Allow descriptors on NEW injection points</li>
        </ul>
      </td>
      <td valign="top">January 2017</td>
    </tr>
    <tr>
      <td valign="top"><b>0.6.5</b></td>
      <td valign="top">
        <ul>
          <li>Add support for injector slices</li>
        </ul>
      </td>
      <td valign="top">January 2017</td>
    </tr>
    <tr>
      <td valign="top"><b>0.6.6</b></td>
      <td valign="top">
        <ul>
          <li>Allow static methods in accessor mixins in Java 8 and above</li>
        </ul>
      </td>
      <td valign="top">January 2017</td>
    </tr>
    <tr>
      <td valign="top"><b>0.6.7</b></td>
      <td valign="top">
        <ul>
          <li>Add support for &#064;Redirect on array access</li>
        </ul>
      </td>
      <td valign="top">January 2017</td>
    </tr>
    <tr>
      <td valign="top"><b>0.6.8</b></td>
      <td valign="top">
        <ul>
          <li>Allow &#064;ModifyConstant to hook implicit zero in comparisons</li>
        </ul>
      </td>
      <td valign="top">February 2017</td>
    </tr>
    <tr>
      <td valign="top"><b>0.6.10</b></td>
      <td valign="top">
        <ul>
          <li>(0.6.9) Minor fix to remove dependence on deprecated helper</li>
          <li>Respect <tt>remap</tt> on Mixin for contained <tt>&#064;At</tt></li>
          <li>Require redirectors which occur before call to superctor to be static</li>
        </ul>
      </td>
      <td valign="top">May 2017</td>
    </tr>
    <tr>
      <td valign="top"><b>0.6.11</b></td>
      <td valign="top">
        <ul>
          <li>Fix handling of <tt>&#064;Unique</tt> when the same unique method
          exists in more than one mixin targetting the same class</li>
          <li>Fix handling of merged lambdas so that lambdas from mixins are
          applied correctly when lambdas already exist in the target class (both
          in the original class and when applied by earlier mixins)
        </ul>
      </td>
      <td valign="top">June 2017</td>
    </tr>
  </tbody>
</table>
