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
package org.spongepowered.asm.mixin.injection.selectors;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Decoration interface for dynamic target selectors
 */
public interface ITargetSelectorDynamic extends ITargetSelector {
    
    /**
     * Decoration for subclasses which indicates id used for a specific selector
     * when specified, for example <tt>@MyNamespace:MySelector(argshere)</tt>
     * would specify "MySelector"
     */
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.TYPE)
    public @interface SelectorId {
        
        /**
         * Namespace for this code. Final selectors will be specified as
         * <tt>&lt;namespace&gt;:&lt;id&gt;</tt> in order to avoid overlaps
         * between consumer-provided selectors. If left blank defaults to the 
         * namespace specified in the configuration.
         */
        public String namespace() default "";
        
        /**
         * The string code used to specify the selector selector strings,
         * prefixed with namespace from the annotation or from the declaring
         * configuration.
         */
        public String value();
        
    }
    
    /**
     * Decoration for subclasses which indicates an annotation type from which
     * the selector can be parsed.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.TYPE)
    public @interface SelectorAnnotation {
        
        /**
         * Annotation type for the selector
         */
        public Class<? extends Annotation> value();
        
    }

}
