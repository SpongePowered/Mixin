/*
 * This file is part of Mixin, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.asm.mixin.extensibility;

import java.util.List;
import java.util.Set;

import org.spongepowered.asm.lib.tree.ClassNode;

/**
 * <p>A companion plugin for a mixin configuration object. Objects implementing
 * this interface get some limited power of veto over the mixin load process as
 * well as an opportunity to apply their own transformations to the target class
 * pre- and post-transform. Since all methods in this class are called
 * indirectly from the transformer, the same precautions as for writing class
 * transformers should be taken. Implementors should take care to not reference
 * any game classes, and avoid referencing other classes in their own mod except
 * those specificially designed to be available at early startup, such as
 * coremod classes or other standalone bootstrap objects.</p>
 * 
 * <p>Instances of plugins are created by specifying the "plugin" key in the
 * mixin config JSON as the fully-qualified class name of a class implementing
 * this interface.</p>
 */
public interface IMixinConfigPlugin {

    /**
     * Called after the plugin is instantiated, do any setup here.
     * 
     * @param mixinPackage The mixin root package from the config
     */
    public abstract void onLoad(String mixinPackage);

    /**
     * Called only if the "referenceMap" key in the config is <b>not</b> set.
     * This allows the refmap file name to be supplied by the plugin
     * programatically if desired. Returning <code>null</code> will revert to
     * the default behaviour of using the default refmap json file.
     * 
     * @return Path to the refmap resource or null to revert to the default
     */
    public abstract String getRefMapperConfig();
    
    /**
     * Called during mixin intialisation, allows this plugin to control whether
     * a specific will be applied to the specified target. Returning false will
     * remove the target from the mixin's target set, and if all targets are
     * removed then the mixin will not be applied at all.
     * 
     * @param targetClassName Fully qualified class name of the target class
     * @param mixinClassName Fully qualified class name of the mixin
     * @return True to allow the mixin to be applied, or false to remove it from
     *      target's mixin set
     */
    public abstract boolean shouldApplyMixin(String targetClassName, String mixinClassName);

    /**
     * Called after all configurations are initialised, this allows this plugin
     * to observe classes targetted by other mixin configs and optionally remove
     * targets from its own set. The set myTargets is a direct view of the
     * targets collection in this companion config and keys may be removed from
     * this set to suppress mixins in this config which target the specified
     * class. Adding keys to the set will have no effect.
     * 
     * @param myTargets Target class set from the companion config
     * @param otherTargets Target class set incorporating targets from all other
     *      configs, read-only
     */
    public abstract void acceptTargets(Set<String> myTargets, Set<String> otherTargets);
    
    /**
     * After mixins specified in the configuration have been processed, this
     * method is called to allow the plugin to add any additional mixins to
     * load. It should return a list of mixin class names or return null if the
     * plugin does not wish to append any mixins of its own.
     * 
     * @return additional mixins to apply
     */
    public abstract List<String> getMixins();

    /**
     * Called immediately <b>before</b> a mixin is applied to a target class,
     * allows any pre-application transformations to be applied.
     * 
     * @param targetClassName Transformed name of the target class
     * @param targetClass Target class tree
     * @param mixinClassName Name of the mixin class
     * @param mixinInfo Information about this mixin
     */
    public abstract void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo);

    /**
     * Called immediately <b>after</b> a mixin is applied to a target class,
     * allows any post-application transformations to be applied.
     * 
     * @param targetClassName Transformed name of the target class
     * @param targetClass Target class tree
     * @param mixinClassName Name of the mixin class
     * @param mixinInfo Information about this mixin
     */
    public abstract void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo);
}
