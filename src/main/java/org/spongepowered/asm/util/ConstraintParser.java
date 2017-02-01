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
package org.spongepowered.asm.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.util.throwables.ConstraintViolationException;
import org.spongepowered.asm.util.throwables.InvalidConstraintException;

/**
 * Parser for constraints
 */
public final class ConstraintParser {
    
    /**
     * A constraint. Constraints are parsed from string expressions which are
     * always of the form:
     * 
     * <blockquote><pre>&lt;token&gt;(&lt;constraint&gt;)</pre></blockquote>
     * 
     * <p><b>token</b> is normalised to uppercase and must be provided by the
     * environment.</p>
     * 
     * <p><b>constraint</b> is an integer range specified in one of the
     * following formats:
     *
     * <dl>
     *   <dt><pre>()</pre></dt>
     *   <dd>The token value must be present in the environment, but can have
     *     any value</dd>
     *   <dt><pre>(1234)</pre></dt>
     *   <dd>The token value must be <em>exactly equal to </em> <code>1234
     *   </code></dd>
     *   <dt><pre>(1234+)
     *(1234-)
     *(1234&gt;)
     *</pre></dt>
     *   <dd>All of these variants mean the same thing, and can be read as "1234
     *     or greater"</dd>
     *   <dt><pre>(&lt;1234)</pre></dt>
     *   <dd><em>Less than</em> 123</dd>
     *   <dt><pre>(&lt;=1234)</pre></dt>
     *   <dd><em>Less than or equal to</em> 1234 (equivalent to <code>1234&lt;
     *     </code>)</dd>
     *   <dt><pre>(&gt;1234)</pre></dt>
     *   <dd><em>Greater than</em> 1234</dd>
     *   <dt><pre>(&gt;=1234)</pre></dt>
     *   <dd><em>Greater than or equal to</em> 1234 (equivalent to <code>1234
     *     &gt;</code>)</dd>
     *   <dt><pre>(1234-1300)</pre></dt>
     *   <dd>Value must be <em>between</em> 1234 and 1300 (inclusive)</dd> 
     *   <dt><pre>(1234+10)</pre></dt>
     *   <dd>Value must be <em>between</em> 1234 and 1234+10 (1234-1244
     *     inclusive)</dd>
     * </dl>
     * 
     * <p>All whitespace is ignored in constraint declarations. The following
     * declarations are equivalent:</p>
     * 
     * <blockquote><pre>token(123-456)
     *token   (   123 - 456   )</pre></blockquote>
     *
     * <p>Multiple constraints should be separated by semicolon (<code>;</code>)
     * and are conjoined by an implied logical <code>AND</code> operator. That
     * is: all constraints must pass for the constraint to be considered valid.
     * </p>  
     */
    public static class Constraint {
        
        public static final Constraint NONE = new Constraint();
        
        private static final Pattern pattern =
                Pattern.compile("^([A-Z0-9\\-_\\.]+)\\((?:(<|<=|>|>=|=)?([0-9]+)(<|(-)([0-9]+)?|>|(\\+)([0-9]+)?)?)?\\)$");
        
        private final String expr;
        
        private String token;
        
        private String[] constraint;
        
        private int min = Integer.MIN_VALUE;
        
        private int max = Integer.MAX_VALUE;
        
        private Constraint next;
        
        Constraint(String expr) {
            this.expr = expr;
            Matcher matcher = Constraint.pattern.matcher(expr);
            if (!matcher.matches()) {
                throw new InvalidConstraintException("Constraint syntax was invalid parsing: " + this.expr);
            }
            
            this.token = matcher.group(1);
            this.constraint = new String[] {
                    matcher.group(2),
                    matcher.group(3),
                    matcher.group(4),
                    matcher.group(5),
                    matcher.group(6),
                    matcher.group(7),
                    matcher.group(8)
            };
            
            this.parse();
        }
        
        private Constraint() {
            this.expr = null;
            this.token = "*";
            this.constraint = new String[0];
        }
        
        private void parse() {
            if (!this.has(1)) {
                return;
            }
            
            this.max = this.min = this.val(1);
            boolean hasModifier = this.has(0);

            if (this.has(4)) {
                if (hasModifier) {
                    throw new InvalidConstraintException("Unexpected modifier '" + this.elem(0) + "' in " + this.expr + " parsing range");
                }
                this.max = this.val(4);
                if (this.max < this.min) {
                    throw new InvalidConstraintException("Invalid range specified '" + this.max + "' is less than " + this.min + " in " + this.expr);
                }
                return;
            } else if (this.has(6)) {
                if (hasModifier) {
                    throw new InvalidConstraintException("Unexpected modifier '" + this.elem(0) + "' in " + this.expr + " parsing range");
                }
                this.max = this.min + this.val(6);
                return;
            }
            
            if (hasModifier) {
                if (this.has(3)) {
                    throw new InvalidConstraintException("Unexpected trailing modifier '" + this.elem(3) + "' in " + this.expr);
                }
                String leading = this.elem(0);
                if (">".equals(leading)) {
                    this.min++;
                    this.max = Integer.MAX_VALUE;
                } else if (">=".equals(leading)) {
                    this.max = Integer.MAX_VALUE;
                } else if ("<".equals(leading)) {
                    this.max = --this.min;
                    this.min = Integer.MIN_VALUE;
                } else if ("<=".equals(leading)) {
                    this.max = this.min;
                    this.min = Integer.MIN_VALUE;
                }
            } else if (this.has(2)) {
                String trailing = this.elem(2);
                if ("<".equals(trailing)) {
                    this.max = this.min;
                    this.min = Integer.MIN_VALUE;
                } else {
                    this.max = Integer.MAX_VALUE;
                }
            }
        }

        private boolean has(int index) {
            return this.constraint[index] != null;
        }
        
        private String elem(int index) {
            return this.constraint[index];
        }
        
        private int val(int index) {
            return this.constraint[index] != null ? Integer.parseInt(this.constraint[index]) : 0;
        }
        
        void append(Constraint next) {
            if (this.next != null) {
                this.next.append(next);
                return;
            }
            this.next = next;
        }
        
        public String getToken() {
            return this.token;
        }
        
        public int getMin() {
            return this.min;
        }
        
        public int getMax() {
            return this.max;
        }
        
        /**
         * Checks the current token against the environment and throws a
         * {@link ConstraintViolationException} if the constraint is invalid
         * 
         * @param environment environment to fetch constraints
         * @throws ConstraintViolationException if constraint is not valid
         */
        public void check(ITokenProvider environment) throws ConstraintViolationException {
            if (this != Constraint.NONE) {
                Integer value = environment.getToken(this.token);
                if (value == null) {
                    throw new ConstraintViolationException("The token '" + this.token + "' could not be resolved in " + environment, this);
                }
                if (value.intValue() < this.min) {
                    throw new ConstraintViolationException("Token '" + this.token + "' has a value (" + value
                            + ") which is less than the minimum value " + this.min + " in " + environment, this, value.intValue());
                }
                if (value.intValue() > this.max) {
                    throw new ConstraintViolationException("Token '" + this.token + "' has a value (" + value 
                            + ") which is greater than the maximum value " + this.max + " in " + environment, this, value.intValue());
                }
            }
            if (this.next != null) {
                this.next.check(environment);
            }
        }
        
        /**
         * Gets a human-readable description of the range expressed by this
         * constraint
         */
        public String getRangeHumanReadable() {
            if (this.min == Integer.MIN_VALUE && this.max == Integer.MAX_VALUE) {
                return "ANY VALUE";
            } else if (this.min == Integer.MIN_VALUE) {
                return String.format("less than or equal to %d", this.max); 
            } else if (this.max == Integer.MAX_VALUE) {
                return String.format("greater than or equal to %d", this.min); 
            } else  if (this.min == this.max) {
                return String.format("%d", this.min);
            }
            return String.format("between %d and %d", this.min, this.max);
        }
        
        @Override
        public String toString() {
            return String.format("Constraint(%s [%d-%d])", this.token, this.min, this.max);
        }
    }
    
    private ConstraintParser() {
    }

    /**
     * Parse the supplied expression as a constraint and returns a new
     * Constraint. Returns {@link Constraint#NONE} if the constraint could not
     * be parsed or is empty.
     * 
     * @param expr constraint expression to parse
     * @return parsed constraint
     */
    public static Constraint parse(String expr) {
        if (expr == null || expr.length() == 0) {
            return Constraint.NONE;
        }
        
        String[] exprs = expr.replaceAll("\\s", "").toUpperCase().split(";");
        Constraint head = null;
        for (String subExpr : exprs) {
            Constraint next = new Constraint(subExpr);
            if (head == null) {
                head = next;
            } else {
                head.append(next);
            }
        }
        
        return head != null ? head : Constraint.NONE;
    }

    /**
     * Parse a constraint expression on the supplied annotation as a constraint
     * and returns a new Constraint. Returns {@link Constraint#NONE} if the
     * constraint could not be parsed or is empty.
     * 
     * @param annotation annotation containing the constraint expression to
     *      parse
     * @return parsed constraint
     */
    public static Constraint parse(AnnotationNode annotation) {
        String constraints = Annotations.getValue(annotation, "constraints", "");
        return ConstraintParser.parse(constraints);
    }

}
