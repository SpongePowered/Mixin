/*
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

import java.util.List;

import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.analysis.SimpleVerifier;


/**
 * Verifier which handles class info lookups via {@link ClassInfo}
 */
public class MixinVerifier extends SimpleVerifier {

    private Type currentClass;
    private Type currentSuperClass;
    private List<Type> currentClassInterfaces;
    private boolean isInterface;

    public MixinVerifier(Type currentClass, Type currentSuperClass, List<Type> currentClassInterfaces, boolean isInterface) {
        super(currentClass, currentSuperClass, currentClassInterfaces, isInterface);
        this.currentClass = currentClass;
        this.currentSuperClass = currentSuperClass;
        this.currentClassInterfaces = currentClassInterfaces;
        this.isInterface = isInterface;
    }

    @Override
    protected boolean isInterface(final Type t) {
        if (this.currentClass != null && t.equals(this.currentClass)) {
            return this.isInterface;
        }
        return this.getClassInfo(t).isInterface();
    }

    @Override
    protected Type getSuperClass(final Type t) {
        if (this.currentClass != null && t.equals(this.currentClass)) {
            return this.currentSuperClass;
        }
        ClassInfo c = this.getClassInfo(t).getSuperClass();
        return c == null ? null : Type.getType("L" + c.getName() + ";");
    }

    @Override
    protected boolean isAssignableFrom(final Type t, final Type u) {
        if (t.equals(u)) {
            return true;
        }
        if (this.currentClass != null && t.equals(this.currentClass)) {
            if (this.getSuperClass(u) == null) {
                return false;
            }
            if (this.isInterface) {
                return u.getSort() == Type.OBJECT || u.getSort() == Type.ARRAY;
            }
            return this.isAssignableFrom(t, this.getSuperClass(u));
        }
        if (this.currentClass != null && u.equals(this.currentClass)) {
            if (this.isAssignableFrom(t, this.currentSuperClass)) {
                return true;
            }
            if (this.currentClassInterfaces != null) {
                for (int i = 0; i < this.currentClassInterfaces.size(); ++i) {
                    Type v = this.currentClassInterfaces.get(i);
                    if (this.isAssignableFrom(t, v)) {
                        return true;
                    }
                }
            }
            return false;
        }
        ClassInfo tc = this.getClassInfo(t);
        if (tc.isInterface()) {
            tc = ClassInfo.forName("java/lang/Object");
        }
        return this.getClassInfo(u).hasSuperClass(tc);
    }

    protected ClassInfo getClassInfo(final Type t) {
        if (t.getSort() == Type.ARRAY) {
            return ClassInfo.forName(t.getDescriptor().replace('/', '.'));
        }
        return ClassInfo.forName(t.getClassName().replace('.', '/'));
    }
}
