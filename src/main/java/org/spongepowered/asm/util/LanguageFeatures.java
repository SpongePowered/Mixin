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
package org.spongepowered.asm.util;

import java.lang.reflect.Field;
import java.util.List;
import java.util.ListIterator;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.util.Bytecode.Visibility;
import org.spongepowered.asm.util.asm.ASM;
import org.spongepowered.asm.util.asm.ClassNodeAdapter;

/**
 * Bitmask values for language features supported. Contains utility methods for
 * detecting language features in use in supplied class nodes.
 */
public final class LanguageFeatures {
    
    /**
     * Language version supports methods in interfaces
     */
    public static final int METHODS_IN_INTERFACES = 1;
    
    /**
     * Language version supports synthetic private methods in interfaces
     */
    public static final int PRIVATE_SYNTHETIC_METHODS_IN_INTERFACES = 2;
    
    /**
     * Language version supports user-defined private methods in interfaces
     */
    public static final int PRIVATE_METHODS_IN_INTERFACES = 4;
    
    /**
     * Native nesting
     */
    public static final int NESTING = 8;
    
    /**
     * Dynamic constants
     */
    public static final int DYNAMIC_CONSTANTS = 16;
    
    /**
     * Utility class
     */
    private LanguageFeatures() {
    }

    /**
     * Scan the supplied class node to determine required language features via
     * heuristic.
     * 
     * @param classNode ClassNode to scan (must include method bodies for
     *      reliable detection)
     * @return detected language features
     */
    public static int scan(ClassNode classNode) {
        int features = LanguageFeatures.scanClassFeatures(classNode);
        
        boolean isInterface = Bytecode.hasFlag(classNode, Opcodes.ACC_INTERFACE);
        for (MethodNode methodNode : classNode.methods) {
            if (isInterface) {
                features |= LanguageFeatures.scanInterfaceFeatures(methodNode);
            } else {
                features |= LanguageFeatures.scanMethodFeatures(methodNode);
            }
        }
        
        return features;
    }

    /**
     * Sacn for features at the class level
     */
    private static int scanClassFeatures(ClassNode classNode) {
        int features = 0;
        
        String nestHostClass = ClassNodeAdapter.getNestHostClass(classNode);
        List<String> nestMembers = ClassNodeAdapter.getNestMembers(classNode);
        if (nestHostClass != null || (nestMembers != null && nestMembers.size() > 0)) {
            features |= LanguageFeatures.NESTING;
        }
        
        return features;
    }

    /**
     * Scan the method for interface-specific features
     */
    private static int scanInterfaceFeatures(MethodNode methodNode) {
        int features = 0;
        
        if (!Bytecode.hasFlag(methodNode, Opcodes.ACC_ABSTRACT)) {
            features |= LanguageFeatures.METHODS_IN_INTERFACES;
        } 
        
        if (Bytecode.getVisibility(methodNode).isLessThan(Visibility.PUBLIC)) {
            features |= Bytecode.hasFlag(methodNode, Opcodes.ACC_SYNTHETIC)
                    ? LanguageFeatures.PRIVATE_SYNTHETIC_METHODS_IN_INTERFACES
                    : LanguageFeatures.PRIVATE_METHODS_IN_INTERFACES; 
        }
        
        return features;
    }

    /**
     * Scan the method code for feature requirements
     */
    private static int scanMethodFeatures(MethodNode methodNode) {
        // ConstantDynamic only exists in ASM 6 and later
        if (ASM.isAtLeastVersion(6)) {
            for (ListIterator<AbstractInsnNode> iter = methodNode.instructions.iterator(); iter.hasNext();) {
                AbstractInsnNode insn = iter.next();
                if (insn instanceof LdcInsnNode && ((LdcInsnNode)insn).cst instanceof ConstantDynamic) {
                    return LanguageFeatures.DYNAMIC_CONSTANTS;
                }
            }
        }
        return 0;
    }
    
    /**
     * Format the supplied feature mask as a plain-text list for use in error 
     * messages etc.
     * 
     * @param features Language features to format
     * @return Formatted list of features
     */
    public static final String format(int features) {
        StringBuilder sb = new StringBuilder("[");
        try {
            int count = 0;
            for (Field field : LanguageFeatures.class.getDeclaredFields()) {
                if ((features & field.getInt(null)) != 0) {
                    if (count++ > 0) {
                        sb.append(',');
                    }
                    sb.append(field.getName());
                }
            }
        } catch (ReflectiveOperationException ex) {
            sb.append("ERROR");
        }
        return sb.append(']').toString();
    }
    
}
