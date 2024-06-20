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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.IInjectionPointContext;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InjectionPoint.RestrictTargetLevel;
import org.spongepowered.asm.mixin.injection.InjectionPoint.Specifier;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.InvalidSelectorException;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.dynamic.DynamicSelectorDesc;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionPointException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Annotations.Handle;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.IMessageSink;
import org.spongepowered.asm.util.asm.IAnnotationHandle;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.primitives.Ints;

/**
 * Data read from an {@link org.spongepowered.asm.mixin.injection.At} annotation
 * and passed into an InjectionPoint ctor
 */
public class InjectionPointData {
    
    /**
     * Regex for recognising at declarations
     */
    private static final Pattern AT_PATTERN = InjectionPointData.createPattern(); 
    
    /**
     * K/V arguments parsed from the "args" node in the {@link At} annotation 
     */
    private final Map<String, String> args = new HashMap<String, String>();
    
    /**
     * Injection point context (annotated method) 
     */
    private final IInjectionPointContext context;

    /**
     * At arg 
     */
    private final String at;

    /**
     * Parsed from the at argument, the injector type (shortcode or class name)
     */
    private final String type;

    /**
     * Selector parsed from the at argument, only used by slices  
     */
    private final Specifier specifier;

    /**
     * Target restriction from the at annotation, if present
     */
    private final RestrictTargetLevel targetRestriction;
    
    /**
     * Target 
     */
    private final String target;
    
    /**
     * Slice id specified in the annotation, only used by slices 
     */
    private final String slice;
    
    /**
     * Ordinal 
     */
    private final int ordinal;
    
    /**
     * Opcode 
     */
    private final int opcode;
    
    /**
     * Injection point id from annotation 
     */
    private final String id;
    
    /**
     * Flags from annotation parser
     */
    private final int flags;
    
    public InjectionPointData(IInjectionPointContext context, String at, List<String> args, String target,
            String slice, int ordinal, int opcode, String id, int flags) {
        this.context = context;
        this.at = at;
        this.target = target;
        this.slice = Strings.nullToEmpty(slice);
        this.ordinal = Math.max(-1, ordinal);
        this.opcode = opcode;
        this.id = id;
        this.flags = flags;
        
        this.parseArgs(args);
        
        this.args.put("target", target);
        this.args.put("ordinal", String.valueOf(ordinal));
        this.args.put("opcode", String.valueOf(opcode));

        Matcher matcher = InjectionPointData.AT_PATTERN.matcher(at);
        this.type = InjectionPointData.parseType(matcher, at);
        this.specifier = InjectionPointData.parseSpecifier(matcher);
        
        this.targetRestriction = this.isUnsafe() ? RestrictTargetLevel.ALLOW_ALL : RestrictTargetLevel.METHODS_ONLY;
    }

    private void parseArgs(List<String> args) {
        if (args == null) {
            return;
        }
        for (String arg : args) {
            if (arg != null) {
                int eqPos = arg.indexOf('=');
                if (eqPos > -1) {
                    this.args.put(arg.substring(0, eqPos), arg.substring(eqPos + 1));
                } else {
                    this.args.put(arg, "");
                }
            }
        }
    }
    
    /**
     * Get the message sink for this injection point
     */
    public IMessageSink getMessageSink() {
        return this.context;
    }

    /**
     * Get the <tt>at</tt> value on the injector
     */
    public String getAt() {
        return this.at;
    }
    
    /**
     * Get the parsed constructor <tt>type</tt> for this injector
     */
    public String getType() {
        return this.type;
    }
    
    /**
     * Get the specifier value parsed from the injector
     */
    public Specifier getSpecifier() {
        return this.specifier;
    }

    /**
     * Get the target restriction specified in the annotation
     */
    public RestrictTargetLevel getTargetRestriction() {
        return this.targetRestriction;
    }

    /**
     * Get the injection point context
     */
    public IInjectionPointContext getContext() {
        return this.context;
    }
    
    /**
     * Get the mixin context
     */
    public IMixinContext getMixin() {
        return this.context.getMixin();
    }
    
    /**
     * Get the annotated method
     */
    public MethodNode getMethod() {
        return this.context.getMethod();
    }
    
    /**
     * Get the return type of the annotated method
     */
    public Type getMethodReturnType() {
        return Type.getReturnType(this.getMethod().desc);
    }
    
    /**
     * Get the root annotation (eg. {@link Inject})
     */
    public AnnotationNode getParent() {
        return this.context.getAnnotationNode();
    }
    
    /**
     * Get the slice id specified on the injector
     */
    public String getSlice() {
        return this.slice;
    }
    
    public LocalVariableDiscriminator getLocalVariableDiscriminator() {
        return LocalVariableDiscriminator.parse(this.getParent());
    }

    /**
     * Get the supplied value from the named args, return defaultValue if the
     * arg is not set
     * 
     * @param key argument name
     * @param defaultValue value to return if the arg is not set
     * @return argument value or default if not set
     */
    public String get(String key, String defaultValue) {
        String value = this.args.get(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Get the supplied value from the named args, return defaultValue if the
     * arg is not set
     * 
     * @param key argument name
     * @param defaultValue value to return if the arg is not set
     * @return argument value or default if not set
     */
    public int get(String key, int defaultValue) {
        return InjectionPointData.parseInt(this.get(key, String.valueOf(defaultValue)), defaultValue);
    }
    
    /**
     * Get the supplied value from the named args, return defaultValue if the
     * arg is not set
     * 
     * @param key argument name
     * @param defaultValue value to return if the arg is not set
     * @return argument value or default if not set
     */
    public boolean get(String key, boolean defaultValue) {
        return InjectionPointData.parseBoolean(this.get(key, String.valueOf(defaultValue)), defaultValue);
    }
    
    /**
     * Get the supplied value from the named args, return defaultValue if the
     * arg is not set
     * 
     * @param <T> enum type
     * @param key argument name
     * @param defaultValue value to return if the arg is not set
     * @return argument value or default if not set
     */
    public <T extends Enum<T>> T get(String key, T defaultValue) {
        return InjectionPointData.<T>parseEnum(this.get(key, defaultValue.name()), defaultValue);
    }

    /**
     * Get the supplied value from the named args as a target selector,
     * throws an exception if the argument cannot be parsed as a target selector
     * 
     * @param key argument name
     * @return argument value as a target selector
     */
    public ITargetSelector get(String key) {
        try {
            return TargetSelector.parseAndValidate(this.get(key, ""), this.context);
        } catch (InvalidSelectorException ex) {
            throw new InvalidInjectionPointException(this.getMixin(), ex, "Failed parsing @At(\"%s\").%s \"%s\" on %s",
                    this.at, key, this.target, this.getDescription());
        }
    }
    
    /**
     * Get the target value specified on the injector
     */
    public ITargetSelector getTarget() {
        try {
            if (Strings.isNullOrEmpty(this.target)) {
                IAnnotationHandle selectorAnnotation = this.context.getSelectorAnnotation();
                AnnotationNode desc = Annotations.<AnnotationNode>getValue(((Handle)selectorAnnotation).getNode(), "desc");
                if (desc != null) {
                    String id = Annotations.<String>getValue(desc, "id", "at");
                    if ("at".equalsIgnoreCase(id)) {
                        return DynamicSelectorDesc.of(Annotations.handleOf(desc), this.context);
                    }
                }
            }
            return TargetSelector.parseAndValidate(this.target, this.context);
        } catch (InvalidSelectorException ex) {
            throw new InvalidInjectionPointException(this.getMixin(), ex, "Failed validating @At(\"%s\").target \"%s\" on %s",
                    this.at, this.target, this.getDescription());
        }
    }

    /**
     * Get a description of this injector for use in error messages
     */
    public String getDescription() {
        return InjectionInfo.describeInjector(this.context.getMixin(), this.context.getAnnotationNode(), this.context.getMethod());
    }

    /**
     * Get the ordinal specified on the injection point
     */
    public int getOrdinal() {
        return this.ordinal;
    }
    
    /**
     * Get the opcode specified on the injection point
     */
    public int getOpcode() {
        return this.opcode;
    }
    
    /**
     * Get the opcode specified on the injection point or return the default if
     * no opcode was specified
     * 
     * @param defaultOpcode opcode to return if none specified
     * @return opcode or default
     */
    public int getOpcode(int defaultOpcode) {
        return this.opcode > 0 ? this.opcode : defaultOpcode;
    }
    
    /**
     * Get the opcode specified on the injection point or return the default if
     * no opcode was specified or if the specified opcode does not appear in the
     * supplied list of valid opcodes
     * 
     * @param defaultOpcode opcode to return if none specified
     * @param validOpcodes valid opcodes
     * @return opcode or default
     */
    public int getOpcode(int defaultOpcode, int... validOpcodes) {
        for (int validOpcode : validOpcodes) {
            if (this.opcode == validOpcode) {
                return this.opcode;
            }
        }
        return defaultOpcode;
    }

    /**
     * Get a list of opcodes specified in the injection point arguments. The
     * opcodes can be specified as raw integer values or as their corresponding
     * constant name from the {@link Opcodes} interface. All the values should
     * be separated by spaces or commas. The returned array is sorted in order
     * to make it suitable for use with the {@link Arrays#binarySearch} method.
     * 
     * @param key argument name
     * @param defaultValue value to return if the key is not specified
     * @return parsed opcodes as array or default value if the key is not
     *      specified in the args
     */
    public int[] getOpcodeList(String key, int[] defaultValue) {
        String value = this.args.get(key);
        if (value == null) {
            return defaultValue;
        }
        
        Set<Integer> parsed = new TreeSet<Integer>();
        String[] values = value.split("[ ,;]");
        for (String strOpcode : values) {
            int opcode = Bytecode.parseOpcodeName(strOpcode.trim());
            if (opcode > 0) {
                parsed.add(opcode);
            }                
        }
        
        return Ints.toArray(parsed);
    }

    /**
     * Get the id specified on the injection point (or null if not specified)
     */
    public String getId() {
        return this.id;
    }
    
    /**
     * Get whether the <tt>unsafe</tt> option is set on the injection point
     */
    public boolean isUnsafe() {
        return (this.flags & InjectionPoint.Flags.UNSAFE) != 0;
    }
    
    @Override
    public String toString() {
        return this.type;
    }

    private static Pattern createPattern() {
        return Pattern.compile(String.format("^(.+?)(:(%s))?$", Joiner.on('|').join(Specifier.values())));
    }

    /**
     * Parse a constructor type from the supplied <tt>at</tt> string
     * 
     * @param at at to parse
     * @return parsed constructor type
     */
    public static String parseType(String at) {
        Matcher matcher = InjectionPointData.AT_PATTERN.matcher(at);
        return InjectionPointData.parseType(matcher, at);
    }

    private static String parseType(Matcher matcher, String at) {
        return matcher.matches() ? matcher.group(1) : at;
    }

    private static Specifier parseSpecifier(Matcher matcher) {
        return matcher.matches() && matcher.group(3) != null ? Specifier.valueOf(matcher.group(3)) : Specifier.DEFAULT;
    }
    
    private static int parseInt(String string, int defaultValue) {
        try {
            return Integer.parseInt(string);
        } catch (Exception ex) {
            return defaultValue;
        }
    }
    
    private static boolean parseBoolean(String string, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(string);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Enum<T>> T parseEnum(String string, T defaultValue) {
        try {
            return (T)Enum.valueOf(defaultValue.getClass(), string);
        } catch (Exception ex) {
            return defaultValue;
        }
    }
}
