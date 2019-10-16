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
 * Class trackers are responsible for interacting with the class loading process
 * to for the purposes of tracking class load activity and class load
 * restrictions. This service component is entirely optional and services can
 * elect to return <tt>null</tt> if the platform does not support this
 * functionality.
 */
public interface IClassTracker {

    /**
     * Register an invalid class with the service classloader
     * 
     * @param className invalid class name
     */
    public abstract void registerInvalidClass(String className);

    /**
     * Check whether the specified class was already loaded by the service
     * classloader
     * 
     * @param className class name to check
     * @return true if the class was already loaded
     */
    public abstract boolean isClassLoaded(String className);

    /**
     * Check whether the specified class name is subject to any restrictions in
     * the context of this service
     * 
     * @param className class name to check
     * @return comma-separated list of restrictions, empty string if no
     *      restrictions apply
     */
    public abstract String getClassRestrictions(String className);

}
