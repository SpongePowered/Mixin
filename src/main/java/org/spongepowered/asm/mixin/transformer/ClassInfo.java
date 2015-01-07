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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import com.google.common.collect.ImmutableList;


/**
 * Information about a class, used as a way of keeping track of class hierarchy information needed to support mixins with detached targets.  
 */
class ClassInfo extends TreeInfo {
    
    /**
     * Loading and parsing classes is expensive, so keep a cache of all the information we generate
     */
    private static final Map<String, ClassInfo> cache = new HashMap<String, ClassInfo>();
    
    static {
        ClassInfo.cache.put("java/lang/Object", new ClassInfo());
    }
    
    /**
     * Class name (binary name)
     */
    private final String name;
    
    /**
     * Class superclass name (binary name)
     */
    private final String superName;
    
    /**
     * Public and protected methods (instance) methods in this class 
     */
    private final List<String> methods;
    
    /**
     * Superclass reference, not initialised until required 
     */
    private ClassInfo superClass;
    
    /**
     * Private constructor used to initialise the ClassInfo for {@link Object}
     */
    private ClassInfo() {
        this.name = "java/lang/Object";
        this.superName = null;
        this.methods = ImmutableList.<String>of (
            "getClass()Ljava/lang/Class;",
            "hashCode()I",
            "equals(Ljava/lang/Object;)Z",
            "clone()Ljava/lang/Object;",
            "toString()Ljava/lang/String;",
            "notify()V",
            "notifyAll()V",
            "wait(J)V",
            "wait(JI)V",
            "wait()V",
            "finalize()V"
        );
    }
    
    /**
     * Initialise a ClassInfo from the supplied {@link ClassNode}
     * 
     * @param classNode Class node to inspect
     */
    private ClassInfo(ClassNode classNode) {
        if ((classNode.access & Opcodes.ACC_INTERFACE) != 0) {
            throw new RuntimeException("Unexpected hierarchy: " + classNode.name + " is an interface");
        }
        
        this.name = classNode.name;
        this.superName = classNode.superName;
        this.methods = new ArrayList<String>();
        
        for (MethodNode method : classNode.methods) {
            if (!method.name.startsWith("<")
                    && (method.access & Opcodes.ACC_PRIVATE) == 0
                    && (method.access & Opcodes.ACC_STATIC) == 0) {
                this.methods.add(method.name + method.desc);
            }
        }
    }
    
    /**
     * Get the class name (binary name)
     */
    public String getName() {
        return this.name;
    }
    
    /**
     * Get the superclass name (binary name)
     */
    public String getSuperName() {
        return this.superName;
    }
    
    /**
     * Get the superclass info, can return null if the superclass cannot be resolved
     */
    public ClassInfo getSuperClass() {
        if (this.superClass == null && this.superName != null) {
            this.superClass = ClassInfo.forName(this.superName);
        }
        
        return this.superClass;
    }
    
    /**
     * Test whether this class has the specified superclass in its hierarchy
     *  
     * @return true if the specified class appears in the class's hierarchy anywhere
     */
    public boolean hasSuperClass(String superClass) {
        if ("java/lang/Object".equals(superClass)) {
            return true;
        }
        
        ClassInfo superClassInfo = this.getSuperClass();
        while (superClassInfo != null) {
            if (superClass.equals(superClassInfo.getName())) {
                return true;
            }
            
            superClassInfo = superClassInfo.getSuperClass();
        }
        
        return false;
    }

    /**
     * Finds the owner of the specified private or protected method in this class's hierarchy
     * 
     * @param name Method name to search for
     * @param desc Method descriptor
     * @param includeThisClass True to return this class if the method exists here, or false to search only superclasses
     * @return the name of the class which contains the specified method or null if the method could not be resolved
     */
    public String findMethodInHierarchy(String name, String desc, boolean includeThisClass) {
        if (includeThisClass && this.hasMethod(name, desc)) {
            return this.name;
        }
        
        ClassInfo superClassInfo = this.getSuperClass();
        if (superClassInfo != null) {
            return superClassInfo.findMethodInHierarchy(name, desc, true);
        }
        
        return null;
    }
    
    /**
     * Get whether this class has a public or protected non-static method
     * 
     * @param name Method name to search for
     * @param desc Method descriptor
     * @return true if the method is defined in this class or false otherwise
     */
    public boolean hasMethod(String name, String desc) {
        return this.methods.contains(name + desc);
    }

    /**
     * Return a ClassInfo for the supplied {@link ClassNode}. If a ClassInfo for the class was already defined, then the original ClassInfo is
     * returned from the internal cache. Otherwise a new ClassInfo is created and returned.
     * 
     * @param classNode
     * @return
     */
    public static ClassInfo fromClassNode(ClassNode classNode) {
        ClassInfo info = ClassInfo.cache.get(classNode.name);
        if (info == null) {
            info = new ClassInfo(classNode);
            ClassInfo.cache.put(classNode.name, info);
        }
        
        return info;
    }

    /**
     * Return a ClassInfo for the specified class name, fetches the ClassInfo from the cache where possible
     * 
     * @param className Binary name of the class to look up
     * @return ClassInfo for the specified class name or null if the specified name cannot be resolved for some reason
     */
    public static ClassInfo forName(String className) {
        className = className.replace('.', '/');
        
        ClassInfo info = ClassInfo.cache.get(className);
        if (info == null) {
            try {
                ClassNode classNode = TreeInfo.getClassNode(className);
                info = new ClassInfo(classNode);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            
            // Put null in the cache if load failed
            ClassInfo.cache.put(className, info);
        }
        
        return info;
    }
}
