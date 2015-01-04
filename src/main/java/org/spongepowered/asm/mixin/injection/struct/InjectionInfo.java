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

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InvalidInjectionException;
import org.spongepowered.asm.mixin.injection.callback.CallbackInjector;
import org.spongepowered.asm.mixin.transformer.MixinData;
import org.spongepowered.asm.util.ASMHelper;


/**
 * Contructs information about an injection from an {@link Inject} annotation and allows the injection to be processed
 */
public class InjectionInfo {
    
    /**
     * Class
     */
    private final ClassNode classNode;
    
    /**
     * Annotated method
     */
    private final MethodNode method;

    /**
     * Annotated method is static 
     */
    private final boolean isStatic;
    
    /**
     * True if the injection should be cancellable
     */
    private boolean cancellable;
    
    /**
     * True if the injection should capture local variables
     */
    private boolean captureLocals;
    
    /**
     * Target method(s)
     */
    private final Deque<MethodNode> targets = new ArrayDeque<MethodNode>();
    
    /**
     * Injection points parsed from {@link At} annotations
     */
    private final List<InjectionPoint> injectionPoints = new ArrayList<InjectionPoint>();
    
    /**
     * Bytecode injector
     */
    private CallbackInjector injector;
    
    /**
     * ctor
     * 
     * @param classNode Class
     * @param method Method
     * @param injectAnnotation Annotation to parse
     */
    public InjectionInfo(ClassNode classNode, MethodNode method, MixinData mixin, AnnotationNode injectAnnotation) {
        this.classNode = classNode;
        this.method = method;
        this.isStatic = ASMHelper.methodIsStatic(method);
        this.parse(mixin, injectAnnotation);
    }

    /**
     * Parse the info from the supplied annotation
     */
    private void parse(MixinData mixin, AnnotationNode injectAnnotation) {
        if (injectAnnotation == null) {
            return;
        }
        
        String method = ASMHelper.<String>getAnnotationValue(injectAnnotation, "method");
        if (method == null) {
            throw new InvalidInjectionException("@Inject annotation on " + this.method.name + " is missing method name");
        }
        
        List<AnnotationNode> ats = ASMHelper.<List<AnnotationNode>>getAnnotationValue(injectAnnotation, "at");
        if (ats == null) {
            throw new InvalidInjectionException("@Inject annotation on " + this.method.name + " is missing 'at' value(s)");
        }
        
        MemberInfo targetMember = MemberInfo.parse(method, mixin);
        
        if (targetMember.owner != null && targetMember.owner.equals(mixin.getTargetClassRef())) {
            throw new InvalidInjectionException("@Inject annotation on " + this.method.name + " specifies a target class '" + targetMember.owner
                    + "', which is not supported");
        }
        
        this.findMethods(targetMember);
        
        if (this.targets.size() == 0) {
            throw new InvalidInjectionException("@Inject annotation on " + this.method.name + " could not find '" + targetMember.name + "'");
        }
        
        this.cancellable = ASMHelper.<Boolean>getAnnotationValue(injectAnnotation, "cancellable", false);
        this.captureLocals = ASMHelper.<Boolean>getAnnotationValue(injectAnnotation, "captureLocals", false);
        
        for (AnnotationNode at : ats) {
            InjectionPoint injectionPoint = InjectionPoint.parse(mixin, at);
            if (injectionPoint != null) {
                this.injectionPoints.add(injectionPoint);
            }
        }
    }
    
    /**
     * Get whether there is enough valid information in this info to actually perform an injection
     */
    public boolean isValid() {
        return this.targets.size() > 0 && this.injectionPoints.size() > 0;
    }
    
    /**
     * Perform the injection
     */
    public void inject() {
        CallbackInjector injector = this.getInjector();
        while (this.targets.size() > 0) {
            injector.injectInto(this.targets.removeFirst(), this.injectionPoints);
        }
    }
    
    /**
     * Performs a full-body massage with scented oils
     */
    private CallbackInjector getInjector() {
        if (this.injector == null) {
            this.injector = new CallbackInjector(this);
        }
        
        return this.injector;
    }

    /**
     * Get the class node for this injection
     */
    public ClassNode getClassNode() {
        return this.classNode;
    }

    /**
     * Get method being called
     */
    public MethodNode getMethod() {
        return this.method;
    }
    
    /**
     * Get methods being injected into
     */
    public Collection<MethodNode> getTargets() {
        return this.targets;
    }

    /**
     * Get whether cancellable or not
     */
    public boolean getCancellable() {
        return this.cancellable;
    }

    /**
     * Get whether injection should capture locals
     */
    public boolean getCaptureLocals() {
        return this.captureLocals;
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
                
                this.targets.add(target);
                ordinal++;
            }
        }
    }
}
