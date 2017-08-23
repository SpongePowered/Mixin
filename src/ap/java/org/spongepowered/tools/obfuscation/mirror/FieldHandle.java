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
package org.spongepowered.tools.obfuscation.mirror;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import org.spongepowered.asm.obfuscation.mapping.common.MappingField;

import com.google.common.base.Strings;

/**
 * Retrieved from a {@link TypeHandle} when searching for fields
 */
public class FieldHandle extends MemberHandle<MappingField> {
    
    /**
     * Actual element, can be null
     */
    private final VariableElement element;
    
    /**
     * True if this is a raw type element returned against a query for a
     * specialised type
     */
    private final boolean rawType;
    
    public FieldHandle(TypeElement owner, VariableElement element) {
        this(TypeUtils.getInternalName(owner), element);
    }

    public FieldHandle(String owner, VariableElement element) {
        this(owner, element, false);
    }
    
    public FieldHandle(TypeElement owner, VariableElement element, boolean rawType) {
        this(TypeUtils.getInternalName(owner), element, rawType);
    }
    
    public FieldHandle(String owner, VariableElement element, boolean rawType) {
        this(owner, element, rawType, TypeUtils.getName(element), TypeUtils.getInternalName(element));
    }
    
    public FieldHandle(String owner, String name, String desc) {
        this(owner, null, false, name, desc);
    }
    
    private FieldHandle(String owner, VariableElement element, boolean rawType, String name, String desc) {
        super(owner, name, desc);
        this.element = element;
        this.rawType = rawType;
    }
    
    /**
     * Get whether the element is imaginary (inaccessible via mirror)
     */
    public boolean isImaginary() {
        return this.element == null;
    }
    
    /**
     * Get the underlying element, may return null if the handle is imaginary
     */
    public VariableElement getElement() {
        return this.element;
    }
    
    @Override
    public Visibility getVisibility() {
        return TypeUtils.getVisibility(this.element);
    }

    /**
     * Returns true if the searched type had a type specifier but the returned
     * type does not
     */
    public boolean isRawType() {
        return this.rawType;
    }

    @Override
    public MappingField asMapping(boolean includeOwner) {
        return new MappingField(includeOwner ? this.getOwner() : null, this.getName(), this.getDesc());
    }

    @Override
    public String toString() {
        String owner = this.getOwner() != null ? "L" + this.getOwner() + ";" : "";
        String name = Strings.nullToEmpty(this.getName());
        String desc = Strings.nullToEmpty(this.getDesc());
        return String.format("%s%s:%s", owner, name, desc);
    }
    
}
