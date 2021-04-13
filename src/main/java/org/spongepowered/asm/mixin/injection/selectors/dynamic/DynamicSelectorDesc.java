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

import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.selectors.ElementNode;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorByName;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorDynamic;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorDynamic.SelectorAnnotation;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorDynamic.SelectorId;
import org.spongepowered.asm.mixin.injection.selectors.InvalidSelectorException;
import org.spongepowered.asm.mixin.injection.selectors.MatchResult;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Quantifier;
import org.spongepowered.asm.util.SignaturePrinter;
import org.spongepowered.asm.util.asm.IAnnotationHandle;

import com.google.common.base.Strings;

/**
 * A {@link ITargetSelector Target Selector} which matches candidates using
 * descriptors contained in {@link Desc &#064;Desc} annotations. The descriptor
 * annotations may be local, or may be applied to the method or even the owning
 * mixin using user-specified ids, or implicit coordinates based on the relative
 * location of the consumer. eg. an &#064;At annotation and the &#064;Desc
 * annotation itself, which might be on a parent element for convenience.
 * 
 * <p>See {@link Desc &#064;Desc} for the specific syntax of descriptor
 * annotations.</p>
 * 
 * <h3>Local Descriptors</h3>
 * 
 * <p>Some annotations can use {@link Desc &#064;Desc} selectors directly,
 * namely {@link At &#064;At} (specified in {@link At#desc &#064;At.desc}) and
 * all <b>Injector</b> annotations (specified in <tt>target</tt> - for example 
 * {@link Inject#target &#064;Inject.target}). In these cases it is not
 * necessary to specify an {@link Desc#id id} for the descriptor since the usage
 * is inferred from context.</p>
 * 
 * <h3>Resolved Descriptors</h3>
 * 
 * <p>Descriptors can also be placed as annotations on the mixin method or the
 * mixin class, thus allowing descriptors to be re-used by multiple consumers.
 * For example where several injectors target the same method, the descriptor
 * can be placed on the mixin itself and consumed by id. In this situation it is
 * necessary to specify an {@link Desc#id id} for the descriptor and specify the
 * chosen id as a selector string:</p>
 * 
 * <blockquote><code>&#064;Mixin(TargetClass.class)<br />
 * &#064;Desc(id = "<ins>foo</ins>", value = "getFoo", args = { int.class })
 * <br />
 * abstract class MyMixin {<br />
 * <br />
 * &nbsp; &nbsp; &#064;Inject(target = "<ins>&#064;Desc(foo)</ins>", at =
 * &#064;At("RETURN"))<br />
 * &nbsp; &nbsp; void myHandlerMethod(int arg, CallbackInfo ci) {<br />
 * &nbsp; &nbsp; &nbsp; ...<br />
 * &nbsp; &nbsp; }<br />
 * <br />
 * }</code></blockquote>
 * 
 * <p>Note that the id string can be anything you wish, but is matched
 * case-insensitively and must be non-empty, so feel free to be as descriptive
 * as you wish.</p>
 * 
 * <h4>Implicit coordinates</h4>
 * 
 * <p>As an alternative to specifying an <em>explicit</em> descriptor id for
 * descriptors, it is also possible to use implicit coordinates generated using
 * the location of the consumer. To do so, use the selector <tt>"&#064;Desc"
 * </tt> with no arguments and give the descriptor an {@link Desc#id id} which
 * corresponds to the coordinate of the consumer:</p>
 * 
 * <p>In this example, the coordinate of the {@link At &#064;At} annotation,
 * <tt>"at"</tt> is used as the descriptor id.</p>
 * 
 * <blockquote><code>
 * &nbsp; &nbsp; &#064;Desc(id = "<ins>at</ins>",
 * value = "theMethodName", args = { String.class }, ret = boolean.class)<br />
 * &nbsp; &nbsp; &#064;Inject(method = "targetMethod()V", at =
 * &#064;At(value = "INVOKE" target = "<ins>&#064;Desc</ins>"))<br />
 * &nbsp; &nbsp; void myHandlerMethod(int arg, CallbackInfo ci) {<br />
 * &nbsp; &nbsp; &nbsp; ...<br />
 * &nbsp; &nbsp; }
 * </code></blockquote>
 * 
 * <p>As the resolver widens its search it adds new components to the start of
 * the implicit coordinate which match the element, separated by dots. For
 * example {@link At &#064;At} annotations inside a {@link Slice &#064;Slice}
 * will first resolve as (local coordinate) <tt>from</tt>, followed by
 * <tt>slice.from</tt>, and finally <tt>handlerMethodName.slice.from</tt>. This
 * allows implicit coordinates to be used up to the mixin level as in the first
 * example.</p>
 * 
 * <blockquote><code>&#064;Mixin(TargetClass.class)<br />
 * &#064;Desc(id = "<ins>myHandlerMethod.at</ins>", value = "theMethodName",
 * args = { String.class }, ret = boolean.class)<br />
 * &#064;Desc(id = "<ins>myHandlerMethod.method</ins>", value = "getFoo", args =
 * { int.class })<br />
 * abstract class MyMixin {<br />
 * <br />
 * &nbsp; &nbsp; &#064;Inject(method = "<ins>@Desc</ins>", at = &#064;At(value =
 * "INVOKE" target = "<ins>&#064;Desc</ins>"))<br />
 * &nbsp; &nbsp; void myHandlerMethod(int arg, CallbackInfo ci) {<br />
 * &nbsp; &nbsp; &nbsp; ...<br />
 * &nbsp; &nbsp; }
 * </code></blockquote>
 * 
 * <p>To view the coordinates used to resolve the descriptor for a particular
 * element, use the special id "?" in the selector:</p>
 * 
 * <blockquote><code>
 * &nbsp; &nbsp; &#064;Inject(method = "&#064;Desc", at = &#064;At(value =
 * "INVOKE" target = "<ins>&#064;Desc(?)</ins>"))
 * <br />
 * &nbsp; &nbsp; void myHandler(int arg, CallbackInfo ci) {
 * </code></blockquote>
 * 
 * <p>This will cause the resolver to pretty-print the considered coordinates
 * and elements into the console:</p>
 * 
 * <blockquote><pre>/*****************************************************&#47;
 *&#47;* Coordinate    Search Element       Detail         *&#47;
 *&#47;*****************************************************&#47;
 *&#47;* at            &#64;At                  &#64;At.desc       *&#47;
 *&#47;* at            myHandler            method         *&#47;
 *&#47;* myhandler.at  mixins.json:MyMixin  mixin          *&#47;
 *&#47;* myhandler.at  &#64;Redirect            &#64;Redirect.desc *&#47;
 *&#47;* myhandler.at  myhandler            method         *&#47;
 *&#47;*****************************************************&#47;</pre>
 *</blockquote>
 *
 * <p>Don't worry that some combinations of coordinate and element are not
 * possible (for example there is no member <tt>desc</tt> on <tt>&#64;At</tt>).
 * The resolver doesn't know that and scans each visited element for members it
 * supports.</p>
 */
@SelectorId("Desc")
@SelectorAnnotation(Desc.class)
public class DynamicSelectorDesc implements ITargetSelectorDynamic, ITargetSelectorByName {
    
    /**
     * Next selector
     */
    final class Next extends DynamicSelectorDesc {
        
        private final int index;
        
        Next(int index, IResolvedDescriptor next) {
            super(null, null, next.getOwner(), next.getName(), next.getArgs(), next.getReturnType(), next.getMatches(), null);
            this.index = index;
        }
        
        @Override
        public ITargetSelector next() {
            return DynamicSelectorDesc.this.next(this.index + 1);
        }
        
    }

    /**
     * Parser/resolver error mesage, only stored if the descriptor is invalid so
     * we can emit it when {@link #validate} is called
     */
    private final InvalidSelectorException parseException;

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
    
    /**
     * Required matches
     */
    private final Quantifier matches;
    
    /**
     * Annotation for next, parsed on demand
     */
    private final List<IAnnotationHandle> next;
    
    private DynamicSelectorDesc(IResolvedDescriptor desc) {
        this(null, desc.getId(), desc.getOwner(), desc.getName(), desc.getArgs(), desc.getReturnType(), desc.getMatches(),
                desc.getNext());
    }
    
    private DynamicSelectorDesc(DynamicSelectorDesc desc, Quantifier quantifier) {
        this(desc.parseException, desc.id, desc.owner, desc.name, desc.args, desc.returnType, quantifier, desc.next);
    }
    
    private DynamicSelectorDesc(DynamicSelectorDesc desc, Type owner) {
        this(desc.parseException, desc.id, owner, desc.name, desc.args, desc.returnType, desc.matches, desc.next);
    }
    
    private DynamicSelectorDesc(InvalidSelectorException ex) {
        this(ex, null, null, null, null, null, Quantifier.NONE, null);
    }
    
    protected DynamicSelectorDesc(InvalidSelectorException ex, String id, Type owner, String name, Type[] args, Type returnType, Quantifier matches,
            List<IAnnotationHandle> then) {
        this.parseException = ex;
        
        this.id = id;
        this.owner = owner;
        this.name = Strings.emptyToNull(name);
        this.args = args;
        this.returnType = returnType;
        this.methodDesc = returnType != null ? Bytecode.getDescriptor(returnType, args) : null;
        this.matches = matches;
        this.next = then;
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
        IResolvedDescriptor descriptor = DescriptorResolver.resolve(input, context);
        if (!descriptor.isResolved()) {
            String extra = input.length() == 0 ? ". " + descriptor.getResolutionInfo() : "";
            return new DynamicSelectorDesc(new InvalidSelectorException("Could not resolve @Desc(" + input + ") for " + context + extra));
        }
        return DynamicSelectorDesc.of(descriptor);
    }
    
    /**
     * Convert the supplied annotation into a selector instance
     * 
     * @param context Selector context
     * @param desc Annotation to parse
     * @return selector
     */
    public static DynamicSelectorDesc parse(IAnnotationHandle desc, ISelectorContext context) {
        IResolvedDescriptor descriptor = DescriptorResolver.resolve(desc, context);
        if (!descriptor.isResolved()) {
            return new DynamicSelectorDesc(new InvalidSelectorException("Invalid descriptor"));
        }
        return DynamicSelectorDesc.of(descriptor);
    }
    
    /**
     * Resolve a descriptor selector from the supplied context only, implicit
     * coordinates are used.
     * 
     * @param context Selector context
     * @return parsed selector
     */
    public static DynamicSelectorDesc resolve(ISelectorContext context) {
        IResolvedDescriptor descriptor = DescriptorResolver.resolve("", context);
        if (!descriptor.isResolved()) {
            return null;
        }
        return DynamicSelectorDesc.of(descriptor);
    }
    
    /**
     * Convert the supplied annotation into a selector instance
     * 
     * @param desc Annotation to parse
     * @param context Selector context
     * @return selector
     */
    public static DynamicSelectorDesc of(IAnnotationHandle desc, ISelectorContext context) {
        IResolvedDescriptor descriptor = DescriptorResolver.resolve(desc, context);
        if (!descriptor.isResolved()) {
            return null;
        }
        return DynamicSelectorDesc.of(descriptor);
    }
    
    /**
     * Convert the supplied annotation into a selector instance
     * 
     * @param desc Resolved descriptor
     * @return selector
     */
    public static DynamicSelectorDesc of(IResolvedDescriptor desc) {
        return new DynamicSelectorDesc(desc);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("@Desc(");
        boolean started = false;
        if (!Strings.isNullOrEmpty(this.id)) {
            sb.append("id = \"").append(this.id).append("\"");
            started = true;
        }
        
        if (this.owner != Type.VOID_TYPE) {
            if (started) {
                sb.append(", ");
            }
            sb.append("owner = ").append(SignaturePrinter.getTypeName(this.owner, false, false)).append(".class");
            started = true;
        }
        
        if (started) {
            sb.append(", ");
        }
        if (this.name != null) {
            sb.append("value = \"").append(this.name).append("\"");
        }
        
        if (this.args.length > 0) {
            sb.append(", args = { ");
            for (int i = 0; i < this.args.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(SignaturePrinter.getTypeName(this.args[i], false, false)).append(".class");
            }
            sb.append(" }");
        }
        
        if (this.returnType != Type.VOID_TYPE) {
            sb.append(", ret = ").append(SignaturePrinter.getTypeName(this.returnType, false, false)).append(".class");
        }
        
        sb.append(")");
        return sb.toString();
    }
    
    public String getId() {
        return this.id;
    }
    
    @Override
    public String getOwner() {
        return this.owner.getInternalName();
    }
    
    @Override
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
    public String getDesc() {
        return this.methodDesc;
    }
    
    @Override
    public String toDescriptor() {
        return new SignaturePrinter(this).setFullyQualified(true).toDescriptor();
    }
    
    @Override
    public ITargetSelector validate() throws InvalidSelectorException {
        if (this.parseException != null) {
            throw this.parseException;
        }
        return this;
    }

    @Override
    public ITargetSelector next() {
        return this.next(0);
    }
    
    protected ITargetSelector next(int index) {
        if (index >= 0 && index < this.next.size()) {
            IAnnotationHandle nextAnnotation = this.next.get(index);
            IResolvedDescriptor descriptor = DescriptorResolver.resolve(nextAnnotation, null);
            return new Next(index, descriptor);
        }
        
        return null;
    }

    @Override
    public ITargetSelector configure(Configure request, String... args) {
        request.checkArgs(args);
        switch (request) {
            case SELECT_MEMBER:
                if (this.matches.isDefault()) {
                    return new DynamicSelectorDesc(this, Quantifier.SINGLE);
                }
                break;
            case SELECT_INSTRUCTION:
                if (this.matches.isDefault()) {
                    return new DynamicSelectorDesc(this, Quantifier.ANY);
                }
                break;
            case MOVE:
                return new DynamicSelectorDesc(this, Type.getObjectType(args[0]));
            case CLEAR_LIMITS:
                if (this.getMinMatchCount() != 0 || this.getMaxMatchCount() < Integer.MAX_VALUE) {
                    return new DynamicSelectorDesc(this, Quantifier.ANY);
                }
                break;
            default:
                break;
        }
        return this;
    }
    
    @Override
    public ITargetSelector attach(ISelectorContext context) throws InvalidSelectorException {
        return this;
    }
    
    @Override
    public int getMinMatchCount() {
        return this.matches.getClampedMin();
    }
    
    @Override
    public int getMaxMatchCount() {
        return this.matches.getClampedMax();
    }
    
    @Override
    public MatchResult matches(String owner, String name, String desc) {
        return this.matches(owner, name, desc, this.methodDesc);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ITargetSelector
     *      #match(org.spongepowered.asm.util.asm.ElementNode)
     */
    @Override
    public <TNode> MatchResult match(ElementNode<TNode> node) {
        if (node == null) {
            return MatchResult.NONE;
        } else if (node.isField()) {
            return this.matches(node.getOwner(), node.getName(), node.getDesc(), this.returnType.getInternalName());
        } else {
            return this.matches(node.getOwner(), node.getName(), node.getDesc(), this.methodDesc);
        }
    }
    
    private MatchResult matches(String owner, String name, String desc, String compareWithDesc) {
        if (!compareWithDesc.equals(desc)) {
            return MatchResult.NONE;
        } else if (this.owner != Type.VOID_TYPE && !this.owner.getInternalName().equals(owner)) {
            return MatchResult.NONE;
        } else if (this.name != null && this.name.equals(name)) {
            return MatchResult.EXACT_MATCH;
        } else if (this.name != null && this.name.equalsIgnoreCase(name)) {
            return MatchResult.MATCH;
        } else if (this.name == null) {
            return MatchResult.EXACT_MATCH;
        }
        return MatchResult.NONE;
    }

}
