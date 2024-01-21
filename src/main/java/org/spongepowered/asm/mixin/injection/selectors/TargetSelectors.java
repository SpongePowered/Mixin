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
package org.spongepowered.asm.mixin.injection.selectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector.Configure;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelector.Result;
import org.spongepowered.asm.mixin.injection.selectors.throwables.SelectorConstraintException;
import org.spongepowered.asm.mixin.injection.struct.InvalidMemberDescriptorException;
import org.spongepowered.asm.mixin.injection.struct.TargetNotSupportedException;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.struct.AnnotatedMethodInfo;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;

public class TargetSelectors implements Iterable<TargetSelectors.SelectedMethod> {
    
    /**
     * Selected target method, paired with the selector which identified it
     */
    public static class SelectedMethod {
        
        /**
         * The parent target of this target. If this target is a lambda then
         * this will be the selector for the enclosing method. Parent is null
         * for the outermost method.
         */
        private final SelectedMethod parent;
        
        /**
         * The target selector which selected this target
         */
        private final ITargetSelector selector;
        
        /**
         * The selected target method
         */
        private final MethodNode method;

        SelectedMethod(SelectedMethod parent, ITargetSelector selector, MethodNode method) {
            this.parent = parent;
            this.selector = selector;
            this.method = method;
        }
        
        SelectedMethod(ITargetSelector selector, MethodNode method) {
            this(null, selector, method);
        }
        
        @Override
        public String toString() {
            return this.method.name + this.method.desc;
        }
        
        public SelectedMethod getParent() {
            return this.parent;
        }
        
        public ITargetSelector next() {
            return this.selector.next();
        }

        public MethodNode getMethod() {
            return this.method;
        }

    }
    
    /**
     * The selector context for these selectors, for example the injector which
     * is running the selectors
     */
    private final ISelectorContext context;
    
    /**
     * The target class node within which targets can be resolved
     */
    private final ClassNode targetClassNode;
    
    /**
     * The mixin
     */
    private final IMixinContext mixin;
    
    /**
     * Annotated method, as MethodNode at runtime, or IAnnotatedElement during
     * compile
     */
    private final Object method;

    /**
     * Whether the annotated method is static
     */
    private final boolean isStatic;
    
    /**
     * Root selectors
     */
    private final Set<ITargetSelector> selectors = new LinkedHashSet<ITargetSelector>();
    
    /**
     * Selected targets
     */
    private final List<SelectedMethod> targets = new ArrayList<SelectedMethod>();
    
    private boolean doPermissivePass;

    public TargetSelectors(ISelectorContext context, ClassNode classNode) {
        this.context = context;
        this.targetClassNode = classNode; 
        this.mixin = context.getMixin();
        this.method = context.getMethod();       
        this.isStatic = this.method instanceof MethodNode && Bytecode.isStatic((MethodNode)this.method);
    }

    public void parse(Set<ITargetSelector> selectors) {
        // Validate and attach the parsed selectors
        for (ITargetSelector selector : selectors) {
            try {
                this.addSelector(selector.validate().attach(this.context));
            } catch (InvalidMemberDescriptorException ex) {
                throw new InvalidInjectionException(this.context, String.format("%s, has invalid target descriptor: %s. %s",
                        this.context.getElementDescription(), ex.getMessage(), this.mixin.getReferenceMapper().getStatus()));
            } catch (TargetNotSupportedException ex) {
                throw new InvalidInjectionException(this.context, String.format("%s specifies a target class '%s', which is not supported",
                        this.context.getElementDescription(), ex.getMessage()));
            } catch (InvalidSelectorException ex) {
                throw new InvalidInjectionException(this.context, String.format("%s is decorated with an invalid selector: %s",
                        this.context.getElementDescription(), ex.getMessage()));
            }
        }
    }

    public TargetSelectors addSelector(ITargetSelector selector) {
        this.selectors.add(selector);
        return this;
    }
    
    public int size() {
        return this.targets.size();
    }
    
    public void clear() {
        this.targets.clear();
    }

    @Override
    public Iterator<SelectedMethod> iterator() {
        return this.targets.iterator();
    }
    
    public void remove(SelectedMethod target) {
        this.targets.remove(target);
    }

    public boolean isPermissivePassEnabled() {
        return this.doPermissivePass;
    }

    public TargetSelectors setPermissivePass(boolean enabled) {
        this.doPermissivePass = enabled;
        return this;
    }

    /**
     * Find methods in the target class which match the parsed selectors
     */
    public void find() {
        this.findRootTargets();
        // this.findNestedTargets();
    }

    /**
     * Evaluate the root selectors parsed from this injector, find the root
     * targets and store them in the {@link #targets} collection.
     */
    private void findRootTargets() {
        int passes = this.doPermissivePass ? 2 : 1;
        
        for (ITargetSelector selector : this.selectors) {
            selector = selector.configure(Configure.SELECT_MEMBER);
            
            int matchCount = 0;
            int maxCount = selector.getMaxMatchCount();
            
            // Second pass ignores descriptor
            ITargetSelector permissiveSelector = selector.configure(Configure.PERMISSIVE);
            int selectorPasses = (permissiveSelector == selector) ? 1 : passes;

            scan: for (int pass = 0; pass < selectorPasses && matchCount < 1; pass++) {
                ITargetSelector passSelector = pass == 0 ? selector : permissiveSelector;
                for (MethodNode target : this.targetClassNode.methods) {
                    if (passSelector.match(ElementNode.of(this.targetClassNode, target)).isExactMatch()) {
                        matchCount++;

                        boolean isMixinMethod = Annotations.getVisible(target, MixinMerged.class) != null;
                        if (maxCount <= 1 || ((this.isStatic || !Bytecode.isStatic(target)) && target != this.method && !isMixinMethod)) {
                            this.checkTarget(target);
                            this.targets.add(new SelectedMethod(passSelector, target));
                        }

                        if (matchCount >= maxCount) {
                            break scan;
                        }
                    }
                }
            }
            
            if (matchCount < selector.getMinMatchCount()) {
                throw new InvalidInjectionException(this.context, new SelectorConstraintException(selector, String.format(
                        "Injection validation failed: %s for %s did not match the required number of targets (required=%d, matched=%d). %s%s",
                        selector, this.context.getElementDescription(), selector.getMinMatchCount(), matchCount,
                        this.mixin.getReferenceMapper().getStatus(), AnnotatedMethodInfo.getDynamicInfo(this.method))));
            }
        }
    }

    /**
     * For each root target, resolve the nested targets from the target
     * descriptor
     */
    protected void findNestedTargets() {
        boolean recursed = false;
        do {
            recursed = false;
            for (ListIterator<SelectedMethod> iter = this.targets.listIterator(); iter.hasNext();) {
                SelectedMethod target = iter.next();
                ITargetSelector next = target.next();
                if (next == null) {
                    continue;
                }
                
                recursed = true;
                Result<AbstractInsnNode> result = TargetSelector.run(next, ElementNode.dynamicInsnList(target.getMethod().instructions));
                iter.remove();
                for (ElementNode<AbstractInsnNode> candidate : result.candidates) {
                    if (candidate.getInsn().getOpcode() != Opcodes.INVOKEDYNAMIC) {
                        continue;
                    }
                    
                    if (!candidate.getOwner().equals(this.mixin.getTargetClassRef())) {
                        throw new InvalidInjectionException(this.context, String.format(
                                "%s, failed to select into child. Cannot select foreign method: %s. %s",
                                this.context.getElementDescription(), candidate, this.mixin.getReferenceMapper().getStatus()));
                    }
                    
                    MethodNode method = this.findMethod(candidate);
                    if (method == null) {
                        throw new InvalidInjectionException(this.context, String.format(
                                "%s, failed to select into child. %s%s was not found in the target class.",
                                this.context.getElementDescription(), candidate.getName(), candidate.getDesc()));
                    }

                    iter.add(new SelectedMethod(target, next, method));
                }
            }
        }
        while (recursed);
    }

    private void checkTarget(MethodNode target) {
        AnnotationNode merged = Annotations.getVisible(target, MixinMerged.class);
        if (merged == null) {
            return;
        }
        
        if (Annotations.getVisible(target, Final.class) != null) {
            throw new InvalidInjectionException(this.context, String.format("%s cannot inject into @Final method %s::%s%s merged by %s", this,
                    this.mixin.getTargetClassName(), target.name, target.desc, Annotations.<String>getValue(merged, "mixin")));
        }
    }

    /**
     * Finds a method in the target class
     * 
     * @return Target method matching searchFor, or null if not found
     */
    private MethodNode findMethod(ElementNode<AbstractInsnNode> searchFor) {
        for (MethodNode target : this.targetClassNode.methods) {
            if (target.name.equals(searchFor.getSyntheticName()) && target.desc.equals(searchFor.getDesc())) {
                return target;
            }
        }
        return null;
    }

    /**
     * Post-search validation that some targets were found, we can fail-fast if
     * no targets were actually identified or if the specified limits are
     * exceeded.
     * 
     * @param expectedCallbackCount Number of callbacks specified by expect
     * @param requiredCallbackCount Number of callbacks specified by require
     */
    public void validate(int expectedCallbackCount, int requiredCallbackCount) {
        int targetCount = this.targets.size(); 
        if (targetCount > 0) {
            return;
        }
        
        if ((this.mixin.getOption(Option.DEBUG_INJECTORS) && expectedCallbackCount > 0)) {
            throw new InvalidInjectionException(this.context,
                    String.format("Injection validation failed: %s could not find any targets matching %s in %s. %s%s", 
                            this.context.getElementDescription(), TargetSelectors.namesOf(this.selectors), this.mixin.getTargetClassRef(),
                            this.mixin.getReferenceMapper().getStatus(), AnnotatedMethodInfo.getDynamicInfo(this.method)));
        } else if (requiredCallbackCount > 0) {
            throw new InvalidInjectionException(this.context,
                    String.format("Critical injection failure: %s could not find any targets matching %s in %s. %s%s", 
                            this.context.getElementDescription(), TargetSelectors.namesOf(this.selectors), this.mixin.getTargetClassRef(),
                            this.mixin.getReferenceMapper().getStatus(), AnnotatedMethodInfo.getDynamicInfo(this.method)));
        }
    }

    /**
     * Print the names of the specified members as a human-readable list 
     * 
     * @param selectors members to print
     * @return human-readable list of member names
     */
    private static String namesOf(Collection<ITargetSelector> selectors) {
        int index = 0, count = selectors.size();
        StringBuilder sb = new StringBuilder();
        for (ITargetSelector selector : selectors) {
            if (index > 0) {
                if (index == (count - 1)) {
                    sb.append(" or ");
                } else {
                    sb.append(", ");
                }
            }
            sb.append('\'').append(selector.toString()).append('\'');
            index++;
        }
        return sb.toString();
    }

}
