### [![Build Status](https://travis-ci.org/SpongePowered/Mixin.svg?branch=master)](https://travis-ci.org/SpongePowered/Mixin)

![Mixin Logo](docs/logo.png?raw=true)

**Mixin** is a trait/mixin framework for Java using [ASM](http://asm.ow2.org/)
and hooking into the runtime class-loading process via Mojang's
[LegacyLauncher](https://github.com/Mojang/LegacyLauncher) system. The main
documentation for **Mixin** can be found in the [Wiki](../../wiki). Additional
documentation for individual features and annotations can be found in the
extensive [Javadoc](http://jenkins.liteloader.com/job/Mixin/javadoc/).

### Building Mixin
**Mixin** uses the [Gradle](http://gradle.org/) build automation system. To
perform a build simply execute

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

### Version History

<table>
  <thead>
    <tr>
      <th>Version</th>
      <th>Features / Changes</th>
      <th>Date</th>
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
  </tbody>
</table>
