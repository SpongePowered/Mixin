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

import org.spongepowered.asm.mixin.MixinEnvironment;

/**
 * Provides a token value into the attached environment
 */
public interface IEnvironmentTokenProvider {
    
    /**
     * Default token provider priority 
     */
    public static final int DEFAULT_PRIORITY = 1000;
    
    /**
     * Get the priority for this provider, should return a priority relative to
     * {@link #DEFAULT_PRIORITY}.
     */
    public abstract int getPriority();

    /**
     * Get the value of the specified token in this environment, or return null
     * if this provider does not have a value for this token. All tokens are 
     * converted to UPPERCASE before being requested from the provider 
     * 
     * @param token Token (in upper case) to search for
     * @param env Current environment
     * @return The token value, or null if this provider does not have the token
     */
    public abstract Integer getToken(String token, MixinEnvironment env);
    
}
