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
package org.spongepowered.asm.mixin.transformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Method;
import org.spongepowered.asm.mixin.transformer.MixinInfo.MixinMethodNode;
import org.spongepowered.asm.util.Counter;

import com.google.common.base.Strings;

/**
 * Maintains method remaps for a target class
 */
public class MethodMapper {

    /**
     * Logger
     */
    private static final Logger logger = LogManager.getLogger("mixin");
    
    private static final List<String> classes = new ArrayList<String>();
    
    /**
     * Method descriptor to ID map, used to ensure that remappings are globally
     * unique 
     */
    private static final Map<String, Counter> methods = new HashMap<String, Counter>();

    private final ClassInfo info;

    public MethodMapper(MixinEnvironment env, ClassInfo info) {
        this.info = info;
    }
    
    public ClassInfo getClassInfo() {
        return this.info;
    }
    
    /**
     * Conforms an injector handler method
     * 
     * @param mixin owner mixin
     * @param handler annotated injector handler method
     * @param method method in target
     */
    public void remapHandlerMethod(MixinInfo mixin, MethodNode handler, Method method) {
        if (!(handler instanceof MixinMethodNode) || !((MixinMethodNode)handler).isInjector()) {
            return;
        }
        
        if (method.isUnique()) {
            MethodMapper.logger.warn("Redundant @Unique on injector method {} in {}. Injectors are implicitly unique", method, mixin);
        }
        
        if (method.isRenamed()) {
            handler.name = method.getName();
            return;
        }
        
        String handlerName = this.getHandlerName((MixinMethodNode)handler);
        handler.name = method.renameTo(handlerName);
    }
    
    /**
     * Get the name for a handler method provided a source mixin method
     * 
     * @param method mixin method
     * @return conformed handler name
     */
    public String getHandlerName(MixinMethodNode method) {
        String prefix = InjectionInfo.getInjectorPrefix(method.getInjectorAnnotation());
        String classUID = MethodMapper.getClassUID(method.getOwner().getClassRef());
        String methodUID = MethodMapper.getMethodUID(method.name, method.desc, !method.isSurrogate());
        return String.format("%s$%s$%s%s", prefix, method.name, classUID, methodUID);
    }

    /**
     * Get a unique identifier for a class
     * 
     * @param classRef Class name (binary)
     * @return unique identifier
     */
    private static String getClassUID(String classRef) {
        int index = MethodMapper.classes.indexOf(classRef);
        if (index < 0) {
            index = MethodMapper.classes.size();
            MethodMapper.classes.add(classRef);
        }
        return MethodMapper.finagle(index);
    }

    /**
     * Get a unique identifier for a method
     * 
     * @param name method name
     * @param desc method descriptor
     * @param increment true to incrememnt the id if it already exists
     * @return unique identifier
     */
    private static String getMethodUID(String name, String desc, boolean increment) {
        String descriptor = String.format("%s%s", name, desc);
        Counter id = MethodMapper.methods.get(descriptor);
        if (id == null) {
            id = new Counter();
            MethodMapper.methods.put(descriptor, id);
        } else if (increment) {
            id.value++;
        }
        return String.format("%03x", id.value);
    }

    /**
     * Finagle a string from an index thingummy, for science, you monster
     * 
     * @param index a positive number
     * @return unique identifier string of some kind
     */
    private static String finagle(int index) {
        String hex = Integer.toHexString(index);
        StringBuilder sb = new StringBuilder();
        for (int pos = 0; pos < hex.length(); pos++) {
            char c = hex.charAt(pos);
            sb.append(c += c < 0x3A ? 0x31 : 0x0A);
        }
        return Strings.padStart(sb.toString(), 3, 'z');
    }

}
