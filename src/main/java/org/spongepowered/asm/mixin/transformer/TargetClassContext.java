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
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.struct.SourceMap;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;
import org.spongepowered.asm.mixin.transformer.ext.extensions.ExtensionCheckClass.ValidationFailedException;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.ClassSignature;
import org.spongepowered.asm.util.perf.Profiler;
import org.spongepowered.asm.util.perf.Profiler.Section;

/**
 * Struct for containing target class information during mixin application
 */
final class TargetClassContext extends ClassContext implements ITargetClassContext {

    /**
     * Logger
     */
    private static final Logger logger = LogManager.getLogger("mixin");

    /**
     * Mixin environment
     */
    private final MixinEnvironment env;

    /**
     * Mixin transformer extensions
     */
    private final Extensions extensions;
    
    /**
     * Profiler 
     */
    private final Profiler profiler;

    /**
     * Transformer session ID
     */
    private final String sessionId;
    
    /**
     * Target class name 
     */
    private final String className;
    
    /**
     * Target class as tree 
     */
    private final ClassNode classNode;
    
    /**
     * Target class metadata 
     */
    private final ClassInfo classInfo;
    
    /**
     * Source map that is generated for target class
     */
    private final SourceMap sourceMap;
    
    /**
     * Target class signature 
     */
    private final ClassSignature signature;
    
    /**
     * Mixins to apply 
     */
    private final SortedSet<MixinInfo> mixins;

    /**
     * Information about methods in the target class, used to keep track of
     * transformations we apply
     */
    private final Map<String, Target> targetMethods = new HashMap<String, Target>();

    /**
     * Information about methods which have been discovered by mixin
     * preprocessors in this pass but which have not yet been merged into the
     * target class. 
     */
    private final Set<MethodNode> mixinMethods = new HashSet<MethodNode>();
    
    /**
     * Exceptions which were suppressed during mixin application because they
     * were raised by an optional mixin 
     */
    private final List<InvalidMixinException> suppressedExceptions = new ArrayList<InvalidMixinException>();

    /**
     * True once mixins have been applied to this class 
     */
    private boolean applied;
    
    /**
     * True if this class is eligible for export. That is, it was successfully
     * applied or if validation failed and export is enabled.  
     */
    private boolean export;
    
    /**
     * True if this class is decorated with an {@link Debug} annotation which
     * instructs an export 
     */
    private boolean forceExport;
    
    TargetClassContext(MixinEnvironment env, Extensions extensions, String sessionId, String name, ClassNode classNode, SortedSet<MixinInfo> mixins) {
        this.env = env;
        this.extensions = extensions;
        this.profiler = MixinEnvironment.getProfiler();
        this.sessionId = sessionId;
        this.className = name;
        this.classNode = classNode;
        this.classInfo = ClassInfo.fromClassNode(classNode);
        this.signature = this.classInfo.getSignature();
        this.mixins = mixins;
        this.sourceMap = new SourceMap(classNode.sourceFile);
        this.sourceMap.addFile(this.classNode);
    }
    
    @Override
    public String toString() {
        return this.className;
    }
    
    boolean isApplied() {
        return this.applied;
    }
    
    boolean isExported() {
        return this.export;
    }
    
    boolean isExportForced() {
        return this.forceExport;
    }
    
    /**
     * Get the transformer extensions
     */
    Extensions getExtensions() {
        return this.extensions;
    }
    
    /**
     * Get the transformer session ID
     */
    String getSessionId() {
        return this.sessionId;
    }
    
    /**
     * Get the internal class name
     */
    @Override
    String getClassRef() {
        return this.classNode.name;
    }
    
    /**
     * Get the class name
     */
    String getClassName() {
        return this.className;
    }

    /**
     * Get the class tree
     */
    @Override
    public ClassNode getClassNode() {
        return this.classNode;
    }

    /**
     * Get the class methods (from the tree)
     */
    List<MethodNode> getMethods() {
        return this.classNode.methods;
    }
    
    /**
     * Get the class fields (from the tree)
     */
    List<FieldNode> getFields() {
        return this.classNode.fields;
    }

    /**
     * Get the target class metadata
     */
    @Override
    public ClassInfo getClassInfo() {
        return this.classInfo;
    }
    
    /**
     * Get the mixins for this target class
     */
    SortedSet<MixinInfo> getMixins() {
        return this.mixins;
    }

    /**
     * Get the source map that is generated for the target class
     */
    SourceMap getSourceMap() {
        return this.sourceMap;
    }
    
    /**
     * Merge the supplied signature into this class's signature
     * 
     * @param signature signature to merge
     */
    void mergeSignature(ClassSignature signature) {
        this.signature.merge(signature);
    }
    
    /**
     * Add the specified method to the pending mixin methods set
     * 
     * @param method method to add
     */
    void addMixinMethod(MethodNode method) {
        this.mixinMethods.add(method);
    }

    void methodMerged(MethodNode method) {
        if (!this.mixinMethods.remove(method)) {
            TargetClassContext.logger.debug("Unexpected: Merged unregistered method {}{} in {}", method.name, method.desc, this);
        }
    }
    
    MethodNode findMethod(Deque<String> aliases, String desc) {
        return this.findAliasedMethod(aliases, desc, true);
    }
    
    MethodNode findAliasedMethod(Deque<String> aliases, String desc) {
        return this.findAliasedMethod(aliases, desc, false);
    }

    private MethodNode findAliasedMethod(Deque<String> aliases, String desc, boolean includeMixinMethods) {
        String alias = aliases.poll();
        if (alias == null) {
            return null;
        }
        
        for (MethodNode target : this.classNode.methods) {
            if (target.name.equals(alias) && target.desc.equals(desc)) {
                return target;
            }
        }

        if (includeMixinMethods) {
            for (MethodNode target : this.mixinMethods) {
                if (target.name.equals(alias) && target.desc.equals(desc)) {
                    return target;
                }
            } 
        }
        
        return this.findAliasedMethod(aliases, desc);
    }
    
    /**
     * Finds a field in the target class
     * 
     * @param aliases aliases for the field
     * @param desc field descriptor
     * @return Target field  or null if not found
     */
    FieldNode findAliasedField(Deque<String> aliases, String desc) {
        String alias = aliases.poll();
        if (alias == null) {
            return null;
        }
        
        for (FieldNode target : this.classNode.fields) {
            if (target.name.equals(alias) && target.desc.equals(desc)) {
                return target;
            }
        }

        return this.findAliasedField(aliases, desc);
    }

    /**
     * Get a target method handle from the target class
     * 
     * @param method method to get a target handle for
     * @return new or existing target handle for the supplied method
     */
    Target getTargetMethod(MethodNode method) {
        if (!this.classNode.methods.contains(method)) {
            throw new IllegalArgumentException("Invalid target method supplied to getTargetMethod()");
        }
        
        String targetName = method.name + method.desc;
        Target target = this.targetMethods.get(targetName);
        if (target == null) {
            target = new Target(this.classNode, method);
            this.targetMethods.put(targetName, target);
        }
        return target;
    }
    
    /**
     * Apply mixins for this target class
     */
    void applyMixins() {
        if (this.applied) {
            throw new IllegalStateException("Mixins already applied to target class " + this.className);
        }
        this.applied = true;
        Section timer = this.profiler.begin("preapply");
        this.preApply();
        timer = timer.next("apply");
        this.apply();
        timer = timer.next("postapply");
        this.postApply();
        timer.end();
    }
    
    /**
     * Run extensions before apply
     */
    private void preApply() {
        this.extensions.preApply(this);
    }

    /**
     * Apply mixins to this class
     */
    private void apply() {
        MixinApplicatorStandard applicator = this.createApplicator();
        applicator.apply(this.mixins);
        this.applySignature();
        this.upgradeMethods();
        this.checkMerges();
    }
    
    /**
     * Run extensions after apply
     */
    private void postApply() {
        try {
            this.extensions.postApply(this);
            this.export = true;
        } catch (ValidationFailedException ex) {
            MixinProcessor.logger.info(ex.getMessage());
            // If verify is enabled and failed, write out the bytecode to allow us to inspect it
            this.export |= this.forceExport || this.env.getOption(Option.DEBUG_EXPORT);
        }
    }

    private MixinApplicatorStandard createApplicator() {
        if (this.classInfo.isInterface()) {
            return new MixinApplicatorInterface(this);
        }
        return new MixinApplicatorStandard(this);
    }

    private void applySignature() {
        this.classNode.signature = this.signature.toString();
    }

    private void checkMerges() {
        for (MethodNode method : this.mixinMethods) {
            if (!method.name.startsWith("<")) {
                TargetClassContext.logger.debug("Unexpected: Registered method {}{} in {} was not merged", method.name, method.desc, this);
            }
        }
    }

    /**
     * Process {@link Debug} annotations on the class after application
     */
    void processDebugTasks() {
        AnnotationNode classDebugAnnotation = Annotations.getVisible(this.classNode, Debug.class);
        this.forceExport = classDebugAnnotation != null && Boolean.TRUE.equals(Annotations.<Boolean>getValue(classDebugAnnotation, "export"));
        
        if (!this.env.getOption(Option.DEBUG_VERBOSE)) {
            return;
        }

        if (classDebugAnnotation != null) {
            if (Boolean.TRUE.equals(Annotations.<Boolean>getValue(classDebugAnnotation, "print"))) {
                Bytecode.textify(this.classNode, System.err);
            }
        }
        
        for (MethodNode method : this.classNode.methods) {
            AnnotationNode methodDebugAnnotation = Annotations.getVisible(method, Debug.class);
            if (methodDebugAnnotation != null && Boolean.TRUE.equals(Annotations.<Boolean>getValue(methodDebugAnnotation, "print"))) {
                Bytecode.textify(method, System.err);
            }
        }
    }
    
    void addSuppressed(InvalidMixinException ex) {
        this.suppressedExceptions.add(ex);
    }
    
    List<InvalidMixinException> getSuppressedExceptions() {
        return this.suppressedExceptions;
    }
    
}
