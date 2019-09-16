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
package org.spongepowered.asm.service.modlauncher;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.IClassInterceptor;
import org.spongepowered.asm.service.modlauncher.ext.IModLauncherClassBytecodeProvider;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService.Phase;

/**
 * Bytecode provider for ModLauncher environment.
 */
class ModLauncherBytecodeProvider implements IModLauncherClassBytecodeProvider, IClassInterceptor {

    /**
     * Exception thrown by the interceptor to terminate the transformation
     * pipeline once we have obtained a ClassNode in the state we want it.
     */
    static class CaptureException extends RuntimeException {

        private static final long serialVersionUID = 1L;
        
        ClassNode captured;

        CaptureException(ClassNode captured) {
            this.captured = captured;
        }

    }
    
    /**
     * Filthy hacks
     */
    private final Internals internals = Internals.getInstance();
    
    /**
     * Class names we want to capture
     */
    private final Map<String, Phase> captures = new HashMap<String, Phase>();
    
    /**
     * Classes which are initially empty
     */
    private final Set<String> emptyClasses = new HashSet<String>();
    
    ModLauncherBytecodeProvider() {
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IClassBytecodeProvider#getClassNode(
     *      java.lang.String)
     */
    @Override
    public synchronized ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        return this.getClassNode(name, true, true);
    }
    
    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
        synchronized (this.captures) {
            String internalName = classType.getInternalName();
            Phase capturePhase = this.captures.get(internalName);
            if (capturePhase != null) {
                if (isEmpty) {
                    this.emptyClasses.add(internalName);
                }
                return EnumSet.<Phase>of(capturePhase);
            }
        }
        return null;
    }

  @Override
  public boolean processClass(Phase phase, ClassNode classNode, Type classType) {
      synchronized (this.captures) {
          Phase capturePhase = this.captures.get(classType.getInternalName());
          if (capturePhase != null && phase == capturePhase) {
              throw new CaptureException(classNode);
          }
      }
      return false;
  }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IClassBytecodeProvider#getClassNode(
     *      java.lang.String, boolean)
     */
    @Override
    public synchronized ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        return this.getClassNode(name, runTransformers, false);
    }
    
    private synchronized ClassNode getClassNode(String name, boolean runTransformers, boolean ignoreEmpty)
            throws ClassNotFoundException {

        String internalName = name.replace('.', '/');
                
        synchronized (this.captures) {
            this.emptyClasses.remove(internalName);
            this.captures.put(internalName, runTransformers ? Phase.AFTER : Phase.BEFORE);
        }

        ClassNode captured = null;
        try {
            this.internals.simulateClassLoad(name, runTransformers);
        } catch (CaptureException ex) {
            captured = ex.captured;
            synchronized (this.captures) {
                if (this.emptyClasses.contains(internalName)) {
                    if (!ignoreEmpty) {
                        throw new ClassNotFoundException(name);
                    }
                }
            }
        } finally {
            synchronized (this.captures) {
                this.captures.remove(internalName);
            }
        }
        
        if (captured == null) {
            throw new ClassNotFoundException("Unable to obtain ClassNode for: " + name);
        }
        
        return captured;
    }

}
