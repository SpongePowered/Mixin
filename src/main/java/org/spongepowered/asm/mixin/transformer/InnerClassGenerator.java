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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.AnnotationVisitor;
import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.ClassVisitor;
import org.spongepowered.asm.lib.ClassWriter;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.commons.Remapper;
import org.spongepowered.asm.lib.commons.ClassRemapper;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Method;
import org.spongepowered.asm.mixin.transformer.ext.IClassGenerator;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.transformers.MixinClassWriter;

/**
 * Class generator which creates unique copies of inner classes within mixins
 * which are specialised to the target class. 
 */
final class InnerClassGenerator implements IClassGenerator {
    
    /**
     * Information about an inner class instance. Implements {@link Remapper} so
     * that it can participate in the remapping process.
     */
    static class InnerClassInfo extends Remapper {
        
        /**
         * Generated class name
         */
        private final String name;
        
        /**
         * Original class name
         */
        private final String originalName;
        
        /**
         * Mixin which owns this inner class
         */
        private final MixinInfo owner;
        
        /**
         * Mixin target context
         */
        private final MixinTargetContext target;
        
        /**
         * Name of the owner mixin (class ref)
         */
        private final String ownerName;
        
        /**
         * Name of the target class (class ref) 
         */
        private final String targetName;

        InnerClassInfo(String name, String originalName, MixinInfo owner, MixinTargetContext target) {
            this.name = name;
            this.originalName = originalName;
            this.owner = owner;
            this.ownerName = owner.getClassRef();
            this.target = target;
            this.targetName = target.getTargetClassRef();
        }
        
        String getName() {
            return this.name;
        }
        
        String getOriginalName() {
            return this.originalName;
        }
        
        MixinInfo getOwner() {
            return this.owner;
        }
        
        MixinTargetContext getTarget() {
            return this.target;
        }
        
        String getOwnerName() {
            return this.ownerName;
        }
        
        String getTargetName() {
            return this.targetName;
        }
        
        byte[] getClassBytes() throws ClassNotFoundException, IOException {
            return MixinService.getService().getBytecodeProvider().getClassBytes(this.originalName, true);
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
         * @see org.spongepowered.asm.lib.commons.Remapper#map(java.lang.String)
         */
        @Override
        public String map(String key) {
            if (this.originalName.equals(key)) {
                return this.name;
            } else if (this.ownerName.equals(key)) {
                return this.targetName;
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
        
        public InnerClassAdapter(ClassVisitor cv, InnerClassInfo info) {
            super(Opcodes.ASM5, cv, info);
            this.info = info;
        }
        
        /* (non-Javadoc)
         * @see org.spongepowered.asm.lib.ClassVisitor
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
         * @see org.spongepowered.asm.lib.commons.RemappingClassAdapter
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
    private static final Logger logger = LogManager.getLogger("mixin");
    
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
     * @param owner Mixin which owns the original inner class
     * @param originalName Original inner class name
     * @param context Target class
     * @return new name
     */
    public String registerInnerClass(MixinInfo owner, String originalName, MixinTargetContext context) {
        String id = String.format("%s%s", originalName, context);
        String ref = this.innerClassNames.get(id);
        if (ref == null) {
            ref = InnerClassGenerator.getUniqueReference(originalName, context);
            this.innerClassNames.put(id, ref);
            this.innerClasses.put(ref, new InnerClassInfo(ref, originalName, owner, context));
            InnerClassGenerator.logger.debug("Inner class {} in {} on {} gets unique name {}", originalName, owner.getClassRef(),
                    context.getTargetClassRef(), ref);
        }
        return ref;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.IClassGenerator
     *      #generate(java.lang.String)
     */
    @Override
    public byte[] generate(String name) {
        String ref = name.replace('.', '/');
        InnerClassInfo info = this.innerClasses.get(ref);
        if (info != null) {
            return this.generate(info);
        }
        return null;
    }
    
    /**
     * Generates a specialised inner class by taking the original class bytecode
     * and remapping it against the target class.
     * 
     * @param info inner class info to process
     * @return generated class or null if generation failed
     */
    private byte[] generate(InnerClassInfo info) {
        try {
            InnerClassGenerator.logger.debug("Generating mapped inner class {} (originally {})", info.getName(), info.getOriginalName());
            ClassReader cr = new ClassReader(info.getClassBytes());
            ClassWriter cw = new MixinClassWriter(cr, 0);
            cr.accept(new InnerClassAdapter(cw, info), ClassReader.EXPAND_FRAMES);
            return cw.toByteArray();
        } catch (InvalidMixinException ex) {
            throw ex;
        } catch (Exception ex) {
            InnerClassGenerator.logger.catching(ex);
        }
        
        return null;
    }

    /**
     * To avoid accidental clashes with existing target classes, or classes from
     * multiple mixins, each remapped class gets a unique name.
     * 
     * @param originalName Original inner class name
     * @param context Target context
     * @return unique class name
     */
    private static String getUniqueReference(String originalName, MixinTargetContext context) {
        String name = originalName.substring(originalName.lastIndexOf('$') + 1);
        if (name.matches("^[0-9]+$")) {
            name = "Anonymous";
        }
        return String.format("%s$%s$%s", context.getTargetClassRef(), name, UUID.randomUUID().toString().replace("-", ""));
    }

}
