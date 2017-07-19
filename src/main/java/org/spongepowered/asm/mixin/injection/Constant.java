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
package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.spongepowered.asm.lib.Opcodes;

/**
 * Annotation for specifying the injection point for an {@link ModifyConstant}
 * injector. Leaving all values unset causes the injection point to match all
 * constants with the same type as the {@link ModifyConstant} handler's return
 * type.
 * 
 * <p>To match a specific constant, specify the appropriate value for the
 * appropriate argument. Specifying values of different types will cause an
 * error to be raised by the injector.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Constant {
    
    /**
     * Available options for the {@link Constant#expandZeroConditions} setting.
     * Each option matches the inverse instructions as well because in the
     * compiled code it is not unusual for <tt>if (x &gt; 0)</tt> to be compiled
     * as <tt>if (!(x &lt;= 0))</tt>
     * 
     * <p>Note that all of these options assume that <tt>x</tt> is on the <b>
     * left-hand side</b> of the expression in question. For expressions where
     * zero is on the <b>right-hand side</b> you should choose the inverse.</p>
     */
    public enum Condition {
        
        /**
         * Match &lt; operators and &gt;= instructions:
         * 
         * <code>x &lt; 0</code>
         */
        LESS_THAN_ZERO(Opcodes.IFLT, Opcodes.IFGE),
        
        /**
         * Match &lt;= operators and &gt; instructions
         * 
         * <code>x &lt;= 0</code>
         */
        LESS_THAN_OR_EQUAL_TO_ZERO(Opcodes.IFLE, Opcodes.IFGT),
        
        /**
         * Match &gt;= operators and &lt; instructions, equivalent to
         * {@link #LESS_THAN_ZERO}
         * 
         * <code>x &gt;= 0</code>
         */
        GREATER_THAN_OR_EQUAL_TO_ZERO(Condition.LESS_THAN_ZERO),
        
        /**
         * Match &gt; operators and &lt;= instructions, equivalent to
         * {@link #LESS_THAN_OR_EQUAL_TO_ZERO}
         * 
         * <code>x &gt; 0</code>
         */
        GREATER_THAN_ZERO(Condition.LESS_THAN_OR_EQUAL_TO_ZERO);
        
        private final int[] opcodes;
        
        private final Condition equivalence;
        
        private Condition(int... opcodes) {
            this(null, opcodes);
        }
        
        private Condition(Condition equivalence) {
            this(equivalence, equivalence.opcodes);
        }
        
        private Condition(Condition equivalence, int... opcodes) {
            this.equivalence = equivalence != null ? equivalence : this;
            this.opcodes = opcodes;
        }
        
        /**
         * Get the condition which is equivalent to this condition
         */
        public Condition getEquivalentCondition() {
            return this.equivalence;
        }
        
        /**
         * Get the opcodes for this condition
         */
        public int[] getOpcodes() {
            return this.opcodes;
        }
        
    }
    
    /**
     * Causes this injector to match <tt>ACONST_NULL</tt> (null object) literals
     * 
     * @return true to match <tt>null</tt>
     */
    public boolean nullValue() default false;

    /**
     * Specify an integer constant to match, includes byte and short values.
     * 
     * <p><b>Special note for referencing <tt>0</tt> (zero) which forms part of
     * a comparison expression:</b> See the {@link #expandZeroConditions} option
     * below.</p>
     * 
     * @return integer value to match
     */
    public int intValue() default 0;
    
    /**
     * Specify a float constant to match
     * 
     * @return float value to match
     */
    public float floatValue() default 0.0F;
    
    /**
     * Specify a long constant to match
     * 
     * @return long value to match
     */
    public long longValue() default 0L;
    
    /**
     * Specify a double constant to match
     * 
     * @return double value to match
     */
    public double doubleValue() default 0.0;
    
    /**
     * Specify a String constant to match
     * 
     * @return string value to match
     */
    public String stringValue() default "";
    
    /**
     * Specify a type literal to match
     * 
     * @return type literal to match
     */
    public Class<?> classValue() default Object.class;
    
    /**
     * Ordinal offset. Many InjectionPoints will return every opcode matching
     * their criteria, specifying <em>ordinal</em> allows a particular opcode to
     * be identified from the returned list. The default value of -1 does not
     * alter the behaviour and returns all matching opcodes. Specifying a value
     * of 0 or higher returns <em>only</em> the requested opcode (if one exists:
     * for example specifying an ordinal of 4 when only 2 opcodes are matched by
     * the InjectionPoint is not going to work particularly well!)
     * 
     * @return ordinal value for supported InjectionPoint types
     */
    public int ordinal() default -1;
    
    /**
     * This specifies the ID of the slice to use for this query.
     * 
     * <p>For more details see the {@link Slice#id}</p>
     * 
     * @return the slice identifier, or empty string to use the default slice
     */
    public String slice() default "";

    /**
     * Whilst most constants can be located in the compiled method with relative
     * ease, there exists a special case when a <tt>zero</tt> is used in a
     * conditional expression. For example:
     * 
     * <blockquote><code>if (x &gt;= 0)</code></blockquote>
     * 
     * <p>This special case occurs because java includes explicit instructions
     * for this type of comparison, and thus the compiled code might look more
     * like this:</p>
     * 
     * <blockquote><code>if (x.isGreaterThanOrEqualToZero())</code></blockquote>
     * 
     * <p>Of course if we know that the constant we are searching for is part of
     * a comparison, then we can explicitly search for the
     * <tt>isGreaterThanOrEqualToZero</tt> and convert it back to the original
     * form in order to redirect it just like any other constant access.</p>
     * 
     * <p>To enable this behaviour, you may specify one or more values for this
     * argument based on the type of expression you wish to expand. Since the
     * Java compiler is wont to compile certain expressions as the <i>inverse
     * </i> of their source-level counterparts (eg. compiling a <em>do this if
     * greater than</em> structure to a <em>ignore this if less than or equal
     * </em> structure); specifying a particular expression type implicitly
     * includes the inverse expression as well.</p>
     * 
     * <p>It is worth noting that the effect on ordinals may be hard to predict,
     * and thus care should be taken to ensure that the selected injection
     * points match the expected locations.</p>
     * 
     * <p>Specifying this option has the following effects:</p>
     * 
     * <ul>
     *   <li>Matching conditional opcodes in the target method are identified
     *     for injection candidacy.</li>
     *   <li>An <tt>intValue</tt> of <tt>0</tt> is implied and does not need to
     *     be explicitly defined.</li>
     *   <li>However, explicitly specifying an <tt>intValue</tt> of <tt>0</tt>
     *     will cause this selector to also match explicit <tt>0</tt> constants
     *     in the method body as well.</li>
     * </ul>
     */
    public Condition[] expandZeroConditions() default {};
    
    /**
     * @return true to enable verbose debug logging for this injection point
     */
    public boolean log() default false;

}
