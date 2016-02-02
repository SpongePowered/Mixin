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

import java.util.SortedSet;

import org.spongepowered.asm.lib.tree.ClassNode;

/**
 * Struct for containing target class information during mixin application
 */
class TargetClassContext {

    /**
     * Transformer session ID
     */
    private final String sessionId;
    
    /**
     * Target class name 
     */
    private final String name;
    
    /**
     * Target class as tree 
     */
    private final ClassNode classNode;
    
    /**
     * Target class metadata 
     */
    private final ClassInfo classInfo;
    
    /**
     * Mixins to apply 
     */
    private final SortedSet<MixinInfo> mixins;
    
    /**
     * True once mixins have been applied to this class 
     */
    private boolean applied;

    TargetClassContext(String sessionId, String name, ClassNode classNode, SortedSet<MixinInfo> mixins) {
        this.sessionId = sessionId;
        this.name = name;
        this.classNode = classNode;
        this.classInfo = ClassInfo.fromClassNode(classNode);
        this.mixins = mixins;
    }
    
    /**
     * Get the transformer session ID
     */
    public String getSessionId() {
        return this.sessionId;
    }
    
    /**
     * Get the class name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the class tree
     */
    public ClassNode getClassNode() {
        return this.classNode;
    }
    
    /**
     * Get the target class metadata
     */
    public ClassInfo getClassInfo() {
        return this.classInfo;
    }
    
    /**
     * Get the mixins for this target class
     */
    public SortedSet<MixinInfo> getMixins() {
        return this.mixins;
    }

    /**
     * Apply mixins to this class
     */
    public void applyMixins() {
        if (this.applied) {
            throw new IllegalStateException("Mixins already applied to target class " + this.name);
        }
        this.applied = true;
        MixinApplicator applicator = this.createApplicator();
        applicator.apply(this.mixins);
    }

    private MixinApplicator createApplicator() {
        if (this.classInfo.isInterface()) {
            return new InterfaceMixinApplicator(this);
        }
        return new MixinApplicator(this);
    }
    
}
