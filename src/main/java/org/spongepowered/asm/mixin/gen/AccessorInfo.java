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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.gen.throwables.InvalidAccessorException;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelector.Result;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.struct.SpecialMethodInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.asm.ElementNode;

import com.google.common.base.Joiner;
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
        },
        
        /**
         * An invoker (proxy) method
         */
        OBJECT_FACTORY(ImmutableSet.<String>of("new", "create")) {
            @Override
            AccessorGenerator getGenerator(AccessorInfo info) {
                return new AccessorGeneratorObjectFactory(info);
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
        public Set<String> getExpectedPrefixes() {
            return Collections.<String>unmodifiableSet(this.expectedPrefixes);
        }
        
        abstract AccessorGenerator getGenerator(AccessorInfo info);
    
    }
    
    /**
     * Accessor Name struct
     */
    public static final class AccessorName {
        
        /**
         * Pattern for matching accessor names (for inflector)
         */
        private static final Pattern PATTERN = Pattern.compile("^(" + AccessorName.getPrefixList() + ")(([A-Z])(.*?))(_\\$md.*)?$");
        
        /**
         * Name of the accessor method 
         */
        public final String methodName;
        
        /**
         * Accessor prefix
         */
        public final String prefix;
        
        /**
         * Accessor name part 
         */
        public final String name;
        
        private AccessorName(String methodName, String prefix, String name) {
            this.methodName = methodName;
            this.prefix = prefix;
            this.name = name;
        }
        
        @Override
        public String toString() {
            return super.toString();
        }
        
        /**
         * Get an accessor name from the supplied string. If the string matches
         * the accessor name regex, split the string into the relevant parts
         * 
         * @param methodName Name of the accessor method
         * @return Parsed AccessorName struct or null if the name is not a valid
         *      accessor name
         */
        public static AccessorName of(String methodName) {
            return AccessorName.of(methodName, true);
        }
        
        /**
         * Get an accessor name from the supplied string. If the string matches
         * the accessor name regex, split the string into the relevant parts
         * 
         * @param methodName Name of the accessor method
         * @param toMemberCase True if the first character of the name should be
         *      conditionally converted to lowercase. If the name is all
         *      uppercase (eg. if the NAME_IS_A_CONSTANT) the first character
         *      will not be lowercased, regardless of the state of this argument
         * @return Parsed AccessorName struct or null if the name is not a valid
         *      accessor name
         */
        public static AccessorName of(String methodName, boolean toMemberCase) { 
            Matcher nameMatcher = AccessorName.PATTERN.matcher(methodName);
            if (nameMatcher.matches()) {
                String prefix = nameMatcher.group(1);
                String namePart = nameMatcher.group(2);
                String firstChar = nameMatcher.group(3);
                String remainder = nameMatcher.group(4);
                boolean nameIsUpperCase = AccessorName.isUpperCase(Locale.ROOT, namePart);
                // If the entire name is upper case, do not lowercase the first char
                String name = String.format("%s%s", AccessorName.toLowerCaseIf(Locale.ROOT, firstChar, toMemberCase && !nameIsUpperCase), remainder);
                return new AccessorName(methodName, prefix, name);
            }
            return null;
        }
        
        private static boolean isUpperCase(Locale locale, String string) {
            return string.toUpperCase(locale).equals(string);
        }
        
        private static String toLowerCaseIf(Locale locale, String string, boolean condition) {
            return condition ? string.toLowerCase(locale) : string;
        }

        private static String getPrefixList() {
            List<String> prefixes = new ArrayList<String>();
            for (AccessorType type : AccessorType.values()) {
                prefixes.addAll(type.getExpectedPrefixes());
            }
            return Joiner.on('|').join(prefixes);
        }

    }
    
    /**
     * Annotation class
     */
    protected final Class<? extends Annotation> annotationClass;
    
    /**
     * Accessor method argument types (raw, from method)
     */
    protected final Type[] argTypes;
    
    /**
     * Accessor method return type (raw, from method)
     */
    protected final Type returnType;
    
    /**
     * Accessor method staticness
     */
    protected final boolean isStatic;
    
    /**
     * Name specified in the attached annotation, can be null 
     */
    protected final String specifiedName;
    
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
    protected final ITargetSelector target;
    
    /**
     * For accessors, stores the discovered target field
     */
    protected FieldNode targetField;
    
    /**
     * For invokers, stores the discovered target method
     */
    protected MethodNode targetMethod;
    
    /**
     * Generator which will be responsible for actually generating the accessor
     * method body 
     */
    protected AccessorGenerator generator;
    
    public AccessorInfo(MixinTargetContext mixin, MethodNode method) {
        this(mixin, method, Accessor.class);
    }
    
    protected AccessorInfo(MixinTargetContext mixin, MethodNode method, Class<? extends Annotation> annotationClass) {
        super(mixin, method, Annotations.getVisible(method, annotationClass));
        this.annotationClass = annotationClass;
        this.argTypes = Type.getArgumentTypes(method.desc);
        this.returnType = Type.getReturnType(method.desc);
        this.isStatic = Bytecode.isStatic(method);
        this.specifiedName = Annotations.<String>getValue(this.annotation);
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

    protected ITargetSelector initTarget() {
        MemberInfo target = new MemberInfo(this.getTargetName(this.specifiedName), null, this.targetFieldType.getDescriptor());
        this.annotation.visit("target", target.toString());
        return target;
    }

    protected String getTargetName(String name) {
        if (Strings.isNullOrEmpty(name)) {
            String inflectedTarget = this.inflectTarget();
            if (inflectedTarget == null) {
                throw new InvalidAccessorException(this.mixin, String.format("Failed to inflect target name for %s, supported prefixes: %s",
                        this, this.type.getExpectedPrefixes()));
            }
            return inflectedTarget;
        }
        return TargetSelector.parseName(name, this.mixin);
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
     * @param name Name of the accessor method
     * @param type Type of accessor being processed, this is calculated
     *      from the method signature (<tt>void</tt> methods being setters,
     *      methods with return types being getters)
     * @param description description of the accessor to include in
     *      error messages
     * @param context Mixin context
     * @param verbose Emit warnings when accessor prefix doesn't match type
     * @return inflected target member name or <tt>null</tt> if name cannot be
     *      inflected 
     */
    public static String inflectTarget(String name, AccessorType type, String description, IMixinContext context, boolean verbose) {
        return AccessorInfo.inflectTarget(AccessorName.of(name), type, description, context, verbose);
    }
    
    /**
     * Uses the name of an accessor method and the accessor type to try and
     * inflect the name of the target field or method. This allows a method
     * named <tt>getFoo</tt> to be inflected to a target named <tt>foo</tt> for
     * example.  
     * 
     * @param name Name of the accessor method
     * @param type Type of accessor being processed, this is calculated
     *      from the method signature (<tt>void</tt> methods being setters,
     *      methods with return types being getters)
     * @param description description of the accessor to include in
     *      error messages
     * @param context Mixin context
     * @param verbose Emit warnings when accessor prefix doesn't match type
     * @return inflected target member name or <tt>null</tt> if name cannot be
     *      inflected 
     */
    public static String inflectTarget(AccessorName name, AccessorType type, String description, IMixinContext context, boolean verbose) {
        if (name != null) {
            if (!type.isExpectedPrefix(name.prefix) && verbose) {
                LogManager.getLogger("mixin").warn("Unexpected prefix for {}, found [{}] expecting {}", description, name.prefix,
                        type.getExpectedPrefixes());
            }
            return TargetSelector.parseName(name.name, context);
        }
        return null;
    }
    
    

    /**
     * Get the inflected/specified target member for this accessor
     */
    public final ITargetSelector getTarget() {
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
    
    /**
     * Get whether the accessor itself is static
     */
    public boolean isStatic() {
        return this.isStatic;
    }

    @Override
    public String toString() {
        String typeString = this.type != null ? this.type.toString() : "UNPARSED_ACCESSOR";
        return String.format("%s->@%s[%s]::%s%s", this.mixin, Bytecode.getSimpleName(this.annotation), typeString,
                this.methodName, this.method.desc);
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
     * Called immediately after locate, initialises the generator for this
     * accessor and runs validation. 
     */
    public void validate() {
        this.generator = this.type.getGenerator(this);
        this.generator.validate();
    }
    
    /**
     * Second pass, generate the actual accessor method for this accessor. The
     * method still respects intrinsic/mixinmerged rules so is not guaranteed to
     * be added to the target class
     * 
     * @return generated accessor method
     */
    public MethodNode generate() {
        MethodNode generatedAccessor = this.generator.generate();
        Annotations.merge(this.method, generatedAccessor);
        return generatedAccessor;
    }

    private FieldNode findTargetField() {
        return this.<FieldNode>findTarget(ElementNode.fieldList(this.classNode));
    }
        
    /**
     * Generified candidate search, since the search logic is the same for both
     * fields and methods.
     * 
     * @param nodes Node list to search (method/field list)
     * @param <TNode> node type
     * @return best match
     */
    protected <TNode> TNode findTarget(List<ElementNode<TNode>> nodes) {
        Result<TNode> result = TargetSelector.<TNode>run(this.target.configure("orphan"), nodes);

        try {
            return result.getSingleResult(true);
        } catch (IllegalStateException ex) {
            throw new InvalidAccessorException(this, ex.getMessage() + " matching " + this.target + " in " + this.classNode.name + " for " + this);
        }
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

}
