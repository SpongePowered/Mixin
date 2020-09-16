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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.IClassProcessor;
import org.spongepowered.asm.launch.Phases;
import org.spongepowered.asm.service.IClassTracker;

import cpw.mods.modlauncher.api.ITransformerActivity;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService.Phase;

/**
 * Tracks invalid (unloadable) classes so we can throw an exception inside the
 * TCL and class load events so we can report when classes were loaded before
 * we could transform them
 */
public class ModLauncherClassTracker implements IClassProcessor, IClassTracker {
    
    private final Set<String> invalidClasses = new HashSet<String>();
    
    private final Set<String> loadedClasses = new HashSet<String>();
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IClassTracker#registerInvalidClass(
     *      java.lang.String)
     */
    @Override
    public void registerInvalidClass(String className) {
        synchronized (this.invalidClasses) {
            this.invalidClasses.add(className);
        }
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IClassTracker#isClassLoaded(
     *      java.lang.String)
     */
    @Override
    public boolean isClassLoaded(String className) {
        synchronized (this.loadedClasses) {
            return this.loadedClasses.contains(className);
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IClassTracker#getClassRestrictions(
     *      java.lang.String)
     */
    @Override
    public String getClassRestrictions(String className) {
        return ""; // TODO ?
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.IClassProcessor#handlesClass(
     *      org.objectweb.asm.Type, boolean, java.lang.String)
     */
    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty, String reason) {
        String name = classType.getClassName();
        synchronized (this.invalidClasses) {
            if (this.invalidClasses.contains(name)) {
                throw new NoClassDefFoundError(String.format("%s is invalid", name));
            }
        }
        
        return Phases.AFTER_ONLY;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.IClassProcessor#processClass(
     *      cpw.mods.modlauncher.serviceapi.ILaunchPluginService.Phase,
     *      org.objectweb.asm.tree.ClassNode, org.objectweb.asm.Type,
     *      java.lang.String)
     */
    @Override
    public boolean processClass(Phase phase, ClassNode classNode, Type classType, String reason) {
        // Only track the classload if the reason is actually classloading
        if (ITransformerActivity.CLASSLOADING_REASON.equals(reason)) {
            synchronized (this.loadedClasses) {
                this.loadedClasses.add(classType.getClassName());
            }
        }
        
        return false;
    }
        
}
