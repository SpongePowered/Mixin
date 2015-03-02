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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.spongepowered.asm.mixin.SoftOverride;
import org.spongepowered.asm.mixin.injection.struct.ReferenceMapper;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Method;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Traversal;
import org.spongepowered.asm.util.ASMHelper;


/**
 * This object keeps track of data for applying a mixin to a specific target
 * class <em>during</em> a mixin application. This is a single-use object which
 * acts as both a handle information we need when applying the mixin (such as
 * the actual mixin ClassNode and the target ClassNode) and a gateway to
 * context-sensitive operations such as re-targetting method and field accesses
 * in the mixin to the appropriate members in the target class hierarchy. 
 */
public class MixinTargetContext implements IReferenceMapperContext {
    
    private static final String INIT = "<init>";
    private static final String IMAGINARY_SUPER = "super$";

    /**
     * Mixin info
     */
    private final MixinInfo mixin;
    
    /**
     * Tree
     */
    private final ClassNode classNode;
    
    /**
     * 
     */
    private final ClassNode targetClass;
    
    /**
     * Target ClassInfo
     */
    private final ClassInfo targetClassInfo;
    
    /**
     * Information about methods in the target class, used to keep track of
     * transformations we apply
     */
    private final Map<String, Target> targetMethods = new HashMap<String, Target>();
    
    /**
     * True if this mixin inherits from a mixin at any point in its hierarchy 
     */
    private final boolean inheritsFromMixin;
    
    /**
     * True if this mixin's superclass is detached from the target superclass 
     */
    private final boolean detachedSuper;

    /**
     * ctor
     * 
     * @param info Mixin information
     * @param mixin Mixin classnode
     * @param target target class
     */
    MixinTargetContext(MixinInfo info, ClassNode mixin, ClassNode target) {
        this.mixin = info;
        this.classNode = mixin;
        this.targetClass = target;
        this.targetClassInfo = ClassInfo.forName(target.name);
        this.inheritsFromMixin = info.getClassInfo().hasMixinInHierarchy() || this.targetClassInfo.hasMixinTargetInHierarchy();
        this.detachedSuper = !this.getClassNode().superName.equals(this.targetClass.superName);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.mixin.toString();
    }

    /**
     * Get the mixin tree
     * 
     * @return mixin tree
     */
    public ClassNode getClassNode() {
        return this.classNode;
    }
    
    /**
     * Get the mixin class name
     * 
     * @return the mixin class name
     */
    public String getClassName() {
        return this.mixin.getClassName();
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.IReferenceMapperContext
     *      #getClassRef()
     */
    @Override
    public String getClassRef() {
        return this.mixin.getClassRef();
    }

    /**
     * Get the target class reference
     * 
     * @return the reference of the target class (only valid on single-target
     *      mixins)
     */
    public String getTargetClassRef() {
        return this.targetClass.name;
    }
    
    /**
     * Get the target class
     * 
     * @return the target class
     */
    public ClassNode getTargetClass() {
        return this.targetClass;
    }
    
    /**
     * Get the target classinfo
     * 
     * @return the target class info 
     */
    public ClassInfo getTargetClassInfo() {
        return this.targetClassInfo;
    }

    /**
     * Find the corresponding class type for the supplied mixin class in this
     * mixin target's hierarchy
     * 
     * @param mixin Mixin class to discover
     * @return Transformed
     */
    public ClassInfo findRealType(ClassInfo mixin) {
        if (mixin == this.mixin.getClassInfo()) {
            return this.targetClassInfo;
        }
        
        ClassInfo type = this.targetClassInfo.findCorrespondingType(mixin);
        if (type == null) {
            throw new InvalidMixinException(this, "Resolution error: unable to find corresponding type for "
                    + mixin + " in hierarchy of " + this.targetClassInfo);
        }
        
        return type;
    }

    /**
     * Transforms field descriptors which contain mixin types to their
     * appropriate target type
     * 
     * @param field Field to transform
     * @return true if the field should be processed further, false to remove it
     */
    public boolean transformField(FieldNode field) {
        if (MixinTargetContext.IMAGINARY_SUPER.equals(field.name)) {
            if (field.access != Opcodes.ACC_PRIVATE) {
                throw new InvalidMixinException(this, "Imaginary super field " + field.name + " must be private and non-final");
            }
            if (!field.desc.equals("L" + this.mixin.getClassRef() + ";")) {
                throw new InvalidMixinException(this, "Imaginary super field " + field.name + " must have the same type as the parent mixin");
            }
            return false;
        }

        this.transformDescriptor(field);
        return true;
    }

    /**
     * Handles "re-parenting" the method supplied, changes all references to the
     * mixin class to refer to the target class (for field accesses and method
     * invokations) and also handles fixing up the targets of INVOKESPECIAL
     * opcodes for mixins with detached targets.
     * 
     * @param method Method to transform
     */
    public void transformMethod(MethodNode method) {
        // Any method tagged with @SoftOverride must have an implementation visible from 
        if (ASMHelper.getInvisibleAnnotation(method, SoftOverride.class) != null) {
            Method superMethod = this.targetClassInfo.findMethodInHierarchy(method.name, method.desc, false, Traversal.SUPER);
            if (superMethod == null || !superMethod.isInjected()) {
                throw new InvalidMixinException(this, "Mixin method " + method.name + method.desc + " is tagged with @SoftOverride but no "
                        + "valid method was found in superclasses of " + this.targetClass.name);
            }
        }
        
        this.transformDescriptor(method);
        
        Iterator<AbstractInsnNode> iter = method.instructions.iterator();
        while (iter.hasNext()) {
            AbstractInsnNode insn = iter.next();

            if (insn instanceof MethodInsnNode) {
                MethodInsnNode methodInsn = (MethodInsnNode)insn;
                this.transformDescriptor(methodInsn);
                if (methodInsn.owner.equals(this.getClassRef())) {
                    methodInsn.owner = this.targetClass.name;
                } else if ((this.detachedSuper || this.inheritsFromMixin)) {
                    if (methodInsn.getOpcode() == Opcodes.INVOKESPECIAL) {
                        this.updateStaticBinding(method, methodInsn);
                    } else if (methodInsn.getOpcode() == Opcodes.INVOKEVIRTUAL && ClassInfo.forName(methodInsn.owner).isMixin()) {
                        this.updateDynamicBinding(method, methodInsn);
                    }
                }
            } else if (insn instanceof FieldInsnNode) {
                FieldInsnNode fieldInsn = (FieldInsnNode)insn;
                if (MixinTargetContext.IMAGINARY_SUPER.equals(fieldInsn.name)) {
                    this.processImaginarySuper(method, fieldInsn);
                    iter.remove();
                }
                this.transformDescriptor(fieldInsn);
                if (fieldInsn.owner.equals(this.getClassRef())) {
                    fieldInsn.owner = this.targetClass.name;
                }
            } else if (insn instanceof TypeInsnNode) {
                TypeInsnNode typeInsn = (TypeInsnNode)insn;
                if (typeInsn.desc.equals(this.getClassRef())) {
                    typeInsn.desc = this.targetClass.name;
                }
                this.transformDescriptor(typeInsn);
            }
        }
    }

    /**
     * Handle "imaginary super" invokations, these are invokations in
     * non-derived mixins for accessing methods known to exist in a supermixin
     * which is not directly inherited by this mixix. The method can only call
     * its <b>own</b> super-implmentation and the methd must also be tagged with
     * {@link SoftOverride} to indicate that the method must exist in a super
     * class.
     * 
     * @param method Method being processed
     * @param fieldInsn the GETFIELD insn which access the pseudo-field which is
     *      used as a handle to the superclass
     */
    private void processImaginarySuper(MethodNode method, FieldInsnNode fieldInsn) {
        if (fieldInsn.getOpcode() != Opcodes.GETFIELD) {
            if (MixinTargetContext.INIT.equals(method.name)) {
                throw new InvalidMixinException(this, "Illegal imaginary super declaration: field " + fieldInsn.name
                        + " must not specify an initialiser");
            }
            
            throw new InvalidMixinException(this, "Illegal imaginary super access: found " + ASMHelper.getOpcodeName(fieldInsn.getOpcode())
                    + " opcode in " + method.name + method.desc);
        }
        
        if ((method.access & Opcodes.ACC_PRIVATE) != 0 || (method.access & Opcodes.ACC_STATIC) != 0) {
            throw new InvalidMixinException(this, "Illegal imaginary super access: method " + method.name + method.desc
                    + " is private or static");
        }
        
        if (ASMHelper.getInvisibleAnnotation(method, SoftOverride.class) == null) {
            throw new InvalidMixinException(this, "Illegal imaginary super access: method " + method.name + method.desc
                    + " is not decorated with @SoftOverride");
        }
        
        for (Iterator<AbstractInsnNode> methodIter = method.instructions.iterator(method.instructions.indexOf(fieldInsn)); methodIter.hasNext();) {
            AbstractInsnNode insn = methodIter.next();
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode methodNode = (MethodInsnNode)insn;
                if (methodNode.owner.equals(this.getClassRef()) && methodNode.name.equals(method.name) && methodNode.desc.equals(method.desc)) {
                    methodNode.setOpcode(Opcodes.INVOKESPECIAL);
                    this.updateStaticBinding(method, methodNode);
                    return;
                }
            }
        }
        
        throw new InvalidMixinException(this, "Illegal imaginary super access: could not find INVOKE for " + method.name + method.desc);
    }

    /**
     * Update INVOKESPECIAL opcodes to target the topmost class in the hierarchy
     * which contains the specified method.
     * 
     * @param method Method containing the instruction
     * @param insn INVOKE instruction node
     */
    private void updateStaticBinding(MethodNode method, MethodInsnNode insn) {
        this.updateBinding(method, insn, Traversal.SUPER);
    }

    /**
     * Update INVOKEVIRTUAL opcodes to target the topmost class in the hierarchy
     * which contains the specified method.
     * 
     * @param method Method containing the instruction
     * @param insn INVOKE instruction node
     */
    private void updateDynamicBinding(MethodNode method, MethodInsnNode insn) {
        this.updateBinding(method, insn, Traversal.ALL);
    }
    
    private void updateBinding(MethodNode method, MethodInsnNode insn, Traversal traversal) {
        if (INIT.equals(method.name) || insn.owner.equals(this.targetClass.name) || this.targetClass.name.startsWith("<")) {
            return;
        }
        
        Method superMethod = this.targetClassInfo.findMethodInHierarchy(insn.name, insn.desc, traversal == Traversal.ALL, traversal);
        if (superMethod != null) {
            if (superMethod.getOwner().isMixin()) {
                throw new InvalidMixinException(this, "Invalid " + ASMHelper.getOpcodeName(insn) + " in " + this + " resolved " + insn.owner
                        + " -> " + superMethod.getOwner() + " for " + insn.name + insn.desc);
            }
            insn.owner = superMethod.getOwner().getName();
        } else if (ClassInfo.forName(insn.owner).isMixin()) {
            throw new MixinTransformerError("Error resolving " + ASMHelper.getOpcodeName(insn) + " target for " + insn.owner + "." + insn.name
                    + " in " + this);
        }
    }
    
    /**
     * Transforms a field descriptor in the context of this mixin target
     * 
     * @param field Field node to transform
     */
    public void transformDescriptor(FieldNode field) {
        if (!this.inheritsFromMixin) {
            return;
        }
        field.desc = this.transformSingleDescriptor(field.desc, false);
    }
    
    /**
     * Transforms a field insn descriptor in the context of this mixin target
     * 
     * @param field Field instruction node to transform
     */
    public void transformDescriptor(FieldInsnNode field) {
        if (!this.inheritsFromMixin) {
            return;
        }
        field.desc = this.transformSingleDescriptor(field.desc, false);
    }
    
    /**
     * Transforms a method descriptor in the context of this mixin target
     * 
     * @param method Method node to transform
     */
    public void transformDescriptor(MethodNode method) {
        if (!this.inheritsFromMixin) {
            return;
        }
        method.desc = this.transformMethodDescriptor(method.desc);
    }

    /**
     * Transforms a method insn descriptor in the context of this mixin target
     * 
     * @param method Method instruction node to transform
     */
    public void transformDescriptor(MethodInsnNode method) {
        if (!this.inheritsFromMixin) {
            return;
        }
        method.desc = this.transformMethodDescriptor(method.desc);
    }
    
    /**
     * Transforms a type insn descriptor in the context of this mixin target
     * 
     * @param typeInsn Type instruction node to transform
     */
    public void transformDescriptor(TypeInsnNode typeInsn) {
        if (!this.inheritsFromMixin) {
            return;
        }
        typeInsn.desc = this.transformSingleDescriptor(typeInsn.desc, true);
    }
    
    private String transformSingleDescriptor(Type type) {
        if (type.getSort() < Type.ARRAY) {
            return type.toString();
        }

        return this.transformSingleDescriptor(type.toString(), false);
    }
    
    private String transformSingleDescriptor(String desc, boolean isObject) {
        String type = desc;
        while (type.startsWith("[") || type.startsWith("L")) {
            if (type.startsWith("[")) {
                type = type.substring(1);
                continue;
            }
            type = type.substring(1, type.indexOf(";"));
            isObject = true;
        }
        
        if (!isObject) {
            return desc;
        }
        
        ClassInfo typeInfo = ClassInfo.forName(type);
        
        if (!typeInfo.isMixin()) {
            return desc;
        }
        
        return desc.replace(type, this.findRealType(typeInfo).toString());
    }
    
    private String transformMethodDescriptor(String desc) {
        StringBuilder newDesc = new StringBuilder();
        newDesc.append('(');
        for (Type arg : Type.getArgumentTypes(desc)) {
            newDesc.append(this.transformSingleDescriptor(arg));
        }
        return newDesc.append(')').append(this.transformSingleDescriptor(Type.getReturnType(desc))).toString();
    }

    /**
     * Get a target method handle from the target class
     * 
     * @param method method to get a target handle for
     * @return new or existing target handle for the supplied method
     */
    public Target getTargetMethod(MethodNode method) {
        if (!this.targetClass.methods.contains(method)) {
            throw new IllegalArgumentException("Invalid target method supplied to getTargetMethod()");
        }
        
        String targetName = method.name + method.desc;
        Target target = this.targetMethods.get(targetName);
        if (target == null) {
            target = new Target(method);
            this.targetMethods.put(targetName, target);
        }
        return target;
    }
    
    /**
     * Get the mixin info for this mixin
     */
    MixinInfo getInfo() {
        return this.mixin;
    }

    /**
     * Get the mixin priority
     * 
     * @return the priority (only meaningful in relation to other mixins)
     */
    public int getPriority() {
        return this.mixin.getPriority();
    }

    /**
     * Get all interfaces for this mixin
     * 
     * @return mixin interfaces
     */
    public Set<String> getInterfaces() {
        return this.mixin.getInterfaces();
    }

    /**
     * Get whether to propogate the source file attribute from a mixin onto the
     * target class
     * 
     * @return true if the sourcefile property should be set on the target class
     */
    public boolean shouldSetSourceFile() {
        return this.mixin.getParent().shouldSetSourceFile();
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.IReferenceMapperContext
     *      #getReferenceMapper()
     */
    @Override
    public ReferenceMapper getReferenceMapper() {
        return this.mixin.getParent().getReferenceMapper();
    }

    /**
     * Called immediately before the mixin is applied to targetClass
     * 
     * @param transformedName Target class's transformed name
     * @param targetClass Target class
     */
    public void preApply(String transformedName, ClassNode targetClass) {
        this.mixin.preApply(transformedName, targetClass);
    }

    /**
     * Called immediately after the mixin is applied to targetClass
     * 
     * @param transformedName Target class's transformed name
     * @param targetClass Target class
     */
    public void postApply(String transformedName, ClassNode targetClass) {
        this.mixin.postApply(transformedName, targetClass);
    }
}
