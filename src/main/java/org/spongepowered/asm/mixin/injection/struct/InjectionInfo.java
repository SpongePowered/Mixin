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
package org.spongepowered.asm.mixin.injection.struct;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InjectorGroupInfo;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.code.ISliceContext;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.code.InjectorTarget;
import org.spongepowered.asm.mixin.injection.code.MethodSlice;
import org.spongepowered.asm.mixin.injection.code.MethodSlices;
import org.spongepowered.asm.mixin.injection.throwables.InjectionError;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.struct.SpecialMethodInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;

/**
 * Contructs information about an injection from an {@link Inject} annotation
 * and allows the injection to be processed.
 */
public abstract class InjectionInfo extends SpecialMethodInfo implements ISliceContext {
    
    /**
     * Annotated method is static 
     */
    protected final boolean isStatic;
    
    /**
     * Target method(s)
     */
    protected final Deque<MethodNode> targets = new ArrayDeque<MethodNode>();
    
    /**
     * Method slice descriptors parsed from the annotation
     */
    protected final MethodSlices slices;
    
    /**
     * The key into the annotation which contains the injection points
     */
    protected final String atKey;

    /**
     * Injection points parsed from
     * {@link org.spongepowered.asm.mixin.injection.At} annotations
     */
    protected final List<InjectionPoint> injectionPoints = new ArrayList<InjectionPoint>();
    
    /**
     * Map of lists of nodes enumerated by calling {@link #prepare}
     */
    protected final Map<Target, List<InjectionNode>> targetNodes = new LinkedHashMap<Target, List<InjectionNode>>();

    /**
     * Bytecode injector
     */
    protected Injector injector;
    
    /**
     * Injection group
     */
    protected InjectorGroupInfo group;
    
    /**
     * Methods injected by injectors 
     */
    private final List<MethodNode> injectedMethods = new ArrayList<MethodNode>(0);
    
    /**
     * Number of callbacks we expect to inject into targets 
     */
    private int expectedCallbackCount = 1;
    
    /**
     * Number of callbacks we require injected 
     */
    private int requiredCallbackCount = 0;
    
    /**
     * Actual number of injected callbacks
     */
    private int injectedCallbackCount = 0;
    
    /**
     * ctor
     * 
     * @param mixin Mixin data
     * @param method Injector method
     * @param annotation Annotation to parse
     */
    protected InjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        this(mixin, method, annotation, "at");
    }
    
    protected InjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation, String atKey) {
        super(mixin, method, annotation);
        this.isStatic = Bytecode.methodIsStatic(method);
        this.slices = MethodSlices.parse(this);
        this.atKey = atKey;
        this.readAnnotation();
    }

    /**
     * Parse the info from the supplied annotation
     */
    protected void readAnnotation() {
        if (this.annotation == null) {
            return;
        }
        
        String type = "@" + Bytecode.getSimpleName(this.annotation);
        List<AnnotationNode> injectionPoints = this.readInjectionPoints(type);
        this.findMethods(this.parseTargets(type), type);
        this.parseInjectionPoints(injectionPoints);
        this.parseRequirements();
        this.injector = this.parseInjector(this.annotation);
    }

    protected Set<MemberInfo> parseTargets(String type) {
        List<String> methods = Annotations.<String>getValue(this.annotation, "method", false);
        if (methods == null) {
            throw new InvalidInjectionException(this, type + " annotation on " + this.method.name + " is missing method name");
        }
        
        Set<MemberInfo> members = new LinkedHashSet<MemberInfo>();
        for (String method : methods) {
            try {
                MemberInfo targetMember = MemberInfo.parseAndValidate(method, this.mixin);
                if (targetMember.owner != null && !targetMember.owner.equals(this.mixin.getTargetClassRef())) {
                    throw new InvalidInjectionException(this, type + " annotation on " + this.method.name + " specifies a target class '"
                            + targetMember.owner + "', which is not supported");
                }
                members.add(targetMember);
            } catch (InvalidMemberDescriptorException ex) {
                throw new InvalidInjectionException(this, type + " annotation on " + this.method.name + ", has invalid target descriptor: \""
                        + method + "\"");
            }
        }
        return members;
    }

    protected List<AnnotationNode> readInjectionPoints(String type) {
        List<AnnotationNode> ats = Annotations.<AnnotationNode>getValue(this.annotation, this.atKey, false);
        if (ats == null) {
            throw new InvalidInjectionException(this, type + " annotation on " + this.method.name + " is missing '" + this.atKey + "' value(s)");
        }
        return ats;
    }

    protected void parseInjectionPoints(List<AnnotationNode> ats) {
        this.injectionPoints.addAll(InjectionPoint.parse(this.mixin, this.method, this.annotation, ats));
    }

    protected void parseRequirements() {
        this.group = this.mixin.getInjectorGroups().parseGroup(this.method, this.mixin.getDefaultInjectorGroup()).add(this);
        
        Integer expect = Annotations.<Integer>getValue(this.annotation, "expect");
        if (expect != null) {
            this.expectedCallbackCount = expect.intValue();
        }

        Integer require = Annotations.<Integer>getValue(this.annotation, "require");
        if (require != null && require.intValue() > -1) {
            this.requiredCallbackCount = require.intValue();
        } else if (this.group.isDefault()) {
            this.requiredCallbackCount = this.mixin.getDefaultRequiredInjections();
        }
    }

    // stub
    protected abstract Injector parseInjector(AnnotationNode injectAnnotation);
    
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
     * Discover injection points
     */
    public void prepare() {
        this.targetNodes.clear();
        for (MethodNode targetMethod : this.targets) {
            Target target = this.mixin.getTargetMethod(targetMethod);
            InjectorTarget injectorTarget = new InjectorTarget(this, target);
            this.targetNodes.put(target, this.injector.find(injectorTarget, this.injectionPoints));
            injectorTarget.dispose();
        }
    }
    
    /**
     * Perform injections
     */
    public void inject() {
        for (Entry<Target, List<InjectionNode>> entry : this.targetNodes.entrySet()) {
            this.injector.inject(entry.getKey(), entry.getValue());
        }
        this.targets.clear();
    }
    
    /**
     * Perform cleanup and post-injection tasks 
     */
    public void postInject() {
        for (MethodNode method : this.injectedMethods) {
            this.classNode.methods.add(method);
        }
        
        if ((MixinEnvironment.getCurrentEnvironment().getOption(Option.DEBUG_INJECTORS) && this.injectedCallbackCount < this.expectedCallbackCount)) {
            throw new InvalidInjectionException(this,
                    String.format("Injection validation failed: %s %s%s in %s expected %d invocation(s) but %d succeeded",
                    this.getDescription(), this.method.name, this.method.desc, this.mixin, this.expectedCallbackCount, this.injectedCallbackCount));
        } else if (this.injectedCallbackCount < this.requiredCallbackCount) {
            throw new InjectionError(
                    String.format("Critical injection failure: %s %s%s in %s failed injection check, (%d/%d) succeeded",
                    this.getDescription(), this.method.name, this.method.desc, this.mixin, this.injectedCallbackCount, this.requiredCallbackCount));
        }
    }
    
    /**
     * Callback from injector which notifies us that a callback was injected. No
     * longer used.
     * 
     * @param target target into which the injector injected
     */
    public void notifyInjected(Target target) {
//        this.targets.remove(target.method);
    }

    
    protected String getDescription() {
        return "Callback method";
    }

    @Override
    public String toString() {
        return InjectionInfo.describeInjector(this.mixin, this.annotation, this.method);
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
     * Get the slice descriptors
     */
    @Override
    public MethodSlice getSlice(String id) {
        return this.slices.get(this.getSliceId(id));
    }
    
    /**
     * Return the mapped slice id for the specified ID. Injectors which only
     * support use of a single slice will always return the default id (an empty
     * string)
     * 
     * @param id slice id
     * @return mapped id
     */
    public String getSliceId(String id) {
        return "";
    }

    /**
     * Get the injected callback count
     * 
     * @return the injected callback count
     */
    public int getInjectedCallbackCount() {
        return this.injectedCallbackCount;
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
     * Notify method, called by injector when adding a callback into a target
     * 
     * @param handler callback handler being invoked
     */
    public void addCallbackInvocation(MethodNode handler) {
        this.injectedCallbackCount++;
    }
    
    /**
     * Finds methods in the target class which match searchFor
     * 
     * @param searchFor members to search for
     * @param type annotation type
     */
    private void findMethods(Set<MemberInfo> searchFor, String type) {
        this.targets.clear();

        for (MemberInfo member : searchFor) {
            int ordinal = 0;
            for (MethodNode target : this.classNode.methods) {
                if (member.matches(target.name, target.desc, ordinal)) {
                    boolean isMixinMethod = Annotations.getVisible(target, MixinMerged.class) != null;
                    if (member.matchAll && (Bytecode.methodIsStatic(target) != this.isStatic || target == this.method || isMixinMethod)) {
                        continue;
                    }
                    
                    this.checkTarget(target);
                    
                    this.targets.add(target);
                    ordinal++;
                }
            }
        }
        
        if (this.targets.size() == 0) {
            throw new InvalidInjectionException(this, type + " annotation on " + this.method.name + " could not find any targets matching "
                    + InjectionInfo.namesOf(searchFor));
        }
    }

    private void checkTarget(MethodNode target) {
        AnnotationNode merged = Annotations.getVisible(target, MixinMerged.class);
        if (merged == null) {
            return;
        }
        
        String owner = Annotations.<String>getValue(merged, "mixin");
        int priority = Annotations.<Integer>getValue(merged, "priority");
        
        if (priority >= this.mixin.getPriority() && !owner.equals(this.mixin.getClassName())) {
            throw new InvalidInjectionException(this, this + " cannot inject into " + this.classNode.name + "::" + target.name + target.desc
                    + " merged by " + owner + " with priority " + priority);
        }
        
        if (Annotations.getVisible(target, Final.class) != null) {
            throw new InvalidInjectionException(this, this + " cannot inject into @Final method " + this.classNode.name + "::" + target.name
                    + target.desc + " merged by " + owner);
        }
    }
    
    /**
     * Parse an injector from the specified method (if an injector annotation is
     * present). If no injector annotation is present then <tt>null</tt> is
     * returned.
     * 
     * @param mixin context
     * @param method mixin method
     * @return parsed InjectionInfo or null
     */
    public static InjectionInfo parse(MixinTargetContext mixin, MethodNode method) {
        AnnotationNode annotation = InjectionInfo.getInjectorAnnotation(mixin.getMixin(), method);
        
        if (annotation == null) {
            return null;
        }
        
        if (annotation.desc.endsWith(Inject.class.getSimpleName() + ";")) {
            return new CallbackInjectionInfo(mixin, method, annotation);
        } else if (annotation.desc.endsWith(ModifyArg.class.getSimpleName() + ";")) {
            return new ModifyArgInjectionInfo(mixin, method, annotation);
        } else if (annotation.desc.endsWith(ModifyArgs.class.getSimpleName() + ";")) {
            return new ModifyArgsInjectionInfo(mixin, method, annotation);
        } else if (annotation.desc.endsWith(Redirect.class.getSimpleName() + ";")) {
            return new RedirectInjectionInfo(mixin, method, annotation);
        } else if (annotation.desc.endsWith(ModifyVariable.class.getSimpleName() + ";")) {
            return new ModifyVariableInjectionInfo(mixin, method, annotation);
        } else if (annotation.desc.endsWith(ModifyConstant.class.getSimpleName() + ";")) {
            return new ModifyConstantInjectionInfo(mixin, method, annotation);
        }
        
        return null;
    }

    /**
     * Returns any injector annotation found on the specified method. If
     * multiple matching annotations are found then an exception is thrown. If
     * no annotations are present then <tt>null</tt> is returned.
     * 
     * @param mixin context
     * @param method mixin method
     * @return annotation or null
     */
    @SuppressWarnings("unchecked")
    public static AnnotationNode getInjectorAnnotation(IMixinInfo mixin, MethodNode method) {
        AnnotationNode annotation = null;
        try {
            annotation = Annotations.getSingleVisible(method,
                Inject.class,
                ModifyArg.class,
                ModifyArgs.class,
                Redirect.class,
                ModifyVariable.class,
                ModifyConstant.class
            );
        } catch (IllegalArgumentException ex) {
            throw new InvalidMixinException(mixin, "Error parsing annotations on " + method.name + " in " + mixin.getClassName() + ": "
                    + ex.getMessage());
        }
        return annotation;
    }

    /**
     * Get the conform prefix for an injector handler by type
     * 
     * @param annotation Annotation to inspect
     * @return conform prefix
     */
    public static String getInjectorPrefix(AnnotationNode annotation) {
        if (annotation != null) {
            if (annotation.desc.endsWith(ModifyArg.class.getSimpleName() + ";")) {
                return "modify";
            } else if (annotation.desc.endsWith(ModifyArgs.class.getSimpleName() + ";")) {
                return "args";
            } else if (annotation.desc.endsWith(Redirect.class.getSimpleName() + ";")) {
                return "redirect";
            } else if (annotation.desc.endsWith(ModifyVariable.class.getSimpleName() + ";")) {
                return "localvar";
            } else if (annotation.desc.endsWith(ModifyConstant.class.getSimpleName() + ";")) {
                return "constant";
            }
        }
        return "handler";
    }

    static String describeInjector(IMixinContext mixin, AnnotationNode annotation, MethodNode method) {
        return String.format("%s->@%s::%s%s", mixin.toString(), Bytecode.getSimpleName(annotation), method.name, method.desc);
    }

    /**
     * Print the names of the specified members as a human-readable list 
     * 
     * @param members members to print
     * @return human-readable list of member names
     */
    private static String namesOf(Collection<MemberInfo> members) {
        int index = 0, count = members.size();
        StringBuilder sb = new StringBuilder();
        for (MemberInfo member : members) {
            if (index > 0) {
                if (index == (count - 1)) {
                    sb.append(" or ");
                } else {
                    sb.append(", ");
                }
            }
            sb.append('\'').append(member.name).append('\'');
            index++;
        }
        return sb.toString();
    }

}
