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

import java.util.List;

import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.invoke.ModifyConstantInjector;
import org.spongepowered.asm.mixin.injection.points.BeforeConstant;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

/**
 * Information about a constant modifier injector
 */
public class ModifyConstantInjectionInfo extends InjectionInfo {

    private static final String CONSTANT_ANNOTATION_CLASS = Constant.class.getName().replace('.', '/');

    public ModifyConstantInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation, "constant");
    }
    
    @Override
    protected List<AnnotationNode> readInjectionPoints(String type) {
        List<AnnotationNode> ats = super.readInjectionPoints(type);
        if (ats.isEmpty()) {
            AnnotationNode c = new AnnotationNode(ModifyConstantInjectionInfo.CONSTANT_ANNOTATION_CLASS);
            c.visit("log", Boolean.TRUE);
            ats = ImmutableList.<AnnotationNode>of(c);
        }
        return ats;
    }

    @Override
    protected void parseInjectionPoints(List<AnnotationNode> ats) {
        Type returnType = Type.getReturnType(this.method.desc);
        
        for (AnnotationNode at : ats) {
            this.injectionPoints.add(new BeforeConstant(this.getContext(), at, returnType.getDescriptor()));
        }
    }
    
    @Override
    protected Injector parseInjector(AnnotationNode injectAnnotation) {
        return new ModifyConstantInjector(this);
    }
    
    @Override
    protected String getDescription() {
        return "Constant modifier method";
    }
    
    @Override
    public String getSliceId(String id) {
        return Strings.nullToEmpty(id);
    }

}
