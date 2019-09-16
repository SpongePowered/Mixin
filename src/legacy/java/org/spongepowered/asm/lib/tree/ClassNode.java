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

package org.spongepowered.asm.lib.tree;

import java.util.Collections;
import java.util.List;

import org.spongepowered.asm.lib.ClassVisitor;
import org.spongepowered.asm.mixin.throwables.CompanionPluginError;

//CHECKSTYLE:OFF

/**
 * Compatibility shim for <tt>IMixinConfigPlugin</tt>. Some of the basic fields
 * are stubbed out so that (for example) simple uses of <tt>classNode.name</tt>
 * will still work but any attempt to <em>write</em> to a field will fail and
 * any access to fields or methods will raise an error which can be caught by
 * the PluginHandle and marshalled to a useful(ish) error message.
 * 
 * <p>This is mainly provided so that mixin companions compiled for 0.7.x will
 * still work with 0.8 but will only crash if they access members of the
 * ClassNode provided in <tt>preApply</tt> and <tt>postApply</tt> (which they
 * might not). This provides a small degree of backward compatibility, or at
 * least as much as is possible without running everything through a transformer
 * to change the renamed ASM references to native ASM classes.</p> 
 */
public class ClassNode extends ClassVisitor {
    
    public final int version;
    public final int access;

    public final String name;
    public final String signature;

    public final String superName;

    public final List<String> interfaces;

    public final String sourceFile;
    public final String sourceDebug;

    public final String outerClass;
    public final String outerMethod;
    public final String outerMethodDesc;

    public ClassNode(org.objectweb.asm.tree.ClassNode classNode) {
        this.version = classNode.version;
        this.access = classNode.access;

        this.name = classNode.name;
        this.signature = classNode.signature;

        this.superName = classNode.superName;
        this.interfaces = Collections.<String>unmodifiableList(classNode.interfaces);

        this.sourceFile = classNode.sourceFile;
        this.sourceDebug = classNode.sourceDebug;

        this.outerClass = classNode.outerClass;
        this.outerMethod = classNode.outerMethod;
        this.outerMethodDesc = classNode.outerMethodDesc;
    }

    public void check(final int api) {
        throw new CompanionPluginError("ClassNode.check");
    }

    public void accept(final ClassVisitor cv) {
        throw new CompanionPluginError("ClassNode.accept");
    }
    
}
