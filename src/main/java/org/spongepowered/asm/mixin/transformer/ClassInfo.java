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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import com.google.common.collect.ImmutableList;


/**
 * Information about a class, used as a way of keeping track of class hierarchy information needed to support mixins with detached targets.  
 */
class ClassInfo extends TreeInfo {
    
    private static final String JAVA_LANG_OBJECT = "java/lang/Object";

    /**
     * Loading and parsing classes is expensive, so keep a cache of all the information we generate
     */
    private static final Map<String, ClassInfo> cache = new HashMap<String, ClassInfo>();
    
    private static final ClassInfo OBJECT = new ClassInfo();
    
    static {
        ClassInfo.cache.put(ClassInfo.JAVA_LANG_OBJECT, ClassInfo.OBJECT);
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
     * Outer class name
     */
    private final String outerName;
    
    /**
     * Interfaces
     */
    private final List<String> interfaces;
    
    /**
     * Public and protected methods (instance) methods in this class 
     */
    private final List<String> methods;
    
    /**
     * True if this is an interface 
     */
    private final boolean isInterface;
    
    /**
     * Access flags
     */
    private final int access;
    
    /**
     * Superclass reference, not initialised until required 
     */
    private ClassInfo superClass;
    
    /**
     * Outer class reference, not initialised until required 
     */
    private ClassInfo outerClass;
    
    /**
     * Private constructor used to initialise the ClassInfo for {@link Object}
     */
    private ClassInfo() {
        this.name = ClassInfo.JAVA_LANG_OBJECT;
        this.superName = null;
        this.outerName = null;
        this.methods = ImmutableList.<String>of(
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
        this.isInterface = false;
        this.interfaces = Collections.<String>emptyList();
        this.access = Opcodes.ACC_PUBLIC;
    }
    
    /**
     * Initialise a ClassInfo from the supplied {@link ClassNode}
     * 
     * @param classNode Class node to inspect
     */
    private ClassInfo(ClassNode classNode) {
        this.name = classNode.name;
        this.superName = classNode.superName != null ? classNode.superName : ClassInfo.JAVA_LANG_OBJECT;
        this.methods = new ArrayList<String>();
        this.isInterface = ((classNode.access & Opcodes.ACC_INTERFACE) != 0);
        this.interfaces = Collections.unmodifiableList(classNode.interfaces);
        this.access = classNode.access;
        
        for (MethodNode method : classNode.methods) {
            if (!method.name.startsWith("<")
                    && (method.access & Opcodes.ACC_PRIVATE) == 0
                    && (method.access & Opcodes.ACC_STATIC) == 0) {
                this.methods.add(method.name + method.desc);
            }
        }

        String outerName = classNode.outerClass;
        if (outerName == null) {
            for (FieldNode field : classNode.fields) {
                if ((field.access & Opcodes.ACC_SYNTHETIC) != 0 && field.name.startsWith("this$")) {
                    outerName = field.desc;
                    if (outerName.startsWith("L")) {
                        outerName = outerName.substring(1, outerName.length() - 1);
                    }
                }
            }
        }
        
        this.outerName = outerName ;
    }
    
    /**
     * Get whether this class has ACC_PUBLIC
     */
    public boolean isPublic() {
        return (this.access & Opcodes.ACC_PUBLIC) != 0;
    }
    
    /**
     * Get whether this class is an inner class
     */
    public boolean isInner() {
        return this.outerName != null;
    }
    
    /**
     * Get whether this is an interface or not
     */
    public boolean isInterface() {
        return this.isInterface;
    }
    
    /**
     * Returns the answer to life, the universe and everything
     */
    public List<String> getInterfaces() {
        return this.interfaces;
    }
    
    @Override
    public String toString() {
        return this.name;
    }
    
    public int getAccess() {
        return this.access;
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
     * Get the name of the outer class, or null if this is not an inner class
     */
    public String getOuterName() {
        return this.outerName;
    }
    
    /**
     * Get the outer class info, can return null if the outer class cannot be resolved or if this is not an inner class
     */
    public ClassInfo getOuterClass() {
        if (this.outerClass == null && this.outerName != null) {
            this.outerClass = ClassInfo.forName(this.outerName);
        }
        
        return this.outerClass;
    }
    
    /**
     * Test whether this class has the specified superclass in its hierarchy
     *  
     * @return true if the specified class appears in the class's hierarchy anywhere
     */
    public boolean hasSuperClass(String superClass) {
        if (ClassInfo.JAVA_LANG_OBJECT.equals(superClass)) {
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
    
    public boolean isAssignableFrom(ClassInfo superClass) {
        if (ClassInfo.OBJECT == superClass) {
            return true;
        }
        
        ClassInfo superClassInfo = this.getSuperClass();
        while (superClassInfo != null) {
            if (superClass == superClassInfo) {
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
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ClassInfo)) {
            return false;
        }
        return ((ClassInfo)other).name.equals(this.name);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.name.hashCode();
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
