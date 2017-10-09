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
package org.spongepowered.asm.service;

/**
 * Global property service
 */
public interface IGlobalPropertyService {

    /**
     * Get a value from the global property store (blackboard) and duck-type it
     * to the specified type
     * 
     * @param key blackboard key
     * @param <T> duck type
     * @return value
     */
    public abstract <T> T getProperty(String key);
    
    /**
     * Set the specified value in the global property store (blackboard)
     * 
     * @param key blackboard key
     * @param value new value
     */
    public abstract void setProperty(String key, Object value);
    
    /**
     * Get the value from the global property store (blackboard) but return
     * <tt>defaultValue</tt> if the specified key is not set.
     * 
     * @param key blackboard key
     * @param defaultValue value to return if the key is not set or is null
     * @param <T> duck type
     * @return value from blackboard or default value
     */
    public abstract <T> T getProperty(String key, T defaultValue);
    
    /**
     * Get a string from the global property store (blackboard), returns default
     * value if not set or null.
     * 
     * @param key blackboard key
     * @param defaultValue default value to return if the specified key is not
     *      set or is null
     * @return value from blackboard or default
     */
    public abstract String getPropertyString(String key, String defaultValue);

}
