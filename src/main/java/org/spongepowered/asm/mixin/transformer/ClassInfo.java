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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Member.Type;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;


/**
 * Information about a class, used as a way of keeping track of class hierarchy
 * information needed to support more complex mixin behaviour such as detached
 * superclass and mixin inheritance.  
 */
public class ClassInfo extends TreeInfo {
    
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
    public static enum Traversal {
        
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
     * Information about frames in a method
     */
    public static class FrameData {
        
        private static final String[] FRAMETYPES = { "NEW", "FULL", "APPEND", "CHOP", "SAME", "SAME1" };
        
        public final int index;
        
        public final int type;
        
        public final int locals;

        FrameData(int index, int type, int locals) {
            this.index = index;
            this.type = type;
            this.locals = locals;
        }
        
        FrameData(int index, FrameNode frameNode) {
            this.index = index;
            this.type = frameNode.type;
            this.locals = frameNode.local != null ? frameNode.local.size() : 0;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return String.format("FrameData[index=%d, type=%s, locals=%d]", this.index, FrameData.FRAMETYPES[this.type + 1], this.locals);
        }
    }
    
    /**
     * Information about a member in this class
     */
    abstract static class Member {
        
        static enum Type {
            METHOD,
            FIELD
        }
        
        /**
         * Member type 
         */
        private final Type type;
        
        /**
         * The original name of the member 
         */
        private final String memberName;
        
        /**
         * The member's signature
         */
        private final String memberDesc;
        
        /**
         * True if this member was injected by a mixin, false if it was
         * originally part of the class
         */
        private final boolean isInjected;
        
        /**
         * Access modifiers
         */
        private final int modifiers;
        
        /**
         * Current name of the member, may be different from {@link #memberName}
         * if the member has been renamed
         */
        private String currentName;
        
        protected Member(Member member) {
            this(member.type, member.memberName, member.memberDesc, member.modifiers, member.isInjected);
            this.currentName = member.currentName;
        }
        
        protected Member(Type type, String name, String desc, int access) {
            this(type, name, desc, access, false);
        }

        protected Member(Type type, String name, String desc, int access, boolean injected) {
            this.type = type;
            this.memberName = name;
            this.memberDesc = desc;
            this.isInjected = injected;
            this.currentName = name;
            this.modifiers = access;
        }
        
        public String getOriginalName() {
            return this.memberName;
        }
        
        public String getName() {
            return this.currentName;
        }
        
        public String getDesc() {
            return this.memberDesc;
        }
        
        public boolean isInjected() {
            return this.isInjected;
        }
        
        public boolean isRenamed() {
            return this.currentName != this.memberName;
        }

        public boolean isPrivate() {
            return (this.modifiers & Opcodes.ACC_PRIVATE) != 0;
        }

        // Abstract because this has to be static in order to contain the enum
        public abstract ClassInfo getOwner();
        
        public int getAccess() {
            return this.modifiers;
        }

        public void renameTo(String name) {
            this.currentName = name;
        }
        
        public boolean equals(String name, String desc) {
            return (this.memberName.equals(name)
                    || this.currentName.equals(name))
                    && this.memberDesc.equals(desc);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Member)) {
                return false;
            }
            
            Member other = (Member)obj;
            return (other.memberName.equals(this.memberName) 
                    || other.currentName.equals(this.currentName))
                    && other.memberDesc.equals(this.memberDesc);
        }
        
        @Override
        public int hashCode() {
            return this.toString().hashCode();
        }
        
        @Override
        public String toString() {
            return this.memberName + this.memberDesc;
        }
    }
    
    /**
     * A method
     */
    public class Method extends Member {
        
        private final List<FrameData> frames;

        public Method(Member member) {
            super(member);
            this.frames = member instanceof Method ? ((Method)member).frames : null;
        }

        public Method(MethodNode method) {
            this(method, false);
        }
        
        public Method(MethodNode method, boolean injected) {
            super(Type.METHOD, method.name, method.desc, method.access, injected);
            this.frames = this.gatherFrames(method);
        }
        
        public Method(String name, String desc) {
            super(Type.METHOD, name, desc, Opcodes.ACC_PUBLIC, false);
            this.frames = null;
        }

        public Method(String name, String desc, int access) {
            super(Type.METHOD, name, desc, access, false);
            this.frames = null;
        }

        public Method(String name, String desc, int access, boolean injected) {
            super(Type.METHOD, name, desc, access, injected);
            this.frames = null;
        }
        
        private List<FrameData> gatherFrames(MethodNode method) {
            List<FrameData> frames = new ArrayList<FrameData>();
            for (Iterator<AbstractInsnNode> iter = method.instructions.iterator(); iter.hasNext();) {
                AbstractInsnNode insn = iter.next();
                if (insn instanceof FrameNode) {
                    frames.add(new FrameData(method.instructions.indexOf(insn), (FrameNode)insn));
                }
            }
            return frames;
        }
        
        public List<FrameData> getFrames() {
            return this.frames;
        }

        @Override
        public ClassInfo getOwner() {
            return ClassInfo.this;
        } 

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Method)) {
                return false;
            }
            
            return super.equals(obj);
        }
    }
    
    /**
     * A field
     */
    class Field extends Member {
        
        public Field(Member member) {
            super(member);
        }
        
        public Field(FieldNode field) {
            this(field, false);
        }
        
        public Field(FieldNode field, boolean injected) {
            super(Type.FIELD, field.name, field.desc, field.access, injected);
        }
        
        public Field(String name, String desc, int access) {
            super(Type.FIELD, name, desc, access, false);
        }
        
        public Field(String name, String desc, int access, boolean injected) {
            super(Type.FIELD, name, desc, access, injected);
        }
        
        @Override
        public ClassInfo getOwner() {
            return ClassInfo.this;
        } 
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Field)) {
                return false;
            }
            
            return super.equals(obj);
        }
    }
    
    private static final Logger logger = LogManager.getLogger("mixin");

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
     * True either if this is not an inner class or if it is an inner class but
     * does not contain a reference to its outer class.
     */
    private final boolean isProbablyStatic;
    
    /**
     * Interfaces
     */
    private final Set<String> interfaces;
    
    /**
     * Public and protected methods (instance) methods in this class 
     */
    private final Set<Method> methods;
    
    /**
     * Public and protected fields in this class
     */
    private final Set<Field> fields;
    
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
        this.isProbablyStatic = true;
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
        this.fields = Collections.<Field>emptySet();
        this.isInterface = false;
        this.interfaces = Collections.<String>emptySet();
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
        this.fields = new HashSet<Field>();
        this.isInterface = ((classNode.access & Opcodes.ACC_INTERFACE) != 0);
        this.interfaces = new HashSet<String>();
        this.access = classNode.access;
        this.isMixin = classNode instanceof MixinClassNode;
        this.mixin = this.isMixin ? ((MixinClassNode)classNode).getMixin() : null;

        this.interfaces.addAll(classNode.interfaces);
        
        for (MethodNode method : classNode.methods) {
            this.addMethod(method, this.isMixin);
        }

        boolean isProbablyStatic = true;
        String outerName = classNode.outerClass;
        if (outerName == null) {
            for (FieldNode field : classNode.fields) {
                if ((field.access & Opcodes.ACC_SYNTHETIC) != 0) {
                    if (field.name.startsWith("this$")) {
                        isProbablyStatic = false;
                        outerName = field.desc;
                        if (outerName.startsWith("L")) {
                            outerName = outerName.substring(1, outerName.length() - 1);
                        }
                    }
                } 
                
                if ((field.access & Opcodes.ACC_STATIC) == 0) {
                    this.fields.add(new Field(field, this.isMixin));
                }
            }
        }
        
        this.isProbablyStatic = isProbablyStatic;
        this.outerName = outerName;
    }
    
    void addInterface(String iface) {
        this.interfaces.add(iface);
    }

    void addMethod(MethodNode method) {
        this.addMethod(method, true);
    }

    private void addMethod(MethodNode method, boolean injected) {
        if (!method.name.startsWith("<") && (method.access & Opcodes.ACC_STATIC) == 0) {
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
        return Collections.<MixinInfo>unmodifiableSet(this.mixins);
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
     * Get whether this class has ACC_ABSTRACT
     */
    public boolean isAbstract() {
        return (this.access & Opcodes.ACC_ABSTRACT) != 0;
    }

    /**
     * Get whether this class has ACC_SYNTHETIC
     */
    public boolean isSynthetic() {
        return (this.access & Opcodes.ACC_SYNTHETIC) != 0;
    }
    
    /**
     * Get whether this class is probably static (or is not an inner class) 
     */
    public boolean isProbablyStatic() {
        return this.isProbablyStatic;
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
    public Set<String> getInterfaces() {
        return Collections.<String>unmodifiableSet(this.interfaces);
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
    List<ClassInfo> getTargets() {
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
        return Collections.<Method>unmodifiableSet(this.methods);
    }
    
    /**
     * If this is an interface, returns a set containing all methods in this
     * interface and all super interfaces. If this is a class, returns a set
     * containing all methods for all interfaces implemented by this class and
     * all super interfaces of those interfaces.
     * 
     * @return read-only view of class methods
     */
    public Set<Method> getInterfaceMethods() {
        Set<Method> methods = new HashSet<Method>();

        ClassInfo superClass = this.addMethodsRecursive(methods);
        if (!this.isInterface) {
            while (superClass != null && superClass != ClassInfo.OBJECT) {
                superClass = superClass.addMethodsRecursive(methods);
            }
        }
        
        return Collections.<Method>unmodifiableSet(methods);
    }

    /**
     * Recursive function used by {@link #getInterfaceMethods} to add all
     * interface methods to the supplied set
     * 
     * @param methods Method set to add to
     * @return superclass reference, used to make the code above more fluent
     */
    private ClassInfo addMethodsRecursive(Set<Method> methods) {
        if (this.isInterface) {
            methods.addAll(this.methods);
        } else if (!this.isMixin) {
            for (MixinInfo mixin : this.mixins) {
                mixin.getClassInfo().addMethodsRecursive(methods);
            }
        }
        
        for (String iface : this.interfaces) {
            ClassInfo.forName(iface).addMethodsRecursive(methods);
        }
        
        return this.getSuperClass();
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
        return this.findMethodInHierarchy(method.name, method.desc, includeThisClass, Traversal.NONE);
    }

    /**
     * Finds the specified private or protected method in this class's hierarchy
     * 
     * @param method Method to search for
     * @param includeThisClass True to return this class if the method exists
     *      here, or false to search only superclasses
     * @param includePrivate Include private members in the search
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethodInHierarchy(MethodNode method, boolean includeThisClass, boolean includePrivate) {
        return this.findMethodInHierarchy(method.name, method.desc, includeThisClass, Traversal.NONE, includePrivate);
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
        return this.findMethodInHierarchy(method.name, method.desc, includeThisClass, Traversal.NONE);
    }
    
    /**
     * Finds the specified public or protected method in this class's hierarchy
     * 
     * @param method Method to search for
     * @param includeThisClass True to return this class if the method exists
     *      here, or false to search only superclasses
     * @param includePrivate Include private members in the search
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethodInHierarchy(MethodInsnNode method, boolean includeThisClass, boolean includePrivate) {
        return this.findMethodInHierarchy(method.name, method.desc, includeThisClass, Traversal.NONE, includePrivate);
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
        return this.findMethodInHierarchy(name, desc, includeThisClass, traversal, false);
    }
    
    /**
     * Finds the specified public or protected method in this class's hierarchy
     * 
     * @param name Method name to search for
     * @param desc Method descriptor
     * @param includeThisClass True to return this class if the method exists
     *      here, or false to search only superclasses
     * @param traversal Traversal type to allow during this lookup
     * @param includePrivate Include private members in the search
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethodInHierarchy(String name, String desc, boolean includeThisClass, Traversal traversal, boolean includePrivate) {
        return this.findInHierarchy(name, desc, includeThisClass, traversal, includePrivate, Type.METHOD);
    }
    
    /**
     * Finds the specified private or protected field in this class's hierarchy
     * 
     * @param field Field to search for
     * @param includeThisClass True to return this class if the field exists
     *      here, or false to search only superclasses
     * @return the field object or null if the field could not be resolved
     */
    public Field findFieldInHierarchy(FieldNode field, boolean includeThisClass) {
        return this.findFieldInHierarchy(field.name, field.desc, includeThisClass, Traversal.NONE);
    }
    
    /**
     * Finds the specified private or protected field in this class's hierarchy
     * 
     * @param field Field to search for
     * @param includeThisClass True to return this class if the field exists
     *      here, or false to search only superclasses
     * @param includePrivate Include private members in the search
     * @return the field object or null if the field could not be resolved
     */
    public Field findFieldInHierarchy(FieldNode field, boolean includeThisClass, boolean includePrivate) {
        return this.findFieldInHierarchy(field.name, field.desc, includeThisClass, Traversal.NONE, includePrivate);
    }
    
    /**
     * Finds the specified public or protected field in this class's hierarchy
     * 
     * @param field Field to search for
     * @param includeThisClass True to return this class if the field exists
     *      here, or false to search only superclasses
     * @return the field object or null if the field could not be resolved
     */
    public Field findFieldInHierarchy(FieldInsnNode field, boolean includeThisClass) {
        return this.findFieldInHierarchy(field.name, field.desc, includeThisClass, Traversal.NONE);
    }
    
    /**
     * Finds the specified public or protected field in this class's hierarchy
     * 
     * @param field Field to search for
     * @param includeThisClass True to return this class if the field exists
     *      here, or false to search only superclasses
     * @param includePrivate Include private members in the search
     * @return the field object or null if the field could not be resolved
     */
    public Field findFieldInHierarchy(FieldInsnNode field, boolean includeThisClass, boolean includePrivate) {
        return this.findFieldInHierarchy(field.name, field.desc, includeThisClass, Traversal.NONE, includePrivate);
    }
    
    /**
     * Finds the specified public or protected field in this class's hierarchy
     * 
     * @param name Field name to search for
     * @param desc Field descriptor
     * @param includeThisClass True to return this class if the field exists
     *      here, or false to search only superclasses
     * @return the field object or null if the field could not be resolved
     */
    public Field findFieldInHierarchy(String name, String desc, boolean includeThisClass) {
        return this.findFieldInHierarchy(name, desc, includeThisClass, Traversal.NONE);
    }

    /**
     * Finds the specified public or protected field in this class's hierarchy
     * 
     * @param name Field name to search for
     * @param desc Field descriptor
     * @param includeThisClass True to return this class if the field exists
     *      here, or false to search only superclasses
     * @param traversal Traversal type to allow during this lookup
     * @return the field object or null if the field could not be resolved
     */
    public Field findFieldInHierarchy(String name, String desc, boolean includeThisClass, Traversal traversal) {
        return this.findFieldInHierarchy(name, desc, includeThisClass, traversal, false);
    }
    
    /**
     * Finds the specified public or protected field in this class's hierarchy
     * 
     * @param name Field name to search for
     * @param desc Field descriptor
     * @param includeThisClass True to return this class if the field exists
     *      here, or false to search only superclasses
     * @param traversal Traversal type to allow during this lookup
     * @param includePrivate Include private members in the search
     * @return the field object or null if the field could not be resolved
     */
    public Field findFieldInHierarchy(String name, String desc, boolean includeThisClass, Traversal traversal, boolean includePrivate) {
        return this.findInHierarchy(name, desc, includeThisClass, traversal, includePrivate, Type.FIELD);
    }
    
    /**
     * Finds a public or protected member in the hierarchy of this class which
     * matches the supplied details 
     * 
     * @param name Member name to search
     * @param desc Member descriptor
     * @param includeThisClass True to return this class if the field exists
     *      here, or false to search only superclasses
     * @param traversal Traversal type to allow during this lookup
     * @param priv Include private members in the search
     * @param type Type of member to search for (field or method)
     * @return the discovered member or null if the member could not be resolved
     */
    private <M extends Member> M findInHierarchy(String name, String desc, boolean includeThisClass, Traversal traversal, boolean priv, Type type) {
        if (includeThisClass) {
            M member = this.findMember(name, desc, priv, type);
            if (member != null) {
                return member;
            }
            
            if (traversal.canTraverse()) {
                for (MixinInfo mixin : this.mixins) {
                    M mixinMember = mixin.getClassInfo().findMember(name, desc, priv, type);
                    if (mixinMember != null) {
                        return this.cloneMember(mixinMember);
                    }
                }               
            }
        }
        
        ClassInfo superClassInfo = this.getSuperClass();
        if (superClassInfo != null) {
            for (ClassInfo superTarget : superClassInfo.getTargets()) {
                // Do not search for private members in superclasses, pass priv as false
                M member = superTarget.findInHierarchy(name, desc, true, traversal.next(), false, type);
                if (member != null) {
                    return member;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Effectively a clone method for member, placed here so that the enclosing
     * instance for the inner class is this class and not the enclosing instance
     * of the existing class. Basically creates a cloned member with this 
     * ClassInfo as its parent.
     * 
     * @param member
     * @return
     */
    @SuppressWarnings("unchecked")
    private <M extends Member> M cloneMember(M member) {
        if (member instanceof Method) {
            return (M)new Method(member);
        }
        
        return (M)new Field(member);
    }
    
    /**
     * Finds the specified public or protected method in this class
     * 
     * @param method Method to search for
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethod(MethodNode method) {
        return this.findMethod(method.name, method.desc, false);
    }
    
    /**
     * Finds the specified public or protected method in this class
     * 
     * @param method Method to search for
     * @param includePrivate also search private fields
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethod(MethodNode method, boolean includePrivate) {
        return this.findMethod(method.name, method.desc, includePrivate);
    }
    
    /**
     * Finds the specified public or protected method in this class
     * 
     * @param method Method to search for
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethod(MethodInsnNode method) {
        return this.findMethod(method.name, method.desc, false);
    }
    
    /**
     * Finds the specified public or protected method in this class
     * 
     * @param method Method to search for
     * @param includePrivate also search private fields
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethod(MethodInsnNode method, boolean includePrivate) {
        return this.findMethod(method.name, method.desc, includePrivate);
    }
    
    /**
     * Finds the specified public or protected method in this class
     * 
     * @param method Method name to search for
     * @param desc Method signature to search for
     * @param includePrivate also search private fields
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethod(String name, String desc, boolean includePrivate) {
        return this.findMember(name, desc, includePrivate, Type.METHOD);
    }
    
    /**
     * Finds the specified field in this class
     * 
     * @param field Field to search for
     * @return the field object or null if the field could not be resolved
     */
    public Field findField(FieldNode field) {
        return this.findField(field.name, field.desc, (field.access & Opcodes.ACC_PRIVATE) != 0);
    }
    
    /**
     * Finds the specified public or protected method in this class
     * 
     * @param field Field to search for
     * @param includePrivate also search private fields
     * @return the field object or null if the field could not be resolved
     */
    public Field findField(FieldInsnNode field, boolean includePrivate) {
        return this.findField(field.name, field.desc, includePrivate);
    }
    
    /**
     * Finds the specified field in this class
     * 
     * @param name Field name to search for
     * @param desc Field signature to search for
     * @param includePrivate also search private fields
     * @return the field object or null if the field could not be resolved
     */
    public Field findField(String name, String desc, boolean includePrivate) {
        return this.findMember(name, desc, includePrivate, Type.FIELD);
    }

    /**
     * Finds the specified member in this class
     * 
     * @param name Field name to search for
     * @param desc Field signature to search for
     * @param includePrivate also search private fields
     * @param memberType Type of member list to search
     * @return the field object or null if the field could not be resolved
     */
    private <M extends Member> M findMember(String name, String desc, boolean includePrivate, Type memberType) {
        @SuppressWarnings("unchecked")
        Set<M> members = (Set<M>)(memberType == Type.METHOD ? this.methods : this.fields);
        
        for (M member : members) {
            if (member.equals(name, desc) && (includePrivate || !member.isPrivate())) {
                return member;
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
    static ClassInfo fromClassNode(ClassNode classNode) {
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
            ClassInfo.logger.debug("Added class metadata for {} to metadata cache", className);
        }
        
        return info;
    }
}
