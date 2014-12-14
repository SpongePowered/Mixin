/**
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.util.ASMHelper;


/**
 * Data for applying a mixin. Keeps a copy of the mixin tree and also handles any pre-transformations required by the mixin class itself such as
 * method renaming and any other pre-processing of the mixin bytecode.
 */
public class MixinData {
    
    /**
     * Mixin info
     */
    private final MixinInfo info;
    
    /**
     * Tree
     */
    private final ClassNode classNode;
    
    /**
     * Methods we need to rename
     */
    private final Map<String, String> renamedMethods = new HashMap<String, String>();
    
    /**
     * Interfaces soft-implemented using {@link Implements} 
     */
    private final List<InterfaceInfo> softImplements = new ArrayList<InterfaceInfo>();
    
    /**
     * All interfaces implemented by this mixin, including soft implementations
     */
    private final Set<String> interfaces = new HashSet<String>();

    /**
     * ctor
     * 
     * @param info
     */
    MixinData(MixinInfo info) {
        this.info = info;
        this.classNode = info.getClassNode(ClassReader.EXPAND_FRAMES);
        this.prepare();
    }

    /**
     * Get the mixin tree
     */
    public ClassNode getClassNode() {
        return this.classNode;
    }

    /**
     * Get all interfaces for this mixin
     */
    public Set<String> getInterfaces() {
        return this.interfaces;
    }

    /**
     * Get whether to propogate the source file attribute from a mixin onto the target class
     */
    public boolean shouldSetSourceFile() {
        return this.info.getParent().shouldSetSourceFile();
    }

    /**
     * Prepare the mixin, applies any pre-processing transformations
     */
    private void prepare() {
        this.readImplementations();
        this.findRenamedMethods();
        this.transformMethods();
    }

    /**
     * Read and process any {@link Implements} annotations on the mixin
     */
    private void readImplementations() {
        this.interfaces.addAll(this.classNode.interfaces);
        
        AnnotationNode implementsAnnotation = ASMHelper.getInvisibleAnnotation(this.classNode, Implements.class);
        if (implementsAnnotation == null) {
            return;
        }
        
        List<AnnotationNode> interfaces = ASMHelper.getAnnotationValue(implementsAnnotation);
        if (interfaces == null) {
            return;
        }
        
        for (AnnotationNode interfaceNode : interfaces) {
            InterfaceInfo interfaceInfo = InterfaceInfo.fromAnnotation(interfaceNode);
            this.softImplements.add(interfaceInfo);
            this.interfaces.add(interfaceInfo.getInternalName());
        }
    }

    /**
     * Let's do this
     */
    private void findRenamedMethods() {
        for (MethodNode mixinMethod : this.classNode.methods) {
            String oldSignature = mixinMethod.name + mixinMethod.desc;
            AnnotationNode shadowAnnotation = ASMHelper.getVisibleAnnotation(mixinMethod, Shadow.class);
            if (shadowAnnotation != null) {
                String prefix = MixinData.getAnnotationValue(shadowAnnotation, "prefix", Shadow.class);
                if (mixinMethod.name.startsWith(prefix)) {
                    String newName = mixinMethod.name.substring(prefix.length());
                    this.renamedMethods.put(oldSignature, newName);
                    mixinMethod.name = newName;
                }
            }
            
            for (InterfaceInfo iface : this.softImplements) {
                if (iface.renameMethod(mixinMethod)) {
                    this.renamedMethods.put(oldSignature, mixinMethod.name);
                }
            }
        }
    }

    /**
     * Apply discovered method renames to method invokations in the mixin
     */
    private void transformMethods() {
        for (MethodNode mixinMethod : this.classNode.methods) {
            for (Iterator<AbstractInsnNode> iter = mixinMethod.instructions.iterator(); iter.hasNext();) {
                AbstractInsnNode insn = iter.next();
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode methodNode = (MethodInsnNode)insn;
                    String newName = this.renamedMethods.get(methodNode.name + methodNode.desc);
                    if (newName != null) {
                        methodNode.name = newName;
                    }
                }
            }
        }
    }

    /**
     * Gets an annotation value or returns the default if the annotation value is not present
     * 
     * @param annotation
     * @param key
     * @param annotationClass
     * @return
     */
    private static String getAnnotationValue(AnnotationNode annotation, String key, Class<?> annotationClass) {
        String value = ASMHelper.getAnnotationValue(annotation, key);
        if (value == null) {
            try {
                value = (String)Shadow.class.getDeclaredMethod(key).getDefaultValue();
            } catch (NoSuchMethodException ex) {
                // Don't care
            }
        }
        return value;
    }
}
