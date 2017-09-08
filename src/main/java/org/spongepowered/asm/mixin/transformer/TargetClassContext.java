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

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.FieldNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.struct.SourceMap;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.ClassSignature;

/**
 * Struct for containing target class information during mixin application
 */
class TargetClassContext extends ClassContext implements ITargetClassContext {

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
     * Unique method and field indices 
     */
    private int nextUniqueMethodIndex, nextUniqueFieldIndex;

    /**
     * True once mixins have been applied to this class 
     */
    private boolean applied;
    
    /**
     * True if this class is decorated with an {@link Debug} annotation which
     * instructs an export 
     */
    private boolean forceExport;

    TargetClassContext(MixinEnvironment env, Extensions extensions, String sessionId, String name, ClassNode classNode, SortedSet<MixinInfo> mixins) {
        this.env = env;
        this.extensions = extensions;
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
    
    String getUniqueName(MethodNode method, boolean preservePrefix) {
        String uniqueIndex = Integer.toHexString(this.nextUniqueMethodIndex++);
        String pattern = preservePrefix ? "%2$s_$md$%1$s$%3$s" : "md%s$%s$%s";
        return String.format(pattern, this.sessionId.substring(30), method.name, uniqueIndex);
    }

    String getUniqueName(FieldNode field) {
        String uniqueIndex = Integer.toHexString(this.nextUniqueFieldIndex++);
        return String.format("fd%s$%s$%s", this.sessionId.substring(30), field.name, uniqueIndex);
    }
    
    /**
     * Apply mixins to this class
     */
    void applyMixins() {
        if (this.applied) {
            throw new IllegalStateException("Mixins already applied to target class " + this.className);
        }
        this.applied = true;
        
        MixinApplicatorStandard applicator = this.createApplicator();
        applicator.apply(this.mixins);
        this.applySignature();
        this.upgradeMethods();
        this.checkMerges();
    }

    private MixinApplicatorStandard createApplicator() {
        if (this.classInfo.isInterface()) {
            return new MixinApplicatorInterface(this);
        }
        return new MixinApplicatorStandard(this);
    }

    private void applySignature() {
        this.getClassNode().signature = this.signature.toString();
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
        if (!this.env.getOption(Option.DEBUG_VERBOSE)) {
            return;
        }

        AnnotationNode classDebugAnnotation = Annotations.getVisible(this.classNode, Debug.class);
        if (classDebugAnnotation != null) {
            this.forceExport = Boolean.TRUE.equals(Annotations.<Boolean>getValue(classDebugAnnotation, "export"));
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
    
}
