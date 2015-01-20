/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered.org <http://www.spongepowered.org>
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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.InvalidMixinException;
import org.spongepowered.asm.util.ASMHelper;


/**
 * Information about an interface being runtime-patched onto a mixin target
 * class, see {@link Implements}
 */
public class InterfaceInfo extends TreeInfo {
    
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
     * Method signatures in the interface, lazy loaded
     */
    private Set<String> methods;

    /**
     * Make with the new thing already
     * 
     * @param prefix Method prefix
     * @param iface Interface to load
     */
    private InterfaceInfo(String prefix, Type iface) {
        if (prefix == null || prefix.length() < 2 || !prefix.endsWith("$")) {
            throw new InvalidMixinException(String.format("Prefix %s for iface %s is not valid", prefix, iface.toString()));
        }
        
        this.prefix = prefix;
        this.iface = iface;
    }
    
    /**
     * Lazy-loaded methods collection initialiser
     */
    private void initMethods() {
        this.methods = new HashSet<String>();
        this.readInterface(this.iface.getClassName());
    }
    
    /**
     * Reads an interface and its super-interfaces and gathers method names in
     * to the local "methods" collection
     * 
     * @param ifaceName Name of the interface to read
     */
    private void readInterface(String ifaceName) {
        ClassNode ifaceNode = new ClassNode();
        try {
            ClassReader classReader = new ClassReader(TreeInfo.loadClass(ifaceName, true));
            classReader.accept(ifaceNode, 0);
        } catch (Exception ex) {
            throw new InvalidMixinException("An error was encountered parsing the interface " + this.iface.toString());
        }
        
        for (MethodNode ifaceMethod : ifaceNode.methods) {
            String signature = ifaceMethod.name + ifaceMethod.desc;
            this.methods.add(signature);
        }
        
        for (String superIface : ifaceNode.interfaces) {
            String sif = superIface.replace('/', '.');
            this.readInterface(sif);
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
    public String getInternalName() {
        return this.iface.getInternalName();
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
            return false;
        }
        
        String realName = method.name.substring(this.prefix.length());
        String signature = realName + method.desc;
        if (!this.methods.contains(signature)) {
            throw new InvalidMixinException(String.format("%s does not exist in target interface %s", realName, this.iface.toString()));
        }
        
        method.name = realName;
        return true;
    }

    /**
     * Convert an {@link Interface} annotation node into an
     * {@link InterfaceInfo}
     * 
     * @param node Annotation node to process
     * @return parsed InterfaceInfo object
     */
    static InterfaceInfo fromAnnotation(AnnotationNode node) {
        String prefix = ASMHelper.<String>getAnnotationValue(node, "prefix");
        Type iface = ASMHelper.<Type>getAnnotationValue(node, "iface");
        
        if (prefix == null || iface == null) {
            throw new InvalidMixinException(String.format("@Interface annotation on is missing a required parameter"));
        }
        
        return new InterfaceInfo(prefix, iface);
    }
}
