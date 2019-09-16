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

import java.net.URL;

/**
 * Interface for marshal object which can retrieve classes from the environment
 */
public interface IClassProvider {

    /**
     * Get the current classpath from the service classloader
     * 
     * @deprecated As of 0.8, use of this method is not a sensible way to access
     *      available containers.  
     */
    @Deprecated
    public abstract URL[] getClassPath();

    /**
     * Find a class in the service classloader
     * 
     * @param name class name
     * @return resultant class
     * @throws ClassNotFoundException if the class was not found
     */
    public abstract Class<?> findClass(final String name) throws ClassNotFoundException;
    
    /**
     * Marshal a call to <tt>Class.forName</tt> for a regular class
     * 
     * @param name class name
     * @param initialize init flag
     * @return Klass
     */
    public abstract Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException;

    /**
     * Marshal a call to <tt>Class.forName</tt> for an agent class
     * 
     * @param name agent class name
     * @param initialize init flag
     * @return Klass
     */
    public abstract Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException;

}
