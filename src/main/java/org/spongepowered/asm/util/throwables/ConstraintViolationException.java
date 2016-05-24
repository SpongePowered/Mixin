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
package org.spongepowered.asm.util.throwables;

import org.spongepowered.asm.util.ConstraintParser.Constraint;

/**
 * Exception thrown when a constraint violation is encountered
 */
public class ConstraintViolationException extends Exception {

    private static final String MISSING_VALUE = "UNRESOLVED";

    private static final long serialVersionUID = 1L;
    
    private final Constraint constraint;
    
    private final String badValue;

    public ConstraintViolationException(Constraint constraint) {
        this.constraint = constraint;
        this.badValue = ConstraintViolationException.MISSING_VALUE;
    }

    public ConstraintViolationException(Constraint constraint, int badValue) {
        this.constraint = constraint;
        this.badValue = String.valueOf(badValue);
    }

    public ConstraintViolationException(String message, Constraint constraint) {
        super(message);
        this.constraint = constraint;
        this.badValue = ConstraintViolationException.MISSING_VALUE;
    }

    public ConstraintViolationException(String message, Constraint constraint, int badValue) {
        super(message);
        this.constraint = constraint;
        this.badValue = String.valueOf(badValue);
    }

    public ConstraintViolationException(Throwable cause, Constraint constraint) {
        super(cause);
        this.constraint = constraint;
        this.badValue = ConstraintViolationException.MISSING_VALUE;
    }

    public ConstraintViolationException(Throwable cause, Constraint constraint, int badValue) {
        super(cause);
        this.constraint = constraint;
        this.badValue = String.valueOf(badValue);
    }

    public ConstraintViolationException(String message, Throwable cause, Constraint constraint) {
        super(message, cause);
        this.constraint = constraint;
        this.badValue = ConstraintViolationException.MISSING_VALUE;
    }
    
    public ConstraintViolationException(String message, Throwable cause, Constraint constraint, int badValue) {
        super(message, cause);
        this.constraint = constraint;
        this.badValue = String.valueOf(badValue);
    }
    
    public Constraint getConstraint() {
        return this.constraint;
    }
    
    public String getBadValue() {
        return this.badValue;
    }

}
