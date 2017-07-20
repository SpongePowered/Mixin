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
package org.spongepowered.asm.mixin.gen;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.FieldNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.gen.throwables.InvalidAccessorException;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.struct.SpecialMethodInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Annotations;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

/**
 * Information about an accessor
 */
public class AccessorInfo extends SpecialMethodInfo {
    
    /**
     * Accessor types
     */
    public enum AccessorType {

        /**
         * A field getter, accessor must accept no args and return field type
         */
        FIELD_GETTER(ImmutableSet.<String>of("get", "is")) {
            @Override
            AccessorGenerator getGenerator(AccessorInfo info) {
                return new AccessorGeneratorFieldGetter(info);
            }
        },
        
        /**
         * A field setter, accessor must accept single arg of the field type and
         * return void
         */
        FIELD_SETTER(ImmutableSet.<String>of("set")) {
            @Override
            AccessorGenerator getGenerator(AccessorInfo info) {
                return new AccessorGeneratorFieldSetter(info);
            }
        },
        
        /**
         * An invoker (proxy) method
         */
        METHOD_PROXY(ImmutableSet.<String>of("call", "invoke")) {
            @Override
            AccessorGenerator getGenerator(AccessorInfo info) {
                return new AccessorGeneratorMethodProxy(info);
            }
        };
        
        private final Set<String> expectedPrefixes;
        
        private AccessorType(Set<String> expectedPrefixes) {
            this.expectedPrefixes = expectedPrefixes;
        }
        
        /**
         * Returns true if the supplied prefix string is an allowed prefix for
         * this accessor type
         * 
         * @param prefix prefix to check
         * @return true if the expected prefix set contains the supplied value
         */
        public boolean isExpectedPrefix(String prefix) {
            return this.expectedPrefixes.contains(prefix);
        }
        
        /**
         * Returns all the expected prefixes for this accessor type as a string
         * for debugging/error message purposes
         * 
         * @return string representation of expected prefixes for this accessor
         *      type
         */
        public String getExpectedPrefixes() {
            return this.expectedPrefixes.toString();
        }
        
        abstract AccessorGenerator getGenerator(AccessorInfo info);
    }
    
    /**
     * Pattern for matching accessor names (for inflector)
     */
    protected static final Pattern PATTERN_ACCESSOR = Pattern.compile("^(get|set|is|invoke|call)(([A-Z])(.*?))(_\\$md.*)?$");
    
    /**
     * Accessor method argument types (raw, from method)
     */
    protected final Type[] argTypes;
    
    /**
     * Accessor method return type (raw, from method)
     */
    protected final Type returnType;
    
    /**
     * Type of accessor to generate, computed based on the signature of the
     * target method.
     */
    protected final AccessorType type;

    /**
     * For field accessors, the expected type of the target field
     */
    private final Type targetFieldType;

    /**
     * Computed information about the target field or method, name and
     * descriptor
     */
    protected final MemberInfo target;
    
    /**
     * For accessors, stores the discovered target field
     */
    protected FieldNode targetField;
    
    /**
     * For invokers, stores the discovered target method
     */
    protected MethodNode targetMethod;
    
    public AccessorInfo(MixinTargetContext mixin, MethodNode method) {
        this(mixin, method, Accessor.class);
    }
    
    protected AccessorInfo(MixinTargetContext mixin, MethodNode method, Class<? extends Annotation> annotationClass) {
        super(mixin, method, Annotations.getVisible(method, annotationClass));
        this.argTypes = Type.getArgumentTypes(method.desc);
        this.returnType = Type.getReturnType(method.desc);
        this.type = this.initType();
        this.targetFieldType = this.initTargetFieldType();
        this.target = this.initTarget();
    }

    protected AccessorType initType() {
        if (this.returnType.equals(Type.VOID_TYPE)) {
            return AccessorType.FIELD_SETTER;
        }
        return AccessorType.FIELD_GETTER;
    }

    protected Type initTargetFieldType() {
        switch (this.type) {
            case FIELD_GETTER:
                if (this.argTypes.length > 0) {
                    throw new InvalidAccessorException(this.mixin, this + " must take exactly 0 arguments, found " + this.argTypes.length);
                }
                return this.returnType;
                
            case FIELD_SETTER:
                if (this.argTypes.length != 1) {
                    throw new InvalidAccessorException(this.mixin, this + " must take exactly 1 argument, found " + this.argTypes.length);
                }
                return this.argTypes[0];
                
            default:
                throw new InvalidAccessorException(this.mixin, "Computed unsupported accessor type " + this.type + " for " + this);
        }
    }

    protected MemberInfo initTarget() {
        MemberInfo target = new MemberInfo(this.getTargetName(), null, this.targetFieldType.getDescriptor());
        this.annotation.visit("target", target.toString());
        return target;
    }

    protected String getTargetName() {
        String name = Annotations.<String>getValue(this.annotation);
        if (Strings.isNullOrEmpty(name)) {
            String inflectedTarget = this.inflectTarget();
            if (inflectedTarget == null) {
                throw new InvalidAccessorException(this.mixin, "Failed to inflect target name for " + this + ", supported prefixes: [get, set, is]");
            }
            return inflectedTarget;
        }
        return MemberInfo.parse(name, this.mixin).name;
    }

    /**
     * Uses the name of this accessor method and the calculated accessor type to
     * try and inflect the name of the target field or method. This allows a
     * method named <tt>getFoo</tt> to be inflected to a target named
     * <tt>foo</tt> for example.  
     */
    protected String inflectTarget() {
        return AccessorInfo.inflectTarget(this.method.name, this.type, this.toString(), this.mixin,
                this.mixin.getEnvironment().getOption(Option.DEBUG_VERBOSE));
    }

    /**
     * Uses the name of an accessor method and the accessor type to try and
     * inflect the name of the target field or method. This allows a method
     * named <tt>getFoo</tt> to be inflected to a target named <tt>foo</tt> for
     * example.  
     * 
     * @param accessorName Name of the accessor method
     * @param accessorType Type of accessor being processed, this is calculated
     *      from the method signature (<tt>void</tt> methods being setters,
     *      methods with return types being getters)
     * @param accessorDescription description of the accessor to include in
     *      error messages
     * @param context Mixin context
     * @param verbose Emit warnings when accessor prefix doesn't match type
     * @return inflected target member name or <tt>null</tt> if name cannot be
     *      inflected 
     */
    public static String inflectTarget(String accessorName, AccessorType accessorType, String accessorDescription,
            IMixinContext context, boolean verbose) {
        Matcher nameMatcher = AccessorInfo.PATTERN_ACCESSOR.matcher(accessorName);
        if (nameMatcher.matches()) {
            String prefix = nameMatcher.group(1);
            String firstChar = nameMatcher.group(3);
            String remainder = nameMatcher.group(4);
            // If the entire name is upper case, do not lowercase the first char
            String name = String.format("%s%s", AccessorInfo.toLowerCase(firstChar, !AccessorInfo.isUpperCase(remainder)), remainder);
            if (!accessorType.isExpectedPrefix(prefix) && verbose) {
                LogManager.getLogger("mixin").warn("Unexpected prefix for {}, found [{}] expecting {}", accessorDescription, prefix,
                        accessorType.getExpectedPrefixes());
            }
            return MemberInfo.parse(name, context).name;
        }
        return null;
    }

    /**
     * Get the inflected/specified target member for this accessor
     */
    public final MemberInfo getTarget() {
        return this.target;
    }
    
    /**
     * For field accessors, returns the field type, returns null for invokers
     */
    public final Type getTargetFieldType() {
        return this.targetFieldType;
    }
    
    /**
     * For field accessors, returns the target field, returns null for invokers
     */
    public final FieldNode getTargetField() {
        return this.targetField;
    }
    
    /**
     * For invokers, returns the target method, returns null for field accessors
     */
    public final MethodNode getTargetMethod() {
        return this.targetMethod;
    }
    
    /**
     * Get the return type of the annotated method
     */
    public final Type getReturnType() {
        return this.returnType;
    }
    
    /**
     * Get the argument types of the annotated method
     */
    public final Type[] getArgTypes() {
        return this.argTypes;
    }

    @Override
    public String toString() {
        return String.format("%s->@%s[%s]::%s%s", this.mixin.toString(), Bytecode.getSimpleName(this.annotation), this.type.toString(),
                this.method.name, this.method.desc);
    }

    /**
     * First pass, locate the target field in the class. This is done after all
     * other mixins are applied so that mixin-added fields and methods can be
     * targetted. 
     */
    public void locate() {
        this.targetField = this.findTargetField();
    }
    
    /**
     * Second pass, generate the actual accessor method for this accessor. The
     * method still respects intrinsic/mixinmerged rules so is not guaranteed to
     * be added to the target class
     * 
     * @return generated accessor method
     */
    public MethodNode generate() {
        MethodNode generatedAccessor = this.type.getGenerator(this).generate();
        Bytecode.mergeAnnotations(this.method, generatedAccessor);
        return generatedAccessor;
    }

    private FieldNode findTargetField() {
        return this.<FieldNode>findTarget(this.classNode.fields);
    }
        
    /**
     * Generified candidate search, since the search logic is the same for both
     * fields and methods.
     * 
     * @param nodes Node list to search (method/field list)
     * @param <TNode> node type
     * @return best match
     */
    protected <TNode> TNode findTarget(List<TNode> nodes) {
        TNode exactMatch = null;
        List<TNode> candidates = new ArrayList<TNode>();
        
        for (TNode node : nodes) {
            String desc = AccessorInfo.<TNode>getNodeDesc(node);
            if (desc == null || !desc.equals(this.target.desc)) {
                continue;
            }
            
            String name = AccessorInfo.<TNode>getNodeName(node);
            if (name != null) {
                if (name.equals(this.target.name)) {
                    exactMatch = node;
                }
                if (name.equalsIgnoreCase(this.target.name)) {
                    candidates.add(node);
                }
            }
        }
        
        if (exactMatch != null) {
            if (candidates.size() > 1) {
                LogManager.getLogger("mixin").debug("{} found an exact match for {} but other candidates were found!", this, this.target);
            }
            return exactMatch;
        }
        
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        
        String number = candidates.size() == 0 ? "No" : "Multiple";
        throw new InvalidAccessorException(this, number + " candidates were found matching " + this.target + " in " + this.classNode.name
                + " for " + this);
    }
    
    private static <TNode> String getNodeDesc(TNode node) {
        return (node instanceof MethodNode) ? ((MethodNode)node).desc : ((node instanceof FieldNode) ? ((FieldNode)node).desc : null);
    }

    private static <TNode> String getNodeName(TNode node) {
        return (node instanceof MethodNode) ? ((MethodNode)node).name : ((node instanceof FieldNode) ? ((FieldNode)node).name : null);
    }
    
    /**
     * Return a wrapper AccessorInfo of the correct type based on the method
     * passed in.
     * 
     * @param mixin mixin context which owns this accessor
     * @param method annotated method
     * @param type annotation type to process
     * @return parsed AccessorInfo
     */
    public static AccessorInfo of(MixinTargetContext mixin, MethodNode method, Class<? extends Annotation> type) {
        if (type == Accessor.class) {
            return new AccessorInfo(mixin, method);
        } else if (type == Invoker.class) {
            return new InvokerInfo(mixin, method);
        }
        throw new InvalidAccessorException(mixin, "Could not parse accessor for unknown type " + type.getName());
    }

    private static String toLowerCase(String string, boolean condition) {
        return condition ? string.toLowerCase() : string;
    }
    
    private static boolean isUpperCase(String string) {
        return string.toUpperCase().equals(string);
    }

}
