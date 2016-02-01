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
package org.spongepowered.asm.mixin.injection.struct;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.modify.ModifyVariableInjector;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.ASMHelper;

public class ModifyVariableInjectionInfo extends InjectionInfo {

    public ModifyVariableInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation);
    }
    
    @Override
    protected Injector initInjector(AnnotationNode injectAnnotation) {
        boolean print = ASMHelper.<Boolean>getAnnotationValue(injectAnnotation, "print", Boolean.FALSE).booleanValue();
        boolean argsOnly = ASMHelper.<Boolean>getAnnotationValue(injectAnnotation, "argsOnly", Boolean.FALSE).booleanValue();
        int ordinal = ASMHelper.<Integer>getAnnotationValue(injectAnnotation, "ordinal", -1);
        int index = ASMHelper.<Integer>getAnnotationValue(injectAnnotation, "index", -1);
        
        Set<String> names = new HashSet<String>();
        List<String> namesList = ASMHelper.<List<String>>getAnnotationValue(injectAnnotation, "name", (List<String>)null);
        if (namesList != null) {
            names.addAll(namesList);
        }
        
        return new ModifyVariableInjector(this, print, argsOnly, ordinal, index, names);
    }
    
    @Override
    protected String getDescription() {
        return "Variable modifier method";
    }
    
}
