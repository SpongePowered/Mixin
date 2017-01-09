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
import org.spongepowered.asm.mixin.injection.InjectionPoint.Selector;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionPointException;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

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
    private final MixinTargetContext mixin;
    
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
    
    public InjectionPointData(MixinTargetContext mixin, MethodNode method, AnnotationNode parent, String at, List<String> args, String target,
            String slice, int ordinal, int opcode) {
        this.mixin = mixin;
        this.method = method;
        this.parent = parent;
        this.at = at;
        this.target = target;
        this.slice = Strings.nullToEmpty(slice);
        this.ordinal = Math.max(-1, ordinal);
        this.opcode = opcode;
        
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

    public String getAt() {
        return this.at;
    }
    
    public String getType() {
        return this.type;
    }
    
    public Selector getSelector() {
        return this.selector;
    }
    
    public MixinTargetContext getMixin() {
        return this.mixin;
    }
    
    public MethodNode getMethod() {
        return this.method;
    }
    
    public AnnotationNode getParent() {
        return this.parent;
    }
    
    public String getSlice() {
        return this.slice;
    }
    
    public Type getReturnType() {
        return Type.getReturnType(this.method.desc);
    }
    
    public LocalVariableDiscriminator getLocalVariableDiscriminator() {
        return LocalVariableDiscriminator.parse(this.parent);
    }

    public String get(String key, String defaultValue) {
        String value = this.args.get(key);
        return value != null ? value : defaultValue;
    }
    
    public int get(String key, int defaultValue) {
        return this.parseInt(this.get(key, String.valueOf(defaultValue)), defaultValue);
    }
    
    public boolean get(String key, boolean defaultValue) {
        return this.parseBoolean(this.get(key, String.valueOf(defaultValue)), defaultValue);
    }

    public MemberInfo get(String key) {
        try {
            return MemberInfo.parseAndValidate(this.get(key, ""), this.mixin);
        } catch (InvalidMemberDescriptorException ex) {
            throw new InvalidInjectionPointException(this.mixin, "Failed parsing @At(\"%s\").%s descriptor \"%s\" on %s",
                    this.at, key, this.target, InjectionInfo.describeInjector(this.mixin, this.parent, this.method));
        }
    }
    
    private int parseInt(String string, int defaultValue) {
        try {
            return Integer.parseInt(string);
        } catch (Exception ex) {
            return defaultValue;
        }
    }
    
    private boolean parseBoolean(String string, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(string);
        } catch (Exception ex) {
            return defaultValue;
        }
    }
    
    public MemberInfo getTarget() {
        try {
            return MemberInfo.parseAndValidate(this.target, this.mixin);
        } catch (InvalidMemberDescriptorException ex) {
            throw new InvalidInjectionPointException(this.mixin, "Failed parsing @At(\"%s\") descriptor \"%s\" on %s",
                    this.at, this.target, InjectionInfo.describeInjector(this.mixin, this.parent, this.method));
        }
    }
    
    public int getOrdinal() {
        return this.ordinal;
    }
    
    public int getOpcode() {
        return this.opcode;
    }
    
    public int getOpcode(int defaultOpcode) {
        return this.opcode > 0 ? this.opcode : defaultOpcode;
    }
    
    public int getOpcode(int defaultOpcode, int... validOpcodes) {
        for (int validOpcode : validOpcodes) {
            if (this.opcode == validOpcode) {
                return this.opcode;
            }
        }
        return defaultOpcode;
    }
    
    @Override
    public String toString() {
        return this.type;
    }

    private static Pattern createPattern() {
        return Pattern.compile(String.format("^([^:]+):?(%s)?$", Joiner.on('|').join(Selector.values())));
    }

    private static String parseType(Matcher matcher, String at) {
        return matcher.matches() ? matcher.group(1) : at;
    }

    private static Selector parseSelector(Matcher matcher) {
        return matcher.matches() && matcher.group(2) != null ? Selector.valueOf(matcher.group(2)) : Selector.DEFAULT;
    }

    public static String parseType(String at) {
        Matcher matcher = InjectionPointData.AT_PATTERN.matcher(at);
        return InjectionPointData.parseType(matcher, at);
    }
    
}
