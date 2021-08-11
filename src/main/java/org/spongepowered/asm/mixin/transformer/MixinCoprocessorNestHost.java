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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.throwables.MixinTransformerError;
import org.spongepowered.asm.util.LanguageFeatures;
import org.spongepowered.asm.util.asm.ClassNodeAdapter;

/**
 * In Java 11 and above, access control semantics for inner members are reified
 * in the concept of nesting, where an "outermost" class becomes a "nest host"
 * for all nested classes. Since a mixin can be applied to an inner class, the
 * process of adding any inner classes of mixins to the nest is not as simple as
 * having the target class become the nest host. Thus the nest host of conformed
 * inner classes is resolved during the <em>prepare</em> phase so that any
 * classes which need to have their nests expanded can be processed accordingly.
 * 
 * <p>This coprocessor therefore processes all classes regardless of whether
 * they are mixin targets or not, in order to add any additional nest members
 * where a mixin targets the class, or one of its existing nest members.</p>
 */
class MixinCoprocessorNestHost extends MixinCoprocessor {
    
    /**
     * Classes which are nest hosts with new members injected by mixins 
     */
    private final Map<String, Set<String>> nestHosts = new HashMap<String, Set<String>>();

    MixinCoprocessorNestHost() {
    }
    
    void registerNestMember(String hostName, String memberName) {
        Set<String> nestMembers = this.nestHosts.get(hostName);
        if (nestMembers == null) {
            this.nestHosts.put(hostName, nestMembers = new HashSet<String>());
        }
        nestMembers.add(memberName);
    }
    
    @Override
    String getName() {
        return "nesthost";
    }

    @Override
    boolean postProcess(String className, ClassNode classNode) {
        if (!this.nestHosts.containsKey(className)) {
            return false;
        }
        
        Set<String> newMembers = this.nestHosts.get(className);
        if (!MixinEnvironment.getCompatibilityLevel().supports(LanguageFeatures.NESTING) || newMembers.isEmpty()) {
            return false;
        }
        
        String nestHost = ClassNodeAdapter.getNestHostClass(classNode);
        if (nestHost != null) {
            throw new MixinTransformerError(String.format("Nest host candidate %s is a nest member", classNode.name));
        }
        
        List<String> nestMembers = ClassNodeAdapter.getNestMembers(classNode);
        if (nestMembers == null) {
            // If there are currently no nest members, just set the ones we have
            nestMembers = new ArrayList<String>(newMembers);
        } else {
            // Otherwise add all the new members
            LinkedHashSet<String> combinedMembers = new LinkedHashSet<String>(nestMembers);
            combinedMembers.addAll(newMembers);
            nestMembers.clear();
            nestMembers.addAll(combinedMembers);
        }
        ClassNodeAdapter.setNestMembers(classNode, nestMembers);
        return true;
    }

}
