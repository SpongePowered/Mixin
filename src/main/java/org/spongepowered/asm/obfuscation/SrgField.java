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
package org.spongepowered.asm.obfuscation;

import com.google.common.base.Objects;

/**
 * Stores information about an SRG field mapping during AP runs
 */
public final class SrgField {

    private final String mapping;

    public SrgField(String mapping) {
        this.mapping = mapping;
    }
    
    public String getName() {
        if (this.mapping == null) {
            return null;
        }
        int pos = this.mapping.lastIndexOf('/');
        return pos > -1 ? this.mapping.substring(pos + 1) : this.mapping;
    }
    
    public String getOwner() {
        if (this.mapping == null) {
            return null;
        }
        int pos = this.mapping.lastIndexOf('/');
        return pos > -1 ? this.mapping.substring(0, pos) : null;
    }

    public String getMapping() {
        return this.mapping;
    }
    
    public SrgField move(String newOwner) {
        return new SrgField((newOwner != null ? newOwner + "/" : "") + this.getName());
    }

    public SrgField copy() {
        return new SrgField(this.mapping);
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(this.mapping);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SrgField) {
            return Objects.equal(this.mapping, ((SrgField)obj).mapping);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.mapping;
    }
    
}