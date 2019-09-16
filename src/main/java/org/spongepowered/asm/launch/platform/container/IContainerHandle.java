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
package org.spongepowered.asm.launch.platform.container;

import java.util.Collection;

/**
 * Interface for container handles. Previously resources considered by Mixin
 * were indexed by URI but in order to provide more flexibility the container
 * handle system now wraps URIs in a more expressive object, and provides
 * support for both virtual containers and nested container resolution.
 */
public interface IContainerHandle {

    /**
     * Retrieve the value of attribute with the specified name, or null if not
     * present
     * 
     * @param name attribute name
     * @return attribute value or null if not present
     */
    public abstract String getAttribute(String name);
    
    /**
     * Get nested containers from this container, allows a container to detect
     * and return containers within itself. For example a folder container
     * detecting and returning file containers, or a virtual container returning
     * real containers after a scan.
     */
    public abstract Collection<IContainerHandle> getNestedContainers();

}
