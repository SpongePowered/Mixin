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
package org.spongepowered.asm.mixin.injection.struct;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InvalidInjectionException;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.transformer.InvalidMixinException;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.util.ASMHelper;


/**
 * Contructs information about an injection from an {@link Inject} annotation
 * and allows the injection to be processed.
 */
public abstract class InjectionInfo {
    
    protected final AnnotationNode annotation;
    
    /**
     * Class
     */
    protected final ClassNode classNode;
    
    /**
     * Annotated method
     */
    protected final MethodNode method;

    /**
     * Mixin data
     */
    protected final MixinTargetContext mixin;


    /**
     * Annotated method is static 
     */
    protected final boolean isStatic;
    
    /**
     * Target method(s)
     */
    protected final Deque<MethodNode> targets = new ArrayDeque<MethodNode>();
    
    /**
     * Injection points parsed from
     * {@link org.spongepowered.asm.mixin.injection.At} annotations
     */
    protected final List<InjectionPoint> injectionPoints = new ArrayList<InjectionPoint>();
    
    /**
     * Bytecode injector
     */
    protected Injector injector;
    
    /**
     * Methods injected by injectors 
     */
    private final List<MethodNode> injectedMethods = new ArrayList<MethodNode>(0);
    
    /**
     * ctor
     * 
     * @param mixin Mixin data
     * @param method Injector method
     * @param annotation Annotation to parse
     */
    protected InjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        this.annotation = annotation;
        this.method = method;
        this.mixin = mixin;
        this.classNode = mixin.getTargetClass();
        this.isStatic = ASMHelper.methodIsStatic(method);
        this.readAnnotation();
    }

    /**
     * Parse the info from the supplied annotation
     */
    @SuppressWarnings("unchecked")
    protected void readAnnotation() {
        if (this.annotation == null) {
            return;
        }
        
        String type = "@" + this.annotation.desc.substring(this.annotation.desc.lastIndexOf('/') + 1, this.annotation.desc.length() - 1);
        
        String method = ASMHelper.<String>getAnnotationValue(this.annotation, "method");
        if (method == null) {
            throw new InvalidInjectionException(this, type + " annotation on " + this.method.name + " is missing method name");
        }
        
        List<AnnotationNode> ats = null;
        Object atValue = ASMHelper.getAnnotationValue(this.annotation, "at");
        if (atValue instanceof List) {
            ats = (List<AnnotationNode>)atValue;
        } else if (atValue instanceof AnnotationNode) {
            ats = new ArrayList<AnnotationNode>();
            ats.add((AnnotationNode)atValue);
        } else {
            throw new InvalidInjectionException(this, type + " annotation on " + this.method.name + " is missing 'at' value(s)");
        }
        
        MemberInfo targetMember = MemberInfo.parse(method, this.mixin);
        
        if (targetMember.owner != null && !targetMember.owner.equals(this.mixin.getTargetClassRef())) {
            throw new InvalidInjectionException(this, type + " annotation on " + this.method.name + " specifies a target class '"
                    + targetMember.owner + "', which is not supported");
        }
        
        this.findMethods(targetMember);
        
        if (this.targets.size() == 0) {
            throw new InvalidInjectionException(this, type + " annotation on " + this.method.name + " could not find '" + targetMember.name + "'");
        }
        
        for (AnnotationNode at : ats) {
            InjectionPoint injectionPoint = InjectionPoint.parse(this.mixin, at);
            if (injectionPoint != null) {
                this.injectionPoints.add(injectionPoint);
            }
        }
        
        this.injector = this.initInjector(this.annotation);
    }

    // stub
    protected abstract Injector initInjector(AnnotationNode injectAnnotation);
    
    /**
     * Get whether there is enough valid information in this info to actually
     * perform an injection.
     * 
     * @return true if this InjectionInfo was successfully parsed
     */
    public boolean isValid() {
        return this.targets.size() > 0 && this.injectionPoints.size() > 0;
    }
    
    /**
     * Perform the injection
     */
    public void inject() {
        while (this.targets.size() > 0) {
            Target target = this.mixin.getTargetMethod(this.targets.removeFirst());
            this.injector.injectInto(target, this.injectionPoints);
        }
    }
    
    /**
     * Perform cleanup and post-injection tasks 
     */
    public void postInject() {
        for (MethodNode method : this.injectedMethods) {
            this.classNode.methods.add(method);
        }
    }
    
    /**
     * Get the mixin target context for this injection
     * 
     * @return the target context
     */
    public MixinTargetContext getContext() {
        return this.mixin;
    }
    
    /**
     * Get the annotation which this InjectionInfo was created from
     *  
     * @return The annotation which this InjectionInfo was created from 
     */
    public AnnotationNode getAnnotation() {
        return this.annotation;
    }

    /**
     * Get the class node for this injection
     * 
     * @return the class containing the injector and the target
     */
    public ClassNode getClassNode() {
        return this.classNode;
    }

    /**
     * Get method being called
     * 
     * @return injector method
     */
    public MethodNode getMethod() {
        return this.method;
    }
    
    /**
     * Get methods being injected into
     * 
     * @return methods being injected into
     */
    public Collection<MethodNode> getTargets() {
        return this.targets;
    }

    /**
     * Inject a method into the target class
     * 
     * @param access Method access flags, synthetic will be automatically added
     * @param name Method name
     * @param desc Method descriptor
     * 
     * @return new method
     */
    public MethodNode addMethod(int access, String name, String desc) {
        MethodNode method = new MethodNode(Opcodes.ASM5, access | Opcodes.ACC_SYNTHETIC, name, desc, null, null);
        this.injectedMethods.add(method);
        return method;
    }
    
    /**
     * Finds methods in the target class which match searchFor
     * 
     * @param searchFor member info to search for
     */
    private void findMethods(MemberInfo searchFor) {
        this.targets.clear();
        int ordinal = 0;
        
        for (MethodNode target : this.classNode.methods) {
            if (searchFor.matches(target.name, target.desc, ordinal)) {
                if (searchFor.matchAll && ASMHelper.methodIsStatic(target) != this.isStatic) {
                    continue;
                }
                
                AnnotationNode merged = ASMHelper.getVisibleAnnotation(target, MixinMerged.class);
                if (merged != null) {
                    throw new InvalidInjectionException(this, "Cannot inject into a mixin method");
                }

                this.targets.add(target);
                ordinal++;
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public static InjectionInfo parse(MixinTargetContext mixin, MethodNode method) {
        AnnotationNode annotation = null;
        try {
            annotation = ASMHelper.getSingleVisibleAnnotation(method, Inject.class, ModifyArg.class, Redirect.class);
        } catch (IllegalArgumentException ex) {
            throw new InvalidMixinException(mixin, "Error parsing annotations on " + method.name + " in " + mixin.getClassName() + ": "
                    + ex.getMessage());
        }
        
        if (annotation == null) {
            return null;
        }
        
        if (annotation.desc.endsWith(Inject.class.getSimpleName() + ";")) {
            return new CallbackInjectionInfo(mixin, method, annotation);
        } else if (annotation.desc.endsWith(ModifyArg.class.getSimpleName() + ";")) {
            return new ModifyArgInjectionInfo(mixin, method, annotation);
        } else if (annotation.desc.endsWith(Redirect.class.getSimpleName() + ";")) {
            return new RedirectInjectionInfo(mixin, method, annotation);
        }
        
        return null;
    }
}
