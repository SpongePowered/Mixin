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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.logging.ILogger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Member.Type;
import org.spongepowered.asm.mixin.transformer.MixinInfo.MixinClassNode;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.ClassSignature;
import org.spongepowered.asm.util.LanguageFeatures;
import org.spongepowered.asm.util.Locals;
import org.spongepowered.asm.util.asm.ClassNodeAdapter;
import org.spongepowered.asm.util.perf.Profiler;
import org.spongepowered.asm.util.perf.Profiler.Section;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Information about a class, used as a way of keeping track of class hierarchy
 * information needed to support more complex mixin behaviour such as detached
 * superclass and mixin inheritance.
 */
public final class ClassInfo {

    /**
     * Include <tt>private</tt> members when running a member search
     */
    public static final int INCLUDE_PRIVATE = Opcodes.ACC_PRIVATE;

    /**
     * Include <tt>static</tt> members when running a member search
     */
    public static final int INCLUDE_STATIC = Opcodes.ACC_STATIC;

    /**
     * Include <tt>private</tt> <b>and</b> <tt>static</tt> members when running
     * a member search
     */
    public static final int INCLUDE_ALL = ClassInfo.INCLUDE_PRIVATE | ClassInfo.INCLUDE_STATIC;
    
    /**
     * Include instance and class initialisers when running a method search 
     */
    public static final int INCLUDE_INITIALISERS = 0x40000;
    
    /**
     * Search type for the findInHierarchy methods, replaces a boolean flag
     * which made calling code difficult to read
     */
    public static enum SearchType {
        
        /**
         * Include this class when searching in the hierarchy
         */
        ALL_CLASSES,
        
        /**
         * Only walk the superclasses when searching the hierarchy 
         */
        SUPER_CLASSES_ONLY
        
    }
    
    /**
     * When using {@link ClassInfo#forType ClassInfo.forType}, determines
     * whether an array type should be returned as declared (eg. as <tt>Object
     * </tt>) or whether the element type should be returned instead.
     */
    public static enum TypeLookup {
        
        /**
         * Return the type as declared in the descriptor. This means that array
         * types will be treated <tt>Object</tt> for the purposes of type
         * hierarchy lookups returning the correct member methods.
         */
        DECLARED_TYPE,
        
        /**
         * Legacy behaviour. A lookup by type will return the element type. 
         */
        ELEMENT_TYPE
        
    }

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
        NONE(null, false, SearchType.SUPER_CLASSES_ONLY),

        /**
         * Traversal is allowed at all stages.
         */
        ALL(null, true, SearchType.ALL_CLASSES),

        /**
         * Traversal is allowed at the bottom of the hierarchy but no further.
         */
        IMMEDIATE(Traversal.NONE, true, SearchType.SUPER_CLASSES_ONLY),

        /**
         * Traversal is allowed only on superclasses and not at the bottom of
         * the hierarchy.
         */
        SUPER(Traversal.ALL, false, SearchType.SUPER_CLASSES_ONLY);

        private final Traversal next;

        private final boolean traverse;
        
        private final SearchType searchType;

        private Traversal(Traversal next, boolean traverse, SearchType searchType) {
            this.next = next != null ? next : this;
            this.traverse = traverse;
            this.searchType = searchType;
        }

        /**
         * Return the next traversal type for this traversal type
         */
        public Traversal next() {
            return this.next;
        }

        /**
         * Return whether this traversal type allows traversal
         */
        public boolean canTraverse() {
            return this.traverse;
        }
        
        public SearchType getSearchType() {
            return this.searchType;
        }

    }

    /**
     * Information about frames in a method
     */
    public static class FrameData {

        private static final String[] FRAMETYPES = { "NEW", "FULL", "APPEND", "CHOP", "SAME", "SAME1" };

        /**
         * Frame index
         */
        public final int index;

        /**
         * Frame type
         */
        public final int type;

        /**
         * Frame local count
         */
        public final int locals;
        
        /**
         * Frame local size 
         */
        public final int size;

        FrameData(int index, int type, int locals, int size) {
            this.index = index;
            this.type = type;
            this.locals = locals;
            this.size = size;
        }

        FrameData(int index, FrameNode frameNode, int initialFrameSize) {
            this.index = index;
            this.type = frameNode.type;
            this.locals = frameNode.local != null ? frameNode.local.size() : 0;
            this.size = Locals.computeFrameSize(frameNode, initialFrameSize);
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return String.format("FrameData[index=%d, type=%s, locals=%d size=%d]", this.index, FrameData.FRAMETYPES[this.type + 1], this.locals,
                    this.size);
        }
    }
    
    /**
     * Information about a member in this class
     */
    abstract static class Member {

        /**
         * Member type
         */
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
        
        /**
         * Current descriptor of the member, may be different from
         * {@link #memberDesc} if the member has been remapped
         */
        private String currentDesc;
        
        /**
         * True if this member is decorated with {@link Final} 
         */
        private boolean decoratedFinal;

        /**
         * True if this member is decorated with {@link Mutable}
         */
        private boolean decoratedMutable;

        /**
         * True if this member is decorated with {@link Unique}
         */
        private boolean unique;

        protected Member(Member member) {
            this(member.type, member.memberName, member.memberDesc, member.modifiers, member.isInjected);
            this.currentName = member.currentName;
            this.currentDesc = member.currentDesc;
            this.unique = member.unique;
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
            this.currentDesc = desc;
            this.modifiers = access;
        }

        public String getOriginalName() {
            return this.memberName;
        }

        public String getName() {
            return this.currentName;
        }

        public String getOriginalDesc() {
            return this.memberDesc;
        }

        public String getDesc() {
            return this.currentDesc;
        }
        
        public boolean isInjected() {
            return this.isInjected;
        }

        public boolean isRenamed() {
            return !this.currentName.equals(this.memberName);
        }

        public boolean isRemapped() {
            return !this.currentDesc.equals(this.memberDesc);
        }
        
        public boolean isPrivate() {
            return (this.modifiers & Opcodes.ACC_PRIVATE) != 0;
        }

        public boolean isStatic() {
            return (this.modifiers & Opcodes.ACC_STATIC) != 0;
        }

        public boolean isAbstract() {
            return (this.modifiers & Opcodes.ACC_ABSTRACT) != 0;
        }

        public boolean isFinal() {
            return (this.modifiers & Opcodes.ACC_FINAL) != 0;
        }
        
        public boolean isSynthetic() {
            return (this.modifiers & Opcodes.ACC_SYNTHETIC) != 0;
        }
        
        public boolean isUnique() {
            return this.unique;
        }
        
        public void setUnique(boolean unique) {
            this.unique = unique;
        }

        public boolean isDecoratedFinal() {
            return this.decoratedFinal;
        }
        
        public boolean isDecoratedMutable() {
            return this.decoratedMutable;
        }

        protected void setDecoratedFinal(boolean decoratedFinal, boolean decoratedMutable) {
            this.decoratedFinal = decoratedFinal;
            this.decoratedMutable = decoratedMutable;
        }
            
        public boolean matchesFlags(int flags) {
            return (((~this.modifiers | (flags & ClassInfo.INCLUDE_PRIVATE)) & ClassInfo.INCLUDE_PRIVATE) != 0
                 && ((~this.modifiers | (flags & ClassInfo.INCLUDE_STATIC)) & ClassInfo.INCLUDE_STATIC) != 0);
        }

        // Abstract because this has to be static in order to contain the enum
        public abstract ClassInfo getOwner();
        
        public ClassInfo getImplementor() {
            return this.getOwner();
        }

        public int getAccess() {
            return this.modifiers;
        }

        /**
         * @param name new name
         * @return the passed-in argument, for fluency
         */
        public String renameTo(String name) {
            this.currentName = name;
            return name;
        }
        
        public String remapTo(String desc) {
            this.currentDesc = desc;
            return desc;
        }

        public boolean equals(String name, String desc) {
            return (this.memberName.equals(name) || this.currentName.equals(name))
                    && (this.memberDesc.equals(desc) || this.currentDesc.equals(desc));
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Member)) {
                return false;
            }

            Member other = (Member)obj;
            return (other.memberName.equals(this.memberName) || other.currentName.equals(this.currentName))
                    && (other.memberDesc.equals(this.memberDesc) || other.currentDesc.equals(this.currentDesc));
        }

        @Override
        public int hashCode() {
            return this.toString().hashCode();
        }

        @Override
        public String toString() {
            return String.format(this.getDisplayFormat(), this.memberName, this.memberDesc);
        }

        protected String getDisplayFormat() {
            return "%s%s";
        }
        
    }

    /**
     * A method
     */
    public class Method extends Member {

        private final List<FrameData> frames;
        
        private boolean isAccessor;
        
        private boolean conformed;

        public Method(Member member) {
            super(member);
            this.frames = member instanceof Method ? ((Method)member).frames : null;
        }

        public Method(MethodNode method) {
            this(method, false);
        }

        @SuppressWarnings("unchecked")
        public Method(MethodNode method, boolean injected) {
            super(Type.METHOD, method.name, method.desc, method.access, injected);
            this.frames = this.gatherFrames(method);
            this.setUnique(Annotations.getVisible(method, Unique.class) != null);
            this.isAccessor = Annotations.getSingleVisible(method, Accessor.class, Invoker.class) != null;
            boolean decoratedFinal = Annotations.getVisible(method, Final.class) != null;
            boolean decoratedMutable = Annotations.getVisible(method, Mutable.class) != null;
            this.setDecoratedFinal(decoratedFinal, decoratedMutable);
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
                    frames.add(new FrameData(method.instructions.indexOf(insn), (FrameNode)insn, Bytecode.getFirstNonArgLocalIndex(method)));
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

        public boolean isAccessor() {
            return this.isAccessor;
        }
        
        public boolean isConformed() {
            return this.conformed;
        }
        
        @Override
        public String renameTo(String name) {
            this.conformed = false;
            return super.renameTo(name);
        }

        /**
         * @param name new name
         * @return the passed-in argument, for fluency
         */
        public String conform(String name) {
            boolean nameChanged = !name.equals(this.getName());
            if (this.conformed && nameChanged) {
                throw new IllegalStateException("Method " + this + " was already conformed. Original= " + this.getOriginalName()
                        + " Current=" + this.getName() + " New=" + name);
            }
            if (nameChanged) {
                this.renameTo(name);
                this.conformed = true;
            }
            return name;
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
     * A method resolved in an interface <em>via</em> a class, return the member
     * wrapped so that the implementing class can be retrieved.
     */
    public class InterfaceMethod extends Method {
        
        private final ClassInfo owner;

        public InterfaceMethod(Member member) {
            super(member);
            this.owner = member.getOwner();
        }
        
        @Override
        public ClassInfo getOwner() {
            return this.owner;
        }
        
        @Override
        public ClassInfo getImplementor() {
            return ClassInfo.this;
        }
        
    }

    /**
     * A field
     */
    public class Field extends Member {

        public Field(Member member) {
            super(member);
        }

        public Field(FieldNode field) {
            this(field, false);
        }

        public Field(FieldNode field, boolean injected) {
            super(Type.FIELD, field.name, field.desc, field.access, injected);
            
            this.setUnique(Annotations.getVisible(field, Unique.class) != null);
            
            if (Annotations.getVisible(field, Shadow.class) != null) {
                boolean decoratedFinal = Annotations.getVisible(field, Final.class) != null;
                boolean decoratedMutable = Annotations.getVisible(field, Mutable.class) != null;
                this.setDecoratedFinal(decoratedFinal, decoratedMutable);
            }
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
        
        @Override
        protected String getDisplayFormat() {
            return "%s:%s";
        }
    }

    private static final ILogger logger = MixinService.getService().getLogger("mixin");
    
    private static final Profiler profiler = Profiler.getProfiler("meta");

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
     * True if this in an inner class
     */
    private final boolean isInner;

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
     * Constructors and initialisers in this class
     */
    private final Set<Method> initialisers;

    /**
     * Methods in this class
     */
    private final Set<Method> methods;

    /**
     * Public and protected fields in this class
     */
    private final Set<Field> fields;

    /**
     * Mixins which target this class
     */
    private final Set<MixinInfo> mixins;

    /**
     * Map of mixin types to corresponding supertypes, to avoid repeated
     * lookups
     */
    private final Map<ClassInfo, ClassInfo> correspondingTypes = new HashMap<ClassInfo, ClassInfo>();

    /**
     * Mixin info if this class is a mixin itself
     */
    private final MixinInfo mixin;
    
    private final MethodMapper methodMapper;
    
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
     * Class signature, lazy-loaded where possible
     */
    private ClassSignature signature;

    /**
     * Mixins which have been applied this class
     */
    private Set<MixinInfo> appliedMixins;
    
    /**
     * Declared nest host
     */
    private String nestHost;
    
    /**
     * Declared nest members 
     */
    private Set<String> nestMembers;

    /**
     * Private constructor used to initialise the ClassInfo for {@link Object}
     */
    private ClassInfo() {
        this.name = ClassInfo.JAVA_LANG_OBJECT;
        this.superName = null;
        this.outerName = null;
        this.isInner = false;
        this.isProbablyStatic = true;
        this.initialisers = ImmutableSet.<Method>of(
            new Method("<init>", "()V")
        );
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
        this.mixins = Collections.<MixinInfo>emptySet();
        this.methodMapper = null;
    }

    /**
     * Initialise a ClassInfo from the supplied {@link ClassNode}
     *
     * @param classNode Class node to inspect
     */
    private ClassInfo(ClassNode classNode) {
        Section timer = ClassInfo.profiler.begin(Profiler.ROOT, "class.meta");
        try {
            this.name = classNode.name;
            this.superName = classNode.superName != null ? classNode.superName : ClassInfo.JAVA_LANG_OBJECT;
            this.initialisers = new HashSet<Method>();
            this.methods = new HashSet<Method>();
            this.fields = new HashSet<Field>();
            this.isInterface = ((classNode.access & Opcodes.ACC_INTERFACE) != 0);
            this.interfaces = new HashSet<String>();
            this.isMixin = classNode instanceof MixinClassNode;
            this.mixin = this.isMixin ? ((MixinClassNode)classNode).getMixin() : null;
            this.mixins = this.isMixin ? Collections.<MixinInfo>emptySet() : new HashSet<MixinInfo>();

            this.interfaces.addAll(classNode.interfaces);

            for (MethodNode method : classNode.methods) {
                this.addMethod(method, this.isMixin);
            }

            boolean isProbablyStatic = true;
            String outerName = classNode.outerClass;
            for (FieldNode field : classNode.fields) {
                if ((field.access & Opcodes.ACC_SYNTHETIC) != 0) {
                    if (field.name.startsWith("this$")) {
                        isProbablyStatic = false;
                        if (outerName == null) {
                            outerName = field.desc;
                            if (outerName != null && outerName.startsWith("L") && outerName.endsWith(";")) {
                                outerName = outerName.substring(1, outerName.length() - 1);
                            }
                        }
                    }
                }

                this.fields.add(new Field(field, this.isMixin));
            }

            this.isProbablyStatic = isProbablyStatic;
            this.methodMapper = new MethodMapper(MixinEnvironment.getCurrentEnvironment(), this);
            this.signature = ClassSignature.ofLazy(classNode);
            
            int access = classNode.access;
            boolean isInner = outerName != null;
            
            for (InnerClassNode innerClass : classNode.innerClasses) {
                if (this.name.equals(innerClass.name)) {
                    access = innerClass.access;
                    isInner = true;
                    outerName = innerClass.outerName;
                }
            }
            
            this.access = access;
            this.isInner = isInner;
            this.outerName = outerName;

            if (MixinEnvironment.getCompatibilityLevel().supports(LanguageFeatures.NESTING)) {
                this.nestHost = ClassNodeAdapter.getNestHostClass(classNode);
                List<String> nestMembers = ClassNodeAdapter.getNestMembers(classNode);
                if (nestMembers != null) {
                    this.nestMembers = new LinkedHashSet<String>();
                    this.nestMembers.addAll(nestMembers);
                }
            }
        } finally {
            timer.end();
        }
    }

    void addInterface(String iface) {
        this.interfaces.add(iface);
        this.getSignature().addInterface(iface);
    }

    void addMethod(MethodNode method) {
        this.addMethod(method, true);
    }

    private void addMethod(MethodNode method, boolean injected) {
        if (method.name.startsWith("<")) {
            this.initialisers.add(new Method(method, injected));
        } else {
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
     * Add a mixin which has been applied to this class
     */
    void addAppliedMixin(MixinInfo mixin) {
        if (this.appliedMixins == null) {
            this.appliedMixins = new HashSet<MixinInfo>(); 
        }
        this.appliedMixins.add(mixin);
    }

    /**
     * Get all mixins which target this class
     */
    Set<MixinInfo> getMixins() {
        return this.isMixin ? Collections.<MixinInfo>emptySet() : Collections.<MixinInfo>unmodifiableSet(this.mixins);
    }

    /**
     * Get all mixins which have been successfully applied to this class
     */
    public Set<IMixinInfo> getAppliedMixins() {
        return this.appliedMixins != null ? Collections.<IMixinInfo>unmodifiableSet(this.appliedMixins) : Collections.<IMixinInfo>emptySet();
    }
    
    /**
     * Get whether this class is a mixin
     */
    public boolean isMixin() {
        return this.isMixin;
    }
    
    /**
     * Get whether this class is loadable mixin
     */
    public boolean isLoadable() {
        return this.mixin != null && this.mixin.isLoadable();
    }

    /**
     * Get whether this class has ACC_PUBLIC
     */
    public boolean isPublic() {
        return (this.access & Opcodes.ACC_PUBLIC) != 0;
    }
    
    /**
     * Get whether this class is really public (only valid for inner classes
     * which may be "public" themselves but aren't actually visible because
     * their enclosing type is package-private for example.
     */
    public boolean isReallyPublic() {
        boolean isPublic = this.isPublic();
        if (!this.isInner || !isPublic) {
            return isPublic;
        }
        
        ClassInfo outer = this;
        while (outer != null && outer.outerName != null) {
            outer = ClassInfo.forName(outer.outerName);
            if (outer != null && !outer.isPublic()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get whether this class has ACC_PROTECTED (only valid for inner classes)
     */
    public boolean isProtected() {
        return (this.access & Opcodes.ACC_PROTECTED) != 0;
    }
    
    /**
     * Get whether this class has ACC_PRIVATE (only valid for inner classes)
     */
    public boolean isPrivate() {
        return (this.access & Opcodes.ACC_PRIVATE) != 0;
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
        return this.isInner;
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
    
    MethodMapper getMethodMapper() {
        return this.methodMapper;
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
     * Get the class name (java format)
     */
    public String getClassName() {
        return this.name.replace('/', '.');
    }

    /**
     * Get the class name (simple name, java format)
     */
    public String getSimpleName() {
        int pos = this.name.lastIndexOf('/');
        return pos < 0 ? this.name : this.name.substring(pos + 1);
    }
    
    /**
     * Get the object type (ASM type)
     */
    public org.objectweb.asm.Type getType() {
        return org.objectweb.asm.Type.getObjectType(this.name);
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
     * Return the class signature
     * 
     * @return signature as a {@link ClassSignature} instance
     */
    public ClassSignature getSignature() {
        return this.signature.wake();
    }
    
    /**
     * Return the nest host declared in the class
     */
    public String getNestHost() {
        return this.nestHost;
    }
    
    /**
     * Get nest members declared in the class
     */
    public Set<String> getNestMembers() {
        return this.nestMembers != null ? Collections.<String>unmodifiableSet(this.nestMembers) : Collections.<String>emptySet();
    }
    
    /**
     * Resolve the nest host for inner classes of this class. If the class
     * itself has a nest host, the host is returned so that members can be added
     * to it. If the class itself <em>is</em> a nest host (already has nest
     * members) or is neither a nest host or member (eg. per the specification
     * is already a nest host with itself as the sole member) then this method
     * simply returns this ClassInfo.
     */
    public ClassInfo resolveNestHost() {
        if (!Strings.isNullOrEmpty(this.nestHost)) {
            return ClassInfo.forName(this.nestHost);
        }
        return this;
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
     * @param  includeMixins Whether to include methods from mixins targeting
     *      this class info
     * @return read-only view of class methods
     */
    public Set<Method> getInterfaceMethods(boolean includeMixins) {
        Set<Method> methods = new HashSet<Method>();

        ClassInfo supClass = this.addMethodsRecursive(methods, includeMixins);
        if (!this.isInterface) {
            while (supClass != null && supClass != ClassInfo.OBJECT) {
                supClass = supClass.addMethodsRecursive(methods, includeMixins);
            }
        }

        // Remove default methods.
        for (Iterator<Method> it = methods.iterator(); it.hasNext();) {
            if (!it.next().isAbstract()) {
                it.remove();
            }
        }

        return Collections.<Method>unmodifiableSet(methods);
    }

    /**
     * Recursive function used by {@link #getInterfaceMethods} to add all
     * interface methods to the supplied set
     *
     * @param methods Method set to add to
     * @param includeMixins Whether to include methods from mixins targeting
     *      this class info
     * @return superclass reference, used to make the code above more fluent
     */
    private ClassInfo addMethodsRecursive(Set<Method> methods, boolean includeMixins) {
        if (this.isInterface) {
            for (Method method : this.methods) {
                // Default methods take priority. They are removed later.
                if (!method.isAbstract()) {
                    // Remove the old method so the new one is added.
                    methods.remove(method);
                }
                methods.add(method);
            }
        } else if (!this.isMixin && includeMixins) {
            for (MixinInfo mixin : this.mixins) {
                mixin.getClassInfo().addMethodsRecursive(methods, includeMixins);
            }
        }

        for (String iface : this.interfaces) {
            ClassInfo.forName(iface).addMethodsRecursive(methods, includeMixins);
        }

        return this.getSuperClass();
    }

    /**
     * Test whether this class has the specified superclass in its hierarchy
     *
     * @param superClass Superclass to search for in the hierarchy
     * @return true if the specified class appears in the class's hierarchy
     *      anywhere
     */
    public boolean hasSuperClass(Class<?> superClass) {
        return this.hasSuperClass(superClass, Traversal.NONE, superClass.isInterface());
    }

    /**
     * Test whether this class has the specified superclass in its hierarchy
     *
     * @param superClass Superclass to search for in the hierarchy
     * @param traversal Traversal type to allow during this lookup
     * @return true if the specified class appears in the class's hierarchy
     *      anywhere
     */
    public boolean hasSuperClass(Class<?> superClass, Traversal traversal) {
        return this.hasSuperClass(superClass, traversal, superClass.isInterface());
    }
    
    /**
     * Test whether this class has the specified superclass in its hierarchy
     *
     * @param superClass Superclass to search for in the hierarchy
     * @param traversal Traversal type to allow during this lookup
     * @param includeInterfaces True to include interfaces in the lookup
     * @return true if the specified class appears in the class's hierarchy
     *      anywhere
     */
    public boolean hasSuperClass(Class<?> superClass, Traversal traversal, boolean includeInterfaces) {
        String internalName = org.objectweb.asm.Type.getInternalName(superClass);
        if (ClassInfo.JAVA_LANG_OBJECT.equals(internalName)) {
            return true;
        }
        
        return this.findSuperClass(internalName, traversal) != null;
    }

    /**
     * Test whether this class has the specified superclass in its hierarchy
     *
     * @param superClass Name of the superclass to search for in the hierarchy
     * @return true if the specified class appears in the class's hierarchy
     *      anywhere
     */
    public boolean hasSuperClass(String superClass) {
        return this.hasSuperClass(superClass, Traversal.NONE, false);
    }

    /**
     * Test whether this class has the specified superclass in its hierarchy
     *
     * @param superClass Name of the superclass to search for in the hierarchy
     * @param traversal Traversal type to allow during this lookup
     * @return true if the specified class appears in the class's hierarchy
     *      anywhere
     */
    public boolean hasSuperClass(String superClass, Traversal traversal) {
        return this.hasSuperClass(superClass, traversal, false);
    }
    
    /**
     * Test whether this class has the specified superclass in its hierarchy
     *
     * @param superClass Name of the superclass to search for in the hierarchy
     * @param traversal Traversal type to allow during this lookup
     * @param includeInterfaces True to include interfaces in the lookup
     * @return true if the specified class appears in the class's hierarchy
     *      anywhere
     */
    public boolean hasSuperClass(String superClass, Traversal traversal, boolean includeInterfaces) {
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
     *      anywhere
     */
    public boolean hasSuperClass(ClassInfo superClass) {
        return this.hasSuperClass(superClass, Traversal.NONE, false);
    }

    /**
     * Test whether this class has the specified superclass in its hierarchy
     *
     * @param superClass Superclass to search for in the hierarchy
     * @param traversal Traversal type to allow during this lookup
     * @return true if the specified class appears in the class's hierarchy
     *      anywhere
     */
    public boolean hasSuperClass(ClassInfo superClass, Traversal traversal) {
        return this.hasSuperClass(superClass, traversal, false);
    }
    
    /**
     * Test whether this class has the specified superclass in its hierarchy
     *
     * @param superClass Superclass to search for in the hierarchy
     * @param traversal Traversal type to allow during this lookup
     * @param includeInterfaces True to include interfaces in the lookup
     * @return true if the specified class appears in the class's hierarchy
     *      anywhere
     */
    public boolean hasSuperClass(ClassInfo superClass, Traversal traversal, boolean includeInterfaces) {
        if (ClassInfo.OBJECT == superClass) {
            return true;
        }
        
        return this.findSuperClass(superClass.name, traversal, includeInterfaces) != null;
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
        return this.findSuperClass(superClass, traversal, false, new HashSet<String>());
    }
    
    /**
     * Search for the specified superclass in this class's hierarchy. If found
     * returns the ClassInfo, otherwise returns null
     *
     * @param superClass Superclass name to search for
     * @param traversal Traversal type to allow during this lookup
     * @param includeInterfaces True to include interfaces in the lookup
     * @return Matched superclass or null if not found
     */
    public ClassInfo findSuperClass(String superClass, Traversal traversal, boolean includeInterfaces) {
        if (ClassInfo.OBJECT.name.equals(superClass)) {
            return null;
        }
        
        return this.findSuperClass(superClass, traversal, includeInterfaces, new HashSet<String>());
    }
    
    private ClassInfo findSuperClass(String superClass, Traversal traversal, boolean includeInterfaces, Set<String> traversed) {
        ClassInfo superClassInfo = this.getSuperClass();
        if (superClassInfo != null) {
            for (ClassInfo superTarget : superClassInfo.getTargets()) {
                if (superClass.equals(superTarget.getName())) {
                    return superClassInfo;
                }

                ClassInfo found = superTarget.findSuperClass(superClass, traversal.next(), includeInterfaces, traversed);
                if (found != null) {
                    return found;
                }
            }
        }
        
        if (includeInterfaces) {
            ClassInfo iface = this.findInterface(superClass);
            if (iface != null) {
                return iface;
            }
        }
        
        if (traversal.canTraverse()) {
            for (MixinInfo mixin : this.mixins) {
                String mixinClassName = mixin.getClassName();
                if (traversed.contains(mixinClassName)) {
                    continue;
                }
                traversed.add(mixinClassName);
                ClassInfo mixinClass = mixin.getClassInfo();
                if (superClass.equals(mixinClass.getName())) {
                    return mixinClass;
                }
                ClassInfo targetSuper = mixinClass.findSuperClass(superClass, Traversal.ALL, includeInterfaces, traversed);
                if (targetSuper != null) {
                    return targetSuper;
                }
            }
        }

        return null;
    }

    private ClassInfo findInterface(String superClass) {
        for (String ifaceName : this.getInterfaces()) {
            ClassInfo iface = ClassInfo.forName(ifaceName);
            if (superClass.equals(ifaceName)) {
                return iface;
            }
            ClassInfo superIface = iface.findInterface(superClass);
            if (superIface != null) {
                return superIface;
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

        ClassInfo supClass = this.getSuperClass();

        while (supClass != null && supClass != ClassInfo.OBJECT) {
            if (supClass.isMixin) {
                return true;
            }
            supClass = supClass.getSuperClass();
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

        ClassInfo supClass = this.getSuperClass();

        while (supClass != null && supClass != ClassInfo.OBJECT) {
            if (supClass.mixins.size() > 0) {
                return true;
            }
            supClass = supClass.getSuperClass();
        }

        return false;
    }

    /**
     * Finds the specified private or protected method in this class's hierarchy
     *
     * @param method Method to search for
     * @param searchType Search strategy to use
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethodInHierarchy(MethodNode method, SearchType searchType) {
        return this.findMethodInHierarchy(method.name, method.desc, searchType, Traversal.NONE);
    }

    /**
     * Finds the specified private or protected method in this class's hierarchy
     *
     * @param method Method to search for
     * @param searchType Search strategy to use
     * @param traversal Traversal type to allow during this lookup
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethodInHierarchy(MethodNode method, SearchType searchType, Traversal traversal) {
        return this.findMethodInHierarchy(method.name, method.desc, searchType, traversal, 0);
    }

    /**
     * Finds the specified private or protected method in this class's hierarchy
     *
     * @param method Method to search for
     * @param searchType Search strategy to use
     * @param flags search flags
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethodInHierarchy(MethodNode method, SearchType searchType, int flags) {
        return this.findMethodInHierarchy(method.name, method.desc, searchType, Traversal.NONE, flags);
    }

    /**
     * Finds the specified private or protected method in this class's hierarchy
     *
     * @param method Method to search for
     * @param searchType Search strategy to use
     * @param traversal Traversal type to allow during this lookup
     * @param flags search flags
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethodInHierarchy(MethodNode method, SearchType searchType, Traversal traversal, int flags) {
        return this.findMethodInHierarchy(method.name, method.desc, searchType, traversal, flags);
    }

    /**
     * Finds the specified public or protected method in this class's hierarchy
     *
     * @param method Method to search for
     * @param searchType Search strategy to use
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethodInHierarchy(MethodInsnNode method, SearchType searchType) {
        return this.findMethodInHierarchy(method.name, method.desc, searchType, Traversal.NONE);
    }

    /**
     * Finds the specified public or protected method in this class's hierarchy
     *
     * @param method Method to search for
     * @param searchType Search strategy to use
     * @param flags search flags
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethodInHierarchy(MethodInsnNode method, SearchType searchType, int flags) {
        return this.findMethodInHierarchy(method.name, method.desc, searchType, Traversal.NONE, flags);
    }

    /**
     * Finds the specified public or protected method in this class's hierarchy
     *
     * @param name Method name to search for
     * @param desc Method descriptor
     * @param searchType Search strategy to use
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethodInHierarchy(String name, String desc, SearchType searchType) {
        return this.findMethodInHierarchy(name, desc, searchType, Traversal.NONE);
    }

    /**
     * Finds the specified public or protected method in this class's hierarchy
     *
     * @param name Method name to search for
     * @param desc Method descriptor
     * @param searchType Search strategy to use
     * @param traversal Traversal type to allow during this lookup
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethodInHierarchy(String name, String desc, SearchType searchType, Traversal traversal) {
        return this.findMethodInHierarchy(name, desc, searchType, traversal, 0);
    }

    /**
     * Finds the specified public or protected method in this class's hierarchy
     *
     * @param name Method name to search for
     * @param desc Method descriptor
     * @param searchType Search strategy to use
     * @param traversal Traversal type to allow during this lookup
     * @param flags search flags
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethodInHierarchy(String name, String desc, SearchType searchType, Traversal traversal, int flags) {
        return this.findInHierarchy(name, desc, searchType, traversal, flags, Type.METHOD);
    }

    /**
     * Finds the specified private or protected field in this class's hierarchy
     *
     * @param field Field to search for
     * @param searchType Search strategy to use
     * @return the field object or null if the field could not be resolved
     */
    public Field findFieldInHierarchy(FieldNode field, SearchType searchType) {
        return this.findFieldInHierarchy(field.name, field.desc, searchType, Traversal.NONE);
    }

    /**
     * Finds the specified private or protected field in this class's hierarchy
     *
     * @param field Field to search for
     * @param searchType Search strategy to use
     * @param flags search flags
     * @return the field object or null if the field could not be resolved
     */
    public Field findFieldInHierarchy(FieldNode field, SearchType searchType, int flags) {
        return this.findFieldInHierarchy(field.name, field.desc, searchType, Traversal.NONE, flags);
    }

    /**
     * Finds the specified public or protected field in this class's hierarchy
     *
     * @param field Field to search for
     * @param searchType Search strategy to use
     * @return the field object or null if the field could not be resolved
     */
    public Field findFieldInHierarchy(FieldInsnNode field, SearchType searchType) {
        return this.findFieldInHierarchy(field.name, field.desc, searchType, Traversal.NONE);
    }

    /**
     * Finds the specified public or protected field in this class's hierarchy
     *
     * @param field Field to search for
     * @param searchType Search strategy to use
     * @param flags search flags
     * @return the field object or null if the field could not be resolved
     */
    public Field findFieldInHierarchy(FieldInsnNode field, SearchType searchType, int flags) {
        return this.findFieldInHierarchy(field.name, field.desc, searchType, Traversal.NONE, flags);
    }

    /**
     * Finds the specified public or protected field in this class's hierarchy
     *
     * @param name Field name to search for
     * @param desc Field descriptor
     * @param searchType Search strategy to use
     * @return the field object or null if the field could not be resolved
     */
    public Field findFieldInHierarchy(String name, String desc, SearchType searchType) {
        return this.findFieldInHierarchy(name, desc, searchType, Traversal.NONE);
    }

    /**
     * Finds the specified public or protected field in this class's hierarchy
     *
     * @param name Field name to search for
     * @param desc Field descriptor
     * @param searchType Search strategy to use
     * @param traversal Traversal type to allow during this lookup
     * @return the field object or null if the field could not be resolved
     */
    public Field findFieldInHierarchy(String name, String desc, SearchType searchType, Traversal traversal) {
        return this.findFieldInHierarchy(name, desc, searchType, traversal, 0);
    }

    /**
     * Finds the specified public or protected field in this class's hierarchy
     *
     * @param name Field name to search for
     * @param desc Field descriptor
     * @param searchType Search strategy to use
     * @param traversal Traversal type to allow during this lookup
     * @param flags search flags
     * @return the field object or null if the field could not be resolved
     */
    public Field findFieldInHierarchy(String name, String desc, SearchType searchType, Traversal traversal, int flags) {
        return this.findInHierarchy(name, desc, searchType, traversal, flags, Type.FIELD);
    }

    /**
     * Finds a public or protected member in the hierarchy of this class which
     * matches the supplied details
     *
     * @param name Member name to search
     * @param desc Member descriptor
     * @param searchType Search strategy to use
     * @param traversal Traversal type to allow during this lookup
     * @param flags Inclusion flags
     * @param type Type of member to search for (field or method)
     * @return the discovered member or null if the member could not be resolved
     */
    @SuppressWarnings("unchecked")
    private <M extends Member> M findInHierarchy(String name, String desc, SearchType searchType, Traversal traversal, int flags, Type type) {
        if (searchType == SearchType.ALL_CLASSES) {
            M member = this.findMember(name, desc, flags, type);
            if (member != null) {
                return member;
            }

            if (traversal.canTraverse()) {
                for (MixinInfo mixin : this.mixins) {
                    M mixinMember = mixin.getClassInfo().findMember(name, desc, flags, type);
                    if (mixinMember != null) {
                        return this.cloneMember(mixinMember);
                    }
                }
            }
        }

        ClassInfo superClassInfo = this.getSuperClass();
        if (superClassInfo != null) {
            for (ClassInfo superTarget : superClassInfo.getTargets()) {
                M member = superTarget.findInHierarchy(name, desc, SearchType.ALL_CLASSES, traversal.next(), flags & ~ClassInfo.INCLUDE_PRIVATE,
                        type);
                if (member != null) {
                    return member;
                }
            }
        }
        
        if (type == Type.METHOD && (this.isInterface || MixinEnvironment.getCompatibilityLevel().supports(LanguageFeatures.METHODS_IN_INTERFACES))) {
            for (String implemented : this.interfaces) {
                ClassInfo iface = ClassInfo.forName(implemented);
                if (iface == null) {
                    ClassInfo.logger.debug("Failed to resolve declared interface {} on {}", implemented, this.name);
                    continue;
//                    throw new RuntimeException(new ClassNotFoundException(implemented));
                }
                M member = iface.findInHierarchy(name, desc, SearchType.ALL_CLASSES, traversal.next(), flags & ~ClassInfo.INCLUDE_PRIVATE, type);
                if (member != null) {
                    return  this.isInterface ? member : (M)new InterfaceMethod(member);
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
     * @param member member to clone
     * @return wrapper member
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
        return this.findMethod(method.name, method.desc, method.access);
    }

    /**
     * Finds the specified public or protected method in this class
     *
     * @param method Method to search for
     * @param flags search flags
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethod(MethodNode method, int flags) {
        return this.findMethod(method.name, method.desc, flags);
    }

    /**
     * Finds the specified public or protected method in this class
     *
     * @param method Method to search for
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethod(MethodInsnNode method) {
        return this.findMethod(method.name, method.desc, 0);
    }

    /**
     * Finds the specified public or protected method in this class
     *
     * @param method Method to search for
     * @param flags search flags
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethod(MethodInsnNode method, int flags) {
        return this.findMethod(method.name, method.desc, flags);
    }

    /**
     * Finds the specified public or protected method in this class
     *
     * @param name Method name to search for
     * @param desc Method signature to search for
     * @param flags search flags
     * @return the method object or null if the method could not be resolved
     */
    public Method findMethod(String name, String desc, int flags) {
        return this.findMember(name, desc, flags, Type.METHOD);
    }

    /**
     * Finds the specified field in this class
     *
     * @param field Field to search for
     * @return the field object or null if the field could not be resolved
     */
    public Field findField(FieldNode field) {
        return this.findField(field.name, field.desc, field.access);
    }

    /**
     * Finds the specified public or protected method in this class
     *
     * @param field Field to search for
     * @param flags search flags
     * @return the field object or null if the field could not be resolved
     */
    public Field findField(FieldInsnNode field, int flags) {
        return this.findField(field.name, field.desc, flags);
    }

    /**
     * Finds the specified field in this class
     *
     * @param name Field name to search for
     * @param desc Field signature to search for
     * @param flags search flags
     * @return the field object or null if the field could not be resolved
     */
    public Field findField(String name, String desc, int flags) {
        return this.findMember(name, desc, flags, Type.FIELD);
    }

    /**
     * Finds the specified member in this class
     *
     * @param name Field name to search for
     * @param desc Field signature to search for
     * @param flags search flags
     * @param memberType Type of member list to search
     * @return the field object or null if the field could not be resolved
     */
    @SuppressWarnings("unchecked")
    private <M extends Member> M findMember(String name, String desc, int flags, Type memberType) {
        Set<M> members = (Set<M>)(memberType == Type.METHOD ? this.methods : this.fields);

        for (M member : members) {
            if (member.equals(name, desc) && member.matchesFlags(flags)) {
                return member;
            }
        }
        
        if (memberType == Type.METHOD && (flags & ClassInfo.INCLUDE_INITIALISERS) != 0) {
            for (Method ctor : this.initialisers) {
                if (ctor.equals(name, desc) && ctor.matchesFlags(flags)) {
                    return (M)ctor;
                }
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
     * @param classNode classNode to get info for
     * @return ClassInfo instance for the supplied classNode
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
     * from the cache where possible.
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
                ClassNode classNode = MixinService.getService().getBytecodeProvider().getClassNode(className);
                info = new ClassInfo(classNode);
            } catch (Exception ex) {
                ClassInfo.logger.catching(Level.TRACE, ex);
                ClassInfo.logger.warn("Error loading class: {} ({}: {})", className, ex.getClass().getName(), ex.getMessage());
//                ex.printStackTrace();
            }

            // Put null in the cache if load failed
            ClassInfo.cache.put(className, info);
            ClassInfo.logger.trace("Added class metadata for {} to metadata cache", className);
        }

        return info;
    }
    
    /**
     * Return a ClassInfo for the specified type descriptor, fetches the
     * ClassInfo from the cache where possible.
     *
     * @param descriptor Internal descriptor of the type to inspect
     * @param lookup Lookup type to use (literal/element)
     * @return ClassInfo for the specified class name or null if the specified
     *      name cannot be resolved for some reason
     */
    public static ClassInfo forDescriptor(String descriptor, TypeLookup lookup) {
        org.objectweb.asm.Type type; 
        try {
            type = org.objectweb.asm.Type.getObjectType(descriptor);
        } catch (IllegalArgumentException ex) {
            ClassInfo.logger.warn("Error resolving type from descriptor: {}", descriptor);
            return null;
        }
        return ClassInfo.forType(type, lookup);
    }
    
    /**
     * Return a ClassInfo for the specified class type, fetches the ClassInfo
     * from the cache where possible and generates the class meta if not.
     *
     * @param type Type to look up
     * @param lookup Lookup type to use (literal/element)
     * @return ClassInfo for the supplied type or null if the supplied type
     *      cannot be found or is a primitive type
     */
    public static ClassInfo forType(org.objectweb.asm.Type type, TypeLookup lookup) {
        if (type.getSort() == org.objectweb.asm.Type.ARRAY) {
            if (lookup == TypeLookup.ELEMENT_TYPE) {
                return ClassInfo.forType(type.getElementType(), TypeLookup.ELEMENT_TYPE);
            }
            return ClassInfo.OBJECT;
        } else if (type.getSort() < org.objectweb.asm.Type.ARRAY) {
            return null;
        }
        return ClassInfo.forName(type.getClassName().replace('.', '/'));
    }

    /**
     * Return a ClassInfo for the specified class name, but only if the class
     * information already exists in the cache. This prevents class loads in the
     * instance that a ClassInfo is only being retrieved for a class which
     * should definitely have already been processed but will be missing in case
     * of an error condition.
     *
     * @param className Binary name of the class to look up
     * @return ClassInfo for the specified class name or null if the specified
     *      class does not have an entry in the cache
     */
    public static ClassInfo fromCache(String className) {
        return ClassInfo.cache.get(className.replace('.', '/'));
    }

    /**
     * Return a ClassInfo for the specified class type, but only if the class
     * information already exists in the cache. This prevents class loads in the
     * instance that a ClassInfo is only being retrieved for a class which
     * should definitely have already been processed but will be missing in case
     * of an error condition.
     *
     * @param type Type to look up
     * @param lookup Lookup type to use (literal/element)
     * @return ClassInfo for the supplied type or null if the supplied type
     *      does not have an entry in the cache
     */
    public static ClassInfo fromCache(org.objectweb.asm.Type type, TypeLookup lookup) {
        if (type.getSort() == org.objectweb.asm.Type.ARRAY) {
            if (lookup == TypeLookup.ELEMENT_TYPE) {
                return ClassInfo.fromCache(type.getElementType(), TypeLookup.ELEMENT_TYPE);
            }
            return ClassInfo.OBJECT;
        } else if (type.getSort() < org.objectweb.asm.Type.ARRAY) {
            return null;
        }
        return ClassInfo.fromCache(type.getClassName());
    }

    /**
     * ASM logic applied via ClassInfo, returns first common superclass of
     * classes specified by <tt>type1</tt> and <tt>type2</tt>.
     * 
     * @param type1 First type
     * @param type2 Second type
     * @return common superclass info
     */
    public static ClassInfo getCommonSuperClass(String type1, String type2) {
        if (type1 == null || type2 == null) {
            return ClassInfo.OBJECT;
        }
        return ClassInfo.getCommonSuperClass(ClassInfo.forName(type1), ClassInfo.forName(type2));
    }
    
    /**
     * ASM logic applied via ClassInfo, returns first common superclass of
     * classes specified by <tt>type1</tt> and <tt>type2</tt>.
     * 
     * @param type1 First type
     * @param type2 Second type
     * @return common superclass info
     */
    public static ClassInfo getCommonSuperClass(org.objectweb.asm.Type type1, org.objectweb.asm.Type type2) {
        if (type1 == null || type2 == null
                || type1.getSort() != org.objectweb.asm.Type.OBJECT || type2.getSort() != org.objectweb.asm.Type.OBJECT) {
            return ClassInfo.OBJECT;
        }
        return ClassInfo.getCommonSuperClass(ClassInfo.forType(type1, TypeLookup.DECLARED_TYPE), ClassInfo.forType(type2, TypeLookup.DECLARED_TYPE));
    }

    /**
     * ASM logic applied via ClassInfo, returns first common superclass of
     * classes specified by <tt>type1</tt> and <tt>type2</tt>.
     * 
     * @param type1 First type
     * @param type2 Second type
     * @return common superclass info
     */
    private static ClassInfo getCommonSuperClass(ClassInfo type1, ClassInfo type2) {
        if (type1 == null || type2 == null) {
            return ClassInfo.OBJECT;
        }
        return ClassInfo.getCommonSuperClass(type1, type2, false);
    }

    /**
     * ASM logic applied via ClassInfo, returns first common superclass of
     * classes specified by <tt>type1</tt> and <tt>type2</tt>.
     * 
     * @param type1 First type
     * @param type2 Second type
     * @return common superclass info
     */
    public static ClassInfo getCommonSuperClassOrInterface(String type1, String type2) {
        if (type1 == null || type2 == null) {
            return ClassInfo.OBJECT;
        }
        return ClassInfo.getCommonSuperClassOrInterface(ClassInfo.forName(type1), ClassInfo.forName(type2));
    }
    
    /**
     * ASM logic applied via ClassInfo, returns first common superclass of
     * classes specified by <tt>type1</tt> and <tt>type2</tt>.
     * 
     * @param type1 First type
     * @param type2 Second type
     * @return common superclass info
     */
    public static ClassInfo getCommonSuperClassOrInterface(org.objectweb.asm.Type type1, org.objectweb.asm.Type type2) {
        if (type1 == null || type2 == null
                || type1.getSort() != org.objectweb.asm.Type.OBJECT || type2.getSort() != org.objectweb.asm.Type.OBJECT) {
            return ClassInfo.OBJECT;
        }
        return ClassInfo.getCommonSuperClassOrInterface(ClassInfo.forType(type1, TypeLookup.DECLARED_TYPE),
                ClassInfo.forType(type2, TypeLookup.DECLARED_TYPE));
    }

    /**
     * ASM logic applied via ClassInfo, returns first common superclass or
     * interface of classes specified by <tt>type1</tt> and <tt>type2</tt>.
     * 
     * @param type1 First type
     * @param type2 Second type
     * @return common superclass info
     */
    public static ClassInfo getCommonSuperClassOrInterface(ClassInfo type1, ClassInfo type2) {
        return ClassInfo.getCommonSuperClass(type1, type2, true);
    }

    private static ClassInfo getCommonSuperClass(ClassInfo type1, ClassInfo type2, boolean includeInterfaces) {
        if (type1.hasSuperClass(type2, Traversal.NONE, includeInterfaces)) {
            return type2;
        } else if (type2.hasSuperClass(type1, Traversal.NONE, includeInterfaces)) {
            return type1;
        } else if (type1.isInterface() || type2.isInterface()) {
            return ClassInfo.OBJECT;
        }
        
        do {
            type1 = type1.getSuperClass();
            if (type1 == null) {
                return ClassInfo.OBJECT;
            }
        } while (!type2.hasSuperClass(type1, Traversal.NONE, includeInterfaces));
        
        return type1;
    }

}
