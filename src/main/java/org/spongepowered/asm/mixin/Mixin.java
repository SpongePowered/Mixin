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
package org.spongepowered.asm.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The main decorator for mixin classes, this annotation denotes a class as a
 * mixin and specifies two key attributes:
 * 
 * <dl>
 *   <dt>The target classes</dt>
 *   <dd>Every mixin requires at least one target class in order to be valid.
 *     The target classes should be specified via {@link #value} where possible
 *     using class literals. However for anonymous inner classes,
 *     package-private classes, or unavailable classes (eg. synthetic classes)
 *     being targetted via {@link Pseudo &#64;Pseudo}, the targets should be
 *     specified via the {@link #targets} instead.</dd>
 *   <dt>The priority</dt>
 *   <dd>The mixin {@link #priority} is used to express the mixin's desired
 *     ordering with respect to other mixins being applied to the same target
 *     class.</dd>
 * </dl>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Mixin {

    /**
     * Target class(es) for this mixin
     * 
     * @return classes this mixin targets
     */
    public Class<?>[] value() default { };
    
    /**
     * Since specifying targets in {@link #value} requires that the classes be
     * publicly visible, this property is provided to allow package-private,
     * anonymous innner, and private inner, or unavailable ({@link Pseudo
     * &#64;Pseudo} classes to be referenced. Referencing an otherwise public
     * class using this property is an error condition and will raise a warning
     * at runtime or an error if {@link MixinEnvironment.Option#DEBUG_STRICT
     * strict checks} are enabled. It is completely fine to specify both publi
     * and private targets for the same mixin however.
     * 
     * <p>Note that unlike class literals specified in {@link #value}, imports
     * are not supported and therefore the class specified here must be
     * fully-qualified with the package name.</p>
     *
     * @return protected, package-private, or {@link Pseudo pseudo} classes this
     *      mixin targets
     */
    public String[] targets() default { };

    /**
     * Priority for the mixin, relative to other mixins targetting the same
     * classes. By default mixins inherit their priority from the parent
     * configuration that contains them, setting this value allows the priority
     * to be overridden on a per-mixin basis.
     * 
     * <p>Priorities sort mixins into <em>natural order</em> with lower priority
     * mixins being applied first. This changes the nature of priority based on
     * your intentions. Especially with respect to injectors, which must be
     * considered on a case-by-case basis since the semantics of priority vary
     * by injector because of first-come-first-served versus implied ordering of
     * injectors such as inject-at-HEAD.</p>
     * 
     * @return the mixin priority (relative to other mixins targetting the same
     *      class)
     */
    public int priority() default 1000;

    /**
     * By default, the annotation processor will attempt to locate an
     * obfuscation mapping for all {@link Overwrite}, {@link Shadow} and
     * {@link org.spongepowered.asm.mixin.injection.Inject} annotated members
     * since it is anticipated that in general the target of a {@link Mixin}
     * will be an obfuscated class and all annotated members will need to be
     * added to the obfuscation table. However since it is possible to also
     * apply mixins to non-obfuscated targets it may be desirable to suppress
     * the compiler warnings which would otherwise be generated. This can be
     * done on an individual member basis by setting <code>remap</code> to
     * <em>false</em> on the individual annotations, or disabled for the entire
     * mixin by setting the value here to <em>false</em>. Doing so will cause
     * the annotation processor to skip all annotations in this mixin when
     * building the obfuscation table unless the individual annotation is
     * explicitly decorated with <tt>remap = true</tt>.
     * 
     * @return True to instruct the annotation processor to search for
     *      obfuscation mappings for this annotation (default true). 
     */
    public boolean remap() default true;

}
