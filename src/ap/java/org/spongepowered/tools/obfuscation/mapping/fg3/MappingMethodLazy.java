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
package org.spongepowered.tools.obfuscation.mapping.fg3;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.mapping.IMappingProvider;

/**
 * A MappingMethod which computes transformed descriptor on demand using the
 * supplied mapping provider. This means we only need to compute transformed
 * descriptors for methods which are actually used.
 */
public class MappingMethodLazy extends MappingMethod {
    
    private static final Pattern PATTERN_CLASSNAME = Pattern.compile("L([^;]+);");
    
    private final String originalDesc;
    
    private final IMappingProvider mappingProvider;

    private String newDesc;

    public MappingMethodLazy(String owner, String simpleName, String originalDesc, IMappingProvider mappingProvider) {
        super(owner, simpleName, "{" + originalDesc + "}"); // descriptor provided since it's used for hash generation
        this.originalDesc = originalDesc;
        this.mappingProvider = mappingProvider;
    }
    
    @Override
    public String getDesc() {
        if (this.newDesc == null) {
            this.newDesc = this.generateDescriptor();
        }
        return this.newDesc;
    }

    @Override
    public String toString() {
        String desc = this.getDesc();
        return String.format("%s%s%s", this.getName(), desc != null ? " " : "", desc != null ? desc : "");
    }

    private String generateDescriptor() {
        StringBuffer desc = new StringBuffer();
        Matcher matcher = MappingMethodLazy.PATTERN_CLASSNAME.matcher(this.originalDesc);
        while (matcher.find()) {
            String remapped = this.mappingProvider.getClassMapping(matcher.group(1));
            if (remapped != null) {
                matcher.appendReplacement(desc, Matcher.quoteReplacement("L" + remapped + ";"));
            } else {
                matcher.appendReplacement(desc, Matcher.quoteReplacement("L" + matcher.group(1) + ";"));
            }
        }
        matcher.appendTail(desc);
        return desc.toString();
    }

}
