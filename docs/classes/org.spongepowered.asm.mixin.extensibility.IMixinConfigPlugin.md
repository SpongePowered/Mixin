[< Back](../README.md)
# public interface IMixinConfigPlugin IMixinConfigPlugin #
>#### Class Overview ####
><p>A companion plugin for a mixin configuration object. Objects implementing
 this interface get some limited power of veto over the mixin load process as
 well as an opportunity to apply their own transformations to the target class
 pre- and post-transform. Since all methods in this class are called
 indirectly from the transformer, the same precautions as for writing class
 transformers should be taken. Implementors should take care to not reference
 any game classes, and avoid referencing other classes in their own mod except
 those specificially designed to be available at early startup, such as
 coremod classes or other standalone bootstrap objects.</p>
 
 <p>Instances of plugins are created by specifying the "plugin" key in the
 mixin config JSON as the fully-qualified class name of a class implementing
 this interface.</p>
## Methods ##
### public void onLoad (String) ###
>#### Method Overview ####
>Called after the plugin is instantiated, do any setup here.
>
>### Parameters ###
>**mixinPackage**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The mixin root package from the config
>
### public String getRefMapperConfig () ###
>#### Method Overview ####
>Called only if the "referenceMap" key in the config is <b>not</b> set.
 This allows the refmap file name to be supplied by the plugin
 programatically if desired. Returning <code>null</code> will revert to
 the default behaviour of using the default refmap json file.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Path to the refmap resource or null to revert to the default
>
### public boolean shouldApplyMixin (String, String) ###
>#### Method Overview ####
>Called during mixin intialisation, allows this plugin to control whether
 a specific will be applied to the specified target. Returning false will
 remove the target from the mixin's target set, and if all targets are
 removed then the mixin will not be applied at all.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;True to allow the mixin to be applied, or false to remove it from
      target's mixin set
>
>### Parameters ###
>**targetClassName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Fully qualified class name of the target class
>
>**mixinClassName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Fully qualified class name of the mixin
>
### public void acceptTargets (Set, Set) ###
>#### Method Overview ####
>Called after all configurations are initialised, this allows this plugin
 to observe classes targetted by other mixin configs and optionally remove
 targets from its own set. The set myTargets is a direct view of the
 targets collection in this companion config and keys may be removed from
 this set to suppress mixins in this config which target the specified
 class. Adding keys to the set will have no effect.
>
>### Parameters ###
>**myTargets**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target class set from the companion config
>
>**otherTargets**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target class set incorporating targets from all other
      configs, read-only
>
### public List getMixins () ###
>#### Method Overview ####
>After mixins specified in the configuration have been processed, this
 method is called to allow the plugin to add any additional mixins to
 load. It should return a list of mixin class names or return null if the
 plugin does not wish to append any mixins of its own.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;additional mixins to apply
>
### public void preApply (String, ClassNode, String, IMixinInfo) ###
>#### Method Overview ####
>Called immediately <b>before</b> a mixin is applied to a target class,
 allows any pre-application transformations to be applied.
>
>### Parameters ###
>**targetClassName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Transformed name of the target class
>
>**targetClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target class tree
>
>**mixinClassName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Name of the mixin class
>
>**mixinInfo**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Information about this mixin
>
### public void postApply (String, ClassNode, String, IMixinInfo) ###
>#### Method Overview ####
>Called immediately <b>after</b> a mixin is applied to a target class,
 allows any post-application transformations to be applied.
>
>### Parameters ###
>**targetClassName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Transformed name of the target class
>
>**targetClass**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Target class tree
>
>**mixinClassName**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Name of the mixin class
>
>**mixinInfo**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Information about this mixin
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)