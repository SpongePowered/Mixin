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

import java.nio.file.Path;
import java.util.List;
import java.util.Map.Entry;

import org.spongepowered.asm.service.MixinService;

/**
 * ModLauncher root container
 */
public class ContainerHandleModLauncher extends ContainerHandleVirtual {
    
    /**
     * Container handle for nio resources offered by ModLauncher
     */
    class Resource extends ContainerHandleURI {

        private String name;
        private Path path;

        public Resource(String name, Path path) {
            super(path.toUri());
            this.name = name;
            this.path = path;
        }
        
        public String getName() {
            return this.name;
        }
        
        public Path getPath() {
            return this.path;
        }
        
        @Override
        public String toString() {
            return String.format("ContainerHandleModLauncher.Resource(%s:%s)", this.name, this.path);
        }
        
    }
    
    public ContainerHandleModLauncher(String name) {
        super(name);
    }
    
    /**
     * Add a resource to to this container
     * 
     * @param name Resource name
     * @param path Resource path
     */
    public void addResource(String name, Path path) {
        this.add(new Resource(name, path));
    }
    
    /**
     * Add a resource to to this container
     * 
     * @param entry Resource entry
     */
    public void addResource(Entry<String, Path> entry) {
        this.add(new Resource(entry.getKey(), entry.getValue()));
    }
    
    /**
     * Add a resource to to this container
     * 
     * @param resource Resource
     */
    @SuppressWarnings("unchecked")
    public void addResource(Object resource) {
        if (resource instanceof Entry) {
            this.addResource((Entry<String, Path>)resource);
        } else {
            MixinService.getService().getLogger("mixin").error("Unrecognised resource type {} passed to {}", resource.getClass(), this);
        }
    }
    
    /**
     * Add a collection of resources to this container
     * 
     * @param resources Resources to add
     */
    public void addResources(List<?> resources) {
        for (Object resource : resources) {
            this.addResource(resource);
        }
    }

    @Override
    public String toString() {
        return String.format("ModLauncher Root Container(%s:%x)", this.getName(), this.hashCode());
    }

}
