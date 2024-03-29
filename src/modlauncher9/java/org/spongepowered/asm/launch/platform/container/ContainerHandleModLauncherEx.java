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

import cpw.mods.jarhandling.SecureJar;

/**
 * ModLauncher root container for ModLauncher 9+
 */
public class ContainerHandleModLauncherEx extends ContainerHandleModLauncher {

    /**
     * Container handle for secure jar resources offered by ModLauncher
     */
    static class SecureJarResource extends ContainerHandleURI {

        private SecureJar jar;

        public SecureJarResource(SecureJar resource) {
            super(resource.getPrimaryPath().toUri());
            this.jar = resource;
        }
        
        @Override
        public String getId() {
            String name = this.jar.name();
            int lastDotPos = name.lastIndexOf('.');
            if (lastDotPos > 0) {
                name = name.substring(0, lastDotPos);
            }
            return name;
        }
        
        @Override
        public String getDescription() {
            return this.jar.getRootPath().toAbsolutePath().toString();
        }

        public String getName() {
            return this.jar.name();
        }
        
        public Path getPath() {
            return this.jar.getPrimaryPath();
        }
        
        @Override
        public String toString() {
            return String.format("SecureJarResource(%s)", this.getName());
        }

    }

    public ContainerHandleModLauncherEx(String name) {
        super(name);
    }
    
    @Override
    public void addResource(Object resource) {
        if (resource instanceof SecureJar) {
            this.add(new SecureJarResource((SecureJar)resource));
        } else {
            super.addResource(resource);
        }
    }

}
