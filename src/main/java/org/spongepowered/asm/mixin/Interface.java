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
 * I'm probably going to the special hell for this
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Interface {
    
    /**
     * Describes the remapping strategy applied to methods matching this
     * interface.
     */
    public enum Remap {
        
        /**
         * Attempt to remap all members of this interface which are declared in
         * the annotated mixin, including non-prefixed methods which match.
         */
        ALL,
        
        /**
         * Attempt to remap all members of this interface which are declared in
         * the annotated mixin, including non-prefixed methods which match. <b>
         * If mappings are not located for a member method, raise a compile-time
         * error.</b> 
         */
        FORCE(true),
        
        /**
         * Remap only methods in the annotated mixin which are prefixed with the
         * declared prefix. Note that if no prefix is defined, this has the same
         * effect as {@link #NONE} 
         */
        ONLY_PREFIXED,
        
        /**
         * Do not remap members matching this interface. (Equivalent to <tt>
         * remap=false</tt> on other remappable annotations)
         */
        NONE;
        
        private final boolean forceRemap;

        private Remap() {
            this(false);
        }
        
        private Remap(boolean forceRemap) {
            this.forceRemap = forceRemap;
        }
        
        /**
         * Returns whether this remap type should force remapping
         */
        public boolean forceRemap() {
            return this.forceRemap;
        }
        
    }
    
    /**
     * Interface that the parent {@link Implements} indicates the mixin 
     * implements. The interface will be hot-patched onto the target class as
     * part of the mixin application.
     * 
     * @return interface to implement
     */
    public Class<?> iface();
    
    /**
     * [Required] prefix for implementing interface methods. Works similarly to
     * {@link Shadow} prefixes, but <b>must</b> end with a dollar sign ($)
     * 
     * @return prefix to use
     */
    public String prefix();

    /**
     * If set to <tt>true</tt>, all methods implementing this interface are
     * treated as if they were individually decorated with {@link Unique}
     * 
     * @return true to mark all implementing methods as unique
     */
    public boolean unique() default false;

    /**
     * By default, the annotation processor will attempt to locate an
     * obfuscation mapping for all methods soft-implemented by the interface
     * declared in this {@link Interface} annotation, since it is possible that
     * the declared interface may be obfuscated and therefore contain obfuscated
     * member methods. However since it may be desirable to skip this pass (for
     * example if an interface method intrinsically shadows a soft-implemented
     * method) this setting is provided to restrict or inhibit processing of
     * member methods matching this soft-implements decoration.
     * 
     * @return Remapping strategy to use, see {@link Remap} for details. 
     */
    public Remap remap() default Remap.ALL;
    
}
