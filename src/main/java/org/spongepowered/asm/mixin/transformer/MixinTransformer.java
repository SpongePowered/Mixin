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

import java.lang.reflect.Constructor;
import java.util.List;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IExtensionRegistry;
import org.spongepowered.asm.mixin.transformer.ext.IHotSwap;
import org.spongepowered.asm.transformers.TreeTransformer;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.asm.ASM;

/**
 * Transformer which manages the mixin configuration and application process
 */
class MixinTransformer extends TreeTransformer implements IMixinTransformer {
    
    private static final String MIXIN_AGENT_CLASS = "org.spongepowered.tools.agent.MixinAgent";

    /**
     * Synthetic class registry
     */
    private final SyntheticClassRegistry syntheticClassRegistry;

    /**
     * Transformer extensions
     */
    private final Extensions extensions;
    
    /**
     * Hotswap agent, if available 
     */
    private final IHotSwap hotSwapper;

    /**
     * Mixin processor which actually manages application of mixins
     */
    private final MixinProcessor processor;
    
    /**
     * Class generator 
     */
    private final MixinClassGenerator generator;

    public MixinTransformer() {
        MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();
        
        Object globalMixinTransformer = environment.getActiveTransformer();
        if (globalMixinTransformer instanceof IMixinTransformer) {
            throw new MixinException("Terminating MixinTransformer instance " + this);
        }
        
        // I am a leaf on the wind
        environment.setActiveTransformer(this);
        
        this.syntheticClassRegistry = new SyntheticClassRegistry();
        this.extensions = new Extensions(this.syntheticClassRegistry);
        
        this.hotSwapper = this.initHotSwapper(environment);

        this.processor = new MixinProcessor(environment, this.extensions, this.hotSwapper);
        this.generator = new MixinClassGenerator(environment, this.extensions);
        
        DefaultExtensions.create(environment, this.extensions, this.syntheticClassRegistry);
    }
    
    private IHotSwap initHotSwapper(MixinEnvironment environment) {
        if (!environment.getOption(Option.HOT_SWAP)) {
            return null;
        }

        try {
            MixinProcessor.logger.info("Attempting to load Hot-Swap agent");
            @SuppressWarnings("unchecked")
            Class<? extends IHotSwap> clazz =
                    (Class<? extends IHotSwap>)Class.forName(MixinTransformer.MIXIN_AGENT_CLASS);
            Constructor<? extends IHotSwap> ctor = clazz.getDeclaredConstructor(IMixinTransformer.class);
            return ctor.newInstance(this);
        } catch (Throwable th) {
            MixinProcessor.logger.info("Hot-swap agent could not be loaded, hot swapping of mixins won't work. {}: {}",
                    th.getClass().getSimpleName(), th.getMessage());
        }

        return null;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.IMixinTransformer
     *      #getExtensions()
     */
    @Override
    public IExtensionRegistry getExtensions() {
        return this.extensions;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.ITransformer#getName()
     */
    @Override
    public String getName() {
        return this.getClass().getName();
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.ITransformer#isDelegationExcluded()
     */
    @Override
    public boolean isDelegationExcluded() {
        return true;
    }

    /**
     * Run audit process on current mixin processor
     * 
     * @param environment Environment for audit
     */
    @Override
    public void audit(MixinEnvironment environment) {
        this.processor.audit(environment);
    }

    /**
     * Callback from hotswap agent
     * 
     * @param mixinClass Name of the mixin
     * @param classNode New class
     * @return List of classes that need to be updated
     */
    @Override
    public List<String> reload(String mixinClass, ClassNode classNode) {
        return this.processor.reload(mixinClass, classNode);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.IMixinTransformer
     *      #transformClassBytes(java.lang.String, java.lang.String, byte[])
     */
    @Override
    public byte[] transformClassBytes(String name, String transformedName, byte[] basicClass) {
        if (transformedName == null) {
            return basicClass;
        }
        
        MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();

        if (basicClass == null) {
            return this.generateClass(environment, transformedName);
        }
        
        return this.transformClass(environment, transformedName, basicClass);
    }

    /**
     * Called when the transformation reason is computing_frames. The only
     * operation we care about here is adding interfaces to target classes but
     * at the moment we don't have sufficient scaffolding to determine that
     * without triggering re-entrance. Currently just a no-op in order to not
     * cause a re-entrance crash under ModLauncher 7.0+.
     * 
     * @param environment Current environment
     * @param name Class transformed name
     * @param classNode Class tree
     * @return true if the class was transformed
     */
    public boolean computeFramesForClass(MixinEnvironment environment, String name, ClassNode classNode) {
        // TODO compute added interfaces
        return false;
    }
    
    /**
     * Apply mixins and postprocessors to the supplied class
     * 
     * @param environment Current environment
     * @param name Class transformed name
     * @param classBytes Class bytecode
     * @return Transformed bytecode
     */
    public byte[] transformClass(MixinEnvironment environment, String name, byte[] classBytes) {
        ClassNode classNode = this.readClass(classBytes);
        if (this.processor.applyMixins(environment, name, classNode)) {
            return this.writeClass(classNode);
        }
        return classBytes;
    }

    /**
     * Apply mixins and postprocessors to the supplied class
     * 
     * @param environment Current environment
     * @param name Class transformed name
     * @param classNode Class tree
     * @return true if the class was transformed
     */
    public boolean transformClass(MixinEnvironment environment, String name, ClassNode classNode) {
        return this.processor.applyMixins(environment, name, classNode);
    }
    
    /**
     * Generate the specified mixin-synthetic class
     * 
     * @param environment Current environment
     * @param name Class name to generate
     * @return Generated bytecode or <tt>null</tt> if no class was generated
     */
    public byte[] generateClass(MixinEnvironment environment, String name) {
        ClassNode classNode = MixinTransformer.createEmptyClass(name);
        if (this.generator.generateClass(environment, name, classNode)) {
            return this.writeClass(classNode);
        }
        return null;
    }
    
    /**
     * @param environment Current environment
     * @param name Class transformed name
     * @param classNode Empty classnode to populate
     * @return True if the class was generated successfully
     */
    public boolean generateClass(MixinEnvironment environment, String name, ClassNode classNode) {
        return this.generator.generateClass(environment, name, classNode);
    }
    
    /**
     * You need to ask yourself why you're reading this comment  
     */
    private static ClassNode createEmptyClass(String name) {
        ClassNode classNode = new ClassNode(ASM.API_VERSION);
        classNode.name = name.replace('.', '/');
        classNode.version = MixinEnvironment.getCompatibilityLevel().classVersion();
        classNode.superName = Constants.OBJECT;
        return classNode;
    }

}
