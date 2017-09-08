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
package org.spongepowered.asm.mixin.transformer;

import java.util.HashSet;
import java.util.Set;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Method;
import org.spongepowered.asm.mixin.transformer.meta.MixinRenamed;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.util.Annotations;

/**
 * Information about an interface being runtime-patched onto a mixin target
 * class, see {@link org.spongepowered.asm.mixin.Implements Implements}
 */
public final class InterfaceInfo {
    
    /**
     * Parent mixin 
     */
    private final MixinInfo mixin;

    /**
     * Prefix for interface methods. Any methods using this prefix must exist in
     * the target interface
     */
    private final String prefix;
    
    /**
     * Interface being patched
     */
    private final Type iface;
    
    /**
     * True if all methods implementing this interface should be treated as
     * unique
     */
    private final boolean unique;
    
    /**
     * Method signatures in the interface, lazy loaded
     */
    private Set<String> methods;

    /**
     * Make with the new thing already
     * 
     * @param mixin Parent mixin
     * @param prefix Method prefix
     * @param iface Interface to load
     */
    private InterfaceInfo(MixinInfo mixin, String prefix, Type iface, boolean unique) {
        if (prefix == null || prefix.length() < 2 || !prefix.endsWith("$")) {
            throw new InvalidMixinException(mixin, String.format("Prefix %s for iface %s is not valid", prefix, iface.toString()));
        }
        
        this.mixin = mixin;
        this.prefix = prefix;
        this.iface = iface;
        this.unique = unique;
    }
    
    /**
     * Lazy-loaded methods collection initialiser
     */
    private void initMethods() {
        this.methods = new HashSet<String>();
        this.readInterface(this.iface.getInternalName());
    }
    
    /**
     * Reads an interface and its super-interfaces and gathers method names in
     * to the local "methods" collection
     * 
     * @param ifaceName Name of the interface to read
     */
    private void readInterface(String ifaceName) {
        ClassInfo interfaceInfo = ClassInfo.forName(ifaceName);
        
        for (Method ifaceMethod : interfaceInfo.getMethods()) {
            this.methods.add(ifaceMethod.toString());
        }
        
        for (String superIface : interfaceInfo.getInterfaces()) {
            this.readInterface(superIface);
        }
    }

    /**
     * Get the prefix string (non null)
     * 
     * @return the prefix
     */
    public String getPrefix() {
        return this.prefix;
    }
    
    /**
     * Get the interface type
     * 
     * @return interface type
     */
    public Type getIface() {
        return this.iface;
    }
    
    /**
     * Get the internal name of the interface
     * 
     * @return the internal name for the interface
     */
    public String getName() {
        return this.iface.getClassName();
    }

    /**
     * Get the internal name of the interface
     * 
     * @return the internal name for the interface
     */
    public String getInternalName() {
        return this.iface.getInternalName();
    }
    
    /**
     * Get whether all methods for this interface should be treated as unique
     * 
     * @return true to treat all member methods as unique
     */
    public boolean isUnique() {
        return this.unique;
    }

    /**
     * Processes a method node in the mixin and renames it if necessary. If the
     * prefix is found then we verify that the method exists in the target
     * interface and throw our teddies out of the pram if that's not the case
     * (replacement behaviour for {@link Override} essentially.
     * 
     * @param method Method to rename
     * @return true if the method was remapped
     */
    public boolean renameMethod(MethodNode method) {
        if (this.methods == null) {
            this.initMethods();
        }
        
        if (!method.name.startsWith(this.prefix)) {
            if (this.methods.contains(method.name + method.desc)) {
                this.decorateUniqueMethod(method);
            }
            return false;
        }
        
        String realName = method.name.substring(this.prefix.length());
        String signature = realName + method.desc;
        
        if (!this.methods.contains(signature)) {
            throw new InvalidMixinException(this.mixin, String.format("%s does not exist in target interface %s", realName, this.getName()));
        }
        
        if ((method.access & Opcodes.ACC_PUBLIC) == 0) {
            throw new InvalidMixinException(this.mixin, String.format("%s cannot implement %s because it is not visible", realName, this.getName()));
        }
        
        Annotations.setVisible(method, MixinRenamed.class, "originalName", method.name, "isInterfaceMember", true);
        this.decorateUniqueMethod(method);
        method.name = realName;
        return true;
    }

    /**
     * Decorate the target method with {@link Unique} if the interface is marked
     * as unique
     * 
     * @param method method to decorate
     */
    private void decorateUniqueMethod(MethodNode method) {
        if (!this.unique) {
            return;
        }
        
        if (Annotations.getVisible(method, Unique.class) == null) {
            Annotations.setVisible(method, Unique.class);
            this.mixin.getClassInfo().findMethod(method).setUnique(true);
        }
    }

    /**
     * Convert an {@link Interface} annotation node into an
     * {@link InterfaceInfo}
     * 
     * @param mixin Parent mixin
     * @param node Annotation node to process
     * @return parsed InterfaceInfo object
     */
    static InterfaceInfo fromAnnotation(MixinInfo mixin, AnnotationNode node) {
        String prefix = Annotations.<String>getValue(node, "prefix");
        Type iface = Annotations.<Type>getValue(node, "iface");
        Boolean unique = Annotations.<Boolean>getValue(node, "unique");
        
        if (prefix == null || iface == null) {
            throw new InvalidMixinException(mixin, String.format("@Interface annotation on %s is missing a required parameter", mixin));
        }
        
        return new InterfaceInfo(mixin, prefix, iface, unique != null && unique.booleanValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        InterfaceInfo that = (InterfaceInfo) o;

        return this.mixin.equals(that.mixin) && this.prefix.equals(that.prefix) && this.iface.equals(that.iface);
    }

    @Override
    public int hashCode() {
        int result = this.mixin.hashCode();
        result = 31 * result + this.prefix.hashCode();
        result = 31 * result + this.iface.hashCode();
        return result;
    }
}
