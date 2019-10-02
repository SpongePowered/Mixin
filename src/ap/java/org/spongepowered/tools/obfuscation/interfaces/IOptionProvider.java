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
package org.spongepowered.tools.obfuscation.interfaces;

import java.util.List;

/**
 * An object which can provide option values to consumers
 */
public interface IOptionProvider {

    /**
     * Fetch the value of the specified option, if available
     * 
     * @param option Name of the option to fetch
     * @return Option value or null if absent
     */
    public abstract String getOption(String option);

    /**
     * Fetch the value of the specified option, if available. If the option is
     * not available, return the specified default value
     * 
     * @param option Name of the option to fetch
     * @param defaultValue Default value to return if the option is not present
     * @return Option value or default if absent
     */
    public abstract String getOption(String option, String defaultValue);

    /**
     * Fetch the value of the specified option, if available. If the option is
     * not available, return the specified default value
     * 
     * @param option Name of the option to fetch
     * @param defaultValue Default value to return if the option is not present
     * @return Option value or default if absent
     */
    public abstract boolean getOption(String option, boolean defaultValue);
    
    /**
     * Fetch the values of the specified comma-separated option, if available
     * 
     * @param option Name of the option to fetch
     * @return Option value or null if absent
     */
    public abstract List<String> getOptions(String option);

}
