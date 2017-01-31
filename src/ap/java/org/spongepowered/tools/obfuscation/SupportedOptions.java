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
package org.spongepowered.tools.obfuscation;

import java.util.Set;

import org.spongepowered.tools.obfuscation.service.ObfuscationServices;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

/**
 * Supported options for the annotation processor. Not an enum mainly so that
 * the string tokens can be inlined.
 */
public final class SupportedOptions {
    
    public static final String TOKENS                    = "tokens";
    public static final String OUT_REFMAP_FILE           = "outRefMapFile";
    public static final String DISABLE_TARGET_VALIDATOR  = "disableTargetValidator";
    public static final String DISABLE_TARGET_EXPORT     = "disableTargetExport";
    public static final String DISABLE_OVERWRITE_CHECKER = "disableOverwriteChecker";
    public static final String OVERWRITE_ERROR_LEVEL     = "overwriteErrorLevel";
    public static final String DEFAULT_OBFUSCATION_ENV   = "defaultObfuscationEnv";
    public static final String DEPENDENCY_TARGETS_FILE   = "dependencyTargetsFile";
    
    private SupportedOptions() {}

    /**
     * Return all supported options
     */
    public static Set<String> getAllOptions() {
        Builder<String> options = ImmutableSet.<String>builder();
        options.add(
            SupportedOptions.TOKENS,
            SupportedOptions.OUT_REFMAP_FILE,
            SupportedOptions.DISABLE_TARGET_VALIDATOR,
            SupportedOptions.DISABLE_TARGET_EXPORT,
            SupportedOptions.DISABLE_OVERWRITE_CHECKER,
            SupportedOptions.OVERWRITE_ERROR_LEVEL,
            SupportedOptions.DEFAULT_OBFUSCATION_ENV,
            SupportedOptions.DEPENDENCY_TARGETS_FILE
        );
        options.addAll(
            ObfuscationServices.getInstance().getSupportedOptions()
        );
        return options.build();
    }
}
