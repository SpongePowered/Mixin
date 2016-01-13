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
 * This annotation allows fine-tuning of the overwrite policy for a
 * soft-implemented interface member method.
 * 
 * <p>By default, all members within a mixin will overwrite (completely replace)
 * matching methods in the target class without prejudice. However this can be
 * a problem when soft-implementing an interface to deal with a method conflict
 * because it may be desirable to only override the method <b>if it does not
 * already exist</b> in the target, for example when running in a development
 * environment.</p>
 * 
 * <p>This annotation introduces two new behaviours for dealing with target
 * methods which implement a soft interface intrinsically.</p>
 * 
 * <p>Consider the following code:</p>
 * 
 * <blockquote><pre>&#064;Shadow public int getSomething();
 * 
 * public int soft$getSomething() {
 *     return this.getSomething();
 * }</pre></blockquote>
 * 
 * <p>This type of accessor is common when an interface conflict occurs, but has
 * the problem that the injected accessor will be effectively self-referential
 * in development, because the prefix will be removed and the accessor will thus
 * cause a stack overflow.</p>
 * 
 * <p>The only way to address this issue is to replace the shadow with a copy of
 * the original accessor, like so:
 * 
 * <blockquote><pre>&#064;Shadow private int something;
 * 
 * public int soft$getSomething() {
 *     return this.something;
 * }</pre></blockquote>
 * 
 * <p>But this has the drawback of forcing the mixin to re-implement the target
 * method, and thus forces ongoing maintenance of the mixin to include updating
 * the contents of the accessor.</p>
 * 
 * <p><code>Intrinsic</code> allows this problem to be circumvented in one of
 * two ways. The first way essentially declares "don't merge this method if it
 * already exists in the target". To use this behaviour, we simply tag our
 * accessor with {@link Intrinsic &#064;Intrinsic}:</p>
 * 
 * <blockquote><pre>&#064;Shadow public int getSomething();
 * 
 * &#064;Intrinsic // don't merge the method if the target already exists
 * public int soft$getSomething() {
 *     return this.getSomething();
 * }</pre></blockquote>
 * 
 * <p>This is ideal if the accessor is simply providing a proxy through to an
 * underlying obfuscated method, as in the example above. However, if the new
 * method contains additional code, such as in this example:</p>
 * 
 * <blockquote><pre>&#064;Shadow public int getSomething();
 * 
 * public int soft$getSomething() {
 *     return this.someCondition ? this.someOtherValue : this.getSomething();
 * }</pre></blockquote>
 * 
 * <p>Then we still have the original problem that the method becomes re-entrant
 * without specifying the additional Overwrite. We can solve this with the
 * second {@link Intrinsic} behaviour, and set the {@link #displace} argument to
 * <code>true</code>. Setting <code>displace</code> instructs the mixin
 * transformer to <em>proxy</em> the method call <em>if</em> the target already
 * exists, but merge the method normally if it does not. This allows our new
 * accessor to call the original method in all circumstances.</p>
 * 
 * <p>When <code>displace</code> is enabled, if the target method exists then
 * instead of being overwritten it is instead <em>renamed</em> with a temporary
 * name and all references to the original method <em>within the Intrinsic
 * method</em> are updated to call the renamed method. This means that all
 * external code will still reference the Intrinsic accessor (just as it would
 * have done with a regular overwrite) but code <em>within</em> the Intrinsic
 * accessor will call the overwritten method.</p>
 * 
 * <blockquote><pre>&#064;Shadow public int getSomething();
 * 
 * &#064;Intrinsic(displace = true) // if the target already exists, displace it
 * public int soft$getSomething() {
 *     return this.someCondition ? this.someOtherValue : this.getSomething();
 * }</pre></blockquote>
 * 
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.CLASS)
public @interface Intrinsic {

    /**
     * If set to true, this intrinsic method will replace any existing method in
     * the target class by renaming it and updating internal references inside
     * the method to reference the displaced method. If false or omitted,
     * instructs this intrinsic method to <b>not</b> overwrite the target method
     * if it exists, contrary to the normal behaviour for mixins
     * 
     * @return true if this intrinsic method should displace matching targets
     */
    public boolean displace() default false;
    
}
