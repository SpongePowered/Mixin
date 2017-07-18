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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.InjectionPoint.Selector;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionPointException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

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
     * Mixin 
     */
    private final IMixinContext context;
    
    /**
     * Injector callback
     */
    private final MethodNode method;

    /**
     * Parent annotation
     */
    private final AnnotationNode parent;

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
    private final Selector selector;

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
    
    public InjectionPointData(IMixinContext context, MethodNode method, AnnotationNode parent, String at, List<String> args, String target,
            String slice, int ordinal, int opcode, String id) {
        this.context = context;
        this.method = method;
        this.parent = parent;
        this.at = at;
        this.target = target;
        this.slice = Strings.nullToEmpty(slice);
        this.ordinal = Math.max(-1, ordinal);
        this.opcode = opcode;
        this.id = id;
        
        this.parseArgs(args);
        
        this.args.put("target", target);
        this.args.put("ordinal", String.valueOf(ordinal));
        this.args.put("opcode", String.valueOf(opcode));

        Matcher matcher = InjectionPointData.AT_PATTERN.matcher(at);
        this.type = InjectionPointData.parseType(matcher, at);
        this.selector = InjectionPointData.parseSelector(matcher);
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
     * Get the selector value parsed from the injector
     */
    public Selector getSelector() {
        return this.selector;
    }
    
    /**
     * Get the context
     */
    public IMixinContext getContext() {
        return this.context;
    }
    
    /**
     * Get the annotated method
     */
    public MethodNode getMethod() {
        return this.method;
    }
    
    /**
     * Get the return type of the annotated method
     */
    public Type getMethodReturnType() {
        return Type.getReturnType(this.method.desc);
    }
    
    /**
     * Get the root annotation (eg. {@link Inject})
     */
    public AnnotationNode getParent() {
        return this.parent;
    }
    
    /**
     * Get the slice id specified on the injector
     */
    public String getSlice() {
        return this.slice;
    }
    
    public LocalVariableDiscriminator getLocalVariableDiscriminator() {
        return LocalVariableDiscriminator.parse(this.parent);
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
     * Get the supplied value from the named args as a {@link MemberInfo},
     * throws an exception if the argument cannot be parsed as a MemberInfo.
     * 
     * @param key argument name
     * @return argument value as a MemberInfo
     */
    public MemberInfo get(String key) {
        try {
            return MemberInfo.parseAndValidate(this.get(key, ""), this.context);
        } catch (InvalidMemberDescriptorException ex) {
            throw new InvalidInjectionPointException(this.context, "Failed parsing @At(\"%s\").%s descriptor \"%s\" on %s",
                    this.at, key, this.target, InjectionInfo.describeInjector(this.context, this.parent, this.method));
        }
    }
    
    /**
     * Get the target value specified on the injector
     */
    public MemberInfo getTarget() {
        try {
            return MemberInfo.parseAndValidate(this.target, this.context);
        } catch (InvalidMemberDescriptorException ex) {
            throw new InvalidInjectionPointException(this.context, "Failed parsing @At(\"%s\") descriptor \"%s\" on %s",
                    this.at, this.target, InjectionInfo.describeInjector(this.context, this.parent, this.method));
        }
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
     * Get the id specified on the injection point (or null if not specified)
     */
    public String getId() {
        return this.id;
    }
    
    @Override
    public String toString() {
        return this.type;
    }

    private static Pattern createPattern() {
        return Pattern.compile(String.format("^([^:]+):?(%s)?$", Joiner.on('|').join(Selector.values())));
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

    private static Selector parseSelector(Matcher matcher) {
        return matcher.matches() && matcher.group(2) != null ? Selector.valueOf(matcher.group(2)) : Selector.DEFAULT;
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

}
