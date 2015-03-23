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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;


/**
 * Information about a class, used as a way of keeping track of class hierarchy
 * information needed to support more complex mixin behaviour such as detached
 * superclass and mixin inheritance.  
 */
class ClassInfo extends TreeInfo {
    
    /**
     * <p>To all intents and purposes, the "real" class hierarchy and the mixin
     * class hierarchy exist in parallel, this means that for some hierarchy
     * validation operations we need to walk <em>across</em> to the other
     * hierarchy in order to allow meaningful validation to occur.</p>
     * 
     * <p>This enum defines the type of traversal operations which are allowed
     * for a particular lookup.</p>
     *  
     * <p>Each traversal type has a <code>next</code> property which defines
     * the traversal type to use on the <em>next</em> step of the hierarchy
     * validation. For example, the type {@link #IMMEDIATE} which requires an 
     * immediate match falls through to {@link #NONE} on the next step, which
     * prevents further traversals from occurring in the lookup.</p>
     */
    static enum Traversal {
        
        /**
         * No traversals are allowed.
         */
        NONE(null, false),
        
        /**
         * Traversal is allowed at all stages.
         */
        ALL(null, true),
        
        /**
         * Traversal is allowed at the bottom of the hierarchy but no further.
         */
        IMMEDIATE(Traversal.NONE, true),
        
        /**
         * Traversal is allowed only on superclasses and not at the bottom of
         * the hierarchy.
         */
        SUPER(Traversal.ALL, false);
        
        private final Traversal next;
        
        private final boolean traverse;
        
        private Traversal(Traversal next, boolean traverse) {
            this.next = next != null ? next : this;
            this.traverse = traverse;
        }
        
        public Traversal next() {
            return this.next;
        }
        
        public boolean canTraverse() {
            return this.traverse;
        }
    }
    
    /**
     * Information about a method in this class
     */
    class Method {
        
        /**
         * The original name of the method 
         */
        private final String methodName;
        
        /**
         * The method signature
         */
        private final String methodDesc;
        
        /**
         * True if this method was injected by a mixin, false if it was
         * originally part of the class
         */
        private final boolean isInjected;
        
        /**
         * Current name of the method, may be different from {@link methodName}
         * if the method has been renamed
         */
        private String currentName;
        
        public Method(MethodNode method) {
            this(method, false);
        }
        
        public Method(Method method) {
            this(method.methodName, method.methodDesc, method.isInjected);
            this.currentName = method.currentName;
        }
        
        public Method(MethodNode method, boolean injected) {
            this(method.name, method.desc, injected);
        }

        public Method(String name, String desc) {
            this(name, desc, false);
        }

        public Method(String name, String desc, boolean injected) {
            this.methodName = name;
            this.methodDesc = desc;
            this.isInjected = injected;
            this.currentName = name;
        }
        
        public String getOriginalName() {
            return this.methodName;
        }
        
        public String getName() {
            return this.currentName;
        }
        
        public String getDesc() {
            return this.methodDesc;
        }
        
        public boolean isInjected() {
            return this.isInjected;
        }
        
        public boolean isRenamed() {
            return this.currentName != this.methodName;
        }
        
        public ClassInfo getOwner() {
            return ClassInfo.this;
        }
        
        public void renameTo(String name) {
            this.currentName = name;
        }
        
        public boolean equals(String name, String desc) {
            return (this.methodName.equals(name)
                    || this.currentName.equals(name))
                    && this.methodDesc.equals(desc);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Method)) {
                return false;
            }
            
            Method other = (Method)obj;
            return (other.methodName.equals(this.methodName) 
                    || other.currentName.equals(this.currentName))
                    && other.methodDesc.equals(this.methodDesc);
        }
        
        @Override
        public int hashCode() {
            return this.toString().hashCode();
        }
        
        @Override
        public String toString() {
            return this.methodName + this.methodDesc;
        }
    }
    
    private static final String JAVA_LANG_OBJECT = "java/lang/Object";

    /**
     * Loading and parsing classes is expensive, so keep a cache of all the
     * information we generate
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
    private final Set<Method> methods;
    
    /**
     * Public and protected fields in this class
     */
    private final List<String> fields;
    
    /**
     * Mixins which target this class
     */
    private final Set<MixinInfo> mixins = new HashSet<MixinInfo>();
    
    /**
     * Map of mixin types to corresponding supertypes, to avoid repeated 
     * lookups 
     */
    private final Map<ClassInfo, ClassInfo> correspondingTypes = new HashMap<ClassInfo, ClassInfo>();
    
    /**
     * Mixin info if this class is a mixin itself 
     */
    private final MixinInfo mixin;
    
    /**
     * True if this is a mixin rather than a class 
     */
    private final boolean isMixin;
    
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
        this.methods = ImmutableSet.<Method>of(
            new Method("getClass", "()Ljava/lang/Class;"),
            new Method("hashCode", "()I"),
            new Method("equals", "(Ljava/lang/Object;)Z"),
            new Method("clone", "()Ljava/lang/Object;"),
            new Method("toString", "()Ljava/lang/String;"),
            new Method("notify", "()V"),
            new Method("notifyAll", "()V"),
            new Method("wait", "(J)V"),
            new Method("wait", "(JI)V"),
            new Method("wait", "()V"),
            new Method("finalize", "()V")
        );
        this.fields = Collections.<String>emptyList();
        this.isInterface = false;
        this.interfaces = Collections.<String>emptyList();
        this.access = Opcodes.ACC_PUBLIC;
        this.isMixin = false;
        this.mixin = null;
    }
    
    /**
     * Initialise a ClassInfo from the supplied {@link ClassNode}
     * 
     * @param classNode Class node to inspect
     */
    private ClassInfo(ClassNode classNode) {
        this.name = classNode.name;
        this.superName = classNode.superName != null ? classNode.superName : ClassInfo.JAVA_LANG_OBJECT;
        this.methods = new HashSet<Method>();
        this.fields = new ArrayList<String>();
        this.isInterface = ((classNode.access & Opcodes.ACC_INTERFACE) != 0);
        this.interfaces = Collections.unmodifiableList(classNode.interfaces);
        this.access = classNode.access;
        this.isMixin = classNode instanceof MixinClassNode;
        this.mixin = this.isMixin ? ((MixinClassNode)classNode).getMixin() : null;
        
        for (MethodNode method : classNode.methods) {
            this.addMethod(method, false);
        }

        String outerName = classNode.outerClass;
        if (outerName == null) {
            for (FieldNode field : classNode.fields) {
                if ((field.access & Opcodes.ACC_SYNTHETIC) != 0) {
                    if (field.name.startsWith("this$")) { 
                        outerName = field.desc;
                        if (outerName.startsWith("L")) {
                            outerName = outerName.substring(1, outerName.length() - 1);
                        }
                    }
                } else if ((field.access & Opcodes.ACC_PRIVATE) == 0) {
                    this.fields.add(field.name + "()" + field.desc);
                }
            }
        }
        
        this.outerName = outerName;
    }

    void addMethod(MethodNode method) {
        this.addMethod(method, true);
    }

    private void addMethod(MethodNode method, boolean injected) {
        if (!method.name.startsWith("<")
                && (method.access & Opcodes.ACC_PRIVATE) == 0
                && (method.access & Opcodes.ACC_STATIC) == 0) {
            this.methods.add(new Method(method, injected));
        }
    }
    
    /**
     * Add a mixin which targets this class
     */
    void addMixin(MixinInfo mixin) {
        if (this.isMixin) {
            throw new IllegalArgumentException("Cannot add target " + this.name + " for " + mixin.getClassName() + " because the target is a mixin");
        }
        this.mixins.add(mixin);
    }
    
    /**
     * Get all mixins which target this class
     */
    public Set<MixinInfo> getMixins() {
        return Collections.unmodifiableSet(this.mixins);
    }
    
    /**
     * Get whether this class is a mixin
     */
    public boolean isMixin() {
        return this.isMixin;
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
     * Get the superclass info, can return null if the superclass cannot be
     * resolved
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
     * Get the outer class info, can return null if the outer class cannot be
     * resolved or if this is not an inner class
     */
    public ClassInfo getOuterClass() {
        if (this.outerClass == null && this.outerName != null) {
            this.outerClass = ClassInfo.forName(this.outerName);
        }
        
        return this.outerClass;
    }
    
    /**
     * Class targets
     */
    protected List<ClassInfo> getTargets() {
        if (this.mixin != null) {
            List<ClassInfo> targets = new ArrayList<ClassInfo>();
            targets.add(this);
            targets.addAll(this.mixin.getTargets());
            return targets;
        }
        
        return ImmutableList.<ClassInfo>of(this);
    }
    
    /**
     * Get class/interface methods
     * 
     * @return read-only view of class methods
     */
    public Set<Method> getMethods() {
        return Collections.unmodifiableSet(this.methods);
    }
    
    /**
     * Test whether this class has the specified superclass in its hierarchy
     * 
     * @param superClass Name of the superclass to search for in the hierarchy
     * @return true if the specified class appears in the class's hierarchy
     * anywhere
     */
    public boolean hasSuperClass(String superClass) {
        return this.hasSuperClass(superClass, Traversal.NONE);
    }

    /**
     * Test whether this class has the specified superclass in its hierarchy
     * 
     * @param superClass Name of the superclass to search for in the hierarchy
     * @param traversal Traversal type to allow during this lookup
     * @return true if the specified class appears in the class's hierarchy
     * anywhere
     */
    public boolean hasSuperClass(String superClass, Traversal traversal) {
        if (ClassInfo.JAVA_LANG_OBJECT.equals(superClass)) {
            return true;
        }
        
        return this.findSuperClass(superClass, traversal) != null;
    }
    
    /**
     * Test whether this class has the specified superclass in its hierarchy
     * 
     * @param superClass Superclass to search for in the hierarchy
     * @return true if the specified class appears in the class's hierarchy
     * anywhere
     */
    public boolean hasSuperClass(ClassInfo superClass) {
        return this.hasSuperClass(superClass, Traversal.NONE);
    }

    /**
     * Test whether this class has the specified superclass in its hierarchy
     * 
     * @param superClass Superclass to search for in the hierarchy
     * @param traversal Traversal type to allow during this lookup
     * @return true if the specified class appears in the class's hierarchy
     * anywhere
     */
    public boolean hasSuperClass(ClassInfo superClass, Traversal traversal) {
        if (ClassInfo.OBJECT == superClass) {
            return true;
        }

        return this.findSuperClass(superClass.name, traversal) != null;
    }

    /**
     * Search for the specified superclass in this class's hierarchy. If found
     * returns the ClassInfo, otherwise returns null
     * 
     * @param superClass Superclass name to search for
     * @return Matched superclass or null if not found 
     */
    public ClassInfo findSuperClass(String superClass) {
        return this.findSuperClass(superClass, Traversal.NONE);
    }

    /**
     * Search for the specified superclass in this class's hierarchy. If found
     * returns the ClassInfo, otherwise returns null
     * 
     * @param superClass Superclass name to search for
     * @param traversal Traversal type to allow during this lookup
     * @return Matched superclass or null if not found 
     */
    public ClassInfo findSuperClass(String superClass, Traversal traversal) {
        ClassInfo superClassInfo = this.getSuperClass();
        if (superClassInfo != null) {
            List<ClassInfo> targets = superClassInfo.getTargets();
            for (ClassInfo superTarget : targets) {
                if (superClass.equals(superTarget.getName())) {
                    return superClassInfo;
                }

                ClassInfo found = superTarget.findSuperClass(superClass, traversal.next());
                if (found != null) {
                    return found;
                }
            }
        }

        if (traversal.canTraverse()) {
            for (MixinInfo mixin : this.mixins) {
                ClassInfo targetSuper = mixin.getClassInfo().findSuperClass(superClass, traversal);
                if (targetSuper != null) {
                    return targetSuper;
                }
            }
        }
        
        return null;
    }

    /**
     * Walks up this class's hierarchy to find the first class targetted by the
     * specified mixin. This is used during mixin application to translate a
     * mixin reference to a "real class" reference <em>in the context of <b>this
     * </b> class</em>.
     * 
     * @param mixin Mixin class to search for
     * @return corresponding (target) class for the specified mixin or null if
     *      no corresponding mixin was found
     */
    ClassInfo findCorrespondingType(ClassInfo mixin) {
        if (mixin == null || !mixin.isMixin || this.isMixin) {
            return null;
        }
        
        ClassInfo correspondingType = this.correspondingTypes.get(mixin);
        if (correspondingType == null) {
            correspondingType = this.findSuperTypeForMixin(mixin);
            this.correspondingTypes.put(mixin, correspondingType);
        }
        return correspondingType;
    }

    /* (non-Javadoc)
     * Only used by findCorrespondingType(), used as a convenience so that
     * sanity checks and caching can be handled more elegantly
     */
    private ClassInfo findSuperTypeForMixin(ClassInfo mixin) {
        ClassInfo superClass = this;
        
        while (superClass != null && superClass != ClassInfo.OBJECT) {
            for (MixinInfo minion : superClass.mixins) {
                if (minion.getClassInfo().equals(mixin)) {
                    return superClass;
                }
            }
            
            superClass = superClass.getSuperClass();
        }
        
        return null;
    }

    /**
     * Find out whether this (mixin) class has another mixin in its superclass
     * hierarchy. This method always returns false for non-mixin classes.
     * 
     * @return true if and only if one or more mixins are found in the hierarchy
     *      of this mixin
     */
    public boolean hasMixinInHierarchy() {
        if (!this.isMixin) {
            return false;
        }
        
        ClassInfo superClass = this.getSuperClass();
        
        while (superClass != null && superClass != ClassInfo.OBJECT) {
            if (superClass.isMixin) {
                return true;
            }
            superClass = superClass.getSuperClass();
        }

        return false;
    }
    
    /**
     * Find out whether this (non-mixin) class has a mixin targetting
     * <em>any</em> of its superclasses. This method always returns false for
     * mixin classes.
     * 
     * @return true if and only if one or more classes in this class's hierarchy
     *      are targetted by a mixin
     */
    public boolean hasMixinTargetInHierarchy() {
        if (this.isMixin) {
            return false;
        }
        
        ClassInfo superClass = this.getSuperClass();
        
        while (superClass != null && superClass != ClassInfo.OBJECT) {
            if (superClass.mixins.size() > 0) {
                return true;
            }
            superClass = superClass.getSuperClass();
        }
        
        return false;
    }

    /**
     * Finds the specified private or protected method in this class's hierarchy
     * 
     * @param method Method to search for
     * @param includeThisClass True to return this class if the method exists
     *      here, or false to search only superclasses
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethodInHierarchy(MethodNode method, boolean includeThisClass) {
        return this.findMethodInHierarchy(method.name, method.desc, includeThisClass);
    }
    
    /**
     * Finds the specified public or protected method in this class's hierarchy
     * 
     * @param method Method to search for
     * @param includeThisClass True to return this class if the method exists
     *      here, or false to search only superclasses
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethodInHierarchy(MethodInsnNode method, boolean includeThisClass) {
        return this.findMethodInHierarchy(method.name, method.desc, includeThisClass);
    }
    
    /**
     * Finds the specified public or protected method in this class's hierarchy
     * 
     * @param name Method name to search for
     * @param desc Method descriptor
     * @param includeThisClass True to return this class if the method exists
     *      here, or false to search only superclasses
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethodInHierarchy(String name, String desc, boolean includeThisClass) {
        return this.findMethodInHierarchy(name, desc, includeThisClass, Traversal.NONE);
    }

    /**
     * Finds the specified public or protected method in this class's hierarchy
     * 
     * @param name Method name to search for
     * @param desc Method descriptor
     * @param includeThisClass True to return this class if the method exists
     *      here, or false to search only superclasses
     * @param traversal Traversal type to allow during this lookup
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethodInHierarchy(String name, String desc, boolean includeThisClass, Traversal traversal) {
        if (includeThisClass) {
            Method method = this.findMethod(name, desc);
            if (method != null) {
                return method;
            }
            
            if (traversal.canTraverse()) {
                for (MixinInfo mixin : this.mixins) {
                    Method mixinMethod = mixin.getClassInfo().findMethod(name, desc);
                    if (mixinMethod != null) {
                        return new Method(mixinMethod);
                    }
                }               
            }
        }
        
        ClassInfo superClassInfo = this.getSuperClass();
        if (superClassInfo != null) {
            for (ClassInfo superTarget : superClassInfo.getTargets()) {
                Method method = superTarget.findMethodInHierarchy(name, desc, true, traversal.next());
                if (method != null) {
                    return method;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Finds the specified public or protected method in this class
     * 
     * @param method Method to search for
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethod(MethodNode method) {
        return this.findMethod(method.name, method.desc);
    }
    
    /**
     * Finds the specified public or protected method in this class
     * 
     * @param method Method to search for
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethod(MethodInsnNode method) {
        return this.findMethod(method.name, method.desc);
    }
    
    /**
     * Finds the specified public or protected method in this class
     * 
     * @param method Method name to search for
     * @param desc Method signature to search for
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethod(String name, String desc) {
        for (Method method : this.methods) {
            if (method.equals(name, desc)) {
                return method;
            }
        }
        
        return null;
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
     * Return a ClassInfo for the supplied {@link ClassNode}. If a ClassInfo for
     * the class was already defined, then the original ClassInfo is returned
     * from the internal cache. Otherwise a new ClassInfo is created and
     * returned.
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
     * Return a ClassInfo for the specified class name, fetches the ClassInfo
     * from the cache where possible
     * 
     * @param className Binary name of the class to look up
     * @return ClassInfo for the specified class name or null if the specified
     *      name cannot be resolved for some reason
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
