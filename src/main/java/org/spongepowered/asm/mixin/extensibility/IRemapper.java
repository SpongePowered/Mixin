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
package org.spongepowered.asm.mixin.extensibility;

/**
 * Interface for remap chain participants
 */
public interface IRemapper {

    /**
     * Map method name to the new name. Subclasses can override.
     * 
     * @param owner owner of the method.
     * @param name name of the method.
     * @param desc descriptor of the method.
     * @return new name of the method
     */
    public abstract String mapMethodName(String owner, String name, String desc);

    /**
     * Map field name to the new name. Subclasses can override.
     * 
     * @param owner owner of the field.
     * @param name name of the field
     * @param desc descriptor of the field
     * @return new name of the field.
     */
    public abstract String mapFieldName(String owner, String name, String desc);

    /**
     * Map type name to the new name. Subclasses can override.
     * 
     * @param typeName Class name to convert 
     * @return new name for the class
     */
    public abstract String map(String typeName);

    /**
     * Convert a mapped type name back to the original obfuscated name
     * 
     * @param typeName Class name to convert
     * @return old name for the class
     */
    public abstract String unmap(String typeName);
    
    /**
     * Convert a descriptor to remapped form
     * 
     * @param desc descriptor to convert
     * @return new descriptor
     */
    public abstract String mapDesc(String desc);

    /**
     * Convert a descriptor back to the original obfuscated form
     * 
     * @param desc descriptor to convert
     * @return old descriptor
     */
    public abstract String unmapDesc(String desc);
    
}
