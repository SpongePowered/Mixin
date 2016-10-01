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
package org.spongepowered.asm.obfuscation.mapping.mcp;

import org.spongepowered.asm.obfuscation.mapping.common.MappingField;

/**
 * An SRG field mapping
 */
public class MappingFieldSrg extends MappingField {

    private final String srg;

    public MappingFieldSrg(String srg) {
        super(MappingFieldSrg.getOwnerFromSrg(srg), MappingFieldSrg.getNameFromSrg(srg), null);
        this.srg = srg;
    }
    
    public MappingFieldSrg(MappingField field) {
        super(field.getOwner(), field.getName(), null);
        this.srg = field.getOwner() + "/" + field.getName();
    }

    @Override
    public String serialise() {
        return this.srg;
    }

    private static String getNameFromSrg(String srg) {
        if (srg == null) {
            return null;
        }
        int pos = srg.lastIndexOf('/');
        return pos > -1 ? srg.substring(pos + 1) : srg;
    }

    private static String getOwnerFromSrg(String srg) {
        if (srg == null) {
            return null;
        }
        int pos = srg.lastIndexOf('/');
        return pos > -1 ? srg.substring(0, pos) : null;
    }

}
