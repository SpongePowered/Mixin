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

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Method;
import org.spongepowered.asm.mixin.transformer.MixinInfo.MixinMethodNode;

/**
 * Maintains method remaps for a target class
 */
public class MethodMapper {
    
    /**
     * Mutable integer
     */
    static class Counter {
        public int value;
    }

    /**
     * Logger
     */
    private static final Logger logger = LogManager.getLogger("mixin");
    
    /**
     * Method descriptor to ID map, used to ensure that remappings are globally
     * unique 
     */
    private static final Map<String, Counter> methodIndices = new HashMap<String, Counter>();

    private final ClassInfo info;

    public MethodMapper(MixinEnvironment env, ClassInfo info) {
        this.info = info;
    }
    
    public ClassInfo getClassInfo() {
        return this.info;
    }
    
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
    
    public String getHandlerName(MixinMethodNode method) {
        String descriptor = String.format("%s%s", method.name, method.desc);
        Counter id = MethodMapper.methodIndices.get(descriptor);
        if (id == null) {
            id = new Counter();
            MethodMapper.methodIndices.put(descriptor, id);
        } else if (!method.isSurrogate()) {
            id.value++;
        }
        
        String prefix = InjectionInfo.getInjectorPrefix(method.getInjectorAnnotation());
        String uniqueIndex = Integer.toHexString(id.value);
        return String.format("%s$%s$%s", prefix, method.name, uniqueIndex);
    }

}
