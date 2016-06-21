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
 * Anotation used to decorate items you might wish to examine after mixin
 * application.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Debug {
    
    /**
     * Only applicable for classes, use this to decorate mixins that you wish
     * to export even if <tt>mixin.debug.export</tt> is not enabled. This is
     * useful if you wish to export only the target of the mixin you are working
     * on without enabling export globally.
     * 
     * <p>Note that if <tt>mixin.debug.export</tt> is <b>not</b> <tt>true</tt>
     * then the decompiler is <em>not</em> initialised (even if present on the
     * classpath) and thus you must set <tt>mixin.debug.export.decompile</tt> to
     * <tt>true</tt> in order to have force-exported classes decompiled.</p>
     * 
     * @return whether to export the decorated target class
     */
    public boolean export() default false;

    /**
     * Print the method bytecode to the console after mixin application. This
     * setting is only used if the <tt>mixin.debug.verbose</tt> option is
     * enabled.
     * 
     * @return whether to print the class or method to the console.
     */
    public boolean print() default false;
    
}
