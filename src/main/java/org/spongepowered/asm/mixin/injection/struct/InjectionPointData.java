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

import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.InvalidInjectionPointException;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

/**
 * Data read from an {@link org.spongepowered.asm.mixin.injection.At} annotation
 * and passed into an InjectionPoint ctor
 */
public class InjectionPointData {

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
    private MethodNode method;

    /**
     * Parent annotation
     */
    private AnnotationNode parent;

    /**
     * At arg 
     */
    private String at;

    /**
     * Target 
     */
    private final String target;
    
    /**
     * Ordinal 
     */
    private final int ordinal;
    
    /**
     * Opcode 
     */
    private final int opcode;

    public InjectionPointData(MixinTargetContext mixin, MethodNode method, AnnotationNode parent, String at, List<String> args, String target,
            int ordinal, int opcode) {
        this.mixin = mixin;
        this.method = method;
        this.parent = parent;
        this.at = at;
        this.target = target;
        this.ordinal = Math.max(-1, ordinal);
        this.opcode = opcode;
        
        this.parseArgs(args);
        
        this.args.put("target", target);
        this.args.put("ordinal", String.valueOf(ordinal));
        this.args.put("opcode", String.valueOf(opcode));
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
    
    public MixinTargetContext getMixin() {
        return this.mixin;
    }
    
    public MethodNode getMethod() {
        return this.method;
    }
    
    public AnnotationNode getParent() {
        return this.parent;
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
}
