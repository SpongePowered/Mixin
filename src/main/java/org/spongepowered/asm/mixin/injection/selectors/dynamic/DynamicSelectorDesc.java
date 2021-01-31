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
package org.spongepowered.asm.mixin.injection.selectors.dynamic;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorDynamic.SelectorId;
import org.spongepowered.asm.mixin.injection.selectors.InvalidSelectorException;
import org.spongepowered.asm.mixin.injection.selectors.MatchResult;
import org.spongepowered.asm.mixin.injection.selectors.dynamic.DescriptorResolver.Result;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.asm.ElementNode;
import org.spongepowered.asm.util.asm.IAnnotationHandle;

/**
 * A {@link ITargetSelector Target Selector} which matches candidates using
 * descriptors contained in {@link Desc} annotations. The descriptor annotations
 * may be local, or may be applied to the method or even the owning mixin using
 * user-specified ids, or implicit coordinates.
 * 
 * <p>TODO complete javadoc</p>
 */
@SelectorId("Desc")
public final class DynamicSelectorDesc extends DynamicSelectorAbstract {

    /**
     * The input string, only stored if the descriptor is invalid so we can emit
     * it in the error message
     */
    private final String input;
    
    /**
     * Searched coordinates, only stored if the descriptor is invalid so we can
     * emit it in the error message
     */
    private final Set<String> searched;

    /**
     * Resolved id 
     */
    private final String id;
    
    /**
     * The owner specified in the resolved {@link Desc} annotation, or null if
     * resolution failed
     */
    private final Type owner;
    
    /**
     * The name specified in the resolved {@link Desc} annotation, or null if
     * resolution failed
     */
    private final String name;
    
    /**
     * The arguments specified in the resolved {@link Desc} annotation, or null
     * if resolution failed
     */
    private final Type[] args;
    
    /**
     * The return type specified in the resolved {@link Desc} annotation, or
     * null if resolution failed
     */
    private final Type returnType;
    
    /**
     * Method descriptor, used for matching against methods. Computed from
     * argument types and return type 
     */
    private final String methodDesc;
    
    private DynamicSelectorDesc(String id, Type owner, String name, Type[] args, Type returnType) {
        this(null, null, id, owner, name, args, returnType, Bytecode.getDescriptor(returnType, args));
    }
    
    private DynamicSelectorDesc(String input, Set<String> searched) {
        this(input, searched, null, null, null, null, null, null);
    }
    
    private DynamicSelectorDesc(String input, Set<String> searched, String id, Type owner, String name, Type[] args, Type returnType, String desc) {
        this.input = input;
        this.searched = searched;
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.args = args;
        this.returnType = returnType;
        this.methodDesc = desc; 
    }
    
    /**
     * Parse a descriptor selector from the supplied input. The input is treated
     * as a descriptor id to be resolved, or if empty uses implicit coordinates
     * from the selection context
     * 
     * @param input ID string, can be empty
     * @param context Selector context
     * @return parsed selector
     */
    public static DynamicSelectorDesc parse(String input, ISelectorContext context) {
        Result result = DescriptorResolver.resolve(input, context);
        if (!result.isResolved()) {
            return new DynamicSelectorDesc(input, result.getSearched());
        }
        return DynamicSelectorDesc.of(context, result.getAnnotation());
    }
    
    /**
     * Resolve a descriptor selector from the supplied context only, implicit
     * coordinates are used.
     * 
     * @param context Selector context
     * @return parsed selector
     */
    public static DynamicSelectorDesc resolve(ISelectorContext context) {
        Result result = DescriptorResolver.resolve("", context);
        if (!result.isResolved()) {
            return null;
        }
        return DynamicSelectorDesc.of(context, result.getAnnotation());
    }
    
    /**
     * Convert the supplied annotation into a selector instance
     * 
     * @param context Selector context
     * @param desc Annotation to parse
     * @return selector
     */
    public static DynamicSelectorDesc of(ISelectorContext context, IAnnotationHandle desc) {
        return DynamicSelectorDesc.of(context.getMixin(), desc);
    }
        
    /**
     * Convert the supplied annotation into a selector instance
     * 
     * @param mixin Mixin context
     * @param desc Annotation to parse
     * @return selector
     */
    public static DynamicSelectorDesc of(IMixinContext mixin, IAnnotationHandle desc) {
        String id = desc.<String>getValue("id", "");
        Type ownerClass = desc.<Type>getValue("owner", Type.VOID_TYPE);
        Type owner = ownerClass == Type.VOID_TYPE ? Type.getObjectType(mixin.getTargetClassRef()) : ownerClass;
        String name = desc.<String>getValue();
        List<Type> args = desc.getTypeList("args");
        Type returnType = desc.<Type>getValue("ret", Type.VOID_TYPE);
        return new DynamicSelectorDesc(id, owner, name, args.toArray(new Type[args.size()]), returnType);
    }
    
    public String getId() {
        return this.id;
    }
    
    public Type getOwner() {
        return this.owner;
    }

    public String getName() {
        return this.name;
    }

    public Type[] getArgs() {
        return this.args;
    }

    public Type getReturnType() {
        return this.returnType;
    }

    @Override
    public ITargetSelector validate() throws InvalidSelectorException {
        if (this.input != null) {
            String extra = this.input.length() == 0 ? " searched in " + this.searched : "";
            throw new InvalidSelectorException("Could not resolve @Desc(" + this.input + ")" + extra);
        }
        return this;
    }

    @Override
    public ITargetSelector next() {
        return this; // Recurse
    }

    @Override
    public int getMatchCount() {
        return 1;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ITargetSelector
     *      #match(org.spongepowered.asm.util.asm.ElementNode)
     */
    @Override
    public <TNode> MatchResult match(ElementNode<TNode> node) {
        if (node == null) {
            return MatchResult.NONE;
        } else if (node.isMethod()) {
            return this.matches(node.getOwnerName(), node.getName(), node.getDesc(), this.methodDesc);
        } else {
            return this.matches(node.getOwnerName(), node.getName(), node.getDesc(), this.returnType.getInternalName());
        }
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ITargetSelector
     *      #matches(org.objectweb.asm.tree.AbstractInsnNode)
     */
    @Override
    public MatchResult match(AbstractInsnNode insn) {
        if (insn instanceof MethodInsnNode) {
            MethodInsnNode method = (MethodInsnNode)insn;
            return this.matches(method.owner, method.name, method.desc, this.methodDesc);
        } else if (insn instanceof FieldInsnNode) {
            FieldInsnNode field = (FieldInsnNode)insn;
            return this.matches(field.owner, field.name, field.desc, this.returnType.getInternalName());
        }
        return MatchResult.NONE;
    }
    
    private MatchResult matches(String owner, String name, String desc, String compareWithDesc) {
        if (!compareWithDesc.equals(desc)) {
            return MatchResult.NONE;
        } else if (!this.owner.getInternalName().equals(owner)) {
            return MatchResult.NONE;
        } else if (this.name.equals(name)) {
            return MatchResult.EXACT_MATCH;
        } else if (this.name.equalsIgnoreCase(name)) {
            return MatchResult.MATCH;
        }
        return MatchResult.NONE;
    }

}
