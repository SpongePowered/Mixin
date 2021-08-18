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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.logging.ILogger;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.commons.ClassRemapper;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Method;
import org.spongepowered.asm.mixin.transformer.ext.IClassGenerator;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.service.ISyntheticClassInfo;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.IConsumer;
import org.spongepowered.asm.util.asm.ASM;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Class generator which creates unique copies of inner classes within mixins
 * which are specialised to the target class. 
 */
final class InnerClassGenerator implements IClassGenerator {
    
    /**
     * Information about an inner class instance. Implements {@link Remapper} so
     * that it can participate in the remapping process.
     */
    static class InnerClassInfo extends Remapper implements ISyntheticClassInfo {
        
        /**
         * Mixin which provides this class
         */
        private final IMixinInfo mixin;
        
        /**
         * Target class info
         */
        private final ClassInfo targetClassInfo;
        
        /**
         * Original class name
         */
        private final String originalName;
        
        /**
         * Class name (internal name)
         */
        private final String name;
        
        /**
         * Mixin which owns this inner class
         */
        private final MixinInfo owner;
        
        /**
         * Name of the owner mixin (class ref)
         */
        private final String ownerName;
        
        /**
         * Name of the new nest host based on the target class 
         */
        private final String nestHostName;
        
        /**
         *  Number of times this inner class has been generated
         */
        private int loadCounter;

        InnerClassInfo(IMixinInfo mixin, ClassInfo targetClass, ClassInfo nestHost, String originalName, String name, MixinInfo owner) {
            this.mixin = mixin;
            this.targetClassInfo = targetClass;
            this.originalName = originalName;
            this.name = name;
            this.owner = owner;
            this.ownerName = owner.getClassRef();
            this.nestHostName = nestHost.getName();
        }
        
        @Override
        public IMixinInfo getMixin() {
            return this.mixin;
        }
        
        @Override
        public boolean isLoaded() {
            return this.loadCounter > 0;
        }

        @Override
        public String getName() {
            return this.name;
        }
        
        @Override
        public String getClassName() {
            return this.name.replace('/', '.');
        }
        
        String getOriginalName() {
            return this.originalName;
        }
        
        MixinInfo getOwner() {
            return this.owner;
        }
        
        String getOwnerName() {
            return this.ownerName;
        }
        
        String getTargetName() {
            return this.targetClassInfo.getName();
        }
        
        ClassInfo getTargetClass() {
            return this.targetClassInfo;
        }
        
        String getNestHostName() {
            return this.nestHostName;
        }
        
        void accept(final ClassVisitor classVisitor) throws ClassNotFoundException, IOException {
            ClassNode classNode = MixinService.getService().getBytecodeProvider().getClassNode(this.originalName);
            classNode.accept(classVisitor);
            this.loadCounter++;
        }
        
        /**
         * Used to remap synthetic accessor methods which have been renamed when
         * being applied to the target class.
         */
        @Override
        public String mapMethodName(String owner, String name, String desc) {
            if (this.ownerName.equalsIgnoreCase(owner)) {
                Method method = this.owner.getClassInfo().findMethod(name, desc, ClassInfo.INCLUDE_ALL);
                if (method != null) {
                    return method.getName();
                }
            }
            return super.mapMethodName(owner, name, desc);
        }

        /* (non-Javadoc)
         * @see org.objectweb.asm.commons.Remapper#map(java.lang.String)
         */
        @Override
        public String map(String key) {
            if (this.originalName.equals(key)) {
                return this.name;
            } else if (this.ownerName.equals(key)) {
                return this.targetClassInfo.getName();
            }
            return key;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return this.name;
        }
        
    }
    
    /**
     * Just a basic remapping adapter, but we also decorate the transformed
     * class with a meta annotation describing the original class.
     */
    static class InnerClassAdapter extends ClassRemapper {
        
        private final InnerClassInfo info;
        
        InnerClassAdapter(ClassVisitor cv, InnerClassInfo info) {
            super(ASM.API_VERSION, cv, info);
            this.info = info;
        }
        
        /* (non-Javadoc)
         * @see org.objectweb.asm.commons.ClassRemapper
         *      #visitNestHost(java.lang.String)
         */
        @Override
        public void visitNestHost(String nestHost) {
            // Discard the original nest host and use the resolved nest host
            // from the target class
            this.cv.visitNestHost(this.info.getNestHostName());
        }
        
        /* (non-Javadoc)
         * @see org.objectweb.asm.ClassVisitor
         *      #visitSource(java.lang.String, java.lang.String)
         */
        @Override
        public void visitSource(String source, String debug) {
            super.visitSource(source, debug);
            AnnotationVisitor av = this.cv.visitAnnotation("Lorg/spongepowered/asm/mixin/transformer/meta/MixinInner;", false);
            av.visit("mixin", this.info.getOwner().toString());
            av.visit("name", this.info.getOriginalName().substring(this.info.getOriginalName().lastIndexOf('/') + 1));
            av.visitEnd();
        }
        
        /* (non-Javadoc)
         * @see org.objectweb.asm.commons.RemappingClassAdapter
         *      #visitInnerClass(java.lang.String, java.lang.String,
         *      java.lang.String, int)
         */
        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            if (name.startsWith(this.info.getOriginalName() + "$")) {
                throw new InvalidMixinException(this.info.getOwner(), "Found unsupported nested inner class " + name + " in "
                        + this.info.getOriginalName());
            }
            
            super.visitInnerClass(name, outerName, innerName, access);
        }
        
    }
    
    /**
     * Logger
     */
    private static final ILogger logger = MixinService.getService().getLogger("mixin");
    
    /**
     * Synthetic class registry 
     */
    private final IConsumer<ISyntheticClassInfo> registry;
    
    /**
     * Mapping of target class context ids to generated inner class names, used
     * so we don't accidentally conform the same class twice.
     */
    private final Map<String, String> innerClassNames = new HashMap<String, String>();

    /**
     * Mapping of generated class names to the respective inner class info
     */
    private final Map<String, InnerClassInfo> innerClasses = new HashMap<String, InnerClassInfo>();
    
    /**
     * Coprocessor which handles merging nest members into nest hosts which may
     * or may not be mixin targets themselves 
     */
    private final MixinCoprocessorNestHost nestHostCoprocessor;

    /**
     * Ctor
     * 
     * @param registry sythetic class registry
     */
    public InnerClassGenerator(IConsumer<ISyntheticClassInfo> registry, MixinCoprocessorNestHost nestHostCoprocessor) {
        this.registry = registry;
        this.nestHostCoprocessor = nestHostCoprocessor;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.ext.IClassGenerator
     *      #getName()
     */
    @Override
    public String getName() {
        return "inner";
    }

    /**
     * Register a mixin inner class against the specified target
     * 
     * @param owner Mixin which owns the original inner class
     * @param targetClass Target class name
     * @param innerClassName Original inner class name
     */
    void registerInnerClass(MixinInfo owner, ClassInfo targetClass, String innerClassName) {
        String coordinate = String.format("%s:%s:%s", owner, innerClassName, targetClass.getName());
        String uniqueName = this.innerClassNames.get(coordinate);
        if (uniqueName != null) {
            return;
        }
        uniqueName = InnerClassGenerator.getUniqueReference(innerClassName, targetClass);
        ClassInfo nestHost = targetClass.resolveNestHost();
        InnerClassInfo info = new InnerClassInfo(owner, targetClass, nestHost, innerClassName, uniqueName, owner);
        this.innerClassNames.put(coordinate, uniqueName);
        this.innerClasses.put(uniqueName, info);
        this.registry.accept(info);
        InnerClassGenerator.logger.debug("Inner class {} in {} on {} gets unique name {}",
                innerClassName, owner.getClassRef(), targetClass, uniqueName);
        this.nestHostCoprocessor.registerNestMember(nestHost.getClassName(), uniqueName);
    }

    /**
     * Get a BiMap of inner classes for the specified mixin+target combination
     * so that references to the inner class can be remapped during application
     * 
     * @param owner Mixin which owns the original inner class
     * @param targetName Target class name
     * @return BiMap of original (mixin) inner class names to conformed class
     *      names
     */
    BiMap<String, String> getInnerClasses(MixinInfo owner, String targetName) {
        BiMap<String, String> innerClasses = HashBiMap.<String, String>create();
        for (InnerClassInfo innerClass : this.innerClasses.values()) {
            if (innerClass.getMixin() == owner && targetName.equals(innerClass.getTargetName())) {
                innerClasses.put(innerClass.getOriginalName(), innerClass.getName());
            }
        }
        return innerClasses;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.ext.IClassGenerator
     *      #generate(java.lang.String, org.objectweb.asm.tree.ClassNode)
     */
    @Override
    public boolean generate(String name, ClassNode classNode) {
        String ref = name.replace('.', '/');
        InnerClassInfo info = this.innerClasses.get(ref);
        if (info == null) {
            return false;
        }
        return this.generate(info, classNode);
    }
    
    /**
     * Generates a specialised inner class by taking the original class bytecode
     * and remapping it against the target class.
     * 
     * @param info inner class info to process
     * @return true if class was generated successfully
     */
    private boolean generate(InnerClassInfo info, ClassNode classNode) {
        try {
            InnerClassGenerator.logger.debug("Generating mapped inner class {} (originally {})", info.getName(), info.getOriginalName());
            info.accept(new InnerClassAdapter(classNode, info));
            return true;
        } catch (InvalidMixinException ex) {
            throw ex;
        } catch (Exception ex) {
            InnerClassGenerator.logger.catching(ex);
        }
        
        return false;
    }

    /**
     * To avoid accidental clashes with existing target classes, or classes from
     * multiple mixins, each remapped class gets a unique name.
     * 
     * @param originalName Original inner class name
     * @param targetClass Target class
     * @return unique class name
     */
    private static String getUniqueReference(String originalName, ClassInfo targetClass) {
        String name = originalName.substring(originalName.lastIndexOf('$') + 1);
        if (name.matches("^[0-9]+$")) {
            name = "Anonymous";
        }
        return String.format("%s$%s$%s", targetClass, name, UUID.randomUUID().toString().replace("-", ""));
    }

}
