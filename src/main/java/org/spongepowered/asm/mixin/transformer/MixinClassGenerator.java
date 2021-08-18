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

import java.util.Locale;

import org.spongepowered.asm.logging.ILogger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IClassGenerator;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.perf.Profiler;
import org.spongepowered.asm.util.perf.Profiler.Section;

/**
 * Handles delegation of class generation tasks to the extensions
 */
public class MixinClassGenerator {

    /**
     * Log things for stuff
     */
    static final ILogger logger = MixinService.getService().getLogger("mixin");

    /**
     * Transformer extensions
     */
    private final Extensions extensions;
    
    /**
     * Profiler 
     */
    private final Profiler profiler;
    
    /**
     * Audit trail (if available); 
     */
    private final IMixinAuditTrail auditTrail;

    /**
     * ctor 
     */
    MixinClassGenerator(MixinEnvironment environment, Extensions extensions) {
        this.extensions = extensions;
        this.profiler = MixinEnvironment.getProfiler();
        this.auditTrail = MixinService.getService().getAuditTrail();
    }

    synchronized boolean generateClass(MixinEnvironment environment, String name, ClassNode classNode) {
        if (name == null) {
            MixinClassGenerator.logger.warn("MixinClassGenerator tried to generate a class with no name!");
            return false;
        }
        
        for (IClassGenerator generator : this.extensions.getGenerators()) {
            Section genTimer = this.profiler.begin("generator", generator.getClass().getSimpleName().toLowerCase(Locale.ROOT));
            boolean success = generator.generate(name, classNode);
            genTimer.end();
            if (success) {
                if (this.auditTrail != null) {
                    this.auditTrail.onGenerate(name, generator.getName());
                }
                this.extensions.export(environment, name.replace('.', '/'), false, classNode);
                return true;
            }
        }
        
        return false;
    }
    
}
