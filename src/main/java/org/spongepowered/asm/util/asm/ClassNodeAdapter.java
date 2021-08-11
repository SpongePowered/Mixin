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
package org.spongepowered.asm.util.asm;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.tree.ClassNode;

/**
 * Adapter for ClassNode to access members added after ASM 5 
 */
public final class ClassNodeAdapter {
    
    private static final String NEST_HOST_FIELD = "nestHostClass";
    private static final String NEST_MEMBERS_FIELD = "nestMembers";
    private static final String EXPERIMENTAL_SUFFIX = "Experimental";
    
    private static final Field fdNestHost = ClassNodeAdapter.getField(ClassNodeAdapter.NEST_HOST_FIELD);
    private static final Field fdNestMembers = ClassNodeAdapter.getField(ClassNodeAdapter.NEST_MEMBERS_FIELD);
    
    private static boolean notSupported = false;
    
    private ClassNodeAdapter() {
    }
    
    public static String getNestHostClass(ClassNode classNode) {
        if (ASM.isAtLeastVersion(7)) {
            return classNode.nestHostClass;
        }
        
        if (ClassNodeAdapter.fdNestHost == null || ClassNodeAdapter.notSupported) {
            return null;
        }
        
        try {
            return (String)ClassNodeAdapter.fdNestHost.get(classNode);
        } catch (ReflectiveOperationException ex) {
            ClassNodeAdapter.notSupported = true;
            return null;
        }
    }
    
    public static void setNestHostClass(ClassNode classNode, String nestHostClass) {
        if (ASM.isAtLeastVersion(7)) {
            classNode.nestHostClass = nestHostClass;
        }
        
        if (ClassNodeAdapter.fdNestHost == null || ClassNodeAdapter.notSupported) {
            return;
        }
        
        try {
            ClassNodeAdapter.fdNestHost.set(classNode, nestHostClass);
        } catch (ReflectiveOperationException ex) {
            ClassNodeAdapter.notSupported = true;
        }
    }
    
    @SuppressWarnings("unchecked")
    public static List<String> getNestMembers(ClassNode classNode) {
        if (ASM.isAtLeastVersion(7)) {
            return classNode.nestMembers;
        }
        
        if (ClassNodeAdapter.fdNestMembers == null || ClassNodeAdapter.notSupported) {
            return null;
        }
        
        try {
            return (List<String>)ClassNodeAdapter.fdNestMembers.get(classNode);
        } catch (ReflectiveOperationException ex) {
            ClassNodeAdapter.notSupported = true;
            return null;
        }
    }
    
    public static List<String> getNestMembersAsList(ClassNode classNode) {
        List<String> nestMembers = ClassNodeAdapter.getNestMembers(classNode);
        if (nestMembers == null) {
            nestMembers = new ArrayList<String>();
            ClassNodeAdapter.setNestMembers(classNode, nestMembers);
        }
        return nestMembers;
    }
    
    public static void setNestMembers(ClassNode classNode, List<String> nestMembers) {
        if (ASM.isAtLeastVersion(7)) {
            classNode.nestMembers = nestMembers;
            return;
        }
        
        if (ClassNodeAdapter.fdNestMembers == null || ClassNodeAdapter.notSupported) {
            return;
        }
        
        try {
            ClassNodeAdapter.fdNestMembers.set(classNode, nestMembers);
        } catch (ReflectiveOperationException ex) {
            ClassNodeAdapter.notSupported = true;
        }
    }

    private static Field getField(String fieldBaseName) {
        try {
            return ClassNode.class.getDeclaredField(fieldBaseName);
        } catch (NoSuchFieldException ex) {
            try {
                return ClassNode.class.getDeclaredField(fieldBaseName + ClassNodeAdapter.EXPERIMENTAL_SUFFIX);
            } catch (NoSuchFieldException ex1) {
                ClassNodeAdapter.notSupported = true;
            }
        }
        return null;
    }

}
