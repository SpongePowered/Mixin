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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A virtual container, used to marshal other containers around
 */
public class ContainerHandleVirtual implements IContainerHandle {
    
    /**
     * Name, used mainly for differentiation since containers are used as map
     * keys
     */
    private final String name;
    
    /**
     * Virtual attributes
     */
    private final Map<String, String> attributes = new HashMap<String, String>();
    
    /**
     * Virtual nested containers (might be real containers, depending on the
     * purpose of this container
     */
    private final Set<IContainerHandle> nestedContainers = new LinkedHashSet<IContainerHandle>();

    /**
     * .ctor
     * 
     * @param name Unique container name
     */
    public ContainerHandleVirtual(String name) {
        this.name = name;
    }
    
    @Override
    public String getId() {
        return this.name;
    }
    
    @Override
    public String getDescription() {
        return this.toString();
    }
    
    /**
     * Get the name of this container
     */
    public String getName() {
        return this.name;
    }
    
    /**
     * Set a virtual attribute on this virtual container
     * 
     * @param key attribute key
     * @param value attribute value
     * @return fluent interface
     */
    public ContainerHandleVirtual setAttribute(String key, String value) {
        this.attributes.put(key, value);
        return this;
    }
    
    /**
     * Add a nested container to this virtual container
     * 
     * @param nested Nested container to add
     * @return fluent
     */
    public ContainerHandleVirtual add(IContainerHandle nested) {
        this.nestedContainers.add(nested);
        return this;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.platform.container.IContainerHandle
     *      #getAttribute(java.lang.String)
     */
    @Override
    public String getAttribute(String name) {
        return this.attributes.get(name);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.platform.container.IContainerHandle
     *      #getNestedContainers()
     */
    @Override
    public Collection<IContainerHandle> getNestedContainers() {
        return Collections.<IContainerHandle>unmodifiableSet(this.nestedContainers);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof String && obj.toString().equals(this.name);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("ContainerHandleVirtual(%s:%x)", this.name, this.hashCode());
    }
    
}
