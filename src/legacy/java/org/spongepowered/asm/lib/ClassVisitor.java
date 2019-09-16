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

package org.spongepowered.asm.lib;

import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.throwables.CompanionPluginError;

// CHECKSTYLE:OFF

/**
 * Shim, see {@link ClassNode}
 */
public abstract class ClassVisitor {

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        throw new CompanionPluginError("ClassVisitor.visit");
    }

    public void visitSource(String source, String debug) {
        throw new CompanionPluginError("ClassVisitor.visitSource");
    }

    public void visitOuterClass(String owner, String name, String desc) {
        throw new CompanionPluginError("ClassVisitor.visitOuterClass");
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        throw new CompanionPluginError("ClassVisitor.visitAnnotation");
    }

    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        throw new CompanionPluginError("ClassVisitor.visitTypeAnnotation");
    }

    public void visitAttribute(Attribute attr) {
        throw new CompanionPluginError("ClassVisitor.visitAttribute");
    }

    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        throw new CompanionPluginError("ClassVisitor.visitInnerClass");
    }

    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        throw new CompanionPluginError("ClassVisitor.visitField");
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        throw new CompanionPluginError("ClassVisitor.visitMethod");
    }

    public void visitEnd() {
        throw new CompanionPluginError("ClassVisitor.visitEnd");
    }
}
